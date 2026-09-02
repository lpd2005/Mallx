# MallX 商品与库存模块设计说明

## 1. 模块边界

`mall-product` 负责定义“卖什么”：

```text
分类 → 品牌 → SPU → SKU
```

`mall-inventory` 负责定义“还能卖多少”：

```text
SKU → 可用库存 / 锁定库存 / 已售库存
```

商品表不保存库存数量，库存中心通过 `sku_id` 与 SKU 业务关联。两个模块之间不建立物理外键，便于以后独立拆分。

## 2. 商品模型

### 2.1 分类 `product_category`

- 使用 `parent_id` 构建最多三级分类树。
- `parent_id = 0` 表示根分类。
- 同一父分类下名称不能重复。
- 存在子分类时禁止直接移动父分类，避免子树层级错误。

### 2.2 品牌 `product_brand`

- 保存品牌名称、Logo OSS Key、简介和排序。
- 品牌可以停用；停用后不能用于创建或更新 SPU。

### 2.3 SPU `product_spu`

SPU 保存一组 SKU 共享的信息，例如商品名称、品牌、分类、主图和详情。

状态值：

| 值 | 状态 |
|---:|---|
| 0 | 草稿 |
| 1 | 上架 |
| 2 | 下架 |

`min_price`、`max_price` 由当前已上架 SKU 的销售价自动汇总。

### 2.4 SKU `product_sku`

SKU 是可销售和管理库存的最小单元，保存：

- 唯一 SKU 编码
- SKU 名称
- JSON 销售规格
- 销售价和划线价
- SKU 图片
- 上下架状态

划线价不能低于销售价，规格必须是 JSON 对象。

## 3. 商品接口

当前开放前台只读接口：

```http
GET /products/categories
GET /products/brands
GET /products/{spuId}
GET /products/{spuId}/skus
GET /products/skus/{skuId}
```

分类、品牌、SPU、SKU 的创建和修改已经提供 Service 方法，后续由带权限控制的 `mall-admin` 调用，不直接暴露无权限保护的管理接口。

## 4. 库存模型

### 4.1 库存表 `inventory_stock`

每个 SKU 一条库存记录：

| 字段 | 含义 |
|---|---|
| `available_stock` | 当前可下单数量 |
| `locked_stock` | 已被订单锁定、尚未最终扣减的数量 |
| `sold_stock` | 已完成最终扣减的累计数量 |

当前不设置库存预警阈值；当 `available_stock = 0` 时，SKU 即为无库存状态。

始终满足：

```text
available_stock >= 0
locked_stock >= 0
sold_stock >= 0
```

### 4.2 库存流水 `inventory_operation`

库存流水同时承担幂等控制，唯一键是：

```text
(business_no, sku_id, operation_type)
```

相同订单、相同 SKU、相同操作类型重复请求时，不会重复修改库存。若业务号相同但数量不同，则拒绝处理。

操作类型：

| 值 | 操作 |
|---:|---|
| 1 | 增加可用库存 |
| 2 | 锁定库存 |
| 3 | 最终扣减库存 |
| 4 | 释放库存 |
| 5 | 售后回补可销售库存 |

流水保存操作前后的可用、锁定和已售库存快照。

## 5. 库存状态变化

假设 SKU 初始库存如下：

```text
可用 = 100，锁定 = 0，已售 = 0
```

### 5.1 下单锁定 3 件

```text
可用 100 → 97
锁定   0 → 3
已售   0 → 0
```

### 5.2 支付成功，最终扣减 3 件

```text
可用 97 → 97
锁定  3 → 0
已售  0 → 3
```

### 5.3 订单取消，释放 3 件

释放发生在锁定之后、最终扣减之前：

```text
可用 97 → 100
锁定  3 → 0
已售  0 → 0
```

不能对同一批库存同时执行扣减和释放；订单模块需要根据订单状态选择其中一种操作。

### 5.4 售后回补

支付后的锁定库存已经扣减，售后退款不能再调用释放库存。新增的 `restockAfterSale` 使用售后单号保证回补幂等，只增加可用库存，不修改锁定库存和历史累计已售数量；损坏的退货不回补可销售库存。详细验收规则见《MallX_售后模块设计说明.md》。

## 6. 防止负库存

库存数量变化不是“先查询、再更新”，而是数据库原子条件更新。

锁定库存的逻辑等价于：

```sql
UPDATE inventory_stock
SET available_stock = available_stock - :quantity,
    locked_stock = locked_stock + :quantity
WHERE sku_id = :skuId
  AND available_stock >= :quantity;
```

最终扣减要求 `locked_stock >= quantity`，释放库存同样要求 `locked_stock >= quantity`。

更新行数为 0 时表示余额不足，事务回滚，库存流水占位记录也会一并回滚。

## 7. 库存 Service

核心方法：

```java
increaseAvailable(skuId, quantity, businessNo, remark)
lockStock(skuId, quantity, businessNo)
deductStock(skuId, quantity, businessNo)
releaseStock(skuId, quantity, businessNo)
getStock(skuId)
```

推荐业务号：

```text
入库：入库单号
锁定、扣减、释放：订单号
```

## 8. 当前接口边界

库存模块当前只开放查询接口：

```http
GET /inventory/skus/{skuId}
GET /inventory/operations?businessNo={businessNo}
```

写操作由后台入库、订单和秒杀模块直接调用 `InventoryStockService`，避免用户绕过业务流程直接修改库存。

## 9. 商品物理删除规则

商品模块不使用逻辑删除。为了避免产生失去关联的数据，物理删除遵循以下规则：

| 数据 | 允许删除的条件 |
|---|---|
| 分类 | 不存在子分类，并且没有SPU引用 |
| 品牌 | 没有SPU引用 |
| SPU | 状态为草稿，并且没有SKU |
| SKU | 状态为草稿，即从未上架 |

分类、品牌、SPU和SKU的删除方法由各自 Service 提供。当前前台商品 Controller 保持只读，后续由 `mall-admin` 在完成权限校验后调用这些删除方法。

商品模块内部通过外键约束分类、品牌、SPU和SKU的关联，业务检查负责返回清晰提示，数据库外键负责并发情况下的最终完整性保护。

## 10. 数据库脚本

```text
mall-product/src/main/resources/db/schema-product.sql
mall-inventory/src/main/resources/db/schema-inventory.sql
```

执行顺序：

```text
1. schema-product.sql
2. schema-inventory.sql
```

## 11. 当前阶段未包含

以下能力留到对应阶段实现：

- 商品 Caffeine + Redis 多级缓存
- 商品变更 RabbitMQ 事件
- Elasticsearch 商品索引同步
- Redis 秒杀库存与 Lua 扣减
- 订单多 SKU 批量锁定和失败补偿
- 仓库、批次、库位等多仓库存模型

当前实现是普通订单数据库库存链路，可作为后续订单和秒杀模块的可靠库存基础。
