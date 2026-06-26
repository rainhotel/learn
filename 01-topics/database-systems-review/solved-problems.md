# 典型题与标准解法

## 0. 关系代数: 查询选修 1 号课程的学生姓名

题目：用关系代数表达“查询选修了 1 号课程的学生姓名”。

关系：

```text
Student(Sno, Sname, Ssex, Sage, Sdept)
SC(Sno, Cno, Grade)
```

解：

1. 先从 `SC` 中选择课程号为 1 的选课记录：

```text
σ_Cno='1'(SC)
```

2. 与 `Student` 按同名属性 `Sno` 自然连接：

```text
Student ⋈ σ_Cno='1'(SC)
```

3. 投影学生姓名：

```text
π_Sname(Student ⋈ σ_Cno='1'(SC))
```

答：

```text
π_Sname(Student ⋈ σ_Cno='1'(SC))
```

## 0.1 关系代数: 查询选修全部课程的学生

题目：用关系代数表达“查询选修了全部课程的学生学号”。

关系：

```text
SC(Sno, Cno, Grade)
Course(Cno, Cname, Ccredit)
```

解：

“全部课程”是典型除运算。

1. 学生选课关系只保留学号和课程号：

```text
π_Sno,Cno(SC)
```

2. 全部课程号：

```text
π_Cno(Course)
```

3. 做除运算：

```text
π_Sno,Cno(SC) ÷ π_Cno(Course)
```

答：

```text
π_Sno,Cno(SC) ÷ π_Cno(Course)
```

## 0.2 完整性判断题

题目：`SC(Sno,Cno,Grade)` 中 `Sno` 参照 `Student(Sno)`，若插入一条选课记录的 `Sno` 在 `Student` 中不存在，违反什么完整性？

解：

`SC.Sno` 是外码，参照 `Student.Sno`。外码取值必须等于被参照关系中某个主码值，或在允许时为空。现在插入的 `Sno` 在学生表中不存在，因此违反参照完整性。

答：违反参照完整性。

## 1. SQL: 查询选修了 1 号课程的学生姓名

题目：查询选修了课程号为 `1` 的学生姓名。

表：

```text
Student(Sno, Sname, Ssex, Sage, Sdept)
SC(Sno, Cno, Grade)
```

解法 1：连接查询。

```sql
SELECT Sname
FROM Student JOIN SC ON Student.Sno = SC.Sno
WHERE Cno = '1';
```

解法 2：嵌套查询。

```sql
SELECT Sname
FROM Student
WHERE Sno IN (
    SELECT Sno
    FROM SC
    WHERE Cno = '1'
);
```

答题要点：

- 涉及学生表和选课表。
- 连接条件是 `Student.Sno = SC.Sno`。
- 筛选条件是 `Cno = '1'`。

## 2. SQL: 查询没有选课的学生

题目：查询没有任何选课记录的学生。

解法 1：`NOT EXISTS`。

```sql
SELECT *
FROM Student S
WHERE NOT EXISTS (
    SELECT *
    FROM SC
    WHERE SC.Sno = S.Sno
);
```

解法 2：左外连接。

```sql
SELECT Student.*
FROM Student LEFT JOIN SC ON Student.Sno = SC.Sno
WHERE SC.Sno IS NULL;
```

易错点：

- 不能用普通连接，因为普通连接会直接丢掉没选课的学生。

## 3. SQL: 查询平均成绩大于 80 的课程

题目：查询平均成绩大于 80 的课程号和平均成绩。

```sql
SELECT Cno, AVG(Grade) AS AvgGrade
FROM SC
GROUP BY Cno
HAVING AVG(Grade) > 80;
```

易错点：

- `AVG(Grade) > 80` 是组条件，写在 `HAVING`，不是 `WHERE`。

## 4. SQL: 查询选修了全部课程的学生

题目：查询选修了全部课程的学生姓名。

思路：

- “全部”常用双重 `NOT EXISTS`。
- 不存在这样一门课程：该学生没有选。

```sql
SELECT Sname
FROM Student S
WHERE NOT EXISTS (
    SELECT *
    FROM Course C
    WHERE NOT EXISTS (
        SELECT *
        FROM SC
        WHERE SC.Sno = S.Sno
          AND SC.Cno = C.Cno
    )
);
```

答题解释：

- 外层遍历学生。
- 中层找课程。
- 内层判断该学生是否选了该课程。
- 如果不存在“没选的课程”，说明该学生选了全部课程。

## 5. SQL: 创建选课表

题目：创建选课表 `SC(Sno, Cno, Grade)`，要求：

- `(Sno, Cno)` 为主键。
- `Sno` 参照 `Student(Sno)`。
- `Cno` 参照 `Course(Cno)`。
- `Grade` 在 0 到 100。

```sql
CREATE TABLE SC (
    Sno char(9),
    Cno char(4),
    Grade int,
    PRIMARY KEY (Sno, Cno),
    FOREIGN KEY (Sno) REFERENCES Student(Sno),
    FOREIGN KEY (Cno) REFERENCES Course(Cno),
    CHECK (Grade >= 0 AND Grade <= 100)
);
```

易错点：

- 复合主键要用表级约束。
- 外键必须参照已有表的候选码或主键。

## 6. E-R 转关系模式

题目：学生和课程之间是多对多联系“选修”，联系有属性“成绩”。转换为关系模式。

解：

实体：

```text
学生(学号, 姓名, 性别)
课程(课程号, 课程名, 学分)
```

联系：

```text
选修(学号, 课程号, 成绩)
```

主键：

```text
学生: 学号
课程: 课程号
选修: (学号, 课程号)
```

外键：

```text
选修.学号 -> 学生.学号
选修.课程号 -> 课程.课程号
```

说明：

- m:n 联系必须单独转换为关系模式。
- 联系属性成绩放入联系关系中。

## 7. 求候选码

题目：关系模式 `R(A,B,C,D,E)`，函数依赖集：

```text
F = { AB -> C, C -> D, D -> E }
```

求候选码。

解：

1. 观察右部出现的属性：

```text
C, D, E
```

2. 未出现在右部的属性：

```text
A, B
```

这些属性必须包含在候选码中。

3. 求 `(AB)+`：

```text
初始: AB
由 AB -> C 得 ABC
由 C -> D 得 ABCD
由 D -> E 得 ABCDE
```

4. `(AB)+ = ABCDE`，所以 `AB` 是超码。

5. 检查最小性：

```text
A+ = A
B+ = B
```

都不能推出全部属性。

答：候选码为 `AB`。

## 8. 判断范式

题目：关系模式：

```text
选课(学号, 课程号, 姓名, 课程名, 成绩)
```

函数依赖：

```text
学号 -> 姓名
课程号 -> 课程名
(学号, 课程号) -> 成绩
```

判断最高范式。

解：

1. 候选码为：

```text
(学号, 课程号)
```

2. 非主属性：

```text
姓名, 课程名, 成绩
```

3. 判断部分依赖：

```text
学号 -> 姓名
课程号 -> 课程名
```

姓名和课程名都只依赖候选码的一部分。

4. 因此存在非主属性对码的部分依赖，不满足 2NF。

答：该关系满足 1NF，但不满足 2NF，最高为 1NF。

分解到 2NF：

```text
学生(学号, 姓名)
课程(课程号, 课程名)
选课(学号, 课程号, 成绩)
```

## 9. 判断 3NF

题目：

```text
学生(学号, 姓名, 所在系, 系主任)
```

函数依赖：

```text
学号 -> 姓名
学号 -> 所在系
所在系 -> 系主任
```

判断范式并分解。

解：

1. 候选码为 `学号`。
2. 因为候选码是单属性，不存在部分依赖，所以满足 2NF。
3. 存在：

```text
学号 -> 所在系
所在系 -> 系主任
```

所以 `系主任` 传递依赖于 `学号`。

4. 不满足 3NF。

分解：

```text
学生(学号, 姓名, 所在系)
系(所在系, 系主任)
```

## 10. BCNF 判断

题目：`R(A,B,C)`，函数依赖：

```text
A -> B
B -> A
A -> C
```

判断是否 BCNF。

解：

1. 求候选码。

```text
A+ = ABC，所以 A 是候选码
B+ = BAC，所以 B 是候选码
```

2. 检查每个非平凡依赖的左部：

```text
A -> B，A 是码
B -> A，B 是码
A -> C，A 是码
```

答：满足 BCNF。

## 11. 无损连接判断

题目：`R(A,B,C)`，`F={A -> B}`，分解为：

```text
R1(A,B)
R2(A,C)
```

判断是否无损连接。

解：

1. 交集：

```text
R1 ∩ R2 = A
```

2. 判断交集是否决定某个子模式：

```text
A -> B
所以 A -> AB，即 A -> R1
```

3. 满足二元无损连接判定。

答：该分解无损连接。

## 12. 并发问题判断

题目：

```text
T1: R(A=16)
T2: R(A=16)
T1: A=A-1, W(A=15)
T2: A=A-1, W(A=15)
```

问发生什么问题？

解：

两个事务都读到旧值 16，各自减 1 后写回 15。第二个写回覆盖第一个写回，导致一次修改丢失。

答：发生丢失修改。

## 13. 封锁协议效果题

题目：一级、二级、三级封锁协议分别能解决什么问题？

答：

一级封锁协议要求事务在写数据前加 X 锁，并保持到事务结束，因此能防止两个事务同时写同一数据造成的丢失修改。

二级封锁协议在一级基础上要求读数据前加 S 锁，读完释放，因此除了防止丢失修改，还能防止读到其他事务未提交修改造成的脏读。

三级封锁协议在一级基础上要求读数据前加 S 锁，并保持到事务结束，因此除了防止丢失修改和脏读，还能防止同一事务两次读取同一数据结果不同，即保证可重复读。

## 14. 恢复策略题

题目：系统故障后如何恢复？

答：

系统故障会导致内存信息丢失，但外存数据库通常没有被破坏。恢复时 DBMS 在重启后检查日志文件，找出故障发生时尚未完成的事务和已经提交但可能尚未写入数据库的事务。对未完成事务执行 UNDO，撤销它们已经做过的修改；对已提交事务执行 REDO，保证其结果真正反映到数据库中。这样可以把数据库恢复到一致状态。
