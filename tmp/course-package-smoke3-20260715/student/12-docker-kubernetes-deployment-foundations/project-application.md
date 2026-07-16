# NotifyFlow Docker/Kubernetes 应用方案

## 1. 运行单元

| 单元 | 容器/Pod 责任 | 状态数据 |
|---|---|---|
| API | 创建任务、返回 taskId | MySQL |
| Outbox/Consumer | 发布、领取、重试 | MySQL/Kafka |
| Provider worker | 有界并发调用下游 | 不保存本地状态 |
| Agent advisor | 只读事故摘要和建议 | 审计记录 |

本地 Compose 可以把这些角色合并为一个应用容器；生产 Kubernetes 再按伸缩和权限边界拆分。

## 2. Docker 合同

- `EXPOSE` 只描述端口，不等于网络暴露。
- `/actuator/health` 与 `/actuator/ready` 使用独立语义。
- 应用以非 root 运行，日志输出 stdout/stderr，状态写入 MySQL/Redis/Kafka。
- SIGTERM 后停止领取新任务，等待 in-flight 有界完成，再关闭消费者和连接池。

## 3. Kubernetes 资源草案

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: notifyflow-worker
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  selector:
    matchLabels:
      app: notifyflow-worker
  template:
    metadata:
      labels:
        app: notifyflow-worker
    spec:
      terminationGracePeriodSeconds: 45
      containers:
        - name: app
          image: registry.example/notifyflow:course-draft
          securityContext:
            runAsNonRoot: true
            allowPrivilegeEscalation: false
          resources:
            requests: { cpu: "250m", memory: "512Mi" }
            limits: { cpu: "1", memory: "1Gi" }
          startupProbe:
            httpGet: { path: /actuator/health, port: 8080 }
            failureThreshold: 30
            periodSeconds: 2
          readinessProbe:
            httpGet: { path: /actuator/ready, port: 8080 }
            periodSeconds: 5
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8080 }
            periodSeconds: 10
```

上面是教学草案，不是已验证生产配置；真实发布必须把示意 tag 替换为经过构建、扫描和签名验证的 digest，并复验探针路径、资源和安全策略。

## 4. 发布时序

1. 构建、SBOM、漏洞扫描和签名。
2. 在隔离 namespace 部署新镜像。
3. 验证 startup/readiness、数据库迁移兼容和消息格式。
4. 以小流量观察 SLI、lag、P99、错误和数据正确性。
5. 分阶段扩大消费者，不超过 Provider 配额。
6. 异常时暂停消费、保留证据并回滚兼容版本。

## 5. Agent 事故助手

输入：Pod 事件、Deployment diff、Service/EndpointSlice、脱敏日志、Metrics、Trace 和 Runbook。

输出：时间线、假设、证据引用、置信度、下一条只读查询和建议审批动作。

禁止：读取 Secret、直接 `kubectl delete/scale/rollout`、清空 Kafka、修改 RBAC 或跨租户查询。
