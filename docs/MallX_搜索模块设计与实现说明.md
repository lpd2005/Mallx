# MallX 搜索模块设计与实现说明

## 1. 模块定位

`mall-search` 使用 Elasticsearch 提供商品全文搜索。MySQL 仍是商品和库存的唯一事实来源，ES 只保存适合检索的冗余文档，允许短暂的最终一致。

当前能力：

- 商品名称、副标题、品牌、分类和 SKU 规格全文检索
- 分类、品牌、价格、有货状态筛选
- 相关度、价格、销量、更新时间排序
- 商品名称和副标题高亮
- 搜索联想、Redis ZSet 热词统计
- RabbitMQ 异步增量同步、事务 Outbox、Confirm/Return、消费重试和死信留档
- 基于版本索引和别名原子切换的全量重建
- 后台索引状态、重建、单商品同步和失败事件重放

## 2. 运行前置条件

项目使用 Spring Boot 3.5.9 管理的 Spring Data Elasticsearch 5.5.x 和 Elasticsearch Java Client 8.18.x，因此服务端使用 Elasticsearch 8.18.x。商品字段映射使用 `ik_max_word` 和 `ik_smart`，ES 节点必须先安装与服务端版本一致的 IK 插件。

已有数据库需要执行 `mall-product/src/main/resources/db/schema-product.sql` 中新增的 `product_search_outbox` 建表语句，并执行 `mall-admin/src/main/resources/db/data-admin-rbac.sql` 补充 `search:manage` 权限。

默认连接配置：

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200

mallx:
  search:
    index-alias: product_index
```

可通过 `mall.search.elasticsearch.uris`、`mall.search.elasticsearch.username` 和 `mall.search.elasticsearch.password` 覆盖连接信息。

## 3. 索引文档

一条 ES 文档对应一个 SPU，并冗余以下内容：

- SPU：名称、副标题、描述、主图、状态时间
- 品牌：ID、名称
- 分类：叶子分类 ID、完整三级分类 ID/名称路径
- 聚合值：最低价、最高价、是否有库存、累计销量
- SKU nested 数组：SKU ID、名称、规格、售价、可售库存、销量

价格与库存筛选放在同一个 nested 查询中，保证条件由同一个 SKU 同时满足。下架 SPU、没有在售 SKU 的 SPU 不进入索引；下架时同步逻辑会删除对应文档。

## 4. 增量同步与可靠性

```text
商品/库存事务
    ├── 修改业务表
    └── 同事务写 product_search_outbox
             ↓ 定时发布
         RabbitMQ
             ↓
       mall-search 消费
             ↓
    重新从 MySQL 聚合最新状态
             ↓
       覆盖写入或删除 ES 文档
```

该设计不直接把“商品修改”和“写 ES”做同步双写：业务事务成功后事件一定留在 Outbox；Broker Confirm 或 Return 失败时事件仍保持待发送并按时间重投。消费端不使用消息中的商品快照，而是按 `spuId` 查询最新数据，因此重复消息和乱序消息都能收敛为最新文档。

消费连续失败超过 RabbitMQ 监听重试次数后进入死信队列。死信监听器将事件记录到 Redis Hash `mallx:search:sync:failures`，管理员排除故障后可手动重放。搜索索引仍可通过全量重建最终校准。

## 5. 全量重建

全量重建不清空线上索引：

1. 创建 `product_index_v时间戳` 物理索引。
2. 以 SPU ID 游标分批从 MySQL 聚合并 bulk 写入。
3. 刷新新索引。
4. 一次 alias action 移除旧指向并把 `product_index` 指向新索引。

重建持有 Redisson 读写锁的写锁，单文档同步持有读锁，避免增量写入落到即将被切走的旧索引。旧物理索引暂时保留，便于人工回滚，确认稳定后再由运维清理。

## 6. 接口

前台公开接口：

- `GET /search/products`
- `GET /search/suggestions?keyword=手机`
- `GET /search/hot-words`

后台接口统一放在 `mall-admin`，均要求 `search:manage`：

- `GET /admin/search/index/status`
- `POST /admin/search/index/rebuild`
- `POST /admin/search/index/products/{spuId}/sync`
- `GET /admin/search/failures`
- `POST /admin/search/failures/{eventId}/replay`

后台重建、单商品同步和失败重放均通过 `AdminOperationLogService` 记录操作日志。

## 7. 使用限制

- 普通页码分页只允许访问 ES 默认结果窗口内的前 10000 条数据，超出时返回参数错误。需要无限滚动时应单独增加 `search_after` 游标接口。
- 高亮结果包含 `<em>` 标记，前端必须转义原始文本并仅对白名单标签放行，不能直接信任为安全 HTML。
- 首次使用前应由管理员执行一次全量重建；空索引的自动创建只保证接口可用，不会自动导入历史商品。
- Redis 热词是轻量实时统计，不代替离线分析和敏感词治理。

## 8. 验证建议

先执行 Maven 测试，再在 MySQL、Redis、RabbitMQ、Elasticsearch 和 IK 都可用的环境中验证：全量重建、中文分词、高亮、组合筛选、商品上下架、SKU 改价、库存变化、RabbitMQ 停机重投以及 ES 故障进入死信并重放。
