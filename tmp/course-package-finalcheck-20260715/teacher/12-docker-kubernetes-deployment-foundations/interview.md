# 第 12 章面试追问

## 容器和虚拟机的区别？

容器通常共享宿主机内核，通过 namespace/cgroup 隔离视图和资源；虚拟机包含独立 guest kernel。容器启动快不等于安全边界天然更强。

## readiness 与 liveness？

readiness 只决定是否接流量，依赖暂时不可用可以不 ready；liveness 失败会重启，过严会造成重启风暴；startup 保护慢启动。

## Pod Running 但访问不到？

查 Pod IP/监听地址、readiness、Service selector、EndpointSlice、targetPort、NetworkPolicy、DNS 和应用日志。

## 为什么不能只靠 HPA CPU？

通知系统瓶颈可能是 Kafka lag、Provider 配额、连接池、数据库锁或 P99；只扩消费者会放大下游压力和重试。

## Secret 安全吗？

Secret 不是自动完成加密、轮换和最小权限。还要限制 RBAC、加密 at rest、审计、挂载范围和日志脱敏。

## 如何回滚？

先确认代码、数据库 schema 和消息格式兼容，再回滚 Deployment；保留发布 diff、指标、lag 和数据正确性证据。

## Agent 如何辅助 K8s 排障？

读取脱敏事件、日志、Metrics、Trace 和 Runbook，生成带引用的假设和只读查询；不直接执行删除、扩容、回滚或读取 Secret。
