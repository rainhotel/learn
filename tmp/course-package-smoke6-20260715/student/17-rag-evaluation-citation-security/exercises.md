# 第 17 章练习

## 一、评测集与标注

1. 为 NotifyFlow 事故助手设计 24 条评测样本的分层清单，至少覆盖 direct、multi-hop、exact token、hard negative、no-answer、access-denied、stale/conflict、injection 和 sensitive。
2. 写出 `EvaluationCase` JSON Schema 需要的字段和约束，说明为什么 `identity`、`asOf`、document version 与 forbidden evidence 不能省略。
3. 给出 development、validation、frozen test、red-team 和 online replay 五个集合的进入/退出规则，设计防止同事故改写和相邻 chunk 跨集合泄漏的方法。
4. 编写一页标注指南，定义 relevant、partially supporting、conflicting、stale、unauthorized、should abstain。让两名同学独立标 10 条并记录分歧；未实际标注前只提交计划。

## 二、检索指标

5. 某问题有 3 条 gold evidence，top-5 的相关等级依次为 `[3, 0, 2, 0, 1]`。计算 Recall@1/3/5、MRR 和 nDCG@5，并写出你的相关等级约定。
6. 设计一个 Hit@5 很高但 RAG 回答仍失败的例子，分别从权限、版本、context 截断、rerank 和模型误读解释。
7. 比较 vector、BM25、hybrid、hybrid + rerank 四个方案。除 Recall/MRR/nDCG 外，必须记录 ACL、延迟、内存/服务成本和失败 tag。
8. 设计一个“全局 ANN 先召回，再删除无权 chunk”会降低质量或泄露信息的反例，并提出安全替代方案。

## 三、生成、引用与拒答

9. 分别构造：faithful but incorrect、correct but unfaithful、relevant but incomplete、fully correct but unsafe 四类回答，解释为什么不能合成一个平均分。
10. 把一段包含三个事实和一个建议的回答拆为 claim，建立 claim-citation 图，并计算 citation completeness。判断建议是否需要事实引用。
11. 设计引用验证器：schema、ACL、document/version、span/hash、context membership 和 entailment 分别由什么机制检查？
12. 为 ANSWER、ABSTAIN_NO_EVIDENCE、ABSTAIN_ACCESS_DENIED、ABSTAIN_CONFLICT 设计混淆矩阵和错误成本。说明权限拒答为什么不能泄露文档存在性。

## 四、安全与红队

13. 写 12 条间接 prompt injection 样本，分别放在 Runbook、事故标题、日志、HTML/PDF 隐藏文本、URL 和 metadata 中。每条给出安全断言，不要使用真实恶意地址或 Secret。
14. 设计跨租户 canary 实验：tenant-b 的受限文档包含合成标记，tenant-a 发起语义相似和精确查询。列出 retriever、context、model、output 和 audit 五层断言。
15. 对缓存、向量库、评测日志、模型 API、截图和人工复核页面做数据泄露威胁建模，给出预防、检测和响应。
16. 用户要求“输出原文以便验证引用”。设计最小披露策略，兼顾可验证性、版权、PII 和 ACL。

## 五、评测工程与发布

17. 画出 Java 21 评测 Runner，包含数据校验、快照、并发调度、检索、生成、确定性检查、judge、人工复核、聚合和门禁。说明如何幂等续跑。
18. 设计 LLM judge 校准实验：人工 gold、prompt/模型固定、分层样本、混淆矩阵、分歧复核和升级策略。
19. 给出一份回归策略：硬安全门禁、相对质量门禁、延迟/成本预算、样本缺失策略和门禁变更审批。不得预填未验证的通过率数字。
20. 对 top-k、rerank、context budget 和生成模型做消融，设计“每次只改一个变量”的配置矩阵和 Pareto 报告。
21. 设计 shadow -> canary -> rollout 的线上验证，包含隐私、采样、自动停止、回滚、用户反馈和线上失败进入离线集的审批。
22. 写一段诚实的项目简历描述：只能陈述已实现/已运行事实；本章当前只能描述评测设计和待运行内容，不得填写 Recall、faithfulness、拦截率或成本改善。

## 作业提交

- 评测数据卡、JSONL 合同、标注指南和 split 泄漏检查设计。
- 指标字典与至少三题手算过程。
- claim-citation 图、引用验证流程和拒答混淆矩阵。
- 红队 taxonomy、合成 canary 与安全断言。
- Java Runner 架构、run manifest 和回归门禁策略。
- 一份 baseline/candidate 报告模板，清楚标注当前 Pending。
- 5/15/45 分钟 Teach-back 录制计划。

## 禁止项

- 不得用几个手选问题代替冻结评测集。
- 不得把 LLM judge 分数当成无需校准的真值。
- 不得用高平均质量抵消任何跨租户、Secret 或 ACL 失败。
- 不得把“回答带链接”直接称为引用准确。
- 不得把未运行的红队、延迟、成本或回归门禁写成已通过。
