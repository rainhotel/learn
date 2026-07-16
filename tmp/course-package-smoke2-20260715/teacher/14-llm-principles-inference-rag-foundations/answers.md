# 第 14 章参考答案与评分

## 关键答案

- 标准 causal attention 通过 mask 阻止当前位置读取未来 token；KV cache 缓存历史 K/V，降低 decode 重复计算但增加显存。
- 召回率高不代表回答正确，还可能有 chunk 截断、权限/版本错误、rerank 错误、context 竞争、模型误读和引用映射错误。
- RAG 评测要分检索指标、生成指标、引用指标、安全指标、延迟和成本，不能只用一个 LLM judge 分数。
- Agent 是受限状态机，不是无限循环；Tool 的权限、幂等、超时、审计和 side effect 等级由代码控制。

## 评分

| 维度 | 分值 |
|---|---:|
| Transformer/推理原理 | 20 |
| KV/成本/性能分析 | 15 |
| RAG 检索与评测 | 25 |
| Java 后端实现 | 15 |
| Tool/Agent 安全 | 20 |
| 诚实证据表达 | 5 |

没有评测集、引用和权限边界的“RAG Demo”最高 60 分；没有 Tool 幂等与审批的 Agent 设计不得进入发布候选。
