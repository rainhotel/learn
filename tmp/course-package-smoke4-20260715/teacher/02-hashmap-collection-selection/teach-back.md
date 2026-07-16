# Teach-back 试讲

## 5 分钟版本

1. Map 是 key-value 契约，HashMap 是无序、非线程安全实现。
2. put：hash、桶、碰撞、更新、扩容。
3. equals/hashCode 契约决定查询正确性。
4. 树化和容量阈值是 JDK 21 实现细节。
5. 选 Map 要看顺序、并发和 key 类型。

## 15 分钟版本

### 0-3 分钟：业务故障

讲可变幂等 key 导致任务查不到。

### 3-8 分钟：机制

画 table、链表/树桶、put 和 resize。

### 8-11 分钟：契约

讲 equals/hashCode、可变 key 和 fail-fast。

### 11-14 分钟：选型

比较 HashMap、EnumMap、LinkedHashMap、ConcurrentHashMap。

### 14-15 分钟：实验与总结

展示五个实验及规范/实现边界。

## 必画图

```text
table[index] -> Node -> Node
             -> TreeNode
```

## 自检

- [ ] 没有把 HashMap 顺序说成稳定。
- [ ] 没有把树化阈值说成 Map 规范。
- [ ] 能解释第 9 个碰撞 key 的实验条件。
- [ ] 能解释 `newHashMap` 与构造参数语义。
- [ ] 能说明 CME 不是并发安全机制。
- [ ] 能结合 NotifyFlow 说明三种 Map 选型。

## 复盘记录

- 日期：
- 听众：
- 卡顿位置：
- 回答失败问题：
- 需要补充的实验：
- 文档修改：

