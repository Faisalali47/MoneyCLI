package com.moneycli.finance

import android.content.ContentValues
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceCore(context: Context) {

    private val dbHelper = TransactionDbHelper(context)

    // ========================================================
    // INCOME
    // ========================================================

    fun addIncome(
        amount: Long,
        description: String,
        wallet: Wallet
    ): Transaction {

        validateAmount(amount)
        validateDescription(description)

        val db = dbHelper.writableDatabase
        val date = getCurrentDate()

        val values = ContentValues().apply {
            put("type", Transaction.Type.INCOME.name)
            put("amount", amount)
            put("description", description)
            put("wallet", wallet.name)
            putNull("source_wallet")
            putNull("destination_wallet")
            put("date", date)
        }

        val id = db.insert("transactions", null, values)

        if (id == -1L) {
            throw IllegalStateException("Failed to save transaction")
        }

        return Transaction(
            id = id.toInt(),
            type = Transaction.Type.INCOME,
            amount = amount,
            description = description,
            wallet = wallet,
            sourceWallet = null,
            destinationWallet = null,
            date = date
        )
    }

    // ========================================================
    // EXPENSE
    // ========================================================

    fun addExpense(
        amount: Long,
        description: String,
        wallet: Wallet
    ): Transaction {

        validateAmount(amount)
        validateDescription(description)

        val currentBalance = getBalance(wallet)

        if (amount > currentBalance) {
            throw IllegalArgumentException(
                "Insufficient balance in ${wallet.name}. " +
                    "Available balance: Rp$currentBalance"
            )
        }

        val db = dbHelper.writableDatabase
        val date = getCurrentDate()

        val values = ContentValues().apply {
            put("type", Transaction.Type.EXPENSE.name)
            put("amount", amount)
            put("description", description)
            put("wallet", wallet.name)
            putNull("source_wallet")
            putNull("destination_wallet")
            put("date", date)
        }

        val id = db.insert("transactions", null, values)

        if (id == -1L) {
            throw IllegalStateException("Failed to save transaction")
        }

        return Transaction(
            id = id.toInt(),
            type = Transaction.Type.EXPENSE,
            amount = amount,
            description = description,
            wallet = wallet,
            sourceWallet = null,
            destinationWallet = null,
            date = date
        )
    }

    // ========================================================
    // TRANSFER
    // ========================================================

    fun addTransfer(
        amount: Long,
        sourceWallet: Wallet,
        destinationWallet: Wallet
    ): Transaction {

        validateAmount(amount)

        if (sourceWallet == destinationWallet) {
            throw IllegalArgumentException(
                "Source and destination wallet cannot be the same"
            )
        }

        val sourceBalance = getBalance(sourceWallet)

        if (amount > sourceBalance) {
            throw IllegalArgumentException(
                "Insufficient balance in ${sourceWallet.name}. " +
                    "Available balance: Rp$sourceBalance"
            )
        }

        val db = dbHelper.writableDatabase
        val date = getCurrentDate()

        val values = ContentValues().apply {
            put("type", Transaction.Type.TRANSFER.name)
            put("amount", amount)
            put("description", "Transfer")
            putNull("wallet")
            put("source_wallet", sourceWallet.name)
            put("destination_wallet", destinationWallet.name)
            put("date", date)
        }

        val id = db.insert("transactions", null, values)

        if (id == -1L) {
            throw IllegalStateException("Failed to save transaction")
        }

        return Transaction(
            id = id.toInt(),
            type = Transaction.Type.TRANSFER,
            amount = amount,
            description = "Transfer",
            wallet = null,
            sourceWallet = sourceWallet,
            destinationWallet = destinationWallet,
            date = date
        )
    }

    // ========================================================
    // TOTAL BALANCE
    // ========================================================

    fun getBalance(): Long {
        return getTransactions().sumOf { transaction ->
            when (transaction.type) {
                Transaction.Type.INCOME -> transaction.amount
                Transaction.Type.EXPENSE -> -transaction.amount
                Transaction.Type.TRANSFER -> 0L
            }
        }
    }

    // ========================================================
    // WALLET BALANCE
    // ========================================================

    fun getBalance(wallet: Wallet): Long {
        return getTransactions().sumOf { transaction ->
            when (transaction.type) {

                Transaction.Type.INCOME -> {
                    if (transaction.wallet == wallet) {
                        transaction.amount
                    } else {
                        0L
                    }
                }

                Transaction.Type.EXPENSE -> {
                    if (transaction.wallet == wallet) {
                        -transaction.amount
                    } else {
                        0L
                    }
                }

                Transaction.Type.TRANSFER -> {
                    when {
                        transaction.sourceWallet == wallet ->
                            -transaction.amount

                        transaction.destinationWallet == wallet ->
                            transaction.amount

                        else -> 0L
                    }
                }
            }
        }
    }

    // ========================================================
    // STATISTICS
    // ========================================================

    fun getTotalIncome(): Long {
        return getTransactions()
            .filter { it.type == Transaction.Type.INCOME }
            .sumOf { it.amount }
    }

    fun getTotalExpense(): Long {
        return getTransactions()
            .filter { it.type == Transaction.Type.EXPENSE }
            .sumOf { it.amount }
    }

    // ========================================================
    // TRANSACTION HISTORY
    // ========================================================

    fun getTransactions(): List<Transaction> {
        val db = dbHelper.readableDatabase
        val transactions = mutableListOf<Transaction>()

        val cursor = db.query(
            "transactions",
            arrayOf(
                "id",
                "type",
                "amount",
                "description",
                "wallet",
                "source_wallet",
                "destination_wallet",
                "date"
            ),
            null,
            null,
            null,
            null,
            "id ASC"
        )

        cursor.use {
            while (it.moveToNext()) {

                val id = it.getInt(
                    it.getColumnIndexOrThrow("id")
                )

                val type = Transaction.Type.valueOf(
                    it.getString(
                        it.getColumnIndexOrThrow("type")
                    )
                )

                val amount = it.getLong(
                    it.getColumnIndexOrThrow("amount")
                )

                val description = it.getString(
                    it.getColumnIndexOrThrow("description")
                )

                val wallet = it.getString(
                    it.getColumnIndexOrThrow("wallet")
                )?.let(Wallet::valueOf)

                val sourceWallet = it.getString(
                    it.getColumnIndexOrThrow("source_wallet")
                )?.let(Wallet::valueOf)

                val destinationWallet = it.getString(
                    it.getColumnIndexOrThrow("destination_wallet")
                )?.let(Wallet::valueOf)

                val date = it.getString(
                    it.getColumnIndexOrThrow("date")
                )

                transactions.add(
                    Transaction(
                        id = id,
                        type = type,
                        amount = amount,
                        description = description,
                        wallet = wallet,
                        sourceWallet = sourceWallet,
                        destinationWallet = destinationWallet,
                        date = date
                    )
                )
            }
        }

        return transactions
    }

    // ========================================================
    // RESET
    // ========================================================

    fun resetTransactions(): Int {
        val db = dbHelper.writableDatabase

        val deletedCount = db.delete(
            "transactions",
            null,
            null
        )

        db.execSQL(
            "DELETE FROM sqlite_sequence WHERE name = 'transactions'"
        )

        return deletedCount
    }

    // ========================================================
    // TRANSACTION COUNTERS
    // ========================================================

    fun getIncomeTransactionCount(): Int {
        return getTransactions()
            .count { it.type == Transaction.Type.INCOME }
    }

    fun getExpenseTransactionCount(): Int {
        return getTransactions()
            .count { it.type == Transaction.Type.EXPENSE }
    }

    fun getTransferTransactionCount(): Int {
        return getTransactions()
            .count { it.type == Transaction.Type.TRANSFER }
    }

// ========================================================
// DELETE TRANSACTION
// ========================================================

fun deleteTransaction(id: Int): Boolean {
    val db = dbHelper.writableDatabase

    db.beginTransaction()

    try {
        val deletedRows = db.delete(
            "transactions",
            "id = ?",
            arrayOf(id.toString())
        )

        if (deletedRows == 0) {
            return false
        }

        // ========================================================
        // TEMPORARY ID
        // ========================================================

        db.execSQL(
            "UPDATE transactions SET id = -id"
        )

        // ========================================================
        // RENUMBER TRANSACTIONS
        // ========================================================

        val cursor = db.query(
            "transactions",
            arrayOf("id"),
            null,
            null,
            null,
            null,
            "id DESC"
        )

        cursor.use {
            var newId = 1

            while (it.moveToNext()) {
                val temporaryId = it.getInt(
                    it.getColumnIndexOrThrow("id")
                )

                val values = ContentValues().apply {
                    put("id", newId)
                }

                db.update(
                    "transactions",
                    values,
                    "id = ?",
                    arrayOf(temporaryId.toString())
                )

                newId++
            }
        }

        // ========================================================
        // RESET AUTOINCREMENT
        // ========================================================

        db.execSQL(
            "DELETE FROM sqlite_sequence WHERE name = 'transactions'"
        )

        db.execSQL(
            "INSERT INTO sqlite_sequence(name, seq) " +
                "VALUES('transactions', " +
                "(SELECT COALESCE(MAX(id), 0) FROM transactions))"
        )

        db.setTransactionSuccessful()

        return true

    } finally {
        db.endTransaction()
    }
}
    // ========================================================
    // EDIT TRANSACTION
    // ========================================================

    fun editTransaction(
        id: Int,
        amount: Long
    ): Boolean {

        validateAmount(amount)

        val transaction = getTransactions()
            .find { it.id == id }
            ?: return false

        when (transaction.type) {

            Transaction.Type.INCOME -> {
                // No balance restriction for income.
            }

            Transaction.Type.EXPENSE -> {
                val wallet = transaction.wallet
                    ?: throw IllegalStateException(
                        "Expense wallet is missing"
                    )

                val currentBalance = getBalance(wallet)

                val balanceAfterRemovingOldExpense =
                    currentBalance + transaction.amount

                if (amount > balanceAfterRemovingOldExpense) {
                    throw IllegalArgumentException(
                        "Insufficient balance in ${wallet.name}. " +
                            "Available balance: " +
                            "Rp$balanceAfterRemovingOldExpense"
                    )
                }
            }

            Transaction.Type.TRANSFER -> {
                val sourceWallet = transaction.sourceWallet
                    ?: throw IllegalStateException(
                        "Transfer source wallet is missing"
                    )

                val currentSourceBalance =
                    getBalance(sourceWallet)

                val balanceAfterRemovingOldTransfer =
                    currentSourceBalance + transaction.amount

                if (amount > balanceAfterRemovingOldTransfer) {
                    throw IllegalArgumentException(
                        "Insufficient balance in ${sourceWallet.name}. " +
                            "Available balance: " +
                            "Rp$balanceAfterRemovingOldTransfer"
                    )
                }
            }
        }

        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("amount", amount)
        }

        val updatedRows = db.update(
            "transactions",
            values,
            "id = ?",
            arrayOf(id.toString())
        )

        return updatedRows > 0
    }

    // ========================================================
    // VALIDATION & DATE
    // ========================================================

    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        )

        return formatter.format(Date())
    }

    private fun validateAmount(amount: Long) {
        if (amount <= 0) {
            throw IllegalArgumentException(
                "Amount must be greater than 0"
            )
        }
    }

    private fun validateDescription(description: String) {
        if (description.isBlank()) {
            throw IllegalArgumentException(
                "Description cannot be empty"
            )
        }
    }
}