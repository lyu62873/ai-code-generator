# AI Code Generator

[简体中文](README.zh-CN.md)

An AI-powered backend service for generating app code, streaming responses, and managing app lifecycle (create, deploy, download) with user auth and rate limiting.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Highlights](#api-highlights)
- [Testing](#testing)
- [Security Notes](#security-notes)

## Overview

This project is a Spring Boot service that:

- supports AI-assisted code generation workflows;
- exposes SSE endpoints for streaming generation output;
- stores app/user data with MyBatis-Flex + MySQL;
- integrates Redis-based session and rate limiting;
- supports deploy and ZIP download for generated projects.

## Features

- **Code generation modes**: `html`, `multi_file`, `vue_project`
- **App lifecycle**: create, update, query, deploy, download
- **Streaming output**: SSE endpoint for chat-to-code
- **Security controls**: login/session, role checks, rate limiter
- **AI workflow extensions**: prompt routing, quality check, image-related tools

## Tech Stack

- Java 21
- Spring Boot 3
- MyBatis-Flex
- MySQL
- Redis + Spring Session + Redisson
- LangChain4j (+ OpenAI-compatible model providers)
- Maven

## Project Structure

```text
src/main/java/com/leyu/aicodegenerator
├── ai/                  # AI services and tool integrations
├── controller/          # REST API controllers
├── core/                # code generation/parsing/saving core modules
├── entity/mapper/service
├── langgraph4j/         # local experimental workflow package (git-ignored)
└── ratelimiter/         # annotation + aspect based rate limit
```

## Quick Start

1. **Prerequisites**
   - JDK 21
   - Maven 3.9+
   - MySQL + Redis
2. **Clone & run**

```bash
git clone <your-repo-url>
cd ai-code-generator
mvn spring-boot:run
```

3. **Default server**
   - Base URL: `http://localhost:8123`
   - Context path: `/api`

## Configuration

Active profile in `src/main/resources/application.yaml` is `local`.

Local secrets are expected in `application-local.yaml` (already ignored by git).  
Recommended setup:

- keep real credentials only in local/private config;
- use environment variables or CI secrets for production;
- rotate any previously exposed keys before publishing.

## API Highlights

- User: `/api/user/register`, `/api/user/login`, `/api/user/get/login`
- App: `/api/app/add`, `/api/app/my/list/page/vo`, `/api/app/get/vo`
- AI generation (SSE): `/api/app/chat/gen/code`
- Deploy: `/api/app/deploy`
- Download ZIP: `/api/app/download/{appId}`

## Testing

Run all tests:

```bash
mvn test
```

## Security Notes

- `src/main/resources/application-local.yaml` contains sensitive values and is ignored by `.gitignore`.
- Before pushing to public GitHub, verify no real credentials exist in tracked files.
- If credentials were ever committed in history, rotate them and clean history before publishing.

