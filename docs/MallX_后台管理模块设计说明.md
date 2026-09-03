# MallX 后台管理模块设计说明

## 1. 这一期完成什么

`mall-admin` 是后台入口和管理视角的组合层，不复制商品、订单、优惠券、售后的核心规则。

本期提供：后台准入与功能权限校验、当前管理员信息、分类/品牌/SPU/SKU管理、库存入库与查询、订单查询与履约、优惠券模板管理、售后管理、用户查询与启停、角色权限分配、基础运营看板、成功操作审计。

没有新增独立管理员账号表，也没有新增一套商品或订单表。管理员仍然使用 `auth_account` 登录，由角色和权限决定能做什么。

秒杀、评价、AI运营助手、报表导出、转化率、通用系统配置、菜单树与前端页面暂未实现。营销配置复用优惠券模板；没有明确消费方的“万能配置表”暂不添加。没有加入Redis统计缓存、MQ或分布式锁。

## 2. 分层与目录

```text
HTTP /admin/**
  → mall-auth / SaTokenConfig：登录态 + 账号启用状态
  → SaInterceptor路由规则：后台准入 + 权限声明兜底
  → SaInterceptor原生注解鉴权：@SaCheckPermission
  → 后台Controller：接收DTO、返回VO、调用服务
  → AdminOperationLogService.execute：写操作事务与审计
  → 原业务Service / 后台RBAC管理Service
  → MySQL
```

| 部分 | 主要作用 |
| --- | --- |
| mall-auth / SaTokenConfig | 统一注册Sa-Token原生拦截器，后台不再维护自定义鉴权注解和拦截器 |
| controller | 分类、品牌独立；SPU/SKU合并；其他业务各自独立 |
| AdminQueryService | 管理视角分页，复用领域Service的lambdaQuery |
| AdminAccountService / AdminRoleService | 复用auth实体和表，维护状态、角色和关联 |
| AdminProductService | SKU删除与库存存在性校验的跨模块组合 |
| AdminStatisticsService | 聚合现有订单、退款、账号数据 |
| AdminOperationLogService | 成功变更与日志同事务提交 |
| mapper / XML | 标准CRUD由MyBatis-Plus处理；关联短SQL用注解，统计聚合用XML |
| dto / vo | 请求参数与展示信息，没有converter包 |

有对应实体的持久化服务使用 `IService<Entity>` 和 `ServiceImpl<Mapper, Entity>`；组合服务没有专属实体，不为继承而虚构实体。自定义Mapper显式注入，不使用 `baseMapper.xxx` 调用。

VO使用 `@Builder`，同名属性使用Hutool `BeanUtil` 拷贝。库存 `outOfStock` 属于计算字段，仍需根据可用库存是否为0显式设置。

## 3. 已有接口如何接入

订单管理、优惠券模板管理、售后管理的Controller移到 `com.mallx.admin.controller`，URL保持不变，原模块只保留领域Service和面向用户的接口。

| 接口 | 调整 |
| --- | --- |
| /admin/orders | 保留备货、发货；增加后台分页和详情；补齐权限 |
| /admin/coupon-templates | 保留原管理功能；补齐权限及后台审计 |
| /admin/after-sales | 保留原售后功能；增加后台准入及统一审计 |
| /inventory/operations | 旧入口移除，改为 /admin/inventory/operations |
| /inventory/skus/{skuId} | 保留原只读库存接口 |

不能只新增一个“安全后台接口”，同时留下一个只登录即可访问的旧写入口。

首次切换建议执行 `mvn -pl mallx-application -am clean test`，清掉迁移前残留的Controller字节码，避免重复路由。

## 4. 权限模型

### 4.1 两道检查

所有 `/admin/**` 请求都需要：

1. 有效登录身份，且数据库中账号状态为启用。
2. 拥有 `admin:access`。
3. 拥有Controller或方法声明的业务权限。

业务权限统一使用Sa-Token原生 `@SaCheckPermission`。类与方法同时声明时，两处都会执行校验，不再采用“方法覆盖类”的自定义规则。

商品等Controller在类上声明统一权限；看板Controller使用方法级声明：`/admin/me` 只要求 `admin:access`，两个统计接口要求 `dashboard:read`，避免当前管理员信息接口被额外的看板权限挡住。

没有原生权限注解的后台接口默认拒绝，也不允许使用 `@SaIgnore` 跳过后台鉴权。这是 `SaTokenConfig` 中保留的一项项目安全规则：只检查声明是否存在，注解读取、权限值校验及AND/OR组合由Sa-Token执行。鉴权与前端菜单展示无关，前端隐藏按钮不是安全边界。

同一个 `SaTokenConfig` 按顺序注册三个原生 `SaInterceptor`：第一个检查登录与账号状态，第二个只处理后台准入和声明兜底，第三个执行原生注解鉴权。前两个关闭注解处理，避免重复执行，也保证未登录/禁用账号先被拦截。`/auth/login`、`/auth/logout` 保留原有放行规则。

`AdminPermission`、`AdminPermissionInterceptor` 和 `AdminWebConfiguration` 已移除；售后Controller不再手工检查同一权限，优惠券Controller不再重复检查登录。`StpInterfaceImpl` 仍负责从数据库提供角色和权限，它是Sa-Token的数据来源，不是另一套鉴权实现。

| 权限 | 用途 |
| --- | --- |
| admin:access | 后台准入、查询当前后台会话 |
| dashboard:read | 运营看板、热销SKU |
| product:manage | 分类、品牌、SPU、SKU |
| inventory:manage | 库存查询、入库、流水 |
| order:manage | 后台订单查询、备货、发货 |
| coupon:manage | 优惠券模板配置、发布、停止 |
| after-sale:manage | 售后查询、审核、收货、退款 |
| user:manage | 用户查询、启用、禁用 |
| rbac:manage | 角色、账号角色、角色权限 |
| audit:read | 成功后台操作流水 |

这是功能权限，不是商家/门店/部门的数据范围权限。当前具有某管理权限的账号能访问该业务的全量管理数据，适用于现阶段的单商城。

### 4.2 账号禁用与旧Token

`CurrentUserProvider` 的auth实现会读取账号的ID和状态，不读取密码摘要；统一登录拦截器在受保护请求上调用它。因此，账号禁用后，即使Redis里的Token尚未过期，下一次受保护请求仍会被拒绝。

这不是物理删除所有Token：重新启用账号后，尚未过期的旧Token可能恢复可用。已进入业务事务的请求也不强行中断。若以后要求禁用即永久撤销所有会话，需要补充会话吊销机制。

角色/权限查询也过滤禁用账号与禁用角色。本期没有额外缓存权限集合，后续请求从数据库重新查询授权结果。

### 4.3 防止误授权和管理员锁死

- 禁止修改自己的账号状态或角色。
- 保留角色 `SYSTEM_ADMIN` 不允许通过API修改、删除或替换权限。
- 持有保留系统角色的账号，不允许通过API改变状态或角色。
- 不允许通过API授予 `SYSTEM_ADMIN`，首次管理员由数据库受控初始化。
- 角色授权、分配角色，以及启用已有角色时，不能引入操作者自己尚未拥有的权限。
- 已被账号关联的角色不能删除，必须先解除关联。
- 关联替换前校验全部ID；有一个无效也拒绝，不做部分成功。
- `ids: []` 表示显式清空，`ids: null` 无效；重复ID会去重。
- 权限点是代码契约，本期只开放权限目录查询和关联分配，不开放任意权限编码、通配符的创建接口。

### 4.4 RBAC为什么使用行锁

角色删除与角色关联替换可能并发发生。所有后台RBAC变更先锁定同一条 `SYSTEM_ADMIN` 角色记录，再读取和修改关联。

这是低频权限管理的串行化保护，不影响普通商品和订单请求。管理员事务使用 `READ_COMMITTED`，避免等待锁后仍从旧的一致性快照读取关联状态。它不保护绕开应用直接执行的手工SQL。

没有初始化保留角色时，RBAC变更直接失败，不降级为无锁操作。这里没有乐观锁版本字段，也没有分布式锁。

## 5. 数据库与初始化

### 5.1 表结构

RBAC复用五张已有表：

```text
auth_account
auth_role
auth_permission
auth_account_role
auth_role_permission
```

后台只新增 `admin_operation_log`：

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| id | BIGINT | 审计主键 |
| operator_id | BIGINT | 登录账号ID |
| action | VARCHAR(64) | 操作编码 |
| target | VARCHAR(128) | 对象ID或业务单号 |
| created_at | DATETIME | 成功时间 |

账号和角色状态仍是数值0/1，数据库仍使用TINYINT；没有字符串状态或逻辑删除字段。

### 5.2 脚本位置与执行顺序

在已完成其他业务模块建表的基础上执行：

```text
mall-admin/src/main/resources/db/schema-admin.sql
mall-admin/src/main/resources/db/data-admin-rbac.sql
```

可选统计索引：

```text
mall-admin/src/main/resources/db/migration-admin-statistics-index-v1.sql
```

索引升级脚本执行一次即可，重复执行会报索引已存在。大表加索引应安排维护窗口。

RBAC种子脚本保留权限ID 910001～910010、角色ID 920001～920002。执行前检查这些ID是否与已有数据冲突；同编码已有记录会沿用旧ID。出现冲突应停止并回滚，不要强行覆盖已有身份。

脚本创建两个角色：

- SYSTEM_ADMIN：本版本全部十个权限。
- OPERATOR：后台准入、看板、商品、库存、订单、优惠券、售后七个权限；不含用户、RBAC和审计权限。

脚本只初始化角色和权限，不创建账号，不关联任何账号。重复执行会补齐这两个内置角色的种子权限；如果刻意回收了它们的某些权限，不要无意重跑种子脚本。自定义角色不受影响。

### 5.3 首位管理员由你明确指定

先检查目标账号ID、手机号和启用状态，再受控执行下面的SQL。示例默认NULL，不会给任何账号授权：

```sql
-- 将NULL替换为你已确认的现有账号ID后，再执行授权语句。
SET @admin_account_id = NULL;

SELECT id, phone, status
FROM auth_account
WHERE id = @admin_account_id;

INSERT INTO auth_account_role(account_id, role_id)
SELECT account.id, role.id
FROM auth_account account
JOIN auth_role role ON role.role_code = 'SYSTEM_ADMIN' AND role.status = 1
WHERE account.id = @admin_account_id AND account.status = 1
  AND NOT EXISTS (
      SELECT 1 FROM auth_account_role link
      WHERE link.account_id = account.id AND link.role_id = role.id
  );
```

本次开发没有执行建表、种子授权或修改真实数据库。已有售后表和退款表仍需按上一阶段文档初始化，否则运营统计无法查询退款。

## 6. 商品与库存管理边界

分类、品牌分开Controller；SPU和SKU共用 `AdminProductController`。

管理查询包含草稿、下架、禁用记录，不复用前台“仅展示启用/上架商品”的过滤条件。分页DTO默认第1页、每页20条，最大100条。

写操作复用原业务Service：

- 分类删除仍检查子分类和SPU引用。
- 品牌删除仍检查SPU引用。
- SPU删除仍要求草稿且没有SKU。
- SKU后台删除要求草稿，且从未建立库存记录；即使库存为0，只要已有库存记录，也只允许下架。
- 创建时生成草稿；后台上下架请求只接受1或2，不接受退回草稿的0。
- SKU删除与入库先取得同一SKU行锁，避免“校验SKU存在之后，SKU被删除，却仍然创建库存”的竞态。

后台不提供任意覆盖可用/锁定/已售数量的接口，只提供正数入库。锁定、扣减、释放仍由订单和售后流程驱动。

入库示例：

```http
POST /admin/inventory/skus/123/inbound
Content-Type: application/json

{"inboundNo":"IN20260902001","quantity":50,"remark":"首次入库"}
```

库存业务号实际为 `ADM-IN-IN20260902001`。重试同一次入库时必须保持单号和数量不变；改新单号会视为新入库。相同入库重试不会重复增加库存，但可以留下多条“成功处理请求”的后台审计记录。

## 7. 运营看板的统计口径

`GET /admin/dashboard?startDate=2026-09-01&endDate=2026-09-02`

日期参数使用自然日，范围为闭区间；SQL转换为 `[startDate 00:00, endDate+1 00:00)`，避免遗漏结束日的最后一秒。看板默认当天，热销榜默认近7天，最多查询93天。默认日期按Asia/Shanghai计算，部署时也应让业务时间写入保持相同口径。

| 字段 | 统计口径 |
| --- | --- |
| userCount | 当前账号总数，含禁用账号，不是活跃用户数 |
| newUserCount | 创建时间在区间内的账号数 |
| orderCount | 创建时间在区间内的订单数，含取消单 |
| orderAmount | 上述订单应付金额合计，含未支付/取消，不是销售额 |
| paidOrderCount | 支付时间在区间内的订单笔数，含后来退款订单 |
| paidAmount | 支付时间在区间内的实付金额 |
| refundAmount | 退款成功时间在区间内的退款金额 |
| netReceiptAmount | 本期支付减本期退款，允许为负数 |

例如今天实付100元、退还昨天订单120元，今天净收款为-20元，不应强行截成0。

支付和退款按各自发生时间统计。不能简单用“当前订单状态不是已退款”过滤历史支付额，否则过去的实收会随着今天退款而消失。

热销榜按SKU订单快照聚合购买件数，排除当前已整单退款的订单；售后处理中仍计入。退款完成后，该榜单会回溯变化。SKU名称取分组内MAX快照名称作为展示标签，不保证是最新商品名。榜单不是不可变财务报表。

没有访问/曝光/点击埋点，因此不返回虚构的转化率。统计直接查MySQL，没有预热、缓存、多级缓存或MQ。

## 8. 成功审计与业务事务

所有本期后台写入口通过 `AdminOperationLogService.execute` 执行业务操作，成功后写审计日志，二者同事务提交。

```text
开始事务
  → 获取真实操作者
  → 调用领域服务
  → 写admin_operation_log
  → 提交
任一步抛出异常 → 回滚
```

创建操作从领域返回实体/VO中获取生成的ID作为target。更新和删除使用请求路径中的对象ID/单号。

日志不保存密码、Token或完整请求正文，不提供后台修改/删除接口。订单/优惠券/售后的业务流水继续保留；它们说明领域状态变化，后台日志说明管理员成功执行了什么操作。

本期审计不是完整请求日志：不记录普通读取，也不将鉴权失败、校验失败或回滚操作保存为成功记录。后台表本身只记录操作编码与目标，不能据此还原每次操作的完整前后值；订单等领域可以结合其自身业务流水追踪状态，RBAC只能查询当前关联。完整前后快照、失败安全事件、外部不可篡改归档暂未实现。

## 9. 接口速查

以下后台接口均需 `admin:access`，并按第4节校验业务权限。

| 路径 | 方法与用途 |
| --- | --- |
| /admin/me | GET：当前管理员ID、角色、权限 |
| /admin/dashboard | GET：运营统计 |
| /admin/dashboard/top-products | GET：SKU销量榜；limit 1～20 |
| /admin/categories | GET分页、POST创建 |
| /admin/categories/{id} | PUT修改、DELETE删除 |
| /admin/brands | GET分页、POST创建 |
| /admin/brands/{id} | PUT修改、DELETE删除 |
| /admin/products/spus | GET分页、POST创建草稿 |
| /admin/products/spus/{id} | GET详情、PUT修改、DELETE删除 |
| /admin/products/spus/{id}/status | PUT上下架 |
| /admin/products/skus | GET分页（可传spuId）、POST创建草稿 |
| /admin/products/skus/{id} | GET详情、PUT修改、DELETE删除 |
| /admin/products/skus/{id}/status | PUT上下架 |
| /admin/inventory | GET库存分页，可传skuId |
| /admin/inventory/skus/{skuId}/inbound | POST入库 |
| /admin/inventory/operations | GET按businessNo查库存流水 |
| /admin/orders | GET分页，可传userId；keyword为精确订单号 |
| /admin/orders/{orderNo} | GET订单详情 |
| /admin/orders/{orderNo}/prepare-shipment | POST备货 |
| /admin/orders/{orderNo}/ship | POST发货 |
| /admin/coupon-templates | GET模板列表、POST创建；沿用原列表结构 |
| /admin/coupon-templates/{templateId} | PUT修改草稿 |
| /admin/coupon-templates/{templateId}/publish | POST发布 |
| /admin/coupon-templates/{templateId}/stop | POST停止发放 |
| /admin/after-sales | GET售后分页 |
| /admin/after-sales/{afterSaleNo} | GET详情 |
| /admin/after-sales/{afterSaleNo}/audit | POST审核 |
| /admin/after-sales/{afterSaleNo}/receive-return | POST验收 |
| /admin/after-sales/{afterSaleNo}/refund | POST退款 |
| /admin/users | GET账号分页；keyword为手机号模糊条件 |
| /admin/users/{userId} | GET安全账号视图，不含密码摘要 |
| /admin/users/{userId}/profile | GET资料；缺失返回null，不自动补建 |
| /admin/users/{userId}/status | PUT启用/禁用，status为1/0 |
| /admin/rbac/roles | GET分页、POST创建 |
| /admin/rbac/roles/{roleId} | PUT修改、DELETE删除 |
| /admin/rbac/permissions | GET代码定义的权限目录 |
| /admin/rbac/roles/{roleId}/permissions | GET关联ID、PUT全量替换 |
| /admin/rbac/users/{userId}/roles | GET关联ID、PUT全量替换 |
| /admin/operation-logs | GET分页，可传operatorId；keyword为精确action |

分页通用参数为 `current`、`size`、`keyword`、`status`，其中库存仅使用分页和skuId，审计只使用分页、操作人和操作编码。

角色请求示例：

```json
{"roleCode":"PRODUCT_OPERATOR","roleName":"商品运营","status":1}
```

关联替换请求示例（传真实查得的权限/角色ID，不是编码字符串）：

```json
{"ids":[910001,910003]}
```

## 10. 验证范围与后续工作

自动化测试覆盖：后台默认拒绝、原生类/方法权限叠加、当前管理员信息与看板权限隔离、原订单/优惠券/售后URL鉴权、有效权限放行、后台路由权限声明、禁止SaIgnore绕过、登录/禁用检查先于权限校验、前台路径与登录退出放行、应用contextPath下的后台保护、旧Controller迁移、旧Token禁用检查、保留管理员保护、无效/越权授权、关联去重/清空、SKU删除保护、统计日期边界、跨日退款净额、审计失败回滚。

同时执行原业务模块测试，确保管理入口重组不破坏原交易与售后逻辑。

本次验证命令：`mvn -pl mallx-application -am clean test`。原生Sa-Token迁移后全量115项测试通过，0失败、0错误，其中后台模块47项（含原生注解鉴权与售后权限测试）。

这些测试使用Mockito、MockMvc和模拟数据库连接；没有启动真实MySQL/Redis，没有执行建表SQL，也没有进行真实并发压测或外部支付验证。上线前还需：

1. 按顺序执行DDL、权限种子，明确指定首位管理员。
2. 使用普通用户、运营管理员、系统管理员分别联调接口。
3. 验证入库重试、SKU删除与入库并发、角色授权并发、订单履约与售后互斥。
4. 在真实数据量上检查统计SQL执行计划；必要时再设计汇总表或缓存。
5. 将审计前后快照、失败安全事件、数据范围权限等作为后续增强。
