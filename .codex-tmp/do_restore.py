import pyodbc
conn = pyodbc.connect(
    'DRIVER={ODBC Driver 18 for SQL Server};'
    'SERVER=.\\SQLEXPRESS01;'
    'Trusted_Connection=yes;'
    'TrustServerCertificate=yes'
)
conn.autocommit = True
c = conn.cursor()

try:
    c.execute("ALTER DATABASE StudentCourse SET SINGLE_USER WITH ROLLBACK IMMEDIATE")
    c.execute("DROP DATABASE StudentCourse")
    print("Dropped existing StudentCourse")
except Exception:
    print("No existing StudentCourse")

bak = r"D:\moniC\project\learn\.codex-tmp\pre_StudentCourse.bak"
mdf = r"C:\Program Files\Microsoft SQL Server\MSSQL16.SQLEXPRESS01\MSSQL\DATA\StudentCourse.mdf"
ldf = r"C:\Program Files\Microsoft SQL Server\MSSQL16.SQLEXPRESS01\MSSQL\DATA\StudentCourse_log.ldf"

sql = f"""
RESTORE DATABASE StudentCourse
FROM DISK = N'{bak}'
WITH REPLACE,
MOVE N'StudentCourse' TO N'{mdf}',
MOVE N'StudentCourse_log' TO N'{ldf}'
"""
c.execute(sql)
print("RESTORE SUCCESS!")
conn.close()
