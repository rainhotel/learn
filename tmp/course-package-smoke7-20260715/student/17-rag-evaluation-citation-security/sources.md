# 第 17 章资料与验证状态

## 一、RAG、检索与评测论文

1. Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks：<https://arxiv.org/abs/2005.11401>
2. BEIR: A Heterogeneous Benchmark for Zero-shot Evaluation of Information Retrieval Models：<https://arxiv.org/abs/2104.08663>
3. RAGAS: Automated Evaluation of Retrieval Augmented Generation：<https://arxiv.org/abs/2309.15217>
4. ARES: An Automated Evaluation Framework for Retrieval-Augmented Generation Systems：<https://arxiv.org/abs/2311.09476>
5. Enabling Large Language Models to Generate Text with Citations (ALCE)：<https://arxiv.org/abs/2305.14627>
6. RAGTruth: A Hallucination Corpus for Developing Trustworthy Retrieval-Augmented Language Models：<https://arxiv.org/abs/2401.00396>
7. Lost in the Middle: How Language Models Use Long Contexts：<https://arxiv.org/abs/2307.03172>
8. Stanford Introduction to Information Retrieval, Evaluation：<https://nlp.stanford.edu/IR-book/html/htmledition/evaluation-in-information-retrieval-1.html>

## 二、安全与风险资料

9. OWASP Top 10 for LLM Applications：<https://owasp.org/www-project-top-10-for-large-language-model-applications/>
10. OWASP LLM Prompt Injection Prevention Cheat Sheet：<https://cheatsheetseries.owasp.org/cheatsheets/LLM_Prompt_Injection_Prevention_Cheat_Sheet.html>
11. NIST AI Risk Management Framework：<https://www.nist.gov/itl/ai-risk-management-framework>
12. NIST AI 600-1, Generative Artificial Intelligence Profile：<https://doi.org/10.6028/NIST.AI.600-1>
13. MITRE ATLAS：<https://atlas.mitre.org/>
14. Google Secure AI Framework (SAIF)：<https://saif.google/>

## 三、工程规范

15. Java 21 API：<https://docs.oracle.com/en/java/javase/21/docs/api/>
16. JSON Schema 2020-12：<https://json-schema.org/draft/2020-12>
17. OpenTelemetry Specifications：<https://opentelemetry.io/docs/specs/>
18. Unicode Technical Standard #39, Security Mechanisms：<https://unicode.org/reports/tr39/>

## 来源使用规则

- 论文用于理解指标和方法，不把论文数据集分数套用到 NotifyFlow。
- Recall/MRR/nDCG 的实现必须固定 relevance、去重、空 gold 和错误 case 的计算口径。
- RAGAS、ARES 或其他自动框架只是工具候选，不替代业务 gold、ACL、确定性引用验证和人工复核。
- OWASP、NIST、MITRE 和 SAIF 提供威胁与治理框架；具体控制必须映射到 NotifyFlow 的租户、文档、模型、缓存、日志和审批链路。
- 所有模型、Embedding、rerank、judge 和服务文档要固定版本；服务行为变化后重新校准。
- 不复制论文、标准或第三方评测集的大段正文；图表自行重绘并保留引用。

## 需要在实验前固定的资料

- 被选模型 Provider 的数据保留、训练使用、区域和日志政策。
- 向量数据库/搜索引擎的 ACL filter 语义、过滤时机和版本限制。
- Reranker、Embedding、Tokenizer 和生成模型的具体模型卡。
- Java JSON、指标、报告和模型客户端依赖的版本与许可证。
- 评测语料的授权、隐私、保留和再分发边界。

## 当前验证状态

| 项目 | 状态 | 证据 |
|---|---|---|
| Recall/MRR/nDCG 与 RAG 指标定义 | 资料核验/讲义初稿 | IR 教材、RAGAS、ARES |
| Claim-level 引用与 hallucination taxonomy | 资料核验/讲义初稿 | ALCE、RAGTruth |
| Prompt injection/泄露/风险框架 | 资料核验/讲义初稿 | OWASP、NIST、MITRE、SAIF |
| NotifyFlow 数据集与人工标注 | Pending | 尚无 JSONL、数据卡和仲裁记录 |
| Java Evaluation Runner | 设计初稿 | 尚无编译和运行输出 |
| 自动 judge 校准 | Pending | 尚无人工 gold 与混淆矩阵 |
| 红队、ACL 和 canary | Pending | 尚无攻击运行和审计证据 |
| 线上 shadow/canary | Pending | 尚无真实线上评测环境 |

本章不能标记为 Lab Verified、Release Candidate 或 Released。
