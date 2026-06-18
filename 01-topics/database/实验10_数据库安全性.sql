-- ============================================================
-- 实验10 数据库安全性（题目4：参考教材7.2.4小节，完成实验10）
-- 实验目的：
--   加深对数据库安全性的理解
--   掌握 SQL Server 中登录、用户和权限的管理方法
-- 姓名：周可名
-- ============================================================

-- ============================================================
-- 前置步骤（需手动操作）：
-- 1. 在 SSMS 中将 SQL Server 身份验证模式设为
--    "SQL Server 和 Windows 身份验证模式"
--    （右键服务器 → 属性 → 安全性 → 选择混合模式 → 重启服务）
-- 2. 确保 StudentCourse 数据库已存在
-- ============================================================

USE StudentCourse
GO

-- ============================================================
-- 步骤1：创建两个登录名 sutdLogin 和 sutdLogin2
-- ============================================================

-- 创建登录名 sutdLogin（SQL Server 身份验证）
IF NOT EXISTS (SELECT name FROM sys.server_principals WHERE name = 'sutdLogin')
BEGIN
    CREATE LOGIN sutdLogin WITH PASSWORD = '123'
    PRINT '登录名 sutdLogin 创建成功。'
END
ELSE
    PRINT '登录名 sutdLogin 已存在，跳过创建。'
GO

-- 创建登录名 sutdLogin2
IF NOT EXISTS (SELECT name FROM sys.server_principals WHERE name = 'sutdLogin2')
BEGIN
    CREATE LOGIN sutdLogin2 WITH PASSWORD = '123'
    PRINT '登录名 sutdLogin2 创建成功。'
END
ELSE
    PRINT '登录名 sutdLogin2 已存在，跳过创建。'
GO

-- ============================================================
-- 步骤2：映射数据库用户 sutdUsr 和 sutdUsr2
-- ============================================================
IF NOT EXISTS (SELECT name FROM sys.database_principals WHERE name = 'sutdUsr')
BEGIN
    CREATE USER sutdUsr FOR LOGIN sutdLogin
    PRINT '数据库用户 sutdUsr 创建成功。'
END
ELSE
    PRINT '数据库用户 sutdUsr 已存在，跳过创建。'
GO

IF NOT EXISTS (SELECT name FROM sys.database_principals WHERE name = 'sutdUsr2')
BEGIN
    CREATE USER sutdUsr2 FOR LOGIN sutdLogin2
    PRINT '数据库用户 sutdUsr2 创建成功。'
END
ELSE
    PRINT '数据库用户 sutdUsr2 已存在，跳过创建。'
GO

-- ============================================================
-- 步骤3：授予 SELECT 权限，验证授权
-- ============================================================
GRANT SELECT ON Student TO sutdUsr, sutdUsr2
GO
PRINT '已授予 sutdUsr 和 sutdUsr2 对 Student 表的 SELECT 权限。'
GO

-- 验证：以 sutdUsr 身份查询 Student 表
PRINT '--- 以 sutdUsr 身份验证 SELECT 权限 ---'
EXECUTE AS USER = 'sutdUsr'
SELECT * FROM Student
REVERT
GO

-- ============================================================
-- 步骤4：将 INSERT 权限授予 sutdUsr，并允许传递
-- ============================================================
GRANT INSERT ON Student TO sutdUsr WITH GRANT OPTION
GO
PRINT '已授予 sutdUsr 对 Student 表的 INSERT 权限（含 GRANT OPTION）。'
GO

-- ============================================================
-- 步骤5：sutdUsr 将 INSERT 权限再授予 sutdUsr2
-- ============================================================
EXECUTE AS USER = 'sutdUsr'
GRANT INSERT ON Student TO sutdUsr2
REVERT
GO
PRINT 'sutdUsr 已将 INSERT 权限授予 sutdUsr2。'
GO

-- ============================================================
-- 步骤6：以 sutdUsr2 身份插入数据，验证权限传递
-- ============================================================
PRINT '--- 以 sutdUsr2 身份插入数据（应成功）---'
EXECUTE AS USER = 'sutdUsr2'
INSERT INTO Student(学号, 姓名, 性别, 出生时间, 总学分, 备注)
VALUES ('070115', '李小明', '女', getdate(), 10, '三好学生')
REVERT
GO

-- 查看插入结果
SELECT * FROM Student WHERE 学号 = '070115'
GO

-- ============================================================
-- 步骤7：回收 INSERT 权限（CASCADE 级联回收）
-- ============================================================
PRINT '--- CASCADE 级联回收 INSERT 权限 ---'
REVOKE INSERT ON Student FROM sutdUsr CASCADE
GO

-- 验证：sutdUsr2 再尝试插入（应失败）
PRINT '--- 以 sutdUsr2 再次插入（应失败，权限已回收）---'
EXECUTE AS USER = 'sutdUsr2'
INSERT INTO Student(学号, 姓名, 性别, 出生时间, 总学分, 备注)
VALUES ('070116', '张测试', '男', getdate(), 10, '测试')
REVERT
GO

-- 清理测试数据
DELETE FROM Student WHERE 学号 = '070115'
GO

-- ============================================================
-- 题目5：使用SQL语句完成思考与练习（级联回收）
-- ============================================================
PRINT ''
PRINT '========== 思考与练习 =========='

-- 练习1：将 Student 表的 UPDATE、DELETE 权限授予 sutdUsr（含 GRANT OPTION）
PRINT '练习1：授予 UPDATE、DELETE 权限给 sutdUsr'
GRANT UPDATE, DELETE ON Student TO sutdUsr WITH GRANT OPTION
GO

-- 验证：sutdUsr 可以修改数据
EXECUTE AS USER = 'sutdUsr'
UPDATE Student SET 备注 = '权限测试' WHERE 学号 = '070110'
REVERT
GO
SELECT 学号, 姓名, 备注 FROM Student WHERE 学号 = '070110'
GO
-- 恢复
UPDATE Student SET 备注 = NULL WHERE 学号 = '070110'
GO

-- 练习2：由 sutdUsr 将 UPDATE、DELETE 权限再授予 sutdUsr2
PRINT '练习2：sutdUsr 将权限传递给 sutdUsr2'
EXECUTE AS USER = 'sutdUsr'
GRANT UPDATE, DELETE ON Student TO sutdUsr2
REVERT
GO

-- 验证：sutdUsr2 可以修改数据
EXECUTE AS USER = 'sutdUsr2'
UPDATE Student SET 备注 = '传递权限测试' WHERE 学号 = '070110'
REVERT
GO
SELECT 学号, 姓名, 备注 FROM Student WHERE 学号 = '070110'
GO
-- 恢复
UPDATE Student SET 备注 = NULL WHERE 学号 = '070110'
GO

-- 练习3：CASCADE 级联回收 UPDATE、DELETE 权限
PRINT '练习3：CASCADE 级联回收 UPDATE、DELETE 权限'
REVOKE UPDATE, DELETE ON Student FROM sutdUsr CASCADE
GO

-- 验证：sutdUsr2 再尝试修改（应失败）
PRINT '--- sutdUsr2 再尝试修改（应失败）---'
EXECUTE AS USER = 'sutdUsr2'
UPDATE Student SET 备注 = '应失败' WHERE 学号 = '070110'
REVERT
GO

-- ============================================================
-- 权限分配情况查询
-- ============================================================
SELECT
    princ.name AS 用户名,
    perm.permission_name AS 权限,
    perm.state_desc AS 状态
FROM sys.database_permissions perm
JOIN sys.database_principals princ ON perm.grantee_principal_id = princ.principal_id
WHERE princ.name IN ('sutdUsr', 'sutdUsr2')
ORDER BY princ.name, perm.permission_name
GO

-- 清理：删除测试用户和登录名
-- DROP USER sutdUsr
-- DROP USER sutdUsr2
-- DROP LOGIN sutdLogin
-- DROP LOGIN sutdLogin2
GO
