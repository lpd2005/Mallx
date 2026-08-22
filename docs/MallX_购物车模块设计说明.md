# MallX 购物车模块设计说明

## 1. 模块定位

`mall-cart` 负责当前登录用户的购物车增删改查、勾选状态和结算预览。模块采用 Redis 作为主存储，不建立购物车 MySQL 表。

购物车只表达：

```text
用户想购买哪个SKU、购买多少、是否勾选
```

购物车不创建订单、不占用库存，也不决定最终成交价格。

## 2. Redis数据结构

每个用户使用一个 Redis Hash：

```text
mall:cart:{userId}
```

Hash Field 是 SKU ID，Value 示例：

```json
{
  "quantity": 2,
  "selected": true,
  "addedAt": 1787241600000
}
```

Value中没有商品价格、商品名称、图片和库存。购物车最后一次访问后保留30天，每个用户最多保存100种SKU，单个SKU最多99件。

`{userId}` 使用Redis Cluster Hash Tag语法，未来扩展多个购物车Key时仍可确保同一用户的数据位于相同Slot。

## 3. 为什么不保存购物车价格

商品价格属于 `mall-product`，不是购物车数据。查询购物车时的处理链路：

```text
Redis读取SKU和数量
  ↓
ProductSkuService批量查询SPU/SKU当前信息
  ↓
InventoryStockService批量查询当前库存
  ↓
使用当前销售价计算小计和汇总金额
```

商品价格修改后，Redis购物车不需要同步更新。用户再次查询购物车时会取得商品模块的当前价格。

订单创建时仍须再次查询价格和库存，并把成交价及商品快照固化到订单明细。前端提交的价格、购物车展示价格都不能直接作为成交依据。

## 4. Lua原子操作

购物车项以JSON保存。并发执行“读取JSON、修改数量、写回JSON”可能造成更新覆盖，因此以下组合操作使用Lua：

- 加入并累加数量
- 覆盖数量
- 修改单项勾选状态
- 全选或取消全选
- 删除全部已勾选商品

Lua在Redis服务端一次执行，避免分布式锁的获取、续期和释放成本。

脚本目录：

```text
mall-cart/src/main/resources/lua
```

## 5. 商品和库存批量查询

购物车可能包含多个SKU，禁止逐个查询形成N+1问题。

商品模块提供：

```java
listCartProducts(Collection<Long> skuIds)
```

该方法使用动态 `IN` 并关联SPU和SKU，SQL放在 `ProductSkuMapper.xml`。

库存模块提供：

```java
listStocks(Collection<Long> skuIds)
```

库存是单表查询，使用MyBatis-Plus Lambda完成，不写XML。

## 6. 商品有效性

加入或修改数量时要求：

- SPU和SKU均已上架
- 库存记录已经初始化
- 可用库存大于0
- 修改后数量不超过当前可用库存和99件上限

查询购物车时，单个失效商品不会导致整个购物车失败，而会返回 `purchasable=false` 和原因：

- 商品不存在
- SPU已下架
- SKU已下架
- 库存尚未初始化
- 商品已无库存
- 购物车数量超过当前库存

## 7. 库存边界

购物车库存检查只用于用户提示，不锁定库存。其他用户仍可能在结算前购买商品。

```text
购物车预校验 ≠ 库存占用
```

真正创建订单时由 `mall-order` 调用 `InventoryStockService.lockStock()`，只有数据库条件更新成功才表示库存占用成功。

## 8. 结算预览

结算预览只处理已勾选商品，并重新校验当前商品状态、价格和库存。返回商品当前原始总额，不计算会员折扣、优惠券、运费和最终应付金额。

这些价格规则后续统一放在订单结算流程，避免购物车和订单维护两套计算逻辑。

## 9. HTTP接口

```http
GET    /users/me/cart
POST   /users/me/cart/items
PATCH  /users/me/cart/items/{skuId}/quantity
PATCH  /users/me/cart/items/{skuId}/selected
PATCH  /users/me/cart/selection
DELETE /users/me/cart/items/{skuId}
DELETE /users/me/cart/selected-items
GET    /users/me/cart/checkout
```

Controller通过 `CurrentUserProvider` 获取登录用户ID，不接受前端传入的用户ID。

订单创建成功后，订单模块调用：

```java
removePurchasedItems(userId, skuIds)
```

该方法不直接暴露给前端。

## 10. 当前阶段未包含

- 购物车商品降价提醒
- 游客购物车及登录后合并
- MySQL购物车持久化
- 会员折扣和优惠券试算
- 商品Caffeine + Redis多级缓存
- 订单创建和库存锁定

当前实现完成“商品 → 购物车 → 结算预览”，下一阶段由 `mall-order` 继续形成普通交易闭环。
