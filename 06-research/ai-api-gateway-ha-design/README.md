# High Availability AI API Gateway Design

## Research Goal

- 调研当前开源 AI API Gateway / LLM Gateway 项目，理解市场上成熟方案的功能边界。
- 提炼一个高可用 AI API 网关的核心设计，用于后续做成可写进简历的项目。
- 目标不是做一个简单 OpenAI 反向代理，而是做出能体现后端系统设计能力的网关。

## Scope

- 包含开源项目扫描：LLM 专用网关、传统 API 网关的 AI 能力、LLMOps 网关。
- 包含高可用能力拆解：路由、重试、降级、熔断、限流、预算、观测、部署。
- 包含简历项目建议：MVP 范围、进阶功能、压测指标和简历表述。
- 不包含灰色中转、绕过订阅限制、账号池滥用等方向。
- 不包含直接复制开源项目代码。

## Research Questions

- 核心问题 1：市面上主流开源 AI API 网关分成哪些类型？
- 核心问题 2：一个高可用 AI API 网关必须有哪些工程能力？
- 核心问题 3：作为简历项目，做哪些功能最能体现技术含量？

## Current Status

- 阶段：Project direction selected
- 最近一次更新：2026-06-18

## Expected Output

- 一份可持续更新的开源项目对比表。
- 一套高可用 AI API 网关的架构草图和模块拆解。
- 一个适合个人实现的简历项目路线。

## Selected Resume Project

- 项目名：`ModelMesh Gateway`
- 定位：High-Availability OpenAI-Compatible AI Gateway
- 推荐技术栈：Java 21 + Spring Boot WebFlux + Redis + PostgreSQL + Prometheus/Grafana
- 详细计划：`resume-project-plan.md`

## Related Research

- `06-research/ai-gateway-open-source-contribution`: 偏向开源贡献切入点。

## Human And AI Views

- 给人看：`human-brief.md`
- 给 AI 看：`ai-context.md`
- 材料来源：`source-log.md`
- 过程笔记：`working-notes.md`
- 研究结论：`conclusion.md`
- 简历项目计划：`resume-project-plan.md`
