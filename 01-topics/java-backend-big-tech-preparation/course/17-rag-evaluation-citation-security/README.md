# 第 17 章：RAG 评测、引用与安全

## 章节定位

- 类型：Evaluation Engineering + RAG Quality + Security + Java Backend + Lab Design
- 难度：深入
- 建议学习时间：28-36 小时
- 先修章节：第 14 章 LLM/RAG 原理；文档 ingestion、混合检索与 rerank 基础
- 对应项目：NotifyFlow Incident Knowledge Assistant 评测与发布门禁

## 当前状态

- 阶段：八件套完整内容初稿，实验 Pending
- 调研日期：2026-07-15
- 已完成：离线/在线评测、指标、引用验证、拒答、ACL、红队、回归门禁、人工标注和 Java 流水线设计
- 未完成：真实语料、标注集、检索索引、模型、评测 Runner、红队与线上 shadow 流量运行证据

本章不能标记为 Lab Verified、Release Candidate 或 Released。

## 相邻章节边界

- 第 14 章解释 Transformer、推理、Embedding、ANN 和 RAG 基础；本章不重复模型底层原理。
- 文档处理章节负责解析、切分、版本和 ingestion；检索章节负责 BM25、向量、hybrid 和 rerank 实现。
- 本章回答“如何证明 RAG 质量、安全和成本达到发布门槛”。
- 第 18 章负责 Tool、Memory、Agent 状态机和副作用执行；本章的被测系统默认只生成带引用回答，不自动执行工具。

## 核心问题

1. 如何定义不会被模型、索引或提示词污染的离线评测集？
2. Recall@k、MRR、nDCG、faithfulness、answer correctness 和 answer relevance 分别测什么？
3. 为什么“答案正确”与“忠于证据”是两个不同维度？
4. 如何把引用从一个 URL 变成可复核的 claim-evidence 映射？
5. 无答案、证据冲突、过期版本和权限不足时，系统何时应拒答？
6. ACL 应在召回前、上下文构建和输出阶段分别做什么？
7. 如何评测间接 prompt injection、跨租户泄露、敏感数据回显和恶意文档？
8. 如何校准 LLM-as-a-judge，避免把 judge 当成事实真相？
9. 离线分数如何变成回归门禁，线上指标如何发现数据漂移和真实失败？
10. Java 21 如何实现可重复、可审计、可并行但不失控的评测流水线？

## 退出标准

- 能设计包含普通题、hard negative、无答案、冲突版本、权限和攻击样本的评测集。
- 能手算 Recall@k、MRR、nDCG、citation precision/recall 和拒答混淆矩阵。
- 能把检索错误、上下文错误、生成错误、引用错误和安全错误分开归因。
- 能定义 claim、evidence、citation、document version、ACL snapshot 和评测 run 的数据合同。
- 能说明 faithfulness、answer correctness、relevance、completeness 和 refusal 的边界。
- 能设计检索前 ACL、上下文最小化、输出验证、审计和红队测试。
- 能建立人工标注规范、双人标注、分歧仲裁和 judge 校准流程。
- 能输出质量—延迟—成本 Pareto 对照，并用门禁阻止明显回归。
- 能画出 Java 评测 Runner 的组件、输入、事件、并发、重试和报告链路。
- 能明确哪些结论已运行、哪些只是设计，不填写虚构准确率或安全通过率。

## 本章交付物

- 版本化 `dataset.jsonl` 合同和数据卡。
- 检索、生成、引用、拒答、安全、延迟和成本指标字典。
- 人工标注指南、分歧记录和 judge 校准报告。
- Java 评测流水线设计与回归门禁策略。
- Prompt injection、跨租户和敏感数据红队样本集。
- 失败样本 taxonomy、每次评测 diff 和发布决策记录。

## 发布前缺口

- 建立脱敏且可授权使用的 NotifyFlow 评测语料与冻结测试集。
- 固定 ingestion、chunk、embedding、索引、rerank、prompt、模型和 judge 版本。
- 真实运行检索、生成、引用、拒答、ACL 和攻击评测并保存原始结果。
- 完成人工双标与分歧仲裁，校准自动 judge。
- 在 shadow/canary 环境完成线上延迟、成本、反馈和安全监控。
- 由陌生评审者复核引用和失败样本，确认门禁没有只优化单一分数。
