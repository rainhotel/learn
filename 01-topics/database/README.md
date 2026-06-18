# 实验9 + 实验10：数据库完整性 + 数据库安全性

## 基本信息

- **实验名称**：南京信息工程大学 数据库系统 实验(实习)报告
- **原始模板**：`D:\不知道是啥\实验6.docx`
- **提取稿**：`01-topics/database/实验6_实验9-10_content.md`
- **待填写**：学院/专业/年级/班次/姓名/学号

## 输出文件清单

| 文件 | 说明 |
|------|------|
| `01-topics/database/实验9_数据库完整性.sql` | 题目1~3 + 触发器测试 + 约束验证 |
| `01-topics/database/实验10_数据库安全性.sql` | 题目4~5 + 登录/用户/权限/级联回收 |
| `01-topics/database/实验6_实验9-10_content.md` | docx 模板提取稿 |

## 题目与 SQL 脚本对照

### 题目1：创建数据库和3个表 → 实验9 SQL 第14~59行

- `CREATE DATABASE StudentCourse1`
- `CREATE TABLE Student1` — PRIMARY KEY, UNIQUE, CHECK, NOT NULL, DEFAULT
- `CREATE TABLE Course` — PRIMARY KEY, CHECK (学期1~8, 学时>0, 学分>0)
- `CREATE TABLE StuCourse` — 复合主键, 外键(Student1, Course), CHECK(成绩0~100)

### 题目2：修改约束定义 → 实验9 SQL 第82~103行

```sql
ALTER TABLE Course DROP CONSTRAINT Tn_Check
ALTER TABLE Course ADD CONSTRAINT Tn_Check CHECK (学时 >= 0 AND 学时 <= 120)
```

> 注意：docx 中写为 "Tm_Check"，实际建表约束名为 "Tn_Check"

### 题目3：6类约束验证 → 实验9 SQL 第109~170行

在 SSMS 中运行后截图每个验证的错误消息（用于填入报告）：
1. PRIMARY KEY — 插入重复学号
2. FOREIGN KEY — 插入不存在的学号/课程号
3. NOT NULL — 插入 NULL 姓名
4. UNIQUE — 插入重复姓名
5. DEFAULT — 不指定性别，自动为 '男'
6. CHECK — 学分<=0 / 成绩>100 / 性别非法

### 题目4：实验10 数据库安全性 → 实验10 SQL 全文

执行顺序：
1. 创建登录名 `sutdLogin` / `sutdLogin2`
2. 映射用户 `sutdUsr` / `sutdUsr2`
3. 授予 SELECT → 验证
4. 授予 INSERT WITH GRANT OPTION
5. sutdUsr 传递 INSERT 给 sutdUsr2
6. sutdUsr2 插入数据验证
7. CASCADE 级联回收 INSERT → 再次验证回收有效

### 题目5：思考与练习（级联回收）→ 实验10 SQL 第109~150行

- 练习1：`GRANT UPDATE, DELETE ON Student TO sutdUsr WITH GRANT OPTION`
- 练习2：sutdUsr → `GRANT UPDATE, DELETE ON Student TO sutdUsr2`
- 练习3：`REVOKE UPDATE, DELETE ON Student FROM sutdUsr CASCADE`

## 触发器的分析方法

实验中创建的触发器 `SC_trg`：

```sql
CREATE TRIGGER SC_trg ON StuCourse FOR INSERT
AS
    IF (SELECT 学号 FROM inserted) IN (SELECT 学号 FROM Student1)
    BEGIN
        RAISERROR ('插入操作违背数据的一致性。', 16, 1)
        ROLLBACK TRANSACTION
    END
```

分析要点：
- 第一次 INSERT 成功（此时触发器尚未存在）
- 创建触发器后，第二次 INSERT 被拦截：学号 '070110' 在 Student1 中存在
- 触发器的逻辑检查插入的学号是否在学生表中，存在则报错并回滚
- 这是一种参照完整性保护机制，防止向选课表插入不存在学生的选课记录

## 实验心得（可直接填入报告）

本次实验我系统掌握了 SQL Server 中 6 类数据完整性约束（PRIMARY KEY、FOREIGN KEY、NOT NULL、UNIQUE、DEFAULT、CHECK）的定义和使用方法，并通过实际测试验证了每种约束的作用机制。在实验过程中，我发现主键约束和外键约束共同保证了数据库的实体完整性和参照完整性，而 CHECK 约束则实现了用户自定义的业务规则。特别是在修改约束定义时，我体会到 ALTER TABLE 给数据库维护带来的灵活性。在安全性实验部分，我学习了登录名、数据库用户以及权限管理之间的层次关系，通过 GRANT WITH GRANT OPTION 实现了权限传递，并用 REVOKE CASCADE 验证了级联回收的效果。实验中遇到的问题主要是总学分的 CHECK 约束要求正值，而测试时用了 0，导致插入失败，这让我更加注意在编写 SQL 语句前仔细检查表结构中的约束条件。

## 需要手动完成的部分

1. **报告头部信息**：学院、专业、年级、班次、姓名、学号
2. **所有截图**：
   - 每个约束验证的错误消息截图
   - 每个权限操作的成功/失败截图
   - 触发器测试前后的数据截图
   - 权限查询结果截图
3. **SSMS 中的手动操作**：
   - 将 SQL Server 设为混合身份验证模式
   - 以 sutdLogin/sutdLogin2 身份登录验证（可选，EXECUTE AS 已模拟）
4. **检查实验心得**：是否满意，可根据实际执行情况调整
