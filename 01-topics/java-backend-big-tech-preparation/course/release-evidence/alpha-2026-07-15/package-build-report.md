# 学生包与教师包构建报告

## 1. 构建命令

```powershell
& course/tools/build-packages.ps1 `
  -OutputRoot tmp/course-package-smoke7-20260715
```

运行日期：2026-07-15。

该输出目录仅用于构建验证，不是正式销售压缩包。

## 2. 真实结果

```text
chapters=23
student_files=243
teacher_files=267
student_answers=0
student_lab_evidence_files=0
teacher_answers=23
student_answer_references=0
invalid_student_chapters=0
student_broken_links=0
teacher_broken_links=0
teacher_student_mismatches=0
student_sensitive_matches=0
teacher_sensitive_matches=0
COURSE_PACKAGES_BUILT
```

## 3. 已验证范围

- 学生包按 allowlist 构建，而不是复制后人工删除。
- 学生包每章包含七个学习文件和 `lab/README.md`，不含 `answers.md` 与教师参考 evidence 文件。
- 教师包每章包含一个 `answers.md`。
- 学生公共内容在教师包中逐字节一致。
- 两个包的本地 Markdown 链接均无断链。
- 没有匹配到个人绝对路径或私钥头。
- 两个包均生成 SHA-256 manifest。

## 4. 未验证范围

- 尚未生成并检查最终 ZIP 文件，也未执行解压后二次校验。
- 尚未完成外部 URL、许可证、依赖、模型和数据集条款审计。
- Secret/PII 扫描只覆盖有限模式，不替代人工安全审查。
- 尚未在另一台干净机器上复现构建。
- 尚未形成正式版本号、签名和交付政策。

因此答案隔离的构建机制已通过本机 smoke test，但整个课程仍不是 Release Candidate。
