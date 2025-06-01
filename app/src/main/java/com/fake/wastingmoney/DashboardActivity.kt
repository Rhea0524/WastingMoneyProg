package com.fake.wastingmoney

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fake.wastingmoney.model.Income
import com.fake.wastingmoney.model.Expense
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
    private val categoryIncome = mutableMapOf<String, Double>()
    private val categoryGoals = mutableMapOf<String, GoalData>()

    data class GoalData(
        val minGoal: Double = 0.0,
        val maxGoal: Double = 0.0,
        val category: String = "",
        val timeLimit: Int = 30
    )

    companion object {
        private const val TAG = "DashboardActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        Log.d(TAG, "onCreate called")
        initializeViews()
        initializeFirebase()
        setupSpinner()
        setupMenuListener()
        setupGoalButton()
        loadTransactionData()
        loadGoalsData()
    }

    private fun initializeViews() {
        chartContainer = findViewById(R.id.chartContainer)
        monthSpinner = findViewById(R.id.monthSpinner)
        setGoalButton = findViewById(R.id.setGoalButton)
        menuIcon = findViewById(R.id.menuIcon)
    }

    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
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
            showGoalSetupDialog()
        }
    }

    private fun showGoalSetupDialog() {
        val dialogView = layoutInflater.inflate(android.R.layout.select_dialog_item, null)
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val categorySpinner = Spinner(this).apply {
            val categories = arrayOf("GROCERIES", "TRANSPORT", "ENTERTAINMENT", "UTILITIES", "CLOTHES", "TOILETRIES", "LIGHTS","CAR", "HEALTHCARE", "SHOPPING", "BILLS","SALARY","GIFT","INVESTMENT","OTHER")
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, categories).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        val minGoalInput = EditText(this).apply {
            hint = "Minimum Goal Amount (R)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val maxGoalInput = EditText(this).apply {
            hint = "Maximum Goal Amount (R)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val timeLimitInput = EditText(this).apply {
            hint = "Time Limit (days)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        dialogLayout.addView(TextView(this).apply {
            text = "Category:"
            setTextColor(Color.BLACK)
        })
        dialogLayout.addView(categorySpinner)
        dialogLayout.addView(TextView(this).apply {
            text = "Minimum Goal:"
            setTextColor(Color.BLACK)
        })
        dialogLayout.addView(minGoalInput)
        dialogLayout.addView(TextView(this).apply {
            text = "Maximum Goal:"
            setTextColor(Color.BLACK)
        })
        dialogLayout.addView(maxGoalInput)
        dialogLayout.addView(TextView(this).apply {
            text = "Time Limit:"
            setTextColor(Color.BLACK)
        })
        dialogLayout.addView(timeLimitInput)

        AlertDialog.Builder(this)
            .setTitle("Set Category Goals")
            .setView(dialogLayout)
            .setPositiveButton("Save") { _, _ ->
                val category = categorySpinner.selectedItem.toString()
                val minGoal = minGoalInput.text.toString().toDoubleOrNull()
                val maxGoal = maxGoalInput.text.toString().toDoubleOrNull()
                val timeLimit = timeLimitInput.text.toString().toIntOrNull()

                if (minGoal != null && maxGoal != null && timeLimit != null && minGoal <= maxGoal) {
                    saveGoalToFirebase(category, minGoal, maxGoal, timeLimit)
                } else {
                    Toast.makeText(this, "Please enter valid goals (min ≤ max)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveGoalToFirebase(category: String, minGoal: Double, maxGoal: Double, timeLimit: Int) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to set goals", Toast.LENGTH_SHORT).show()
            return
        }

        val goalData = mapOf(
            "category" to category,
            "minGoal" to minGoal,
            "maxGoal" to maxGoal,
            "timeLimit" to timeLimit,
            "timestamp" to System.currentTimeMillis()
        )

        database.getReference("categoryGoals").child(currentUser.uid).child(category)
            .setValue(goalData)
            .addOnSuccessListener {
                Toast.makeText(this, "Goal saved successfully!", Toast.LENGTH_SHORT).show()
                loadGoalsData() // Reload goals and refresh display
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save goal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadGoalsData() {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        val goalsRef = database.getReference("categoryGoals").child(currentUser.uid)

        goalsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryGoals.clear()

                for (goalSnapshot in snapshot.children) {
                    try {
                        val category = goalSnapshot.child("category").getValue(String::class.java) ?: ""
                        val minGoal = goalSnapshot.child("minGoal").getValue(Double::class.java) ?: 0.0
                        val maxGoal = goalSnapshot.child("maxGoal").getValue(Double::class.java) ?: 0.0
                        val timeLimit = goalSnapshot.child("timeLimit").getValue(Int::class.java) ?: 30

                        categoryGoals[category] = GoalData(minGoal, maxGoal, category, timeLimit)
                        Log.d(TAG, "Loaded goal for $category: min=$minGoal, max=$maxGoal")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing goal: ${e.message}", e)
                    }
                }

                // Update chart display with goals
                updateChartDisplay()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load goals: ${error.message}")
            }
        })
    }

    private fun loadTransactionData(selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to view dashboard", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Loading transactions for month: $selectedMonth")

        // Clear previous data
        categoryExpenses.clear()
        categoryIncome.clear()

        // Load both incomes and expenses
        loadIncomesForMonth(currentUser.uid, selectedMonth)
        loadExpensesForMonth(currentUser.uid, selectedMonth)
    }

    private fun loadIncomesForMonth(uid: String, selectedMonth: Int) {
        val incomesRef = database.getReference("incomes").child(uid)

        incomesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "=== INCOME DEBUG ===")
                Log.d(TAG, "Total incomes in Firebase: ${snapshot.childrenCount}")

                var incomeCount = 0

                for (incomeSnapshot in snapshot.children) {
                    try {
                        val income = incomeSnapshot.getValue(Income::class.java)
                        income?.let {
                            Log.d(TAG, "Income found: ${it.description}, Amount: ${it.amount}, Date: ${it.date}, Source: ${it.source}")

                            if (isDateInMonth(it.date, selectedMonth)) {
                                incomeCount++
                                val currentAmount = categoryIncome[it.source] ?: 0.0
                                categoryIncome[it.source] = currentAmount + it.amount
                                Log.d(TAG, "Added income to category '${it.source}': ${it.amount}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing income: ${e.message}", e)
                    }
                }

                Log.d(TAG, "Income processed for month $selectedMonth: $incomeCount items")
                Log.d(TAG, "Final income by category: $categoryIncome")

                // Update chart after loading incomes
                updateChartDisplay()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load incomes: ${error.message}")
                Toast.makeText(this@DashboardActivity, "Failed to load income data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadExpensesForMonth(uid: String, selectedMonth: Int) {
        val expensesRef = database.getReference("expenses").child(uid)

        expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "=== EXPENSE DEBUG ===")
                Log.d(TAG, "Total expenses in Firebase: ${snapshot.childrenCount}")

                var expenseCount = 0

                for (expenseSnapshot in snapshot.children) {
                    try {
                        val expense = expenseSnapshot.getValue(Expense::class.java)
                        expense?.let {
                            Log.d(TAG, "Expense found: ${it.description}, Amount: ${it.amount}, Date: ${it.date}, Category: ${it.category}")

                            if (isDateInMonth(it.date, selectedMonth)) {
                                expenseCount++
                                val currentAmount = categoryExpenses[it.category] ?: 0.0
                                categoryExpenses[it.category] = currentAmount + it.amount
                                Log.d(TAG, "Added expense to category '${it.category}': ${it.amount}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing expense: ${e.message}", e)
                    }
                }

                Log.d(TAG, "Expenses processed for month $selectedMonth: $expenseCount items")
                Log.d(TAG, "Final expenses by category: $categoryExpenses")

                // Update chart after loading expenses
                updateChartDisplay()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load expenses: ${error.message}")
                Toast.makeText(this@DashboardActivity, "Failed to load expense data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun isDateInMonth(dateString: String, targetMonth: Int): Boolean {
        return try {
            Log.d(TAG, "Checking date '$dateString' for month $targetMonth")

            val formats = arrayOf(
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "dd-MM-yyyy",
                "yyyy/MM/dd",
                "MMM dd, yyyy",
                "dd MMM yyyy"
            )

            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    val date = sdf.parse(dateString)
                    if (date != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = date
                        val month = calendar.get(Calendar.MONTH) + 1
                        Log.d(TAG, "Date '$dateString' parsed with format '$format' -> month $month")
                        return month == targetMonth
                    }
                } catch (e: Exception) {
                    // Try next format
                }
            }

            // If no format worked, try to extract month number directly
            val monthPattern = Regex("""(\d{1,2})[/-]""")
            val match = monthPattern.find(dateString)
            if (match != null) {
                val extractedMonth = match.groupValues[1].toIntOrNull()
                Log.d(TAG, "Extracted month from '$dateString': $extractedMonth")
                if (extractedMonth != null && extractedMonth == targetMonth) {
                    return true
                }
            }

            Log.e(TAG, "Could not parse date: '$dateString'")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $dateString", e)
            false
        }
    }

    private fun updateChartDisplay() {
        chartContainer.removeAllViews()

        // Show summary at the top
        createSummaryView()

        if (categoryExpenses.isEmpty() && categoryIncome.isEmpty()) {
            showNoDataMessage()
            return
        }

        // Create charts for expense categories with goals
        if (categoryExpenses.isNotEmpty()) {
            addSectionHeader("EXPENSES")
            for ((category, expenseAmount) in categoryExpenses.toList().sortedByDescending { it.second }) {
                val goalData = categoryGoals[category.uppercase()]
                createCategoryChartWithGoals(category, expenseAmount, goalData, "Expense")
            }
        }

        // Create charts for income categories
        if (categoryIncome.isNotEmpty()) {
            addSectionHeader("INCOME")
            for ((category, incomeAmount) in categoryIncome.toList().sortedByDescending { it.second }) {
                createIncomeChart(category, incomeAmount)
            }
        }

        // Show goal summary if goals exist
        if (categoryGoals.isNotEmpty()) {
            addSectionHeader("GOAL SUMMARY")
            createGoalSummaryView()
        }
    }

    private fun createSummaryView() {
        val summaryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            setBackgroundColor(Color.parseColor("#2D5A47"))
            setPadding(16, 16, 16, 16)
        }

        val totalIncome = categoryIncome.values.sum()
        val totalExpenses = categoryExpenses.values.sum()
        val netAmount = totalIncome - totalExpenses

        val summaryText = TextView(this).apply {
            text = """
                Monthly Summary
                Income: R${String.format("%.2f", totalIncome)}
                Expenses: R${String.format("%.2f", totalExpenses)}
                Net: ${if (netAmount >= 0) "+" else "-"}R${String.format("%.2f", Math.abs(netAmount))}
            """.trimIndent()
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        summaryLayout.addView(summaryText)
        chartContainer.addView(summaryLayout)
    }

    private fun createGoalSummaryView() {
        val summaryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            setBackgroundColor(Color.parseColor("#3A4D5A"))
            setPadding(16, 16, 16, 16)
        }

        val titleText = TextView(this).apply {
            text = "Goal Performance"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        summaryLayout.addView(titleText)

        for ((category, goalData) in categoryGoals) {
            val actualExpense = categoryExpenses[category] ?: 0.0
            val status = when {
                actualExpense <= goalData.minGoal -> "✅ Under Min Goal"
                actualExpense <= goalData.maxGoal -> "⚠️ Within Range"
                else -> "❌ Over Max Goal"
            }

            val goalText = TextView(this).apply {
                text = """
                    $category: $status
                    Spent: R${String.format("%.2f", actualExpense)} | Min: R${String.format("%.2f", goalData.minGoal)} | Max: R${String.format("%.2f", goalData.maxGoal)}
                """.trimIndent()
                setTextColor(Color.WHITE)
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 8)
                }
            }

            summaryLayout.addView(goalText)
        }

        chartContainer.addView(summaryLayout)
    }

    private fun addSectionHeader(title: String) {
        val headerText = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 8)
            }
        }
        chartContainer.addView(headerText)
    }

    private fun createIncomeChart(category: String, amount: Double) {
        val categoryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
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
                48
            )
        }

        // Calculate bar width based on amount
        val maxWidth = resources.displayMetrics.widthPixels - (64 * resources.displayMetrics.density).toInt()
        val maxAmount = categoryIncome.values.maxOrNull() ?: amount
        val barWidth = if (maxAmount > 0) {
            ((amount / maxAmount) * maxWidth * 0.8).toInt()
        } else 0

        // Income bar (always green)
        val incomeBar = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(barWidth, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#4CAF50")) // Green for income
        }

        chartFrame.addView(incomeBar)

        // Amount label
        val amountText = TextView(this).apply {
            text = "R${String.format("%.2f", amount)}"
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 0)
            }
        }

        categoryLayout.addView(titleTextView)
        categoryLayout.addView(chartFrame)
        categoryLayout.addView(amountText)

        chartContainer.addView(categoryLayout)
    }

    private fun createCategoryChartWithGoals(category: String, actualAmount: Double, goalData: GoalData?, type: String) {
        val categoryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        // Category title with goal status
        val goalStatus = if (goalData != null) {
            when {
                actualAmount <= goalData.minGoal -> " ✅"
                actualAmount <= goalData.maxGoal -> " ⚠️"
                else -> " ❌"
            }
        } else ""

        val titleTextView = TextView(this).apply {
            text = "${category.uppercase()}$goalStatus"
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
                48
            )
        }

        // Calculate maximum for chart scaling
        val maxAmount = if (goalData != null) {
            maxOf(actualAmount, goalData.maxGoal, goalData.minGoal)
        } else {
            actualAmount
        }

        val maxWidth = resources.displayMetrics.widthPixels - (64 * resources.displayMetrics.density).toInt()

        // Create goal range background if goals exist
        if (goalData != null) {
            val minGoalWidth = ((goalData.minGoal / maxAmount) * maxWidth * 0.8).toInt()
            val maxGoalWidth = ((goalData.maxGoal / maxAmount) * maxWidth * 0.8).toInt()

            // Min goal bar (light green background)
            val minGoalBar = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(minGoalWidth, FrameLayout.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.parseColor("#90EE90")) // Light green
                alpha = 0.3f
            }

            // Max goal bar (yellow background)
            val maxGoalBar = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(maxGoalWidth, FrameLayout.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.parseColor("#FFFF99")) // Light yellow
                alpha = 0.3f
            }

            chartFrame.addView(minGoalBar)
            chartFrame.addView(maxGoalBar)
        }

        // Actual amount bar
        val actualWidth = ((actualAmount / maxAmount) * maxWidth * 0.8).toInt()
        val actualBar = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(actualWidth, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(
                if (goalData != null) {
                    when {
                        actualAmount > goalData.maxGoal -> Color.parseColor("#FF6B6B") // Red for over max
                        actualAmount > goalData.minGoal -> Color.parseColor("#FFA500") // Orange for within range
                        else -> Color.parseColor("#4CAF50") // Green for under min
                    }
                } else {
                    Color.parseColor("#4CAF50") // Default green
                }
            )
        }

        chartFrame.addView(actualBar)

        // Amount labels
        val amountLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
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
        }

        amountLayout.addView(actualAmountText)

        if (goalData != null) {
            val goalRangeText = TextView(this).apply {
                text = "Goal Range: R${String.format("%.2f", goalData.minGoal)} - R${String.format("%.2f", goalData.maxGoal)}"
                setTextColor(Color.parseColor("#CCCCCC"))
                textSize = 10f
            }
            amountLayout.addView(goalRangeText)
        }

        categoryLayout.addView(titleTextView)
        categoryLayout.addView(chartFrame)
        categoryLayout.addView(amountLayout)

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
            text = "No transaction data for the selected month.\n\nAdd some transactions to see your budget dashboard!"
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 16f
            gravity = android.view.Gravity.CENTER
        }

        val addTransactionButton = Button(this).apply {
            text = "Add Transaction"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#4CAF50"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 24, 0, 0)
            }
            setOnClickListener {
                navigateToTransactions()
            }
        }

        messageLayout.addView(messageText)
        messageLayout.addView(addTransactionButton)
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
            "Category details",
            "📝 Transactions",
            "🚪 Logout"
        )

        AlertDialog.Builder(this)
            .setTitle("Navigation Menu")
            .setItems(menuOptions) { _, which ->
                when (which) {
                    0 -> navigateToHome()
                    1 -> Toast.makeText(this, "You are already on Dashboard", Toast.LENGTH_SHORT).show()
                    2 -> navigateToAddIncome()
                    3 -> navigateToAddExpense()
                    4 -> navigateToBudgetGoal()
                    5 -> navigateToCategories()
                //    6 -> navigateToCategoryDetail()
                    6 -> navigateToTransactions()
                    7 -> logout()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Navigation methods
    private fun navigateToHome() {
        startActivity(Intent(this, Home::class.java))
    }

    private fun navigateToAddIncome() {
        startActivity(Intent(this, AddIncome::class.java))
    }

    private fun navigateToAddExpense() {
        startActivity(Intent(this, AddExpense::class.java))
    }

    private fun navigateToBudgetGoal() {
        startActivity(Intent(this, BudgetGoal::class.java))
    }

    private fun navigateToCategories() {
        startActivity(Intent(this, Categories::class.java))
    }

  //  private fun navigateToCategoryDetail() {
  //      startActivity(Intent(this, Categories::class.java))
 //   }

    private fun navigateToTransactions() {
        startActivity(Intent(this, Transaction::class.java))
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        val selectedMonth = monthSpinner.selectedItemPosition + 1
        loadTransactionData(selectedMonth)
        loadGoalsData()
    }
}