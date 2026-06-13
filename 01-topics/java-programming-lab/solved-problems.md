# Java Programming Lab Solved Problems

## 题目 1：6-1 HashSet 应用举例

- Date: 2026-06-11
- Source: 《Java 语言程序设计（第 3 版）》例 6-1
- Topic: 集合框架
- Difficulty: Easy

### Problem

- 使用 `HashSet` 演示集合元素的添加、删除、重复添加、保留交集、删除交集、清空以及 `null` 元素处理。

### Final Answer

- 已完成 `HashSetTester.java`，能够正确展示 `HashSet` 的核心操作现象。

### Solution

1. 创建 `HashSet<String>` 集合并加入三个字符串。
2. 调用 `remove()` 和 `add()` 观察布尔返回值。
3. 创建辅助集合，分别使用 `retainAll()` 与 `removeAll()`。
4. 清空集合并测试 `null` 元素。

### Formula Or Method Used

- 公式/定理/方法：`HashSet` 常用方法演示
- 含义：说明无重复、无序、可做交集和差集相关操作
- 适用条件：需要保存不重复元素时

### Sources

- 教材第 6 章例 6-1

### Knowledge Points

- `HashSet` 不保证顺序
- 重复插入元素会失败
- `HashSet` 允许保存 `null`

### Reflection

- 这道题的重点不是输出顺序，而是理解每个集合操作的语义。

## 题目 2：6-4 Iterator 类使用举例

- Date: 2026-06-11
- Source: 《Java 语言程序设计（第 3 版）》例 6-4
- Topic: 集合遍历
- Difficulty: Easy

### Problem

- 使用 `Iterator` 遍历 `Vector` 中的字符串，并删除长度大于 4 的元素。

### Final Answer

- 已完成 `IteratorTester.java`，遍历后得到结果 `[one, two, four, five, six, nine, ten]`。

### Solution

1. 创建字符串数组并转换为 `Vector<String>`。
2. 获取迭代器。
3. 遍历每个元素并输出。
4. 对长度大于 4 的元素调用 `Iterator.remove()`。

### Formula Or Method Used

- 公式/定理/方法：`Iterator` 遍历 + `remove()`
- 含义：安全删除当前遍历元素
- 适用条件：遍历时需要同步删除元素

### Sources

- 教材第 6 章例 6-4

### Knowledge Points

- `Iterator` 是集合统一遍历接口
- 遍历时删除应使用 `Iterator.remove()`

### Reflection

- 这道题的核心是掌握“遍历时如何删”而不是记住 `Vector` 本身。

## 题目 3：11-1 JSP 计算连续和

- Date: 2026-06-11
- Source: 《Java 语言程序设计（第 3 版）》例 11-1
- Topic: JSP 基础
- Difficulty: Medium

### Problem

- 编写一个 JSP 页面，在表单中输入自然数，计算从 `1` 到该数的累加和。

### Final Answer

- 已完成 `Ex11_1.jsp`，包含方法声明、参数读取、结果回显和简单输入校验。

### Solution

1. 用 JSP 声明定义求和方法。
2. 用脚本片段读取 `request` 参数。
3. 将输入值转换为整数。
4. 用表达式输出计算结果。

### Formula Or Method Used

- 公式/定理/方法：连续和循环累加
- 含义：`sum = 1 + 2 + ... + n`
- 适用条件：`n` 为非负整数

### Sources

- 教材第 11 章例 11-1

### Knowledge Points

- JSP 的三种脚本元素
- 表单参数读取
- 页面动态回显

### Reflection

- 这道题帮助把 JSP 页面和 Java 代码执行过程联系起来理解。
