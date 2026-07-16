# Teach-back 试讲

## 5 分钟版本

1. JMM 定义允许的共享内存行为。
2. data race 是未被 happens-before 排序的跨线程冲突访问。
3. volatile 适合发布，不保护 count++。
4. synchronized 使用 monitor 提供互斥与可见性。
5. NotifyFlow 用不可变配置快照，而非共享可变配置。

## 15 分钟版本

### 0-3 分钟：问题

普通 stopping、volatile count++、热更新配置的三个错误。

### 3-7 分钟：JMM

解释 action、data race、synchronizes-with 和 happens-before。

### 7-10 分钟：volatile

画发布链，展示确定性丢失更新实验。

### 10-13 分钟：synchronized

讲 monitor、可重入、异常释放、锁对象和临界区。

### 13-15 分钟：项目

讲 ProviderConfig 快照、LongAdder 指标和跨实例边界。

## 必画图

```text
payload write
   -> volatile write
   -> volatile read
   -> payload read
```

## 自检

- [ ] 没有把 JMM 只讲成缓存刷新。
- [ ] 能区分 happens-before 与墙钟先后。
- [ ] 没有说 volatile++ 原子。
- [ ] 能解释 sleep 无同步语义。
- [ ] 能解释 static/instance synchronized 锁对象。
- [ ] 能说明 synchronized 不跨 JVM。
- [ ] 能说明实验 2 的 Barrier 作用和边界。

## 复盘

- 日期：
- 听众：
- 卡顿位置：
- 错误回答：
- 需补实验：
- 文档修改：

