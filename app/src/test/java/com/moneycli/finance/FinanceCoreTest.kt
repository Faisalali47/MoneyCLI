package com.moneycli.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FinanceCoreTest {

    private lateinit var finance: FinanceCore

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        finance = FinanceCore(context)
        finance.resetTransactions()
    }

    @Test
    fun addIncome_shouldIncreaseBalance() {
        finance.addIncome(
            500_000,
            "Uang bulanan",
            Wallet.CASH
        )

        assertEquals(500_000, finance.getBalance())
    }

    @Test
    fun addExpense_shouldDecreaseBalance() {
        finance.addIncome(
            500_000,
            "Uang bulanan",
            Wallet.CASH
        )

        finance.addExpense(
            20_000,
            "Ayam goreng",
            Wallet.CASH
        )

        assertEquals(480_000, finance.getBalance())
    }

    @Test
    fun multipleTransactions_shouldCalculateCorrectBalance() {
        finance.addIncome(
            1_000_000,
            "Uang bulanan",
            Wallet.CASH
        )

        finance.addExpense(
            100_000,
            "Makan",
            Wallet.CASH
        )

        finance.addExpense(
            50_000,
            "Transport",
            Wallet.CASH
        )

        finance.addIncome(
            200_000,
            "Freelance",
            Wallet.BCA
        )

        assertEquals(1_050_000, finance.getBalance())
    }

    @Test
    fun addTransaction_shouldGenerateUniqueIds() {
        val first = finance.addIncome(
            100_000,
            "Income",
            Wallet.CASH
        )

        val second = finance.addExpense(
            20_000,
            "Expense",
            Wallet.CASH
        )

        assertEquals(1, first.id)
        assertEquals(2, second.id)
    }

    @Test
    fun walletBalance_shouldCalculateSeparately() {
        finance.addIncome(
            500_000,
            "Cash income",
            Wallet.CASH
        )

        finance.addIncome(
            300_000,
            "BCA income",
            Wallet.BCA
        )

        assertEquals(500_000, finance.getBalance(Wallet.CASH))
        assertEquals(300_000, finance.getBalance(Wallet.BCA))
        assertEquals(800_000, finance.getBalance())
    }

    @Test
    fun transfer_shouldMoveBalanceBetweenWallets() {
        finance.addIncome(
            500_000,
            "Income",
            Wallet.BCA
        )

        finance.addTransfer(
            100_000,
            Wallet.BCA,
            Wallet.CASH
        )

        assertEquals(400_000, finance.getBalance(Wallet.BCA))
        assertEquals(100_000, finance.getBalance(Wallet.CASH))
        assertEquals(500_000, finance.getBalance())
    }

    @Test
    fun expenseOverBalance_shouldBeRejected() {
        finance.addIncome(
            100_000,
            "Income",
            Wallet.CASH
        )

        try {
            finance.addExpense(
                150_000,
                "Expense",
                Wallet.CASH
            )

            assertFalse(true)
        } catch (e: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    @Test
    fun transferOverBalance_shouldBeRejected() {
        finance.addIncome(
            100_000,
            "Income",
            Wallet.BCA
        )

        try {
            finance.addTransfer(
                150_000,
                Wallet.BCA,
                Wallet.CASH
            )

            assertFalse(true)
        } catch (e: IllegalArgumentException) {
            assertTrue(true)
        }
    }
}