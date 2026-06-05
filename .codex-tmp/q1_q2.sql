-- Q1: 创建行列子集视图 Stu_View
CREATE VIEW Stu_View AS
SELECT * FROM Student WHERE 专业名 = '计算机科学与技术';

-- Q2: 在 Stu_View 上创建 Stu_View_1
CREATE VIEW Stu_View_1 AS
SELECT 学号, 姓名, 性别, 专业名, DATEDIFF(YEAR, 出生时间, GETDATE()) AS 年龄, 总学分
FROM Stu_View;
