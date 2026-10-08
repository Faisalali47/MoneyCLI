import sqlite3
import csv
from pathlib import Path
from datetime import datetime

# ========================================================
# CONFIGURATION
# ========================================================

BASE_DIR = Path(__file__).resolve().parent
DB_FILE = BASE_DIR / "money_cli.db"
OUTPUT_FILE = BASE_DIR / "MoneyCLI_export.csv"

# ========================================================
# EXPORT DATABASE
# ========================================================

if not DB_FILE.exists():
    print()
    print("Database not found.")
    print("Run pulldb.bat first.")
    print()
    input("Press Enter to exit...")
    raise SystemExit

try:
    connection = sqlite3.connect(DB_FILE)
    cursor = connection.cursor()

    cursor.execute("""
        SELECT
            id,
            type,
            amount,
            description,
            wallet,
            source_wallet,
            destination_wallet,
            date
        FROM transactions
        ORDER BY id ASC
    """)

    rows = cursor.fetchall()

    with open(
        OUTPUT_FILE,
        "w",
        newline="",
        encoding="utf-8-sig"
    ) as file:

        writer = csv.writer(file)

        writer.writerow([
            "ID",
            "Type",
            "Amount",
            "Description",
            "Wallet",
            "Source Wallet",
            "Destination Wallet",
            "Date"
        ])

        writer.writerows(rows)

    connection.close()

    print()
    print("CSV export successful.")
    print()
    print(f"Transactions : {len(rows)}")
    print(f"Output       : {OUTPUT_FILE}")
    print()

except Exception as error:
    print()
    print("Failed to export database.")
    print()
    print(f"Error: {error}")
    print()

input("Press Enter to exit...")