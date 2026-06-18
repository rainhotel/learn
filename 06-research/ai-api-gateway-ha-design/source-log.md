# High Availability AI API Gateway Design Source Log

## Source Template

### Source Title

- Type:
- Link or identifier:
- Date:
- Why it matters:
- Reliability:
- Used for:

## Search Queries

### GitHub repository search: LLM gateway and AI gateway

- Type: GitHub Search API
- Link or identifier: `llm gateway ai gateway`, `ai api gateway openai proxy`, `openai compatible gateway`, `llm proxy gateway`
- Date: 2026-06-18
- Why it matters: 用于发现当前活跃的开源 AI Gateway / LLM Proxy 项目。
- Reliability: Medium-high, repository metadata is primary but search ranking can miss projects.
- Used for: 候选项目池构建。

### GitHub repository search: gateway ecosystem

- Type: GitHub Search API
- Link or identifier: `APISIX AI gateway LLM`, `Higress AI gateway LLM`, `Envoy AI gateway LLM`, `Kong AI gateway LLM`, `Helicone AI gateway`, `LLM observability gateway open source`
- Date: 2026-06-18
- Why it matters: 补充传统 API Gateway 和 LLMOps 项目，避免只看 OpenAI proxy。
- Reliability: Medium-high.
- Used for: 项目分类和市场图谱。

## Sources

### BerriAI/litellm

- Type: GitHub repository and README
- Link or identifier: https://github.com/BerriAI/litellm
- Date: 2026-06-18
- Snapshot: 50,767 stars, 8,970 forks, Python, open issues 3,408.
- Why it matters: 最大的开源 LLM Gateway 之一，支持 100+ providers、OpenAI format、cost tracking、guardrails、load balancing、logging。
- Reliability: High, primary source.
- Used for: LLM 专用网关功能基线。

### QuantumNous/new-api

- Type: GitHub repository and README
- Link or identifier: https://github.com/QuantumNous/new-api
- Date: 2026-06-18
- Snapshot: 39,291 stars, 8,941 forks, Go, AGPL-3.0, open issues 835.
- Why it matters: 国内常见统一模型管理 / 中转站方向，强调 OpenAI / Claude / Gemini 兼容转换、组织鉴权、用量和成本。
- Reliability: High, primary source.
- Used for: 国内网关面板类项目对照。

### Apache APISIX

- Type: GitHub repository and README
- Link or identifier: https://github.com/apache/apisix
- Date: 2026-06-18
- Snapshot: 16,745 stars, 2,885 forks, Lua, Apache-2.0, open issues 266.
- Why it matters: 传统高性能 API Gateway 进入 AI Gateway 场景，README 提到 AI proxy、LLM load balancing、retries/fallbacks、token-based rate limiting、security。
- Reliability: High, primary source.
- Used for: 高可用 API Gateway 设计参考。

### Portkey-AI/gateway

- Type: GitHub repository and README
- Link or identifier: https://github.com/Portkey-AI/gateway
- Date: 2026-06-18
- Snapshot: 12,111 stars, 1,136 forks, TypeScript, MIT, open issues 194.
- Why it matters: 明确强调 fast, reliable, secure routing、automatic retries、fallbacks、load balancing、conditional routing、guardrails。
- Reliability: High, primary source.
- Used for: LLM routing 和 fallback 设计参考。

### tensorzero/tensorzero

- Type: GitHub repository and README
- Link or identifier: https://github.com/tensorzero/tensorzero
- Date: 2026-06-18
- Snapshot: 11,656 stars, 931 forks, Rust, Apache-2.0, open issues 393.
- Why it matters: 把 LLM gateway、observability、evaluation、optimization、experimentation 放在一个平台里，强调 <1ms p99 gateway latency、A/B testing、fallbacks、retries。
- Reliability: High, primary source.
- Used for: 网关 + 观测 + 实验平台边界参考。

### Higress

- Type: GitHub repository and README
- Link or identifier: https://github.com/higress-group/higress
- Date: 2026-06-18
- Snapshot: 8,678 stars, 1,153 forks, Go, Apache-2.0, open issues 800.
- Why it matters: AI Native API Gateway，基于 Istio / Envoy / Wasm 插件，README 提到主流模型 provider 支持、MCP hosting、阿里云 99.99% gateway high availability guarantee。
- Reliability: High, primary source.
- Used for: 云原生高可用网关和 AI 插件化设计参考。

### maximhq/bifrost

- Type: GitHub repository and README
- Link or identifier: https://github.com/maximhq/bifrost
- Date: 2026-06-18
- Snapshot: 5,880 stars, 775 forks, Go, Apache-2.0, open issues 541.
- Why it matters: 明确定位 high-performance AI gateway，强调 automatic failover、load balancing、semantic caching、cluster mode、guardrails。
- Reliability: High for feature claims, performance claims need reproduction.
- Used for: 高可用卖点和压测指标参考。

### looplj/axonhub

- Type: GitHub repository and README
- Link or identifier: https://github.com/looplj/axonhub
- Date: 2026-06-18
- Snapshot: 4,337 stars, 547 forks, Go, open issues 79.
- Why it matters: 强调 Any SDK -> Any Model、full request tracing、enterprise RBAC、smart load balancing、auto failover in <100ms、cost tracking。
- Reliability: High for feature claims, latency claim needs reproduction.
- Used for: SDK 兼容、追踪、快速 failover 设计参考。

### Envoy AI Gateway

- Type: GitHub repository and README
- Link or identifier: https://github.com/envoyproxy/ai-gateway
- Date: 2026-06-18
- Snapshot: 1,762 stars, 276 forks, Go, Apache-2.0, open issues 165.
- Why it matters: 提出 two-tier gateway pattern：Tier One 负责 centralized entry、auth、top-level routing、global rate limiting；Tier Two 负责 self-hosted model serving cluster 的细粒度访问控制和 endpoint picker。
- Reliability: High, primary source.
- Used for: 控制面 / 数据面和多层网关架构参考。

### ENTERPILOT/GoModel

- Type: GitHub repository and README
- Link or identifier: https://github.com/ENTERPILOT/GoModel
- Date: 2026-06-18
- Snapshot: 953 stars, 66 forks, Go, MIT, open issues 16.
- Why it matters: 轻量 Go AI Gateway，强调 OpenAI-compatible API、observability、guardrails、streaming、cost and usage tracking。
- Reliability: High, primary source.
- Used for: 个人项目体量参考。

### Helicone AI Gateway

- Type: GitHub repository and README
- Link or identifier: https://github.com/Helicone/ai-gateway
- Date: 2026-06-18
- Snapshot: 602 stars, 59 forks, Rust, GPL-3.0, open issues 12.
- Why it matters: 轻量 AI Gateway，强调 low latency、many models、maximum reliability、OpenAI syntax、多 provider。
- Reliability: High for repository metadata; README 状态为 Public Beta.
- Used for: 轻量网关和观测平台入口参考。
