# MallX 多级缓存设计与实现说明

## 1. 实现目标

本次升级在商品域落地 `Caffeine L1 -> Redis L2 -> MySQL` 多级缓存。缓存读写生命周期由 JetCache 注解负责，SPU/SKU 穿透防护直接使用 Redisson `RBloomFilter`。

首批缓存覆盖：

- 已上架 SPU 详情；
- 已上架 SKU 详情；
- SPU 下的已上架 SKU 列表；
- 启用的分类树；
- 启用的品牌列表；
- 无筛选条件的商品首页（第 1 页，每页 20 条）。

任意组合的搜索分页暂不缓存，防止低复用查询制造大量缓存 Key。

## 2. 读链路

商品首页、商品、SKU、分类和品牌查询使用 `@Cached(cacheType = CacheType.BOTH)`，由 JetCache 依次读取 Caffeine 和 Redis；缓存未命中后，`@CachePenetrationProtect` 合并当前 JVM 内相同 Key 的并发回源。普通 SPU/SKU 及 SKU 列表不主动刷新；`@CacheRefresh` 只用于首页、分类树和品牌列表等少量聚合热点。SPU/SKU 详情只在缓存未命中进入方法后检查 `RBloomFilter.contains()`，再读取 MySQL。

商品首页只缓存无筛选的第一页，通过 `condition` 控制，避免任意搜索条件制造大量 Key。秒杀库存及 Lua 原子扣减不属于本阶段，将在秒杀模块实现。

## 3. 风险防护

### 3.1 缓存穿透

- Redisson `RBloomFilter` 保存数据库中曾经存在的 SPU/SKU ID；
- Bloom 明确判定不存在时直接返回 404，不访问数据库；
- `ProductBloomFilterConfiguration` 直接创建两个 `RBloomFilter<Long>` Bean，并调用 `tryInit()`；
- `ProductBloomFilterInitializer` 使用 `@PostConstruct` 查询全部已有 ID，再调用 `add(Collection)`；
- 只在创建商品后调用 `add(id)` 增量写入；修改和状态变化不会改变 ID，因此不重复添加；
- 删除商品时无需删除 Bloom 中的 ID，残留只会产生允许的假阳性。

### 3.2 缓存击穿

- 普通 Key 使用 JetCache `@CachePenetrationProtect` 合并 JVM 内相同 Key 的并发回源；
- 首页、分类树和品牌列表等少量聚合热点使用 `@CacheRefresh` 主动刷新；
- `refreshLockTimeout` 使用 JetCache 的远程刷新锁，避免多实例同时刷新同一 Key；
- 秒杀等需要强原子性的超级热点单独使用 Redisson 锁或 Redis Lua，不放进普通商品缓存层。

### 3.3 缓存雪崩

- L1 和 L2 使用不同有效期；
- 不同业务缓存使用分层 TTL；
- 热点通过 `@CacheRefresh` 在过期前刷新，降低集中失效风险。

### 3.4 数据一致性

- 分类、品牌及参数可直接表达的 SPU/SKU 写方法直接使用 `@CacheInvalidate`；
- `ProductCacheInvalidator` 只处理旧所属 SPU、子 SKU 集合等局部变量产生的动态关联 Key；
- 不等待事务提交完成后再删除；若事务最终回滚，只会造成下一次查询回源；
- JetCache `syncLocal` 通过 Redis 广播失效消息，使其他应用实例同步清理 L1；
- SPU 状态改变时同时清理子 SKU 详情；SKU 改动时清理 SKU、所属 SPU、SKU 列表与首页；
- 缓存失效通知不是数据库事务的一部分，极端的进程崩溃窗口最终由 TTL 收敛。后续 MQ 阶段可使用事务消息或 Outbox 将失效事件升级为可重试的可靠事件。

## 4. 缓存清单

| 缓存 | Key 前缀 | L1 TTL | L2 基础 TTL | 失效触发 |
| --- | --- | ---: | ---: | --- |
| 按 ID 查询 SPU | `mallx:cache:v2:product:spu-by-id:` | 30s | 10m | SPU/SKU 变更、状态变化 |
| 按 ID 查询 SKU | `mallx:cache:v2:product:sku-by-id:` | 25s | 8m | SKU 变更、父 SPU 状态变化 |
| 按 SPU ID 查询 SKU 列表 | `mallx:cache:v2:product:skus-by-spu-id:` | 20s | 5m | SPU/SKU 变更 |
| 分类树 | `mallx:cache:v2:product:category-tree:` | 1m | 30m | 分类写操作 |
| 启用品牌列表 | `mallx:cache:v2:product:enabled-brands:` | 1m | 30m | 品牌写操作 |
| 默认商品首页 | `mallx:cache:v2:product:home-page:` | 10s | 2m | SPU/SKU/分类/品牌写操作 |

普通注解缓存使用版本化的静态缓存名称，SpEL 只计算方法参数，逻辑格式为：

```text
mallx:cache:v2:product:{查询用途}:{业务参数}
```

例如 SPU 1001 的详情 Key 是 `mallx:cache:v2:product:spu-by-id:1001`。代码统一在
`ProductCacheDefinition` 中分组：`Name` 是 JetCache 缓存名称，`Key` 是注解 SpEL，
`Condition` 是缓存条件，`Bloom` 是 Redisson 布隆过滤器名称。

布隆过滤器使用以下名称：

- `mallx:cache:v2:product:bloom:existing-spu-ids`
- `mallx:cache:v2:product:bloom:existing-sku-ids`

每个 BloomFilter 在 Redis 中还会出现一个带 `{...}:config` 的 Key，这是 Redisson 自动维护的
容量、误判率等元数据，不是重复的业务缓存。

JetCache 2.7.9 的注解表达式上下文不提供 Spring BeanResolver，因此不能在 `key` 或 `condition` 中使用 `@cacheProperties`。环境间应使用独立 Redis 实例或 database 隔离；修改缓存对象的序列化结构时应提升静态缓存名称版本，例如从 `v1` 改为 `v2`，避免读取旧结构。

## 5. 配置

主要配置位于 `mallx-application/src/main/resources/application.yml`：

```yaml
mallx:
  cache:
    bloom:
      expected-insertions: 1000000
      false-probability: 0.001
    redisson:
      host: ${spring.data.redis.host}
      port: ${spring.data.redis.port}
      database: ${spring.data.redis.database}
      password: ${spring.data.redis.password:}
      connection-pool-size: 32
      connection-minimum-idle-size: 4
```

容量规划时应根据实际已上架 SKU 数量调整 `expected-insertions`。预估容量过小会导致误判率上升；误判只会让请求继续访问缓存或数据库，不会把存在的数据判为不存在。

## 6. Redis 与服务降级行为

| 故障 | 行为 |
| --- | --- |
| Caffeine 缓存未命中 | 继续读取 Redis |
| Redis 缓存未命中 | 进入被注解的业务方法并访问数据库 |
| Bloom 判定不存在 | 直接返回商品不存在，不访问数据库 |
| Redisson 不可用 | Bloom 查询失败；由统一异常处理返回服务错误 |
| 事务回滚 | 此前删除的缓存将在下一次查询时重新加载 |

## 7. 验证方式

执行商品域测试：

```powershell
mvn -pl mall-product -am test
```

执行全项目打包：

```powershell
mvn package
```

单元测试覆盖：

- 查询方法使用 `@Cached`、`@CacheRefresh` 和 `@CachePenetrationProtect`；
- 写后失效方法使用 JetCache `@CacheInvalidate`；
- Bloom 初始化直接调用 Redisson `tryInit()` 和 `add(Collection)`。
- JetCache 远端缓存使用内置 Java 序列化，缓存 VO 均实现 `Serializable`。

JetCache 每分钟输出命中率、读取次数、失败次数和加载耗时。生产环境还应将这些数据接入统一监控。

## 8. 本阶段边界

本阶段不包括秒杀库存、购物车、订单、用户会话与双 Token 缓存；这些对象的一致性模型不同，应在各自阶段独立设计。后续接入 MQ 时，优先把商品缓存失效改造为 Outbox + 消费者幂等重试，以进一步缩短数据库提交后进程崩溃造成的不一致窗口。
