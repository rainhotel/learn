# NotifyFlow JVM 内存与生产排障应用

## 1. 目标

把 NotifyFlow 的任务、Provider、重试和 Agent 上下文映射到 JVM 资源，形成可执行的内存预算和事故 Runbook。

## 2. 资源预算

| 资源 | 主要来源 | 观测 | 风险 |
|---|---|---|---|
| Java heap | DTO、消息 payload、缓存、队列 | heap used/max、GC、对象直方图 | OOM、长暂停 |
| Thread stack | 平台线程、调用栈、递归 | thread count、RSS、`-Xss` | native thread 失败 |
| Metaspace | 类元数据、动态代理、脚本类 | NMT、class count | classloader leak |
| Direct memory | Netty/NIO/压缩缓冲区 | buffer pool、NMT | Direct buffer OOM |
| Code cache | JIT 编译代码 | Code Cache JMX/JFR | 编译退化 |
| Agent context | prompt、工具结果、检索文档 | token、字节数、保留时间 | 上下文膨胀 |

预算必须写成公式和上限，而不是“给堆越大越好”：

```text
process_budget
  = heap_budget
  + thread_stack_budget
  + metaspace_budget
  + direct_memory_budget
  + code_cache_budget
  + native_overhead
```

## 3. NotifyFlow 高风险对象

- 大 payload 被任务、Outbox、日志和 Agent 摘要同时保留。
- 无界 `Map<taskId, Future>` 在 Provider 永久不回调时增长。
- ThreadLocal 保存请求上下文但在线程池复用后未清理。
- 重试和 DLT 同时保留原始 payload，形成乘法放大。
- RAG 文档、检索结果和工具输出未经预算直接拼进上下文。
- 动态脚本、类加载器或热部署导致 Metaspace 保留链。

## 4. 排障 Runbook

### 4.1 先止血

1. 记录版本、实例、JVM 参数、容器限制和事故时间窗口。
2. 限制入口速率，暂停高成本 replay，不直接重启所有实例。
3. 保留一台代表实例做低风险诊断，避免所有实例同时 dump。

### 4.2 按症状分流

| 症状 | 第一证据 | 第二证据 | 不要先做 |
|---|---|---|---|
| heap 持续上升 | GC log/heap used | class histogram/heap dump | 盲目加堆 |
| RSS 上升但 heap 平稳 | NMT/buffer pool | thread count/direct buffer | 只看 heap |
| CPU 高 | JFR CPU/线程 | allocation/锁 | 先开更多线程 |
| CPU 低但 P99 高 | Thread Dump/JFR wait | 队列/连接池/下游 | 归因 GC |
| Metaspace 上升 | class count/NMT | classloader 保留链 | 频繁重启掩盖 |

### 4.3 Agent 使用边界

Agent 可以：

- 聚合脱敏 GC log、JFR 事件、指标和 Runbook。
- 生成带时间窗口和证据引用的假设。
- 推荐下一条只读查询和低风险采样。

Agent 不可以直接：

- 生成并下载 heap dump 到公共位置。
- 修改 JVM 参数、重启实例、清空队列或执行 replay。
- 依据单一摘要认定内存泄漏或 GC 是根因。

## 5. 可转化简历的证据

只有真实运行后才可写：环境、JDK/JVM 参数、负载、故障注入、P99/GC/RSS 变化、修复和边界。模型推演只能写“设计了排障方案”或“构建了实验”，不得写成线上优化比例。
