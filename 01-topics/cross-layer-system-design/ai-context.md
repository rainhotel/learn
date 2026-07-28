# 跨层系统设计哲学 AI Context

## Topic State

- Current phase: Phase 0 - 系统推理与测量
- Confidence estimate: 主题结构高，实践证据低
- Last updated: 2026-07-20
- Current capstone: 高并发预约与库存服务 V0

## Dependency Map

- 先修知识：数据结构、Java/C/Go 任一语言、线程基础、TCP/HTTP、SQL 和事务基础。
- 当前核心概念：不变量、状态所有权、故障模型、延迟分位数、Little's Law。
- 后续高级主题：WAL、MVCC、Outbox、部分失败、Raft、Fencing、SLO 与故障演练。

## Knowledge Gaps

- 尚无贯穿项目代码和基准数据。
- 尚未选择具体数据库、缓存和消息系统；当前不应提前冻结。
- 尚未完成租约/Fencing、重试风暴、复制延迟实验。
- 资源链接主要来自权威公开课程、论文和官方工程资料，后续按阶段验证可访问性。

## AI Collaboration Rules

- 每次回答先识别不变量、状态所有权和故障模型。
- 不把组件保证扩大成端到端保证。
- 所有 OS ↔ Web 类比必须写成立条件和失效边界。
- 推荐中间件前先回答“不使用它会怎样”。
- 项目演进保持单体优先，只有出现明确边界或容量需求才拆分。
- 每个阶段优先产出实验数据和一页结论，不只产出笔记。

## Extraction Backlog

- Phase 0 实验后，将负载模型与指标结论提炼到 `notes.md`。
- 每次误判追加到 `qa.md`。
- 每个实验结果追加到 `projects.md` 对应版本。
- 新公式或估算方法追加到 `formula-sheet.md`。

## Source Map

- 主教材：OSTEP、CSAPP、DDIA、CMU 15-445、MIT 6.5840。
- 工程资料：Google SRE、AWS Builders' Library。
- 基础论文：End-to-End、Lamport Clock、Raft、Dynamo、CAP、Leases、Sagas。
- 详细索引：`resource-map.md`。

## Next Best Edits

1. 用户确定贯穿项目语言和大致业务场景后，细化 V0 接口与数据模型。
2. 完成第一次压测后，将原始数据、图表结论写入 `projects.md` 和 `progress.md`。
3. Phase 1 开始前，为并发实验建立可重复测试和不变量断言。

