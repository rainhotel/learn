# 课程质量审计（2026-07-15）

## 1. 审计范围

审计目录：`course/` 下所有编号章节、课程产品文件、实验源码与状态说明。

检查项：

- 单章八件套和 `lab/README.md`。
- 状态、Verified/Pending 和发布边界。
- 常见占位标记、尾随空格和 `git diff --check`。
- 当前课程产品完整度和缺失章节。
- 真实运行证据与静态/模型证据区分。

## 2. 当前规模

```text
编号章节：23
Markdown 文件：243
Java 源文件：101
k6 JavaScript：3
YAML/Compose 配置：4
PowerShell：9
```

课程产品文件：

- `product-spec.md`
- `dependency-map.md`
- `student-workbook.md`
- `job-readiness-pack.md`
- `instructor-editorial-guide.md`
- `glossary.md`
- `learning-tracks.md`
- `evidence-matrix.md`
- `release-checklist.md`
- `notifyflow-mvp-spec.md`
- `release-evidence/alpha-2026-07-15/`

岗位研究目录：`06-research/job-jd-agent-java-2026/`。

## 3. 章节文件完整性

当前 23 个编号章节全部具备：

```text
README.md
lesson.md
project-application.md
exercises.md
answers.md
interview.md
teach-back.md
sources.md
lab/README.md
```

文件完整性：23/23 通过。

## 4. 章节状态

| 章节 | 内容状态 | 运行证据 | 发布判断 |
|---|---|---|---|
| 01 线程池 | 完整初稿 | JDK 21 实验通过 | Teach Pending |
| 02 HashMap | 完整初稿 | JDK 21 实验通过 | Teach Pending |
| 03 JMM | 完整初稿 | JDK 21 实验通过 | Teach Pending |
| 04 MySQL | 完整初稿 | MySQL 8.0.40 实验通过 | Teach Pending |
| 05 Spring 事务 | 完整初稿 | Maven 依赖/权限阻塞 | Draft |
| 06 Redis | 完整初稿 | Docker 运行 Pending | Draft |
| 07 Kafka/Outbox | 完整初稿 | 静态检查通过，Kafka runtime Pending | Draft |
| 08 恢复控制面 | 完整初稿 | 2/8 Java 实验通过 | Partial Lab |
| 09 可观测性 | 完整初稿 | 基础实验和 JFR Phase A 通过 | Partial Lab |
| 10 JVM | 八件套初稿 | GC/OOM/NMT 等 Pending | Draft |
| 11 网络/连接池/超时 | 八件套初稿 | 网络、池、UNKNOWN、SSE 实验 Pending | Draft |
| 12 Docker/K8s | 八件套初稿 | Docker/集群 Pending | Draft |
| 13 系统设计/答辩 | 八件套初稿 | 端到端演练、陌生评审和录像 Pending | Draft |
| 14 LLM/RAG | 八件套初稿 | 模型/向量库/评测 Pending | Draft |
| 15 文档 ingestion | 八件套初稿 | 解析、OCR、版本、ACL、幂等实验 Pending | Draft |
| 16 检索/向量数据库 | 八件套初稿 | pgvector/Milvus、召回、rerank、ACL 实验 Pending | Draft |
| 17 RAG 评测/安全 | 八件套初稿 | 评测集、引用、拒答、红队、回归门禁 Pending | Draft |
| 18 Agent Runtime | 八件套初稿 | Java Runtime/攻击评测 Pending | Draft |
| 19 多机分布式 | 八件套初稿 | 多进程/多节点 Pending | Draft |
| 20 简历事实/证据 | 八件套初稿 | 逐句事实审计和三类简历验证 Pending | Draft |
| 21 技术面试 | 八件套初稿 | 闭卷、录音和三轮模拟 Pending | Draft |
| 22 算法/Teach-back | 八件套初稿 | 96 题路线、成绩和陌生读者验证 Pending | Draft |
| 23 岗位/作品/发布 | 八件套初稿 | 实时 JD、投递、复现、版权和 RC 审计 Pending | Draft |

没有任何章节达到 Released。

## 5. 已核验证据

- 第 01-03 章 Java 21 实验。
- 第 04 章 MySQL 8.0.40 SQL、锁与事务实验。
- 第 07 章 Kafka Compose/PowerShell 静态检查；运行态未通过。
- 第 08 章重试放大与 Full Jitter。
- 第 09 章长尾、开放/封闭负载模型、tag 基数、线程池容量模型。
- 第 09 章真实 ThreadPoolExecutor 拒绝路径和自定义 JFR 事件。
- 第 09 章 k6 脚本 Node.js 语法和语义静态审阅；没有 k6 runtime。

## 6. 主要阻塞

| 阻塞 | 影响 | 当前事实 |
|---|---|---|
| Maven 精确依赖下载审批 403 | 第 05/09 章 Spring/Micrometer | 工程已准备，未编译运行 |
| Docker Engine 未运行 | Redis、Kafka、Compose | 静态设计存在，无容器证据 |
| k6 未安装 | 开放负载、threshold、容量报告 | 脚本静态检查通过 |
| Kubernetes 集群未建立 | 第 12/19 章 | YAML/实验设计，runtime Pending |
| 模型/向量库/评测集未固定 | 第 14-18 章 | 理论和实验矩阵，runtime Pending |
| 学习者未完成作业/试讲 | 所有章节发布 | 不能 Released |
| Playwright 公开岗位访问审批 403 | 实时 JD 研究 | 研究框架完成，实时样本 Pending |

## 7. 缺失章节

课程规划中的第 01-23 章已全部形成完整八件套。

当前缺口已从“章节缺失”转为“实验、真实学习、面试、岗位样本、陌生读者和发布验证不足”。

## 8. 自动检查结果

```text
常见占位标记：0
未完成注释标记：0
八件套缺失：0
lab/README.md 缺失：0
trailing whitespace：0
git diff --check：通过，仅有 Windows LF/CRLF warning
本地 Markdown 断链：0
课程审计脚本：`tools/audit-course.ps1` 退出码 0，输出 `COURSE_AUDIT_PASSED`
学生/教师分包：`tools/build-packages.ps1` 输出 `COURSE_PACKAGES_BUILT`；学生答案 0，教师答案 23，包内断链 0
```

本次审计已覆盖 23 章，并新增术语表、核心/进阶学时合同、证据矩阵、发布检查表和可重复运行的课程审计脚本。

## 9. 产品成熟度

当前可以称为：

```text
内部 Alpha 学习系统
完整课程骨架 + 部分真实实验 + 多章教学初稿
```

当前不能称为：

- 完整 V1.0 售卖课程。
- 全部 Lab Verified。
- 可以保证 offer/薪资/面试命中。
- 已完成生产级 NotifyFlow/RAG/Agent/Kubernetes 集群。

当前最主要的产品阻断已从章节缺失转为：NotifyFlow 尚不能一键启动、真实学习者验证不足、版权/许可证与最终 ZIP 发布验收未完成。学生/教师答案隔离已通过本机 smoke test，但尚未在干净环境复现。

NotifyFlow MVP 已进入 Phase 0-3 实现初稿：五模块、75 个 Java 文件、Flyway V1、REST API、JDBC Store、Provider Stub、Outbox/Delivery/Reconciliation 用例和课程内存 bridge 已落盘；Java 核心合同与 H2 schema 已真实验证，Maven/JUnit/Spring/MySQL/Kafka runtime 因审批服务 403 和环境未启动仍 Pending。

## 10. 发布优先级

### P0：V0.1 可靠后端证据闭环

1. 解锁 Maven，完成第 05 章事务与第 09 章 Micrometer。
2. 启动 Docker，完成 Redis/Kafka/Outbox 故障实验。
3. 安装/运行 k6，获得开放负载 threshold 和数据正确性报告。
4. 完成第 08-09 章剩余恢复/故障实验。
5. 让学习者完成第 01-09 章作业和三次 Teach-back。

### P1：V0.2-V0.4 技术章节

1. 补强第 11/13/15-17 章可执行实验、评测集与评审证据。
2. 运行 JVM、Docker/K8s、RAG/Agent、多机实验。
3. 建立一键启动的 NotifyFlow 主项目和版本策略。

### P2：V1.0 求职发布

1. 执行第 20-23 章事实审计、算法路线、三轮模拟面试和陌生读者测试。
2. 完成实时 JD 采样、事实台账、三类简历和投递反馈闭环。
3. 完成陌生读者测试、版权复核、录制和版本发布。

## 11. 下一次审计门槛

以下任一事件发生后重新审计：

- 新增或完成一个编号章节。
- 任一 Pending 实验获得真实运行结果。
- NotifyFlow 可以在干净环境启动。
- 学习者完成一次正式 Teach-back。
- 准备 Release Candidate 或对外销售页面。
