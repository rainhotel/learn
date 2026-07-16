# 第 12 章：Docker、Kubernetes 与 NotifyFlow 部署基础

## 章节定位

- 类型：Concept + Deployment Design + Lab Design + Incident + Interview + Teach-back
- 难度：深入
- 建议学习时间：24-32 小时
- 先修章节：第 09 章可观测性、第 10 章 JVM、第 11 章网络/连接池（初稿）
- 对应项目：NotifyFlow 容器化、Compose 本地环境、Kubernetes 发布与故障排查

## 当前状态

- 阶段：八件套完整内容初稿，实验 Pending
- 调研日期：2026-07-15
- 已完成：Docker 原理、Compose、Kubernetes 核心对象、发布/探针/资源/排障讲义与实验矩阵设计
- 未完成：Docker Engine、Compose、Kubernetes 集群和真实部署证据

本章不能标记为 Lab Verified、Release Candidate 或 Released。

## 相邻章节边界

- 第 09-10 章证明应用行为、JVM 和故障证据。
- 第 12 章解释进程如何被打包、隔离、调度、探活、发布和回滚。
- 第 13 章再讨论系统设计与项目答辩，不把 YAML 背诵当成系统设计。

## 核心问题

1. Docker 的 namespace、cgroup、overlay filesystem 和 image layer 分别解决什么问题？
2. 为什么容器不是虚拟机？镜像层和 writable layer 如何影响构建与运行？
3. 如何写非 root、可复现、有健康检查的 Java 镜像？
4. Compose 如何复现 NotifyFlow 的 MySQL、Redis、Kafka 和应用依赖？
5. Pod、Deployment、Service、ConfigMap、Secret 的责任边界是什么？
6. readiness、liveness、startup probe 为什么不能混用？
7. requests/limits、滚动发布、回滚和 HPA 如何影响可用性？
8. Pod 运行但请求失败时，如何沿 DNS、Service、Endpoint、容器端口和应用日志排查？
9. 如何在多实例下避免重复领取、错误扩容和恢复风暴？
10. Agent 可以怎样辅助 Kubernetes 事故分析而不执行高风险操作？

## 退出标准

- 能解释镜像构建上下文、层缓存、容器进程和资源限制。
- 能写一个非 root Java 21 镜像并说明 JVM 容器参数边界。
- 能用 Compose 描述 NotifyFlow 的本地依赖和健康顺序。
- 能解释 Pod/Deployment/Service/ConfigMap/Secret 和探针。
- 能完成一次 Deployment 滚动发布、回滚和故障排查设计。
- 能依据 requests/limits、队列、P99 和 backlog 讨论扩容，而不是只看 CPU。
- 能指出 Secret、日志、镜像和管理端点的安全风险。
- 能为 Agent 设计只读诊断、证据引用、审批和 kill switch。

## 发布前缺口

- 固定 Docker/Compose/Kubernetes 版本并完成本地运行。
- 构建并扫描 NotifyFlow 非 root 镜像。
- 运行 Compose 全链路并保存健康、日志和数据正确性证据。
- 在 kind/minikube 或真实集群完成部署、探针、滚动发布、回滚和 DNS 排障。
- 完成资源限制、OOM、节点不可用、Service 错配和恢复实验。
- 完成学习者作业与 Teach-back。
