# Resume Project Plan: ModelMesh Gateway

## Final Project Choice

- Project name: `ModelMesh Gateway`
- Chinese name: 高可用 AI API 网关
- English positioning: High-Availability OpenAI-Compatible AI Gateway
- Target resume role: Java backend / infrastructure-leaning backend

## Why This Is The Highest-Value Version

这个项目的含金量来自 4 个交叉点：

- AI 工程：接入 LLM provider，兼容 OpenAI Chat Completions 和 SSE streaming。
- 网关系统：鉴权、路由、限流、重试、fallback、熔断、观测。
- 分布式后端：Redis 分布式限流、共享熔断状态、多副本网关部署。
- 可验证结果：Mock Provider 故障注入 + 压测报告，而不是只写功能介绍。

面试官容易继续追问，也说明项目有东西可讲：

- 为什么 streaming 失败后不能简单重试？
- RPM 和 TPM 限流有什么区别？
- 熔断状态为什么要放 Redis？
- fallback 如何避免重复计费和重复响应？
- 网关 p95 / p99 延迟如何压测？

## Recommended Tech Stack

- Language: Java 21
- Framework: Spring Boot 3 + WebFlux
- HTTP client: Reactor Netty `WebClient`
- Resilience: Resilience4j or custom lightweight circuit breaker
- Storage: PostgreSQL
- Cache and distributed state: Redis
- Metrics: Micrometer + Prometheus + Grafana
- Deployment: Docker Compose first, Kubernetes optional
- Load testing: k6 or vegeta

选择 Java WebFlux 的原因：

- 贴合 Java 后端岗位，比 Go 更容易和简历主线一致。
- WebFlux / Reactor Netty 能讲清楚高并发、非阻塞 IO、SSE 长连接和背压。
- Spring 生态天然接 Micrometer、Redis、PostgreSQL，工程展示完整。

## Architecture

```text
OpenAI SDK / Client
  -> Nginx or local load balancer
  -> ModelMesh Gateway replicas
      -> RequestId filter
      -> Virtual API key auth
      -> Quota and Redis rate limiter
      -> Model alias resolver
      -> Routing engine
      -> Circuit breaker and health scorer
      -> Provider adapter
      -> SSE streaming proxy
      -> Usage recorder
  -> Provider channels
      -> Mock OpenAI provider A
      -> Mock OpenAI provider B
      -> Real OpenAI-compatible provider

Control and observability
  -> PostgreSQL: users, keys, channels, model aliases, usage records
  -> Redis: RPM/TPM counters, circuit states, channel health scores
  -> Prometheus: metrics
  -> Grafana: dashboards
```

## Core Data Model

### Virtual API Key

- `id`
- `key_hash`
- `owner`
- `enabled`
- `allowed_models`
- `rpm_limit`
- `tpm_limit`
- `daily_budget_cents`
- `expire_at`

### Model Alias

- `alias`: for example `gpt-4o-mini`
- `strategy`: `weighted_round_robin`, `least_latency`, `failover`
- `channels`: channel ids
- `fallback_enabled`
- `timeout_ms`

### Provider Channel

- `id`
- `provider`: `openai`, `deepseek`, `mock-openai`
- `base_url`
- `api_key_ref`
- `model_name`
- `weight`
- `enabled`
- `priority`
- `health_score`

### Usage Record

- `request_id`
- `virtual_key_id`
- `model_alias`
- `provider`
- `channel_id`
- `status`
- `latency_ms`
- `input_tokens`
- `output_tokens`
- `estimated_cost_cents`
- `fallback_count`

## Must-Have Features

### 1. OpenAI-compatible protocol

- Implement `POST /v1/chat/completions`.
- Support non-streaming JSON response.
- Support `stream: true` SSE response.
- Preserve OpenAI SDK compatibility as much as possible.

### 2. Multi-provider routing

- One model alias maps to multiple upstream channels.
- Support weighted round-robin first.
- Add simple failover strategy later.
- Record the final selected channel in logs and metrics.

### 3. Retry, fallback, and circuit breaker

- Retry only before response streaming starts.
- Fallback when connect timeout, first-token timeout, 429, or 5xx happens.
- Circuit breaker opens when a channel crosses failure threshold.
- Half-open state periodically probes the channel.
- Redis stores circuit state so multiple gateway replicas share decisions.

### 4. Distributed rate limiting

- RPM: requests per minute.
- TPM: tokens per minute, with pre-estimated input tokens and reserved output budget.
- Dimensions:
  - per virtual key
  - per model alias
  - per provider channel
- Use Redis Lua script for atomic counter update.

### 5. Cost and quota tracking

- Count or estimate input/output tokens.
- Map model price to estimated request cost.
- Enforce daily budget for virtual API keys.
- Usage write should be asynchronous or fail-soft, so logging failure does not block model response.

### 6. Observability

- Metrics:
  - gateway request total
  - gateway request latency p95/p99
  - upstream request latency
  - upstream 429/5xx count
  - fallback count
  - circuit open count
  - rate limit reject count
  - token usage
- Logs:
  - `request_id`
  - `model_alias`
  - `channel_id`
  - `status`
  - `latency_ms`
  - `fallback_count`
- Dashboard:
  - QPS
  - p95/p99 latency
  - error rate
  - fallback rate
  - top models by token usage

## Mock Provider Design

为了不依赖真实付费 API，必须做 Mock Provider。

### Mock endpoints

- `POST /mock/v1/chat/completions`
- query or header controls:
  - `x-mock-mode: ok`
  - `x-mock-mode: slow`
  - `x-mock-mode: 429`
  - `x-mock-mode: 500`
  - `x-mock-mode: stream-drop`

### Demo scenarios

- Scenario A: Provider A returns 500, gateway falls back to Provider B.
- Scenario B: Provider A is slow, first-token timeout triggers fallback.
- Scenario C: Provider A repeatedly fails, circuit breaker opens and traffic avoids it.
- Scenario D: Redis RPM limit rejects excess requests.
- Scenario E: `stream: true` works through the gateway and preserves SSE format.

## Four-Week Build Plan

### Week 1: Protocol and gateway skeleton

- Spring Boot WebFlux project skeleton.
- `/v1/chat/completions` request and response DTOs.
- Mock Provider service.
- Non-streaming proxy works end to end.
- Basic request id and structured logs.

### Week 2: Streaming and routing

- SSE streaming passthrough.
- Model alias configuration.
- Weighted round-robin routing.
- Timeout configuration.
- Basic fallback before streaming starts.

### Week 3: HA controls

- Redis RPM limiter.
- Basic TPM estimator.
- Circuit breaker state.
- Channel health score.
- Virtual API Key auth.

### Week 4: Observability and proof

- Prometheus metrics.
- Grafana dashboard JSON.
- Docker Compose: gateway + Redis + PostgreSQL + Prometheus + Grafana.
- k6 / vegeta load test scripts.
- Failure injection report.
- README architecture diagram and demo commands.

## Stretch Features

- Semantic cache with embeddings.
- Admin API for updating routes at runtime.
- Canary model routing.
- Least-latency routing.
- OpenTelemetry trace.
- Kubernetes deployment with 2 gateway replicas.

## Do Not Build In V1

- Chat web UI.
- Recharge/payment system.
- Too many real providers.
- Complex user organization management.
- Full prompt management platform.

这些会稀释主线。第一版只做“高可用网关”。

## Resume Bullets

### Conservative Version

- 设计并实现高可用 AI API 网关 `ModelMesh Gateway`，兼容 OpenAI Chat Completions 与 SSE 流式响应，基于 Spring Boot WebFlux 实现非阻塞代理、多 Provider 权重路由、超时重试、故障转移和熔断降级。
- 基于 Redis Lua 实现分布式 RPM/TPM 限流与虚拟 API Key 配额控制，接入 Prometheus/Grafana 观测 QPS、p95/p99 延迟、上游错误率、fallback 次数和 token 用量，并通过 Mock Provider 构造 5xx、429、慢响应等故障场景完成压测验证。

### Stronger Version After Benchmark

- 设计并实现高可用 AI API 网关 `ModelMesh Gateway`，支持 OpenAI-compatible API、SSE 流式透传、多 Provider 权重路由、自动 fallback、Redis 共享熔断状态和分布式限流；在多副本网关部署下完成故障注入压测，将上游 5xx/429 故障自动切换到健康通道，并通过 Prometheus/Grafana 输出完整可观测指标。

## Interview Storyline

1. 背景：AI 应用直接调用模型 API 会遇到 provider 不稳定、429、延迟波动、成本不可控和观测缺失。
2. 目标：做一个统一的 OpenAI-compatible 网关，把模型访问治理集中到一层。
3. 难点：
   - SSE 流式响应不能像普通 HTTP 一样随便重试。
   - 多副本网关下限流和熔断状态需要共享。
   - 限流不能只按请求数，还要考虑 token。
4. 方案：
   - WebFlux 做非阻塞代理。
   - Redis Lua 做原子限流。
   - channel health score + circuit breaker 做故障隔离。
   - Prometheus/Grafana 做可验证观测。
5. 结果：用 Mock Provider 复现 5xx、429、慢响应、断流等情况，并用压测数据证明 fallback 和限流有效。

## Success Criteria

- OpenAI SDK can call the gateway without changing business code except `base_url`.
- Streaming response is actually streamed, not buffered until completion.
- When Provider A fails before streaming starts, Provider B receives traffic.
- Redis rate limit works when running 2 gateway replicas.
- Prometheus shows fallback count, error rate, p95/p99 latency, and rate limit rejects.
- README includes reproducible commands and benchmark screenshots or tables.
