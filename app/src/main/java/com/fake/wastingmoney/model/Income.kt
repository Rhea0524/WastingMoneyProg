
package com.fake.wastingmoney.model


data class Income(
    val amount: Double = 0.0,
    val description: String = "",
    val source: String = "",
    val date: String = "",
    val timestamp: Long = 0L,
    val documentBase64: String? = null
) {
    // No-argument constructor for Firebase
    constructor() : this(0.0, "", "", "", 0L)
}