# 第 11 章资料与验证状态

## 一手资料

1. RFC 9293: Transmission Control Protocol：<https://www.rfc-editor.org/rfc/rfc9293>
2. RFC 9110: HTTP Semantics：<https://www.rfc-editor.org/rfc/rfc9110>
3. Java 21 HttpClient：<https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html>
4. HikariCP configuration：<https://github.com/brettwooldridge/HikariCP>
5. AWS Builders' Library: Timeouts, retries and backoff：<https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/>
6. gRPC deadline guide：<https://grpc.io/docs/guides/deadlines/>
7. Kubernetes DNS：<https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/>
8. RFC 8446: TLS 1.3：<https://www.rfc-editor.org/rfc/rfc8446>
9. RFC 9113: HTTP/2：<https://www.rfc-editor.org/rfc/rfc9113>
10. HTML Living Standard: Server-sent events：<https://html.spec.whatwg.org/multipage/server-sent-events.html>
11. Java networking properties（DNS cache TTL）：<https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/doc-files/net-properties.html>
12. Linux `ip-sysctl` TCP 配置：<https://docs.kernel.org/networking/ip-sysctl.html>
13. MySQL Connector/J properties：<https://dev.mysql.com/doc/connector-j/en/connector-j-reference-configuration-properties.html>
14. Redis client handling：<https://redis.io/docs/latest/develop/clients/client-side-caching/>
15. Kafka producer configuration：<https://kafka.apache.org/documentation/#producerconfigs>

## 使用规则

- timeout 字段按具体客户端/版本核验，不假设所有库同名同义。
- Little's Law 只在稳定系统和一致统计口径下解释平均在途。
- 连接池和网络结果必须注明操作系统、容器、代理、协议和负载。

## 当前状态

| 项目 | 状态 | 证据 |
|---|---|---|
| TCP/TLS/HTTP/timeout 原理 | 资料核验/讲义初稿 | RFC、JDK、官方指南 |
| Hikari/HTTP pool | Pending | 尚无运行输出 |
| 慢依赖/端口/断线 | Pending | 尚无网络故障证据 |
| SSE/模型流式 | Pending | 尚无真实模型服务 |

本章不能标记为 Lab Verified、Release Candidate 或 Released。
