# 第 17 章面试追问

## 一、评测框架

### 如何评测一个 RAG 系统？

把链路拆成 retrieval、context、generation、citation、refusal、security 和 operations。固定数据集与语料快照，保存完整 ranking/context/response，用确定性规则、人工 gold 和校准 judge 分工，最后做 baseline diff 和发布门禁。

### 为什么不能只看回答准确率？

准确答案可能来自模型记忆而没有证据；忠实答案也可能引用过期或越权文档。单一准确率还会隐藏召回、引用、拒答、安全、延迟和成本失败。

### 评测集怎么构造？

从真实产品问题和风险 taxonomy 分层，覆盖 direct、改写、多跳、精确 token、hard negative、无答案、权限、版本冲突、注入和敏感数据。按事故/document family 切分，冻结测试集并记录数据卡、来源和标注状态。

### 如何防止过拟合评测集？

分 development/validation/frozen/challenge；限制 frozen 访问；调参只看开发和验证；保存所有实验和门禁变更；用时间切分与新在线失败检验泛化。不能每次失败就修改 gold 让系统通过。

## 二、检索指标

### Recall@k、MRR、nDCG 的区别？

Recall@k 看全部相关证据覆盖；MRR 只重视第一个相关结果的位置；nDCG 支持分级相关并考虑排序。多证据问答不能只用 MRR，分级 gold 不可靠时 nDCG 也会受标注噪声影响。

### Hit@5 很高为什么回答仍差？

相关 chunk 可能越权、过期、被 context 截断，或只覆盖多跳问题的一半；rerank 可能把 hard negative 放前面；模型也可能误读。要看最终 context 和 claim-level 生成指标。

### 如何评测 hybrid 和 rerank？

固定 corpus、embedding、数据集和其余配置，比较 vector/BM25/hybrid/rerank 的 ranking、tag 指标、ACL、延迟和成本。保存每 case diff，不能只比总 Recall。

### ACL 过滤放在哪里？

尽可能在召回前绑定 identity 过滤，避免越权 chunk 进入候选、rerank、模型或日志；context 和引用返回时再次授权。先全局召回再删可能损失合法 Recall，也扩大泄露面。

## 三、生成与引用

### Faithfulness 和 correctness 有什么区别？

Faithfulness 判断回答是否由实际 context 支持；correctness 判断是否符合参考事实或权威状态。忠实总结旧文档可能不正确，模型凭记忆答对也可能不忠实。两者必须分别报告。

### Answer relevance 有什么用？

它判断是否回应问题、是否冗余，但不保证事实正确。一个简洁、相关但错误的回答仍失败。

### 引用怎么评测？

先拆 claim，再检查每个 claim 的 citation。确定性验证 ID、ACL、document/version/span/hash 和 context membership；人工或校准 judge 判断 entailment；分别报告 citation completeness、precision/entailment 和 validity。

### 回答带 URL 不算引用吗？

不够。URL 可能指向错误版本、无权页面或根本不支持该 claim。必须能在当前身份下复取同一版本和 span，并证明该证据进入过本次 context。

### 一个 claim 可以有多个引用吗？

可以，多跳或因果结论常需要多条证据。反过来一条证据也可支持多个 claim。评测结构应是图，而不是简单的一问一链接。

## 四、拒答

### 如何评测“我不知道”？

给每个 case 标 expected behavior，建立应回答/应拒答与实际行为的混淆矩阵，报告 refusal precision/recall、answer coverage 和 unsafe answer rate。按 no evidence、access denied、conflict 分开。

### 拒答越多越安全吗？

可能更安全但产品无用。要在可回答样本保持覆盖，同时对高风险无答案和权限样本提高召回。阈值按风险分层，并观察错误成本。

### 权限不足时如何回答？

只说明当前授权上下文不足，提供合法申请或只读检查路径。不能透露受限文档标题、存在性、摘要、相似度或其中的事实。

## 五、Judge 与人工标注

### 能完全用 LLM judge 吗？

不能。Judge 有位置、长度、自偏好、提示敏感和版本漂移；它也无法验证未提供的 ACL/业务状态。确定性规则优先，judge 先用人工 gold 校准，高风险和分歧样本人工复核。

### 如何校准 judge？

固定模型/prompt/参数，在分层人工仲裁集上计算各标签混淆矩阵、precision/recall，分析偏差。升级模型或 prompt 时重跑同一校准集，不能把不同 judge 分数直接比较。

### 人工标注一致性低怎么办？

先检查任务和指南是否模糊，查看分歧类别，补充正反例和决策优先级，再独立重标并仲裁。不能简单删除争议样本或以多数票掩盖领域问题。

## 六、安全

### RAG 的 prompt injection 来自哪里？

用户输入是直接注入；Runbook、日志、网页、PDF、metadata 和工具结果可构成间接注入。任何可检索内容都应视为不可信数据，不能改变系统权限和指令。

### 只做内容清洗够吗？

不够。攻击可被编码、混入正常内容且清洗会误删业务信息。需要 ingestion 标记、检索前 ACL、context 最小化、指令/数据隔离、结构化输出、引用/敏感验证、审计和 kill switch 多层防护。

### 如何证明没有跨租户泄露？

不能用“代码看起来有过滤”证明。用合成 canary 和 tenant-a/tenant-b 对抗查询，检查 ranking、context、模型请求、回答、引用、缓存和审计；任何一层出现受限内容都硬失败。

### 模型供应商会不会泄露数据？

需要核对数据处理合同、保留/训练政策、区域和日志；发送前最小化与脱敏；敏感场景使用批准的模型端点。评测 artifact 和 judge 调用也属于数据流，不能忽略。

### 如何防止引用泄露原文？

按当前 identity 返回最小支持 span、版本和受控链接；PII 脱敏并限制长度。无权用户不应看到标题、snippet 或文档存在性。后台 hash 可验证完整性，不要求公开全文。

## 七、Java 评测工程

### Java Runner 为什么要保存完整 ranking 和 context？

只保存最终答案无法区分召回、截断和生成错误，也无法复核 ACL。完整 ranking/context 加配置 hash 才能做 case-level diff 和引用验证。

### 并发怎么设计？

使用有界 executor/结构化并发，按检索与模型端点分别限流，设置 case deadline、总 token/成本预算。429 和安全传输错误有限退避；失败 case 明确记录，不从分母中消失。

### 如何从中断继续？

每阶段以 `(runId, caseId, stage, configHash)` 幂等落盘或入库，append-only 记录状态。重启后只续跑未完成阶段，配置变化则开启新 run。

### temperature=0 就可重复吗？

不一定。外部服务的模型、批调度和底层实现可能变化。保存模型版本、请求/响应和时间，关键样本重复运行，门禁阈值考虑测量波动。

## 八、回归、成本与线上

### 回归门禁怎么设？

硬门禁覆盖 ACL、跨租户、Secret、伪造引用和 artifact 缺失；质量指标按 baseline 和风险 tag 设容忍下界；另有限制 P95/P99、token、成本和错误。门禁版本化并审批。

### 新版本平均分更高就能发布吗？

不能。可能某个高风险 tag 回归、出现一次跨租户泄露、P95/成本不可接受或样本缺失。必须检查硬失败、分层 diff 和 Pareto 取舍。

### 如何做线上评测？

先 shadow，再小流量 canary，最后分阶段 rollout。记录授权范围内的系统指标、用户反馈、引用解析和安全事件，设置自动停止/回滚。线上失败脱敏、审批和仲裁后进入离线集。

### 用户点赞能代表答案正确吗？

不能。点赞受表达、期望和幸存偏差影响；引用点击也不证明支持 claim。它们是弱反馈，需要和人工复核、业务结果及安全指标结合。

### 如何做质量与成本取舍？

同时比较 Recall/faithfulness/citation/refusal、安全、P95、token 和 query cost，画 Pareto 前沿。通过 top-k、rerank、context budget、模型和输出长度消融；每次只改一个变量。

## 九、项目边界

### 第 14、17、18 章有什么区别？

第 14 章讲模型、Embedding、检索和 RAG 原理；第 17 章建立 RAG 的数据集、指标、引用、安全和发布门禁；第 18 章实现 Tool、Memory、Agent 状态机和副作用可靠执行。

### 当前可以在简历写什么？

只能写已完成的评测架构、数据合同、指标和实验设计，并注明实验 Pending。真实跑完后才可写数据集规模和指标，且要附模型、版本、环境和限制；不能预填提升百分比。

### 最严重的 RAG 失败是什么？

跨租户/Secret 泄露和越权行为通常是硬失败；其次是高置信无依据回答或错误引用。回答时结合业务风险，而不是只按某个平均质量分排序。
