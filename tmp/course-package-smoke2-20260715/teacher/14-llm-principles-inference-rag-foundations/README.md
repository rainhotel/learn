# 第 14 章：大语言模型原理、推理、RAG 与 Agent 基础

## 章节定位

- 类型：Theory + Inference Design + RAG + Agent + Project + Lab Design + Interview + Teach-back
- 难度：深入
- 建议学习时间：28-36 小时
- 先修章节：第 09 章可观测性、第 12 章部署基础；Java/HTTP/SQL 基础
- 对应项目：NotifyFlow Knowledge Assistant 与安全 Agent Runtime

## 当前状态

- 阶段：八件套完整内容初稿，实验 Pending
- 调研日期：2026-07-15
- 已完成：Transformer、推理瓶颈、Embedding/向量检索、RAG、Agent Runtime 讲义和实验矩阵设计
- 未完成：模型下载、Embedding/向量数据库、RAG 评测集、推理服务和真实 Agent 运行证据

本章不能标记为 Lab Verified、Release Candidate 或 Released。

## 相邻章节边界

- 第 09-12 章提供 Java 后端、可观测性、部署、权限和故障恢复基础。
- 第 14 章解释模型和 AI 应用链路，不把“调用 API”当成理解模型。
- 后续章节再分别深入文档处理、混合检索、评测、Tool/Memory 和生产 Agent。

## 核心问题

1. Tokenizer、Embedding、Transformer、Attention 和 next-token prediction 如何连接？
2. RoPE、残差、LayerNorm、MLP 和 causal mask 分别解决什么问题？
3. prefill、decode、KV cache、batch、量化和显存预算如何影响延迟/吞吐？
4. Embedding 相似度、ANN、HNSW、混合检索和 rerank 如何配合？
5. Chunk size、overlap、metadata、引用和权限为什么决定 RAG 质量？
6. 如何用 Recall@k、MRR、nDCG、faithfulness、citation accuracy 和 answer relevance 评测？
7. Agent 与普通 RAG 的边界是什么？Tool、状态机、Memory、重试和幂等如何设计？
8. Java 后端如何承载流式输出、超时、限流、向量查询、工具执行和审计？
9. 哪些内容可以交给 LLM，哪些必须由确定性代码、权限和人工审批控制？

## 退出标准

- 能从 token 到 Transformer block 解释一次生成，而不是只背术语。
- 能计算 KV cache、上下文和并发对显存的影响。
- 能设计可评测的文档 ingestion、检索、重排、引用和回答链路。
- 能实现 Java 后端的模型调用、超时、流式响应、重试和成本指标。
- 能把 Agent Tool 设计成有 schema、权限、幂等、审计和停止条件的状态机。
- 能区分模型幻觉、检索失败、上下文丢失、工具错误和业务数据错误。
- 能说明实验环境、模型、数据集和评测限制，不编造准确率或 QPS。

## 发布前缺口

- 固定 Embedding、向量库、模型和推理服务版本。
- 建立包含 hard negative、权限和时间有效性的 RAG 评测集。
- 完成 chunk/embedding/ANN/rerank/引用消融实验。
- 完成 Java SSE/异步任务/超时/限流/成本和 Trace 链路。
- 完成 Tool 安全、重放、幂等、人工审批和故障恢复实验。
- 完成学习者项目答辩与 Teach-back。
