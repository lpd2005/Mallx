# MallX 前端

基于现有后端 MVP 的商城端和管理端，使用 JavaScript、Vue 3、Element Plus、Vue Router、Pinia、Axios、Vite。当前对接 `http://localhost:8080`。

## 本地启动

需要 Node.js 22.12 或更新版本（本次环境使用 Node.js 24）。在 PowerShell 中执行：

```powershell
cd D:\MallX\Mallx\_UI
npm install
npm run dev:storefront
```

再开一个终端启动管理端：

```powershell
cd D:\MallX\Mallx\_UI
npm run dev:admin
```

- 商城端：<http://localhost:5173>
- 管理端：<http://localhost:5174>
- 后端：<http://localhost:8080>

若 PowerShell 阻止 `npm.ps1`，直接使用 `npm.cmd` 执行相同命令，不必修改系统执行策略。

两个应用分别启动、构建、发布，安装依赖统一通过根目录的 npm workspaces 管理。提交了 `package-lock.json`，后续可用 `npm ci` 复现依赖。`shared` 只共享通用接口处理、格式化和展示组件，不共享登录状态。

## 目录

```text
_UI/
  storefront/             商城端
    src/views/            商品、购物车、结算、订单、售后、账户
    src/router.js         路由与登录守卫
  admin/                  管理端
    src/views/            看板、库存、订单、售后、角色分配
    src/resources.js      分类、品牌、商品、规格、优惠券等表单及列表定义
    src/navigation.js     菜单与权限点
    src/components/       通用管理表单、规格编辑器
  shared/                 双端共享接口、组件、样式
  tests/                  接口与组件自动化测试
  scripts/smoke-live.mjs  真实环境只读联调
```

## 页面范围

| 商城端                                           | 管理端                                                 |
| ------------------------------------------------ | ------------------------------------------------------ |
| 商品列表、名称筛选、分类与品牌筛选、商品规格详情 | 看板统计、SKU热销榜                                    |
| 手机号密码注册、登录、退出                       | 管理员登录、权限菜单和路由守卫                         |
| 购物车数量、勾选、删除、结算                     | 分类、品牌、SPU、SKU新增编辑、删除、上下架             |
| 地址选择、会员折扣与可用优惠券预估               | 库存分页、入库、业务单号流水                           |
| 创建订单、模拟支付、取消订单、确认收货           | 订单筛选、详情、备货、发货                             |
| 整单仅退款或退货退款、撤销、填写寄回物流         | 售后审核、退货验收、模拟退款                           |
| 领券中心、我的优惠券                             | 优惠券草稿、发布、停止                                 |
| 资料、头像、省市区地址、会员状态                 | 用户资料、账号启停、角色和权限、账号角色分配、操作日志 |

商品名称筛选调用现有 `/products?keyword=` 接口；未接入 `mall-search`、`mall-seckill`、`mall-ai`。会员只展示已有状态，没有新增开通或续费入口。发货沿用后端现有状态接口，不伪造发货物流字段。

## 接口配置

每个应用都提供 `.env.example`。需要调整时复制为对应目录的 `.env.local`：

```dotenv
VITE_API_BASE_URL=/api
API_PROXY_TARGET=http://localhost:8080
```

开发环境浏览器请求同源 `/api`，Vite 代理转发到后端并移除 `/api` 前缀，因此无需为开发联调修改后端 CORS。修改环境变量后重启对应开发服务。

所有页面使用真实接口，不包含自动回退的模拟商品、订单或统计数据。测试中的样本仅存在于 `tests`，不会打入生产包。

### 登录与权限

- 使用登录响应的 `tokenName` 和 `tokenValue` 设置请求头，不自行拼接 `Bearer`。
- 会话放在各自应用的 `sessionStorage`，商城与管理端独立登录，关闭标签页后会话不持久保存。
- HTTP 或业务码 401 清理本端会话并跳转登录。403 保留登录态，显示无权限提示。
- 管理端进入业务路由时查询 `/admin/me`，按权限控制菜单、页面和敏感按钮，后端仍是最终授权边界。
- 管理员账号必须已经由后端授予相应权限。前端不会创建系统管理员或自动给注册用户赋权。

### 数字与金额

后端返回的雪花 ID 可能超过 JavaScript 安全整数范围。接口层在原始 JSON 文本解析时使用 `json-bigint` 保留大整数为字符串，不先经过普通 `JSON.parse`；状态和页码等普通数值保留为 number。请求中的长 ID 保持字符串，后端 Jackson 可绑定至 Long。

结算页用 Decimal 按后端规则计算会员折后金额（HALF_UP，保留两位），再调用可用券接口。下单仅传地址 ID、用户券 ID、备注，不发送客户端计算的应付金额。实际订单金额以服务器响应为准。

### 图片

- 图片字段仍是 `mainImageOssKey`、`imageOssKey`、`avatarOssKey`、`logoOssKey`、`iconOssKey`，其值现为完整 HTTPS URL，展示时直接使用。
- 商品图片：`POST /admin/files/images/upload`；头像：`POST /users/me/files/avatar/upload`。
- 请求采用 `FormData`，字段名为 `file`。不手动设置 multipart boundary。
- 成功响应按 URL 纯文本处理；失败按 HTTP 状态及业务错误处理。上传后由业务表单保存 URL。
- 不调用已移除的签名刷新接口；没有图片、地址不是 HTTPS URL或加载失败时显示空图状态。
- JPEG/PNG 最大 5 MB。后端继续检查真实图片内容、尺寸与像素数。旧对象 Key 需要在后端迁移为 URL，前端不会猜测图片域名。

### 重试与操作状态

支付请求号按订单存入本标签页的 `sessionStorage`，同一次模拟支付失败重试时复用。入库单号在打开表单时生成，失败后保留表单供重试。不要在结果不明时另开入库表单生成新单号；先查询库存流水。

所有写操作在请求中禁用重复提交。请求超时不自动重发写操作；下单超时可先到订单列表确认结果。售后类型和可操作状态对应后端状态机，退款金额由后端决定。

## 构建与检查

```powershell
npm run lint
npm test
npm run build
npm run format:check
```

也可独立构建：

```powershell
npm run build -w @mallx/storefront
npm run build -w @mallx/admin
```

产物分别为 `storefront/dist` 和 `admin/dist`。格式化源码使用 `npm run format`。

在后端及两个开发服务已启动时运行真实只读联调：

```powershell
node scripts/smoke-live.mjs
```

或仅检查后端：

```powershell
node scripts/smoke-live.mjs http://localhost:8080
```

脚本验证公开商品接口、长 ID、游客鉴权边界，不修改业务数据。

## 部署

两份 `dist` 分别部署到两个域名或两个独立端口，路由使用 HTML5 history。静态服务器必须将非文件路径回退至对应 `index.html`，同时将 `/api/` 转发到后端并去掉前缀。Vite 的开发代理不会打入生产包。

同域反向代理示例（两个应用分别配置 server）：

```nginx
server {
    listen 80;
    server_name shop.example.com;
    root /srv/mallx/storefront/dist;

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        client_max_body_size 6m;
    }
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

管理端更换 `server_name` 与 `root` 指向 `admin/dist`。正式环境配置 HTTPS。若将 `VITE_API_BASE_URL` 改为跨域完整地址，则需要后端或网关配置对应 CORS。

## 验证范围

自动化测试覆盖原始JSON长ID、Token请求头、上传文本及错误、401/403、金额精度、关键商城页面、管理页面、登录提交、结算请求和保留角色按钮。组件测试使用 jsdom 与接口样本，不能等同于真实浏览器端到端验收。真实只读检查与后续需账号验证的项目见 `VERIFICATION.md`。
