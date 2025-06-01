package com.fake.wastingmoney.model

data class CategorySummary(
    val name: String = "",
    val totalSpent: Double = 0.0,
    val monthlySpent: Double = 0.0,
    val budget: Double = 0.0,
    val transactionCount: Int = 0
)