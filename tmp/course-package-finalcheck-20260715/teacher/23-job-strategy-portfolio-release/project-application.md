# NotifyFlow 求职作品与课程发布方案

## 1. 三类作品入口

| 版本 | 首屏重点 | 核心证据 |
|---|---|---|
| Java 后端 | 可靠通知、事务、MQ、恢复、排障 | 第 01-13、19 章 |
| Agent 后端 | ingestion、retrieval、评测、Tool Runtime | 第 09、14-18 章 |
| 制造数字化 | 可追溯任务、权限、集成、长期运维 | 第 04-13、19 章 |

三者引用同一事实和代码，不创建互相冲突的项目历史。

## 2. 发布目录

```text
portfolio/
  overview.md
  architecture.md
  quickstart.md
  evidence-index.md
  incident-report.md
  rag-evaluation.md
  security-boundary.md
  resume-variants/
  interview-feedback/
  changelog.md
```

## 3. 证据索引

每个简历 bullet 链接到 claimId；claimId 再链接代码、运行日期、环境、原始输出、结论和限制。没有真实运行结果时只链接设计文档并使用“设计”措辞。

## 4. 投递看板

状态使用 `SAMPLED -> QUALIFIED -> APPLIED -> TEST -> INTERVIEW -> OFFER/REJECTED/WITHDRAWN`。保存状态变化时间，避免把未回复当作明确拒绝。

## 5. 发布决策

课程 Release Candidate 必须通过内容、运行、教学、版权和隐私五类门禁。任一阻断项未关闭就维持 Draft，并在产品页公开已验证范围。
