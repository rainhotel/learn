# 第 1 章：线程池与异步通知任务

## 章节定位

- 类型：Concept + Lab + Project + Incident + Interview + Teach-back
- 难度：进阶
- 建议学习时间：12-16 小时
- Java 版本：JDK 21
- 对应项目：NotifyFlow 异步通知执行器

## 学习顺序

1. `lesson.md`：理解问题、机制、设计与边界。
2. `lab/README.md`：运行三个验证实验。
3. `project-application.md`：设计 NotifyFlow 线程池边界。
4. `exercises.md`：闭卷完成分层练习。
5. `answers.md`：对照答案与评分标准。
6. `interview.md`：完成三层面试追问。
7. `teach-back.md`：进行 5 分钟与 15 分钟试讲。
8. `sources.md`：核对官方依据与版本。

## 完成标准

- 能画出 `ThreadPoolExecutor.execute` 的任务接收决策图。
- 能解释核心线程、最大线程、队列和拒绝策略如何共同形成容量边界。
- 能为通知发送业务给出参数推导、指标和过载策略。
- 能正确处理中断和两阶段关闭。
- 实验输出包含 `ALL_EXPERIMENTS_PASSED`。
- 分层练习达到 80 分以上。
- 能脱稿完成 15 分钟试讲并回答至少三层追问。

