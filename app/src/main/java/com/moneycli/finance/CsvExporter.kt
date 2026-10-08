package com.moneycli.finance

// ========================================================
// CSV EXPORTER
// ========================================================

class CsvExporter {

    fun generateCsv(transactions: List<Transaction>): String {
        val builder = StringBuilder()

        builder.appendLine(
            "ID,Type,Amount,Description,Wallet,Source Wallet,Destination Wallet,Date"
        )

        transactions.forEach { transaction ->

            builder.appendLine(
                listOf(
                    transaction.id,
                    transaction.type.name,
                    transaction.amount,
                    escape(transaction.description),
                    transaction.wallet?.name ?: "",
                    transaction.sourceWallet?.name ?: "",
                    transaction.destinationWallet?.name ?: "",
                    escape(transaction.date)
                ).joinToString(",")
            )
        }

        return builder.toString()
    }

    private fun escape(value: String): String {
        return "\"${value.replace("\"", "\"\"")}\""
    }
}