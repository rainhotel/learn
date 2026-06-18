# High Availability AI API Gateway Design AI Context

## Research State

- Current stage: Initial market scan complete, design synthesis started.
- Confidence: Medium. GitHub repository metadata and README summaries are primary-source based, but deeper source-code architecture has not been read yet.
- Last updated: 2026-06-18

## Evidence Map

- 已确认事实：
  - LiteLLM、Portkey、Bifrost、TensorZero、Helicone、GoModel、AxonHub 都明确把自己定位为 AI Gateway 或 LLM Gateway。
  - APISIX、Higress、Envoy AI Gateway 代表传统网关 / Envoy 生态对 AI 流量治理的方向。
  - 高可用关键词反复出现：load balancing、fallback、retry、rate limiting、guardrails、observability、cost tracking、cache。
  - Higress README 明确提到阿里云基于 Higress 的云原生 API 网关提供 99.99% gateway high availability guarantee。
  - APISIX README 明确提到 AI Gateway、LLM load balancing、retries and fallbacks、token-based rate limiting。
- 待验证说法：
  - 各项目的具体故障转移实现细节。
  - Bifrost 宣称的性能指标是否有可复现实验。
  - LiteLLM、Portkey、Bifrost 对 streaming 请求失败后的 fallback 策略差异。
- 冲突信息：
  - LLM 网关和传统 API 网关在“谁应该负责 token 级限流、模型语义路由、成本记账”上的边界不同。
  - 开源版和企业版功能边界可能不一致，不能只看 README 宣传语。

## Search Backlog

- 深入阅读 LiteLLM 的 proxy routing、budget、load balancing 文档。
- 深入阅读 APISIX 的 `ai-proxy`、`ai-proxy-multi`、`ai-rate-limiting` 插件文档。
- 深入阅读 Higress 的 `ai-proxy` Wasm 插件和 MCP hosting 文档。
- 深入阅读 Envoy AI Gateway 的两层网关架构和 Kubernetes 部署示例。
- 搜索是否有公开 benchmark：LiteLLM vs Bifrost vs Portkey vs APISIX / Envoy。

## Next Best Edits

1. 根据目标岗位选择技术栈，并补一份项目模块设计。
2. 把 MVP 拆成 2 周 / 4 周 / 6 周实现计划。
3. 设计 Mock LLM Provider 和压测场景，避免依赖真实付费模型 API。
