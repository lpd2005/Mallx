# MallX 优惠券模块设计说明

## 1. 模块定位

`mall-coupon` 负责 MallX 的优惠券营销能力，包括：

```text
优惠券模板配置
优惠券发布和停止
用户领取
用户优惠券查询
优惠金额试算
订单锁券
确认使用
订单取消回滚
优惠券过期
操作流水
```

优惠券模块只负责判断“一张优惠券能否使用以及可以优惠多少钱”，不负责创建订单、计算会员身份、修改商品价格或锁定商品库存。

模块之间的边界是：

```text
mall-user       → 提供会员等级和会员折扣
mall-cart       → 提供用户选中的商品和当前商品原始金额
mall-coupon     → 根据可优惠金额计算优惠券优惠
mall-order      → 组合商品、会员、优惠券、库存并生成最终订单
mall-inventory  → 负责订单库存锁定、扣减和释放
```

## 2. 当前版本业务规则

第一版支持两种全场优惠券：

| 类型值 | 类型 | 示例 |
|---:|---|---|
| 1 | 满减券 | 满100减20 |
| 2 | 折扣券 | 满100打8折，最高优惠50元 |

当前约束：

- 只支持全场券。
- 暂不支持分类券、品牌券、SPU券和SKU券。
- 同一用户对同一优惠券模板最多领取一次。
- 一个订单最多使用一张优惠券。
- 会员折扣和一张优惠券可以叠加。
- 多张优惠券之间不能叠加。
- 模板发布后，优惠规则不能修改。
- 停止发放只阻止新用户继续领取，不影响已经领取的优惠券。
- 模块不使用逻辑删除。

## 3. 数据库模型

### 3.1 优惠券模板 `coupon_template`

优惠券模板保存运营人员定义的发行规则和优惠规则。

| 字段 | 含义 |
|---|---|
| `id` | 优惠券模板ID |
| `coupon_name` | 优惠券名称 |
| `coupon_type` | 1满减券，2折扣券 |
| `threshold_amount` | 最低可优惠金额，0表示无门槛 |
| `discount_amount` | 满减金额，仅满减券使用 |
| `discount_rate` | 折扣率，例如0.8000表示8折 |
| `max_discount_amount` | 折扣券最高优惠金额，可为空 |
| `total_stock` | 总发行数量 |
| `received_stock` | 已领取数量 |
| `claim_start_at` | 领取开始时间 |
| `claim_end_at` | 领取结束时间 |
| `valid_start_at` | 使用开始时间 |
| `valid_end_at` | 使用结束时间 |
| `status` | 模板状态 |

金额统一使用 `DECIMAL`，不使用 `float` 或 `double`。

`total_stock - received_stock` 表示当前剩余可领取数量。`received_stock` 表示已经发放给用户的数量，不表示已经使用的数量。

### 3.2 用户优惠券 `coupon_user`

用户领取成功后生成一条实际持有的优惠券记录。

| 字段 | 含义 |
|---|---|
| `id` | 用户优惠券ID |
| `template_id` | 优惠券模板ID |
| `user_id` | 领取用户ID |
| `status` | 未使用、已锁定、已使用或已过期 |
| `order_no` | 当前绑定订单号 |
| `received_at` | 领取时间 |
| `valid_start_at` | 使用开始时间快照 |
| `valid_end_at` | 使用结束时间快照 |
| `locked_at` | 订单锁券时间 |
| `used_at` | 确认使用时间 |

领取时把模板有效期复制到用户券，避免后续模板状态变化影响已领取优惠券。

两个关键唯一索引：

```text
UNIQUE(user_id, template_id)
UNIQUE(order_no)
```

第一个唯一索引保证同一用户不能重复领取同一模板；第二个唯一索引保证一个订单最多绑定一张优惠券。MySQL允许唯一索引中存在多个 `NULL`，因此未绑定订单的用户券不会互相冲突。

### 3.3 操作流水 `coupon_operation_log`

操作流水记录：

- 领取
- 锁定
- 使用
- 回滚
- 过期

流水中的 `operation_no` 建立唯一索引，用于保证相同业务操作不会产生重复流水，并保留状态变化前后的值用于问题排查。

## 4. 枚举与数据库状态

所有类型和状态都使用 `TINYINT` 存入数据库，Java枚举值与数据库注释保持一致，不直接把枚举名称字符串写入数据库。

### 4.1 模板状态

| 值 | 状态 | 含义 |
|---:|---|---|
| 0 | DRAFT | 草稿，可以修改 |
| 1 | ACTIVE | 已发布，处于发放状态 |
| 2 | STOPPED | 已人工停止发放 |
| 3 | ENDED | 领取时间已经结束 |

模板状态流转：

```text
DRAFT
  ↓ 发布
ACTIVE
  ├── 人工停止 → STOPPED
  └── 领取期结束 → ENDED
```

发布后的模板不能重新修改优惠门槛、满减金额、折扣率、库存和有效期，避免已经领取的优惠券规则发生变化。

### 4.2 用户券状态

| 值 | 状态 | 含义 |
|---:|---|---|
| 1 | UNUSED | 已领取但尚未使用 |
| 2 | LOCKED | 正在被某个订单使用 |
| 3 | USED | 已确认用于订单 |
| 4 | EXPIRED | 已经过期 |

主要状态流转：

```text
领取成功
   ↓
UNUSED
   ↓ 订单提交锁券
LOCKED
   ↓ 订单创建成功
USED
   ↓ 订单取消
UNUSED 或 EXPIRED
```

订单取消时：

- 优惠券仍在有效期内，恢复为 `UNUSED`。
- 优惠券已经超过有效期，转为 `EXPIRED`。

订单取消不会减少模板的 `received_stock`，因为优惠券没有退回公共发行库存，它仍然属于原用户，只是恢复可用或变成过期。

## 5. 模板规则校验

创建和修改草稿时统一校验：

- 优惠券名称不能为空且不能超过100个字符。
- 使用门槛不能小于0。
- 总发行数量必须大于0。
- 领取开始时间必须早于领取结束时间。
- 使用开始时间必须早于使用结束时间。
- 使用结束时间必须晚于领取结束时间。

满减券要求：

```text
discount_amount > 0
discount_rate = NULL
max_discount_amount = NULL
```

折扣券要求：

```text
0 < discount_rate < 1
discount_amount = NULL
max_discount_amount为空或大于0
```

Service负责返回清晰的业务提示，数据库 `CHECK` 约束负责防止异常程序或手工SQL写入非法数据。

## 6. 优惠券领取流程

领取过程在一个 MySQL 事务中完成：

```text
校验用户和模板
  ↓
检查是否已经领取
  ↓
原子增加模板 received_stock
  ↓
插入 coupon_user
  ↓
保存领取流水
  ↓
事务提交
```

模板库存不能使用“先查询剩余数量，再执行更新”的方式，否则多个请求可能同时读取到相同库存并造成超发。

当前使用的核心条件更新等价于：

```sql
UPDATE coupon_template
SET received_stock = received_stock + 1
WHERE id = :templateId
  AND status = 1
  AND claim_start_at <= :now
  AND claim_end_at > :now
  AND received_stock < total_stock;
```

更新行数为1表示占用成功，更新行数为0表示模板状态、领取时间或发行库存不满足领取条件。

### 6.1 同一用户并发重复领取

假设同一用户同时发送两个领取请求：

```text
请求A：原子增加库存 → 插入用户券成功
请求B：原子增加库存 → 用户券唯一索引冲突 → 整个事务回滚
```

请求B事务回滚时，它增加的 `received_stock` 也会一起回滚，因此不会因为重复领取造成库存永久少一张。

### 6.2 为什么当前不需要分布式锁

当前普通领取链路的并发保护由以下能力完成：

- MySQL原子条件更新防止超发。
- `(user_id, template_id)` 唯一索引防止重复领取。
- Spring事务保证模板库存、用户券和操作流水同时成功或同时失败。

增加分布式锁不会替代上述数据库约束，反而会增加获取、续期、释放和异常恢复成本。

## 7. 优惠金额计算

优惠券模块接收一个 `eligibleAmount`，表示本次允许参与优惠券计算的金额。

### 7.1 满减券

满100减20，当前可优惠金额为150元：

```text
优惠金额 = 20.00
优惠后金额 = 150.00 - 20.00 = 130.00
```

### 7.2 折扣券

8折券，当前可优惠金额为200元：

```text
优惠金额 = 200.00 × (1 - 0.8000) = 40.00
优惠后金额 = 160.00
```

如果最高优惠金额为30元：

```text
实际优惠金额 = min(40.00, 30.00) = 30.00
优惠后金额 = 170.00
```

计算规则：

- 金额使用 `BigDecimal`。
- 金额统一保留两位小数。
- 未达到门槛时拒绝使用。
- 优惠金额不能超过当前可优惠金额。
- 优惠后金额不能小于0。

## 8. 会员折扣与优惠券组合

优惠券模块不直接依赖 `mall-user`，会员和优惠券的组合由后续 `mall-order` 负责。

推荐计算顺序：

```text
商品当前总金额
  ↓
应用会员折扣
  ↓
得到 eligibleAmount
  ↓
调用优惠券计算
  ↓
得到优惠券优惠金额和订单应付金额
```

例如商品金额100元，普通会员95折，再使用满90减10优惠券：

```text
会员折扣后金额 = 100.00 × 0.9500 = 95.00
优惠券优惠金额 = 10.00
最终应付金额 = 85.00
```

购物车结算预览当前仍只返回商品原始金额，不直接执行会员和优惠券最终计算。订单创建时必须重新读取商品价格、会员权益和优惠券状态，不能相信前端提交的金额。

## 9. 订单接入接口

优惠券模块为订单模块提供以下内部 Service 方法：

```java
listUsableCoupons(userId, eligibleAmount)
calculateDiscount(userId, couponUserId, eligibleAmount)
lockForOrder(userId, couponUserId, orderNo, eligibleAmount)
confirmUsed(couponUserId, orderNo)
rollbackForOrder(couponUserId, orderNo)
```

推荐订单创建流程：

```text
重新查询商品和会员信息
  ↓
计算 eligibleAmount
  ↓
lockForOrder() 锁定优惠券
  ↓
锁定商品库存
  ↓
保存订单和订单明细
  ↓
confirmUsed() 确认优惠券使用
  ↓
事务提交
```

同一张用户券的锁定、确认使用和回滚会先执行 `SELECT ... FOR UPDATE`，串行处理该用户券的状态变化，防止同一张券同时用于两个订单。

订单取消时需要同时处理：

```text
释放商品库存
回滚优惠券
修改订单状态
```

这些内部方法不直接暴露给前端，避免用户伪造订单号锁定、使用或回滚优惠券。

## 10. 过期处理

定时任务默认每五分钟处理：

- 领取期已经结束的 `ACTIVE` 模板转为 `ENDED`。
- 已超过有效期的 `UNUSED` 用户券转为 `EXPIRED`。

每批最多处理500条，避免单次任务长时间占用数据库。

定时任务只负责让数据库状态最终收敛，不承担实时正确性。以下业务仍会检查当前时间：

- 优惠券领取
- 用户券查询
- 可用优惠券查询
- 优惠金额试算
- 订单锁券
- 订单取消回滚

因此即使定时任务延迟，过期优惠券也不能继续用于新订单。

## 11. HTTP接口

### 11.1 优惠券中心

```http
GET  /coupons
POST /coupons/{templateId}/claim
```

### 11.2 当前用户优惠券

```http
GET /users/me/coupons
GET /users/me/coupons?status={status}
GET /users/me/coupons/usable?amount={amount}
GET /users/me/coupons/{couponUserId}/calculation?amount={amount}
```

Controller通过 `CurrentUserProvider` 获取当前登录用户ID，不接受前端提交的用户ID。

### 11.3 模板管理

```http
POST /admin/coupon-templates
PUT  /admin/coupon-templates/{templateId}
POST /admin/coupon-templates/{templateId}/publish
POST /admin/coupon-templates/{templateId}/stop
GET  /admin/coupon-templates
```

模板管理 Controller 已迁入 `mall-admin`，路径保持 `/admin/coupon-templates`，统一要求 `admin:access` 与 `coupon:manage` 权限。业务规则仍在本模块 Service 中。

## 12. MyBatis-Plus与SQL边界

三个持久化Service都使用完整MyBatis-Plus结构：

```java
IService<Entity>
ServiceImpl<Mapper, Entity>
```

实现规则：

- 普通保存、按ID查询和更新使用Service通用方法。
- 普通条件查询和更新使用lambda。
- 不在Service中使用 `baseMapper` 调用Mapper。
- 自定义SQL通过显式注入的Mapper调用。
- 原子领取库存和单行 `FOR UPDATE` 都是短小固定SQL，使用Mapper注解。
- 当前没有复杂联表或动态SQL，因此没有创建Mapper XML。
- VO使用 `@Builder` 创建。
- 模板DTO到Entity复制使用Hutool `BeanUtil`。
- Entity依赖全局驼峰配置，不编写重复字段映射。

## 13. 数据库脚本

脚本位置：

```text
mall-coupon/src/main/resources/db/schema-coupon.sql
```

模块化单体阶段不建立跨模块物理外键，`coupon_user.user_id` 与认证账号ID保持业务一致。优惠券模块内部通过业务校验、唯一索引和数据库约束保护数据完整性。

## 14. 测试覆盖

当前单元测试覆盖：

- 满减模板字段规范化。
- 非法折扣率校验。
- 发布后的模板禁止修改。
- 领取时占用库存并生成用户券和流水。
- 折扣券最高优惠金额。
- 未达到满减门槛时拒绝使用。
- 锁券、确认使用和订单取消回滚。
- 订单取消时优惠券已过期的处理。

已经执行：

```text
mvn -pl mall-coupon -am test
mvn -pl mallx-application -am test
```

优惠券模块8个测试通过，全项目16个Maven模块聚合构建成功。当前测试没有连接真实MySQL执行DDL，数据库脚本仍需要在MySQL 8环境中实际执行并进行接口联调。

## 15. 当前阶段未包含

以下能力保留到后续阶段：

- Redis优惠券库存预热
- Lua原子校验库存和用户限领
- RabbitMQ异步领取落库
- 消息发送失败和消费失败补偿
- Redis与MySQL发行库存对账
- 优惠券领取并发压测
- 分类券、品牌券、SPU券和SKU券
- 每人可领取多张同一模板优惠券
- 多券叠加和最优优惠组合
- 动态有效期，例如领取后7天有效
- 更细粒度的运营数据范围控制（`mall-admin`功能权限已接入）
- 与 `mall-order` 的完整下单、取消和超时关闭联调

当前实现先保证普通领取、优惠计算、订单状态接口和MySQL并发正确性，为后续订单闭环和高并发领取升级提供稳定基础。
