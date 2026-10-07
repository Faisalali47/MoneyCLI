import sqlite3

db = sqlite3.connect("money_cli.db")
cursor = db.cursor()

cursor.execute("""
    SELECT id, type, amount, description, date
    FROM transactions
""")

rows = cursor.fetchall()

print()
print(f"{'ID':<5} {'TYPE':<10} {'AMOUNT':<12} {'DESCRIPTION':<20} {'DATE'}")
print("-" * 80)

for row in rows:
    print(
        f"{row[0]:<5} "
        f"{row[1]:<10} "
        f"Rp{row[2]:<10} "
        f"{row[3]:<20} "
        f"{row[4]}"
    )

print()

db.close()