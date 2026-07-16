# 隐私与安全报告

## 1. 当前判定

```text
Basic course text scan: NO HIGH-CONFIDENCE MATCHES IN CHECKED PATTERNS
Privacy release clearance: NOT COMPLETE
Security release clearance: NOT COMPLETE
```

零匹配只说明本次有限规则没有命中，不能证明不存在 Secret、个人信息、企业数据或供应链风险。

## 2. 本次自动扫描范围与结果

扫描日期：2026-07-15。

扫描范围为 `course/` 中的 Markdown、Java、JavaScript、YAML、XML、PowerShell、CSV、JSON 和 properties 文本文件。检查的高置信模式及命中文件数：

| 模式类别 | 命中文件数 |
|---|---:|
| AWS access key 形式 | 0 |
| PEM private key header | 0 |
| GitHub token 形式 | 0 |
| `sk-` 形式长 key | 0 |
| 引号包裹的常见 secret/token/password 赋值 | 0 |
| 常见电子邮箱形式 | 0 |
| 中国大陆手机号码形式 | 0 |

课程目录扩展名盘点未发现图片、音频、视频和字体文件。仓库根目录存在课程范围外的 `.env` 文件，本次没有读取其内容；发布流程必须明确排除它，不能直接打包整个工作区。

## 3. 未覆盖范围

- Git 历史、未被上述扩展名纳入的文件、压缩包、数据库、容器层和远端对象存储。
- 高熵 Secret、变形 token、短密码、内部域名、IP、租户 ID、日志业务标识和上下文相关个人信息。
- 依赖漏洞、恶意包、镜像漏洞、SAST/DAST、容器基线和 Kubernetes 安全配置。
- 外部链接内容、模型提示词泄露、训练数据授权和第三方服务的数据保留策略。
- 简历 PDF、JD 样本、录音、转写、截图及未来学习者提交物的人工隐私审查。

## 4. 发布前强制动作

1. 只从明确白名单生成学生包、教师包和证据包，禁止直接压缩仓库根目录。
2. 对当前文件、Git 历史、依赖、镜像和构建产物运行独立 Secret/漏洞扫描，并保存工具版本和原始报告。
3. 对简历、JD、面试反馈、日志、截图、语料和学习者作业执行人工匿名化复核。
4. 为 RAG/Agent 测试建立脱敏语料、租户隔离、工具权限、审批、审计和数据删除规则。
5. 明确数据收集目的、保留期限、访问人员、删除方式和学习者授权。
6. 对发现项建立严重度、负责人、修复证据和复测记录。

完成上述动作并关闭阻断项之前，本报告不能作为安全或隐私合规证明。
