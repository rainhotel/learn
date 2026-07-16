# 第 10 章参考答案与评分

## 1. 概念边界

JMM 规定并发读写语义；JVM 规范定义运行时数据区和执行语义；HotSpot 决定 G1 Region、压缩指针、线程栈实现和 native 分配。三者不能用一张“堆栈图”互相替代。

## 2. GC 与延迟

GC pause 只是 JVM 停顿时间，API P99 还受队列、连接池、网络、下游 Provider、线程调度和客户端到达模型影响。必须通过 Trace/指标/JFR 时间窗口关联，而不是看到同时上升就下结论。

## 3. RSS 排查

先对照 heap used/max 与容器 RSS，再看线程数/`-Xss`、Direct buffer、Metaspace、Code Cache 和 NMT。若只加 heap，可能让 RSS 更快触顶。

## 4. OOM 分类

异常类型只能作为入口：heap OOM 看保留链和分配；Metaspace 看 classloader/class count；Direct buffer 看 buffer pool 与上限；native thread 看线程数、栈大小和容器 PID 限制。最终结论必须有第二证据。

## 5. Agent 答案

Agent 输出应包含时间窗口、原始证据链接、反例、置信度和下一条只读查询。heap dump、重启、改参数和 replay 属高风险动作，必须人工审批和审计。

## 6. 评分锚点

| 题目 | 满分关键点 |
|---|---|
| 1-4 | 三层边界、资源类别和取舍准确 |
| 5-8 | 不单因果归因，能给出可执行证据链 |
| 9-11 | 有大小/时间/权限预算，字段低基数 |
| 12-14 | 先止血、后诊断，Agent 不越权 |
| 15-16 | 实验可复现，简历边界诚实 |

完整作业 100 分：概念 20、诊断 25、项目应用 25、证据与边界 20、表达 10。
