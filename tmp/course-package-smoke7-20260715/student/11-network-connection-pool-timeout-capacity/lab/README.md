# 第 11 章实验：网络、连接池与超时

## 当前状态

- 状态：Pending
- 已完成：实验矩阵、通用指标、最小验收条件和证据目录设计
- 未完成：代理、连接池、网络故障和模型流式运行

## 实验矩阵

1. DNS/TCP/TLS/TTFB/read 分段延迟。
2. HTTP keep-alive 与短连接/TIME_WAIT。
3. Hikari pool acquire、query timeout 和数据库饱和。
4. HTTP pool 大小、线程池、下游容量与 Little's Law。
5. 慢响应、RST、半开、丢包和 idle timeout。
6. Provider timeout、UNKNOWN、查询/回调对账。
7. 多层重试、retry budget、jitter 和 hedging。
8. SSE 断开、取消、Last-Event-ID 和最终状态。
9. ephemeral port、文件句柄和连接泄漏。
10. Kubernetes DNS/Service 变更和旧连接复用。

## 最小验收条件

| 实验 | 必须控制的变量 | Pass 条件 |
|---|---|---|
| 分段延迟 | 同一目标、固定请求数、冷/热连接分组 | 能分别观察并解释 DNS/connect/TLS/TTFB/read，且不把复用连接误算为新建连 |
| keep-alive/TIME_WAIT | 相同吞吐与响应 | 给出 socket 状态和端口占用，证明差异来自连接策略而非负载变化 |
| Hikari 饱和 | 固定 pool、并发、SQL 服务时间 | 观察 active/pending/acquire timeout，并核对事务最终状态 |
| 容量模型 | 固定到达率阶梯和下游并发 | 报告稳定区、排队拐点、拒绝与 Little's Law 估算误差 |
| 网络故障 | 逐项注入 delay/RST/断连 | 客户端错误能映射到阶段，deadline 后资源被释放 |
| UNKNOWN | 可查询的 Provider stub 与幂等键 | 超时后不产生重复副作用，对账收敛到终态 |
| 重试/hedging | 相同错误序列和预算 | 报告额外负载、尾延迟和调用次数；不得突破 retry budget |
| SSE | 事件 id、断点和保留期固定 | 重连后无业务事件丢失，重复可去重，断档可快照恢复 |
| 资源耗尽 | 固定短连接率/泄漏数量 | 能用 pool、socket、fd 和线程证据定位，清理后资源回落 |
| K8s DNS/连接复用 | 明确 Service/Endpoint 时间线 | 记录 DNS 变化与旧连接寿命，不把单次现象外推为平台保证 |

## 证据

```text
evidence/<experiment>/
  environment.md
  network-topology.md
  client-config.md
  workload.md
  packet-or-proxy-log.txt
  metrics.json
  traces.json
  socket-state.txt
  correctness.md
  conclusion.md
```

## 发布门槛

- 区分每个 timeout 和错误阶段。
- 报告 pool/queue/in-flight/throughput/P95/P99 和数据正确性。
- timeout 副作用有幂等/UNKNOWN/对账证据。
- 未运行前不得填写连接优化比例、端口容量或模型流式稳定性。
