# 第 14 章 Teach-back

## 5 分钟：一次 token 到回答

画出 tokenizer -> embedding -> Transformer block -> logits -> sampling，并说明 causal mask 和 KV cache。

## 15 分钟：RAG 不是“向量库 + prompt”

讲清 ingestion、权限/版本 metadata、hybrid retrieval、rerank、context budget、引用和评测失败样本。

## 45 分钟：NotifyFlow Agent 事故助手

问题 -> 权限 -> 检索 -> evidence context -> 结构化回答 -> Tool 建议 -> 审批/执行边界 -> 审计与恢复。

## 验收

- 能解释一次 KV cache 内存计算。
- 能区分 retrieval failure、generation failure、tool failure 和业务数据错误。
- 能展示一个有引用的拒答样例。
- 能说明为什么 Agent 不能直接重放、清队列或改 Kubernetes。
