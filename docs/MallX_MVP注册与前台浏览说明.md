# MallX MVP注册与前台浏览说明

## 1. 本次补齐的范围

这次补齐普通商城MVP的两个入口：手机号密码注册、前台商品分页列表，并允许游客读取商品。交易继续使用现有模拟支付和模拟退款。

不开发 `mall-search`、`mall-seckill`、`mall-ai`，不增加多级缓存、Lua、MQ或短信平台；也不调整购物车当前仅存Redis的实现。

## 2. 注册

### 2.1 接口

`POST /auth/register`，不需要登录。

```json
{
  "phone": "13800138000",
  "password": "test-pass123",
  "nickname": "小明"
}
```

- `phone`：必填，中国大陆11位手机号，与登录接口格式相同。
- `password`：必填，8～64位，UTF-8编码后不超过72字节。字节限制用于兼容现有BCrypt实现，中文等多字节字符也会计入。密码不做trim。
- `nickname`：可选，最长32位；为空或全空白时由用户模块生成默认昵称。

成功响应的 `data` 只包含 `userId`，不返回密码摘要或Token。注册成功后再调用 `POST /auth/login`，携带相同手机号和密码获取会话。登录响应中的 `tokenValue` 是 Access Token，后续业务请求使用 `tokenName: tokenValue` 作为请求头；`refreshToken` 只能用于刷新接口。

### 2.2 执行逻辑

1. 校验参数，检查手机号是否已经注册。
2. 使用现有 `PasswordEncoder` 生成BCrypt密码摘要，写入 `auth_account`，账号状态为启用（1）。
3. 调用 `UserProfileService.initializeProfile` 初始化 `user_profile`，用户ID与认证账号ID一致。
4. 提交事务，返回用户ID。

账号和资料处于同一个MySQL本地事务中，资料初始化抛出异常时账号一并回滚。手机号预查询只用于友好提示，并发安全最终依靠已有 `uk_auth_account_phone` 唯一索引；并发插入冲突也返回“手机号已注册”。重复注册不会更新旧账号密码或资料。

注册不创建登录会话、不授予后台角色、不创建会员记录。新用户是非会员，不能通过注册请求指定VIP、管理员或账号状态。现有用户资料懒初始化机制继续保留，以兼容历史账号。

模块依赖新增为 `mall-auth → mall-user → mall-common`。auth负责注册事务编排，user负责用户资料规则；user不反向依赖auth，因此没有循环依赖。

### 2.3 当前限制

这是本地开发和演示用的手机号密码注册，**暂未验证手机号归属**。不能将其视为已经完成真实短信注册；正式开放前还需补短信验证码、注册频率限制等防滥用措施。

### 2.4 双Token会话

`POST /auth/login` 成功后同时返回短期 Access Token 和长期 Refresh Token：

```json
{
  "userId": 10001,
  "tokenName": "Authorization",
  "tokenValue": "access-token",
  "accessTokenExpiresIn": 1800,
  "refreshToken": "refresh-token",
  "refreshTokenExpiresIn": 604800
}
```

- Access Token 默认有效30分钟，只用于访问业务接口；保留 `tokenValue` 字段名以兼容现有客户端。
- Refresh Token 默认有效7天，是独立生成的随机不透明令牌，只在 Redis 中保存其 SHA-256 摘要对应的会话，不能充当 Access Token。
- Access Token 到期后，调用 `POST /auth/refresh` 并提交 `{"refreshToken":"..."}`，成功后得到一对新令牌。
- 刷新采用轮换策略：新令牌签发成功后立即注销旧 Access Token并删除旧 Refresh Token；旧 Refresh Token不能再次使用。
- 同一个 Refresh Token 的并发刷新由 Redisson 分布式锁串行化，只有一个请求能成功，避免并发产生多组有效令牌。
- 调用 `POST /auth/logout` 时携带当前 Access Token，会同时注销当前 Access Token及其关联的 Refresh Token。
- 有效期通过 `mallx.auth.token.access-ttl` 和 `mallx.auth.token.refresh-ttl` 配置，Refresh Token有效期必须长于Access Token。

## 3. 前台商品分页

### 3.1 接口和参数

`GET /products`，不需要登录。

示例：`GET /products?current=1&size=20&categoryId=10&brandId=20&keyword=手机`

| 参数 | 含义 | 默认值或限制 |
| --- | --- | --- |
| current | 页码 | 默认1，范围1～100000 |
| size | 每页条数 | 默认20，范围1～100 |
| categoryId | 精确分类筛选 | 可选，必须为正数；传商品绑定的叶子分类ID |
| brandId | 精确品牌筛选 | 可选，必须为正数 |
| keyword | 商品名称包含匹配 | 可选，最长120位；去首尾空白，空白表示不过滤 |

返回 `Result<PageResult<SpuVO>>`。分页部分包含 `records`、`total`、`current`、`size`；商品字段复用现有 `SpuVO`，包括名称、主图OSS Key、描述、分类品牌ID、最低价和最高价等。没有结果时返回空数组和总数0。

### 3.2 查询规则

- 强制 `status = 1`（已上架），客户端不能指定草稿或下架状态。
- 分类、品牌、名称条件之间为AND关系。
- 分类只匹配当前ID，不自动递归父分类的全部子分类。前端可以从分类树选择叶子分类。
- 关键词使用MyBatis绑定参数的LIKE查询，`%`、`_`按普通字符处理，不提供自定义SQL或排序字段。
- 固定按 `updated_at DESC, id DESC` 排序，ID用于同一更新时间下的确定性排序；数据变更时分页内容仍可能变化。
- 使用MyBatis-Plus `lambdaQuery().page(...)`，复用common中的分页拦截器；不新增XML，不引入ES。
- 与原有详情接口保持相同的商品可见性规则：以SPU上架状态为准。分类/品牌导航只返回启用项，但禁用导航项不等于商品下架，隐藏商品应执行商品下架操作。

价格取MySQL中SPU维护的在售SKU价格区间，不存入Redis购物车。列表价格用于展示，真正下单时仍由订单模块按当前商品、会员和优惠券重新计算，不能采用客户端传来的金额。

## 4. 游客权限边界

下面这些读取接口允许匿名访问：

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | /products | 商品分页 |
| GET | /products/categories | 启用分类树 |
| GET | /products/brands | 启用品牌 |
| GET | /products/{spuId} | 已上架SPU详情 |
| GET | /products/{spuId}/skus | 对应商品的已上架SKU |
| GET | /products/skus/{skuId} | 已上架SKU详情 |

公开接口通过Controller方法上的 `@Public` 声明，不再维护游客URL白名单，也不将整个 `/products/**` 排除鉴权。注解定义在 `mall-common` 的 `com.mallx.common.annotation.Public`，商品模块无需依赖认证模块。

例如 `ProductController` 已经声明 `@RequestMapping("/products")`，其列表方法写成：

```java
@Public
@GetMapping
public Result<PageResult<SpuVO>> products(@Valid ProductPageQuery query) {
    return Result.success(spuService.pageProducts(query));
}
```

第一个Sa-Token拦截器检查 `handler instanceof HandlerMethod` 和 `hasMethodAnnotation(Public.class)`；有注解就返回，没有注解则执行登录和账号启用检查。`@Public` 仅允许标注方法，不支持整类放行。修改URL或配置应用contextPath后不需要同步修改白名单；请求方法仍由Spring MVC映射决定，未标注注解的写接口不会被一并放行。

单独读取SKU时，除了检查SKU自身已上架，还检查所属SPU已上架，避免绕过SPU详情访问草稿或已下架商品的SKU。

注册、登录、退出的方法也统一使用 `@Public`，不再保留 `/auth/login`、`/auth/logout` 的路径排除配置。公开注解只跳过登录与账号状态校验，后续原生权限注解处理仍然执行。后台接口禁止标注 `@Public`，即使同时声明并具备业务权限，也会被后台默认拒绝规则拦截。

购物车、个人资料、地址、订单、支付、售后等接口仍要求登录；`/admin/**` 仍要求登录、账号启用、`admin:access`及具体业务权限。游客浏览商品不需要读取Redis登录会话，但商品查询仍需要MySQL。

## 5. 数据库与模拟交易

本次不新增表或字段，不需要新的迁移脚本。继续使用：

- `mall-auth/src/main/resources/db/schema-auth.sql` 中的 `auth_account` 和手机号唯一索引。
- `mall-user/src/main/resources/db` 中的用户资料、地址等表。
- `mall-product/src/main/resources/db/schema-product.sql` 中的分类、品牌、SPU和SKU表。

交易没有改成真实支付，仍使用现有的支付单、模拟支付成功、订单履约及售后模拟退款逻辑；模拟不意味着跳过库存锁定、金额计算、订单状态和幂等校验。

## 6. 服务开启后的验收顺序

以下步骤留待你检查代码并启动MySQL、Redis等配置要求的服务后执行，本次没有代为执行。

1. 按已有模块文档准备数据库、后台权限种子和明确指定的管理员。不要给所有新注册用户赋予管理员角色。
2. 用后台账号创建分类、品牌、SPU、SKU，初始化库存并上架SKU和SPU。
3. 不带Token调用商品列表、分类、品牌和详情；确认草稿与下架商品不能作为前台商品查看。
4. 调用注册接口，检查账号与资料各一条、密码为摘要、没有会员与后台角色。重复手机号注册应失败。
5. 登录，保存响应中的Token；创建收货地址，将有库存的SKU加入购物车并勾选。
6. 调用 `GET /users/me/cart/checkout` 查看预结算，再调用 `POST /users/me/orders`，请求体例如 `{"addressId":实际地址ID}`。不选优惠券也可以完成基本链路。
7. 调用 `POST /users/me/orders/{orderNo}/payments` 创建支付单，再调用 `POST /users/me/payments/{paymentNo}/mock-pay`，请求体例如 `{"requestNo":"mvp-pay-001"}`；同一支付动作重试时复用请求号。
8. 用具备订单管理权限的后台账号调用 `POST /admin/orders/{orderNo}/prepare-shipment`、`POST /admin/orders/{orderNo}/ship`，再由购买用户调用 `POST /users/me/orders/{orderNo}/receipt`。
9. 验证未登录不能访问购物车、订单、支付和后台；普通用户不能操作后台，不能查看或支付别人的订单。

其他请求字段与状态变化参见已有《商品与库存模块设计说明》《购物车模块设计说明》《订单与支付模块设计说明》《售后模块设计说明》和《后台管理模块设计说明》。

## 7. 本次测试范围

验证命令：`mvn -pl mallx-application -am test`（最终运行附加 `-q` 减少日志）。改为Public注解后全量164项测试通过，0失败、0错误、0跳过。

自动化测试覆盖注册密码摘要与原登录兼容、重复手机号和并发唯一键冲突、Spring事务提交/回滚边界、参数校验、商品分页条件和VO映射、方法级Public放行及应用contextPath、旧公开URL未标注注解时拒绝访问、URL变更后注解仍生效、原生权限注解不被绕过、后台禁止Public、写方法与个人接口保护，并回归已有交易与权限测试。

测试使用Mockito和MockMvc，不等同于真实MySQL/Redis环境验收，也不证明实际并发数据库行为已经通过压测。真实注册事务、唯一索引并发和模拟交易闭环仍需在服务启动后联调验证。
