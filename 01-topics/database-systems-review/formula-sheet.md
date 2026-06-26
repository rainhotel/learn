# 公式、SQL 模板与规则清单

## 1. 基本概念速记

```text
DB = 数据库
DBMS = 数据库管理系统
DBS = 数据库系统
```

```text
DBS = DB + DBMS + 应用程序 + DBA + 用户 + 硬件/软件环境
```

数据模型三要素：

```text
数据结构 + 数据操作 + 完整性约束
```

关系模型三要素：

```text
关系数据结构 + 关系操作 + 关系完整性约束
```

三级模式：

```text
外模式 - 模式 - 内模式
```

两级映像：

```text
外模式/模式映像 -> 逻辑独立性
模式/内模式映像 -> 物理独立性
```

## 2. 关系模型速记

术语对应：

```text
表       -> 关系
行       -> 元组
列       -> 属性
单元格值 -> 分量
取值范围 -> 域
```

关系模式：

```text
R(U, D, DOM, F)
常简写为 R(U) 或 R(A1, A2, ..., An)
```

候选码：

```text
最小唯一标识元组的属性集
```

实体完整性：

```text
主码属性不能为空
```

参照完整性：

```text
外码值必须等于被参照关系某个主码值，或在允许时为空
```

用户定义完整性：

```text
满足具体应用语义，如成绩 0-100
```

## 3. 关系代数

选择：

```text
σ_F(R)
```

投影：

```text
π_A1,A2,...,Ak(R)
```

并：

```text
R ∪ S
```

差：

```text
R - S
```

交：

```text
R ∩ S
```

笛卡尔积：

```text
R x S
```

连接：

```text
R ⋈_{条件} S = σ_条件(R x S)
```

自然连接：

```text
R ⋈ S
```

除：

```text
R(X,Y) ÷ S(Y)
```

含义：找出满足“对 S 中所有 Y 都匹配”的 X。

常见表达：

```text
选修 1 号课程的学生:
π_Sno(σ_Cno='1'(SC))

选修 1 号课程的学生姓名:
π_Sname(Student ⋈ σ_Cno='1'(SC))

选修全部课程的学生:
π_Sno,Cno(SC) ÷ π_Cno(Course)
```

## 4. SQL 定义模板

### 创建数据库

```sql
CREATE DATABASE dbname;
```

### 创建表

```sql
CREATE TABLE 表名 (
    列名 数据类型 [NOT NULL] [DEFAULT 默认值],
    列名 数据类型,
    PRIMARY KEY (列名),
    FOREIGN KEY (列名) REFERENCES 参照表(列名),
    UNIQUE (列名),
    CHECK (条件)
);
```

### 修改表

```sql
ALTER TABLE 表名 ADD 列名 数据类型;
ALTER TABLE 表名 DROP COLUMN 列名;
ALTER TABLE 表名 ADD CONSTRAINT 约束名 CHECK (条件);
ALTER TABLE 表名 DROP CONSTRAINT 约束名;
```

### 删除表

```sql
DROP TABLE 表名;
```

### 创建索引

```sql
CREATE INDEX 索引名 ON 表名(列名);
CREATE UNIQUE INDEX 索引名 ON 表名(列名);
```

## 5. SQL 查询模板

### 基本查询

```sql
SELECT [DISTINCT] 列名
FROM 表名
WHERE 条件
ORDER BY 列名 [ASC|DESC];
```

### 分组查询

```sql
SELECT 分组列, 聚集函数(列)
FROM 表名
WHERE 分组前条件
GROUP BY 分组列
HAVING 分组后条件;
```

### 连接查询

```sql
SELECT 列
FROM A JOIN B ON A.key = B.key
WHERE 条件;
```

### 左外连接

```sql
SELECT 列
FROM A LEFT JOIN B ON A.key = B.key;
```

### 嵌套查询 IN

```sql
SELECT 列
FROM A
WHERE 属性 IN (
    SELECT 属性
    FROM B
    WHERE 条件
);
```

### EXISTS

```sql
SELECT *
FROM A
WHERE EXISTS (
    SELECT *
    FROM B
    WHERE B.key = A.key
);
```

### 集合查询

```sql
查询1
UNION
查询2;
```

```sql
查询1
INTERSECT
查询2;
```

```sql
查询1
EXCEPT
查询2;
```

## 6. SQL 更新模板

### 插入

```sql
INSERT INTO 表名 (列1, 列2)
VALUES (值1, 值2);
```

### 插入查询结果

```sql
INSERT INTO 表名 (列1, 列2)
SELECT 列1, 列2
FROM 另一个表
WHERE 条件;
```

### 修改

```sql
UPDATE 表名
SET 列名 = 表达式
WHERE 条件;
```

### 删除

```sql
DELETE FROM 表名
WHERE 条件;
```

## 7. 视图模板

### 创建视图

```sql
CREATE VIEW 视图名 [(列名列表)]
AS
SELECT 查询语句
[WITH CHECK OPTION];
```

### 删除视图

```sql
DROP VIEW 视图名;
```

## 8. 授权模板

### 授权

```sql
GRANT 权限
ON 对象
TO 用户
[WITH GRANT OPTION];
```

### 回收

```sql
REVOKE 权限
ON 对象
FROM 用户
[CASCADE];
```

## 9. E-R 转关系规则

实体：

```text
实体 -> 一个关系模式
实体属性 -> 关系属性
实体码 -> 关系主码
```

1:1 联系：

```text
可单独建关系，也可把一方主码加入另一方作为外码
```

1:n 联系：

```text
在 n 端加入 1 端主码作为外码，并加入联系属性
```

m:n 联系：

```text
联系单独建关系，两端主码共同作为主码，并加入联系属性
```

## 10. 函数依赖与范式

函数依赖：

```text
X -> Y
```

候选码：

```text
K+ = U 且 K 的任何真子集闭包都不是 U
```

1NF：

```text
属性不可再分
```

2NF：

```text
1NF + 非主属性完全依赖于码
```

3NF：

```text
2NF + 非主属性不传递依赖于码
```

BCNF：

```text
每个非平凡函数依赖 X -> Y 中，X 都是超码
```

## 11. 属性闭包算法

```text
输入 X 和 F
X+ = X
重复扫描 F:
  若 A -> B 且 A 包含于 X+
  则 X+ = X+ 并 B
直到 X+ 不再变化
```

用途：

- 判断 `X -> Y` 是否成立：看 `Y` 是否包含于 `X+`。
- 判断 X 是否为超码：看 `X+` 是否等于全属性集 U。
- 求候选码。

## 12. Armstrong 公理

自反律：

```text
若 Y 包含于 X，则 X -> Y
```

增广律：

```text
若 X -> Y，则 XZ -> YZ
```

传递律：

```text
若 X -> Y 且 Y -> Z，则 X -> Z
```

常用规则：

```text
合并: X -> Y 且 X -> Z，则 X -> YZ
分解: X -> YZ，则 X -> Y 且 X -> Z
伪传递: X -> Y 且 WY -> Z，则 WX -> Z
```

## 13. 无损连接判定

二元分解：

```text
R -> R1, R2
```

若：

```text
(R1 ∩ R2) -> R1
```

或：

```text
(R1 ∩ R2) -> R2
```

则分解无损连接。

## 14. 事务 ACID

```text
A Atomicity 原子性: 要么全做，要么全不做
C Consistency 一致性: 事务前后数据库一致
I Isolation 隔离性: 并发事务互不干扰
D Durability 持久性: 提交结果永久保存
```

## 15. 并发不一致

```text
丢失修改: 两个事务修改同一数据，后写覆盖前写
脏读: 读到另一个事务未提交的数据
不可重复读: 同一事务两次读取同一数据结果不同
```

## 16. 锁相容矩阵

```text
        已有 S   已有 X
申请 S    Y       N
申请 X    N       N
```

## 17. 封锁协议

一级封锁协议：

```text
写前加 X 锁，事务结束释放
防止丢失修改
```

二级封锁协议：

```text
一级 + 读前加 S 锁，读完释放
防止丢失修改、脏读
```

三级封锁协议：

```text
一级 + 读前加 S 锁，事务结束释放
防止丢失修改、脏读、不可重复读
```

两段锁：

```text
加锁阶段: 只能加锁，不能解锁
解锁阶段: 只能解锁，不能加锁
```

性质：

```text
两段锁是可串行化的充分条件，不是必要条件
```

## 18. 恢复规则

事务故障：

```text
UNDO 未完成事务
```

系统故障：

```text
UNDO 未完成事务
REDO 已提交但可能未写入数据库的事务
```

介质故障：

```text
装入备份
利用日志 REDO 备份后已提交事务
```

备份分类：

```text
静态转储: 转储期间不允许更新
动态转储: 转储期间允许更新
海量转储: 全库备份
增量转储: 只备份更新过的数据
```
