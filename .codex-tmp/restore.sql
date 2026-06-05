RESTORE DATABASE StudentCourse
FROM DISK = N'D:\moniC\project\learn\.codex-tmp\pre_StudentCourse.bak'
WITH REPLACE,
MOVE N'StudentCourse' TO N'C:\Program Files\Microsoft SQL Server\MSSQL16.SQLEXPRESS01\MSSQL\DATA\StudentCourse.mdf',
MOVE N'StudentCourse_log' TO N'C:\Program Files\Microsoft SQL Server\MSSQL16.SQLEXPRESS01\MSSQL\DATA\StudentCourse_log.ldf';
