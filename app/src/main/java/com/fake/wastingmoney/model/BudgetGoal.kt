package com.fake.wastingmoney.model

data class BudgetGoal(
    val id: Long = 0,
    val userId: Long,
    val minMonthlyGoal: Double,
    val maxMonthlyGoal: Double,
    val month: String, // Format: "2024-01"
    val year: Int
)