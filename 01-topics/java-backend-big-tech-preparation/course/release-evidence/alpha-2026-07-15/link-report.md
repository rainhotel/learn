# 本地链接与结构报告

## 1. 自动检查

执行日期：2026-07-15。

执行命令：

```powershell
& "01-topics/java-backend-big-tech-preparation/course/tools/audit-course.ps1"
```

本证据包创建后的最终输出应以本文件记录和同次命令的控制台原始输出共同复核。自动检查覆盖章节八件套、`lab/README.md`、常见占位标记、尾随空格和本地 Markdown 文件路径。

```text
chapters=23
markdown=239
java=26
javascript=3
yaml=2
powershell=8
missing_required_files=0
trailing_whitespace_matches=0
broken_local_links=0
COURSE_AUDIT_PASSED
```

检查器报告的常见占位标记命中数为 0。

## 2. 当前结论

在本次自动检查范围内：

- 23 个编号章节的标准文件完整。
- 未发现被检查器识别的本地 Markdown 断链。
- 未发现被检查器识别的常见占位标记或尾随空格。

此结论只代表当前文件树和检查器覆盖范围，不代表内容正确、实验通过或发布构建已经验收。

## 3. 未覆盖范围

- 外部 HTTP/HTTPS URL 的可达性、重定向、登录要求和内容漂移。
- Markdown 标题锚点、渲染器差异、目录生成和移动端阅读体验。
- PDF、DOCX、网站或压缩包等最终交付形态的链接。
- 代码内部资源路径、Docker/Kubernetes runtime 引用和远端模型/数据端点。
- Windows 之外的大小写敏感路径兼容性。

这些项目必须在 Release Candidate 构建中另行验证。
