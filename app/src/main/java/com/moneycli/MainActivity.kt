package com.moneycli

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.moneycli.finance.FinanceCore
import com.moneycli.finance.Transaction
import com.moneycli.finance.Wallet
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : Activity() {

    // ========================================================
    // UI
    // ========================================================

    private lateinit var terminal: TextView
    private lateinit var terminalScroll: ScrollView
    private lateinit var input: EditText

    // ========================================================
    // RESET STATE
    // ========================================================

    private var awaitingResetConfirmation = false

    // ========================================================
    // INCOME STATE
    // ========================================================

    private var awaitingIncomeWallet = false
    private var pendingIncomeAmount: Long? = null
    private var pendingIncomeDescription: String? = null

    // ========================================================
    // EXPENSE STATE
    // ========================================================

    private var awaitingExpenseWallet = false
    private var pendingExpenseAmount: Long? = null
    private var pendingExpenseDescription: String? = null

    // ========================================================
    // TRANSFER STATE
    // ========================================================

    private var awaitingTransferSource = false
    private var awaitingTransferDestination = false
    private var pendingTransferAmount: Long? = null
    private var pendingTransferSource: Wallet? = null

    private val finance by lazy {
        FinanceCore(this)
    }

    // ========================================================
    // ACTIVITY
    // ========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(32, 32, 32, 32)
        root.setBackgroundColor(Color.BLACK)

        terminal = TextView(this)

        terminal.text = """
            MONEY CLI
            ────────────────────────────
            Balance: Rp${finance.getBalance()}

            ical:\MoneyCLI>
        """.trimIndent()

        terminal.setTextColor(Color.WHITE)
        terminal.textSize = 18f
        terminal.typeface = Typeface.MONOSPACE

        terminalScroll = ScrollView(this)
        terminalScroll.isFillViewport = true

        terminalScroll.addView(
            terminal,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        input = EditText(this)
        input.hint = "Enter command..."
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(Color.GRAY)
        input.textSize = 18f
        input.typeface = Typeface.MONOSPACE

        val button = Button(this)
        button.text = "EXECUTE"

        button.setOnClickListener {
            executeCommand()
        }

        input.setOnEditorActionListener { _, _, _ ->
            executeCommand()
            true
        }

        root.addView(
            terminalScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(
            input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)
    }

    // ========================================================
    // COMMAND PROCESSING
    // ========================================================

    private fun executeCommand() {
        val command = input.text.toString().trim()

        if (command.isEmpty()) {
            return
        }

        if (awaitingResetConfirmation) {
            handleResetConfirmation(command)
            input.text.clear()
            return
        }

        if (awaitingIncomeWallet) {
            appendTerminal("ical:\\MoneyCLI> $command")
            handleIncomeWalletSelection(command)
            input.text.clear()
            return
        }

        if (awaitingExpenseWallet) {
            appendTerminal("ical:\\MoneyCLI> $command")
            handleExpenseWalletSelection(command)
            input.text.clear()
            return
        }

        if (awaitingTransferSource) {
            appendTerminal("ical:\\MoneyCLI> $command")
            handleTransferSource(command)
            input.text.clear()
            return
        }

        if (awaitingTransferDestination) {
            appendTerminal("ical:\\MoneyCLI> $command")
            handleTransferDestination(command)
            input.text.clear()
            return
        }

        appendTerminal("ical:\\MoneyCLI> $command")

        when {
            command.lowercase() == "balance" -> {
                showBalance()
            }

            command.lowercase() == "transactions" -> {
                showTransactions()
            }

            command.lowercase().startsWith("income add") -> {
                handleIncome(command)
            }

            command.lowercase().startsWith("expense add") -> {
                handleExpense(command)
            }

            command.lowercase().startsWith("transfer") -> {
                handleTransfer(command)
            }

            command.lowercase() == "transaction reset" -> {
                requestResetConfirmation()
            }

            command.lowercase() == "help" -> {
                showHelp()
            }

            command.lowercase().startsWith("transaction delete") -> {
                handleDeleteTransaction(command)
            }

            command.lowercase().startsWith("transaction edit") -> {
                handleEditTransaction(command)
            }

            command.lowercase() == "stats" -> {
                showStats()
            }

            command.lowercase() == "clear" -> {
                terminal.text =
                    "MONEY CLI\n────────────────────────────\n\nical:\\MoneyCLI>"
            }

            command.lowercase() == "cancel" -> {
                cancelPendingOperation()
            }

            else -> {
                appendTerminal(
                    """
                    Unknown command: $command

                    Type 'help' for available commands.
                    """.trimIndent()
                )
            }
        }

        input.text.clear()
        scrollToBottom()
    }

    // ========================================================
    // INCOME
    // ========================================================

    private fun handleIncome(command: String) {
        try {
            val amount = extractAmount(command)
            val description = extractDescription(command)

            pendingIncomeAmount = amount
            pendingIncomeDescription = description
            awaitingIncomeWallet = true

            appendTerminal(
                """
                Select wallet:

                1. Cash
                2. BCA

                Type 1 or 2:
                """.trimIndent()
            )

        } catch (e: Exception) {
            appendTerminal(
                """
                Error: ${e.message}

                Example:
                income add -n 500000 -d "uang bulanan"
                """.trimIndent()
            )
        }
    }

    private fun handleIncomeWalletSelection(command: String) {

        if (command.lowercase() == "cancel") {
            cancelPendingOperation()
            return
        }

        val wallet = parseWallet(command)

        if (wallet == null) {
            appendTerminal(
                """
                Invalid wallet selection.

                Please type:
                1 for Cash
                2 for BCA
                """.trimIndent()
            )
            return
        }

        try {
            val amount = pendingIncomeAmount
                ?: throw IllegalStateException("Income amount is missing")

            val description = pendingIncomeDescription
                ?: throw IllegalStateException("Income description is missing")

            val transaction = finance.addIncome(
                amount = amount,
                description = description,
                wallet = wallet
            )

            appendTerminal(
                """
                ✓ Income added
                +Rp${transaction.amount}
                ${transaction.description}
                Wallet: ${formatWallet(wallet)}
                """.trimIndent()
            )

            clearIncomeState()

        } catch (e: Exception) {
            appendTerminal("Error: ${e.message}")
            clearIncomeState()
        }
    }

    // ========================================================
    // EXPENSE
    // ========================================================

    private fun handleExpense(command: String) {
        try {
            val amount = extractAmount(command)
            val description = extractDescription(command)

            pendingExpenseAmount = amount
            pendingExpenseDescription = description
            awaitingExpenseWallet = true

            appendTerminal(
                """
                Select wallet:

                1. Cash
                2. BCA

                Type 1 or 2:
                """.trimIndent()
            )

        } catch (e: Exception) {
            appendTerminal(
                """
                Error: ${e.message}

                Example:
                expense add -n 20000 -d "ayam goreng"
                """.trimIndent()
            )
        }
    }

    private fun handleExpenseWalletSelection(command: String) {

        if (command.lowercase() == "cancel") {
            cancelPendingOperation()
            return
        }

        val wallet = parseWallet(command)

        if (wallet == null) {
            appendTerminal(
                """
                Invalid wallet selection.

                Please type:
                1 for Cash
                2 for BCA
                """.trimIndent()
            )
            return
        }

        try {
            val amount = pendingExpenseAmount
                ?: throw IllegalStateException("Expense amount is missing")

            val description = pendingExpenseDescription
                ?: throw IllegalStateException("Expense description is missing")

            val transaction = finance.addExpense(
                amount = amount,
                description = description,
                wallet = wallet
            )

            appendTerminal(
                """
                ✓ Expense added
                -Rp${transaction.amount}
                ${transaction.description}
                Wallet: ${formatWallet(wallet)}
                """.trimIndent()
            )

            clearExpenseState()

        } catch (e: Exception) {
            appendTerminal("Error: ${e.message}")
            clearExpenseState()
        }
    }

    // ========================================================
    // TRANSFER
    // ========================================================

    private fun handleTransfer(command: String) {
        try {
            val amount = extractAmount(command)

            pendingTransferAmount = amount
            awaitingTransferSource = true

            appendTerminal(
                """
                Select source wallet:

                1. Cash
                2. BCA

                Type 1 or 2:
                """.trimIndent()
            )

        } catch (e: Exception) {
            appendTerminal(
                """
                Error: ${e.message}

                Example:
                transfer -n 100000
                """.trimIndent()
            )
        }
    }

    private fun handleTransferSource(command: String) {

        if (command.lowercase() == "cancel") {
            cancelPendingOperation()
            return
        }

        val wallet = parseWallet(command)

        if (wallet == null) {
            appendTerminal(
                """
                Invalid wallet selection.

                Please type:
                1 for Cash
                2 for BCA
                """.trimIndent()
            )
            return
        }

        pendingTransferSource = wallet
        awaitingTransferSource = false
        awaitingTransferDestination = true

        appendTerminal(
            """
            Select destination wallet:

            1. Cash
            2. BCA

            Type 1 or 2:
            """.trimIndent()
        )
    }

    private fun handleTransferDestination(command: String) {

        if (command.lowercase() == "cancel") {
            cancelPendingOperation()
            return
        }

        val destinationWallet = parseWallet(command)

        if (destinationWallet == null) {
            appendTerminal(
                """
                Invalid wallet selection.

                Please type:
                1 for Cash
                2 for BCA
                """.trimIndent()
            )
            return
        }

        try {
            val amount = pendingTransferAmount
                ?: throw IllegalStateException("Transfer amount is missing")

            val sourceWallet = pendingTransferSource
                ?: throw IllegalStateException("Source wallet is missing")

            if (sourceWallet == destinationWallet) {
                appendTerminal(
                    """
                    Source and destination wallet cannot be the same.

                    Please select another destination wallet.
                    """.trimIndent()
                )
                return
            }

            val transaction = finance.addTransfer(
                amount = amount,
                sourceWallet = sourceWallet,
                destinationWallet = destinationWallet
            )

            appendTerminal(
                """
                ✓ Transfer successful
                Rp${transaction.amount}
                ${formatWallet(sourceWallet)} → ${formatWallet(destinationWallet)}
                """.trimIndent()
            )

            clearTransferState()

        } catch (e: Exception) {
            appendTerminal("Error: ${e.message}")
            clearTransferState()
        }
    }

    // ========================================================
    // WALLET HELPERS
    // ========================================================

    private fun parseWallet(command: String): Wallet? {
        return when (command.trim()) {
            "1" -> Wallet.CASH
            "2" -> Wallet.BCA
            else -> null
        }
    }

    private fun formatWallet(wallet: Wallet): String {
        return when (wallet) {
            Wallet.CASH -> "Cash"
            Wallet.BCA -> "BCA"
        }
    }

    private fun clearIncomeState() {
        awaitingIncomeWallet = false
        pendingIncomeAmount = null
        pendingIncomeDescription = null
    }

    private fun clearExpenseState() {
        awaitingExpenseWallet = false
        pendingExpenseAmount = null
        pendingExpenseDescription = null
    }

    private fun clearTransferState() {
        awaitingTransferSource = false
        awaitingTransferDestination = false
        pendingTransferAmount = null
        pendingTransferSource = null
    }

    private fun cancelPendingOperation() {
        clearIncomeState()
        clearExpenseState()
        clearTransferState()

        appendTerminal("Operation cancelled.")
    }

    // ========================================================
    // DELETE
    // ========================================================

    private fun handleDeleteTransaction(command: String) {
        try {
            val regex = Regex("""-id\s+(\d+)""")

            val match = regex.find(command)
                ?: throw IllegalArgumentException("Transaction ID not found")

            val id = match.groupValues[1].toInt()

            val deleted = finance.deleteTransaction(id)

            if (deleted) {
                appendTerminal(
                    """
                    ✓ Transaction deleted
                    ID: #$id
                    """.trimIndent()
                )
            } else {
                appendTerminal(
                    "Transaction #$id not found"
                )
            }

        } catch (e: Exception) {
            appendTerminal(
                """
                Error: ${e.message}

                Example:
                transaction delete -id 2
                """.trimIndent()
            )
        }
    }

    // ========================================================
    // EDIT
    // ========================================================

    private fun handleEditTransaction(command: String) {
        try {
            val idRegex = Regex("""-id\s+(\d+)""")
            val amountRegex = Regex("""-n\s+(\d+)""")

            val idMatch = idRegex.find(command)
                ?: throw IllegalArgumentException("Transaction ID not found")

            val amountMatch = amountRegex.find(command)
                ?: throw IllegalArgumentException("Amount not found")

            val id = idMatch.groupValues[1].toInt()
            val amount = amountMatch.groupValues[1].toLong()

            val updated = finance.editTransaction(id, amount)

            if (updated) {
                appendTerminal(
                    """
                    ✓ Transaction updated
                    ID: #$id
                    Amount: Rp$amount
                    """.trimIndent()
                )
            } else {
                appendTerminal(
                    "Transaction #$id not found"
                )
            }

        } catch (e: Exception) {
            appendTerminal(
                """
                Error: ${e.message}

                Example:
                transaction edit -id 2 -n 35000
                """.trimIndent()
            )
        }
    }

    // ========================================================
    // PARSING
    // ========================================================

    private fun extractAmount(command: String): Long {
        val regex = Regex("""-n\s+(\d+)""")

        val match = regex.find(command)
            ?: throw IllegalArgumentException("Amount not found")

        return match.groupValues[1].toLong()
    }

    private fun extractDescription(command: String): String {
        val regex = Regex("""-d\s+"([^"]*)"""")
        
        val match = regex.find(command)
            ?: throw IllegalArgumentException("Description not found")

        return match.groupValues[1]
    }

    // ========================================================
    // TRANSACTIONS
    // ========================================================

    private fun showTransactions() {
        val transactions = finance.getTransactions()

        if (transactions.isEmpty()) {
            appendTerminal("No transactions yet.")
            return
        }

        val result = StringBuilder()

        result.appendLine("Transaction History")
        result.appendLine()

        for (transaction in transactions) {

            result.appendLine(
                "#${transaction.id}  ${transaction.type}"
            )

            when (transaction.type) {

                Transaction.Type.INCOME -> {
                    result.appendLine(
                        "    + Rp${transaction.amount}"
                    )

                    result.appendLine(
                        "    ${transaction.description}"
                    )

                    result.appendLine(
                        "    Wallet: ${formatWallet(transaction.wallet!!)}"
                    )
                }

                Transaction.Type.EXPENSE -> {
                    result.appendLine(
                        "    - Rp${transaction.amount}"
                    )

                    result.appendLine(
                        "    ${transaction.description}"
                    )

                    result.appendLine(
                        "    Wallet: ${formatWallet(transaction.wallet!!)}"
                    )
                }

                Transaction.Type.TRANSFER -> {
                    result.appendLine(
                        "    Rp${transaction.amount}"
                    )

                    result.appendLine(
                        "    ${formatWallet(transaction.sourceWallet!!)} → " +
                            formatWallet(transaction.destinationWallet!!)
                    )
                }
            }

            result.appendLine(
                "    ${formatTransactionDate(transaction.date)}"
            )

            result.appendLine()
        }

        result.appendLine(
            "Total Balance: Rp${finance.getBalance()}"
        )

        appendTerminal(
            result.toString().trimEnd()
        )
    }

    // ========================================================
    // BALANCE
    // ========================================================

    private fun showBalance() {
        val cashBalance = finance.getBalance(Wallet.CASH)
        val bcaBalance = finance.getBalance(Wallet.BCA)
        val totalBalance = finance.getBalance()

        appendTerminal(
            """
            Balance

            Cash : Rp$cashBalance
            BCA  : Rp$bcaBalance
            ────────────────────────────
            Total: Rp$totalBalance
            """.trimIndent()
        )
    }

    // ========================================================
    // HELP
    // ========================================================

    private fun showHelp() {
        appendTerminal(
            """
            Available commands:

            balance
            transactions
            stats

            income add -n <amount> -d "<description>"

            expense add -n <amount> -d "<description>"

            transfer -n <amount>

            transaction delete -id <id>
            transaction edit -id <id> -n <amount>
            transaction reset

            help
            clear

            Type "cancel" during wallet selection
            to cancel the current operation.
            """.trimIndent()
        )
    }

    // ========================================================
    // TERMINAL
    // ========================================================

    private fun appendTerminal(text: String) {
        terminal.append(
            "\n\n────────────────────────────\n\n$text"
        )

        scrollToBottom()
    }

    // ========================================================
    // STATS
    // ========================================================

    private fun showStats() {
        val totalIncome = finance.getTotalIncome()
        val totalExpense = finance.getTotalExpense()

        val incomeCount = finance.getIncomeTransactionCount()
        val expenseCount = finance.getExpenseTransactionCount()
        val transferCount = finance.getTransferTransactionCount()

        val totalCount = finance.getTransactions().size

        val cashBalance = finance.getBalance(Wallet.CASH)
        val bcaBalance = finance.getBalance(Wallet.BCA)
        val totalBalance = finance.getBalance()

        appendTerminal(
            """
            Financial Statistics

            Total Income         : Rp$totalIncome
            Total Expense        : Rp$totalExpense

            Income Transactions  : $incomeCount
            Expense Transactions : $expenseCount
            Transfer Transactions: $transferCount
            Total Transactions   : $totalCount

            Cash Balance         : Rp$cashBalance
            BCA Balance          : Rp$bcaBalance
            Total Balance        : Rp$totalBalance
            """.trimIndent()
        )
    }

    // ========================================================
    // RESET
    // ========================================================

    private fun requestResetConfirmation() {
        awaitingResetConfirmation = true

        appendTerminal(
            """
            WARNING: This will delete ALL transactions.

            Type "confirm" to continue:
            """.trimIndent()
        )
    }

    private fun handleResetConfirmation(command: String) {

        if (command.lowercase() == "confirm") {

            val deletedCount = finance.resetTransactions()

            awaitingResetConfirmation = false

            appendTerminal(
                """
                ✓ All transactions deleted

                Deleted: $deletedCount transactions
                Balance: Rp${finance.getBalance()}
                """.trimIndent()
            )

        } else {

            awaitingResetConfirmation = false

            appendTerminal(
                "Reset cancelled."
            )
        }

        scrollToBottom()
    }

    // ========================================================
    // DATE
    // ========================================================

    private fun formatTransactionDate(date: String): String {
        val inputFormat = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        )

        val outputFormat = SimpleDateFormat(
            "d MMM yyyy, hh:mm a",
            Locale.getDefault()
        )

        val parsedDate = inputFormat.parse(date)
            ?: return date

        return outputFormat.format(parsedDate)
    }

    // ========================================================
    // SCROLL
    // ========================================================

    private fun scrollToBottom() {
        terminalScroll.post {
            terminalScroll.fullScroll(
                ScrollView.FOCUS_DOWN
            )
        }
    }
}