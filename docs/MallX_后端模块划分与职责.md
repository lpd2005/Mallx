# MallX 后端模块划分与职责设计

> 项目定位：**模块化单体的企业级电商平台**，后续可平滑演进为微服务架构。
>
> 核心技术方向：Spring Boot 3 + MyBatis-Plus + Sa-Token + MySQL + Caffeine + Redis + RabbitMQ + Elasticsearch + 阿里云 OSS + Python/FastAPI + LangChain/LangGraph。

---

## 1. 整体模块划分

推荐采用以下模块结构：

```text
mallx-parent
│
├── mall-common          公共基础模块
├── mall-auth            认证与权限模块
├── mall-user            用户中心模块
├── mall-product         商品中心模块
├── mall-inventory       库存中心模块
├── mall-cart            购物车模块
├── mall-order           订单交易模块
├── mall-payment         支付模块
├── mall-coupon          优惠券/营销模块
├── mall-seckill         秒杀模块
├── mall-search          搜索模块
├── mall-after-sale      售后模块
├── mall-admin           后台管理与运营模块
└── mall-ai              AI 能力接入模块
```

前端单独作为一个项目：

```text
mallx-ui
```

Python Agent 服务单独作为一个项目：

```text
mallx-ai-service
```

---

# 2. 模块职责总览

| 模块 | 核心职责 | 主要技术点 |
|---|---|---|
| mall-common | 公共基础能力 | Redis、Caffeine、RabbitMQ、OSS、统一异常、统一返回 |
| mall-auth | 登录、认证、授权 | Sa-Token、JWT/Token、RBAC |
| mall-user | 用户与地址 | 用户、地址、会员、收藏、历史 |
| mall-product | 商品中心 | SPU、SKU、分类、品牌、商品缓存 |
| mall-inventory | 库存中心 | 库存扣减、锁定、释放、库存流水 |
| mall-cart | 购物车 | Redis、购物车结算 |
| mall-order | 订单交易 | 下单、订单状态机、订单超时、幂等 |
| mall-payment | 支付 | 支付单、支付回调、退款、幂等 |
| mall-coupon | 优惠券 | 领取、使用、回滚、高并发防重复 |
| mall-seckill | 秒杀 | Redis、Lua、分布式锁、RabbitMQ、高并发 |
| mall-search | 搜索 | Elasticsearch、商品索引、筛选、排序、高亮 |
| mall-after-sale | 售后 | 退款、退货、换货、售后审核 |
| mall-admin | 后台运营 | 数据统计、运营管理、审核、配置 |
| mall-ai | AI 接入层 | SSE、AI 会话、调用 Python Agent |

---

# 3. mall-common —— 公共基础模块

## 3.1 模块定位

`mall-common` 是所有业务模块共享的基础模块。

核心原则：

> **公共模块只提供通用能力，不承载具体业务。**

## 3.2 主要职责

### 统一返回

```text
Result<T>
PageResult<T>
```

### 全局异常处理

```text
BusinessException
GlobalExceptionHandler
```

### 公共工具

```text
雪花 ID
日期工具
JSON 工具
分页工具
加密工具
```

### Redis

负责：

```text
RedisTemplate
Redis配置
序列化
Lua脚本基础能力
分布式锁基础能力
```

### Caffeine

负责：

```text
本地缓存配置
CacheManager
缓存淘汰策略
TTL
容量配置
```

### 多级缓存

提供统一能力：

```text
Caffeine L1
    ↓
Redis L2
    ↓
MySQL
```

业务模块负责自己的缓存数据，`mall-common` 只提供缓存框架和基础能力。

### RabbitMQ

负责：

```text
ConnectionFactory
Exchange/Queue基础配置
消息序列化
消息发送基础封装
```

具体业务消息生产者和消费者放在对应业务模块。

### 阿里云 OSS

负责：

```text
OSS客户端初始化
文件上传
文件删除
文件URL处理
```

### 日志与链路基础能力

```text
统一日志格式
TraceId
请求耗时记录
```

## 3.3 典型目录

```text
mall-common
└── src/main/java/com/mallx/common
    ├── cache
    │   ├── caffeine
    │   └── redis
    ├── config
    ├── constant
    ├── enums
    ├── exception
    ├── mq
    ├── oss
    ├── result
    ├── utils
    └── web
```

---

# 4. mall-auth —— 认证与权限模块

## 4.1 模块定位

负责整个系统的身份认证、会话管理和权限控制。

本项目使用：

> **Sa-Token**

不使用 Spring Security。

## 4.2 主要职责

```text
用户登录
用户登出
Token管理
Session管理
验证码
角色管理
权限校验
```

## 4.3 角色示例

```text
USER
ADMIN
OPERATOR
```

后续可以扩展：

```text
SUPER_ADMIN
CUSTOMER_SERVICE
WAREHOUSE
FINANCE
```

## 4.4 权限示例

```text
product:add
product:update
product:delete
order:view
order:ship
coupon:manage
seckill:manage
user:manage
```

## 4.5 重点

- Sa-Token 登录
- Token 生命周期
- RBAC
- 路由权限控制
- 接口权限控制

---

# 5. mall-user —— 用户中心模块

## 5.1 模块定位

负责 C 端用户相关业务。

## 5.2 主要职责

```text
用户基本信息
收货地址
用户等级/会员
用户收藏
浏览历史
```

## 5.3 注意事项

`mall-user` 负责用户业务，但**登录本身属于 mall-auth**。

例如：

```text
mall-auth
    ↓
负责登录/Token

mall-user
    ↓
负责用户资料/地址/会员
```

## 5.4 主要实体

```text
User
UserAddress
UserFavorite
UserHistory
```

---

# 6. mall-product —— 商品中心模块

## 6.1 模块定位

电商核心数据中心之一，负责商品定义和商品展示。

## 6.2 主要职责

```text
SPU
SKU
商品分类
品牌
商品属性
商品图片
商品详情
商品上下架
```

## 6.3 典型实体

```text
ProductSpu
ProductSku
ProductCategory
ProductBrand
ProductAttribute
```

## 6.4 多级缓存

商品详情是多级缓存的重点使用场景：

```text
请求
 ↓
Caffeine L1
 ↓ 未命中
Redis L2
 ↓ 未命中
MySQL
 ↓
写入 Redis
 ↓
写入 Caffeine
 ↓
返回
```

## 6.5 重点技术

- Caffeine + Redis 多级缓存
- 热点商品缓存
- 缓存穿透
- 缓存击穿
- 缓存雪崩
- 缓存预热

> 注意：秒杀库存不要放到 Caffeine 中承担核心扣减职责，秒杀库存仍以 Redis 原子操作为主。

---

# 7. mall-inventory —— 库存中心模块

## 7.1 模块定位

把库存从商品模块中独立出来，因为库存会直接参与订单、秒杀和高并发场景。

## 7.2 主要职责

```text
可用库存
锁定库存
扣减库存
释放库存
库存流水
```

## 7.3 核心接口示例

```text
lockStock()
deductStock()
releaseStock()
queryAvailableStock()
```

## 7.4 重点技术

- 数据库库存控制
- Redis 预扣库存
- Lua 原子扣减
- 库存幂等
- 库存回滚
- 最终一致性

## 7.5 设计目标

让：

```text
mall-order
mall-seckill
```

都可以通过统一的库存能力操作库存，而不是自己直接修改库存表。

---

# 8. mall-cart —— 购物车模块

## 8.1 模块定位

负责用户购物车相关业务。

## 8.2 主要职责

```text
加入购物车
删除商品
修改数量
勾选/取消勾选
购物车查询
购物车结算
```

## 8.3 Redis 使用

购物车适合使用 Redis 保存高频数据，例如：

```text
cart:user:{userId}
```

可以结合：

```text
Hash
List
JSON
```

根据具体设计选择存储结构。

## 8.4 注意

购物车的数据结构应围绕“高频读写”设计，不要每次操作都直接查询 MySQL。

---

# 9. mall-order —— 订单交易模块

## 9.1 模块定位

整个电商平台最核心的交易模块之一。

## 9.2 主要职责

```text
创建订单
订单详情
订单列表
取消订单
订单支付状态
确认收货
订单关闭
订单超时
```

## 9.3 订单状态机

推荐明确设计状态机，而不是大量 `if/else`：

```text
WAIT_PAY
   ↓
PAID
   ↓
WAIT_SHIP
   ↓
SHIPPED
   ↓
COMPLETED
```

异常状态：

```text
CANCELLED
REFUNDING
REFUNDED
```

## 9.4 重点技术

- Spring 事务
- 幂等
- 订单状态机
- 库存一致性
- 优惠券一致性
- RabbitMQ 异步处理
- 超时订单关闭

---

# 10. mall-payment —— 支付模块

## 10.1 模块定位

从订单业务中独立出支付能力。

## 10.2 主要职责

```text
支付单
支付状态
支付回调
退款
支付幂等
```

## 10.3 支付实现建议

第一阶段可以使用：

```text
MockPayment
```

后续可以扩展：

```text
Alipay
WechatPay
```

统一抽象：

```text
PaymentService
```

## 10.4 重点

支付回调一定要考虑幂等，例如同一个回调通知多次时不能重复处理订单。

---

# 11. mall-coupon —— 优惠券/营销模块

## 11.1 模块定位

负责营销优惠能力。

## 11.2 主要职责

```text
优惠券模板
优惠券创建
优惠券领取
用户优惠券
优惠券使用
优惠券过期
优惠券回滚
```

## 11.3 高并发场景

例如：

```text
优惠券库存：10,000
请求：100,000
```

可以设计：

```text
Redis
  ↓
Lua原子扣减
  ↓
RabbitMQ
  ↓
MySQL
```

同时在数据库建立：

```text
UNIQUE(user_id, coupon_id)
```

防止重复领取。

## 11.4 重点技术

- Redis
- Lua
- RabbitMQ
- 幂等
- 数据库唯一索引

---

# 12. mall-seckill —— 秒杀模块

## 12.1 模块定位

这是整个项目的**高并发核心模块**。

## 12.2 主要职责

```text
秒杀活动
秒杀商品
库存预热
秒杀接口
用户限购
重复提交控制
秒杀订单
```

## 12.3 推荐请求链路

```text
用户
 ↓
Nginx
 ↓
接口限流
 ↓
SeckillController
 ↓
SeckillService
 ↓
用户限购检查
 ↓
Redis Lua
 ↓
Redis预扣库存
 ↓
RabbitMQ
 ↓
异步创建订单
 ↓
InventoryService
 ↓
MySQL
```

## 12.4 重点技术

- Redis
- Lua
- Redisson/分布式锁
- RabbitMQ
- 限流
- 防刷
- 幂等
- 库存一致性

## 12.5 面试核心

这一模块以后要重点回答：

- 为什么不能直接扣 MySQL 库存？
- 为什么使用 Redis？
- Lua 为什么能解决并发扣减问题？
- 为什么需要 MQ？
- 如何防止超卖？
- 如何防止一个用户重复购买？
- Redis 扣成功但 MQ 失败怎么办？
- 消息重复消费怎么办？

---

# 13. mall-search —— 搜索模块

## 13.1 模块定位

负责电商商品搜索能力。

## 13.2 主要职责

```text
商品索引
关键词搜索
分类筛选
品牌筛选
价格区间
排序
高亮
搜索建议
```

## 13.3 Elasticsearch 数据同步

推荐：

```text
商品变更
 ↓
RabbitMQ
 ↓
Search Consumer
 ↓
Elasticsearch
```

而不是业务代码每次直接强耦合写 ES。

## 13.4 数据职责

```text
MySQL：商品真实数据
ES：搜索索引
```

ES 不作为商品主数据库。

---

# 14. mall-after-sale —— 售后模块

## 14.1 模块定位

处理退款、退货和换货等售后流程。

## 14.2 主要职责

```text
售后申请
退款
退货
换货
售后审核
售后状态管理
```

## 14.3 状态示例

```text
WAIT_AUDIT
APPROVED
REJECTED
REFUNDING
COMPLETED
```

## 14.4 后续 AI 使用

这个模块非常适合给 AI Agent 提供工具：

```text
getOrder()
getAfterSalePolicy()
createAfterSale()
getAfterSaleStatus()
```

实现：

> 用户通过自然语言发起售后业务。

---

# 15. mall-admin —— 后台管理与运营模块

## 15.1 模块定位

负责运营后台入口和管理员能力，而不是把所有业务重新复制一套。

## 15.2 主要职责

```text
后台首页
运营数据
用户管理
权限管理
审核
系统配置
营销配置
```

## 15.3 业务边界

例如商品管理仍然属于：

```text
mall-product
```

订单管理仍然属于：

```text
mall-order
```

`mall-admin` 更像是：

> **后台管理入口 + 运营层 + 管理视角的业务组合。**

## 15.4 数据统计

可以展示：

```text
今日订单
GMV
销售额
订单数量
用户数量
热销商品
转化率
```

结合 ECharts 实现可视化。

---

# 16. mall-ai —— AI 接入模块

## 16.1 模块定位

`mall-ai` 不直接实现 LangChain，而是作为 **Java 侧 AI 接入层**。

## 16.2 Java 侧职责

```text
AI请求入口
用户身份校验
AI会话
SSE流式返回
Agent服务鉴权
调用Python AI服务
```

## 16.3 Python Agent 服务

独立项目：

```text
mallx-ai-service
```

负责：

```text
LangChain
LangGraph
RAG
Tool Calling
Prompt
Memory
LLM
```

## 16.4 架构

```text
Vue3
 ↓
Spring Boot
 ↓
mall-ai
 ↓ HTTP / SSE
Python Agent Service
 ↓
LangChain / LangGraph
 ↓
LLM
```

## 16.5 Agent 可以调用的业务工具

### 商品工具

```text
searchProduct()
getProductDetail()
compareProduct()
```

### 订单工具

```text
getOrder()
getOrderStatus()
getLogistics()
```

### 售后工具

```text
getAfterSalePolicy()
createAfterSale()
getAfterSaleStatus()
```

### 运营工具

```text
getSalesStatistics()
getHotProducts()
getCategoryStatistics()
```

这样 Agent 才是真正连接业务系统，而不是单纯聊天。

---

# 17. 模块之间如何通信

## 17.1 模块化单体阶段

优先使用：

> **Service 直接调用**

例如订单查询商品：

```text
mall-order
    ↓
ProductService
    ↓
mall-product
    ↓
ProductMapper
```

例如：

```java
@Resource
private ProductService productService;

ProductSku sku = productService.getSku(skuId);
```

不建议为了“看起来像微服务”而让单体模块之间使用 HTTP 调自己。

---

## 17.2 异步场景

适合使用 RabbitMQ：

```text
mall-order
    ↓
RabbitMQ
    ↓
mall-inventory
```

例如：

```text
订单创建
 ↓
RabbitMQ
 ├── 库存处理
 ├── 积分处理
 ├── 搜索同步
 └── 通知处理
```

---

## 17.3 未来拆成微服务

当模块真正拆成独立服务之后，再考虑：

```text
HTTP / REST
OpenFeign / RestClient
RPC
```

例如：

```text
mall-order-service
       ↓ HTTP/RPC
mall-product-service
```

所以整个项目的演进路线是：

```text
模块化单体
    ↓
业务边界清晰
    ↓
完成压测与稳定性验证
    ↓
挑选核心模块拆分
    ↓
微服务
```

---

# 18. 模块依赖关系

推荐大体关系：

```text
mall-common
    ↑
    │
所有业务模块
```

核心业务关系：

```text
mall-product
      ↓
mall-inventory

mall-order
  ↓      ↓      ↓
product inventory payment

mall-seckill
  ↓
inventory
  ↓
order

mall-coupon
  ↓
order

mall-search
  ↓
product
```

AI：

```text
mall-ai
   ↓
Python Agent
   ↓
业务 Tool
   ↓
product / order / after-sale / admin
```

## 18.1 依赖设计原则

必须避免：

```text
A → B
B → A
```

也就是尽量避免业务模块之间双向强依赖。

可以通过以下方式解耦：

```text
接口抽象
事件
RabbitMQ
应用层编排
```

---

# 19. 每个业务模块统一代码结构

以 `mall-product` 为例：

```text
mall-product
└── src/main/java/com/mallx/product
    ├── controller
    ├── service
    │   └── impl
    ├── mapper
    ├── entity
    ├── dto
    ├── vo
    ├── converter
    ├── enums
    └── config
```

职责：

### controller

只负责：

```text
接收请求
参数校验
调用 Service
返回结果
```

### service

负责：

```text
核心业务逻辑
事务
缓存
MQ
模块编排
```

### mapper

只负责数据库访问。

### entity

数据库实体。

### dto

接收前端请求的数据结构。

### vo

返回给前端的数据结构。

### converter

负责 DTO/Entity/VO 转换。

---

# 20. 多级缓存应该归属哪里

## 公共缓存能力

放在：

```text
mall-common/cache
```

负责：

```text
Caffeine
Redis
多级缓存模板
缓存序列化
```

## 业务缓存数据

放在业务模块。

例如：

```text
mall-product
 └── ProductCacheService
```

负责：

```text
product:{id}
product:detail:{id}
```

这样形成：

> **缓存框架属于 common，缓存数据属于业务模块。**

---

# 21. RabbitMQ 应该怎么放

RabbitMQ 基础配置放在：

```text
mall-common/mq
```

具体消息生产和消费放在业务模块。

例如：

```text
mall-seckill
├── SeckillProducer
└── SeckillConsumer
```

```text
mall-order
├── OrderProducer
└── OrderConsumer
```

```text
mall-search
└── ProductIndexConsumer
```

这样可以避免出现一个“大而全”的 MQ 模块。

---

# 22. 推荐的最终目录结构

```text
mallx-parent
│
├── mall-common
│
├── mall-auth
│
├── mall-user
│
├── mall-product
│
├── mall-inventory
│
├── mall-cart
│
├── mall-order
│
├── mall-payment
│
├── mall-coupon
│
├── mall-seckill
│
├── mall-search
│
├── mall-after-sale
├── mall-admin
└── mall-ai
```

前端：

```text
mallx-ui
├── router
├── store
├── views
├── components
├── api
└── utils
```

Python：

```text
mallx-ai-service
├── api
├── agents
├── tools
├── rag
├── workflows
├── prompts
└── services
```

---

# 23. 当前阶段不要过度拆分

本项目现阶段应该坚持：

> **模块化单体，不要一开始就微服务化。**

不推荐一开始就搭建：

```text
Nacos
Gateway
OpenFeign
Sentinel
Seata
Kubernetes
```

原因：

```text
技术栈过重
学习成本增加
排查问题困难
项目容易变成CRUD拼装
```

先把：

```text
商品
订单
库存
优惠券
秒杀
搜索
```

做好，并真正解决：

```text
缓存
高并发
幂等
消息可靠性
库存一致性
```

再考虑拆微服务。

---

# 24. 后期可以拆哪些服务

当模块化单体成熟后，优先考虑拆：

```text
mall-seckill
mall-inventory
mall-order
mall-search
```

原因：

- 秒杀：高并发、访问峰值明显
- 库存：强一致要求明显
- 订单：核心交易域
- 搜索：天然适合独立扩展

最终可以演进为：

```text
mall-gateway
     │
 ┌───┼───────────────┐
 ↓   ↓               ↓
商品 用户            订单
                     │
              ┌──────┴──────┐
              ↓             ↓
           库存服务       支付服务
              │
            秒杀服务

         搜索服务

         Python AI Service
```

---

# 25. 最终设计原则

整个 MallX 的后端模块设计遵循以下原则：

1. **业务边界清晰**：一个模块负责一个明确业务域。
2. **公共能力下沉**：Redis、Caffeine、RabbitMQ、OSS、统一异常等放在 `mall-common`。
3. **业务数据归属明确**：商品数据属于 product，库存数据属于 inventory，订单数据属于 order。
4. **模块化单体优先**：先直接 Service 调用，避免过早引入微服务通信复杂度。
5. **异步场景使用 MQ**：适合解耦、削峰、最终一致性的业务采用 RabbitMQ。
6. **AI 独立服务**：Java 负责业务和接入层，Python 负责 LangChain/LangGraph/Agent。
7. **为未来拆分预留边界**：当前模块划分直接按照未来微服务边界设计。

---

# 26. 面试时可以这样介绍

> MallX 采用模块化单体架构，按照用户、商品、库存、订单、支付、营销、秒杀、搜索、售后等业务域进行模块划分。公共基础能力统一沉淀到 mall-common，使用 Caffeine + Redis 构建多级缓存，RabbitMQ 负责异步解耦与削峰。当前通过模块间 Service 调用保持低复杂度，同时保留清晰的领域边界，后续可以将秒杀、库存、订单、搜索等模块独立拆分为微服务。AI 部分通过 Java AI 接入层连接独立的 Python Agent 服务，实现 RAG、Tool Calling 和业务 Agent。

