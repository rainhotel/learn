# 第 12 章练习

1. 解释 namespace、cgroup、overlayfs 与虚拟机的差异。
2. 为 Java 21 应用设计多阶段 Dockerfile，说明每层缓存和 Secret 风险。
3. Compose 中 `depends_on` 为什么不能保证 MySQL 已经可用？
4. readiness 与 liveness 如何区分？供应商 503 应影响哪一个？
5. Pod Running 但 Service 无流量，给出五步排查顺序。
6. 设计 NotifyFlow worker 的 requests/limits，并说明 heap 外内存预算。
7. Kafka lag 上升时，为什么盲目 HPA 可能制造 Provider 重试风暴？
8. 设计一次 Deployment 滚动发布、验证、回滚和数据库兼容流程。
9. 为 `/internal/lab/recovery/drain-one` 设计 namespace、RBAC、租户和 kill switch。
10. 编写一个 Agent 事故摘要，必须包含证据引用、反例和人工审批点。
11. 设计节点 OOM、ImagePullBackOff、CrashLoopBackOff 三个实验的停止条件。
12. 写一段简历描述，区分“设计 Kubernetes 部署”和“真实运行并验证”。

## 作业提交

- Dockerfile 与构建说明。
- Compose 依赖图和健康检查表。
- Deployment/Service/ConfigMap/Secret 草案。
- 发布回滚时间线。
- 故障排查 Runbook 和 Agent 安全边界。
