# NotifyFlow 项目应用：安全发布与并发状态

## 1. ProviderConfig 不可变快照

```java
record ProviderConfig(
        URI endpoint,
        Duration connectTimeout,
        Duration requestTimeout,
        int maxAttempts) {

    ProviderConfig {
        Objects.requireNonNull(endpoint);
        Objects.requireNonNull(connectTimeout);
        Objects.requireNonNull(requestTimeout);
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts");
    }
}
```

配置持有器：

```java
final class ProviderConfigHolder {
    private volatile ProviderConfig current;

    ProviderConfigHolder(ProviderConfig initial) {
        this.current = Objects.requireNonNull(initial);
    }

    ProviderConfig snapshot() {
        return current;
    }

    void replace(ProviderConfig next) {
        validate(next);
        current = next;
    }
}
```

一次发送只调用一次 `snapshot()`，避免过程中版本切换。

## 2. 指标与业务状态分离

```java
LongAdder successMetric = new LongAdder();
```

适合指标，不适合判断“批次是否全部完成”。完成状态应来自持久化明细和状态机。

## 3. 单实例配额

如果需要在单实例内保护一组字段：

```java
final class LocalQuota {
    private final Object lock = new Object();
    private int remaining;
    private long windowStart;

    boolean tryAcquire(long now) {
        synchronized (lock) {
            refreshWindowIfNeeded(now);
            if (remaining == 0) return false;
            remaining--;
            return true;
        }
    }
}
```

多实例总配额不能由此保证，需要集中协调或合理分片。

## 4. 错误的停止标记

```java
boolean stopping;
```

若一个线程写、其他线程轮询，存在 data race。可以使用 volatile，但更优先使用 ExecutorService、Future 取消和中断等生命周期 API。

## 5. 锁边界

禁止在本地配额锁中调用供应商：

```java
synchronized (lock) {
    provider.send(command);
}
```

锁只负责本地状态转换，远程调用在锁外执行。业务一致性由任务状态机和幂等保证。

## 6. 验收实验

- 并发读取配置时循环替换不可变快照，确认单次任务只使用一个版本。
- 将 volatile 计数替换为实验 2 的模式，复现丢失更新。
- 对 LocalQuota 进行 20 线程并发测试，确认发放数不超过初始配额。
- 故意把锁对象改为每次新建，证明互斥失效。

## 7. ADR

题目：NotifyFlow 配置热更新为什么采用“volatile 不可变快照”，而不是锁住所有读取或在可变对象上逐字段更新。

