# 第 14 章讲义：从 Token 到可评测 Agent

## 学习目标

本章把“模型原理”和“后端工程”放在一条链路中：输入如何变成 token，模型如何计算下一个 token，检索如何提供证据，Agent 如何在权限和状态机内调用工具。

## 一、Tokenizer 与 Embedding

模型接收的不是字符串，而是 token id 序列。Tokenizer 的词表、子词切分、特殊 token 和 Unicode 处理会影响上下文长度、成本和截断。中文、代码、JSON 和长 URL 的 token 比例可能不同，不能用字符数精确替代 token 数。

Embedding 是把 token、句子或文档映射到向量空间的表示。相似度可以使用 cosine 或 inner product，但“向量更近”不等于“答案一定正确”；训练目标、领域、切分和查询表达都会影响结果。

## 二、Transformer 的最小数学模型

给定隐藏状态矩阵 `X`：

```text
Q = XWq
K = XWk
V = XWv
Attention(Q,K,V) = softmax(QKᵀ / √dk + mask)V
```

causal mask 保证当前位置不能看未来 token。多头注意力让不同 head 学习不同关系；残差连接和归一化帮助深层网络优化；MLP 对每个位置独立做非线性变换。

工程上要理解三件事：

- 计算量随序列长度近似二次增长（标准 attention）。
- KV cache 保存历史 K/V，避免 decode 每步重复计算，但会消耗显存。
- context window 是上限，不等于模型能可靠使用全部上下文。

## 三、位置编码与生成目标

Transformer 没有天然顺序，需要绝对位置、相对位置或 RoPE 等机制注入位置信息。自回归语言模型以历史 token 预测下一个 token，训练时常使用 teacher forcing 和交叉熵。

预训练获得语言与世界知识的统计能力；SFT 使输出遵循任务格式；偏好优化/安全对齐改变回答倾向，但都不能替代权限、数据校验和工具状态机。

## 四、推理底层：prefill 与 decode

- prefill：一次处理输入上下文，计算并写入 KV cache，通常计算密集。
- decode：逐 token 生成，每步读取 KV cache，通常受内存带宽和调度影响。
- TTFT：首 token 时间，受 prefill/排队影响。
- inter-token latency：后续 token 间延迟。
- throughput：tokens/s 或 requests/s，必须注明 batch、上下文、输出长度和硬件。

KV cache 粗略预算：

```text
bytes ≈ layers × 2(K/V) × sequence_length × kv_heads × head_dim × bytes_per_value × concurrent_sequences
```

实际还受 GQA/MQA、分页管理、对齐和框架实现影响。量化可以降低权重或 KV 的内存，但可能影响精度和算子性能，必须用任务评测验证。

## 五、采样与可靠输出

temperature、top-p、top-k 改变采样分布；temperature=0 也不保证所有服务完全确定。生产链路应使用结构化输出 schema、JSON 校验、重试上限和错误分类，不能把模型文本直接当作 SQL、Shell 或业务命令执行。

## 六、RAG 全链路

```text
文档获取
-> 解析/去重/权限继承
-> chunk + metadata
-> embedding
-> vector index
-> query rewrite/embedding
-> ANN/hybrid retrieval
-> rerank
-> context budget
-> generation + citations
-> answer/evidence evaluation
```

### 6.1 Chunking

固定 token 窗口简单但可能切断标题、表格和代码；按章节/段落/语义切分更贴近文档结构，但需要处理过长段落。overlap 可能提高召回，也会增加重复、成本和相邻 chunk 噪声。每个 chunk 必须保留 documentId、版本、租户、权限、页码/段落和时间有效性。

### 6.2 ANN 与混合检索

ANN 用近似结构换取延迟；HNSW 的 efConstruction、M 和 efSearch 影响内存、召回和延迟。向量检索擅长语义相似，BM25/关键词擅长精确编号、错误码和专有名词。混合检索、过滤和 rerank 要用评测集比较，而不是凭感觉选择。

### 6.3 Rerank 与引用

初检 top-k 可以更宽，rerank 再选 context；最终 prompt 必须明确证据块、来源和回答格式。引用不是把 URL 拼进文本，而是回答句子与 chunk 的可追溯映射。无证据时应拒答或说明不确定性。

## 七、RAG 评测

检索指标：Recall@k、MRR、nDCG、命中权限过滤、时间版本正确性。

生成指标：answer relevance、faithfulness/groundedness、citation precision/recall、拒答正确率、格式正确率、延迟、成本和 token 使用。

评测集必须包含：

- 直接命中、跨段组合、同义表达和 hard negative。
- 过期文档、权限越权、无答案和冲突版本。
- Java 错误堆栈、SQL、JSON、中文长文和表格。

LLM judge 只能作为辅助，必须保留人工标注、规则指标和失败样本。

## 八、Agent Runtime

Agent 不应被理解为“让模型自由循环”。可靠实现是有限状态机：

```text
RECEIVED -> PLAN -> TOOL_REQUEST -> AUTHZ -> EXECUTE -> VERIFY -> RESPOND
                         |                       |
                       DENY                    RETRY/COMPENSATE
```

Tool 合同至少包含 name、version、JSON Schema、租户、权限、幂等键、超时、预算、side effect 等级和审计字段。工具执行结果必须经过确定性校验，再交给模型总结。

Memory 分为会话上下文、短期工作记忆、长期知识和业务状态。业务状态不能只放在 prompt；它必须落在数据库/事件/状态机中。摘要可能丢失关键约束，必须保留原始事件引用。

## 九、Java 后端实现边界

- 模型调用使用连接池、超时、取消、有限重试、熔断和 token/cost 指标。
- SSE/流式响应要处理客户端断开、背压、半截 JSON 和最终状态持久化。
- RAG ingestion 使用异步任务、Outbox、幂等 document version 和可重建索引。
- 向量查询必须带租户/权限/版本过滤，不能先召回再把越权文档交给模型。
- Agent Tool 通过 Java service 层执行，模型不能直接访问数据库、Kafka 或 Kubernetes。

## 十、NotifyFlow Knowledge Assistant

输入：任务状态、错误分类、脱敏日志、Runbook、版本和指标时间线。

流程：权限过滤 -> 混合检索 -> rerank -> 生成带引用摘要 -> 输出只读查询和建议审批动作。

禁止：把原始 Secret、完整用户 payload、跨租户日志或未脱敏 token 放进向量库/上下文；禁止 Agent 自动重放、清队列、扩容或改 RBAC。

## 十一、实验全部 Pending

本章计划实验：tokenizer 长度、attention mask、KV cache 预算、量化对照、embedding/ANN、chunk 消融、hybrid/rerank、RAG 评测、引用拒答、Tool 幂等和 Agent 故障恢复。未运行前不得填写召回率、准确率、tokens/s、成本或容量数字。
