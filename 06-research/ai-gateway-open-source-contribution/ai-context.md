# AI Gateway Open Source Contribution AI Context

## Research State

- Current stage: repository selected, issue triage started
- Confidence: medium-high
- Last updated: 2026-06-16

## Evidence Map

- 已确认事实：
  - `Portkey-AI/gateway` 在 2026-06-16 查看时为 12.1k stars、88 issues、104 pull requests。
  - `Portkey-AI/gateway` 的 labels 页面包含 `good first issue` 和 `first timer only`。
  - `Portkey-AI/gateway` 的 open `good first issue` 列表当前可见多个候选任务。
  - `BerriAI/litellm` 在 2026-06-16 查看时为 50.5k stars、1.4k issues，但 open `good first issue` 结果为 0。
  - `QuantumNous/new-api` 和 `songquanpeng/one-api` 都是强候选，但 issue 池明显更大。
- 待验证说法：
  - Portkey 的 provider integration 任务是否更容易被维护者接受。
  - `good first issue` 列表里的部分问题是否已经有人在做但还没更新状态。
- 冲突信息：
  - 暂未发现明显冲突信息。

## Search Backlog

- 还需要找的材料：
  - Portkey provider 适配层目录结构
  - Portkey 是否有 CONTRIBUTING 或测试说明
- 还需要验证的来源：
  - 目标 issue 的最新评论和 assignee 状态
  - 相关 provider 文档是否能支持本地复现
- 还需要比较的观点：
  - Portkey 首次贡献体验 vs `new-api`
  - Provider parity bug vs 文档类贡献的时间收益比

## Next Best Edits

1. 用导出脚本抓取 `Portkey-AI/gateway` 的 open issues，并按 label 聚类。
2. 读 `#1189`、`#1215` 关联的源码路径，判断改动面大小。
3. 如果 Portkey 卡住，再把 `new-api` 作为第二候选补做同样的 issue 统计。
