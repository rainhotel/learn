# ClaudeCode 源码学习 Formula Sheet

## Core Formulas Or Methods

### Item 1

- 名称：四主线阅读法
- 表达式/方法：输入主线 + 会话主线 + 流处理主线 + 收尾主线
- 含义：把超长 orchestrator 按职责切成 4 条线来读
- 什么时候用：读 agent 框架中的大调度函数时
- 不该在什么时候用：读局部算法函数时
- 常见误用：只按代码顺序读，不先分职责

### Item 2

- 名称：双消息数组识别法
- 表达式/方法：类字段持久状态 + 局部快照工作集
- 含义：先确认哪些变量跨 turn 存活，哪些只服务当前处理
- 什么时候用：看到 `mutableX`、`currentX`、`snapshotX` 这类并存变量时
- 不该在什么时候用：完全纯函数式的数据流里
- 常见误用：把它们误判成重复数据或坏味道

### Item 3

- 名称：注释即历史 bug 法
- 表达式/方法：优先阅读长注释里的“without this”“if killed before”“resume fails”等字样
- 含义：借注释反推系统最脆弱的边界条件
- 什么时候用：读 streaming、resume、transcript、compaction 相关代码时
- 不该在什么时候用：纯定义文件或简单类型声明里
- 常见误用：把工程注释当成啰嗦背景，直接跳过

## Derived From

- 来自哪几道题：本次“如何拆读 `submitMessage()`”源码阅读题
- 来自哪些资料：用户提供的 `QueryEngine` / `ask` 源码
