# 第 11 章 Teach-back

## 5 分钟：一次请求为什么慢

画地址解析、池/stream 配额、可选的新建 TCP/TLS、TTFB、read 和服务端队列，说明连接复用会跳过部分阶段，端到端 P99 不能定位根因。

## 15 分钟：连接池不是越大越好

用线程池、连接池、DB 容量和 Little's Law 解释排队与拒绝。

## 45 分钟：Provider timeout 事故

连接/响应超时 -> UNKNOWN -> 幂等/对账 -> retry budget -> 可观测性 -> 配置变更审批。

## 验收

- 能区分 DNS/connect/acquire/write/response/idle/overall deadline，并说明具体客户端可能没有完全相同的字段。
- 能解释 timeout 不等于 failed。
- 能设计 SSE 断线恢复。
- 不用“调大连接池”代替容量分析。
