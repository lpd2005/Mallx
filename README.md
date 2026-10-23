# MallX

面向学习与实践的高并发电商项目，覆盖商城前台、运营后台和模块化 Java 后端。项目围绕真实交易链路实现缓存、库存、订单、消息队列与商品搜索，并提供可复现的压测报告和模块设计文档。

> 当前项目以本地开发与功能验证为主。性能数据来自单机测试环境，用于同环境对比，不代表生产环境容量。

## 项目亮点

- **模块化单体后端：** 使用 Spring Boot 按认证、用户、商品、库存、购物车、订单、支付、优惠券、秒杀、搜索、售后和管理后台划分模块，模块间通过服务调用协作。
- **多级缓存：** 使用 JetCache 组合 Caffeine 与 Redis，为热点商品和首页数据提供本地及分布式缓存；配合布隆过滤器、空值缓存和缓存击穿保护。
- **异步秒杀：** 使用 Redis Lua 原子预扣库存和限制用户重复参与，通过 RabbitMQ 异步创建订单，并结合幂等校验与补偿任务处理最终一致性。
- **商品搜索：** 使用 Elasticsearch 与 IK 分词实现全文搜索、条件筛选、排序、高亮和联想；通过 Outbox 与 RabbitMQ 异步同步商品索引。
- **可靠消息与订单超时：** 使用 RabbitMQ Confirm、Return、TTL、死信队列、幂等消费与补偿任务，处理投递失败、重复消费和未支付订单关闭。
- **认证与后台权限：** 使用 Sa-Token、RBAC 和 Access/Refresh 双 Token，支持登录态续期、令牌轮换与会话注销。
- **前后端分离：** Vue 3 分别构建商城端与管理端；管理员操作集中在后台，并记录关键操作日志。

## 技术栈

| 部分 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3、Spring MVC、MyBatis-Plus、Maven |
| 数据与缓存 | MySQL、Redis、Caffeine、JetCache、Redisson |
| 消息与搜索 | RabbitMQ、Elasticsearch、IK 分词 |
| 前端 | Vue 3、Vue Router、Pinia、Element Plus、Axios、Vite、npm Workspaces |
| 文件存储 | 阿里云 OSS |
| AI 接入 | `mall-ai` Java 模块；独立 Python Agent 服务使用 FastAPI、LangChain 与 DeepSeek，单独维护 |

## 架构概览

```mermaid
flowchart LR
    Browser[商城端 / 管理端<br/>Vue 3] --> App[Spring Boot 应用<br/>模块化单体]
    App --> MySQL[(MySQL)]
    App --> Redis[(Redis<br/>Caffeine + JetCache)]
    App --> MQ[(RabbitMQ)]
    App --> ES[(Elasticsearch)]
    App --> OSS[阿里云 OSS]
    App -. 可选 AI 接入 .-> Py[独立 Python Agent<br/>FastAPI + LangChain]
    Py -. 签名调用商品工具 .-> App
```

## 模块结构

| 路径 | 职责 |
| --- | --- |
| `mallx-application` | Spring Boot 启动模块及应用配置 |
| `mall-common` | 通用响应、异常处理、缓存、Redis、RabbitMQ 等基础能力 |
| `mall-auth`、`mall-user` | 登录认证、双 Token、用户与地址 |
| `mall-product`、`mall-inventory` | 商品、分类、品牌、SKU 与库存 |
| `mall-cart`、`mall-order`、`mall-payment` | 购物车、订单与支付流程 |
| `mall-coupon`、`mall-seckill` | 优惠券与秒杀活动 |
| `mall-search` | Elasticsearch 商品索引与搜索 API |
| `mall-after-sale`、`mall-admin` | 售后及后台管理、RBAC、操作审计 |
| `mall-ai` | Java 侧 AI API、会话与内部工具安全接入 |
| `_UI/storefront`、`_UI/admin` | 商城前台和管理后台 |
| `docs` | 模块设计、验收记录与压测报告 |

## 本地运行

### 环境依赖

- JDK 17 或更高版本
- Maven（或使用项目根目录的 `mvnw`，如果已提供）
- Node.js 22.12 或更高版本及 npm
- MySQL、Redis、RabbitMQ、Elasticsearch
- OSS 配置（需要验证图片上传时）

先准备数据库和中间件，再按 [应用配置](mallx-application/src/main/resources/application.yml) 配置连接信息。敏感配置放在本地配置文件或环境变量中，不要提交真实密码、访问密钥或服务签名密钥。数据库初始化脚本分布在各模块的 `src/main/resources/db` 目录，按项目文档完成初始化。

### 启动后端

在仓库根目录执行：

```bash
mvn -pl mallx-application -am spring-boot:run
```

### 启动前端

在 `_UI` 目录安装依赖并分别运行商城端或后台：

```bash
npm install
npm run dev:storefront
```

```bash
npm run dev:admin
```

构建两个前端：

```bash
npm run build
```

具体接口地址、环境配置和初始化步骤以各模块说明及 `docs` 中的文档为准。Python Agent 位于独立目录，不包含在本仓库克隆内容中；AI 功能需要单独准备该服务及 DeepSeek 配置。

## 性能验证

项目包含固定数据准备脚本和压力测试报告。2026-09-11 的本机单节点对比测试使用 10,000 个 SPU、20,000 个 SKU：

- 固定首页热点请求命中 JetCache 后，平均响应时间由 280.14 ms 降至 2.62 ms。
- 相同宽泛关键词场景下，Elasticsearch 吞吐量约为 MySQL `LIKE` 查询的 4.84 倍。
- 秒杀场景中，500 个用户争抢 100 件库存，最终得到 100 个成功订单，无超卖及重复订单。

以上为特定本地环境和测试条件下的结果。详见 [升级前后压力测试对比报告](docs/MallX_升级前后压力测试对比报告_20260911.md) 与 [秒杀压力测试报告](docs/MallX_秒杀模块压力测试报告_20260911.md)。

## 文档

- [后端模块划分与职责](docs/MallX_后端模块划分与职责.md)
- [多级缓存设计与实现](docs/MallX_多级缓存设计与实现说明.md)
- [秒杀模块设计与实现](docs/MallX_秒杀模块设计与实现说明.md)
- [搜索模块设计与实现](docs/MallX_搜索模块设计与实现说明.md)
- [订单与支付模块设计](docs/MallX_订单与支付模块设计说明.md)
- [后台管理模块设计](docs/MallX_后台管理模块设计说明.md)
- [全部项目文档](docs/)

## 许可证

当前仓库未附带许可证文件。如需允许他人使用、修改或分发代码，请添加合适的开源许可证。
