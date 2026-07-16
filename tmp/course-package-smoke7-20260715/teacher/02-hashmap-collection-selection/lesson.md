# HashMap 与集合选型

## 1. 学习目标

完成本章后，你能够：

1. 从 `Map` 契约出发解释键值映射，而不是从背诵源码常量开始。
2. 讲清 JDK 21 HashMap 的哈希扰动、索引、碰撞、扩容和树化路径。
3. 解释 `equals/hashCode` 契约为什么直接决定查询正确性。
4. 识别可变 key、无序依赖、并发写入和过度预分配等工程风险。
5. 根据顺序、排序、并发、键类型和内存需求选择 Map 实现。
6. 将集合选择应用到 NotifyFlow，而不是默认所有场景都使用 HashMap。

## 2. 为什么要学

消息通知系统中随处可见映射：

- 模板变量名 -> 变量值。
- 渠道类型 -> 渠道处理器。
- 接收方 ID -> 发送结果。
- 错误码 -> 错误分类。
- 租户 ID -> 限流器或配置。

如果 key 设计错误，可能出现已经写入的数据查询不到；如果错误依赖 HashMap 遍历顺序，输出在版本或扩容后变化；如果多个线程并发修改普通 HashMap，程序缺少正确性保证；如果初始容量设置过大，迭代成本和内存都会增加。

## 3. 先从 Map 契约开始

`Map<K,V>` 表示 key 到 value 的映射：不能包含重复 key，每个 key 最多映射一个 value。

重要契约：

- 是否允许 null，由具体实现决定。
- 是否有稳定遍历顺序，由具体实现决定。
- 可修改性由具体实现决定。
- 使用可变对象作为 key 时，如果修改影响 `equals`，行为没有被规范定义。
- `Map.of` 等不可修改 Map 不允许 null，且遍历顺序未指定。

HashMap 只是 Map 的一种实现，不应把 HashMap 的树化阈值等细节说成 Map 的普遍性质。

## 4. HashMap 的公开保证

JDK 21 API 说明：

- 支持可选 Map 操作。
- 允许一个 null key 和多个 null value。
- 不保证顺序，也不保证顺序保持不变。
- 在哈希分布合理时，基本 `get/put` 具有期望常数时间性能。
- 遍历成本与容量加 size 相关。
- 非线程安全。
- fail-fast 迭代器只用于尽力发现 bug，不能依赖它保证正确性。

“期望 O(1)”包含前提：哈希函数应把 key 合理分散到桶中。大量相同 hashCode 会显著降低性能。

## 5. JDK 21 当前数据结构

```text
HashMap
  |
  `-- Node<K,V>[] table
        |-- null
        |-- Node -> Node -> Node
        `-- TreeNode（红黑树桶）
```

单个普通节点保存：

- hash
- key
- value
- next

JDK 21 当前实现常量：

- 默认初始容量：16。
- 默认负载因子：0.75。
- 树化阈值：8。
- 反树化相关阈值：6。
- 最小树化 table 容量：64。

这些数值是当前实现细节。业务代码不应通过反射依赖它们。

## 6. hash 扰动与桶索引

JDK 21 当前实现先处理 key 的 hashCode：

```java
int h;
return key == null ? 0 : (h = key.hashCode()) ^ (h >>> 16);
```

索引计算：

```java
index = (tableLength - 1) & hash;
```

table 长度使用 2 的幂，使按位与可以代替取模，并便于扩容时拆分桶。高 16 位参与扰动，是为了让高位信息影响低位索引。

不要把该实现误讲成“HashMap 调用 hashCode 后一定经过某个永远不变的公式”。这是 JDK 21 当前实现。

## 7. put 主流程

```text
put(key, value)
  |
  v
计算扰动 hash
  |
table 未初始化？-- yes --> resize 分配
  |
计算桶索引
  |
桶为空？-------- yes --> 创建 Node
  |
首节点 key 相等？-- yes --> 更新 value
  |
树桶？---------- yes --> 树中插入/更新
  |
遍历链表
  |-- 找到相等 key：更新
  `-- 到末尾：追加；达到条件时尝试树化
  |
size 超过 threshold？-- yes --> resize
```

判断 key 相等时会综合 hash、引用相等和 `equals`。hash 不同的对象即使 `equals` 错误地返回 true，也可能被放入不同位置，因此必须遵守契约。

## 8. equals 与 hashCode 契约

Object API 的关键规则：

- 相等对象必须产生相同 hashCode。
- 不相等对象不要求 hashCode 不同，但更好的分布有利于性能。
- 在参与 equals 的信息未修改时，同一对象在一次运行期间应保持 hashCode 一致。

如果覆盖 `equals` 却不覆盖 `hashCode`，两个逻辑相等的 key 可能产生不同 hash，导致：

- `get` 找不到已存在映射。
- Map 中出现两个逻辑相等的条目。
- Set 去重失败。

## 9. 为什么 key 应当不可变

HashMap 插入时依据当时的 hash 将 key 放入某个桶。如果之后修改影响 `equals/hashCode` 的字段，查询会使用新 hash 寻找另一个桶。

条目仍在旧桶中，却无法通过当前 key 正常找到，形成“物理存在、逻辑失联”。

推荐 key：

- String、UUID、Integer 等不可变类型。
- 正确实现契约的 record。
- 只由不可变标识字段参与 equals/hashCode 的领域值对象。

## 10. 碰撞、链表与树化

两个不同 key 可能落入同一桶。普通碰撞通过链表保存。

JDK 21 当前实现中，新增节点使桶达到树化条件时调用 `treeifyBin`：

- table 容量小于 64：优先扩容。
- 容量足够：桶转换为 TreeNode 结构。

因此“链表长度达到 8 就一定立即变红黑树”不够准确。树化还依赖 table 容量，且具体触发点应结合插入过程表述。

树结构主要缓解恶劣碰撞下的查找退化，但 TreeNode 占用更多空间。正常 hash 分布下很少需要树桶。

## 11. 扩容

当 size 超过 threshold 时扩容。默认情况下：

```text
threshold ≈ capacity × loadFactor
```

JDK 21 常见扩容是容量翻倍。旧桶中的节点根据旧容量对应的 hash 位拆分到：

- 原索引。
- 原索引 + oldCapacity。

不需要对每个节点重新执行普通取模。

扩容仍然需要分配新数组并迁移/拆分节点，可能带来延迟和内存峰值。因此已知数据量时应合理预估容量。

## 12. 初始容量不是期望映射数量

```java
new HashMap<>(100)
```

100 表达 initial capacity 参数，内部会调整到 2 的幂。默认负载因子下，容量 128 的阈值为 96，因此插入第 97 个映射时会扩容。

JDK 19 起提供：

```java
HashMap.newHashMap(100)
```

它直接表达“期望存储 100 个 mappings”，为避免预期数量内扩容计算适当容量，语义更清晰。

不要盲目把容量设得极大。官方文档指出，HashMap 遍历成本与容量 + size 成正比，过度预分配会增加空间和遍历成本。

## 13. fail-fast 的正确理解

创建迭代器后，如果 Map 发生结构修改，迭代器通常抛出 `ConcurrentModificationException`。

但官方文档明确说明：它是 best-effort，不能保证一定检测，也不能用异常作为并发控制逻辑。

正确用途：帮助尽快暴露程序 bug。

错误用途：认为“没有抛异常就说明并发访问安全”。

## 14. HashMap 为什么不是线程安全的

多个线程并发访问且至少一个线程结构性修改时，需要外部同步或选择并发实现。

问题不只包括旧版本常被背诵的“扩容环”。在当前 Java 中仍然存在：

- 数据竞争和可见性问题。
- 复合操作非原子。
- 更新覆盖或状态不一致。
- 迭代期间结构变化没有正确性保证。

不要用 `if (!map.containsKey(k)) map.put(k,v)` 作为并发原子操作。

## 15. ConcurrentHashMap

JDK 21 API 的重要语义：

- 读取通常不阻塞，可以与更新并发。
- 对给定 key，已完成更新与观察到该值的非 null 读取之间存在 happens-before 关系。
- 迭代器弱一致，不抛 `ConcurrentModificationException`。
- `size` 等聚合结果在并发更新时通常只适合监控或估算，不适合程序控制。
- 不允许 null key 或 null value。

示例：并发频次统计可使用 `ConcurrentHashMap<K, LongAdder>` 与 `computeIfAbsent`。

选择 ConcurrentHashMap 并不意味着任意跨 key、跨步骤业务操作自动原子。复杂一致性仍需额外设计。

## 16. 常见 Map 选型

| 实现 | 顺序 | 并发 | null | 典型场景 |
|---|---|---|---|---|
| HashMap | 不保证 | 否 | 允许 | 单线程/受保护的通用映射 |
| LinkedHashMap | 插入或访问顺序 | 否 | 允许 | 稳定遍历、简单 LRU |
| TreeMap | key 排序 | 否 | Comparator 约束 | 范围查询、有序键 |
| EnumMap | enum 自然顺序 | 否 | key 不允许 null | 枚举键的高效映射 |
| ConcurrentHashMap | 不保证 | 是 | 不允许 | 高并发共享注册表/统计 |
| Map.of/copyOf | 未指定 | 不可修改 | 不允许 | 小型固定配置 |

### LinkedHashMap

维护双向链表，提供明确 encounter order。访问顺序模式结合 `removeEldestEntry` 可实现简单 LRU，但仍非线程安全，也不等于完整生产缓存。

### TreeMap

根据 key 自然顺序或 Comparator 排序，适合范围操作。Comparator 必须与 equals 的业务语义保持合理一致，否则 Map 可能把比较结果为 0 的不同对象视作同一排序键。

### EnumMap

当 key 是固定枚举时通常比 HashMap 更直接，能表达完整渠道集合并避免字符串拼写错误。

## 17. NotifyFlow 中的选择

### 模板变量

`Map<String,String>` 通常可以使用 HashMap，但渲染输入应复制为不可变快照，避免渲染过程中被修改。

### 渠道处理器注册表

如果渠道是 enum：

```java
EnumMap<Channel, NotificationSender>
```

比 `Map<String,Object>` 更能表达类型约束。

如果注册表运行期间动态并发更新，可考虑 ConcurrentHashMap，并明确发布/替换语义。

### 错误码展示顺序

需要稳定顺序时使用 LinkedHashMap 或显式排序，不能依赖 HashMap 当前恰好呈现的顺序。

### 并发批次聚合

多个线程更新接收方结果时，使用 ConcurrentHashMap 仍要考虑：

- value 是否可变。
- 更新是否需要原子 compute。
- 最终完成判断能否依赖 size。
- 数据是否应该直接写数据库而非长期停留在内存。

## 18. 常见错误

### 错误一：只覆盖 equals

破坏相等对象必须具有相同 hashCode 的契约。

### 错误二：使用可变领域对象作为 key

修改后查询失败。

### 错误三：依赖 HashMap 遍历顺序

顺序没有保证，版本、容量和数据变化都可能改变结果。

### 错误四：把 initialCapacity 当作元素数量

负载因子导致阈值小于容量。

### 错误五：为避免扩容设置巨大容量

浪费内存并增加遍历成本。

### 错误六：认为 ConcurrentHashMap 让所有逻辑原子

跨多次调用、多个 key 或外部状态的操作仍可能竞争。

### 错误七：依赖 CME 保证并发安全

fail-fast 只是 best-effort bug detector。

## 19. 本章小结

- 先理解 Map 契约，再学习 HashMap 实现。
- HashMap 正确性依赖 key 的 equals/hashCode 契约与稳定性。
- JDK 21 使用数组、链表和树桶处理映射与碰撞。
- 树化阈值等是实现细节，并非接口保证。
- 初始容量要结合期望映射数和负载因子；JDK 21 可使用 `newHashMap` 表达期望 mappings。
- 集合选择应由顺序、排序、并发、键类型和修改方式决定。

