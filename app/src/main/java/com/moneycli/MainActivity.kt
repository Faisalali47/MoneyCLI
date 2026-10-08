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
import android.text.SpannableString
import android.text.Spannable
import android.text.style.ForegroundColorSpan

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

	private val commandParser = CommandParser()

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

executeParsedCommand(command)

    input.text.clear()
    scrollToBottom()
}

// ========================================================
// PARSED COMMAND EXECUTION
// ========================================================

private fun executeParsedCommand(command: String) {
    val parsedCommand = try {
        commandParser.parse(command)
    } catch (e: IllegalArgumentException) {
        appendTerminal(
            e.message ?: "Invalid command"
        )
        return
    }

    when (parsedCommand) {
        ParsedCommand.Balance -> showBalance()

        ParsedCommand.Transactions -> showTransactions()

        ParsedCommand.Stats -> showStats()

        ParsedCommand.Help -> showHelp()

        ParsedCommand.Clear -> {
            terminal.text =
                "MONEY CLI\n────────────────────────────\n\nical:\\MoneyCLI>"
        }

        ParsedCommand.Cancel -> cancelPendingOperation()

        ParsedCommand.Reset -> requestResetConfirmation()

        is ParsedCommand.Income -> handleIncome(command)

        is ParsedCommand.Expense -> handleExpense(command)

        is ParsedCommand.Transfer -> handleTransfer(command)

        is ParsedCommand.Delete -> handleDeleteTransaction(command)

        is ParsedCommand.Edit -> handleEditTransaction(command)

        is ParsedCommand.Unknown -> {
            appendTerminal(
                """
                Unknown command: ${parsedCommand.command}

                Type 'help' for available commands.
                """.trimIndent()
            )
        }
    }
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
        val shortCommand = command.trim().lowercase()

        val id = if (shortCommand.startsWith("del ")) {
            val parts = command.trim().split(Regex("\\s+"))

            if (parts.size < 2) {
                throw IllegalArgumentException("Transaction ID not found")
            }

            parts[1].toIntOrNull()
                ?: throw IllegalArgumentException("Transaction ID not found")
        } else {
            val regex = Regex("""-id\s+(\d+)""")

            val match = regex.find(command)
                ?: throw IllegalArgumentException("Transaction ID not found")

            match.groupValues[1].toInt()
        }

        val deleted = finance.deleteTransaction(id)

        if (deleted) {
            appendTerminal(
                """
                ✓ Transaction deleted
                ID: #$id
                """.trimIndent()
            )
        } else {
            appendTerminal("Transaction #$id not found")
        }

    } catch (e: Exception) {
        appendTerminal(
            """
            Error: ${e.message}

            Examples:
            transaction delete -id 2
            del 2
            """.trimIndent()
        )
    }
}

    // ========================================================
    // EDIT
    // ========================================================

    private fun handleEditTransaction(command: String) {
    try {
        val shortCommand = command.trim().lowercase()

        val id: Int
        val amount: Long

        if (shortCommand.startsWith("edit ")) {
            val parts = command.trim().split(Regex("\\s+"))

            if (parts.size < 3) {
                throw IllegalArgumentException("ID or amount not found")
            }

            id = parts[1].toIntOrNull()
                ?: throw IllegalArgumentException("Transaction ID not found")

            amount = parts[2].toLongOrNull()
                ?: throw IllegalArgumentException("Amount not found")
        } else {
            val idRegex = Regex("""-id\s+(\d+)""")
            val amountRegex = Regex("""-n\s+(\d+)""")

            val idMatch = idRegex.find(command)
                ?: throw IllegalArgumentException("Transaction ID not found")

            val amountMatch = amountRegex.find(command)
                ?: throw IllegalArgumentException("Amount not found")

            id = idMatch.groupValues[1].toInt()
            amount = amountMatch.groupValues[1].toLong()
        }

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
            appendTerminal("Transaction #$id not found")
        }

    } catch (e: Exception) {
        appendTerminal(
            """
            Error: ${e.message}

            Examples:
            transaction edit -id 2 -n 35000
            edit 2 35000
            """.trimIndent()
        )
    }
}

    // ========================================================
    // PARSING
    // ========================================================

    private fun extractAmount(command: String): Long {
    val shortCommand = command.trim().lowercase()

    if (
        shortCommand.startsWith("in ") ||
        shortCommand.startsWith("ex ") ||
        shortCommand.startsWith("tr ")
    ) {
        val parts = command.trim().split(Regex("\\s+"), limit = 3)

        if (parts.size < 2) {
            throw IllegalArgumentException("Amount not found")
        }

        return parts[1].toLongOrNull()
            ?: throw IllegalArgumentException("Amount not found")
    }

    val regex = Regex("""-n\s+(\d+)""")

    val match = regex.find(command)
        ?: throw IllegalArgumentException("Amount not found")

    return match.groupValues[1].toLong()
}

    private fun extractDescription(command: String): String {
    val shortCommand = command.trim().lowercase()

    if (
        shortCommand.startsWith("in ") ||
        shortCommand.startsWith("ex ")
    ) {
        val parts = command.trim().split(Regex("\\s+"), limit = 3)

        if (parts.size < 3 || parts[2].isBlank()) {
            throw IllegalArgumentException("Description not found")
        }

        return parts[2].trim()
    }

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
        bal

        transactions
        tx

        stats
        st

        income add -n <amount> -d "<description>"
        in <amount> <description>

        expense add -n <amount> -d "<description>"
        ex <amount> <description>

        transfer -n <amount>
        tr <amount>

        transaction delete -id <id>
        del <id>

        transaction edit -id <id> -n <amount>
        edit <id> <amount>

        transaction reset
        reset

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
	val styledText = styleTerminalText(text)	

        terminal.append(
            "\n\n────────────────────────────\n\n"
        )
	
	terminal.append(styledText)	

        scrollToBottom()
    }

// ========================================================
// TERMINAL DESIGN
// ========================================================

private fun applyColor(
    text: SpannableString,
    color: Int
) {
    text.setSpan(
        ForegroundColorSpan(color),
        0,
        text.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
}

private fun styleTerminalText(text: String): SpannableString {
    val styledText = SpannableString(text)

    val commandColor = Color.rgb(86, 156, 214)
    val numberColor = Color.rgb(181, 206, 168)
    val successColor = Color.rgb(106, 153, 85)
    val errorColor = Color.rgb(244, 71, 71)
    val warningColor = Color.rgb(220, 220, 170)
    val walletColor = Color.rgb(78, 201, 176)
    val labelColor = Color.rgb(197, 134, 192)

    // ========================================================
    // SUCCESS
    // ========================================================

    if (
        text.contains("✓") ||
        text.contains("successful") ||
        text.contains("added") ||
        text.contains("updated") ||
        text.contains("deleted")
    ) {
        applyColor(styledText, successColor)
    }

    // ========================================================
    // ERROR
    // ========================================================

    if (
        text.startsWith("Error:") ||
        text.contains("Invalid") ||
        text.contains("not found") ||
        text.contains("cannot") ||
        text.contains("failed")
    ) {
        applyColor(styledText, errorColor)
    }

    // ========================================================
    // WARNING / CONFIRMATION
    // ========================================================

    if (
        text.contains("Are you sure") ||
        text.contains("Type") ||
        text.contains("confirm") ||
        text.contains("Warning")
    ) {
        applyColor(styledText, warningColor)
    }

    // ========================================================
    // COMMAND
    // ========================================================

    val commandRegex = Regex(
        """\b(income|expense|transfer|balance|transactions|stats|help|clear|cancel|reset|in|ex|tr|bal|tx|st|del|edit)\b"""
    )

    commandRegex.findAll(text).forEach { match ->
        styledText.setSpan(
            ForegroundColorSpan(commandColor),
            match.range.first,
            match.range.last + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    // ========================================================
    // WALLET
    // ========================================================

    val walletRegex = Regex(
        """\b(Cash|BCA)\b"""
    )

    walletRegex.findAll(text).forEach { match ->
        styledText.setSpan(
            ForegroundColorSpan(walletColor),
            match.range.first,
            match.range.last + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    // ========================================================
    // LABEL
    // ========================================================

    val labelRegex = Regex(
        """\b(Balance|Total|Income|Expense|Transactions|Statistics|Source|Destination|Description|Amount)\b"""
    )

    labelRegex.findAll(text).forEach { match ->
        styledText.setSpan(
            ForegroundColorSpan(labelColor),
            match.range.first,
            match.range.last + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    // ========================================================
    // NUMBER
    // ========================================================

    val numberRegex = Regex(
        """(?<=Rp)\d+|\b\d+\b"""
    )

    numberRegex.findAll(text).forEach { match ->
        styledText.setSpan(
            ForegroundColorSpan(numberColor),
            match.range.first,
            match.range.last + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    return styledText
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