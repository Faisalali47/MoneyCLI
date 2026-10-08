package com.moneycli

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandParserTest {

    private val parser = CommandParser()

    // ========================================================
    // BALANCE
    // ========================================================

    @Test
    fun balanceCommands_shouldReturnBalance() {
        assertEquals(
            ParsedCommand.Balance,
            parser.parse("balance")
        )

        assertEquals(
            ParsedCommand.Balance,
            parser.parse("bal")
        )
    }

    // ========================================================
    // TRANSACTIONS
    // ========================================================

    @Test
    fun transactionCommands_shouldReturnTransactions() {
        assertEquals(
            ParsedCommand.Transactions,
            parser.parse("transactions")
        )

        assertEquals(
            ParsedCommand.Transactions,
            parser.parse("tx")
        )
    }

    // ========================================================
    // STATS
    // ========================================================

    @Test
    fun statsCommands_shouldReturnStats() {
        assertEquals(
            ParsedCommand.Stats,
            parser.parse("stats")
        )

        assertEquals(
            ParsedCommand.Stats,
            parser.parse("st")
        )
    }

    // ========================================================
    // INCOME
    // ========================================================

    @Test
    fun incomeCommands_shouldReturnSameResult() {
        val oldCommand = parser.parse(
            """income add -n 2000 -d "jajan""""
        )

        val newCommand = parser.parse(
            "in 2000 jajan"
        )

        val expected = ParsedCommand.Income(
            amount = 2000,
            description = "jajan"
        )

        assertEquals(expected, oldCommand)
        assertEquals(expected, newCommand)
        assertEquals(oldCommand, newCommand)
    }

    // ========================================================
    // EXPENSE
    // ========================================================

    @Test
    fun expenseCommands_shouldReturnSameResult() {
        val oldCommand = parser.parse(
            """expense add -n 15000 -d "ayam goreng""""
        )

        val newCommand = parser.parse(
            "ex 15000 ayam goreng"
        )

        val expected = ParsedCommand.Expense(
            amount = 15000,
            description = "ayam goreng"
        )

        assertEquals(expected, oldCommand)
        assertEquals(expected, newCommand)
        assertEquals(oldCommand, newCommand)
    }

    // ========================================================
    // TRANSFER
    // ========================================================

    @Test
    fun transferCommands_shouldReturnSameResult() {
        val oldCommand = parser.parse(
            "transfer -n 100000"
        )

        val newCommand = parser.parse(
            "tr 100000"
        )

        val expected = ParsedCommand.Transfer(
            amount = 100000
        )

        assertEquals(expected, oldCommand)
        assertEquals(expected, newCommand)
        assertEquals(oldCommand, newCommand)
    }

    // ========================================================
    // DELETE
    // ========================================================

    @Test
    fun deleteCommands_shouldReturnSameResult() {
        val oldCommand = parser.parse(
            "transaction delete -id 3"
        )

        val newCommand = parser.parse(
            "del 3"
        )

        val expected = ParsedCommand.Delete(
            id = 3
        )

        assertEquals(expected, oldCommand)
        assertEquals(expected, newCommand)
        assertEquals(oldCommand, newCommand)
    }

    // ========================================================
    // EDIT
    // ========================================================

    @Test
    fun editCommands_shouldReturnSameResult() {
        val oldCommand = parser.parse(
            "transaction edit -id 3 -n 25000"
        )

        val newCommand = parser.parse(
            "edit 3 25000"
        )

        val expected = ParsedCommand.Edit(
            id = 3,
            amount = 25000
        )

        assertEquals(expected, oldCommand)
        assertEquals(expected, newCommand)
        assertEquals(oldCommand, newCommand)
    }

    // ========================================================
    // RESET
    // ========================================================

    @Test
    fun resetCommands_shouldReturnReset() {
        assertEquals(
            ParsedCommand.Reset,
            parser.parse("transaction reset")
        )

        assertEquals(
            ParsedCommand.Reset,
            parser.parse("reset")
        )
    }

    // ========================================================
    // OTHER COMMANDS
    // ========================================================

    @Test
    fun basicCommands_shouldBeRecognized() {
        assertEquals(
            ParsedCommand.Help,
            parser.parse("help")
        )

        assertEquals(
            ParsedCommand.Clear,
            parser.parse("clear")
        )

        assertEquals(
            ParsedCommand.Cancel,
            parser.parse("cancel")
        )
    }

    // ========================================================
    // UNKNOWN COMMAND
    // ========================================================

    @Test
    fun unknownCommand_shouldReturnUnknown() {
        val result = parser.parse("hello world")

        assertTrue(result is ParsedCommand.Unknown)

        assertEquals(
            "hello world",
            (result as ParsedCommand.Unknown).command
        )
    }

    // ========================================================
    // INVALID COMMAND
    // ========================================================

    @Test(expected = IllegalArgumentException::class)
    fun invalidIncome_shouldThrowException() {
        parser.parse("in 2000")
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidExpense_shouldThrowException() {
        parser.parse("ex 15000")
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidTransfer_shouldThrowException() {
        parser.parse("tr")
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDelete_shouldThrowException() {
        parser.parse("del")
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidEdit_shouldThrowException() {
        parser.parse("edit 3")
    }
}