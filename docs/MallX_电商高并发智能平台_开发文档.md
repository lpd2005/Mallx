# MallX · 企业级高并发智能电商平台 · 开发文档

MallX

企业级高并发智能电商平台

开发设计与实施文档

定位：Java 后端实习核心项目 + 高并发/分布式能力展示 + AI Agent 能力展示

技术路线：Spring Boot 3 + MyBatis-Plus + Sa-Token + MySQL + Caffeine + Redis + RabbitMQ + Elasticsearch + 阿里云 OSS + Vue3 + Vue Router + LangChain/LangGraph

# 文档信息

版本约定：本开发文档以“当前技术栈方向”为准，具体依赖版本在实际初始化项目时锁定，并通过 Maven、npm、pip/uv 的 lock 文件固定。

# 核心原则

业务完整但不过度堆功能

高并发场景必须可复现、可压测、可解释

Caffeine、Redis、RabbitMQ、Elasticsearch 都要解决真实问题

AI Agent 必须能够调用真实业务 Tool，而不是只做聊天框

先做模块化单体，核心链路成熟后再考虑微服务化

# 目录

1. 项目定位与目标

2. 系统边界与用户角色

3. 总体架构

4. 技术栈与职责分工

5. 功能模块设计

6. 核心数据库设计

7. Caffeine + Redis 多级缓存设计

8. RabbitMQ 设计

9. Elasticsearch 设计

10. 秒杀与高并发方案

11. 订单与库存一致性

12. AI Agent 架构

13. 前端工程设计

14. Java 项目工程结构

15. 开发规范与接口规范

16. 测试与压测方案

17. Docker 与部署

18. 开发阶段与时间顺序

19. 面试与简历呈现

20. 最终验收标准

# 1. 项目定位与目标

MallX 不是简单商城 CRUD，而是一个围绕“交易闭环 + 高并发场景 + AI 业务智能”设计的综合项目。核心目的是让项目能证明：你不仅会 Spring Boot 开接口，还理解缓存、消息、并发、库存一致性、搜索、幂等、压测以及 AI Agent 与传统业务系统的整合。

## 项目核心能力矩阵

项目验收不是“功能全部都有”，而是下面三条链路必须非常扎实：

普通下单链路：商品 -> 购物车 -> 结算 -> 订单 -> 支付模拟 -> 发货 -> 收货

秒杀链路：预热 -> 限流 -> Redis扣库存 -> MQ -> 异步创建订单 -> 一致性校验

AI链路：用户自然语言 -> Agent判断 -> Tool Calling/RAG -> Java业务接口/知识库 -> 结构化结果 -> 自然语言回复

# 2. 系统边界与用户角色

## 用户角色

## 前台功能边界

首页与推荐位

分类与商品列表

商品搜索与筛选

商品详情

购物车

结算页

订单中心

优惠券中心

秒杀专区

收藏/浏览记录

售后中心

个人中心

AI助手

## 后台功能边界

控制台/数据大盘

商品与SKU管理

库存管理

订单管理

优惠券管理

秒杀活动管理

售后管理

评价管理

用户/角色/权限

AI运营助手

操作日志

# 3. 总体架构

推荐采用“模块化单体 + 独立 AI Service”的架构。这样既能保持 Java 项目的整体性，又能体现 Python Agent 能力；后续只拆分高并发或独立演进的模块。

Vue3 + Element Plus

|

Vue Router + Pinia + Axios

|

Nginx

|

Spring Boot 3 Application

|

+-------------------------+--------------------------+

|            |            |             |            |

Auth        Product       Order        Promotion     Search

|            |            |             |            |

+------------+------------+-------------+------------+

|

+------------------+------------------+

|           |               |               |

MySQL        Caffeine(L1)        Redis(L2)       RabbitMQ

|           |               |               |

|           |               |               +----> Async workers

|           +----> local cache  +----> cache / lock / stock

|

+----> transaction / index / relation

+---------------------------+

| Python AI Service         |

| FastAPI                   |

| LangChain / LangGraph     |

| RAG / Tool Calling / Agent |

+-------------+-------------+

|

LLM / Vector DB

|

+-----------+-----------+

| Java业务Tools | 知识库 |

+-----------------------+

阿里云 OSS（图片/文件）

## 部署原则

开发阶段：Docker Compose 启动 MySQL、Redis、RabbitMQ、Elasticsearch。

正式部署：Linux + Docker Compose + Nginx，OSS 存图片和文件，不在应用服务器本地长期存储。

AI 服务独立于 Java 服务部署，使用内部 HTTP/SSE 通信。

暂不引入 Kubernetes/Nacos/Seata 等复杂组件，只有在实际遇到拆分需求时再升级。

# 4. 技术栈与职责分工

# 5. 功能模块设计

## 5.1 用户与认证

注册、登录、退出、个人信息、地址管理、角色权限。技术重点：Sa-Token + MySQL + Redis。

## 5.2 商品中心

分类、品牌、SPU、SKU、详情、上下架、库存。技术重点：MySQL + Redis + OSS。

## 5.3 搜索中心

全文搜索、筛选、排序、高亮、联想、热门词。技术重点：Elasticsearch + RabbitMQ。

## 5.4 购物车

增删改、勾选、数量变更、结算。技术重点：Caffeine + Redis，购物车以 Redis 为主，MySQL 持久化策略按业务选择。

## 5.5 订单中心

创建订单、取消、支付模拟、发货、收货。技术重点：MySQL + RabbitMQ + Redis。

## 5.6 营销中心

优惠券、满减/折扣规则、领取、使用、回滚。技术重点：Redis + MySQL + RabbitMQ。

## 5.7 秒杀中心

活动、预热、库存、限购、下单、风控。技术重点：Redis + Lua + MQ + 分布式锁。

## 5.8 售后中心

退款、退货、处理、状态流转。技术重点：MySQL + MQ。

## 5.9 评价中心

评价、追评、评分、图片。技术重点：MySQL + OSS。

## 5.10 运营中心

数据大盘、热销商品、订单统计。技术重点：MySQL/ES + ECharts。

## 5.11 AI中心

AI客服、导购、售后、运营助手。技术重点：Python Agent + Java Tools + RAG。

# 6. 核心数据库设计

## 核心表建议

## 数据库原则

金额统一使用 DECIMAL，不使用浮点数。

核心订单号、支付单号、业务流水号都使用唯一约束。

订单/库存等交易表保留必要的业务状态字段与更新时间。

对高频查询建立合理索引，任何索引都通过 EXPLAIN 验证。

秒杀、优惠券等高并发场景不能直接依赖“先查再改”的普通 SQL 模式。

# 7. Caffeine + Redis 多级缓存设计

## 缓存三类问题处理（L1 Caffeine + L2 Redis）

穿透：布隆过滤器或缓存空对象，关键接口增加参数校验。

击穿：热点 Key 互斥重建或逻辑过期 + 后台刷新。

雪崩：TTL 加随机抖动、热点预热、分批加载、限流。

## Redis 使用原则

缓存是性能组件，不把 Caffeine 或 Redis 当最终事实来源；交易最终一致性仍以 MySQL 与业务流水为基础。

Lua 主要用于需要 Redis 内原子组合操作的场景，例如“检查库存 + 扣减 + 记录用户限购”。

分布式锁用于并发控制，不用于替代数据库事务。

## 7.1 Caffeine + Redis 多级缓存架构

推荐采用 L1 本地缓存 Caffeine + L2 分布式缓存 Redis 的二级缓存结构。请求优先访问 Caffeine，未命中再访问 Redis，Redis 未命中才查询 MySQL；查询成功后按“回写 Redis -> 回写 Caffeine”的顺序更新缓存。这样可以显著降低热点商品查询的网络开销和 Redis QPS，同时保留 Redis 在多实例环境下的共享能力。

请求链路：Controller -> Caffeine(L1) -> Redis(L2) -> MySQL -> Redis回写 -> Caffeine回写。

L1 Caffeine：保存热点、短 TTL、容量受控的数据，例如商品详情、分类、品牌等高频只读数据。每个应用实例独立维护。

L2 Redis：保存跨实例共享的数据，TTL 更长，并承担分布式缓存、秒杀库存、幂等 Key、分布式锁等职责。

缓存一致性推荐：MySQL 更新成功后，通过业务事件或消息通知执行 Redis 删除/更新，再刷新本实例 Caffeine；多实例场景推荐通过 RabbitMQ 广播缓存失效事件，避免某一节点长期持有旧的 L1 数据。

缓存失效原则：业务方法先更新数据库，再通过 JetCache `@CacheInvalidate` 删除缓存，不使用延迟双删；禁止只更新 Caffeine 而不处理 Redis，商品价格、库存等交易敏感字段不要只依赖本地缓存。

热点保护：对 Caffeine 未命中且 Redis 未命中的热点 Key，使用 Redis 分布式锁或逻辑过期 + 后台刷新，避免多个实例同时回源 MySQL。

TTL 建议：Caffeine TTL 更短（例如 10~60 秒），Redis TTL 更长（例如 5~30 分钟），具体数值通过压测和业务更新频率调整。秒杀库存不走 Caffeine，以 Redis + Lua 作为并发控制事实入口。

# 8. RabbitMQ 设计

## 消息可靠性

生产端确认：开启 Publisher Confirm，失败进入重试/补偿机制。

消费端幂等：业务唯一键 + Redis短幂等Key + 数据库唯一约束。

消费失败：设置重试策略，达到次数后进入死信队列。

监控：记录消息ID、业务ID、消费次数、失败原因。

典型链路：

Redis预扣库存 -> 发送秒杀消息 -> Consumer消费 -> 创建订单 -> 更新业务状态

|

+-> 失败 -> Retry -> DLQ -> 人工/定时补偿

# 9. Elasticsearch 设计

商品索引建议：product_index。索引文档以 SKU/SPU 展示为中心，包含商品标题、品牌、分类、价格、销量、评价分、库存状态等可搜索/筛选字段。

## 索引同步策略

商品创建/更新/下架

|

+--> MySQL提交

|

+--> MQ发送商品变更事件

|

+--> ES Consumer

|

+--> upsert / delete index

说明：不要让前台请求同步完成 MySQL + ES 双写，否则失败补偿复杂。推荐 MySQL 作为业务事实来源，通过 MQ 异步维护 ES；必须允许短暂最终一致。

# 10. 秒杀与高并发方案

秒杀是整个项目的核心展示场景。推荐采用“前置校验 + Redis原子库存 + MQ削峰 + 异步订单 + 幂等/补偿”的架构。

海量请求

|

Nginx

|

IP/用户限流

|

Spring Boot API

|

+-----------+-----------+

|                       |

活动校验                 登录校验

|                       |

+-----------+-----------+

|

Redis Lua

检查时间/库存/限购并原子扣减

|

+---------+---------+

|                   |

失败                成功

|                   |

直接返回             MQ

|

异步订单Consumer

|

MySQL事务创建订单

|

库存/订单状态校验

|

最终一致/补偿

## 关键保护措施

接口限流：避免流量直接压垮应用。

商品详情等热点数据采用 Caffeine + Redis 多级缓存，秒杀库存单独预热到 Redis，减少瞬时访问 MySQL。

Lua：保证检查与扣减的原子性。

用户限购：Redis + DB 唯一约束双重保护。

MQ：削峰，避免瞬间创建大量订单。

订单状态与库存流水：出现异常时可追查、可补偿。

压测：必须有真实 JMeter 结果，记录吞吐与延迟。

## 分布式锁使用位置

锁不是秒杀吞吐的主手段。高并发库存扣减优先通过 Lua/原子操作完成；锁主要用于避免同一用户同一 SKU 重复下单、热点缓存重建、补偿任务并发执行等低粒度互斥场景。

# 11. 订单与库存一致性

普通订单推荐“数据库事务保障单次写入 + MQ做异步扩散 + 幂等保证重试安全”的模式。不要试图用分布式锁解决所有一致性问题。

## 订单状态机建议

WAIT_PAY -> PAID -> WAIT_DELIVER -> DELIVERED -> COMPLETED

|

+-> CANCELED

|

+-> AFTER_SALE  (从已支付/已完成等合法状态进入售后流程)

说明：状态流转必须通过服务层显式校验，禁止 Controller 直接修改 status。所有状态变更保留 order_log。

# 12. AI Agent 架构

AI 服务独立使用 Python + FastAPI。Java 是业务事实来源，Python Agent 负责理解用户请求、组织工具调用、RAG 检索和生成回答。

Vue3

|

+--> Java Spring Boot  -------> MySQL / Redis / ES

|            |

|            +--> /api/ai/chat  (SSE/HTTP)

|                           |

|                    Python FastAPI

|                           |

|              +------------+------------+

|              |            |            |

|          LangChain    LangGraph      RAG

|              |            |            |

|              +-----+------+------------+

|                    |

|                 LLM

|                    |

|            Tool Calling / Structured Output

|                    |

+--------------------+-------------------+

|

Java Business Tools

## 建议的 Agent 能力

## Java Tool API 原则

Tool 接口尽量语义清晰，例如 getOrder、searchProduct、getSalesStats，而不是暴露任意 SQL。

所有 Tool 都必须再次校验用户身份与权限，不能因为请求来自 AI 就绕过业务鉴权。

会改变业务状态的 Tool（例如创建售后、修改地址）应增加二次确认或严格规则。

敏感信息只返回 Agent 所需字段，避免把数据库对象原样暴露给 LLM。

# 13. 前端工程设计

前端技术栈明确为：Vue3 + Vite + Element Plus + Vue Router + Pinia + Axios + ECharts。

## Router 设计

src/router/

index.ts

routes.ts

核心路由：

/                  首页

/search            搜索

/product/:id       商品详情

/cart              购物车

/order             订单中心

/order/:id         订单详情

/coupon            优惠券

/seckill           秒杀

/after-sale        售后

/profile           个人中心

/ai                AI助手

/admin              后台

/admin/product     商品管理

/admin/order       订单管理

/admin/seckill     秒杀管理

## 路由守卫

未登录访问受保护页面 -> 跳转登录页，并保留 redirect。

后台路由 -> 根据 Sa-Token 返回的权限集合做前端展示控制；后端必须再次鉴权。

动态菜单只控制显示，不作为安全边界。

404、无权限页统一设计。

## 前端分层

src/

api/           Axios请求封装

router/        Vue Router

stores/        Pinia

views/         页面

components/    通用组件

layouts/       前后台布局

hooks/         可复用组合式函数

utils/         token、格式化、校验

types/         TS类型

assets/        静态资源

# 14. Java 项目工程结构

mallx/

├── mall-common/

│   ├── response/          # 统一响应

│   ├── exception/         # 全局异常

│   ├── util/              # 工具类

│   ├── idempotent/        # 幂等组件

│   └── config/            # 通用配置

├── mall-auth/             # Sa-Token认证

├── mall-user/             # 用户/地址

├── mall-product/          # SPU/SKU/分类/品牌

├── mall-cart/             # 购物车

├── mall-order/            # 订单/订单状态机

├── mall-payment/          # 支付模拟

├── mall-coupon/           # 优惠券

├── mall-seckill/          # 秒杀

├── mall-search/           # ES查询与同步

├── mall-after-sale/       # 售后

├── mall-admin/             # 后台接口

└── mall-ai-gateway/       # Java侧AI调用入口

## 每个业务模块内部推荐：

controller/

service/

service/impl/

mapper/

entity/

dto/

vo/

convert/

listener/

config/

原则：Controller 只做参数接收与响应；Service 承载业务规则；Mapper 负责持久化；Listener 负责 MQ 消费；DTO/VO 与 Entity 分离。

# 15. 开发规范与接口规范

## 统一返回

{

"code": 200,

"message": "success",

"data": {},

"requestId": "..."

}

## HTTP 约定

## 命名与工程规范

Controller 路径按领域命名，例如 /api/products、/api/orders。

业务异常使用自定义 BusinessException + 错误码。

日志必须带 requestId；订单、支付、消息必须带业务ID。

禁止直接把 Exception.getMessage 返回给前端。

所有外部调用设置超时、重试策略和失败日志。

涉及状态变化的接口必须设计幂等策略。

## OSS 约定

商品图、评价图、用户头像上传阿里云 OSS。

前端上传建议采用后端签名/临时凭证方案，不把永久 AK/SK 放在浏览器。

数据库只保存 OSS Key/URL 元数据，不保存文件二进制。

删除商品或用户资源时建立清理策略，避免对象存储垃圾。

# 16. 测试与压测方案

## 测试分层

## 秒杀压测建议指标

说明：压测结论必须来自你的真实实验。开发文档中预留“优化前/优化后”数据栏，完成压测后再填写。

## 压测流程

先测普通商品接口，确认基线。

再测单用户重复请求，验证幂等。

再测多用户秒杀，检查库存、订单、错误率。

逐步增加并发，观察 Redis、MySQL、MQ 瓶颈。

针对瓶颈优化后重新压测，并保留结果。

# 17. Docker 与部署

开发环境建议用 Docker Compose 统一中间件。

services:

mysql:

redis:

rabbitmq:

elasticsearch:

kibana:

mallx-api:

mallx-ai:

nginx:

## 环境变量

MYSQL_HOST=

MYSQL_PORT=

MYSQL_DATABASE=

MYSQL_USERNAME=

MYSQL_PASSWORD=

REDIS_HOST=

RABBITMQ_HOST=

ES_HOST=

OSS_ENDPOINT=

OSS_BUCKET=

OSS_ACCESS_KEY=

OSS_SECRET_KEY=

AI_BASE_URL=

AI_API_KEY=

说明：密钥只放 .env 或部署平台 Secret；不要提交到 Git。生产环境关闭详细 SQL 日志和敏感信息日志。

## 部署路径

浏览器

-> Nginx

-> Vue静态资源

-> /api -> Spring Boot

-> /ai  -> Java AI Gateway -> Python AI Service

Spring Boot -> MySQL / Redis / RabbitMQ / ES / OSS

Python AI -> LLM / Vector DB / Java Tools

# 18. 开发阶段与时间顺序

按照“先业务闭环，再中间件，再高并发，再 AI”的顺序开发。

## 建议开发节奏

每个阶段结束都打 Git Tag，例如 v0.1-user、v0.2-product。

先写 API 和数据模型，再写前后端页面，避免页面驱动数据库设计。

每完成一个核心模块，补充异常场景和幂等策略。

秒杀完成后立即做压测，不要等全部功能做完才压测。

AI 放在业务稳定之后，避免因为 Agent 开发阻塞核心交易链路。

# 19. 面试与简历呈现

## 项目介绍建议

“MallX 是我独立设计和开发的企业级高并发智能电商平台，Java 使用 Spring Boot、MyBatis-Plus、Sa-Token，Caffeine + Redis 负责多级缓存与并发控制，RabbitMQ 负责异步削峰和事件驱动，Elasticsearch 负责商品搜索；AI 部分独立使用 Python + LangChain/LangGraph，Agent 可以通过 Tool Calling 查询商品、订单、物流和售后信息，并结合 RAG 回答平台规则问题。”

## 必须能回答的技术问题

Caffeine 与 Redis 为什么要做 L1/L2 多级缓存？缓存更新时如何避免本地缓存脏数据？

为什么秒杀库存放 Redis？

为什么 Redis 扣库存还需要数据库？

Lua 解决了什么问题？

分布式锁在哪里用，为什么不用 synchronized？

RabbitMQ 消息丢失怎么处理？

RabbitMQ 重复消费怎么处理？

订单超时为什么使用 TTL + DLX？

缓存穿透、击穿、雪崩怎么区别？

MySQL 和 ES 为什么不是同步双写？

如何保证 MQ 与数据库最终一致？

如何设计接口幂等？

Sa-Token 和传统 JWT 方案如何选？

Vue Router 路由守卫与后端鉴权有什么区别？

为什么 AI Agent 要独立 Python 服务？

Agent 如何安全调用 Java 业务接口？

RAG 和 Tool Calling 分别解决什么问题？

## 简历项目亮点模板

MallX 企业级高并发智能电商平台

技术栈：Spring Boot 3、MyBatis-Plus、Sa-Token、MySQL、Caffeine、Redis、RabbitMQ、Elasticsearch、阿里云OSS、Vue3、Vue Router、LangChain/LangGraph

1. 设计Redis + RabbitMQ异步秒杀架构，通过Redis Lua原子扣减、用户限购、MQ削峰和幂等控制解决高并发库存超卖与重复下单问题。

2. 构建Caffeine + Redis 多级热点缓存体系，针对缓存穿透、击穿、雪崩设计空值缓存、互斥重建、随机TTL和热点预热策略。

3. 使用RabbitMQ实现订单超时取消、商品索引异步同步和订单事件处理，并通过Publisher Confirm、重试、死信队列和幂等消费提升消息可靠性。

4. 基于Elasticsearch实现商品全文搜索、筛选、排序和高亮，并通过消息事件异步维护搜索索引。

5. 独立Python AI Service，基于LangChain/LangGraph实现RAG与Tool Calling，使AI Agent能够查询订单、搜索商品和发起售后流程。

# 20. 最终验收标准

## 功能验收

用户可注册、登录、浏览商品、搜索、加购物车、下单、支付模拟、查询订单、申请售后。

管理员可管理商品、SKU、库存、订单、优惠券、秒杀和用户权限。

图片可上传阿里云 OSS，数据库只保存对象信息。

前端 Vue Router 支持前台/后台路由及登录、权限守卫。

## 技术验收

Sa-Token 认证授权能够正确区分用户/管理员权限。

Redis 缓存、分布式锁、幂等至少有三个真实业务场景。

RabbitMQ 至少覆盖异步下单、超时取消、ES同步中的两个场景。

Elasticsearch 能完成商品搜索、筛选、高亮。

秒杀能在压测中验证无超卖、无重复成功订单。

AI Agent 能通过 Tool Calling 调用 Java 真实接口，并有 RAG 知识库。

## 工程验收

代码有 README、接口文档、数据库脚本、Docker Compose、部署说明。

核心模块有单元/集成测试或可复现测试用例。

有一份真实压测报告和优化记录。

Git 提交有阶段性 Tag，README 有架构图、启动说明和项目截图。

## 最终目标

完成后的项目应该能够让你在面试中完整讲清楚：业务为什么这样设计、Redis 为什么这样用、RabbitMQ 解决了什么问题、秒杀怎么抗并发、如何保证幂等和最终一致性、ES 为什么异步同步，以及为什么把 Agent 独立成 Python 服务。做到这一层，项目价值就不再是“做过一个商城”，而是“做过一个包含真实后端工程问题的综合系统”。

# 附录：第一版开发清单

[ ] 创建 Git 仓库与 README

[ ] 初始化 Spring Boot 3 + MyBatis-Plus

[ ] 接入 Sa-Token 完成登录

[ ] 设计 RBAC 表结构

[ ] 初始化 Vue3 + Vite + Element Plus + Vue Router + Pinia

[ ] 准备 Docker Compose：MySQL/Redis/RabbitMQ/ES

[ ] 完成商品 SPU/SKU/分类/品牌

[ ] 接入阿里云 OSS 图片上传

[ ] 完成商品详情 Caffeine + Redis 多级缓存

[ ] 完成购物车

[ ] 完成订单与订单状态机

[ ] 完成支付模拟

[ ] 接入 RabbitMQ

[ ] 完成订单超时取消

[ ] 完成 ES 商品搜索

[ ] 完成优惠券

[ ] 完成秒杀

[ ] 完成 Lua 原子扣减

[ ] 完成幂等与分布式锁

[ ] 完成 JMeter 压测

[ ] 整理性能优化报告

[ ] 创建 Python FastAPI AI Service

[ ] 接入 LangChain

[ ] 完成 Tool Calling

[ ] 完成 RAG

[ ] 使用 LangGraph 编排复杂 Agent

[ ] 完成 AI 客服/导购/运营助手至少两个

[ ] Docker Compose 一键部署

[ ] 补齐 README/架构图/接口文档/压测报告/演示账号

### 表格 1
| 项目 | 内容 |
| --- | --- |
| 项目名 | MallX |
| 项目定位 | 企业级电商交易系统 / 高并发系统 / AI Agent 应用 |
| 主要后端 | Java + Spring Boot 3 + MyBatis-Plus + Sa-Token |
| 中间件 | Caffeine + Redis + RabbitMQ + Elasticsearch |
| 对象存储 | 阿里云 OSS |
| 前端 | Vue3 + Vite + Element Plus + Vue Router + Pinia + Axios + ECharts |
| AI服务 | Python + FastAPI + LangChain + LangGraph + RAG + Tool Calling |
| 部署 | Docker Compose + Nginx + Linux |
| 开发策略 | 先模块化单体，再按需要拆分核心服务 |
| 项目目标 | 能完整开发、部署、压测、讲解，并可作为 Java 后端实习主项目 |

### 表格 2
| 能力层 | 要证明的能力 | 对应模块 |
| --- | --- | --- |
| 业务能力 | 完整电商交易闭环 | 用户、商品、购物车、订单、支付、优惠券、售后、评价 |
| 数据能力 | 关系型数据库与查询优化 | MySQL、索引、事务、SQL优化 |
| 缓存能力 | 热点数据与高并发缓存 | Caffeine + Redis、多级缓存、缓存穿透/击穿/雪崩、热点预热 |
| 消息能力 | 异步解耦与最终一致 | RabbitMQ、TTL、DLX、幂等消费 |
| 并发能力 | 高并发库存与限购 | 秒杀、Redis原子操作、分布式锁、限流 |
| 搜索能力 | 商品全文搜索 | Elasticsearch、同步、筛选、排序、高亮 |
| 工程能力 | 前后端分离与部署 | Vue3、Vue Router、Docker、Nginx、Linux |
| AI能力 | AI与业务系统结合 | LangChain、LangGraph、RAG、Tool Calling、Agent |

### 表格 3
| 角色 | 主要能力 |
| --- | --- |
| 普通用户 | 浏览商品、搜索、购物车、下单、支付模拟、优惠券、评价、售后、AI客服/导购 |
| 运营管理员 | 商品、分类、品牌、SKU、库存、优惠券、秒杀、订单、售后、运营数据 |
| 客服/售后人员 | 处理售后单、查看订单、回复用户问题 |
| 系统管理员 | 用户、角色、权限、系统配置、审计日志 |

### 表格 4
| 技术 | 用途 | 必须掌握到的程度 |
| --- | --- | --- |
| Java 17+ | 后端主语言 | 集合、并发、IO、异常、JVM基础 |
| Spring Boot 3 | Web业务骨架 | IOC/AOP/事务/配置/自动装配 |
| MyBatis-Plus | ORM | CRUD、Wrapper、分页、逻辑删除、自动填充 |
| Sa-Token | 认证授权 | 登录、Token、权限、角色、注解鉴权、会话管理 |
| MySQL 9.20 | 核心交易数据 | 索引、事务、锁、执行计划、SQL优化 |
| Caffeine | L1 本地缓存 | 热点商品/分类/品牌本地缓存、短 TTL、maximumSize、refreshAfterWrite、缓存击穿保护 |
| Redis | 缓存/锁/高并发 | String/Hash/Set/ZSet、Lua、分布式锁、缓存策略 |
| RabbitMQ | 异步与削峰 | Exchange、Queue、Binding、确认、重试、TTL、DLX、幂等 |
| Elasticsearch | 商品搜索 | 索引、映射、分词、查询、聚合、同步 |
| 阿里云 OSS | 对象存储 | 图片上传、STS/签名URL、公共/私有资源管理 |
| Vue3 | C端/B端前端 | Composition API、组件化 |
| Vue Router | 前端路由 | 嵌套路由、动态路由、路由守卫、权限路由 |
| Pinia | 前端状态管理 | 用户、购物车、UI状态 |
| Element Plus | 后台UI | 表格、表单、分页、弹窗、上传 |
| ECharts | 数据可视化 | 运营大盘 |
| Python + FastAPI | AI Service | API、异步、Pydantic、服务化 |
| LangChain | Agent/RAG基础 | Model、Prompt、Retriever、Tool |
| LangGraph | 复杂Agent流程 | State、Node、Edge、条件分支、人机协同 |
| Docker | 环境与部署 | 镜像、网络、卷、Compose |
| Nginx | 反向代理与静态资源 | 代理、缓存、限流基础 |

### 表格 5
| 领域 | 表 | 关键字段/说明 |
| --- | --- | --- |
| 认证 | sys_user | id、username、password、status、create_time |
| 认证 | sys_role / sys_permission / sys_user_role / sys_role_permission | RBAC |
| 商品 | product_category | id、parent_id、name、sort、status |
| 商品 | product_spu | id、category_id、brand_id、title、sub_title、detail、status |
| 商品 | product_sku | id、spu_id、spec_json、price、stock、sale_count、status |
| 库存 | product_stock_log | sku_id、change_type、change_qty、biz_id |
| 订单 | orders | order_no、user_id、total_amount、pay_amount、status、expire_time |
| 订单 | order_item | order_id、sku_id、price、quantity、snapshot_json |
| 支付 | payment_record | order_no、pay_no、amount、status、paid_time |
| 营销 | coupon_template | name、type、discount、stock、start_time、end_time |
| 营销 | coupon_user | coupon_id、user_id、status、order_id |
| 秒杀 | seckill_activity / seckill_sku | activity、sku、price、stock、limit_count |
| 售后 | after_sale | after_no、order_id、user_id、type、reason、status |
| 评价 | product_comment | order_item_id、sku_id、user_id、score、content |
| 系统 | operation_log | user_id、module、action、request_id、result、create_time |

### 表格 6
| Key / L1缓存名 | 用途 | 策略 |
| --- | --- | --- |
| mall:product:detail:{skuId} | 商品详情 L2 缓存 | Redis TTL + 热点预热 |
| mall:product:spu:{spuId} | SPU L2 缓存 | Redis TTL + 主动失效 |
| mall:cart:{userId} | 购物车 | Redis Hash/JSON，按用户隔离 |
| mall:seckill:stock:{skuId} | 秒杀库存 | Redis warm-up + Lua atomic deduction; no Caffeine |
| mall:seckill:user:{userId}:{skuId} | 秒杀限购 | Redis Set/Bitmap/Key，按业务选择 |
| mall:lock:order:{orderNo} | 订单相关短锁 | 优先 Redisson；必要时封装锁服务 |
| mall:idempotent:{requestId} | 幂等控制 | 短 TTL |
| mall:search:hot | 热门搜索词 | ZSet |
| mall:login:rate:{ip} | 登录/接口限流 | 计数器 + TTL |
| Caffeine:product:detail:{skuId} | 商品详情 L1 热点缓存 | 短 TTL + maximumSize + refreshAfterWrite/失效 |
| Caffeine:product:spu:{spuId} | SPU L1 热点缓存 | 短 TTL + 容量控制 |

### 表格 7
| 场景 | Exchange/Queue | 用途 |
| --- | --- | --- |
| 秒杀下单 | seckill.exchange / seckill.order.queue | 削峰、异步创建订单 |
| 订单超时 | order.delay / order.timeout.queue | TTL + DLX实现延迟处理 |
| ES同步 | product.exchange / product.es.queue | 商品变更异步同步搜索索引 |
| 订单事件 | order.event / order.event.queue | 积分、通知、统计等异步处理 |
| 售后事件 | after_sale.exchange / after_sale.queue | 售后状态变更异步任务 |

### 表格 8
| 能力 | 实现方式 |
| --- | --- |
| 全文搜索 | title、sub_title、brand_name 等字段分词 |
| 筛选 | category_id、brand_id、price、stock_status 等 Filter |
| 排序 | 综合相关度、销量、价格、上架时间 |
| 高亮 | 关键词字段 highlight |
| 搜索建议 | 前缀匹配/联想词索引 |
| 热词统计 | Redis ZSet 或 ES 聚合 |

### 表格 9
| 问题 | 方案 |
| --- | --- |
| 重复提交订单 | requestId + Redis幂等Key + 业务唯一约束 |
| 库存超卖 | 数据库条件扣减或 Redis 原子扣减；高并发场景以 Redis 为前置控制 |
| MQ重复消费 | 消息ID/业务ID幂等 + DB唯一键 |
| 支付重复回调 | 支付流水唯一约束 + 状态机 |
| 超时未支付 | RabbitMQ TTL/DLX触发取消任务 |
| 取消后恢复库存 | 库存流水 + 幂等恢复操作 |
| MySQL与ES不一致 | MQ异步同步 + 重试 + 定时校准任务 |

### 表格 10
| Agent | 示例问题 | Tool/RAG |
| --- | --- | --- |
| AI客服 | 我的订单什么时候到？ | get_order、get_logistics |
| AI导购 | 3000元以内推荐游戏手机 | search_product、get_product_detail、compare_product |
| AI售后 | 耳机坏了怎么退？ | get_order、get_after_sale_policy、create_after_sale |
| AI运营 | 近7天销量最高的商品有哪些？ | get_sales_stats、get_top_products |
| 知识问答 | 什么情况下可以退款？ | RAG检索平台规则 |

### 表格 11
| 方法 | 语义 | 示例 |
| --- | --- | --- |
| GET | 查询 | GET /api/products/{id} |
| POST | 新增/创建动作 | POST /api/orders |
| PUT | 完整更新 | PUT /api/user/address/{id} |
| PATCH | 局部更新 | PATCH /api/cart/items/{id} |
| DELETE | 删除 | DELETE /api/cart/items/{id} |

### 表格 12
| 层级 | 内容 | 工具/方式 |
| --- | --- | --- |
| 单元测试 | Service核心规则、状态机、价格/优惠券计算 | JUnit 5 |
| 集成测试 | Redis、RabbitMQ、MySQL、ES关键链路 | Testcontainers 或 Docker测试环境 |
| 接口测试 | 登录、商品、订单、优惠券、秒杀 | Apifox/JMeter |
| 并发测试 | 秒杀、优惠券领取、热点商品 | JMeter |
| 异常测试 | MQ重复、消费失败、Redis异常、ES延迟 | 故障注入/手工模拟 |
| 回归测试 | 核心业务链路 | 固定用例集 |

### 表格 13
| 指标 | 记录方式 |
| --- | --- |
| 吞吐量 | Requests/sec |
| 成功率 | 成功请求/总请求 |
| 平均响应 | Avg |
| P95/P99 | 长尾延迟 |
| 数据库连接数 | 连接池监控 |
| Redis QPS | Redis监控 |
| MQ堆积 | Queue depth |
| 超卖数量 | 结果校验：必须为0 |

### 表格 14
| 阶段 | 目标 | 主要工作 | 完成标志 |
| --- | --- | --- | --- |
| Phase 0 | 项目初始化 | Git、Maven、Vue、Docker Compose、开发规范 | 项目可启动 |
| Phase 1 | 认证基础 | 用户、Sa-Token、角色权限、地址 | 可登录且权限正确 |
| Phase 2 | 商品中心 | SPU/SKU、分类、品牌、OSS图片 | 商品上下架闭环 |
| Phase 3 | 购物车 | 购物车、结算页、价格汇总 | 能从商品到购物车 |
| Phase 4 | 订单中心 | 订单、支付模拟、状态机 | 普通下单闭环 |
| Phase 5 | 多级缓存 | Caffeine L1、Redis L2、商品缓存、购物车、幂等、热点缓存、一致性失效 | 核心查询走 Caffeine + Redis 两级缓存 |
| Phase 6 | RabbitMQ | 订单事件、ES同步、延迟取消 | 异步链路稳定 |
| Phase 7 | ES | 商品同步、搜索、筛选、排序 | 搜索体验完整 |
| Phase 8 | 优惠券 | 领取、使用、回滚、幂等 | 营销链路稳定 |
| Phase 9 | 秒杀 | Redis库存、Lua、MQ、限购、锁、限流 | 秒杀压测通过 |
| Phase 10 | 性能优化 | JMeter、Caffeine/Redis命中率、SQL/Redis/MQ优化、监控 | 有真实压测报告 |
| Phase 11 | AI | FastAPI、LangChain、Tool、RAG、LangGraph | Agent可调用真实业务 |
| Phase 12 | 部署与打磨 | Docker、Nginx、文档、演示数据 | 可一键部署/演示 |
