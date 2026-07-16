# 第 11 章讲义：不要把所有网络错误都叫 timeout

## 一、一次请求的阶段

```text
解析目标地址（可能命中 DNS 缓存）
-> 从客户端池获取可复用连接或 stream 配额
-> 若没有可复用连接：TCP connect -> TLS handshake
-> request write -> server queue/processing
-> first byte -> response read/stream
```

这不是所有客户端都严格遵循的固定顺序。连接复用时会跳过 DNS、TCP 或 TLS；HTTP/2 还要区分物理连接与逻辑 stream。排障时应以所用客户端、代理和协议的真实埋点为准。

端到端 P99 只能说明用户等待，不能指出阶段。Trace、客户端指标、服务端指标和网络事件要按同一时间窗口关联。

## 二、DNS 与地址变化

DNS 有缓存和 TTL。长连接可能继续访问旧实例；短 TTL 不等于客户端立即刷新。Kubernetes Service、负载均衡和模型 Provider 切换都要考虑连接复用与 DNS 更新。

## 三、TCP/TLS 最小知识

TCP 提供字节流、重传和拥塞控制，不提供消息边界。连接建立、重传、窗口、keepalive 和 TIME_WAIT 会影响延迟和端口。TLS 在 TCP 之上增加握手、证书和加密；连接复用可减少握手，但旧连接也可能指向故障节点。

## 四、HTTP 语义

HTTP status、连接错误和业务错误要分开。HTTP/1.1 常按连接并发；HTTP/2 在单连接复用多 stream，但仍有连接级拥塞、server max streams 和流控。重定向、代理和网关会改变实际链路。

## 五、超时类型

- DNS timeout：解析阶段。
- connect timeout：建立 TCP/TLS 前后的连接阶段，客户端实现可能分开。
- pool acquire timeout：等待可用连接。
- write timeout：请求写出。
- response/read timeout：等待首字节或后续数据。
- request timeout/deadline：端到端上限。
- idle timeout：连接或 stream 空闲。

单一 30 秒 timeout 会让故障堆积。总 deadline 应分配给各阶段，并给重试和清理留预算。

## 六、连接池

连接池减少建连成本并限制并发，但池过大可能压垮数据库/Provider、增加 socket/内存和慢查询并发；池过小会造成 acquire 排队。

数据库连接是服务端稀缺资源；HTTP/2 连接与 stream 关系不同；Redis/Kafka 客户端有自己的连接、线程和缓冲模型，不能用同一个公式机械配置。

## 七、Little's Law

稳定系统中：

```text
L = λ × W
```

如果到达率 200 req/s，平均端到端时间 0.5 s，平均在途约 100。P99 和突发需要余量，但不能把 P99 直接代入当作固定池大小。持续队列增长说明到达率超过完成能力。

## 八、线程池、连接池与下游容量

线程数 200、连接数 20 时，大量线程会等待连接；连接数 200、数据库只能安全处理 30 时，数据库排队。容量必须从最窄资源反推：下游 QPS、服务时间、连接、线程、CPU、内存和配额。

## 九、timeout 不等于 failed

客户端超时时，下游可能：未收到、收到未处理、处理中、已经成功但响应丢失。对有副作用请求使用幂等键；timeout 后进入 UNKNOWN，查询或回调对账，不直接创建新副作用。

## 十、重试与 hedging

只对明确瞬时错误有限重试，使用 deadline、backoff、jitter 和 retry budget。多层重试会指数放大。hedging 会主动发第二请求，只适用于幂等、可取消、成本可控的读请求，并用实验验证尾延迟收益和额外负载。

## 十一、取消和流式响应

客户端断开时要取消无意义的模型/Provider 调用，但已经提交的业务任务仍需持久化最终状态。SSE/流式输出可能在 JSON 中途断开，客户端以事件 ID 重连；服务器保存 run 状态和事件，而不是依赖连接存活。

## 十二、端口与资源耗尽

频繁短连接会增加 TIME_WAIT 和 ephemeral port 使用；连接泄漏、未关闭响应体和代理 NAT 也会耗尽连接。排查要结合 socket 状态、连接池、线程、文件句柄、容器限制和请求速率。

## 十三、NotifyFlow 超时预算

```text
API deadline
  -> DB acquire/query
  -> queue/dispatch
  -> Provider connect/request/read
  -> result persistence
```

异步通知 API 不应同步等待 Provider 完成。Provider attempt 使用自己的 deadline、幂等键和 UNKNOWN；模型 Agent 允许更长流式时间，但 Tool 仍有独立超时和预算。

## 十四、可观测性

指标：pool active/idle/pending/timeout、connect error、DNS、TLS、TTFB、read duration、in-flight、cancel、retry、timeout、UNKNOWN、socket/port。

低基数 tag 使用 dependency/operation/result；requestId/taskId 放日志和 Trace。

## 十五、Agent 边界

Agent 可以聚合 Trace、连接池、DNS、socket 和 Runbook，推荐只读查询。它不能修改系统 timeout、扩大连接池、关闭防火墙、重置网络或重试高风险操作；需要容量验证、审批和回滚。

## 十六、实验全部 Pending

未有真实代理、连接池和网络故障输出前，不得声称连接优化比例、端口容量或模型流式稳定性。
