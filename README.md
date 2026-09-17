# C2C 二手交易平台后端

基于 Spring Boot 3、Spring Cloud 和 Java 17 实现的 C2C 二手交易平台后端。项目采用微服务架构，提供用户、商品、订单、支付、评价、购物车和即时聊天等能力。

前端项目位于 [c2c-mobile](https://github.com/ppp110888/c2c-mobile)。

## 功能概览

- 用户注册、登录、JWT 身份认证和个人资料管理
- 收货地址、关注、粉丝及浏览记录
- 商品发布、编辑、详情、库存和图片上传
- Elasticsearch 商品检索与高亮
- AI 根据商品图片生成标题或描述
- Redis 购物车、首页缓存和分布式库存锁
- 订单创建、取消、超时关闭和库存补偿
- 支付宝沙箱支付、异步通知验签和订单金额校验
- 买卖双方评价及重复评价限制
- WebSocket 实时聊天、离线消息和未读数量
- RocketMQ 异步同步商品搜索索引

## 技术栈

| 组件 | 用途 |
| --- | --- |
| Spring Boot 3.2 / Spring Cloud 2023 | 应用与微服务基础框架 |
| Spring Cloud Gateway | 统一入口、JWT 校验和身份透传 |
| Nacos | 服务注册与发现 |
| MySQL / MyBatis-Plus | 用户、商品和订单数据 |
| Redis / Redisson | 缓存、购物车、锁和延时队列 |
| MongoDB / Netty WebSocket | 聊天记录和实时通信 |
| Elasticsearch | 商品搜索 |
| MinIO | 商品图片存储 |
| RocketMQ | 商品索引异步同步 |
| Alipay SDK | 支付宝沙箱支付 |

## 项目结构

```text
c2c-platform-parent/
├── c2c-common/                 # 统一返回、异常与 JWT 工具
├── c2c-gateway/                # API 网关，端口 8080
├── c2c-service/
│   ├── user-service/           # 用户服务，端口 8081
│   ├── item-service/           # 商品服务，端口 8082
│   ├── order-service/          # 订单服务，端口 8083
│   └── message-service/        # 消息 HTTP 端口 8084，WebSocket 端口 9090
├── docker/                     # MySQL 初始化和 RocketMQ 配置
├── docker-compose.yml          # 本地中间件编排
└── .env.example                # 环境变量模板
```

业务服务仅监听 `127.0.0.1`，外部 HTTP 请求应统一经过 `8080` 网关。服务间 HTTP 请求还需要内部凭据，不能直接信任客户端提交的 `X-User-Id`。

## 环境要求

- JDK 17
- Maven 3.9+
- Docker Desktop 或兼容的 Docker Compose 环境
- IDEA 2023+，可选
- 支付功能需要支付宝沙箱应用
- AI 功能需要兼容接口的 API Key

## 首次配置

复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

必须替换 `.env` 中的全部占位值：

| 变量 | 说明 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | Docker MySQL root 密码 |
| `MYSQL_PASSWORD` | Java 服务连接 MySQL 的密码，当前应与 root 密码一致 |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | Docker MinIO 管理凭据 |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | Java 服务访问 MinIO 的凭据 |
| `JWT_SECRET` | JWT 签名密钥，至少 32 个随机字符 |
| `INTERNAL_API_SECRET` | 网关与微服务间的共享凭据，至少 32 个随机字符 |
| `AI_API_KEY` | AI 图片识别服务密钥 |
| `ALIPAY_APP_ID` | 支付宝沙箱应用 ID |
| `ALIPAY_PRIVATE_KEY` | 应用私钥 |
| `ALIPAY_PUBLIC_KEY` | 支付宝公钥 |

> `.env` 只会被 Docker Compose 自动读取。直接从 IDEA 或命令行启动 Java 服务时，还必须把同一组变量加入 IDEA Run Configuration 或当前 PowerShell 会话。

PowerShell 临时配置示例：

```powershell
$env:MYSQL_PASSWORD = "your-password"
$env:MINIO_ACCESS_KEY = "your-minio-user"
$env:MINIO_SECRET_KEY = "your-minio-password"
$env:JWT_SECRET = "at-least-32-random-characters"
$env:INTERNAL_API_SECRET = "another-32-character-random-secret"
$env:AI_API_KEY = "your-api-key"
$env:ALIPAY_APP_ID = "your-app-id"
$env:ALIPAY_PRIVATE_KEY = "your-private-key"
$env:ALIPAY_PUBLIC_KEY = "alipay-public-key"
```

## 本地启动

1. 启动中间件：

```powershell
docker compose up -d
docker compose ps
```

2. 等待 MySQL、Redis、MongoDB、Elasticsearch、MinIO、Nacos 和 RocketMQ 就绪。

3. 在 IDEA 中按顺序启动：

```text
UserApplication
ItemApplication
OrderApplication
MessageApplication
GatewayApplication
```

4. 网关地址为 `http://localhost:8080`，WebSocket 地址为 `ws://localhost:9090/ws`。

数据库表会在 MySQL 数据卷首次创建时由 `docker/init.sql` 初始化。修改初始化脚本不会自动更新已有数据卷；开发环境需要重新初始化时可执行：

```powershell
docker compose down -v
docker compose up -d
```

该命令会删除 Docker 中的本地数据库数据，请确认无需保留后再执行。

## 测试与构建

```powershell
mvn test
mvn clean package
```

当前自动化测试覆盖密码哈希校验，以及订单使用服务端价格、拒绝越权地址等关键安全场景。支付宝、WebSocket 和全部中间件仍需要在完整本地环境中做集成测试。

## 支付说明

- 订单价格和卖家信息由商品服务返回，客户端提交的金额不会作为支付金额。
- 支付宝同步返回页面只负责跳转，不会直接修改订单状态。
- 只有验签成功，并且应用 ID、订单号和支付金额均匹配的异步通知才能完成订单支付。
- `alipay.notifyUrl` 必须配置为支付宝能够访问的公网 HTTPS 地址。
- 本地联调可使用合规的内网穿透地址，但不要关闭验签。

## 修改后的兼容性变化

相较于旧版本，启动和支付流程有以下变化：

1. 不再提供硬编码密码或密钥，缺少必要环境变量时服务会拒绝启动。
2. 微服务端口只监听本机，并校验来自网关或其他服务的内部凭据。
3. 地址详情、修改和删除均验证当前用户所有权。
4. 库存变更与 ES 全量同步接口仅允许内部调用。
5. 支付结果必须经过支付宝异步通知验签，旧的同步跳转“直接支付成功”行为已移除。
6. 上传文件限制为 5 MB 内的 JPG、PNG 或 WebP，并校验文件签名。
7. 数据库初始化脚本不再包含历史用户、地址、订单或密码哈希样例数据。

因此，旧环境若没有补齐环境变量和支付宝异步回调配置，不能直接按原启动方式运行。

## 安全注意事项

- `.env` 已被 Git 忽略，不要把真实配置写回 `application.yml`。
- 曾出现在旧本地历史中的 API Key、支付宝私钥、数据库密码、MinIO 凭据和 JWT 密钥都应在对应平台轮换。
- 生产环境只开放网关和经过 TLS 的 WebSocket 入口。
- Redis、MongoDB、Elasticsearch、Nacos、MinIO 管理端和 RocketMQ 不应暴露到公网。

## 相关仓库

- 后端：https://github.com/ppp110888/c2c-platform-backend
- 移动端：https://github.com/ppp110888/c2c-mobile
