# 第 3 章：JMM、volatile 与 synchronized

## 章节定位

- 类型：Specification + Concept + Lab + Project + Interview + Teach-back
- 难度：深入
- 建议学习时间：16-20 小时
- Java 版本：JDK 21
- 对应项目：NotifyFlow 配置发布、并发计数与任务状态保护

## 学习顺序

1. `lesson.md`
2. `lab/README.md`
3. `project-application.md`
4. `exercises.md`
5. `answers.md`
6. `interview.md`
7. `teach-back.md`
8. `sources.md`

## 完成标准

- 能用“允许出现哪些执行结果”解释 JMM，而不是画一套伪硬件模型。
- 能列出核心 happens-before 规则并用于代码推理。
- 能区分可见性、原子性、有序性与互斥。
- 能解释 volatile 的适用条件和 `volatile++` 的错误。
- 能解释 monitor、可重入、异常解锁、wait/notify 和锁对象选择。
- 能为 NotifyFlow 设计不可变配置安全发布和线程安全统计。
- 实验通过，练习达到 80 分，完成 15 分钟试讲。

