package com.fake.wastingmoney.model

data class Expense(
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val date: String = "",
    val timestamp: Long = 0L,
    val documentBase64: String? = null  // <-- Add this
) {
    // Firebase requires a no-argument constructor
    constructor() : this(0.0, "", "", "", 0L, null)
}