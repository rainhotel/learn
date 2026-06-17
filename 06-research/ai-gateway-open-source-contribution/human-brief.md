# AI Gateway Open Source Contribution Human Brief

## What This Research Is About

- 这项研究在回答一个很实际的问题：如果要给开源 AI gateway 贡献代码，先投哪个项目最划算。
- 现在值得看，因为目标不是“围观热门项目”，而是尽快找到一个可以真实提交 PR 的入口。

## Current Best Understanding

- 目前最重要的发现：`Portkey-AI/gateway` 的仓库规模、issue 体量、标签质量和任务形状最平衡。
- 目前最值得相信的结论：第一次贡献应优先避开 issue 池过大的仓库，先选有明确 `good first issue` 的项目。
- 目前最大的疑问：Portkey 的 provider 适配层代码结构是否足够清晰，能否在 1 到 2 次阅读内上手。

## Decision Value

- 这项研究能帮助我决定：先把时间投入哪个仓库，以及先从哪几个 issue 类型切入。
- 还不能帮助我判断：具体哪一个 issue 一定能在本周合并，这还需要看源码结构和维护者反馈。

## Resume Fast

- 下次打开先读：`conclusion.md`
- 下次打开先做：运行 `scripts/export_github_issues.py` 导出 `Portkey-AI/gateway` 的 open issues
- 当前最关键的 1 个问题：`#1189` 和 `#1215` 对应的 provider 代码路径是否足够小，适合首次 PR
