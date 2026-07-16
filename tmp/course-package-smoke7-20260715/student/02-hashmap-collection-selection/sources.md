# 来源、版本与验证记录

## A 级：官方 API

- [Map, Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html)
  - Map 契约、顺序、可变 key 风险、不可修改 Map。
- [HashMap, Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html)
  - 公开性能语义、容量/负载因子、并发和 fail-fast。
- [Object.equals/hashCode, Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html)
  - 相等关系与 hashCode 契约。
- [LinkedHashMap, Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedHashMap.html)
- [TreeMap, Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html)
- [ConcurrentHashMap, Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html)

访问日期：2026-07-13。

## A 级：JDK 21 源码观察

- 本机源码：`${JAVA_HOME}/lib/src.zip`，具体安装目录由学习者环境决定。
- 文件：`java.base/java/util/HashMap.java`
- JDK：Oracle JDK 21.0.6。

核对内容：

- 默认容量 16、默认负载因子 0.75。
- `hash` 的高位扰动。
- `putVal`、`resize`、`treeifyBin` 主路径。
- `TREEIFY_THRESHOLD=8`、`MIN_TREEIFY_CAPACITY=64`。
- JDK 21 的 `HashMap.newHashMap(int)`。

这些属于当前实现观察，课程正文已与接口契约区分。

## B 级

- Joshua Bloch，《Effective Java》：equals/hashCode、可变性、API 设计。
- Robert Sedgewick，《算法》：哈希表、树与复杂度模型。

## C 级

- 牛客公开 Java 后端面经中 HashMap put、扩容、树化、ConcurrentHashMap 和集合选型为高频问题。
- 面经用于确定教学重点，不用于定义 API 行为。

## 实验

- 文件：`lab/HashMapLab.java`。
- 环境：Oracle JDK 21.0.6，Windows。
- 运行需要：`--add-opens java.base/java.util=ALL-UNNAMED`，仅用于教学反射观察。

已验证：

1. 破坏 equals/hashCode 契约导致逻辑相等 key 查询失败并形成两个条目。
2. 修改 hash 相关字段导致同一 key 查询失败，但迭代仍能看见条目。
3. 预留足够 table 容量时，8 个同 hash key 使用 Node 桶，第 9 个后转为 TreeNode。
4. `new HashMap<>(100)` 在第 97 条时从容量 128 扩至 256；`HashMap.newHashMap(100)` 首次分配容量 256。
5. 单线程结构修改触发 CME。

### 边界

- 反射结构和阈值不是公开兼容性承诺。
- CME 是 best-effort。
- 单次行为实验不能证明并发正确性或性能上界。
