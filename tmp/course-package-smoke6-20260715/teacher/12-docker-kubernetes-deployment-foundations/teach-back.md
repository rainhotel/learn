# 第 12 章 Teach-back

## 5 分钟：容器不是虚拟机

画出 namespace、cgroup、镜像层和 writable layer，说明它们对 NotifyFlow 的进程、资源和状态数据有什么影响。

## 15 分钟：Pod Running 但无流量

按监听地址 -> readiness -> Service selector -> EndpointSlice -> 端口 -> NetworkPolicy 的顺序演示排查，最后说明为什么不能直接重启全部 Pod。

## 45 分钟：一次安全发布

构建扫描 -> 非 root 镜像 -> Compose 验证 -> Deployment -> startup/readiness -> 小流量 -> 指标/lag/数据正确性 -> 回滚 -> 事故复盘。

## 验收

- 能区分启动、就绪、存活。
- 能解释 requests/limits 与 JVM heap 外内存。
- 能说出至少三个 k8s 故障的证据链。
- 能明确 Agent 只读、审批、审计和 kill switch。
