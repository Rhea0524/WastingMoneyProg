package com.fake.wastingmoney.database

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

data class Expense(
    val id: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

object FirebaseHelper {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    fun addExpense(expense: Expense) {
        val key = database.child("expenses").push().key ?: return
        val newExpense = expense.copy(id = key)
        database.child("expenses").child(key).setValue(newExpense)
    }

    fun getExpenses(onData: (List<Expense>) -> Unit) {
        database.child("expenses").get().addOnSuccessListener { snapshot ->
            val expenses = snapshot.children.mapNotNull { it.getValue(Expense::class.java) }
            onData(expenses)
        }
    }
}
