"""
Connect to SQL Server, discover databases, explore table schemas and data.

Usage:
  python explore_db.py <host> [database]

If database is omitted, lists all databases first.
Outputs schema and data summary to stdout in a structured format.

Safety note: All SQL operations are allowed. The user must confirm
a full database backup before this script is called (enforced by the
skill workflow, not by this script).
"""
import sys
import pyodbc
import os


def connect(host: str, database: str = None):
    """Connect to SQL Server with Windows authentication."""
    conn_str = (
        f"DRIVER={{ODBC Driver 18 for SQL Server}};"
        f"SERVER={host};"
        f"Trusted_Connection=yes;"
        f"TrustServerCertificate=yes;"
    )
    if database:
        conn_str += f"DATABASE={database};"
    return pyodbc.connect(conn_str)


def list_databases(host: str):
    """List all user databases on the server."""
    conn = connect(host)
    cursor = conn.cursor()
    cursor.execute("SELECT name FROM sys.databases ORDER BY name")
    dbs = [row[0] for row in cursor.fetchall()]
    conn.close()
    return dbs


def explore_database(host: str, database: str):
    """Explore all tables in a database and their data."""
    conn = connect(host, database)
    cursor = conn.cursor()

    # Get tables
    cursor.execute(
        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME")
    tables = [row[0] for row in cursor.fetchall()]

    output = []
    output.append(f"Database: {database}")
    output.append(f"Tables: {len(tables)}")
    output.append("=" * 60)

    for table in tables:
        output.append(f"\n## {table}")
        output.append("-" * 40)

        # Columns
        cursor.execute(
            f"SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH "
            f"FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='{table}' ORDER BY ORDINAL_POSITION")
        cols_info = cursor.fetchall()
        output.append("Columns:")
        for col in cols_info:
            output.append(f"  {col[0]} ({col[1]})")

        # Column names for data display
        col_names = [col[0] for col in cols_info]

        # All data
        cursor.execute( f"SELECT * FROM [{table}]")
        rows = cursor.fetchall()
        output.append(f"\nData ({len(rows)} rows):")
        output.append("  " + " | ".join(col_names))
        output.append("  " + "-" * 40)
        for row in rows:
            values = []
            for v in row:
                if v is None:
                    values.append("NULL")
                elif isinstance(v, float):
                    values.append(f"{v:.2f}")
                else:
                    values.append(str(v).strip())
            output.append("  " + " | ".join(values))

    conn.close()
    return "\n".join(output)


def main():
    if len(sys.argv) < 2:
        print("Usage: python explore_db.py <host> [database]")
        sys.exit(1)

    host = sys.argv[1]

    if len(sys.argv) >= 3:
        database = sys.argv[2]
        print(explore_database(host, database))
    else:
        print("=== Available Databases ===")
        for db in list_databases(host):
            print(f"  {db}")


if __name__ == "__main__":
    main()
