package com.fake.wastingmoney

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.util.*

class CategoryDetail : AppCompatActivity() {

    // UI Components
    private lateinit var menuIcon: LinearLayout
    private lateinit var categoryTitle: TextView
    private lateinit var chartContainer: LinearLayout
    private lateinit var chartArea: FrameLayout
    private lateinit var chartPlaceholder: View
    private lateinit var totalAmount: TextView
    private lateinit var fabAddTransaction: FloatingActionButton
    private lateinit var transactionsContainer: LinearLayout

    // Firebase
    private lateinit var firestore: FirebaseFirestore

    // Data variables
    private var currentTotal: Double = 0.0
    private val transactions = mutableListOf<Transaction>()
    private var categoryName: String = "WATER and LIGHTS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_category_detail)

            // Initialize Firebase
            firestore = FirebaseFirestore.getInstance()

            // Get category from intent if passed
            categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "WATER and LIGHTS"

            initializeViews()
            setupWindowInsets()
            setupClickListeners()
            loadTransactionsFromFirebase()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing CategoryDetail: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            // Initialize all views from XML - with null checks
            menuIcon = findViewById(R.id.menuIcon)
                ?: throw Exception("menuIcon not found in layout")
            categoryTitle = findViewById(R.id.categoryTitle)
                ?: throw Exception("categoryTitle not found in layout")
            chartContainer = findViewById(R.id.chartContainer)
                ?: throw Exception("chartContainer not found in layout")
            chartArea = findViewById(R.id.chartArea)
                ?: throw Exception("chartArea not found in layout")
            chartPlaceholder = findViewById(R.id.chartPlaceholder)
                ?: throw Exception("chartPlaceholder not found in layout")
            totalAmount = findViewById(R.id.totalAmount)
                ?: throw Exception("totalAmount not found in layout")
            fabAddTransaction = findViewById(R.id.fabAddTransaction)
                ?: throw Exception("fabAddTransaction not found in layout")

            // Set category title
            categoryTitle.text = categoryName

            // Create transactions container for dynamic content
            transactionsContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

        } catch (e: Exception) {
            throw Exception("Failed to initialize views: ${e.message}")
        }
    }

    private fun setupWindowInsets() {
        try {
            val mainView = findViewById<View>(R.id.main)
            if (mainView != null) {
                ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }
        } catch (e: Exception) {
            // Window insets setup is optional, log but don't crash
            e.printStackTrace()
        }
    }

    private fun setupClickListeners() {
        try {
            // Menu icon click listener
            menuIcon.setOnClickListener {
                openMenu()
            }

            // FAB click listener for adding new transaction
            fabAddTransaction.setOnClickListener {
                addNewTransaction()
            }

            // Chart area click listener
            chartArea.setOnClickListener {
                showChartDetails()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up click listeners: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTransactionsFromFirebase() {
        try {
            firestore.collection("transactions")
                .whereEqualTo("category", categoryName)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    try {
                        transactions.clear()
                        for (document in documents) {
                            val transaction = Transaction(
                                id = document.id,
                                date = document.getString("date") ?: "",
                                description = document.getString("description") ?: "",
                                amount = document.getDouble("amount") ?: 0.0,
                                category = document.getString("category") ?: "",
                                timestamp = document.getLong("timestamp") ?: 0L
                            )
                            transactions.add(transaction)
                        }
                        updateUI()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        loadDefaultTransactions()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error loading transactions: ${exception.message}",
                        Toast.LENGTH_SHORT).show()
                    // Load default data if Firebase fails
                    loadDefaultTransactions()
                }
        } catch (e: Exception) {
            e.printStackTrace()
            loadDefaultTransactions()
        }
    }

    private fun loadDefaultTransactions() {
        try {
            // Fallback data matching your XML
            transactions.clear()
            transactions.addAll(listOf(
                Transaction("", "28.01.25", "LIGHTS", 1500.00, categoryName, System.currentTimeMillis()),
                Transaction("", "30.01.25", "WATER", 2500.00, categoryName, System.currentTimeMillis()),
                Transaction("", "28.02.25", "LIGHTS", 1500.00, categoryName, System.currentTimeMillis())
            ))
            updateUI()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading default data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        try {
            calculateTotal()
            updateTotalDisplay()
            // Note: Transaction list is static in XML, but you could make it dynamic
            // by replacing the static LinearLayout with a RecyclerView
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error updating UI: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateTotal() {
        currentTotal = transactions.sumOf { it.amount }
    }

    private fun updateTotalDisplay() {
        try {
            totalAmount.text = "TOTAL:        R ${String.format("%.2f", currentTotal)}"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openMenu() {
        try {
            // Simple implementation - just go back for now
            Toast.makeText(this, "Going back to previous screen", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    private fun addNewTransaction() {
        try {
            showAddTransactionDialog()
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening add transaction dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddTransactionDialog() {
        try {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Add Transaction")

            // Create input fields programmatically
            val container = LinearLayout(this)
            container.orientation = LinearLayout.VERTICAL
            container.setPadding(50, 40, 50, 10)

            val descInput = android.widget.EditText(this)
            descInput.hint = "Description (e.g., WATER, LIGHTS)"
            container.addView(descInput)

            val amountInput = android.widget.EditText(this)
            amountInput.hint = "Amount"
            amountInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            container.addView(amountInput)

            builder.setView(container)

            builder.setPositiveButton("Add") { _, _ ->
                val description = descInput.text.toString().trim()
                val amountStr = amountInput.text.toString().trim()

                if (description.isNotEmpty() && amountStr.isNotEmpty()) {
                    try {
                        val amount = amountStr.toDouble()
                        val currentDate = java.text.SimpleDateFormat("dd.MM.yy", Locale.getDefault())
                            .format(Date())

                        saveTransactionToFirebase(currentDate, description, amount)
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }

            builder.setNegativeButton("Cancel", null)
            builder.show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error creating dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveTransactionToFirebase(date: String, description: String, amount: Double) {
        try {
            val transaction = hashMapOf(
                "date" to date,
                "description" to description,
                "amount" to amount,
                "category" to categoryName,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("transactions")
                .add(transaction)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(this, "Transaction added successfully", Toast.LENGTH_SHORT).show()
                    // Reload transactions
                    loadTransactionsFromFirebase()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error adding transaction: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving transaction: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showChartDetails() {
        Toast.makeText(this, "Chart functionality coming soon", Toast.LENGTH_SHORT).show()
    }

    // Data class for transactions
    data class Transaction(
        val id: String = "",
        val date: String,
        val description: String,
        val amount: Double,
        val category: String,
        val timestamp: Long
    )

    override fun onResume() {
        super.onResume()
        try {
            // Refresh data when returning to activity
            loadTransactionsFromFirebase()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}