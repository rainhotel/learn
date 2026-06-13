# Java 实验四：6-1、6-4、11-1

本目录用于整理《Java 语言程序设计（第 3 版）》中的三个实验：

- `6-1`：`HashSet` 应用举例
- `6-4`：`Iterator` 类使用举例
- `11-1`：JSP 页面计算 1 到 n 的连续和

## 目录说明

- `test6_1/HashSetTester.java`
  - `HashSet` 的增删改查、集合保留、集合删除、清空与 `null` 元素测试
- `test6_4/IteratorTester.java`
  - 使用 `Iterator` 遍历 `Vector`，并在遍历过程中安全删除满足条件的元素
- `test11_1/Ex11_1.jsp`
  - 使用 JSP 声明、脚本片段和表达式实现“输入自然数并计算连续和”
- `report.md`
  - 实验报告 Markdown 版
- `generate_report.py`
  - 生成 `.docx` 实验报告
  - 默认输出文件名：`java-exp4-report.docx`

## 编译与运行

### 6-1 HashSet 实验

```powershell
cmd /c javac test6_1\\HashSetTester.java
cmd /c java test6_1.HashSetTester
```

### 6-4 Iterator 实验

```powershell
cmd /c javac test6_4\\IteratorTester.java
cmd /c java test6_4.IteratorTester
```

### 11-1 JSP 实验

将 `test11_1/Ex11_1.jsp` 部署到 Tomcat 的 Web 应用目录，例如：

```text
<tomcat>/webapps/JSPTest/Ex11_1.jsp
```

浏览器访问：

```text
http://localhost:8080/JSPTest/Ex11_1.jsp
```

本机当前已经部署到：

```text
D:\moniC\project\learn\.tools\apache-tomcat-9.0.118\webapps\JSPTest\Ex11_1.jsp
```

可直接访问：

```text
http://localhost:8080/JSPTest/Ex11_1.jsp
```

快速启动 / 停止：

```powershell
.\start-tomcat.ps1
.\stop-tomcat.ps1
```

## 说明

- 本次本地环境已验证 `6-1` 与 `6-4` 的 Java 代码可编译运行。
- 本机已在工作区安装并启动 `Tomcat 9.0.118`，`11-1` 页面已验证可访问。
