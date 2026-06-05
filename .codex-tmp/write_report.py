"""为实验4生成完整的 docx 报告"""
import os, json, sys
sys.path.insert(0, "skills/db-experiment-report/scripts")
from generate_docx import generate

OUTDIR = r"D:\xwechat_files\wxid_0vnbq4ga2qso22_630f\msg\file\2026-06"
TEMPLATE = os.path.join(OUTDIR, "实验4.docx")

questions = []

questions.append({
    "num": 1, "keyword": "创建一个行列子集视图Stu _View",
    "has_placeholders": True,
    "sql": "CREATE VIEW Stu_View AS\nSELECT * FROM Student WHERE 专业名 = '计算机科学与技术'",
    "result": "视图创建成功。Stu_View包含4条记录（计算机科学与技术专业学生）。"
})

questions.append({
    "num": 2, "keyword": "在视图Stu _View上创建视图Stu _View_1",
    "sql": "CREATE VIEW Stu_View_1 AS\nSELECT 学号, 姓名, 性别, 专业名,\n       DATEDIFF(YEAR, 出生日期, GETDATE()) AS 年龄,\n       总学分\nFROM Stu_View",
    "result": "视图创建成功。Stu_View_1（4行）：\n070101 | 陈明 | 男 | 计算机科学与技术 | 26 | 85\n070102 | 李丽 | 女 | 计算机科学与技术 | 26 | 90\n070103 | 王强 | 男 | 计算机科学与技术 | 26 | 78\n700210 | 周明 | 男 | 计算机科学与技术 | 25 | 65"
})

questions.append({
    "num": 3, "keyword": "在视图Stu _View中添加一个计算机科学与技术的学生信息",
    "sql": "INSERT INTO Stu_View (学号, 姓名, 性别, 专业名, 出生日期, 总学分)\nVALUES ('702030', '严正', '男', '计算机科学与技术', '2001-02-13', 50)",
    "result": "插入成功。基本表Student中新增了学号702030的学生。\n视图Stu_View和Stu_View_1中也自动出现该学生信息。"
})

questions.append({
    "num": 4, "keyword": "在视图Stu _View中将上题新添加的学生专业改成",
    "sql": "UPDATE Stu_View SET 专业名 = '软件工程' WHERE 学号 = '702030'",
    "result": "更新成功。基本表Student中702030的专业名改为软件工程。\n由于专业不再是计算机科学与技术，702030从视图Stu_View和Stu_View_1中消失。"
})

questions.append({
    "num": 5, "keyword": "在视图Stu _View_1中将所有学生的年龄减10岁",
    "sql": "UPDATE Stu_View_1 SET 年龄 = 年龄 - 10",
    "result": "执行失败。原因：年龄是派生列（DATEDIFF计算），不属于基表实际列，无法更新。"
})

questions.append({
    "num": 6, "keyword": "在表Student中删除题3所添加的学生信息",
    "sql": "DELETE FROM Student WHERE 学号 = '702030'",
    "result": "删除成功。基本表Student中702030记录被永久删除。\n视图Stu_View记录数恢复为4条。基本表的删除自动反映到视图中。"
})

questions.append({
    "num": 7, "keyword": "在视图Stu _View_1中添加题3的学生信息",
    "sql": "INSERT INTO Stu_View_1 (学号, 姓名, 性别, 专业名, 年龄, 总学分)\nVALUES ('702030', '严正', '男', '计算机科学与技术', 25, 50)",
    "result": "执行失败。原因：Stu_View_1包含派生列，系统无法将INSERT映射到基表。"
})

questions.append({
    "num": 8, "keyword": "在视图Stu _View上创建视图Stu _View_2",
    "sql": "CREATE VIEW Stu_View_2 AS\nSELECT 学号, 姓名, 性别, 专业名, 年龄, 总学分\nFROM Stu_View_1\nWHERE 总学分 >= 80\nWITH CHECK OPTION",
    "result": "创建成功。Stu_View_2（2行）：\n070101 | 陈明 | 26 | 85\n070102 | 李丽 | 26 | 90"
})

questions.append({
    "num": 9, "keyword": "在视图Stu _View_2中添加",
    "sql": "INSERT INTO Stu_View_2 (学号, 姓名, 性别, 专业名, 年龄, 总学分)\nVALUES ('702032', '刘丹', '女', '计算机科学与技术', 24, 75)",
    "result": "执行失败。原因：总学分75不满足WITH CHECK OPTION的>=80条件，操作被拒绝。"
})

questions.append({
    "num": 10, "keyword": "创建基于多个基本表的视图SC_View",
    "sql": "CREATE VIEW SC_View AS\nSELECT s.学号, s.姓名, sc.课程号, c.课程名, sc.成绩\nFROM Student s\nJOIN StuCourse sc ON s.学号 = sc.学号\nJOIN Course c ON sc.课程号 = c.课程号",
    "result": "创建成功。SC_View共12行，显示每个学生的选课及成绩。部分数据：\n070101 | 陈明 | 1001 | 高等数学1 | 85\n070101 | 陈明 | 3001 | 数据库系统 | 92\n070102 | 李丽 | 1001 | 高等数学1 | 88"
})

questions.append({
    "num": 11, "keyword": "利用视图SC_View完成如下数据操作",
    "sql": "-- (1) 查询平均成绩>80的学生\nSELECT 学号, 姓名, ROUND(AVG(成绩), 1) AS 平均成绩\nFROM SC_View\nGROUP BY 学号, 姓名\nHAVING AVG(成绩) > 80\nORDER BY 平均成绩 DESC\n\n-- (2) 插入选课记录\nINSERT INTO SC_View (学号, 课程号) VALUES ('700210', '3001')",
    "result": "（1）查询结果（4人）：李丽88.3, 陈明87.7, 孙伟85.0, 赵芳83.0\n（2）插入失败。原因：SC_View是多表连接视图，无法确定写入哪个基表。"
})

questions.append({
    "num": 12, "keyword": "建立基于SC_View视图的视图VMgrade",
    "sql": "CREATE VIEW VMgrade AS\nSELECT sc.学号, s.姓名, sc.课程号, c.课程名, sc.成绩\nFROM StuCourse sc\nJOIN Student s ON sc.学号 = s.学号\nJOIN Course c ON sc.课程号 = c.课程号\nWHERE sc.成绩 = (\n    SELECT MAX(sc2.成绩) FROM StuCourse sc2 WHERE sc2.学号 = sc.学号\n)\n\nUPDATE VMgrade SET 成绩 = 100 WHERE 学号 = '070101'",
    "result": "VMgrade创建成功（6行），每个学生的最高成绩课程。\nUPDATE失败。原因：VMgrade包含子查询，属不可更新视图。"
})

questions.append({
    "num": 13, "keyword": "创建分组视图CP_View",
    "sql": "CREATE VIEW CP_View AS\nSELECT c.课程名, COUNT(sc.学号) AS 选课人数, AVG(sc.成绩) AS 平均成绩\nFROM Course c\nLEFT JOIN StuCourse sc ON c.课程号 = sc.课程号\nGROUP BY c.课程名\n\nUPDATE CP_View SET 平均成绩 = 90 WHERE 课程名 = '高等数学1'",
    "result": "CP_View创建成功（6行）：\n高等数学1 | 7 | 77.1, 数据结构 | 2 | 73.0, 操作系统 | 2 | 83.5\n数据库系统 | 3 | 87.7, 高等数学2、计算机网络选课0人。\nUPDATE失败。原因：分组视图含GROUP BY和聚合函数，不可更新。"
})

questions.append({
    "num": 14, "keyword": "以下数据更新操作在基本表上进行",
    "sql": "-- (1) 无选课学生增加1001课程\nINSERT INTO StuCourse (学号, 课程号)\nSELECT 学号, '1001' FROM Student\nWHERE 学号 NOT IN (SELECT DISTINCT 学号 FROM StuCourse)\n\n-- (2) 高等数学1最低分+10\nUPDATE StuCourse SET 成绩 = 成绩 + 10\nWHERE 课程号 = (SELECT 课程号 FROM Course WHERE 课程名 = '高等数学1')\n  AND 成绩 = (SELECT MIN(sc.成绩) FROM StuCourse sc\n    JOIN Course c ON sc.课程号 = c.课程号 WHERE c.课程名 = '高等数学1')",
    "result": "（1）成功。080101和080102添加了1001课程（成绩NULL）。\n（2）成功。700210的55分更新为65分。"
})

questions.append({
    "num": 15, "keyword": "索引的创建",
    "sql": "-- (1) 查看索引\nEXEC sp_helpindex 'GoodsInfo'\n\n-- (2) 尝试建聚簇索引（会失败）\nCREATE CLUSTERED INDEX IX_GoodsName ON GoodsInfo(商品名称)\n\n-- (3) 建立唯一非聚簇索引\nCREATE UNIQUE NONCLUSTERED INDEX IX_CustomerName_Birth\nON CustomerInfo(客户姓名 ASC, 出生日期 DESC)",
    "result": "（1）GoodsInfo已有1个聚簇索引（主键PK__GoodsInfo__...）。\n（2）失败。原因：一个表只能有一个聚簇索引。\n（3）唯一非聚簇索引创建成功。索引键：(客户姓名 ASC, 出生日期 DESC)。"
})

summary = "通过本次实验，我系统掌握了视图的创建、查询与更新操作。实验验证了多种视图类型的特性：（1）行列子集视图（Stu_View）支持INSERT/UPDATE/DELETE，操作会自动反映到基表；（2）包含派生列的视图（Stu_View_1）不能进行INSERT和UPDATE操作；（3）WITH CHECK OPTION（Stu_View_2）能有效约束通过视图的数据修改，不满足条件的操作会被拒绝；（4）基于多表连接的视图（SC_View）不能直接用于INSERT操作；（5）包含子查询（VMgrade）或分组聚合（CP_View）的视图属于不可更新视图。在索引实验中，我明确了聚簇索引的唯一性限制——一个表只能有一个聚簇索引，并成功在CustomerInfo表上创建了组合唯一非聚簇索引。本实验使我对SQL Server视图机制和索引原理有了更深入的理解。"

queries_data = {"questions": questions, "summary": summary}

json_path = os.path.join(OUTDIR, "queries_data.json")
with open(json_path, "w", encoding="utf-8") as f:
    json.dump(queries_data, f, ensure_ascii=False, indent=2)

output_path = os.path.join(OUTDIR, "实验4_已完成.docx")
generate(TEMPLATE, queries_data, output_path)
print(f"Report saved to: {output_path}")
