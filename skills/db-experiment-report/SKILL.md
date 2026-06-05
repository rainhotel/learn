---
name: db-experiment-report
description: >
  完成数据库实验报告。当用户要求"完成数据库实验报告"、"填写实验报告"、
  "做数据库实验"、处理数据库课程实验 docx 模板时触发。此技能读取实验报告 docx 模板，
  连接 SQL Server（Windows 认证）分析每道 SQL 题目，将 SQL 语句和查询结果
  写入新生成的 docx 报告，并完成实验心得。仅用于数据库/数据库系统实验或实习报告场景，
  不适用于其他类型的实验报告。
---

# 数据库实验报告

从 docx 模板出发，连接 SQL Server 分析题目，生成完整填写的实验报告。

## 前置检查

开始前确认：
1. 项目根目录有 `.env` 文件，定义了 `SQL_SERVER_HOST=<主机名>`
2. 存在由 `uv venv` 创建的 Python 虚拟环境 `.venv`，且已激活

   首次设置：
   
   ```bash
   uv venv
   source .venv/bin/activate
   pip install python-docx pyodbc
   ```
   ```powershell
   uv venv
   .venv\Scripts\activate
   pip install python-docx pyodbc
   ```

   后续使用（每次新终端窗口需重新激活）：
   ```bash
   source .venv/bin/activate
   ```
   ```powershell
   .venv\Scripts\activate
   ```

   所有 Python 脚本均在激活 venv 后通过 `python` 直接执行，自动使用 `.venv` 中的解释器和依赖（路径分隔符用正斜杠 `scripts/xxx.py`，可跨平台）。
3. 用户指定的 docx 文件存在

## 工作流程

按顺序完成以下 6 个阶段：

### 阶段 1：解析 docx，提取为 md

读取 docx 的所有段落和表格，提取实验名称、实验目的、实验步骤（每道题），保存为 `<原文件名>_content.md`，放在 docx 同目录。

md 结构：

```markdown
# <实验名称>
## 头部信息
- 实验名称：...（待填写）
- 学院/专业/年级/班次/姓名/学号：（待填写）

## 一、实验目的
1. ...
2. ...

## 二、实验内容与步骤
### 题目 1：<题目描述>
### 题目 2：<题目描述>
...

## 三、实验心得
（待填写）
```

使用 `scripts/extract_docx.py` 完成提取。

### 阶段 2：安全确认 + 连接数据库，发现表结构

**⚠️ 连接前必须先执行**：按照 `references/security.md` 中的"连接前的强制步骤"，向用户展示全库备份警告，**等待用户明确确认**（回复"是"/"确认"/"y"）后方可继续。用户未确认前，绝对不要连接数据库。

用户确认后，从 `.env` 读取 `SQL_SERVER_HOST`，用 Windows 身份认证连接 SQL Server。

**关键**：实验文档中的数据库名可能是代称（如 "StuCourse"），实际数据库名可能不同（如 "StudentCourse"）。先用 `SELECT name FROM sys.databases` 列出所有数据库，根据表名匹配找到正确的数据库。

连接后获取：
- 所有表名（`INFORMATION_SCHEMA.TABLES`）
- 每张表的列信息（`INFORMATION_SCHEMA.COLUMNS`）
- 每张表的全部数据（`SELECT * FROM <table>`）

将结果追加到 `_content.md` 的末尾，作为"附录：数据库原始数据"，供截图参考。

使用 `scripts/explore_db.py` 完成探索。

### 阶段 3：按题号编写 SQL，逐个保存

对阶段 1 提取的每道题：

1. **分析题目**，确定 SQL 语句（SELECT 及其子句：JOIN、LEFT JOIN、子查询、EXISTS、IN、GROUP BY、HAVING、聚合函数等）
2. **执行 SQL**（允许所有操作，安全策略见 `references/security.md`）
3. **格式化结果**：列名 | 分隔线 | 数据行 | 行数。浮点数保留 2 位小数
4. **保存 SQL** 到 docx 同目录的 `SQLQuery<N>.sql`：

```sql
-- 题目<N>：<题目原文>
-- 涉及表：<表名列表>
<SQL 语句>
```

若一道题要求两种方法（连接查询 + 嵌套查询），在同一文件中用注释分隔：

```sql
-- 方法一：连接查询
<SQL 1>

-- 方法二：嵌套查询
<SQL 2>
```

### 阶段 4：生成填写的 docx 报告

在原 docx 基础上，在每个题目的段落之后按顺序插入三部分内容：

1. `涉及的各基本表原始数据（截图）：[此处插入截图]` — 截图占位符
2. `SQL查询语句：` + 完整 SQL 文本（保留缩进）
3. `执行SQL语句后查询结果（截图）：[此处插入截图]` ，不要包含查询结果文本。

对于有两种方法的题目，分别标注 `【方法一：连接查询】` / `【方法二：EXISTS嵌套查询】` 等。

**插入顺序**：从后往前（题目 N → 题目 2），避免段落索引偏移。
**题目 1** 特殊处理：原模板可能已有占位段落（P10 原始数据截图 / P11 SQL / P12 结果截图），直接在对应段落后插入内容即可。

截图处**只写占位符文字**，不插入实际图片。

使用 `scripts/generate_docx.py` 辅助生成。

新文件保存为 `<原文件名>_已完成.docx`，在原 docx 同目录下。

### 阶段 5：完成实验心得

在 docx 中"三、实验心得"段落之后写入心得。用第一人称，结合实验目的和实际查询结果来写，避免套话，**用一段话回答，不要分点，字数控制在200字左右**。

结构参考：
- 本次实验**掌握了什么**（连接查询、嵌套查询的语法和区别、IN vs EXISTS 的场景选择、LEFT JOIN vs INNER JOIN 等）
- 实验中**遇到的问题及解决**（如数据库名不匹配的情况、LEFT JOIN 产生 NULL 的含义等）
- 对 SQL 查询的**初步认识**（从具体数据出发的观察）

将心得写入 docx 对应位置。

### 阶段 6：汇报结果

向用户报告：

- 输出文件列表（完整路径）
- 各题查询结果摘要（题目、结果行数、关键发现）
- 需要手动完成的：
  - 头部信息（姓名、学号等）
  - 所有截图
  - 检查实验心得是否满意

## 常见问题

| 问题 | 处理方式 |
|------|---------|
| 数据库名不匹配 | 用 `sys.databases` 列出所有库，按表名匹配 |
| char 字段尾部空格 | `.strip()` 处理 |
| Unicode 编码错误 | PowerShell 中 `$env:PYTHONIOENCODING='utf-8'` |
| docx 段落索引偏移 | 从后往前插入 |
| 题目数量不确定 | 从 md 动态检测，范围 8-15 题均可 |
| ODBC 驱动未安装 | 提示用户安装 "ODBC Driver 17 for SQL Server" |
