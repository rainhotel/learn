# AI Gateway Open Source Contribution Conclusion

## Final Position For Now

- 当前结论：第一次尝试给开源 AI gateway 贡献代码，优先选择 `Portkey-AI/gateway`。

## Evidence Chain

1. `Portkey-AI/gateway` 在 2026-06-16 查看时为 12.1k stars、88 issues、104 pull requests，说明它已经足够成熟，但 issue 池没有大到失控。
2. Portkey 的 labels 页面明确包含 `good first issue` 和 `first timer only`，说明维护者有意识地为新贡献者留入口。
3. Portkey 当前 open `good first issue` 列表里可见多个 provider 集成或参数映射问题，任务形状更偏代码修复，而不是泛泛讨论。
4. `BerriAI/litellm` 虽然更大更活跃，但在 2026-06-16 的 open `good first issue` 搜索结果为 0，首次切入成本更高。
5. `QuantumNous/new-api` 和 `songquanpeng/one-api` 都是强候选，但前者更像产品面板和资产管理系统，后者当前 release 节奏偏慢，首次公开贡献不如 Portkey 顺手。

## What Seems True

- 最适合第一次贡献的 issue 类型，大概率是 provider parity、参数映射、endpoint 对齐。
- 首次贡献的关键不是仓库越大越好，而是 issue 是否清晰、可复现、可测试。
- Portkey 比 `litellm` 更像一个“能快速打第一枪”的仓库。

## What Is Still Uncertain

- `#1189` 和 `#1215` 的源码改动范围是否真的足够小。
- 维护者对 provider integration 类 PR 的 review 周期如何。
- 是否存在更容易的测试、文档或小型 bug 修复入口还没被这轮筛出来。

## Recommendation

- 是否继续研究：是
- 最值得继续的方向：下一步直接围绕 Portkey 导出 issue 数据，优先检查 `#1189` 和 `#1215`，把“能不能在一周内做出第一个 PR”作为判断标准
