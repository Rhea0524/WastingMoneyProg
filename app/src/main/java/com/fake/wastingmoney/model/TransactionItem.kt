package com.fake.wastingmoney.model

data class TransactionItem(
    val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val date: String = "",
    val timestamp: Long = 0L,
    val type: String = "" // "INCOME" or "EXPENSE"
)