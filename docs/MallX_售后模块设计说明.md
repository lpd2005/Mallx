# MallX 售后模块设计说明

> 当前实现：整单未发货退款、整单退货退款，以及与订单、库存、优惠券、模拟支付的事务闭环。本阶段未开发 `mall-search`，没有引入 MQ 或真实退款渠道。

## 1. 模块边界

`mall-after-sale` 负责申请、审核、撤销、退货物流、验收、退款编排和操作流水。

各模块分工如下：

| 模块 | 本次职责 |
|---|---|
| mall-after-sale | 管理售后状态，决定何时允许退款 |
| mall-order | 校验订单归属和状态，暂停/恢复履约，完成整单退款 |
| mall-payment | 校验原支付单，执行模拟退款，保存成功退款流水 |
| mall-inventory | 使用售后业务号幂等回补可销售库存 |
| mall-coupon | 整单退款后退还仍有效的用户券，已过期的券转为过期 |
| mall-auth / mall-common | 登录身份与管理权限校验 |

依赖方向：

```text
mall-after-sale → mall-order
mall-after-sale → mall-payment → mall-order
mall-order     → mall-inventory、mall-coupon
```

订单和支付不反向依赖售后模块。跨模块通过 Service 调用，不从售后模块直接操作订单、支付或库存 Mapper。

## 2. 第一版业务规则

### 2.1 两种售后类型

| 数据库值 | 类型 | 允许申请的订单状态 |
|---:|---|---|
| 1 | 仅退款 REFUND_ONLY | 已支付、待发货 |
| 2 | 退货退款 RETURN_REFUND | 已发货、已完成 |

当前以整单为单位，不允许只选部分 SKU、只退部分件数或由前端指定退款金额。

退货退款要求用户寄回整单商品，并由仓库确认验收。尚未发货的订单不需要寄回，审核后即可执行退款。

### 2.2 退款金额与优惠券

退款金额等于下单时保存的 `payAmount`，不会重新查询商品现价、重新计算会员折扣，也不会退款券面值。

例如：

```text
商品原价100元
会员优惠5元
优惠券优惠10元
订单应付85元
整单退款85元
```

支付模块还会再次核对申请退款金额与原支付单金额相等，防止错误调用导致多退或少退。

整单退款成功后，原优惠券通过现有回滚方法处理：

- 仍在有效期内：恢复未使用，允许该用户再次使用。
- 已经过期：转为已过期。
- 不减少优惠券模板已领取数量，不将用户券退回公共发行库存。

零元订单仍需有成功的模拟支付单；对应整单退款允许金额为0。

### 2.3 重复申请

同一订单最多存在一个处理中或已退款完成的售后单。驳回或撤销后可以重新申请，原记录和流水保留。

当前重复提交申请会因订单已经进入售后而被拒绝；尚未提供客户端申请幂等请求号。退款完成后的重复退款则会返回原结果。

## 3. 数据库设计

### 3.1 售后主表 after_sale

| 字段 | 含义 |
|---|---|
| after_sale_no | 售后单号 |
| order_id、order_no、user_id | 原订单与申请用户 |
| type、status | 数值类型和状态 |
| active_order_no | 订单占位键 |
| original_order_status | 进入售后前的订单状态快照 |
| refund_amount | 原订单应付金额快照 |
| reason | 用户申请原因 |
| auditor_id、audit_remark、audited_at | 审核信息 |
| restock | 0不回补、1回补；退货验收前为空 |
| return_carrier、return_tracking_no | 用户寄回物流 |
| return_shipped_at、received_at | 寄回与验收时间 |
| refund_no、completed_at | 成功退款单号与完成时间 |

两个关键唯一索引：

```text
UNIQUE(after_sale_no)
UNIQUE(active_order_no)
```

`active_order_no` 的处理规则：

- 待审核、待退款、待寄回、待验收：保存订单号。
- 退款完成：继续保存订单号，阻止再次为整单退款。
- 驳回、撤销：置空，允许重新申请。

MySQL唯一索引允许多条记录为NULL，因此历史驳回或撤销记录可以共存。数据库CHECK约束同时检查占位键与状态的对应关系。

Entity中的 `@TableField(updateStrategy = FieldStrategy.ALWAYS)` 只用来允许更新该字段为NULL，不是驼峰字段映射，也不是逻辑删除。

### 3.2 售后流水 after_sale_log

每次状态变化记录：

- 售后单号。
- 操作前后状态。
- 操作类型。
- 实际操作人ID。
- 说明和时间。

申请原因最长500字，审核、验收说明最长255字。历史售后与流水没有删除接口。

### 3.3 退款成功流水 payment_refund

该表属于 `mall-payment`，记录：

```text
退款单号
售后单号
原订单号、支付单号和支付用户
退款金额
退款完成时间
```

唯一索引为退款单号、售后单号和订单号，保护“同一售后不重复退款”和“整单只退款一次”。

这是一张模拟退款成功流水表。模拟退款失败时，本地事务回滚，不留下伪造的成功记录。当前没有第三方退款中、退款失败或异步回调状态。

## 4. 售后状态流转

| 值 | 枚举 | 含义 |
|---:|---|---|
| 1 | WAIT_AUDIT | 待审核 |
| 2 | APPROVED | 已满足退款条件，待执行退款 |
| 3 | WAIT_RETURN | 审核通过，等待用户寄回 |
| 4 | WAIT_RECEIVE | 已寄回，等待仓库验收 |
| 5 | COMPLETED | 退款完成 |
| 6 | REJECTED | 审核驳回 |
| 7 | CANCELED | 用户撤销 |

仅退款：

```text
待审核 → 待退款 → 已完成
   └──→ 已驳回
```

退货退款：

```text
待审核 → 待寄回 → 待验收 → 待退款 → 已完成
   └──→ 已驳回
```

撤销规则：

- 待审核可以撤销。
- 待寄回可以撤销。
- 仅退款的待退款阶段可以撤销。
- 已寄回或已经验收的退货退款不能撤销。
- 已完成退款不能撤销。

重复审核不会重新流转，返回状态不允许。重复寄回物流要求内容相同，重复验收不能改变回补结论；重复撤销已撤销申请、重复退款已完成申请直接返回原结果。

## 5. 订单状态联动

订单新增：

| 值 | 枚举 | 含义 |
|---:|---|---|
| 7 | AFTER_SALE | 售后处理中，暂停履约 |
| 8 | REFUNDED | 整单退款完成 |

订单新增字段：

- `after_sale_no`：最近一次售后单号，保留用于核对请求归属。
- `status_before_after_sale`：申请前的订单状态。
- `refunded_at`：整单退款完成时间。

主要流转：

```text
合法原状态 → AFTER_SALE
                 ├── 驳回/撤销 → 恢复原状态
                 └── 退款成功 → REFUNDED
```

申请与发货都使用同一条订单记录的行锁。因此申请先成功，订单进入售后，发货请求就不能继续；发货先成功，未发货仅退款申请就会被拒绝。

驳回或撤销时校验售后单号与订单绑定一致，避免旧售后操作覆盖后一次申请。

订单创建时间、支付时间、发货时间和原完成时间不因售后而被清空。订单状态变化继续写入订单操作流水，新增操作类型8申请售后、9退出售后、10退款。

## 6. 申请、审核与退货流程

### 6.1 申请

1. 从CurrentUserProvider获取用户ID。
2. 校验订单号、类型和申请原因。
3. 锁定订单并检查归属与合法状态。
4. 将订单改为售后中，保存原状态和售后单号。
5. 复制可信订单金额，创建待审核售后单。
6. 写售后流水并提交事务。

若创建售后记录或写流水失败，订单进入售后的修改同时回滚。

### 6.2 审核

审核人需拥有管理权限：

- 仅退款通过：进入待退款。
- 退货退款通过：进入待寄回。
- 驳回：恢复订单原状态，释放订单占位键，记录驳回说明。

审核通过不直接退款，避免退货商品尚未寄回就退款。

### 6.3 寄回和验收

用户填写物流公司与单号后进入待验收。物流信息记录在本地数据库，不代表接入了物流公司查询服务。

管理员确认整单退货后进入待退款，并必须明确 `restock`：

- true：全部退回商品可再次销售，退款时回补可用库存。
- false：不回补可销售库存，例如退回商品损坏。

本版是整单回补或整单不回补，不支持按单个SKU设置验收结果。退货地址沟通、部分退货和仓库质检子流程暂未实现。

## 7. 模拟退款事务

退款流程：

```text
锁定售后单并校验待退款状态
  ↓
锁定原支付单，核对用户、渠道、支付状态和完整退款金额
  ↓
保存退款成功流水，支付单改为已退款
  ↓
锁定订单并核对售后绑定
  ↓
按验收结果回补库存、退还原优惠券
  ↓
订单改为已退款并写订单流水
  ↓
售后单改为已完成并写售后流水
  ↓
提交同一个MySQL事务
```

`PaymentRefundService.refundForAfterSale` 与订单的售后内部方法使用 `Propagation.MANDATORY`，要求已经存在售后外层事务，防止它们被单独调用后局部提交。

当前只模拟数据库状态变更，没有请求第三方支付平台。真实支付退款无法被本地事务回滚，接入时必须重新设计申请、外部调用、回调、查单、补偿与对账链路。

## 8. 库存回补为何不是releaseStock

订单在支付时已经执行：

```text
lockedStock减少
soldStock增加
```

此时锁定库存已经不存在，因此售后不能调用 `releaseStock`。

本次新增 `restockAfterSale`，库存操作类型为5：

```text
availableStock增加
lockedStock不变
soldStock不变
```

保留 `soldStock` 原有的“历史累计完成扣减数量”口径，不将其悄悄改成净销量。退款净销量应以后从成交与退款流水计算。

回补以售后单号作为业务号，使用现有 `(business_no, sku_id, operation_type)` 唯一索引防止重复增加库存。订单退款还会按SKU ID排序回补，减少不同请求加锁顺序不一致的情况。

## 9. 并发与幂等

- 同一售后单的审核、撤销、验收、退款共用售后行锁。
- 售后申请和订单履约共用订单行锁。
- 付款、退款以及可能关闭支付单的查询共用支付行锁。
- 有效售后占位键阻止同一订单出现多个有效申请。
- 退款流水的订单号和售后单号唯一索引防止重复退款。
- 库存流水保证重复回补幂等。
- 订单状态与售后绑定校验防止过期请求越过新流程。
- 异常继续向外抛出，由Spring回滚事务，不在关键步骤吞掉异常。

退款主流程的锁顺序为售后单→支付单→订单→库存/优惠券。行锁不能替代业务状态检查，数据库异常仍需回滚处理；当前尚未经过真实MySQL并发压测。

## 10. HTTP接口与权限

### 10.1 当前用户

```http
POST /users/me/after-sales
GET  /users/me/after-sales?status=1&current=1&size=20
GET  /users/me/after-sales/{afterSaleNo}
POST /users/me/after-sales/{afterSaleNo}/cancel
POST /users/me/after-sales/{afterSaleNo}/return-shipment
```

申请示例：

```json
{
  "orderNo": "MX订单号",
  "type": 1,
  "reason": "不再需要，申请整单退款"
}
```

退货物流示例：

```json
{
  "carrier": "物流公司",
  "trackingNo": "退货物流单号"
}
```

### 10.2 管理端

```http
GET  /admin/after-sales?status=1&current=1&size=20
GET  /admin/after-sales/{afterSaleNo}
POST /admin/after-sales/{afterSaleNo}/audit
POST /admin/after-sales/{afterSaleNo}/receive-return
POST /admin/after-sales/{afterSaleNo}/refund
```

审核示例：

```json
{
  "approved": true,
  "remark": "符合申请条件"
}
```

验收示例：

```json
{
  "restock": false,
  "remark": "整单验收完成，商品损坏不可再次销售"
}
```

所有管理接口显式检查 `after-sale:manage` 权限，单纯登录不够。需要在现有 `auth_permission` 中配置该权限编码，通过 `auth_role_permission` 与 `auth_account_role` 授予指定账号；本次不会自动给任意账号授予权限。

用户查询与写操作都校验资源归属，列表使用MyBatis-Plus分页，默认每页20条，最多100条。详情附带操作流水，列表不逐条加载流水。

## 11. 代码约定

- Service继承IService，实现类继承ServiceImpl。
- 普通条件查询使用Lambda，不通过baseMapper手写CRUD。
- 行锁Mapper显式注入；短小固定FOR UPDATE使用注解。
- 暂无复杂联表或动态SQL，不增加XML文件。
- 同名Entity/VO字段使用Hutool BeanUtil复制，VO保留Builder。
- 类型与状态使用Integer持久化到TINYINT，Boolean请求参数在Service转换。
- 不使用未配置的乐观锁、逻辑删除或converter包。

## 12. 建表与旧库升级

### 全新数据库

在订单、库存、优惠券、支付基础表已经初始化后执行：

```text
mall-payment/src/main/resources/db/schema-refund.sql
mall-after-sale/src/main/resources/db/schema-after-sale.sql
```

全新订单表应使用更新后的 `schema-order.sql`，已经包含售后字段和扩展状态。

### 已有旧版订单数据库

依次执行：

```text
1. mall-after-sale/src/main/resources/db/migration-after-sale-v1.sql
2. mall-payment/src/main/resources/db/schema-refund.sql
3. mall-after-sale/src/main/resources/db/schema-after-sale.sql
```

升级脚本只适用于尚未增加售后字段的旧库，执行一次；已用新版建表脚本初始化的数据库不应重复执行。MySQL DDL会隐式提交，执行前应备份并核对原约束名称。

仅重新执行CREATE TABLE IF NOT EXISTS不会给现有表补上新字段或更新CHECK约束。本次未连接用户MySQL执行上述SQL。

## 13. 测试与验证范围

覆盖以下关键情况：

- 申请金额来自订单快照。
- 非法类型和不允许的订单状态被拒绝。
- 申请售后后阻止发货或确认收货。
- 驳回、撤销恢复原订单状态。
- 其他用户不能操作售后。
- 寄回物流与验收结论不能被重试覆盖。
- 未验收不能退款。
- 不可销售退货可以退款但不回补库存。
- 重复退款不重复写款项、库存和订单。
- 不等额、未支付、其他用户支付单不能退款。
- 支付查询不能把已退款状态关闭成未支付过期状态。
- 管理权限失败时不会调用退款Service。
- 使用Spring事务代理和模拟JDBC连接验证下游失败触发rollback，而非commit。

运行命令：

```text
mvn -pl mallx-application -am test
```

本次全项目16个Maven模块构建成功，70个测试通过。其中售后模块17个、订单模块10个、支付模块10个、库存模块5个测试通过。

这些是单元测试和事务代理测试，不等价于真实MySQL事务、唯一索引、锁等待或并发集成测试。数据库脚本执行、接口联调和并发压测仍需在测试环境完成。

## 14. 后续范围

- 按商品或件数的部分退款与优惠分摊。
- 换货、补发及替换商品库存。
- 退货地址配置和物流查询。
- 按SKU验收和分仓入库。
- 售后申请时间窗口、寄回期限与自动关闭策略。
- 多轮审核、争议处理和申诉。
- 真实渠道退款、异步回调、查单、失败重试与对账。
- MQ事件与模块拆分后的分布式一致性。
- 用户重复申请的客户端幂等请求号。
