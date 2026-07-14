# Java 后端大厂暑期准备

## Goal

- 将 Java 确立为主语言，达到大厂后端实习/校招面试可深入追问的水平。
- 用真实的大烨实习业务场景复盘传统 Java 工程能力，但不虚构当时没有使用的组件。
- 完成一个可公开验证的消息通知平台，并补充向量数据库与 RAG 工程能力。
- 暑假结束后具备冲击互联网中大厂和先进制造软件岗位的完整准备度。

## Positioning

目标定位：

> Java 后端工程师，具备企业级 Agent 平台经历、消息系统与可靠性工程能力，并能实现 RAG/Agent 应用的服务端落地。

## Resume Integrity Rule

简历内容分成两层：

1. 实习事实：只写大烨期间真实参与的 Android 性能优化、Python/Java 消息通知、实例分配和模板配置。
2. 暑期补强：将匿名化业务需求重新实现为独立 Java 工程项目，明确标注“独立重构/场景复现”，用于证明 MySQL、Redis、消息队列和 RAG 能力。

没有证据证明在实习中使用过的技术，不得写成实习技术栈。

## Summer Outcomes

- 1 个 Java 消息通知平台，具备 MySQL、Redis、Kafka、并发、幂等、重试、限流和可观测性；RocketMQ 作为岗位选型对照。
- 1 个可独立运行的 RAG 知识服务，具备向量检索、混合召回、重排、引用和评测。
- 1 套大模型原理与 AI 应用工程模块，覆盖 Transformer、推理、Embedding、RAG、Tool Calling、Agent、评测与安全。
- 1 套容器与多机部署模块，覆盖 Docker 原理、Kubernetes 入门和真实故障处理。
- 1 套分布式系统模块，覆盖多实例幂等、分布式限流、任务分片、服务发现、消息一致性和故障恢复。
- 40 个 Java/后端核心问题的口述卡片与最小实验。
- 80-100 道算法题，其中至少 30 道能限时独立完成并讲清复杂度。
- 2 版简历：Java 后端版、Agent/AI 应用后端版。
- 3 次完整模拟面试和 1 次项目架构答辩。

## Current Status

- 阶段：Phase 0 - 事实核验与基线
- 优先级：Highest
- 最近更新：2026-07-14
- 预计周期：8 周
- 课程形态：后端基础 + AI 应用 + 容器平台 + 分布式工程 + 求职输出

## Evidence Basis

- 企业 JD 研究：`../../06-research/agent-backend-job-market/`
- 近期 Java 面经采样：牛客 Java 后端/实习搜索结果，2026-07-13。
- 总体求职计划：`../big-tech-summer-preparation/`

## Course Production

- 课程设计：`course-design.md`
- 章节模板：`lesson-template.md`
- 资料地图：`resource-map.md`
- 制作路线：`production-roadmap.md`
- 通用生产规范：`../../05-meta/course-production-system.md`

## Next 3 Actions

1. 按 `projects.md` 完成大烨实习事实台账，区分真实工作和暑期重构。
2. 使用 `qa.md` 做一次闭卷基线测试，标记不会和只能背诵的内容。
3. 以 Kafka 冻结消息平台 MVP，RocketMQ 只做选型对照，不同时堆两套 MQ。
