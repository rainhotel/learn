# 第 15 章：企业文档处理、切分与 Ingestion

## 章节定位

- 类型：Data Pipeline + Java Backend + RAG Foundation + Project + Lab Design + Interview + Teach-back
- 难度：深入
- 建议学习时间：24-32 小时
- 先修章节：第 04-09 章可靠后端主链路、第 11 章超时与容量、第 14 章 RAG 基础
- 对应项目：NotifyFlow Knowledge Assistant 文档摄取平台

## 为什么重要

RAG 的输入不是“几个 PDF”，而是一套持续变化、权限复杂、格式不稳定的企业知识源。解析顺序错乱、OCR 误识别、ACL 丢失、旧版本残留或删除不彻底，都会让后续检索和回答在看似正常时产生错误。

本章不以“成功调一次文档解析 API”为目标，而是建立可追溯、可重放、可增量、可删除、可观测的 ingestion 管道。

## 当前状态

- 阶段：Draft
- 编写日期：2026-07-15
- 已完成：八件套、数据模型、状态机、失败恢复、质量指标和实验矩阵设计
- 未完成：解析器/OCR/对象存储/消息队列/Embedding 服务的真实运行与证据归档
- 实验状态：Pending

本章不能标记为 Lab Verified、Release Candidate 或 Released。

## 相邻章节边界

- 第 14 章解释 LLM、Embedding、RAG 和 Agent 的总体原理。
- 本章负责从企业数据源到“可索引 chunk manifest”的可靠数据工程链路。
- 后续章节负责 Embedding、向量数据库、混合检索、rerank、评测、引用和安全策略。
- 本章只定义 `EMBED_PENDING`、索引生成和激活合同，不用未经实验的数据宣称检索效果。

## 核心问题

1. 如何区分文件、逻辑文档、文档版本、解析产物、chunk 和索引生成？
2. PDF、DOCX、HTML、表格和扫描件分别会丢失哪些结构与语义？
3. OCR 何时启用，怎样保存置信度、坐标、原图和人工复核边界？
4. 为什么原始文件必须不可变，清洗结果和 chunk 必须可重建？
5. 如何设计结构优先、token 上限约束、可复现实验的 chunk 策略？
6. metadata、租户、ACL、来源、时间有效性为什么必须随 chunk 传播？
7. 更新、增量、重命名、撤权和删除怎样避免新旧版本同时可见？
8. 如何用幂等、队列、Outbox、DLT、对账和受控重放恢复失败任务？
9. Java 后端怎样隔离不可信解析器并控制 CPU、内存、超时和并发？
10. 如何观察解析质量、数据新鲜度、删除滞后和权限完整性？

## 学习成果

完成本章并通过真实实验后，学习者应能：

- 画出企业文档 ingestion 的状态机、数据模型和故障边界。
- 为 PDF、DOCX、HTML、表格、图片设计解析与降级策略。
- 解释固定窗口、结构切分、父子 chunk、表格和代码切分的取舍。
- 设计稳定的文档身份、版本、chunk lineage、ACL 和 provenance metadata。
- 实现至少一次安全更新、撤权、删除、失败恢复和索引代际切换。
- 用质量指标和 golden corpus 判断解析升级是否退化。
- 明确 Java 服务负责的确定性控制面与 Python/OCR/模型服务边界。
- 在简历和面试中只陈述真实运行过的格式、规模、指标和限制。

## 学习路径

1. 阅读 `lesson.md`，先理解原始层、规范层、chunk 层与索引层。
2. 用 `project-application.md` 完成 NotifyFlow 数据模型与状态机设计。
3. 在 `lab/README.md` 中选定版本、golden corpus 和断言，再开始编码。
4. 独立完成 `exercises.md`，提交设计和失败样本后再看答案。
5. 使用 `interview.md` 做机制追问，使用 `teach-back.md` 完成三档试讲。
6. 真实运行后保存原始输出，再申请把状态从 Pending 提升。

## 退出标准

- 能解释 born-digital、扫描、混合 PDF 的不同处理路径。
- 能给出 canonical document model，并保留标题、段落、表格、页码、坐标和来源。
- 能设计可重复的 chunk 实验，而不是声称某个固定大小普遍最优。
- 能证明 ACL 在 discovery、parse、chunk、index、query 和 delete 全链路不丢失。
- 能处理重复事件、进程崩溃、毒文档、部分写入、版本竞争和删除重试。
- 能定义解析覆盖率、空页率、OCR 置信度、chunk 分布、freshness 和 deletion lag。
- 能说明哪些结论已有运行证据，哪些仍是设计或资料草稿。

## 发布前缺口

- 固定 Java、Tika/PDFBox/POI、OCR、对象存储、队列和数据库版本。
- 建立脱敏 golden corpus，覆盖数字 PDF、扫描 PDF、混合 PDF、DOCX、HTML、XLSX 和损坏文件。
- 完成资源限制、zip bomb、超时、崩溃、重复消息和删除故障实验。
- 完成至少一次解析器升级回归、chunk 消融和权限/删除正确性报告。
- 完成陌生学习者实验复现、作业评分与 Teach-back 修订。

