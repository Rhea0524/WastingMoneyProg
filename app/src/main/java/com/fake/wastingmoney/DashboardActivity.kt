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
        chartContainer = findViewById(R.id.categoriesContainer) // Changed from chartContainer to categoriesContainer
        monthSpinner = findViewById(R.id.monthSpinner)
        setGoalButton = findViewById(R.id.setGoalButton)
        menuIcon = findViewById(R.id.menuButton) // Changed from menuIcon to menuButton

        // Add the missing addExpenseButton
        val addExpenseButton: Button = findViewById(R.id.addExpenseButton)
        addExpenseButton.setOnClickListener {
            navigateToAddExpense()
        }
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

        // Use counters to track completion
        var loadingCount = 2 // We're loading 2 things: incomes and expenses

        fun checkAndUpdateDisplay() {
            loadingCount--
            if (loadingCount == 0) {
                // Both incomes and expenses are loaded, now update display
                updateChartDisplay()
            }
        }

// Load incomes and expenses with completion callbacks
        loadIncomesForMonth(currentUser.uid, selectedMonth) { checkAndUpdateDisplay() }
        loadExpensesForMonth(currentUser.uid, selectedMonth) { checkAndUpdateDisplay() }
    }

    private fun loadIncomesForMonth(uid: String, selectedMonth: Int, onComplete: () -> Unit) {
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
                onComplete() // Call completion callback instead of updateChartDisplay
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load incomes: ${error.message}")
                Toast.makeText(this@DashboardActivity, "Failed to load income data", Toast.LENGTH_SHORT).show()
                onComplete() // Still call completion even on error
            }

        })
    }

    private fun loadExpensesForMonth(uid: String, selectedMonth: Int, onComplete: () -> Unit) {
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
                onComplete() // Call completion callback instead of updateChartDisplay
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load expenses: ${error.message}")
                Toast.makeText(this@DashboardActivity, "Failed to load expense data", Toast.LENGTH_SHORT).show()
                onComplete() // Still call completion even on error
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
        // Clear only the dynamic category views, not the static legend
        val categoriesContainer = findViewById<LinearLayout>(R.id.categoriesContainer)
        categoriesContainer.removeAllViews()



        if (categoryExpenses.isEmpty() && categoryIncome.isEmpty()) {
            showNoDataMessage()
            return
        }

        // Create charts for expense categories with goals
        if (categoryExpenses.isNotEmpty()) {
            addSectionHeader("EXPENSES")
            for ((category, expenseAmount) in categoryExpenses.toList()
                .sortedByDescending { it.second }) {
                val goalData = categoryGoals[category.uppercase()]
                createCategoryChartWithGoals(category, expenseAmount, goalData, "Expense")
            }
        }

        // Create charts for income categories
        if (categoryIncome.isNotEmpty()) {
            addSectionHeader("INCOME")
            for ((category, incomeAmount) in categoryIncome.toList()
                .sortedByDescending { it.second }) {
                createIncomeChart(category, incomeAmount)
            }
        }
    }







    private fun addSectionHeader(title: String) {
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 24, 0, 16)
            }
            setBackgroundColor(Color.parseColor("#2196F3"))
            setPadding(24, 16, 24, 16)
            elevation = 1f
        }

        val headerText = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        headerLayout.addView(headerText)

        val categoriesContainer = findViewById<LinearLayout>(R.id.categoriesContainer)
        categoriesContainer.addView(headerLayout)
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
            setBackgroundColor(Color.WHITE)
            setPadding(24, 24, 24, 24)
            elevation = 1f
            isClickable = true
            isFocusable = true
            foreground = getDrawable(android.R.drawable.list_selector_background)
        }

        // Category title with emoji
        val emoji = when(category.uppercase()) {
            "SALARY" -> "💼"
            "GIFT" -> "🎁"
            "INVESTMENT" -> "📈"
            "OTHER" -> "💰"
            else -> "💰"
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        val titleTextView = TextView(this).apply {
            text = "$emoji ${category.uppercase()}"
            setTextColor(Color.parseColor("#1A1A1A"))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val amountLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.END
        }

        val amountText = TextView(this).apply {
            text = "R${String.format("%.2f", amount)}"
            setTextColor(Color.parseColor("#1A1A1A"))
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        amountLayout.addView(amountText)
        headerLayout.addView(titleTextView)
        headerLayout.addView(amountLayout)

        // Progress bar for visual consistency
        val progressFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                8.dpToPx()
            )
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }

        // Calculate bar width based on amount
        val maxAmount = categoryIncome.values.maxOrNull() ?: amount
        val progressPercentage = if (maxAmount > 0) (amount / maxAmount) else 0.0

        val progressView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.8 * progressPercentage).toInt(),
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#4CAF50"))
        }

        progressFrame.addView(progressView)

        val statusText = TextView(this).apply {
            text = "✓ Income received"
            setTextColor(Color.parseColor("#4CAF50"))
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 0)
            }
        }

        categoryLayout.addView(headerLayout)
        categoryLayout.addView(progressFrame)
        categoryLayout.addView(statusText)

        val categoriesContainer = findViewById<LinearLayout>(R.id.categoriesContainer)
        categoriesContainer.addView(categoryLayout)
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
            setBackgroundColor(Color.WHITE)
            setPadding(24, 24, 24, 24)
            elevation = 1f
            isClickable = true
            isFocusable = true
            foreground = getDrawable(android.R.drawable.list_selector_background)
        }

        // Category title with goal status and emoji
        val emoji = when(category.uppercase()) {
            "UTILITIES" -> "⚡"
            "CLOTHES" -> "👕"
            "TRANSPORT", "TRANSPORTATION" -> "🚗"
            "TOILETRIES" -> "🧴"
            "GROCERIES" -> "🛒"
            "ENTERTAINMENT" -> "🎬"
            "HEALTHCARE" -> "🏥"
            else -> "💰"
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        val titleTextView = TextView(this).apply {
            text = "$emoji ${category.uppercase()}"
            setTextColor(Color.parseColor("#1A1A1A"))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val amountLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.END
        }

        val amountText = TextView(this).apply {
            text = if (goalData != null) {
                "R${String.format("%.2f", actualAmount)} / R${String.format("%.2f", goalData.maxGoal)}"
            } else {
                "R${String.format("%.2f", actualAmount)}"
            }
            setTextColor(Color.parseColor("#1A1A1A"))
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val remainingText = TextView(this).apply {
            text = if (goalData != null) {
                val remaining = goalData.maxGoal - actualAmount
                if (remaining >= 0) {
                    "R${String.format("%.2f", remaining)} remaining"
                } else {
                    "R${String.format("%.2f", Math.abs(remaining))} over budget"
                }
            } else {
                "No goal set"
            }
            setTextColor(
                if (goalData != null && actualAmount <= goalData.maxGoal)
                    Color.parseColor("#4CAF50")
                else
                    Color.parseColor("#FF5722")
            )
            textSize = 12f
        }

        amountLayout.addView(amountText)
        amountLayout.addView(remainingText)
        headerLayout.addView(titleTextView)
        headerLayout.addView(amountLayout)

        // Progress bar - FIXED VERSION
        val progressFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                8.dpToPx()
            )
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }

        if (goalData != null) {
            // Calculate progress percentage (cap at 100% for the main bar)
            val progressPercentage = (actualAmount / goalData.maxGoal).coerceAtMost(1.0)

            // Determine color based on goal status
            val progressColor = when {
                actualAmount <= goalData.minGoal -> Color.parseColor("#4CAF50") // Green - under min goal
                actualAmount <= goalData.maxGoal -> Color.parseColor("#FF9800") // Orange - between min and max
                else -> Color.parseColor("#4CAF50") // Green for the budget portion, red will be added for overspend
            }

            // Main progress view (up to 100% of budget)
            val progressView = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    (resources.displayMetrics.widthPixels * 0.8 * progressPercentage).toInt(), // Calculate actual pixel width
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(progressColor)
            }
            progressFrame.addView(progressView)

            // If over budget, add red overspend indicator
            if (actualAmount > goalData.maxGoal) {
                val overSpendAmount = actualAmount - goalData.maxGoal
                val overSpendPercentage = (overSpendAmount / goalData.maxGoal).coerceAtMost(0.3) // Cap overspend display at 30%

                val overSpendView = View(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        (resources.displayMetrics.widthPixels * 0.8 * overSpendPercentage).toInt(),
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ).apply {
                        // Position it after the main progress bar
                        leftMargin = (resources.displayMetrics.widthPixels * 0.8).toInt()
                    }
                    setBackgroundColor(Color.parseColor("#FF5722")) // Red for overspend
                }
                progressFrame.addView(overSpendView)
            }
        } else {
            // No goal set - show simple progress based on category average or just a default
            val maxExpense = categoryExpenses.values.maxOrNull() ?: actualAmount
            val progressPercentage = if (maxExpense > 0) (actualAmount / maxExpense) else 0.0

            val progressView = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    (resources.displayMetrics.widthPixels * 0.8 * progressPercentage).toInt(),
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#2196F3")) // Blue for no goal
            }
            progressFrame.addView(progressView)
        }

        val statusText = TextView(this).apply {
            text = if (goalData != null) {
                when {
                    actualAmount <= goalData.minGoal -> "✓ Under minimum goal"
                    actualAmount <= goalData.maxGoal -> "⚠️ Within budget range"
                    else -> "❌ Over budget"
                }
            } else {
                "No goal set"
            }
            setTextColor(
                if (goalData != null) {
                    when {
                        actualAmount <= goalData.minGoal -> Color.parseColor("#4CAF50")
                        actualAmount <= goalData.maxGoal -> Color.parseColor("#FF9800")
                        else -> Color.parseColor("#FF5722")
                    }
                } else {
                    Color.parseColor("#666666")
                }
            )
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 0)
            }
        }

        categoryLayout.addView(headerLayout)
        categoryLayout.addView(progressFrame)
        categoryLayout.addView(statusText)

        val categoriesContainer = findViewById<LinearLayout>(R.id.categoriesContainer)
        categoriesContainer.addView(categoryLayout)
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
            "📂 Categories",
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
                    4 -> navigateToCategories()
                    5 -> navigateToTransactions()
                    6 -> logout()
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

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        val selectedMonth = monthSpinner.selectedItemPosition + 1
        loadTransactionData(selectedMonth)
        loadGoalsData()
    }
}