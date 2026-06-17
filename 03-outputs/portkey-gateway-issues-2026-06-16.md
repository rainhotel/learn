# Portkey Gateway Issue Scout - 2026-06-16

## Repo Snapshot

- Repository: `Portkey-AI/gateway`
- Observed on: `2026-06-16`
- Repo page snapshot: `12.1k` stars, `88` issues, `104` pull requests
- Release signal: latest listed release observed on `2026-01-12`
- Label signal: includes `good first issue` and `first timer only`

## Current Recommendation

- First contribution target: `Portkey-AI/gateway`
- Best starter issue types:
  - provider parity
  - parameter mapping
  - endpoint support

## Candidate Issues

### High Priority

- `#1189` Support embeddings and chat completions for the triton provider
  - Link: https://github.com/Portkey-AI/gateway/issues/1189
  - Why it looks good: 描述清楚，属于 provider 能力补齐，适合代码型贡献。

- `#1215` Support ai21 Jamba 1.6 and 1.7 models
  - Link: https://github.com/Portkey-AI/gateway/issues/1215
  - Why it looks good: 升级目标明确，接口范围看起来相对可控。

### Medium Priority

- `#1342` Support TextCompletion API for Cerebras models
  - Link: https://github.com/Portkey-AI/gateway/issues/1342
  - Why it looks good: 任务边界具体。
  - Why not first: 还需要确认是否已有相关工作或上下游依赖。

- `#753` Support Huggingface image to image endpoint
  - Link: https://github.com/Portkey-AI/gateway/issues/753
  - Why it looks good: 功能点清晰。
  - Why not first: 可能涉及图像相关接口和额外测试面。

### Deprioritized

- `#1216` Support Cloudflare Workers AI chat/completions route
  - Link: https://github.com/Portkey-AI/gateway/issues/1216
  - Why lower: 页面显示已分配给 `msarvi0`。

- `#831` Update ai21 integration with a transformer for streaming
  - Link: https://github.com/Portkey-AI/gateway/issues/831
  - Why lower: 需要先确认是否已有并行工作，以及流式转换器改动面是否偏大。

## Next Actions

1. 用 `scripts/export_github_issues.py` 抓全量 open issues。
2. 只保留 `good first issue`、`provider`、`bug` 相关标签做二次筛选。
3. 针对 `#1189` 和 `#1215` 开始读代码路径和测试方式。
