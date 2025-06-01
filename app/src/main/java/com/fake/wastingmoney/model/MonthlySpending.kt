package com.fake.wastingmoney.model

data class MonthlySpending(
    val month: String = "",
    val categories: Map<String, Double> = emptyMap(),
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0
)