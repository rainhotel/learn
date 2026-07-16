# 第 11 章参考答案与评分

## 逐题要点

1. 链路图必须注明缓存和连接复用可能跳过 DNS/TCP/TLS，并区分 pool acquire、TTFB 与持续 read。
2. connect 管建连，acquire 管等池，read/response 管响应，idle 管无数据间隔，overall deadline 管端到端剩余预算；字段语义以具体客户端版本为准。
3. `200 req/s × 0.5 s = 100` 个平均在途；这不是固定池大小，还要考虑突发、分布和最窄瓶颈。
4. 线程池 200、连接池 20 时，最多约 20 个数据库操作并行，其余线程在 acquire 或上游队列等待；应限制在途并快速拒绝，避免 180 个线程长期占用。
5. Provider timeout 进入 UNKNOWN：复用原幂等键，先查询/等待回调，再由对账状态机决定是否重试，不能直接创建第二个副作用。
6. 多层各重试 3 次可能形成乘法放大；指定 single retry owner，并设置 deadline、retry budget、指数退避和 Full Jitter。
7. HTTP/2 的连接数不等于并发数；同时约束 stream、客户端 in-flight、下游配额和请求 deadline，使用 P95/P99 与拒绝率验证。
8. SSE 事件使用单调 `id`、持久化 run/event、`Last-Event-ID`、重复容忍、保留期、断档快照和取消传播。
9. TIME_WAIT 高先检查短连接率、keep-alive、NAT/代理、端口范围；连接泄漏还要检查未关闭响应体、pool pending、文件句柄和线程栈。
10. 每个依赖表至少写明 acquire/connect/read/overall、重试所有者、副作用语义、UNKNOWN 处理和低基数指标。
11. 故障实验必须同时报告吞吐、in-flight、queue/pool pending、P95/P99、timeout/retry 和最终数据正确性，不能只报平均延迟。
12. 合格 Runbook 顺序是：确认影响面与时间窗 -> 按阶段定位 -> 保护系统 -> 验证数据正确性 -> 小步变更 -> 观察与回滚；Agent 默认只读。

## 评分

| 维度 | 分值 |
|---|---:|
| 网络阶段/协议 | 20 |
| 超时/连接池 | 25 |
| 容量/Little's Law | 15 |
| 重试/UNKNOWN/取消 | 20 |
| 事故证据 | 15 |
| 诚实表达 | 5 |

只说“把 timeout 调大/连接池调大”而没有容量和故障证据，不得超过 50 分。

实验题没有环境、命令、原始输出和 `correctness.md` 时，即使结论正确，也不得获得“运行证据”分。
