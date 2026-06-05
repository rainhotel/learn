-- Q1: 创建行列子集视图 Stu_View
CREATE VIEW Stu_View AS
SELECT * FROM Student WHERE 专业名 = '计算机科学与技术';

-- Q2: 在 Stu_View 上创建 Stu_View_1
CREATE VIEW Stu_View_1 AS
SELECT 学号, 姓名, 性别, 专业名, DATEDIFF(YEAR, 出生时间, GETDATE()) AS 年龄, 总学分
FROM Stu_View;

-- Q3: 通过 Stu_View 插入学生 702030 严正
INSERT INTO Stu_View (学号, 姓名, 性别, 专业名, 出生时间, 总学分)
VALUES ('702030', '严正', '男', '计算机科学与技术', '2001-02-13', 50);

-- Q4: 将 702030 的专业改为软件工程
UPDATE Stu_View SET 专业名 = '软件工程' WHERE 学号 = '702030';
