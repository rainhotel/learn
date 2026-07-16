# 第 14 章练习

1. 对一段中文、Java 代码和 JSON 估计 token 变化，并说明为什么必须实际 tokenizer 验证。
2. 推导 causal mask 对 attention 矩阵的影响。
3. 给定 layers、kv heads、head dim、context 和并发，计算 KV cache 粗略内存预算。
4. 比较 prefill 与 decode 的瓶颈和可观测指标。
5. 设计文档 chunk metadata，包含租户、权限、版本、页码和时间有效性。
6. 用一个包含 hard negative 和无答案问题的评测集比较 vector、BM25、hybrid 和 rerank。
7. 解释 Recall@k 高但回答仍错误的至少四个原因。
8. 设计带引用和拒答的回答 schema。
9. 为 NotifyFlow 设计一个只读 Tool 和一个高风险 replay Tool 的合同。
10. 设计 Agent Tool 超时、重试、幂等、UNKNOWN、审批和审计。
11. 设计 Java SSE 接口，处理客户端断开、半截 JSON 和最终状态。
12. 写一段不夸大模型效果的简历描述，列出模型、数据集、指标和限制。

## 作业提交

- Transformer/推理流程图。
- KV cache 和 token 成本计算表。
- RAG ingestion/retrieval/evaluation 设计。
- Tool schema、权限矩阵和状态机。
- 失败样本分析与 Agent 安全清单。
