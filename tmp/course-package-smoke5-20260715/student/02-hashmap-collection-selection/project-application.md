# NotifyFlow 项目应用：集合与键设计

## 1. 渠道注册表

```java
enum Channel {
    SMS, EMAIL, IN_APP
}

final class SenderRegistry {
    private final Map<Channel, NotificationSender> senders;

    SenderRegistry(List<NotificationSender> senderList) {
        EnumMap<Channel, NotificationSender> mutable = new EnumMap<>(Channel.class);
        for (NotificationSender sender : senderList) {
            NotificationSender previous = mutable.put(sender.channel(), sender);
            if (previous != null) {
                throw new IllegalStateException("duplicate sender: " + sender.channel());
            }
        }
        this.senders = Map.copyOf(mutable);
    }
}
```

选择理由：渠道集合固定、类型安全、构造后不修改。

## 2. 模板变量快照

API 收到的 Map 可能由调用方继续修改。进入异步任务前创建快照：

```java
Map<String, String> variables = Map.copyOf(command.variables());
```

注意 `Map.copyOf` 不允许 null key/value。如果业务允许 null，应先定义明确语义，而不是让渲染阶段猜测。

## 3. 幂等 key

不要直接用包含可变状态的实体作为 key。推荐不可变值对象：

```java
record IdempotencyKey(String tenantId, String requestId) {
    IdempotencyKey {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(requestId);
    }
}
```

record 会基于组件生成 equals/hashCode，前提是组件本身具有正确且稳定的相等语义。

## 4. 并发聚合

如果多个执行线程统计错误类型：

```java
ConcurrentHashMap<ErrorType, LongAdder> counts = new ConcurrentHashMap<>();
counts.computeIfAbsent(type, ignored -> new LongAdder()).increment();
```

如果统计必须持久化或用于任务完成判定，不能只依赖进程内 Map。

## 5. 顺序需求

模板变量替换通常不应依赖 Map 顺序。如果业务明确要求按定义顺序展示字段：

- 在数据库中保存 position。
- 查询后显式排序。
- 使用有顺序保证的集合承载结果。

不要通过“当前 HashMap 输出看起来稳定”来满足需求。

## 6. ADR 作业

为渠道注册表编写选型 ADR，比较：

- `HashMap<String, NotificationSender>`
- `EnumMap<Channel, NotificationSender>`
- `ConcurrentHashMap<Channel, NotificationSender>`
- Spring Bean 注入后的不可变 Map

说明运行时是否允许动态注册，以及动态注册带来的并发与配置一致性问题。

