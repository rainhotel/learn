# 第 11 章面试追问

## connect timeout 和 read timeout？

connect 控制建立连接，read/response 控制等待响应数据；pool acquire 是等待可用连接，overall deadline 是端到端上限。

## 连接池越大越好吗？

不是。过大会让下游并发、内存、socket 和排队增加；池大小应结合下游容量、服务时间、线程和 deadline。

## timeout 后能直接重试吗？

读请求可能有限重试；有副作用请求可能已执行，进入 UNKNOWN，通过幂等键、查询或回调对账。

## HTTP/2 一个连接够吗？

不一定。多 stream 共享连接和拥塞窗口，还受 max concurrent streams、流控、故障域和客户端实现影响。

## P99 高但 CPU 低？

检查连接池 pending、下游慢、DNS/TLS、队列、锁和网络；CPU 低不代表无饱和。

## Agent 能自动调 timeout 吗？

不能默认执行。它可提出证据化建议，配置变化需压测、审批、灰度和回滚。
