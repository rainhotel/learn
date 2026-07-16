# 第 15 章 Teach-back

## 5 分钟：为什么“上传 PDF”不是 RAG Ingestion

### 目标

让听众记住四句话：

1. PDF 文本不等于语义结构。
2. 原始文件不可变，派生结果可重建。
3. ACL、版本和删除与正文同等重要。
4. 新版本先隔离构建，再原子激活。

### 讲解顺序

画出 `source -> raw -> parse -> canonical -> chunk -> generation -> active`，再用“扫描页 OCR 错误码”和“撤权后旧 chunk 仍可见”两个反例收尾。

## 15 分钟：一条可靠文档管道

### 结构

- 3 分钟：六种身份和不可变原始层。
- 4 分钟：PDF/DOCX/HTML/XLSX/OCR 的结构边界。
- 3 分钟：结构优先 chunk、metadata、ACL 和 lineage。
- 3 分钟：状态机、幂等、UNKNOWN、DLT 和 generation 激活。
- 2 分钟：quality metrics、golden corpus 与证据边界。

### 必答追问

- 为什么固定字符窗口不可靠？
- Parser 写成功但调用方超时怎么办？
- 删除一份文档要检查哪些存储？
- 授权服务超时时为什么不能降级为不过滤？

## 45 分钟：NotifyFlow 企业知识摄取设计评审

### 第一部分：需求与威胁（8 分钟）

定义知识源、格式、更新频率、freshness、租户、ACL、删除、保留期和非目标。展示一个毒文档、一个混合 PDF 和一个撤权场景。

### 第二部分：数据与处理（12 分钟）

讲解 document/version/stage job/chunk manifest/generation 数据模型，展示 canonical document 和 chunk schema，解释表格、代码、OCR 与清洗规则。

### 第三部分：可靠性（10 分钟）

讲解 Outbox、stage 幂等、lease、失败分类、UNKNOWN 查询、受控重放、v1/v2 原子激活和删除对账。

### 第四部分：质量与 Java 边界（10 分钟）

展示 golden corpus、质量门禁、低基数指标和执行器隔离。解释 Java 控制面与解析/OCR worker 的合同。

### 第五部分：证据审查（5 分钟）

逐项标记 Design、Static Checked、Runtime Pending 或 Lab Verified。本章当前所有运行实验都必须标记 Pending。

## 演示脚本

1. 同一 source revision 重复提交，预期返回同一 version/job。
2. v2 构建时注入解析失败，预期 v1 保持 active。
3. Parser 写产物后中断 ack，预期通过 hash 对账恢复。
4. 撤销 ACL，预期查询先不可见，后台删除完成后有审计。
5. 对扫描页展示 OCR warning 与原图定位。

在真实运行前，这些只是演示设计，不得展示伪造输出。

## 评分表

| 项目 | 分值 |
|---|---:|
| 业务问题与边界清楚 | 15 |
| 格式/OCR/结构解释准确 | 20 |
| Chunk、ACL、版本与删除 | 25 |
| 幂等、失败恢复与激活 | 20 |
| 可观测性与实验设计 | 10 |
| 表达、追问与证据诚实 | 10 |

## 试讲后复盘

- 听众在哪个身份或状态概念上混淆？
- 哪个反例最能说明数据正确性问题？
- 是否把设计方案误说成运行事实？
- 哪个问题需要补来源、代码或真实实验？
- 下一版讲义要删减、重画或增加什么？

