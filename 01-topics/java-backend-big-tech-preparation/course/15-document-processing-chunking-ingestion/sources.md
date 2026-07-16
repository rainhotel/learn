# 第 15 章资料与核验状态

## 使用说明

本章优先列一手规范、项目官方文档和安全指南。当前文件完成了资料入口和使用边界设计；尚未在本章固定依赖版本、下载组件或真实运行实验。版本、页面内容和许可证需在实验启动前再次核验。

## 格式解析与 Java 组件

1. Apache Tika 官方站点：<https://tika.apache.org/>
   - 用途：统一内容检测与 metadata/文本抽取入口。
   - 边界：统一 API 不保证保留所有版面语义；不可信文件仍需资源隔离。
2. Apache PDFBox 官方站点：<https://pdfbox.apache.org/>
   - 用途：PDF 文本、页面和低层对象处理。
   - 边界：PDF 的绘制顺序不等于语义阅读顺序。
3. Apache POI 官方站点：<https://poi.apache.org/>
   - 用途：DOCX/XLSX 等 Office Open XML 文档处理。
   - 边界：正文、表格、批注、页眉页脚、公式等需要分别处理。
4. WHATWG HTML Living Standard：<https://html.spec.whatwg.org/>
   - 用途：HTML/DOM 语义与解析边界。
5. jsoup 官方文档：<https://jsoup.org/>
   - 用途：Java HTML 解析、DOM 清洗与 selector。

## OCR、文本与文档规范

6. Tesseract User Manual：<https://tesseract-ocr.github.io/tessdoc/>
   - 用途：OCR 引擎、语言和输出格式边界。
   - 边界：置信度不是业务字段正确性的充分证明。
7. Unicode Standard Annex #15, Unicode Normalization Forms：<https://unicode.org/reports/tr15/>
   - 用途：NFC/NFKC 等规范化语义。
   - 边界：兼容规范化可能改变代码、标识符或法律文本。
8. PDF Association, PDF 2.0 / ISO 32000 information：<https://pdfa.org/resource/pdf-specification-index/>
   - 用途：PDF 规范入口和格式能力边界。

## 安全与资源隔离

9. OWASP File Upload Cheat Sheet：<https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html>
   - 用途：扩展名、类型、文件名、大小、存储和恶意文件控制。
10. OWASP XML External Entity Prevention Cheat Sheet：<https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html>
    - 用途：XML/Office 文档解析时的外部实体风险。
11. Apache Commons Compress Security Reports：<https://commons.apache.org/proper/commons-compress/security.html>
    - 用途：压缩容器依赖的安全公告入口。

## 可靠管道、对象版本与批处理

12. Spring Batch Reference Documentation：<https://docs.spring.io/spring-batch/reference/>
    - 用途：可重启批处理、Job/Step、执行状态与 skip/retry 思路。
13. Apache Kafka Documentation：<https://kafka.apache.org/documentation/>
    - 用途：至少一次消费、幂等和消息处理语义；需与第 07-08 章结合。
14. Amazon S3 Versioning documentation：<https://docs.aws.amazon.com/AmazonS3/latest/userguide/Versioning.html>
    - 用途：对象版本和删除标记的参考语义。
    - 边界：课程可使用兼容对象存储，但不能假设所有实现行为完全相同。
15. Debezium Documentation：<https://debezium.io/documentation/>
    - 用途：数据库源增量变更/CDC 的可选方案。
    - 边界：CDC 事件不自动等于业务完整快照，仍需 bootstrap 和对账。

## RAG 数据与评测衔接

16. Retrieval-Augmented Generation：<https://arxiv.org/abs/2005.11401>
    - 用途：RAG 总体背景，与第 14 章衔接。
17. Lost in the Middle: How Language Models Use Long Contexts：<https://arxiv.org/abs/2307.03172>
    - 用途：说明“塞入更多上下文”不保证更好使用证据。
18. NIST AI Risk Management Framework：<https://www.nist.gov/itl/ai-risk-management-framework>
    - 用途：AI 数据治理、可追溯和风险管理背景。

## 核验矩阵

| 主题 | 当前状态 | 发布前证据 |
|---|---|---|
| Tika/PDFBox/POI/jsoup | 官方入口已列，版本 Pending | 固定版本、许可证、golden corpus 输出 |
| OCR/Tesseract | 官方入口已列，运行 Pending | 模型/语言/参数、原图、置信度与错误样本 |
| 文件安全/资源限制 | 指南入口已列，注入 Pending | zip bomb/超时/隔离测试与资源曲线 |
| 版本/队列/批处理 | 设计初稿 | 数据库/队列/对象存储真实时间线 |
| Chunk/ACL/删除 | 设计初稿 | 消融、越权、撤权、删除与对账证据 |
| Java 控制服务 | 接口和边界初稿 | 编译、测试、故障注入和运行报告 |

## 引用与版权边界

- 课程使用自写解释和自建脱敏样本，不复制官方文档或付费资料的大段正文。
- 第三方代码、测试语料和样例文件进入仓库前必须检查许可证与隐私。
- 任何性能、准确率和格式支持结论必须绑定组件版本、语料、环境与原始输出。
- 本章当前不能标记为 Lab Verified。

