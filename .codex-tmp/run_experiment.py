"""
执行实验4所有题目的SQL，逐题保存SQL文件和结果
"""
import pyodbc
import os

OUTDIR = r"D:\xwechat_files\wxid_0vnbq4ga2qso22_630f\msg\file\2026-06"

def connect():
    return pyodbc.connect(
        "DRIVER={ODBC Driver 18 for SQL Server};"
        "SERVER=.\\SQLEXPRESS;"
        "DATABASE=StudentCourse;"
        "Trusted_Connection=yes;"
        "TrustServerCertificate=yes"
    )

def save_sql(num, title, sql_list, note=""):
    sqlpath = os.path.join(OUTDIR, f"SQLQuery{num}.sql")
    with open(sqlpath, "w", encoding="utf-8") as f:
        f.write(f"-- 题目{num}：{title}\n")
        if note:
            f.write(f"-- {note}\n")
        for i, s in enumerate(sql_list):
            if len(sql_list) > 1:
                label = f"方法{i+1}" if not s.startswith("--") else ""
                if label:
                    f.write(f"\n-- {label}\n")
            f.write(s.strip() + "\n")

    # 追加查询结果
    c = conn.cursor()
    with open(sqlpath, "a", encoding="utf-8") as f:
        f.write("\n\n-- ====== 执行结果 ======\n")
        for i, s in enumerate(sql_list):
            if s.startswith("--"): # 注释跳过
                continue
            if len(sql_list) > 1:
                f.write(f"\n-- 方法{i+1}结果：\n")
            try:
                c.execute(s)
                conn.commit()
                if c.description:
                    rows = c.fetchall()
                    cols = [d[0] for d in c.description]
                    f.write(f"-- 列: {' | '.join(cols)}\n")
                    f.write(f"-- 行数: {len(rows)}\n")
                    for r in rows:
                        vals = [str(v).strip() if v is not None else "NULL" for v in r]
                        f.write(f"-- {' | '.join(vals)}\n")
                else:
                    f.write(f"-- 影响行数: {c.rowcount}\n")
            except Exception as e:
                f.write(f"-- 执行失败(预期): {e}\n")

    print(f"Q{num}: {title[:50]}... saved")

# ====== 重新初始化数据 ======
conn = connect()
c = conn.cursor()

# 清理视图
for v in ['Stu_View_1','Stu_View_2','SC_View','VMgrade','CP_View','Stu_View']:
    try:
        c.execute(f'DROP VIEW {v}')
    except:
        pass

# 清理数据
c.execute("DELETE FROM StuCourse")
c.execute("DELETE FROM Student")
c.execute("DELETE FROM Course")
conn.commit()

# 重新插入
c.execute("INSERT INTO Student VALUES ('070101','陈明','男','计算机科学与技术','2000-01-15',85,NULL)")
c.execute("INSERT INTO Student VALUES ('070102','李丽','女','计算机科学与技术','2000-03-20',90,NULL)")
c.execute("INSERT INTO Student VALUES ('070103','王强','男','计算机科学与技术','1999-11-08',78,NULL)")
c.execute("INSERT INTO Student VALUES ('070201','赵芳','女','软件工程','2000-06-12',82,NULL)")
c.execute("INSERT INTO Student VALUES ('070202','孙伟','男','软件工程','2000-09-25',75,NULL)")
c.execute("INSERT INTO Student VALUES ('700210','周明','男','计算机科学与技术','2001-04-18',65,NULL)")
c.execute("INSERT INTO Student VALUES ('080101','吴刚','男','网络工程','2000-12-03',70,NULL)")
c.execute("INSERT INTO Student VALUES ('080102','郑红','女','网络工程','2001-05-17',88,NULL)")

c.execute("INSERT INTO Course VALUES ('1001','高等数学1',3,64,4)")
c.execute("INSERT INTO Course VALUES ('1002','高等数学2',4,64,4)")
c.execute("INSERT INTO Course VALUES ('2001','数据结构',4,48,3)")
c.execute("INSERT INTO Course VALUES ('2002','操作系统',5,64,4)")
c.execute("INSERT INTO Course VALUES ('3001','数据库系统',5,48,3)")
c.execute("INSERT INTO Course VALUES ('3002','计算机网络',6,48,3)")

c.execute("INSERT INTO StuCourse VALUES ('070101','1001',85)")
c.execute("INSERT INTO StuCourse VALUES ('070101','2001',78)")
c.execute("INSERT INTO StuCourse VALUES ('070101','3001',92)")
c.execute("INSERT INTO StuCourse VALUES ('070102','1001',88)")
c.execute("INSERT INTO StuCourse VALUES ('070102','2002',82)")
c.execute("INSERT INTO StuCourse VALUES ('070102','3001',95)")
c.execute("INSERT INTO StuCourse VALUES ('070103','1001',72)")
c.execute("INSERT INTO StuCourse VALUES ('070103','2001',68)")
c.execute("INSERT INTO StuCourse VALUES ('070201','1001',90)")
c.execute("INSERT INTO StuCourse VALUES ('070201','3001',76)")
c.execute("INSERT INTO StuCourse VALUES ('070202','2002',85)")
c.execute("INSERT INTO StuCourse VALUES ('700210','1001',55)")
conn.commit()
print("Data re-initialized: Student 8, Course 6, StuCourse 12")

# ========== Q1 ==========
save_sql(1, "创建一个行列子集视图Stu_View，可以看到计算机科学与技术专业的学生情况", [
    """CREATE VIEW Stu_View AS
SELECT * FROM Student WHERE 专业名 = '计算机科学与技术'"""
])

# ========== Q2 ==========
save_sql(2, "在视图Stu_View上创建视图Stu_View_1，可以看到学生的学号、姓名、性别、专业，年龄和总学分", [
    """CREATE VIEW Stu_View_1 AS
SELECT 学号, 姓名, 性别, 专业名,
       DATEDIFF(YEAR, 出生日期, GETDATE()) AS 年龄,
       总学分
FROM Stu_View""",
    "SELECT * FROM Stu_View_1"
])

# ========== Q3 ==========
save_sql(3, "在视图Stu_View中添加一个计算机科学与技术的学生信息：702030严正男2001-02-13", [
    """INSERT INTO Stu_View (学号, 姓名, 性别, 专业名, 出生日期, 总学分)
VALUES ('702030', '严正', '男', '计算机科学与技术', '2001-02-13', 50)""",
    "SELECT * FROM Student WHERE 学号='702030'",
    "SELECT * FROM Stu_View WHERE 学号='702030'",
    "SELECT * FROM Stu_View_1 WHERE 学号='702030'"
])

# ========== Q4 ==========
save_sql(4, "在视图Stu_View中将702030的专业改成软件工程", [
    """UPDATE Stu_View SET 专业名 = '软件工程' WHERE 学号 = '702030'""",
    "SELECT * FROM Student WHERE 学号='702030'",
    "SELECT * FROM Stu_View WHERE 学号='702030'"
])

# ========== Q5 ==========
save_sql(5, "在视图Stu_View_1中将所有学生的年龄减10岁", [
    """UPDATE Stu_View_1 SET 年龄 = 年龄 - 10"""
], note="年龄是派生列(DATEDIFF计算结果)，不可更新。此操作会失败。")

# ========== Q6 ==========
save_sql(6, "在表Student中删除题3所添加的学生702030", [
    """DELETE FROM Student WHERE 学号 = '702030'""",
    "SELECT * FROM Student",
    "SELECT * FROM Stu_View"
])

# ========== Q7 ==========
save_sql(7, "在视图Stu_View_1中添加题3的学生信息（年龄不为空）", [
    """INSERT INTO Stu_View_1 (学号, 姓名, 性别, 专业名, 年龄, 总学分)
VALUES ('702030', '严正', '男', '计算机科学与技术', 25, 50)"""
], note="Stu_View_1包含派生列'年龄'，无法通过视图插入数据。此操作会失败。")

# ========== Q8 ==========
save_sql(8, "在视图Stu_View上创建视图Stu_View_2，总学分>=80，WITH CHECK OPTION", [
    """CREATE VIEW Stu_View_2 AS
SELECT 学号, 姓名, 性别, 专业名, 年龄, 总学分
FROM Stu_View_1
WHERE 总学分 >= 80
WITH CHECK OPTION""",
    "SELECT * FROM Stu_View_2"
])

# ========== Q9 ==========
save_sql(9, "在视图Stu_View_2中添加计算机科学与技术的学生702032刘丹女总学分75", [
    """INSERT INTO Stu_View_2 (学号, 姓名, 性别, 专业名, 年龄, 总学分)
VALUES ('702032', '刘丹', '女', '计算机科学与技术', 24, 75)"""
], note="总学分75不满足CHECK OPTION条件(>=80)，插入会失败。")

# ========== Q10 ==========
save_sql(10, "创建基于多个基本表的视图SC_View，显示学号姓名课程号课程名成绩", [
    """CREATE VIEW SC_View AS
SELECT s.学号, s.姓名, sc.课程号, c.课程名, sc.成绩
FROM Student s
JOIN StuCourse sc ON s.学号 = sc.学号
JOIN Course c ON sc.课程号 = c.课程号""",
    "SELECT * FROM SC_View"
])

# ========== Q11 ==========
save_sql(11, "利用视图SC_View完成数据操作", [
    # (1)
    """SELECT 学号, 姓名, ROUND(AVG(成绩), 1) AS 平均成绩
FROM SC_View
GROUP BY 学号, 姓名
HAVING AVG(成绩) > 80
ORDER BY 平均成绩 DESC""",
    # (2) 插入 - 预期失败
    """INSERT INTO SC_View (学号, 课程号) VALUES ('700210', '3001')"""
], note="(1)查询平均成绩>80的学生；(2)通过SC_View插入选课记录，由于SC_View涉及多表连接，预期失败。")

# ========== Q12 ==========
save_sql(12, "建立基于SC_View视图的视图VMgrade，每个学生最高成绩的课程信息", [
    """CREATE VIEW VMgrade AS
SELECT sc.学号, s.姓名, sc.课程号, c.课程名, sc.成绩
FROM StuCourse sc
JOIN Student s ON sc.学号 = s.学号
JOIN Course c ON sc.课程号 = c.课程号
WHERE sc.成绩 = (
    SELECT MAX(sc2.成绩)
    FROM StuCourse sc2
    WHERE sc2.学号 = sc.学号
)""",
    "SELECT * FROM VMgrade",
    # 验证能否更新
    """UPDATE VMgrade SET 成绩 = 100 WHERE 学号 = '070101'"""
], note="VMgrade包含子查询，UPDATE预期会失败或产生意外结果。")

# ========== Q13 ==========
save_sql(13, "创建分组视图CP_View，显示每门课的课程名、选课人数和平均成绩", [
    """CREATE VIEW CP_View AS
SELECT c.课程名,
       COUNT(sc.学号) AS 选课人数,
       AVG(sc.成绩) AS 平均成绩
FROM Course c
LEFT JOIN StuCourse sc ON c.课程号 = sc.课程号
GROUP BY c.课程名""",
    "SELECT * FROM CP_View",
    # 验证能否更新
    "UPDATE CP_View SET 平均成绩 = 90 WHERE 课程名 = '高等数学1'"
], note="CP_View是分组视图，包含聚合函数COUNT/AVG，UPDATE预期失败。")

# ========== Q14 ==========
save_sql(14, "以下数据更新操作在基本表上进行", [
    """INSERT INTO StuCourse (学号, 课程号)
SELECT 学号, '1001'
FROM Student
WHERE 学号 NOT IN (SELECT DISTINCT 学号 FROM StuCourse)""",
    """UPDATE StuCourse
SET 成绩 = 成绩 + 10
WHERE 课程号 = (SELECT 课程号 FROM Course WHERE 课程名 = '高等数学1')
  AND 成绩 = (
      SELECT MIN(sc.成绩)
      FROM StuCourse sc
      JOIN Course c ON sc.课程号 = c.课程号
      WHERE c.课程名 = '高等数学1'
  )""",
    "SELECT * FROM StuCourse ORDER BY 学号, 课程号"
])

conn.close()

# ========== Q15: GoodsOrder ==========
print("\n--- Q15 ---")
conn2 = pyodbc.connect(
    "DRIVER={ODBC Driver 18 for SQL Server};"
    "SERVER=.\\SQLEXPRESS;"
    "DATABASE=GoodsOrder;"
    "Trusted_Connection=yes;"
    "TrustServerCertificate=yes"
)
c2 = conn2.cursor()

# 清理测试索引
try:
    c2.execute("DROP INDEX CustomerInfo.IX_CustomerName_Birth")
except:
    pass

q15_sql = []
q15_sql.append("""-- (1) 查看GoodsInfo表的索引详细信息
EXEC sp_helpindex 'GoodsInfo';""")

q15_sql.append("""-- (2) 验证能否在GoodsInfo表上为商品名称建立聚簇索引
-- 一个表只能有一个聚簇索引，GoodsInfo已有主键聚簇索引，所以会失败
CREATE CLUSTERED INDEX IX_GoodsName ON GoodsInfo(商品名称);""")

q15_sql.append("""-- (3) 在CustomerInfo的客户姓名(升序)、出生日期(降序)上建立唯一非聚簇索引
CREATE UNIQUE NONCLUSTERED INDEX IX_CustomerName_Birth
ON CustomerInfo(客户姓名 ASC, 出生日期 DESC);""")

q15_sql.append("EXEC sp_helpindex 'CustomerInfo';")

sqlpath = os.path.join(OUTDIR, "SQLQuery15.sql")
with open(sqlpath, "w", encoding="utf-8") as f:
    f.write("-- 题目15：索引的创建\n")
    for s in q15_sql:
        f.write(s + "\n")
    f.write("\n\n-- ====== 执行结果 ======\n")
    for s in q15_sql:
        f.write(f"\n{s}\n")
        try:
            c2.execute(s)
            conn2.commit()
            if c2.description:
                rows = c2.fetchall()
                cols = [d[0] for d in c2.description]
                f.write(f"-- 列: {' | '.join(cols)}\n")
                for r in rows:
                    vals = [str(v).strip() if v is not None else "NULL" for v in r]
                    f.write(f"-- {' | '.join(vals)}\n")
            else:
                f.write(f"-- 影响行数: {c2.rowcount}\n")
        except Exception as e:
            f.write(f"-- 执行失败(预期): {e}\n")

conn2.close()
print("Q15 saved")
print("\n=== ALL DONE ===")
print(f"SQL files saved to: {OUTDIR}")
