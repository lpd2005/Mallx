# MallX 秒杀模块设计与实现说明

## 1. 模块职责

`mall-seckill` 负责秒杀活动配置、库存预留、Redis 库存预热、高并发资格抢占、RabbitMQ 异步下单、结果查询、消息恢复与失败补偿。正式订单、支付和取消仍由 `mall-order`、`mall-inventory` 等原有模块负责。

秒杀入口不使用 Caffeine 和 JetCache。秒杀库存是频繁变化的热点数据，必须由 Redis Lua 直接做原子判断和扣减；普通商品详情仍继续使用 JetCache 多级缓存。

## 2. 核心流程

1. 管理员创建草稿活动并配置 SKU、秒杀价、资格库存和每人限购数。
2. 管理员预热活动，Redisson 活动锁防止多实例重复预热；MySQL 库存从 `available_stock` 转入 `locked_stock`，随后活动元数据和资格库存写入 Redis。
3. 用户提交秒杀请求，Lua 在 Redis 内一次完成活动时间、状态、限购、库存和请求记录校验。
4. Lua 成功后返回请求号，服务将请求投递到 RabbitMQ，客户端通过请求号轮询结果。
5. 消费者在数据库事务中幂等创建秒杀订单。库存已在预热阶段锁定，此时不能再次调用 `lockStock`。
6. 订单成功后将 Redis 请求状态改为成功；商品下架、地址失效等确定性业务失败时，Lua 幂等归还资格库存和用户限购计数。
7. 系统异常由 RabbitMQ 重试、死信队列和 Redis 待处理集合共同兜底。系统异常无法证明订单一定未创建，因此不会贸然补偿库存。
8. 活动到期或人工关闭后先停止新请求；待该商品的处理中请求归零，再把未形成订单的余额从 `locked_stock` 释放回 `available_stock`。

## 3. 一致性与并发策略

- 热点资格库存：Redis Lua 原子扣减，不使用 `synchronized`，支持多实例部署。
- 库存隔离：预热调用 `InventoryStockService.lockStock`，使普通订单只能消费扣除秒杀预留量后的 `available_stock`。
- 防止重复锁定：预热业务号固定为 `SECKILL-PREPARE-{itemId}`，库存流水唯一键保证重复预热幂等。
- 秒杀订单库存：创建订单不再锁库存；支付把 `locked_stock` 转为 `sold_stock`，取消把它释放回 `available_stock`。
- 关闭释放：Redis 每个商品维护待处理计数；计数归零后使用 `SECKILL-CLOSE-{itemId}` 幂等释放 `total_stock - sold_stock`。
- 消息幂等：秒杀请求号唯一，正式订单号固定为 `SK + requestNo`。
- Redis 补偿：只有状态仍为“处理中”时才能执行一次，重复消息不会重复加库存。
- MQ 丢失恢复：成功预扣的请求同时进入 Redis ZSet；定时任务在 Redisson 锁保护下重新投递超时请求。
- Redis 重建：库存 Key 丢失后按 `total_stock - sold_stock` 恢复，不会恢复成活动初始库存。

## 4. Redis Key

- `mallx:seckill:v1:item:{itemId}`：活动时间、SKU、价格、状态和限购元数据。
- `mallx:seckill:v1:stock:{itemId}`：秒杀资格剩余库存。
- `mallx:seckill:v1:purchased:{itemId}`：用户累计购买数量。
- `mallx:seckill:v1:pending-count:{itemId}`：该商品尚未形成成功或失败终态的请求数量。
- `mallx:seckill:v1:request:{requestNo}`：异步请求状态和下单参数。
- `mallx:seckill:v1:pending-requests`：需要恢复投递的请求 ZSet。

## 5. RabbitMQ

- 业务交换机：`mallx.seckill.order.exchange`
- 业务队列：`mallx.seckill.order.queue`
- 死信交换机：`mallx.seckill.order.dlx`
- 死信队列：`mallx.seckill.order.dlq`
- 公共配置：`mall-common` 只提供 RabbitMQ JSON 消息转换器，不包含秒杀业务回调。
- 发布确认：秒杀发送端以 `requestNo + UUID` 作为单次发布的 `CorrelationData`，通过该消息自己的 Future 处理 Confirm。
- 路由失败：开启 mandatory 和 Publisher Return；Confirm 完成时通过 `CorrelationData.getReturned()` 判断该次消息是否无法路由。

Confirm ACK 只代表交换机已接收消息；消息是否成功路由还要结合是否触发 Return 判断。两者都不代表订单已经创建，因此不能删除 Redis 待处理记录。NACK、Return 或确认异常同样保留待处理状态，由恢复任务重新投递。消费者对确定性业务异常直接失败并补偿；内部系统异常重新抛出，由 Spring AMQP 执行有限次数重试。进入死信队列后仍保留 Redis 待处理状态，由恢复任务继续投递。

秒杀 MQ 代码按职责拆分：`message` 只保存消息体，`publisher` 负责投递及发布结果，`listener` 负责业务队列和死信队列消费，`task` 负责待处理请求重投及关闭活动库存释放。

## 6. 接口

前台：

- `GET /seckill/activities`：游客查看未结束且已预热的活动。
- `POST /seckill/items/{itemId}/purchase`：登录用户提交秒杀请求。
- `GET /seckill/requests/{requestNo}`：当前用户查询异步结果。

后台（需要 `seckill:manage`）：

- `POST /admin/seckill-activities`
- `PUT /admin/seckill-activities/{activityId}`
- `POST /admin/seckill-activities/{activityId}/items`
- `PUT /admin/seckill-activities/{activityId}/items/{itemId}`
- `DELETE /admin/seckill-activities/{activityId}/items/{itemId}`
- `POST /admin/seckill-activities/{activityId}/prepare`
- `POST /admin/seckill-activities/{activityId}/close`
- `GET /admin/seckill-activities`
- `GET /admin/seckill-activities/{activityId}`

## 7. 初始化

1. 执行 `mall-seckill/src/main/resources/db/schema-seckill.sql`。
2. 重新执行 `mall-admin/src/main/resources/db/data-admin-rbac.sql`，补充 `seckill:manage` 权限。
3. 确认 MySQL、Redis、RabbitMQ 配置可用。
4. 先配置并预热活动，再开放前台秒杀入口。
