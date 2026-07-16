# 第 12 章参考答案与评分

## 核心答案

- namespace 隔离视图，cgroup 限制资源，overlayfs 组织镜像层；它们不等于独立内核的虚拟机。
- `depends_on` 只表达启动依赖，健康必须由 healthcheck 和应用重试确认。
- readiness 控制流量，liveness 判断是否需要重启，startup 保护慢启动；供应商 503 通常不应直接触发 liveness。
- Pod Running 不代表 Service 可用，需检查 selector、EndpointSlice、端口、readiness、NetworkPolicy 和监听地址。
- HPA 要结合 backlog/lag、处理吞吐、P99、错误和下游配额，不能只看 CPU。

## 评分锚点

| 维度 | 分值 |
|---|---:|
| Docker/K8s 原理 | 20 |
| 部署合同和安全 | 20 |
| 探针/资源/发布/回滚 | 20 |
| 故障排查证据链 | 20 |
| Agent 权限边界 | 10 |
| 诚实表达 | 10 |

把 YAML 当答案而不能解释调度、网络、资源和失败边界，最多得 50 分。
