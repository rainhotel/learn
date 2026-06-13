# Java Programming Lab Notes

## 集合框架：本次实验提炼

### HashSet

- `HashSet` 是 `Set` 接口的典型实现。
- 特点：
  - 元素不能重复
  - 不保证元素顺序
  - 允许一个 `null` 元素
- 常见方法：
  - `add(E e)`：添加元素，成功返回 `true`
  - `remove(Object o)`：删除元素，成功返回 `true`
  - `retainAll(Collection<?> c)`：保留交集
  - `removeAll(Collection<?> c)`：删除交集
  - `clear()`：清空集合

### Iterator

- `Iterator` 用于顺序访问集合中的元素。
- 常见方法：
  - `hasNext()`：判断是否还有下一个元素
  - `next()`：获取下一个元素
  - `remove()`：删除最近一次 `next()` 返回的元素
- 重点：
  - 在遍历时删除元素，应使用 `Iterator.remove()`
  - 不建议在遍历过程中直接调用集合对象的删除方法

## JSP：本次实验提炼

### 三种基本脚本元素

- `<%! ... %>`
  - JSP 声明
  - 用于定义方法或成员变量
- `<% ... %>`
  - JSP 脚本片段
  - 用于编写会被放到 `_jspService()` 中执行的 Java 代码
- `<%= ... %>`
  - JSP 表达式
  - 用于把表达式结果直接输出到页面

### 本题流程

1. 用户在表单中输入自然数
2. `request.getParameter("number")` 获取参数
3. 将字符串转换为整数
4. 调用 `continuousSum(int n)` 计算结果
5. 将结果返回到页面

## 运行现象

- `HashSet` 打印顺序可能和教材不同，但不影响实验结论。
- `Iterator` 删除后，原 `Vector` 会直接变化。
- JSP 本质上会被容器转换成 Servlet 后再执行。
