# 第 14 章实验：LLM、推理、RAG 与 Agent

## 当前状态

- 状态：Pending
- 已完成：实验变量、数据集字段、断言、评测指标和安全边界设计
- 未完成：模型/Embedding/向量库/推理服务下载与真实运行

## 实验矩阵

1. Tokenizer：中文、代码、JSON 和长 URL 的 token 长度。
2. Attention：causal mask 和小矩阵手算/代码对照。
3. KV cache：不同 context、KV heads、并发和精度的内存预算。
4. Embedding：同义、hard negative、跨租户文本的 cosine/inner-product。
5. ANN：HNSW M/efConstruction/efSearch 的 Recall@k/延迟/内存。
6. Chunk：固定窗口、结构化切分、overlap 的检索和引用消融。
7. Hybrid/rerank：BM25、向量、混合和 rerank 对照。
8. RAG 评测：Recall@k、MRR、nDCG、faithfulness、citation、拒答、成本。
9. 推理：prefill/decode、batch、TTFT、tokens/s、上下文截断和量化。
10. Tool：schema、RBAC、幂等、超时、重试、UNKNOWN 和审计。
11. Agent：prompt injection、越权文档、工具失败和人工审批。
12. Java 后端：SSE 断开、模型超时、限流、fallback、Trace 和最终状态。

## 证据目录

```text
evidence/<experiment>/
  environment.md
  model-and-index.md
  dataset.jsonl
  config.yaml
  raw-results.json
  metrics.csv
  failure-cases.md
  correctness.md
  conclusion.md
```

## 安全约束

- 评测数据使用脱敏、隔离租户和可清理测试 run。
- 不把真实 Secret、完整用户 payload 或跨租户文档送入模型/向量库。
- Tool 高风险动作默认 dry-run，执行需审批、幂等和审计。
- 模型输出不直接执行 SQL、Shell、Kubernetes 或消息重放。
- 未有评测集和原始结果，不得填写准确率、召回率、tokens/s、成本或容量。
