# 第 15 章练习

## 一、机制与数据建模

1. 区分 source、document、document version、parse artifact、chunk 和 index generation，并给出各自稳定 ID。
2. 为数字 PDF、扫描 PDF、混合 PDF、DOCX、HTML 和 XLSX 写解析决策表，列出至少一个结构丢失风险。
3. 设计 canonical document JSON Schema，覆盖标题、段落、表格、代码、图片、脚注、页码和坐标。
4. 说明原始 bytes、规范化文本和脱敏文本为什么应分层保存，并写出各层保留策略。
5. 给出 Unicode、页眉页脚、连字符和空白清洗的四个反例，说明如何回滚。

## 二、Chunk、Metadata 与权限

6. 为 NotifyFlow Runbook 设计结构优先 chunk 策略，说明 token 上限、overlap、表格、代码和父子 chunk 规则。
7. 设计一个消融实验，比较固定窗口、结构切分和父子 chunk。明确数据集、指标、控制变量和失败样本。
8. 写出 chunk metadata 必填字段，并解释每个字段支持哪类引用、权限、更新或排障需求。
9. 比较 ACL 快照展开和查询时求值。设计授权服务超时、组成员变更和跨租户攻击测试。
10. 设计 bytes hash、normalized hash 和 near-duplicate 的使用边界，避免错误合并不同权限/有效期的文档。

## 三、可靠管道与 Java 工程

11. 画完整 ingestion 状态机，区分 job 与 attempt、暂时失败、永久失败、资源失败和 UNKNOWN。
12. 为 `PARSE` 与 `CHUNK` 两个 stage 设计幂等键、唯一约束、Outbox 事件和恢复查询。
13. 设计 v1 到 v2 的 generation 构建、校验、原子激活、回滚和旧版本回收时序。
14. 设计源删除与 ACL 撤权流程，列出所有必须清理的派生存储，并给出完成证明。
15. 为 Java 控制服务划分下载、解析/OCR、对象存储和 Embedding RPC 的执行器与容量限制。
16. 设计 Parser worker 安全边界，覆盖文件大小、zip bomb、页数、CPU、内存、临时磁盘、超时和外部网络。

## 四、质量、面试与证据

17. 建立至少 12 个文件的 golden corpus 规格，包含正常、边界、损坏和攻击样本。
18. 设计低基数 Metrics、结构化日志、Trace 和每天的 source/manifest/delete 对账查询。
19. 给出“任务成功但知识不可用”的六种情况，并为每种情况指定检测证据。
20. 写一段简历项目描述，区分“已设计”“已实现”“已真实运行”，不得虚构格式数量、吞吐或准确率。

## 作业提交物

- 架构图、状态机和 v1/v2 激活时序图。
- canonical document 与 chunk schema。
- DDL、API、幂等键和错误分类表。
- golden corpus 清单、实验设计和质量门禁。
- ACL/删除威胁模型与对账方案。
- 5 分钟录音和一页失败样本复盘。

