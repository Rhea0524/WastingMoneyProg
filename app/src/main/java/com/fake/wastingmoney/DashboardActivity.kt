package com.fake.wastingmoney

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fake.wastingmoney.model.Expense
import com.fake.wastingmoney.model.Income
import com.fake.wastingmoney.model.TransactionItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var chartContainer: LinearLayout
    private lateinit var monthSpinner: Spinner
    private lateinit var goalInput: EditText
    private lateinit var timeLimitInput: EditText
    private lateinit var setGoalButton: Button
    private lateinit var menuIcon: LinearLayout

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    private val categoryExpenses = mutableMapOf<String, Double>()
    private val categoryBudgets = mutableMapOf<String, Double>()
    private val transactionsList = mutableListOf<TransactionItem>()

    companion object {
        private const val TAG = "DashboardActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        Log.d(TAG, "onCreate called")
        try {
            initializeViews()
            initializeFirebase()
            setupSpinner()
            setupMenuListener()
            setupGoalButton()
            loadTransactionData()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading dashboard: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        chartContainer = findViewById(R.id.chartContainer)
        monthSpinner = findViewById(R.id.monthSpinner)
        goalInput = findViewById(R.id.goalInput)
        timeLimitInput = findViewById(R.id.timeLimitInput)
        setGoalButton = findViewById(R.id.setGoalButton)
        menuIcon = findViewById(R.id.menuIcon)

        Log.d(TAG, "Views initialized successfully")
    }

    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        Log.d(TAG, "Firebase initialized")
    }

    private fun setupSpinner() {
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = adapter

        // Set current month as default
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        monthSpinner.setSelection(currentMonth)

        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadTransactionData(position + 1) // Month is 1-based
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupMenuListener() {
        menuIcon.setOnClickListener {
            showMenuDialog()
        }
    }

    private fun setupGoalButton() {
        setGoalButton.setOnClickListener {
            val goalAmount = goalInput.text.toString().toDoubleOrNull()
            val timeLimit = timeLimitInput.text.toString().toIntOrNull()

            if (goalAmount != null && timeLimit != null) {
                saveGoalToFirebase(goalAmount, timeLimit)
            } else {
                Toast.makeText(this, "Please enter valid goal amount and time limit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveGoalToFirebase(goalAmount: Double, timeLimit: Int) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to set goals", Toast.LENGTH_SHORT).show()
            return
        }

        val goalData = mapOf(
            "amount" to goalAmount,
            "timeLimit" to timeLimit,
            "timestamp" to System.currentTimeMillis()
        )

        database.getReference("goals").child(currentUser.uid)
            .setValue(goalData)
            .addOnSuccessListener {
                Toast.makeText(this, "Goal saved successfully!", Toast.LENGTH_SHORT).show()
                goalInput.setText("")
                timeLimitInput.setText("")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save goal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTransactionData(selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated")
            Toast.makeText(this, "Please login to view dashboard", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = currentUser.uid
        Log.d(TAG, "Loading transactions for UID: $uid, Month: $selectedMonth")

        // Clear previous data
        categoryExpenses.clear()
        transactionsList.clear()

        // Load transactions and budgets with proper synchronization
        loadTransactionsAndBudgets(uid, selectedMonth)
    }

    private fun loadTransactionsAndBudgets(uid: String, month: Int) {
        var transactionsLoaded = false
        var budgetsLoaded = false

        fun checkAndUpdateChart() {
            if (transactionsLoaded && budgetsLoaded) {
                Log.d(TAG, "Both transactions and budgets loaded, updating chart")
                updateChartDisplay()
            }
        }

        // Load transactions
        val transactionsRef = database.getReference("transactions").child(uid)
        transactionsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Transaction snapshot received. Total children: ${snapshot.childrenCount}")

                categoryExpenses.clear()
                transactionsList.clear()

                for (transactionSnapshot in snapshot.children) {
                    try {
                        val transaction = transactionSnapshot.getValue(TransactionItem::class.java)
                        transaction?.let {
                            Log.d(TAG, "Processing transaction: Type='${it.type}', Category='${it.category}', Amount=${it.amount}, Date='${it.date}'")

                            // Check if transaction is in the selected month
                            if (isDateInMonth(it.date, month)) {
                                transactionsList.add(it)
                                Log.d(TAG, "Transaction added to list for month $month")

                                // Count expenses for budget comparison (case insensitive)
                                if (it.type.uppercase().trim() == "EXPENSE") {
                                    val currentAmount = categoryExpenses[it.category] ?: 0.0
                                    categoryExpenses[it.category] = currentAmount + it.amount
                                    Log.d(TAG, "Added expense: ${it.category} = ${it.amount} (total: ${categoryExpenses[it.category]})")
                                } else {

                                }
                            } else {
                                Log.d(TAG, "Transaction not in month $month: ${it.date}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction: ${e.message}", e)
                    }
                }

                Log.d(TAG, "Final - Total transactions in month: ${transactionsList.size}")
                Log.d(TAG, "Final - Total expense categories: ${categoryExpenses.size}")
                Log.d(TAG, "Final - Category expenses: $categoryExpenses")

                transactionsLoaded = true
                checkAndUpdateChart()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load transactions: ${error.message}")
                Toast.makeText(this@DashboardActivity, "Failed to load transactions: ${error.message}", Toast.LENGTH_SHORT).show()
                transactionsLoaded = true
                checkAndUpdateChart()
            }
        })

        // Load budgets
        loadBudgetsFromFirebase(uid) {
            budgetsLoaded = true
            checkAndUpdateChart()
        }
    }

    // Updated budget loading method with callback
    private fun loadBudgetsFromFirebase(uid: String, onComplete: () -> Unit = {}) {
        val budgetsRef = database.getReference("budgets").child(uid)

        budgetsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Budget snapshot received. Total children: ${snapshot.childrenCount}")

                categoryBudgets.clear()

                if (snapshot.exists()) {
                    for (budgetSnapshot in snapshot.children) {
                        try {
                            val category = budgetSnapshot.key ?: continue
                            val amount = budgetSnapshot.getValue(Double::class.java) ?: 0.0
                            categoryBudgets[category] = amount
                            Log.d(TAG, "Added budget: $category = $amount")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing budget: ${e.message}", e)
                        }
                    }
                } else {
                    Log.d(TAG, "No budgets found, creating default budgets")
                    createDefaultBudgets()
                }

                Log.d(TAG, "Final - Total categories with budgets: ${categoryBudgets.size}")
                Log.d(TAG, "Final - Category budgets: $categoryBudgets")

                onComplete()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load budgets: ${error.message}")
                createDefaultBudgets()
                onComplete()
            }
        })
    }

    // Keep the existing loadExpensesForMonth as fallback if you still have separate expense nodes
    private fun loadExpensesForMonth(uid: String, month: Int) {
        val expensesRef = database.getReference("expenses").child(uid)

        expensesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Expense snapshot received for month $month")

                categoryExpenses.clear()

                for (expenseSnapshot in snapshot.children) {
                    try {
                        val expense = expenseSnapshot.getValue(Expense::class.java)
                        expense?.let {
                            // Parse date and check if it's in the selected month
                            if (isDateInMonth(it.date, month)) {
                                val currentAmount = categoryExpenses[it.category] ?: 0.0
                                categoryExpenses[it.category] = currentAmount + it.amount

                                Log.d(TAG, "Added expense: ${it.category} = ${it.amount}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing expense: ${e.message}", e)
                    }
                }

                Log.d(TAG, "Total categories with expenses: ${categoryExpenses.size}")
                updateChartDisplay()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load expenses: ${error.message}")
                Toast.makeText(this@DashboardActivity, "Failed to load expenses", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createDefaultBudgets() {
        // Set default budgets for common categories
        categoryBudgets["GROCERIES"] = 1000.0
        categoryBudgets["TRANSPORT"] = 800.0
        categoryBudgets["ENTERTAINMENT"] = 500.0
        categoryBudgets["UTILITIES"] = 600.0
        categoryBudgets["CLOTHES"] = 400.0

        Log.d(TAG, "Created default budgets: $categoryBudgets")
    }

    // Improved date parsing method
    private fun isDateInMonth(dateString: String, targetMonth: Int): Boolean {
        return try {
            Log.d(TAG, "Checking if date '$dateString' is in month $targetMonth")

            // Try multiple date formats
            val formats = arrayOf(
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "yyyy/MM/dd",
                "dd-MM-yyyy",
                "MM-dd-yyyy"
            )

            var date: Date? = null
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    date = sdf.parse(dateString)
                    if (date != null) {
                        Log.d(TAG, "Successfully parsed date with format: $format")
                        break
                    }
                } catch (e: Exception) {
                    // Try next format
                }
            }

            if (date == null) {
                Log.e(TAG, "Could not parse date: $dateString")
                return false
            }

            val calendar = Calendar.getInstance()
            calendar.time = date
            val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based

            Log.d(TAG, "Date '$dateString' is in month $month, target month is $targetMonth")
            month == targetMonth
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $dateString", e)
            false
        }
    }

    private fun updateChartDisplay() {
        try {
            Log.d(TAG, "Updating chart display")
            Log.d(TAG, "Category expenses: $categoryExpenses")
            Log.d(TAG, "Category budgets: $categoryBudgets")

            // Clear existing chart views
            chartContainer.removeAllViews()

            // Get all unique categories from both expenses and budgets
            val allCategories = (categoryExpenses.keys + categoryBudgets.keys).toSet()
            Log.d(TAG, "All categories: $allCategories")

            if (allCategories.isEmpty()) {
                Log.d(TAG, "No categories found, showing no data message")
                showNoDataMessage()
                return
            }

            // Create chart for each category
            for (category in allCategories.sorted()) {
                val actualAmount = categoryExpenses[category] ?: 0.0
                val budgetAmount = categoryBudgets[category] ?: (actualAmount * 1.2) // Default budget 20% higher than actual

                Log.d(TAG, "Creating chart for category: $category, actual: $actualAmount, budget: $budgetAmount")
                createCategoryChart(category, actualAmount, budgetAmount)
            }

            Log.d(TAG, "Chart display updated with ${allCategories.size} categories")

        } catch (e: Exception) {
            Log.e(TAG, "Error updating chart display: ${e.message}", e)
            Toast.makeText(this, "Error updating chart: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCategoryChart(category: String, actualAmount: Double, budgetAmount: Double) {
        val categoryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        // Category title
        val titleTextView = TextView(this).apply {
            text = category.uppercase()
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
        }

        // Chart container
        val chartFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                48 // Height in dp converted to pixels
            )
        }

        // Calculate bar widths (max width = screen width - margins)
        val maxWidth = resources.displayMetrics.widthPixels - (32 * resources.displayMetrics.density).toInt()
        val maxAmount = maxOf(actualAmount, budgetAmount)

        val actualWidth = if (maxAmount > 0) {
            ((actualAmount / maxAmount) * maxWidth * 0.8).toInt()
        } else {
            0
        }
        val budgetWidth = if (maxAmount > 0) {
            ((budgetAmount / maxAmount) * maxWidth * 0.8).toInt()
        } else {
            0
        }

        // Actual amount bar (background)
        val actualBar = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(actualWidth, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#2D5A41"))
        }

        // Budget bar
        val budgetBar = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(budgetWidth, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(
                when {
                    actualAmount > budgetAmount -> Color.parseColor("#FF6B6B") // Red for overspend
                    actualAmount > budgetAmount * 0.8 -> Color.parseColor("#FFA500") // Orange for near budget
                    else -> Color.parseColor("#4CAF50") // Green for under budget
                }
            )
        }

        // Add bars to frame
        chartFrame.addView(actualBar)
        chartFrame.addView(budgetBar)

        // Amount labels
        val amountLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 0)
            }
        }

        val actualAmountText = TextView(this).apply {
            text = "Actual: R${String.format("%.2f", actualAmount)}"
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val budgetAmountText = TextView(this).apply {
            text = "Budget: R${String.format("%.2f", budgetAmount)}"
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.END
        }

        amountLayout.addView(actualAmountText)
        amountLayout.addView(budgetAmountText)

        // Add all views to category layout
        categoryLayout.addView(titleTextView)
        categoryLayout.addView(chartFrame)
        categoryLayout.addView(amountLayout)

        // Add to main container
        chartContainer.addView(categoryLayout)
    }

    private fun showNoDataMessage() {
        val messageLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(32, 64, 32, 64)
            }
        }

        val messageText = TextView(this).apply {
            text = "No transaction data available for the selected month.\n\nAdd some expenses to see your budget dashboard!"
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 16f
            gravity = android.view.Gravity.CENTER
        }

        val addExpenseButton = Button(this).apply {
            text = "Add Expense"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#4CAF50"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 24, 0, 0)
            }
            setOnClickListener {
                try {
                    val intent = Intent(this@DashboardActivity, AddExpense::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@DashboardActivity, "Add Expense activity not found", Toast.LENGTH_SHORT).show()
                }
            }
        }

        messageLayout.addView(messageText)
        messageLayout.addView(addExpenseButton)
        chartContainer.addView(messageLayout)
    }

    private fun showMenuDialog() {
        val menuOptions = arrayOf(
            "🏠 Home",
            "📊 Dashboard",
            "💰 Add Income",
            "💸 Add Expense",
            "🎯 Budget Goal",
            "📂 Categories",
            "🔧 Manage Categories",
            "📋 Category Details",
            "📝 Transactions",
            "🚪 Logout"
        )

        AlertDialog.Builder(this)
            .setTitle("Navigation Menu")
            .setItems(menuOptions) { _, which ->
                when (which) {
                    0 -> navigateToHome()
                    1 -> navigateToDashboard()
                    2 -> navigateToAddIncome()
                    3 -> navigateToAddExpense()
                    4 -> navigateToBudgetGoal()
                    5 -> navigateToCategories()
                    6 -> navigateToManageCategories()
                    7 -> navigateToCategoryDetail()
                    8 -> navigateToTransactions()
                    9 -> logout()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Navigation methods
    private fun navigateToHome() {
        try {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Home activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToDashboard() {
        Toast.makeText(this, "You are already on the Dashboard", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToAddIncome() {
        try {
            val intent = Intent(this, AddIncome::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Add Income activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToAddExpense() {
        try {
            val intent = Intent(this, AddExpense::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Add Expense activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToBudgetGoal() {
        try {
            val intent = Intent(this, BudgetGoal::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Budget Goal activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToCategories() {
        try {
            val intent = Intent(this, Categories::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Categories activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToManageCategories() {
        try {
            val intent = Intent(this, ManageCategoriesActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Manage Categories activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToCategoryDetail() {
        try {
            val intent = Intent(this, CategoryDetail::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Category Detail activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToTransactions() {
        try {
            val intent = Intent(this, Transaction::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Transactions activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        try {
            auth.signOut()
            val sharedPrefs = getSharedPreferences("user_session", MODE_PRIVATE)
            sharedPrefs.edit().clear().apply()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout: ${e.message}", e)
            Toast.makeText(this, "Error during logout", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload data when returning to the activity
        val selectedMonth = monthSpinner.selectedItemPosition + 1
        loadTransactionData(selectedMonth)
    }
}