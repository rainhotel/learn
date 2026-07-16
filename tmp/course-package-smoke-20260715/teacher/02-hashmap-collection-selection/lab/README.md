# HashMap 实验运行说明

## 环境

- JDK 21
- 不需要第三方依赖

## 编译

在仓库根目录执行：

```powershell
New-Item -ItemType Directory -Force '.\tmp\course-classes' | Out-Null
javac -d '.\tmp\course-classes' '.\01-topics\java-backend-big-tech-preparation\course\02-hashmap-collection-selection\lab\HashMapLab.java'
```

## 运行

树化与容量实验需要读取 JDK 21 当前实现的私有 `table` 字段，因此显式打开模块：

```powershell
java --add-opens java.base/java.util=ALL-UNNAMED -cp '.\tmp\course-classes' HashMapLab
```

成功标志：

```text
ALL_EXPERIMENTS_PASSED
```

## 实验内容

1. 破坏 `equals/hashCode` 契约后的查询错误。
2. 修改作为 key 的可变字段后，条目“存在但无法按 key 找回”。
3. JDK 21 中碰撞桶从链表转为树节点的实现行为。
4. `new HashMap<>(100)` 与 `HashMap.newHashMap(100)` 的容量语义差异。
5. 单线程结构修改触发 fail-fast 迭代器。

## 边界

- 反射读取 `table` 仅用于教学观察，不应进入业务代码。
- 树化阈值、桶结构和扩容实现属于 JDK 21 实现细节，不是 `Map` 接口保证。
- fail-fast 是尽力检测，不能作为并发正确性机制。

