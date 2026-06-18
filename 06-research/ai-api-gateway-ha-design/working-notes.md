# High Availability AI API Gateway Design Working Notes

## Market Map

### 1. LLM 专用网关

- 代表项目：LiteLLM、Portkey Gateway、Bifrost、Helicone AI Gateway、GoModel、AxonHub、new-api。
- 核心价值：把多个模型供应商统一成一个 OpenAI-compatible API，处理 provider adapter、路由、计费、限流、fallback。
- 简历启发：最适合学习“模型适配层”和“请求治理层”。

### 2. 传统 API 网关的 AI 化

- 代表项目：Apache APISIX、Higress、Envoy AI Gateway、Kong AI Gateway。
- 核心价值：复用成熟网关的高可用能力，例如动态路由、服务发现、健康检查、熔断、插件、Ingress、Kubernetes 部署。
- 简历启发：最适合学习“网关基础设施”和“云原生部署”。

### 3. LLMOps 平台

- 代表项目：TensorZero、Langfuse OSS LLMOps Stack。
- 核心价值：网关只是入口，后面连接 tracing、eval、prompt version、experiment、optimization。
- 简历启发：可以作为二期扩展，不建议一开始就做成大而全平台。

## Candidate Snapshot

| Project | Type | Language | Stars | License | Most Relevant Features |
| --- | --- | --- | ---: | --- | --- |
| LiteLLM | LLM gateway | Python | 50,767 | NOASSERTION | 100+ providers, OpenAI format, virtual keys, spend tracking, guardrails, load balancing |
| new-api | model hub / gateway | Go | 39,291 | AGPL-3.0 | OpenAI / Claude / Gemini compatible, organization auth, quota, cost accounting |
| APISIX | API gateway + AI | Lua | 16,745 | Apache-2.0 | AI proxy, LLM load balancing, retries/fallbacks, token-based rate limiting, security |
| Portkey Gateway | LLM gateway | TypeScript | 12,111 | MIT | retries, fallbacks, load balancing, conditional routing, guardrails |
| TensorZero | LLMOps + gateway | Rust | 11,656 | Apache-2.0 | gateway, observability, eval, A/B testing, routing, fallback, retry |
| Higress | AI native API gateway | Go | 8,678 | Apache-2.0 | Istio/Envoy/Wasm, AI proxy providers, MCP hosting, cloud-native HA |
| Bifrost | LLM gateway | Go | 5,880 | Apache-2.0 | automatic failover, load balancing, semantic caching, cluster mode, guardrails |
| AxonHub | LLM gateway | Go | 4,337 | NOASSERTION | any SDK to any model, tracing, RBAC, smart load balancing, fast failover |
| Envoy AI Gateway | Envoy-based AI gateway | Go | 1,762 | Apache-2.0 | two-tier gateway, auth, top-level routing, global rate limiting, endpoint picker |
| GoModel | lightweight LLM gateway | Go | 953 | MIT | OpenAI-compatible API, streaming, observability, cost usage tracking |
| Helicone AI Gateway | lightweight LLM gateway | Rust | 602 | GPL-3.0 | low latency, many providers, OpenAI syntax, reliability |

## What High Availability Means Here

AI API 网关的高可用不是只开多个副本。LLM 上游有特殊问题：429、上下文长度错误、长连接 streaming、中途断流、不同 provider 参数不兼容、token 成本不确定、响应延迟波动很大。

所以高可用至少要分 6 层：

1. Gateway process HA
   - 网关服务无状态，多副本部署。
   - 配置、Key、配额、路由策略放在数据库或配置中心。
   - Redis 负责分布式限流和短期健康状态。

2. Upstream provider HA
   - 一个逻辑模型 alias 可以映射到多个 provider / model / api key。
   - 每个 channel 维护健康分数：成功率、429 比例、5xx 比例、超时比例、p95 延迟。
   - 不健康 channel 自动降权或熔断。

3. Request-level resilience
   - 每次请求有总超时预算。
   - 连接超时、首 token 超时、读超时分开配置。
   - fallback 优先发生在上游尚未返回 token 之前。
   - 对 streaming 请求谨慎重试，因为上游可能已经计费且客户端已经看到部分 token。

4. Traffic governance
   - 支持 RPM：requests per minute。
   - 支持 TPM：tokens per minute，至少要有 token 预估。
   - 支持 per-user、per-key、per-model、per-provider 维度。
   - 支持 burst 和滑动窗口，Redis 实现分布式一致。

5. Cost and quota HA
   - 虚拟 API Key 绑定用户、预算、可用模型、过期时间。
   - 请求前做预算预扣或额度检查。
   - 请求后记录 input tokens、output tokens、cache tokens、估算成本。
   - 日志写入失败不能阻塞主请求，可用异步队列或 outbox。

6. Observability HA
   - Prometheus metrics：请求量、错误率、延迟、fallback 次数、熔断状态、限流次数、token 用量。
   - 结构化日志：request_id、user_id、model_alias、provider、channel、status、latency、tokens、cost。
   - Trace：网关鉴权、限流、路由、上游调用、响应聚合分段记录。

## Suggested Architecture

```text
Client / SDK
  -> Load Balancer
  -> Gateway Replica 1..N
      -> Auth and virtual key module
      -> Quota and rate limit module
      -> Model alias resolver
      -> Routing engine
      -> Provider adapter
      -> Streaming proxy
  -> Upstream Providers / Mock Providers

Control Plane
  -> Admin API
  -> PostgreSQL: users, keys, model aliases, channels, policies, usage records
  -> Redis: distributed rate limit, circuit breaker state, cache, health score
  -> Prometheus / Grafana / OpenTelemetry
```

## MVP For Resume

### Phase 1: OpenAI-compatible gateway

- 实现 `/v1/chat/completions`。
- 支持 JSON response 和 SSE streaming。
- 支持一个 OpenAI-compatible upstream 和一个 Mock upstream。
- 保留 `request_id` 并输出结构化日志。

### Phase 2: Routing and failover

- 支持模型 alias，例如 `gpt-4o-mini` -> 多个 upstream channel。
- 支持 weighted round-robin。
- 支持 timeout、retry、fallback。
- 支持 channel health score 和简单熔断。

### Phase 3: Rate limit and quota

- 支持虚拟 API Key。
- 支持 Redis 分布式 RPM 限流。
- 支持 token 预估和 TPM 限流。
- 支持用户级日预算和模型白名单。

### Phase 4: Observability and benchmark

- 暴露 `/metrics`。
- Grafana dashboard：QPS、p95/p99、错误率、fallback 次数、上游健康状态、token 用量。
- 用 k6 / vegeta / hey 做压测。
- 构造 3 个故障场景：上游 5xx、上游 429、上游慢响应。

## Strong Resume Angles

- 高可用：多副本网关、分布式限流、上游健康探测、熔断降级、自动 fallback。
- 协议能力：兼容 OpenAI Chat Completions，支持 SSE 流式透传和中断处理。
- 性能能力：压测 p95 / p99、网关额外延迟、故障切换时间。
- 工程能力：Docker Compose 一键启动，Prometheus/Grafana 可观测，Mock Provider 可复现实验。
- 业务理解：支持 token 计量、预算、成本统计，不只是 HTTP 转发。

## Risks

- 不要一开始做太多 provider，适配层会拖慢核心网关能力。
- 不要复制 new-api / one-api 的面板路线，否则容易变成“账号和充值系统”。
- 不要把 fallback 写得过度理想化，streaming 已经吐出 token 后很难无感切换。
- 不要直接使用 AGPL/GPL 项目代码，除非明确接受对应开源协议义务。
