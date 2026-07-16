# 第 19 章 Teach-back

## 5 分钟：为什么锁不够

用 A pause、B 接管、A 恢复的时间线解释 lease 和 fencing。

## 15 分钟：多实例任务领取

比较 DB SKIP LOCKED、Kafka 分区和 shard lease，说明吞吐、顺序、故障和幂等。

## 45 分钟：一次集群恢复事故

Provider 慢 -> lag/backlog -> retry 放大 -> 限流/暂停 -> lease/fencing -> 分阶段恢复 -> 数据正确性 -> Agent 只读摘要。

## 验收

- 能说出旧 owner 陈旧写的阻断点。
- 能解释 offset 与业务幂等的区别。
- 能设计扩缩容和下线时间线。
- 不把 Agent 当作无审批的集群控制器。
