"""导出 SQL Server 数据库为 SQL 脚本，发给同学用"""
import pyodbc
import sys

SERVER = r".\SQLEXPRESS01"  # 同学改这里
DATABASE = "StudentCourse"
OUTPUT = "StudentCourse_export.sql"


def export():
    conn = pyodbc.connect(
        f"DRIVER={{ODBC Driver 18 for SQL Server}};"
        f"SERVER={SERVER};"
        f"DATABASE={DATABASE};"
        f"Trusted_Connection=yes;"
        f"TrustServerCertificate=yes"
    )
    c = conn.cursor()

    with open(OUTPUT, "w", encoding="utf-8") as f:
        f.write(f"-- Exported from: {SERVER}.{DATABASE}\n")
        f.write(f"USE {DATABASE};\nGO\n\n")

        # 获取所有表
        c.execute(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
            "WHERE TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME"
        )
        tables = [r[0] for r in c.fetchall()]

        for table in tables:
            print(f"Exporting {table}...")

            # 导出表结构
            c.execute(f"""
                SELECT COLUMN_NAME, DATA_TYPE,
                    CHARACTER_MAXIMUM_LENGTH,
                    IS_NULLABLE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME='{table}'
                ORDER BY ORDINAL_POSITION
            """)
            cols_info = c.fetchall()
            col_names = [ci[0] for ci in cols_info]

            # 导出所有数据行
            c.execute(f"SELECT * FROM [{table}]")
            rows = c.fetchall()

            if rows:
                f.write(f"-- {table} ({len(rows)} rows)\n")
                for row in rows:
                    vals = []
                    for i, v in enumerate(row):
                        if v is None:
                            vals.append("NULL")
                        elif isinstance(v, (int, float)):
                            vals.append(str(v))
                        elif isinstance(v, bool):
                            vals.append("1" if v else "0")
                        elif isinstance(v, bytes):
                            vals.append(f"0x{v.hex()}")
                        else:
                            s = str(v).replace("'", "''")
                            vals.append(f"'{s}'")
                    f.write(
                        f"INSERT INTO [{table}] "
                        f"({', '.join(f'[{n}]' for n in col_names)}) "
                        f"VALUES ({', '.join(vals)});\n"
                    )
                f.write("\n")
            else:
                f.write(f"-- {table}: empty\n\n")

    conn.close()
    print(f"\nDone! Saved to {OUTPUT}")


if __name__ == "__main__":
    if len(sys.argv) > 1:
        SERVER = sys.argv[1]
    if len(sys.argv) > 2:
        DATABASE = sys.argv[2]
    export()
