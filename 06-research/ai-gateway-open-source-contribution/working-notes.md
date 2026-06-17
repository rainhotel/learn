# AI Gateway Open Source Contribution Working Notes

## Hypotheses

- 假设 1：最适合第一次贡献的 gateway issue，多半是 provider parity 或参数映射问题。
- 假设 2：有明确 `good first issue` 标签且 issue 总量较小的仓库，更适合作为第一站。

## Observations

- `BerriAI/litellm`
  - 2026-06-16 查看时：50.5k stars、1.4k issues、146 pull requests。
  - 最新 release 页面显示最近发布为 2026-06-16。
  - labels 页面有 `good first issue`，但 open 搜索结果为 0。
- `QuantumNous/new-api`
  - 2026-06-16 查看时：39k stars、546 issues、39 pull requests。
  - 最新 release 页面显示最近发布为 2026-06-13。
  - labels 页面有 `good first issue` 和 `help wanted`，但首次贡献入口不如 Portkey 直观。
- `songquanpeng/one-api`
  - 2026-06-16 查看时：35k stars、918 issues、83 pull requests。
  - 最新 release 页面显示最近发布为 2025-02-02。
  - issue 池较大，且仓库节奏相对 `new-api` 更慢。
- `Portkey-AI/gateway`
  - 2026-06-16 查看时：12.1k stars、88 issues、104 pull requests。
  - 最新 release 页面显示最近发布为 2026-01-12。
  - labels 页面有 `good first issue` 和 `first timer only`。
  - open `good first issue` 列表里当前可见多条 provider 集成或参数映射问题。

## Comparisons

- `litellm` vs `Portkey`
  - `litellm` 生态更大、更新更快。
  - `Portkey` 的 issue 池更小，首次落地贡献的摩擦更低。
- `new-api` vs `Portkey`
  - `new-api` 更像国内常见“中转站面板”，贴近你最初的兴趣方向。
  - `Portkey` 更像工程化 gateway，issue 更偏代码问题，适合作为第一次公开贡献。
- `one-api` vs `Portkey`
  - `one-api` 社区知名度高，但目前 issue 池更大且 release 节奏更慢。
  - `Portkey` 更容易快速定位一个边界明确的修复点。

## Initial Issue Shortlist

- 优先看
  - `#1189` Support embeddings and chat completions for the triton provider
  - `#1215` Support ai21 Jamba 1.6 and 1.7 models
- 次优先看
  - `#1342` Support TextCompletion API for Cerebras models
  - `#753` Support Huggingface image to image endpoint
- 暂时降级
  - `#1216` Support Cloudflare Workers AI chat/completions route
    - 页面显示已分配给 `msarvi0`
  - `#831` Update ai21 integration with a transformer for streaming
    - 需要额外确认是否已有相关工作在进行

## Open Threads

- 继续确认 `#1189` 是否仍无人认领。
- 继续确认 `#1215` 涉及的 ai21 新接口是否已有外部分支在做。
- 导出全部 open issues 后，按 `provider`、`bug`、`good first issue` 三个维度再做一次筛选。
