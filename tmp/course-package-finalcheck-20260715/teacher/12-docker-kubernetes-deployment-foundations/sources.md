# 第 12 章资料与验证状态

## Docker 官方资料

1. Docker Engine security：<https://docs.docker.com/engine/security/>
2. Docker build cache：<https://docs.docker.com/build/cache/>
3. Docker storage drivers/overlay2：<https://docs.docker.com/engine/storage/drivers/>
4. Docker Compose specification：<https://compose-spec.io/>
5. Dockerfile reference：<https://docs.docker.com/reference/dockerfile/>

## Kubernetes 官方资料

1. Pods：<https://kubernetes.io/docs/concepts/workloads/pods/>
2. Deployments：<https://kubernetes.io/docs/concepts/workloads/controllers/deployment/>
3. Services and networking：<https://kubernetes.io/docs/concepts/services-networking/service/>
4. Probes：<https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/>
5. Resource management：<https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/>
6. ConfigMaps and Secrets：<https://kubernetes.io/docs/concepts/configuration/configmap/>、<https://kubernetes.io/docs/concepts/configuration/secret/>
7. HPA：<https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/>
8. Debugging services：<https://kubernetes.io/docs/tasks/debug/debug-application/debug-service/>

## 当前状态

| 项目 | 状态 | 证据 |
|---|---|---|
| Docker 原理与 Dockerfile | 资料核验/讲义初稿 | Docker 官方文档 |
| Compose NotifyFlow 环境 | Pending | Docker Engine 未运行 |
| Kubernetes 对象与探针 | 资料核验/讲义初稿 | Kubernetes 官方文档 |
| 发布、回滚、资源和 HPA | 设计 Pending | 尚无集群运行证据 |
| 节点/网络/数据正确性故障 | Pending | 尚无真实演练 |

本章不能标记为 Lab Verified、Release Candidate 或 Released。
