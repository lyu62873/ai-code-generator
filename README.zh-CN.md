# AI Code Generator（中文说明）

[English](README.md)

这是一个 AI 驱动的后端服务，用于生成应用代码，并提供流式生成、应用管理（创建/部署/下载）、用户鉴权与限流能力。

## 目录

- [项目概览](#项目概览)
- [核心功能](#核心功能)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [主要接口](#主要接口)
- [测试](#测试)
- [安全提示](#安全提示)

## 项目概览

该项目基于 Spring Boot，主要提供：

- AI 辅助代码生成工作流；
- 基于 SSE 的流式生成结果输出；
- MyBatis-Flex + MySQL 的应用/用户数据存储；
- 基于 Redis 的会话管理与限流；
- 生成后项目部署与 ZIP 下载能力。

## 核心功能

- **代码生成模式**：`html`、`multi_file`、`vue_project`
- **应用生命周期管理**：创建、更新、查询、部署、下载
- **流式输出**：chat-to-code 的 SSE 接口
- **安全控制**：登录会话、角色鉴权、接口限流
- **AI 扩展能力**：路由分发、质量检查、图像相关工具

## 技术栈

- Java 21
- Spring Boot 3
- MyBatis-Flex
- MySQL
- Redis + Spring Session + Redisson
- LangChain4j（兼容 OpenAI 风格模型）
- Maven

## 项目结构

```text
src/main/java/com/leyu/aicodegenerator
├── ai/                  # AI 服务与工具集成
├── controller/          # REST API 控制器
├── core/                # 代码生成/解析/落盘核心模块
├── entity/mapper/service
├── langgraph4j/         # 本地实验工作流包（已被 git ignore）
└── ratelimiter/         # 注解 + AOP 限流模块
```

## 快速开始

1. **环境准备**
   - JDK 21
   - Maven 3.9+
   - MySQL + Redis
2. **拉取并运行**

```bash
git clone <your-repo-url>
cd ai-code-generator
mvn spring-boot:run
```

3. **默认访问信息**
   - 服务地址：`http://localhost:8123`
   - 上下文路径：`/api`

## 配置说明

`src/main/resources/application.yaml` 当前激活配置为 `local`。

本地敏感配置建议放在 `application-local.yaml`（该文件已被 git 忽略）。  
建议实践：

- 真实密钥仅放在本地/私有配置；
- 生产环境通过环境变量或 CI Secret 注入；
- 公网发布前请轮转历史暴露过的密钥。

## 主要接口

- 用户：`/api/user/register`、`/api/user/login`、`/api/user/get/login`
- 应用：`/api/app/add`、`/api/app/my/list/page/vo`、`/api/app/get/vo`
- AI 生成（SSE）：`/api/app/chat/gen/code`
- 部署：`/api/app/deploy`
- 下载 ZIP：`/api/app/download/{appId}`

## 测试

运行全部测试：

```bash
mvn test
```

## 安全提示

- `src/main/resources/application-local.yaml` 包含敏感信息且已被 `.gitignore` 忽略。
- push 到 GitHub 前请再次确认跟踪文件中不包含真实凭据。
- 若历史提交中曾出现真实密钥，请先轮转密钥并清理提交历史再公开仓库。

