# Alpha 发布决定

## 1. 决定

```text
Course status: Internal Alpha
Release Candidate: NO
Released: NO
Approved for formal sale: NO
```

本证据包允许用于内部学习、实验补全、技术审校和受控 Alpha 试教。它不满足 Release Candidate 门禁，不能作为已经完成实验验证、学习者验证、版权清理或商业验收的正式课程发布。

## 2. 门禁判定

| 门禁 | 判定 | 当前证据 | 阻断原因 |
|---|---|---|---|
| 产品定位与章节结构 | 部分通过 | [范围](./scope.md)、[章节状态](./chapter-status.csv)、[课程质量审计](../../quality-audit-2026-07-15.md) | 尚无多入口真实学习者验证、样章和发布构建 |
| 本地文档结构与链接 | 当前自动检查通过 | [链接报告](./link-report.md) | 未检查外部 URL、页面锚点和最终渲染产物 |
| 固定环境与干净复现 | 未通过 | [环境快照](./environment-lock.md) | 环境未完整锁定，工作区为脏状态，NotifyFlow 不能一键干净复现 |
| 核心实验 | 未通过 | [实验结果索引](./lab-results/README.md)、[章节状态](./chapter-status.csv) | 多数真实组件、故障和端到端实验 Pending；既有结果缺统一原始证据包 |
| 学习者独立完成 | 未通过 | [学习者验证](./learner-validation/README.md) | 当前正式样本数为 0 |
| Teach-back 与模拟面试 | 未通过 | [学习者验证](./learner-validation/README.md)、[面试验证](./interview-validation/README.md) | 无正式录音、评分、反馈和复测记录 |
| 求职事实与岗位研究 | 未通过 | [章节状态](./chapter-status.csv) 第 20-23 章 | 事实台账、实时 JD、三类简历、投递反馈均未验证 |
| 许可证与版权 | 未通过 | [许可证报告](./license-report.md) | 无仓库许可证、第三方清单、SBOM 和法律审查 |
| 隐私与安全 | 未通过 | [隐私与安全报告](./privacy-security-report.md) | 只有有限文本扫描，无历史、依赖、二进制和人工审查 |
| 商业交付 | 部分通过 | [学生/教师分包 smoke test](./package-build-report.md)、[已知限制](./known-limitations.md)、[课程发布检查表](../../release-checklist.md) | 已通过答案隔离构建；仍无支持政策、最终压缩包、签名和陌生环境验收 |

## 3. 当前允许的表述

可以表述为：

> 一套覆盖 23 章的内部 Alpha 学习系统，课程骨架完整，部分 Java/MySQL/模型与本地诊断实验已有受限证据，其余实验和教学验证正在补齐。

不得表述为：

- 23 章全部实验通过。
- NotifyFlow、RAG、Agent、Docker/Kubernetes 或多机系统已经生产级验证。
- 已被陌生学习者证明可独立学会。
- 已完成版权、隐私和安全审计。
- 已达到 V1.0 或可以正式售卖。

## 4. 下一次重新判定的最低条件

至少完成以下事实后重新召开发布审计：

1. NotifyFlow 最小纵切可在干净环境一键启动并保存原始证据。
2. 第 05-09 章关键 Spring、Redis、Kafka、恢复和可观测性实验形成 L5-L6 证据。
3. 在干净环境复现学生包和教师包构建，并完成最终 ZIP 解压后二次校验。
4. 至少一名陌生学习者独立完成一个完整模块，并回写修订。
5. 完成正式许可证、隐私、Secret 和依赖/模型/数据使用条款审计。

满足这些条件只触发重新审计，不自动获得 Release Candidate 状态。
