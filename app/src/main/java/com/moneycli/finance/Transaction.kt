package com.moneycli.finance

data class Transaction(
    val id: Int,
    val type: Type,
    val amount: Long,
    val description: String,
    val wallet: Wallet?,
    val sourceWallet: Wallet?,
    val destinationWallet: Wallet?,
    val date: String
) {
    enum class Type {
        INCOME,
        EXPENSE,
        TRANSFER
    }
}