# 第 14 章资料与验证状态

## 论文与官方资料

1. Attention Is All You Need：<https://arxiv.org/abs/1706.03762>
2. RoFormer / Rotary Position Embedding：<https://arxiv.org/abs/2104.09864>
3. Retrieval-Augmented Generation for Knowledge-Intensive NLP：<https://arxiv.org/abs/2005.11401>
4. Dense Passage Retrieval：<https://arxiv.org/abs/2004.04906>
5. Sentence-BERT：<https://arxiv.org/abs/1908.10084>
6. Efficient and Robust Approximate Nearest Neighbor Search Using HNSW：<https://arxiv.org/abs/1603.09320>
7. FAISS: A Library for Efficient Similarity Search：<https://github.com/facebookresearch/faiss>
8. FlashAttention：<https://arxiv.org/abs/2205.14135>
9. vLLM / PagedAttention：<https://arxiv.org/abs/2309.06180>
10. Hugging Face Tokenizers documentation：<https://huggingface.co/docs/tokenizers/>
11. Hugging Face Transformers documentation：<https://huggingface.co/docs/transformers/>

## 来源使用规则

- 论文用于解释机制，不把论文实验硬件和数字直接套到 NotifyFlow。
- 向量库、Embedding 和推理框架必须记录版本、模型、维度、距离、索引参数和硬件。
- RAG 结果必须提供评测集、标注规则、失败样本和权限过滤说明。
- 模型 API 返回的文本不能替代业务状态、权限和数据库事实。

## 当前验证状态

| 项目 | 状态 | 证据 |
|---|---|---|
| Transformer/Attention/RoPE | 资料核验/讲义初稿 | 原论文 |
| 推理 prefill/decode/KV cache | 资料核验/讲义初稿 | FlashAttention、vLLM 论文 |
| Embedding/ANN/HNSW | 资料核验/讲义初稿 | DPR、SBERT、HNSW、FAISS |
| RAG 评测/引用/权限 | 设计初稿 | 尚无本章评测集运行输出 |
| Java Model Gateway/Agent Runtime | 设计初稿 | 尚无依赖和服务运行证据 |
| Tool 安全/故障恢复 | 设计初稿 | 需结合第 08-12 章实验 |

本章不能标记为 Lab Verified、Release Candidate 或 Released。
