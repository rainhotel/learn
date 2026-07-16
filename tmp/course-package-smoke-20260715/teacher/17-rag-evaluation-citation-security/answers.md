# 第 17 章参考答案与评分

## 1-4. 数据集与标注

完整数据集应按能力和风险分层，不追求简单题占多数后的高平均分。高质量 `EvaluationCase` 至少包含 query、identity/tenant/role、asOf、expected behavior、gold evidence、forbidden evidence、reference claims、tags、risk、来源和 annotation status。

关键解释：

- 没有 identity，无法判断同一证据对谁相关且可见。
- 没有 asOf/version，旧 Runbook 可能内容正确但时态错误。
- 没有 forbidden evidence，跨租户或敏感召回可能被普通 Recall 忽略。
- 没有 split 来源关系，同一事故的改写会让测试分数虚高。

开发集可用于调参；validation 用于方案选择；frozen test 限制查看和修改；red-team 独立报告硬失败；online replay 需脱敏、授权和审批。按 incident/document family 分组切分，不能只随机按 query 切分。

标注指南必须给正反例和优先级。内容相关但越权或过期时，不应作为当前请求的有效 gold evidence；可以单独标为 `CONTENT_RELEVANT_BUT_FORBIDDEN`。双标分歧应保留并仲裁，不能在计算一致性前互相商量。

## 5. 指标手算

假设三个等级大于 0 的结果分别对应全部三条 gold evidence：

```text
Recall@1 = 1/3
Recall@3 = 2/3
Recall@5 = 3/3 = 1
MRR = 1/1 = 1
```

`nDCG@5` 使用：

```text
DCG = (2^3-1)/log2(2)
    + (2^2-1)/log2(4)
    + (2^1-1)/log2(6)
    = 7 + 1.5 + 约0.387
    ≈ 8.887

IDCG = 7/log2(2) + 3/log2(3) + 1/log2(4)
     ≈ 7 + 1.893 + 0.5
     ≈ 9.393

nDCG@5 ≈ 0.946
```

若 relevant 定义或 gold 去重方式不同，必须先声明；同一 gold 的重复 chunk 不应被错误计作多个独立相关证据。

## 6-8. 检索评测

Hit@5 高仍可能失败：命中的是越权/过期版本；相关 chunk 被 context 截断；rerank 把 hard negative 排在真正证据前；模型忽略或误读证据；多跳问题只命中一半。故必须报告 context、权限、版本和生成指标。

方案对比必须固定数据集、corpus snapshot、embedding、top-k、硬件/region 和并发。保存完整 ranking 而不是只存 top-1；按 exact-token、paraphrase、multi-hop、stale 等 tag 分层。

全局 ANN 后过滤会让 top-k 被越权候选占满，合法证据没有机会进入；即使删除文本，score、document ID、日志或 rerank 服务也可能已经泄露。优先使用 identity-bound pre-filter、租户/安全分区索引或允许集合检索，并在 context/引用阶段再次授权。

## 9. 四类回答

- faithful but incorrect：忠实总结了旧 Runbook，但 `asOf` 应使用新版本。
- correct but unfaithful：模型凭内部知识答对错误码，却没有任何 context 支持。
- relevant but incomplete：回答了 Provider 503，却漏掉连接池耗尽这个必要原因。
- fully correct but unsafe：结论正确、引用正确，却包含另一租户原始手机号或 Secret。

四者失败机制和修复不同，安全失败也不能被质量平均分抵消。

## 10-11. 引用

先把复合句拆成原子 claim；每个可验证事实都应连接至少一个支持证据。citation completeness 的分母是需要外部证据的 claim。纯操作建议是否需要引用取决于其依据：若建议来自 Runbook 或基于某个指标，应引用；若只是明确标注的通用下一条只读检查，可以单独分类但仍要说明理由。

验证责任：

- JSON/schema/ID：确定性代码。
- tenant/ACL：授权服务重新判断。
- document/version/span/hash：文档存储和快照解析器。
- context membership：本次 run 的不可变 context manifest。
- claim entailment：人工 gold 或经校准 judge，边界样本人工复核。

仅由模型输出一个 URL 不足以通过。

## 12. 拒答

核心混淆矩阵是 expected answerability 与 actual answer/abstain。应报告 refusal precision/recall、answer coverage 和 unsafe answer rate，并按风险设置错误成本。

- no evidence 时实质回答是幻觉风险。
- access denied 时实质回答是泄露风险，成本通常更高。
- 可回答却拒答降低产品覆盖。
- conflict 时强行选边会制造错误确定性。

权限拒答只能说当前授权上下文不足，不能透露受限文档标题、存在性、摘要或相似度。

## 13-16. 红队与泄露

合格攻击样本覆盖可见正文、隐藏文本、metadata、URL、日志和结构化字段。安全断言不是“模型说不会”，而是：越权证据不进入 ranking/context；恶意指令不改变回答 schema、ACL 和动作边界；合成 canary 不出现在模型请求、输出、日志和截图中。

跨租户实验应检查五层：

1. retriever 没有 tenant-b candidate。
2. context manifest 没有 tenant-b chunk/hash。
3. 模型请求不含 canary。
4. 输出和引用不含 canary/tenant-b 事实。
5. audit 只记录 tenant-a 授权范围，不保存受限正文。

泄露面包括共享 cache key、向量 namespace、评测 artifact、模型 provider、日志、截图和复核 UI。最小披露引用可返回短授权 span、页码/章节、版本/hash 和受控查看链接；PII 脱敏，禁止为“验证”输出整篇原文。

## 17. Java Runner

Runner 必须把 dataset validation、snapshot、retrieval、generation、deterministic checks、judge、人审、aggregation 和 gate 分开。每个 case/stage 使用 `(runId, caseId, stage, configHash)` 唯一键或文件路径幂等；保存原始 ranking/context/response。进程恢复只重跑缺失或失败阶段，不能把失败 case 从分母静默排除。

并发要受有界 executor、rate limit、总 token/成本和 deadline 控制。429/传输错误有限重试；安全或 schema 错误不通过重试掩盖。

## 18. Judge 校准

从风险和能力分层抽取人工已仲裁样本，固定 judge 模型、版本、prompt 和解析器。计算每个标签的混淆矩阵、precision/recall 和系统性偏差，检查答案长度、顺序、自模型偏好和中文/代码差异。高风险、门槛附近和 judge/规则冲突进入人工复核。judge 或 prompt 升级要重跑同一校准集。

## 19. 回归门禁

门禁至少包含：

- 硬门禁：跨租户、ACL、Secret/PII、伪造引用、注入改变策略、关键 artifact 缺失。
- 相对门禁：冻结集及关键 tag 相对 baseline 的质量下界，容忍度依据重复运行波动。
- 预算门禁：P95/P99、token、query cost、错误/timeout。
- 完整性：预期 case 数、每阶段结果数、配置 hash 和人工复核状态一致。

门禁配置有版本、owner、理由和审批。降低门槛必须形成新版本和风险接受记录。

## 20-21. 消融和线上

消融一次只改 top-k、rerank、context budget 或模型之一，其余使用同一 config snapshot。报告总分之外的 tag diff、硬失败、P95、token 和成本，并标出 Pareto 前沿。

shadow 不展示候选答案，但仍需合法的数据发送权限；canary 使用小流量、固定用户范围和自动停止；rollout 分阶段。线上严重失败经授权、脱敏和仲裁后进入 development/red-team，不能直接污染 frozen gold。

## 22. 简历边界

当前可写“设计了覆盖检索、引用、拒答、ACL 和 prompt injection 的 RAG 评测体系，定义 Java 评测流水线与回归门禁”；不能写“Recall 提升 X%”“拦截率 100%”“P95 降至 X ms”。完成真实运行后，量化也必须注明数据集规模、模型/索引版本、环境和限制。

## 总评分

| 维度 | 分值 |
|---|---:|
| 数据集、版本与人工标注 | 15 |
| 检索/生成指标与手算 | 15 |
| 引用与拒答设计 | 15 |
| ACL、注入和泄露安全 | 20 |
| Java 评测流水线 | 15 |
| 回归、成本与线上闭环 | 15 |
| 事实边界与表达 | 5 |

## 发布门槛

- 未有冻结集、数据卡和人工仲裁，最高 69 分。
- 未保存 ranking/context/引用 span，只看最终回答，评测工程维度最高一半。
- 出现任何跨租户、Secret、ACL 或伪造引用未被硬门禁阻断，不得进入发布候选。
- 只用未经校准的 LLM judge，生成评测维度最高一半。
- 未真实运行 Java Runner、红队和回归，本章保持 Pending，无论书面得分多高。
