# 第 4 章：MySQL 索引、事务与任务表设计

## 章节定位

- 类型：Concept + SQL Lab + Project + Incident + Interview + Teach-back
- 难度：进阶
- 建议学习时间：18-24 小时
- 实验版本：MySQL Community Server 8.0.40
- 资料版本：MySQL 8.4 Reference Manual
- 对应项目：NotifyFlow 任务状态、幂等、领取与恢复

## 学习顺序

1. `lesson.md`
2. `lab/README.md` 与 SQL 文件
3. `project-application.md`
4. `exercises.md`
5. `answers.md`
6. `interview.md`
7. `teach-back.md`
8. `sources.md`

## 完成标准

- 能从查询和写入模式推导索引，而不是给每个字段都建索引。
- 能读懂 `EXPLAIN ANALYZE` 的访问路径、过滤、排序和实际耗时。
- 能解释 InnoDB 聚簇索引、二级索引、回表、MVCC、记录锁、间隙锁和 next-key lock。
- 能设计任务领取、租约、幂等和重试状态。
- 能复现 `SKIP LOCKED`、隔离级别快照差异和死锁，并说明应用如何处理。
- SQL 实验通过，练习达到 80 分，完成 15 分钟试讲。

