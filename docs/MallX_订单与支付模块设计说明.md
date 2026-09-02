# MallX 订单与支付模块设计说明

> 本文以当前 `mall-order` 与 `mall-payment` 代码为准，说明普通订单和模拟支付的业务边界、数据结构、状态流转、事务设计与后续演进方向。

## 1. 模块定位与边界

`mall-order` 负责把购物车中的购买意向转换为不可随商品改价而变化的交易事实，主要职责包括：

```text
创建订单
保存商品和收货地址快照
计算会员与优惠券优惠
锁定、扣减和释放库存
订单查询与取消
订单备货、发货和确认收货
超时关闭待支付订单
记录订单状态流水
```

`mall-payment` 负责独立支付单和支付状态处理，当前阶段实现：

```text
创建模拟支付单
查询支付单
模拟支付
支付请求幂等
支付成功后通知订单模块
同步关闭不可继续支付的支付单
```

模块之间的调用关系是：

```text
mall-user       ─┐
mall-cart        ├──→ mall-order
mall-coupon      │          ↑
mall-inventory  ─┘          │ 支付成功时调用订单内部Service
                    mall-payment
```

实际 Maven 依赖方向为：

```text
mall-order   → mall-user、mall-cart、mall-coupon、mall-inventory
mall-payment → mall-order
```

`mall-order` 不反向依赖 `mall-payment`，避免订单与支付形成循环依赖。支付成功由支付模块调用订单内部 Service 完成订单确认。

## 2. 当前版本业务规则

- 当前只开发普通订单，不包含秒杀订单。
- 订单从购物车已勾选且当前可购买的 SKU 创建。
- 前端只提交收货地址ID、可选用户优惠券ID和订单备注，不提交可信价格。
- 创建订单时重新读取商品当前价格、会员权益、优惠券状态和库存。
- 会员折扣先计算，优惠券再对会员折扣后的金额计算。
- 一个订单最多使用一张优惠券。
- 一个订单当前只创建一个支付单。
- 待支付订单有效期为30分钟。
- 当前只支持 Mock 模拟支付渠道。
- 订单和支付状态均以 `TINYINT` 保存，Java枚举数值与数据库注释保持一致。
- 模块不使用逻辑删除。

## 3. 数据库模型

### 3.1 订单主表 `mall_order`

订单主表保存订单归属、金额、当前状态和交易快照。

| 字段组 | 主要字段 | 含义 |
|---|---|---|
| 订单标识 | `id`、`order_no`、`user_id` | 订单主键、订单号和下单用户 |
| 状态 | `status` | 订单当前状态 |
| 商品金额 | `total_quantity`、`product_amount` | 商品总件数和原价合计 |
| 优惠金额 | `member_discount_amount`、`coupon_discount_amount` | 会员和优惠券优惠金额 |
| 实付金额 | `pay_amount` | 最终应付金额 |
| 优惠快照 | `coupon_user_id`、`member_level_code`、`member_discount_rate` | 使用的用户券及下单时会员权益 |
| 地址快照 | `recipient_name` 到 `postal_code` | 下单时收货地址完整快照 |
| 生命周期 | `expire_at`、`paid_at`、`shipped_at`、`completed_at`、`canceled_at` | 订单各阶段时间 |

订单号有唯一索引：

```text
UNIQUE(order_no)
```

用户订单查询使用 `(user_id, created_at)` 索引，超时任务使用 `(status, expire_at)` 索引。

### 3.2 订单明细表 `order_item`

订单明细不是商品表的实时引用，而是下单时的成交快照：

| 字段 | 含义 |
|---|---|
| `order_id`、`order_no` | 所属订单 |
| `spu_id`、`sku_id` | 原商品标识 |
| `product_name`、`sku_name` | 下单时商品和SKU名称 |
| `spec_json` | 下单时销售规格JSON |
| `image_oss_key` | 下单时商品图片OSS对象Key |
| `unit_price` | 商品成交单价 |
| `quantity` | 购买数量 |
| `subtotal` | 明细成交小计 |

订单生成后，即使商品改名、改图、改规格或改价，历史订单仍展示原成交信息。

### 3.3 订单操作流水 `order_operation_log`

订单每次状态变化都记录：

- 操作前状态。
- 操作后状态。
- 操作类型。
- 操作人ID。
- 操作说明和时间。

创建订单时没有操作前状态；系统超时任务执行时没有操作人ID。

### 3.4 支付记录表 `payment_record`

支付记录与订单分表保存，避免把支付渠道状态直接混入订单主表。

| 字段 | 含义 |
|---|---|
| `payment_no` | 支付单号 |
| `order_no` | 关联订单号 |
| `user_id` | 支付用户ID |
| `amount` | 创建支付单时的应付金额快照 |
| `channel` | 支付渠道，当前固定为模拟支付 |
| `status` | 支付单状态 |
| `paid_request_no` | 一次支付动作的幂等请求号 |
| `paid_at`、`closed_at`、`refunded_at` | 支付生命周期时间 |

关键唯一索引：

```text
UNIQUE(payment_no)
UNIQUE(order_no)
UNIQUE(paid_request_no)
```

`UNIQUE(order_no)` 保证一个订单当前只能创建一个支付单；`UNIQUE(paid_request_no)` 防止同一个支付请求号用于多次支付。

## 4. 状态枚举与流转

### 4.1 订单状态

| 值 | Java枚举 | 含义 |
|---:|---|---|
| 1 | `WAIT_PAY` | 待支付，库存已锁定 |
| 2 | `PAID` | 已支付，锁定库存已转为已售库存 |
| 3 | `WAIT_SHIP` | 待发货 |
| 4 | `SHIPPED` | 已发货 |
| 5 | `COMPLETED` | 已完成 |
| 6 | `CANCELED` | 已取消或超时关闭 |

正常流转：

```text
WAIT_PAY
   ↓ 支付成功
PAID
   ↓ 后台备货
WAIT_SHIP
   ↓ 后台发货
SHIPPED
   ↓ 用户确认收货
COMPLETED
```

待支付阶段的异常流转：

```text
WAIT_PAY
   ├── 用户取消 ──────→ CANCELED
   └── 超过支付期限 ──→ CANCELED
```

当前不允许已支付订单通过普通取消接口直接取消。已支付后的退款或售后应由后续 `mall-after-sale` 与退款流程处理。

### 4.2 订单操作类型

| 值 | Java枚举 | 操作 |
|---:|---|---|
| 1 | `CREATE` | 创建订单 |
| 2 | `PAY` | 支付成功 |
| 3 | `PREPARE_SHIPMENT` | 进入待发货 |
| 4 | `SHIP` | 发货 |
| 5 | `RECEIVE` | 确认收货 |
| 6 | `CANCEL` | 用户取消 |
| 7 | `TIMEOUT_CLOSE` | 超时关闭 |

### 4.3 支付状态

| 值 | Java枚举 | 含义 |
|---:|---|---|
| 1 | `PENDING` | 待支付 |
| 2 | `PAID` | 已支付 |
| 3 | `CLOSED` | 已关闭，不能继续支付 |
| 4 | `REFUNDED` | 已退款，当前只预留状态 |

当前主要流转：

```text
PENDING ──模拟支付成功──→ PAID
PENDING ──订单取消或过期→ CLOSED
```

`REFUNDED` 已在枚举和数据库中预留，但当前版本尚未实现退款方法。

## 5. 订单金额计算

订单金额全部使用 `BigDecimal`，金额统一保留两位小数。

计算顺序：

```text
productAmount = 购物车当前商品原价合计

memberAmount = productAmount × memberDiscountRate
memberDiscountAmount = productAmount - memberAmount

couponDiscountAmount = 优惠券对 memberAmount 计算出的优惠

payAmount = max(memberAmount - couponDiscountAmount, 0)
```

例如商品原价100元，普通会员95折，再使用满90减10优惠券：

```text
商品原价           = 100.00
会员折扣后金额     = 100.00 × 0.9500 = 95.00
会员优惠金额       = 5.00
优惠券优惠金额     = 10.00
最终应付金额       = 85.00
```

前端传入的商品价格、会员折扣、优惠金额和应付金额都不能作为订单事实。订单必须使用后端模块重新查询的结果。

## 6. 创建订单流程

创建订单在一个数据库事务中完成：

```text
CurrentUserProvider获取当前用户ID
  ↓
校验收货地址属于当前用户
  ↓
CartService重新读取已勾选商品并校验商品、价格和库存
  ↓
查询当前会员权益并计算会员优惠
  ↓
可选：锁定优惠券并计算优惠券优惠
  ↓
按skuId升序锁定每个SKU库存
  ↓
保存订单主表和订单明细快照
  ↓
确认优惠券已使用
  ↓
保存订单创建流水
  ↓
事务提交
  ↓
清理Redis中本次购买的购物车项
```

任何数据库步骤失败时，Spring事务会回滚订单、订单明细、优惠券状态和库存操作。

### 6.1 为什么按SKU ID排序锁库存

一个订单可能包含多个SKU。并发订单如果使用不同顺序锁定相同的一组SKU，更容易形成循环等待。

当前先按 `skuId` 升序排序，再依次调用库存锁定，使不同请求尽量使用一致的加锁顺序，降低数据库死锁概率。

### 6.2 为什么事务提交后才清理购物车

Redis购物车不属于MySQL本地事务。如果先删除购物车再保存订单，订单创建失败时用户的购物车内容已经丢失。

当前通过事务同步回调在订单提交成功后清理：

```text
MySQL订单事务成功
  ↓
afterCommit回调
  ↓
删除Redis中已购买的SKU
```

如果订单已经提交但Redis清理失败，系统记录错误日志，不回滚已经成功创建的订单。此时可能暂时残留购物车项，后续可以通过重试或事件机制增强最终一致性。

## 7. 库存与优惠券联动

### 7.1 创建待支付订单

```text
库存：availableStock减少，lockedStock增加
优惠券：锁定后确认使用
订单：WAIT_PAY
```

### 7.2 支付成功

```text
库存：lockedStock减少，soldStock增加
订单：WAIT_PAY → PAID
支付单：PENDING → PAID
```

### 7.3 用户取消或超时关闭

```text
库存：lockedStock减少，availableStock恢复
优惠券：仍有效则恢复UNUSED，已过期则转EXPIRED
订单：WAIT_PAY → CANCELED
```

库存模块自身通过原子条件更新防止负库存，并通过库存操作流水保证同一订单、SKU和操作类型的幂等性。

## 8. 支付单创建与模拟支付

### 8.1 创建支付单

支付模块先从订单模块读取只包含支付必要字段的订单信息：

```text
orderNo
userId
status
payAmount
expireAt
```

只有属于当前用户、仍处于 `WAIT_PAY` 且未过期的订单才能创建支付单。支付金额从订单读取，不接受前端传入。

如果同一订单重复请求创建支付单：

- 已存在支付单时直接返回原支付单。
- 并发插入时由 `UNIQUE(order_no)` 兜底，避免生成多条支付记录。

### 8.2 模拟支付流程

```text
校验当前用户和requestNo
  ↓
SELECT payment_record ... FOR UPDATE
  ↓
已支付则直接返回原结果
  ↓
检查支付单必须为PENDING
  ↓
重新检查订单状态和支付期限
  ├── 订单已取消或过期：关闭支付单
  └── 订单可支付：继续
  ↓
订单行锁定并确认支付
  ↓
将订单锁定库存转为已售库存
  ↓
订单更新为PAID并写订单流水
  ↓
支付单更新为PAID并记录requestNo、paidAt
  ↓
同一数据库事务提交
```

支付单与订单确认位于同一个本地数据库事务，因此不会出现“支付单已成功但订单仍待支付”这种由本次程序事务局部提交造成的不一致。

## 9. 幂等与并发安全

### 9.1 支付单行锁

模拟支付先按支付单号执行：

```sql
SELECT *
FROM payment_record
WHERE payment_no = :paymentNo
FOR UPDATE;
```

同一支付单的并发支付请求会串行处理。第一个请求完成后，后续请求重新读取到 `PAID`，直接返回支付成功结果，不重复修改订单和库存。

### 9.2 订单行锁

取消、支付、备货、发货、确认收货和超时关闭都需要改变订单状态，因此按订单号执行 `SELECT ... FOR UPDATE`。

行锁保证同一订单的状态判断和状态修改在事务中串行完成。例如支付与取消并发时，只会有一个操作先获得订单行锁，后获得锁的操作会根据最新状态决定返回幂等结果或拒绝操作。

### 9.3 条件更新与唯一索引

当前并发安全由多层机制共同保证：

- 订单和支付单行锁保护状态流转。
- 库存条件更新防止可用库存或锁定库存变成负数。
- 库存操作流水保证扣减、释放幂等。
- 用户优惠券行锁防止同一张券同时用于两个订单。
- 订单号、支付单号和支付请求号唯一索引防止重复记录。
- Spring事务保证跨表修改同时提交或回滚。

普通订单链路当前不需要分布式锁。数据库行锁、条件更新、唯一索引和本地事务已经覆盖本阶段的共享状态竞争。

## 10. 订单超时关闭

订单创建时设置：

```text
expireAt = 创建时间 + 30分钟
```

当前定时任务默认每分钟执行一次：

```text
cron = 0 * * * * *
```

每次最多扫描100个已经过期的 `WAIT_PAY` 订单，逐个获取订单行锁后再次检查状态和过期时间，然后释放库存、回滚优惠券、更新订单状态并保存流水。

定时任务只负责让状态最终收敛。支付操作本身仍会实时检查 `expireAt`，因此即使定时任务尚未执行，过期订单也不能继续支付。

当前没有使用 RabbitMQ 延迟消息。后续可改为：

```text
创建订单后发送延迟关单消息
  ↓
消息到期后重新检查订单状态
  ↓
仍为WAIT_PAY才执行关闭
```

即使接入MQ，数据库状态检查和幂等处理仍然不能删除。

## 11. 订单履约流程

后台履约接口负责：

```text
PAID → WAIT_SHIP
WAIT_SHIP → SHIPPED
```

用户确认收货负责：

```text
SHIPPED → COMPLETED
```

Service会校验期望状态，不能跳过流程直接从已支付变为已发货，也不能对待支付订单确认收货。重复请求到达目标状态时，部分内部流转会按幂等方式返回。

当前 `/admin/orders` 接口只校验登录用户，尚未接入完整RBAC。后续应由 `mall-admin` 限制备货和发货权限。

## 12. HTTP接口

### 12.1 当前用户订单

```http
POST /users/me/orders
GET  /users/me/orders
GET  /users/me/orders?status={status}
GET  /users/me/orders/{orderNo}
POST /users/me/orders/{orderNo}/cancel
POST /users/me/orders/{orderNo}/receipt
```

创建订单请求示例：

```json
{
  "addressId": 10001,
  "couponUserId": 20001,
  "remark": "工作日送达"
}
```

取消订单请求示例：

```json
{
  "reason": "暂时不需要了"
}
```

### 12.2 后台订单履约

```http
POST /admin/orders/{orderNo}/prepare-shipment
POST /admin/orders/{orderNo}/ship
```

### 12.3 当前用户支付

```http
POST /users/me/orders/{orderNo}/payments
GET  /users/me/payments/{paymentNo}
POST /users/me/payments/{paymentNo}/mock-pay
```

模拟支付请求示例：

```json
{
  "requestNo": "CLIENT-PAY-20260825-0001"
}
```

所有用户接口都通过 `CurrentUserProvider` 获取当前登录用户ID，不接受前端传入的 `userId`。订单和支付查询同时校验资源归属，防止用户访问他人的交易记录。

## 13. MyBatis-Plus与SQL边界

订单和支付Service使用完整MyBatis-Plus结构：

```java
public interface OrderService extends IService<MallOrder>

public class OrderServiceImpl
        extends ServiceImpl<MallOrderMapper, MallOrder>
        implements OrderService
```

支付模块使用相同结构。

实现约定：

- 普通查询使用 `lambdaQuery()`。
- 普通保存、批量保存和更新使用Service通用方法。
- Service不使用 `baseMapper` 调用Mapper。
- 需要行锁时显式注入 `MallOrderMapper` 或 `PaymentRecordMapper`。
- 两条 `FOR UPDATE` 都是短小固定SQL，因此直接使用Mapper注解。
- 当前没有复杂联表或动态SQL，所以没有新增Mapper XML。
- Entity依赖项目全局驼峰配置，不重复声明字段映射。
- VO使用 `@Builder` 创建，不设置converter包。

## 14. 数据库脚本

脚本位置：

```text
mall-order/src/main/resources/db/schema-order.sql
mall-payment/src/main/resources/db/schema-payment.sql
```

执行顺序：

```text
1. schema-order.sql
2. schema-payment.sql
```

所有表和字段均已添加中文 `COMMENT`。模块化单体阶段跨模块ID只做业务关联，不建立物理外键，避免以后拆分模块时产生数据库级强耦合。

## 15. 测试覆盖

订单模块测试覆盖：

- 支付成功后将订单更新为已支付。
- 支付成功后把锁定库存转为已售库存。
- 取消待支付订单时释放库存。
- 取消订单时回滚优惠券。

支付模块测试覆盖：

- 从订单应付金额创建支付单快照。
- 模拟支付成功后通知订单模块。
- 重复支付请求返回原支付结果，不重复确认订单。

已经执行：

```text
mvn -pl mall-payment -am test
mvn -pl mallx-application -am test
```

订单模块2个测试、支付模块3个测试通过；全项目16个Maven模块聚合构建成功。当前单元测试没有连接真实MySQL执行DDL，也没有进行HTTP接口、真实Redis和高并发联调。

## 16. 当前实现中的一致性边界

当前项目是模块化单体，订单、库存、优惠券和支付表共用一个MySQL数据源，因此可以使用本地事务保证核心交易一致性。

Redis购物车不属于MySQL事务，采用“订单提交后再清理”的方式降低不一致风险。以后模块拆分为独立服务和独立数据库后，当前本地事务不能直接覆盖所有模块，需要引入可靠消息、事务消息、补偿任务和对账机制。

支付单不会在用户取消订单的同一事务中被订单模块直接关闭，因为订单模块不能反向依赖支付模块。当前支付单在查询或尝试支付时发现订单已取消或过期，会同步转为 `CLOSED`。后续可通过订单取消事件及时关闭支付单。

## 17. 当前阶段未包含

以下能力留到后续阶段：

- 微信支付、支付宝等真实支付渠道。
- 支付参数签名和第三方支付回调验签。
- 第三方支付回调的业务幂等与回调日志。
- 主动查询第三方支付结果和掉单补偿。
- 完整退款、部分退款与退款流水。
- `mall-after-sale` 售后单与支付退款联动。
- RabbitMQ延迟关单和订单事件。
- 支付成功、订单取消和发货事件通知。
- 订单列表分页、后台订单搜索和导出。
- 物流公司、物流单号和物流轨迹。
- 自动确认收货和评价入口。
- 管理端RBAC权限控制。
- 真实MySQL、Redis联调和并发压测。

当前实现完成了普通订单从购物车下单、库存锁定、优惠计算、模拟支付、库存扣减、取消释放、超时关闭到确认收货的基础闭环，并为后续真实支付、售后退款和消息驱动演进保留了清晰边界。
