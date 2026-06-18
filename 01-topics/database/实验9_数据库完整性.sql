-- ============================================================
-- 实验9 数据库完整性 + 实验10 数据库安全性
-- 实验名称：南京信息工程大学 数据库系统 实验(实习)报告
-- 实验目的：
--   1. 掌握 SQL Server 的6类约束(NOT NULL、PRIMARY KEY、CHECK、
--      FOREIGN KEY、DEFAULT、UNIQUE)的使用方法，在创建表时用相应
--      约束描述实体完整性、参照完整性和用户定义完整性
--   2. 掌握增加和删除约束的方法
--   3. 理解登录账号和数据库用户及其关系
--   4. 能够创建登录账号、创建数据库用户
--   5. 能够为数据库用户分配权限
-- 姓名：周可名
-- ============================================================

-- ============================================================
-- 题目1：创建数据库 StudentCourse1 及 3 个表
-- ============================================================

-- 1.1 创建数据库
IF EXISTS (SELECT name FROM sys.databases WHERE name = 'StudentCourse1')
    DROP DATABASE StudentCourse1
GO

CREATE DATABASE StudentCourse1
GO

USE StudentCourse1
GO

-- 1.2 创建表 A9.1 学生情况表 Student1
CREATE TABLE Student1 (
    学号     CHAR(6)         PRIMARY KEY,
    姓名     VARCHAR(20)     NOT NULL CONSTRAINT sname_unique UNIQUE,
    性别     CHAR(2)         NOT NULL CONSTRAINT ssex_Check CHECK (性别 = '男' OR 性别 = '女'),
    出生时间  SMALLDATETIME   NULL,
    总学分    INT             CONSTRAINT tot_Credit_Check CHECK (总学分 > 0),
    备注     TEXT            NULL
)
GO

-- 为"性别"列添加默认值约束 '男'
ALTER TABLE Student1 ADD CONSTRAINT DF_Student1_性别 DEFAULT '男' FOR 性别
GO

-- 1.3 创建表 A9.2 课程表 Course
CREATE TABLE Course (
    课程号   CHAR(4)         PRIMARY KEY,
    课程名   VARCHAR(40)     NOT NULL,
    开课学期  INT             NOT NULL CONSTRAINT Sem_Check CHECK (开课学期 BETWEEN 1 AND 8),
    学时     INT             NOT NULL CONSTRAINT Tn_Check CHECK (学时 > 0),
    学分     REAL            NOT NULL CONSTRAINT Cno_Check CHECK (学分 > 0)
)
GO

-- 1.4 创建表 A9.3 学生选课表 StuCourse
CREATE TABLE StuCourse (
    学号     CHAR(6)         NOT NULL,
    课程号   CHAR(4)         NOT NULL,
    成绩     INT             NULL CONSTRAINT Score_Check CHECK (成绩 BETWEEN 0 AND 100),
    CONSTRAINT PK_StuCourse PRIMARY KEY (学号, 课程号),
    CONSTRAINT FK_StuCourse_Student1 FOREIGN KEY (学号) REFERENCES Student1(学号),
    CONSTRAINT FK_StuCourse_Course   FOREIGN KEY (课程号) REFERENCES Course(课程号)
)
GO

-- ============================================================
-- 题目2：修改约束定义
-- 将 Course 表的 Tn_Check 约束修改为：学时 >= 0 且 <= 120
--（docx中写为Tm_Check，实际建表时约束名为Tn_Check）
-- ============================================================

-- 查看当前约束
EXEC sp_helpconstraint 'Course'
GO

-- 删除旧约束，添加新约束
ALTER TABLE Course DROP CONSTRAINT Tn_Check
GO

ALTER TABLE Course ADD CONSTRAINT Tn_Check CHECK (学时 >= 0 AND 学时 <= 120)
GO

-- 验证：插入学时=0的记录（新约束允许）
PRINT '--- 验证学时=0（新约束允许）---'
INSERT INTO Course (课程号, 课程名, 开课学期, 学时, 学分)
VALUES ('C001', '测试课程', 1, 0, 2)
GO

-- 验证：插入学时=121的记录（应失败）
PRINT '--- 验证学时=121（应失败，超出0~120范围）---'
INSERT INTO Course (课程号, 课程名, 开课学期, 学时, 学分)
VALUES ('C002', '测试课程2', 1, 121, 2)
GO

-- 清理测试数据
DELETE FROM Course WHERE 课程号 = 'C001'
GO

-- ============================================================
-- 插入基础测试数据（满足后续测试的外键要求）
-- ============================================================
INSERT INTO Student1 (学号, 姓名, 性别, 出生时间, 总学分)
VALUES ('070110', '测试学生', '男', '2000-01-01', 10)
GO

INSERT INTO Course (课程号, 课程名, 开课学期, 学时, 学分)
VALUES ('2001', '数据库原理', 3, 64, 4)
GO

INSERT INTO StuCourse VALUES ('070110', '2001', 70)
GO

-- ============================================================
-- 题目3：对6类数据约束进行验证
-- ============================================================

PRINT ''
PRINT '========== 六类约束验证 =========='

-- (1) PRIMARY KEY 约束验证
PRINT '--- (1) PRIMARY KEY：插入重复学号（应失败）---'
INSERT INTO Student1 (学号, 姓名, 性别, 出生时间, 总学分)
VALUES ('070110', '重复学号', '女', '2000-02-02', 20)
GO

-- (2) FOREIGN KEY 约束验证
PRINT '--- (2) FOREIGN KEY：插入不存在的学号（应失败）---'
INSERT INTO StuCourse VALUES ('999999', '2001', 80)
GO

PRINT '--- FOREIGN KEY：插入不存在的课程号（应失败）---'
INSERT INTO StuCourse VALUES ('070110', '9999', 80)
GO

-- (3) NOT NULL 约束验证
PRINT '--- (3) NOT NULL：插入姓名=NULL（应失败）---'
INSERT INTO Student1 (学号, 姓名, 性别, 出生时间, 总学分)
VALUES ('070111', NULL, '男', '2000-01-01', 10)
GO

-- (4) UNIQUE 约束验证
PRINT '--- (4) UNIQUE：插入重复姓名（应失败）---'
INSERT INTO Student1 (学号, 姓名, 性别, 出生时间, 总学分)
VALUES ('070112', '测试学生', '男', '2000-03-03', 15)
GO

-- (5) DEFAULT 约束验证
PRINT '--- (5) DEFAULT：不指定性别，应默认"男"---'
INSERT INTO Student1 (学号, 姓名, 出生时间, 总学分)
VALUES ('070113', '默认性别测试', '2000-04-04', 12)
GO
SELECT 学号, 姓名, 性别 FROM Student1 WHERE 学号 = '070113'
GO
-- 清理
DELETE FROM Student1 WHERE 学号 = '070113'
GO

-- (6) CHECK 约束验证
PRINT '--- (6) CHECK：插入总学分<=0（应失败）---'
INSERT INTO Student1 (学号, 姓名, 性别, 出生时间, 总学分)
VALUES ('070114', '负学分测试', '男', '2000-05-05', -5)
GO

PRINT '--- CHECK：插入成绩=101（应失败，范围0~100）---'
INSERT INTO StuCourse VALUES ('070110', '2001', 101)
GO

PRINT '--- CHECK：插入性别为"未知"（应失败）---'
INSERT INTO Student1 (学号, 姓名, 性别, 出生时间, 总学分)
VALUES ('070115', '性别测试', '未知', '2000-06-06', 10)
GO

-- ============================================================
-- 触发器测试（教材附录实验8 内容3 相关内容）
-- ============================================================

-- 删除已存在的触发器
IF EXISTS (SELECT name FROM sysobjects WHERE name = 'SC_trg' AND type = 'TR')
    DROP TRIGGER SC_trg
GO

PRINT '--- 创建触发器 SC_trg ---'
CREATE TRIGGER SC_trg ON StuCourse
FOR INSERT
AS
    IF (SELECT 学号 FROM inserted) IN (SELECT 学号 FROM Student1)
    BEGIN
        RAISERROR ('插入操作违背数据的一致性。', 16, 1)
        ROLLBACK TRANSACTION
    END
GO

-- 测试触发器（应被拒绝并回滚）
PRINT '--- 测试触发器：插入学号已存在的选课记录（应失败）---'
INSERT INTO StuCourse VALUES ('070110', '2001', 80)
GO

-- 查看最终数据
SELECT * FROM StuCourse
GO

-- ============================================================
-- 题目2（思考与练习1和2）：记录运行结果并分析
-- ============================================================
PRINT ''
PRINT '========== 思考与练习 =========='
PRINT '练习1：记录上述各约束验证语句的运行结果。'
PRINT '  - PRIMARY KEY: 违反主键约束，插入失败'
PRINT '  - FOREIGN KEY: 违反外键约束，插入失败'
PRINT '  - NOT NULL: 不允许NULL值，插入失败'
PRINT '  - UNIQUE: 违反唯一性约束，插入失败'
PRINT '  - DEFAULT: 未指定性别时自动填入"男"'
PRINT '  - CHECK: 违反CHECK约束，插入失败'
PRINT ''
PRINT '练习2：分析触发器SC_trg的运行结果。'
PRINT '  第一次INSERT成功（触发器尚未创建）；'
PRINT '  创建触发器后，学号"070110"已存在于Student1表，'
PRINT '  触发器检测到后通过RAISERROR报错并ROLLBACK回滚。'
GO
