# Java 实验报告：6-1、6-4、11-1

## 一、基本信息

- 课程名称：Java 程序设计
- 实验主题：集合框架与 JSP 基础编程
- 实验题目：6-1、6-4、11-1
- 姓名：`[请替换]`
- 学号：`[请替换]`
- 班级：`[请替换]`
- 实验日期：2026-06-11

## 二、实验目的

1. 掌握 `HashSet` 的基本特性，理解集合去重、无序存储及常用操作方法。
2. 掌握 `Iterator` 的使用方式，理解遍历过程中安全删除元素的方法。
3. 理解 JSP 中声明、脚本片段和表达式的基本用法，能够编写简单的动态页面。
4. 通过三个实验巩固 Java 集合框架和 Web 基础编程的实际应用能力。

## 三、实验环境

- 操作系统：Windows
- JDK：21
- 开发方式：命令行编译运行 + 文本编辑
- JSP 运行环境：Tomcat（报告中给出部署方法，本机未实际检测到 Tomcat）

## 四、实验内容

### 1. 实验 6-1：HashSet 应用举例

要求使用 `HashSet` 完成以下操作：

- 添加元素 `"one"`、`"two"`、`"three"`
- 删除指定元素并观察返回值
- 重复添加同一元素并观察返回值
- 使用 `retainAll()` 保留交集
- 使用 `removeAll()` 删除交集
- 使用 `clear()` 清空集合
- 测试集合对 `null` 元素的处理

源码文件：

- `java-exp4/test6_1/HashSetTester.java`

### 2. 实验 6-4：Iterator 类使用举例

要求使用 `Iterator` 遍历 `Vector` 中的字符串元素，并在遍历过程中删除长度大于 4 的字符串，观察删除前后的集合变化。

源码文件：

- `java-exp4/test6_4/IteratorTester.java`

### 3. 实验 11-1：JSP 计算连续和

要求编写一个 JSP 页面：

- 在表单中输入一个自然数
- 服务器端读取参数
- 调用自定义方法计算 `1 + 2 + ... + n`
- 将结果返回到页面中显示

源码文件：

- `java-exp4/test11_1/Ex11_1.jsp`

## 五、实验步骤与关键代码说明

### 1. 实验 6-1

实现思路：

1. 使用 `HashSet<String>` 创建字符串集合。
2. 通过 `add()`、`remove()`、`retainAll()`、`removeAll()`、`clear()` 等方法演示集合操作。
3. 使用布尔返回值判断操作是否成功。
4. 通过 `contains(null)` 验证 `HashSet` 是否允许保存 `null`。

关键点说明：

- `HashSet` 中元素不能重复，因此重复添加同一元素时返回 `false`。
- `HashSet` 不保证遍历顺序，因此控制台中的元素顺序可能与教材图片略有不同。
- `retainAll()` 保留两个集合的交集。
- `removeAll()` 删除与另一个集合重复的元素。

### 2. 实验 6-4

实现思路：

1. 先将字符串数组转换为 `Vector<String>`。
2. 使用 `Iterator<String>` 逐个遍历元素。
3. 输出当前元素内容。
4. 若字符串长度大于 4，则调用 `Iterator.remove()` 删除该元素。
5. 遍历结束后输出删除后的 `Vector`。

关键点说明：

- 在遍历集合时，若要删除当前元素，必须调用 `Iterator.remove()`。
- 如果直接对集合调用删除方法，容易引发并发修改异常。
- 本实验中会删除 `"three"`、`"seven"`、`"eight"` 这三个长度大于 4 的字符串。

### 3. 实验 11-1

实现思路：

1. 使用 JSP 声明定义 `continuousSum(int n)` 方法。
2. 使用脚本片段读取表单参数 `number`。
3. 若用户未输入数据，则默认取 `10`。
4. 将字符串转换为整数并进行求和。
5. 使用 JSP 表达式在 HTML 页面中输出计算结果。

关键点说明：

- `<%! ... %>` 用于声明 JSP 页面级方法。
- `<% ... %>` 用于编写 Java 脚本片段。
- `<%= ... %>` 用于将表达式结果输出到页面。
- 本实验额外加入了简单输入校验，使页面对非数字输入更友好。

## 六、运行结果

### 1. 实验 6-1 控制台输出

```text
The initial set: [one, two, three]
The element 'three' is removed from the set: true
The element 'three' is removed from the set once again: false
The element 'three' is added to the set: true
The element 'three' is added to the set once again: false
The elements to retain: [one, two]
The set after retaining: [one, two]
The elements to remove: [two, three]
The set after removing: [one]
The set is empty after clearing: true
The set now contains a 'null' element: true
```

说明：

- `HashSet` 的输出顺序在不同 JDK 或不同运行中可能不同，但实验现象保持一致。

### 2. 实验 6-4 控制台输出

```text
The initial Vector is: [one, two, three, four, five, six, seven, eight, nine, ten]
one
two
three
four
five
six
seven
eight
nine
ten
The Vector after iteration is: [one, two, four, five, six, nine, ten]
```

### 3. 实验 11-1 页面预期结果

当用户输入 `10` 并单击“计算”按钮后，页面显示：

```text
10 的连续和是 55
```

若输入非数字，则页面显示错误提示：

```text
输入格式错误，请输入整数。
```

## 七、实验分析

### 1. HashSet 实验分析

- `HashSet` 适合保存不允许重复的数据。
- 插入和删除操作简单高效。
- 元素顺序不固定，因此在需要有序存储时不适合直接使用 `HashSet`。
- `HashSet` 允许插入一个 `null` 元素，这一点与部分其他集合实现不同。

### 2. Iterator 实验分析

- `Iterator` 提供了统一的集合遍历方式。
- 在遍历过程中安全删除元素是 `Iterator` 的一个重要用途。
- 通过该实验可以看出，删除操作会实时反映到原集合中。

### 3. JSP 实验分析

- JSP 能够把 Java 逻辑嵌入到 HTML 页面中，实现简单动态网页。
- 通过本实验理解了请求参数获取、数据处理和结果回显的完整流程。
- JSP 适合教学和简单页面演示，但在实际项目中通常会结合 Servlet、JavaBean 或 MVC 框架进一步分层。

## 八、实验中遇到的问题与解决方法

### 1. HashSet 输出顺序与教材图片不完全一致

原因：

- `HashSet` 本身不保证元素顺序。

解决方法：

- 理解实验重点在于“集合操作现象”，而不是固定的打印顺序。

### 2. 遍历时删除元素的方式容易出错

原因：

- 如果直接使用集合对象删除元素，而不是调用 `Iterator.remove()`，可能导致异常。

解决方法：

- 在遍历过程中只通过迭代器自身提供的 `remove()` 删除当前元素。

### 3. JSP 页面运行依赖 Web 容器

原因：

- JSP 不能像普通 Java 程序一样直接用 `java` 命令运行，必须部署到 Tomcat 等容器中。

解决方法：

- 将 `Ex11_1.jsp` 放到 Tomcat 的 Web 应用目录，再通过浏览器访问。

## 九、实验总结

通过本次实验，我分别练习了 Java 集合框架中的 `HashSet`、`Iterator` 以及 JSP 页面编程的基础方法。实验 6-1 让我掌握了集合去重和常见集合运算；实验 6-4 让我理解了迭代器在遍历与删除元素时的正确使用方式；实验 11-1 则帮助我理解了 JSP 页面中 Java 代码与 HTML 页面结合的基本过程。

总体来看，本次实验把“控制台程序中的数据结构操作”和“Web 页面中的动态处理”结合起来，有助于建立对 Java 基础知识更完整的理解。后续还可以继续学习 `ArrayList`、`Map`、`Servlet`、`JavaBean` 等内容，逐步形成更系统的 Java 编程能力。
