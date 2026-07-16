# 第 12 章实验：容器与 Kubernetes 部署

## 当前状态

- 状态：Pending
- 已完成：实验矩阵、断言、停止条件、证据目录和安全边界设计
- 未完成：Docker Engine、Compose、kind/minikube 或集群运行

## 实验矩阵

1. 多阶段 Java 21 镜像、非 root、只读文件系统和漏洞/SBOM 扫描。
2. 镜像层缓存与构建上下文泄露对照。
3. Compose MySQL/Redis/Kafka/NotifyFlow 健康检查和数据正确性。
4. Kubernetes Deployment、Service、ConfigMap、Secret 和三种探针。
5. 滚动发布、readiness gate、回滚与数据库/消息兼容。
6. requests/limits、容器 OOM、JVM heap 外内存和 HPA 边界。
7. Service selector、EndpointSlice、DNS、端口和 NetworkPolicy 故障。
8. 节点不可用、消费者 lag、暂停/恢复和数据正确性。

## 每组证据

```text
evidence/<experiment>/
  versions.md
  manifests/
  commands.txt
  stdout.log
  events.txt
  metrics.json
  logs.txt
  correctness.sql
  timeline.md
  conclusion.md
```

## 安全与停止条件

- 使用独立 namespace、测试租户和非生产数据。
- OOM、节点故障和消费者暂停必须有自动停止与清理步骤。
- Secret 不进入 Git、镜像、stdout 或截图。
- 不在 Docker Engine 未启动时声称 Compose 运行通过。
- 不在没有数据正确性查询时声称“零丢失/零停机”。

## 发布门槛

每组实验需有固定版本、原始命令输出、事件/日志、指标、业务正确性和限制说明，才能从 Pending 进入 Lab Verified。
