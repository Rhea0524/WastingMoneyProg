package com.fake.wastingmoney

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class Categories : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var barChart: BarChart
    private lateinit var spinner: Spinner
    private lateinit var refreshBtn: Button
    private lateinit var title: TextView
    private lateinit var menuIcon: LinearLayout // Added menu icon

    private val spending = mutableMapOf<String, Double>()
    private val categoryGoals = mutableMapOf<String, Pair<Double, Double>>() // Pair<minGoal, maxGoal>

    private val categoryColors = mapOf(
        "LIGHTS" to Color.parseColor("#FFEB3B"),        // Yellow
        "CLOTHES" to Color.parseColor("#9C27B0"),       // Purple
        "CAR" to Color.parseColor("#2196F3"),           // Blue
        "TOILETRIES" to Color.parseColor("#F44336"),    // Red
        "FOOD" to Color.parseColor("#4CAF50"),          // Green
        "TRANSPORT" to Color.parseColor("#FF9800"),     // Orange
        "ENTERTAINMENT" to Color.parseColor("#E91E63"), // Pink
        "UTILITIES" to Color.parseColor("#795548"),     // Brown
        "HEALTHCARE" to Color.parseColor("#00BCD4"),    // Cyan
        "SHOPPING" to Color.parseColor("#9E9E9E"),      // Gray
        "BILLS" to Color.parseColor("#607D8B"),         // Blue Gray
        "OTHERS" to Color.parseColor("#8BC34A")         // Light Green
    )

    private val periodOptions = listOf("Last 30 Days", "Last 3 Months", "This Year")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        auth = Firebase.auth
        db = Firebase.database

        initializeViews()
        setupBarChart()
        setupSpinner()
        setupMenuListener() // Setup menu functionality

        refreshBtn.setOnClickListener { loadAllData() }

        if (auth.currentUser != null) {
            loadAllData()
        } else {
            Toast.makeText(this, "Please log in.", Toast.LENGTH_SHORT).show()
            // Redirect to login activity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun initializeViews() {
        barChart = findViewById(R.id.bar_chart)
        spinner = findViewById(R.id.period_spinner)
        refreshBtn = findViewById(R.id.refresh_button)
        title = findViewById(R.id.monthly_graph_title)
        menuIcon = findViewById(R.id.menuIcon) // Initialize menu icon
    }

    // Setup menu listener
    private fun setupMenuListener() {
        menuIcon.setOnClickListener {
            showMenuDialog()
        }
    }

    // Show menu dialog with navigation options
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
                    1 -> navigateToDashboard()
                    2 -> navigateToAddIncome()
                    3 -> navigateToAddExpense()
                    4 -> Toast.makeText(this, "You are already on Categories", Toast.LENGTH_SHORT).show()
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
        finish()
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun navigateToAddIncome() {
        startActivity(Intent(this, AddIncome::class.java))
        finish()
    }

    private fun navigateToAddExpense() {
        startActivity(Intent(this, AddExpense::class.java))
        finish()
    }

    private fun navigateToTransactions() {
        startActivity(Intent(this, Transaction::class.java))
        finish()
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

    private fun setupBarChart() {
        barChart.apply {
            description.isEnabled = false

            // X-axis configuration for better label spacing
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelRotationAngle = -30f  // Slightly less rotation for better readability
                textSize = 11f
                textColor = Color.WHITE
                setAvoidFirstLastClipping(true)
                spaceMin = 0.3f
                spaceMax = 0.3f
            }

            // Y-axis configuration
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#40FFFFFF")
                textColor = Color.WHITE
                textSize = 11f
                setDrawZeroLine(true)
                zeroLineColor = Color.WHITE
                axisMinimum = 0f
            }

            // General chart styling
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            legend.isEnabled = false

            // Increased padding for better spacing and professional look
            setExtraOffsets(15f, 30f, 15f, 50f)

            // Background styling
            setBackgroundColor(Color.parseColor("#40916C"))
        }
    }

    private fun setupSpinner() {
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periodOptions)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateBarChart()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadAllData() {
        // Get current user's UID - this is the key fix
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val userId = currentUser.uid // Use the actual logged-in user's UID
        Log.d("Categories", "Loading data for user: $userId")

        spending.clear()
        categoryGoals.clear()

        // Load spending data with enhanced debugging
        db.getReference("expenses").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("Categories", "=== EXPENSES DEBUG INFO ===")
                    Log.d("Categories", "Expenses snapshot exists: ${snapshot.exists()}")
                    Log.d("Categories", "Expenses children count: ${snapshot.childrenCount}")
                    Log.d("Categories", "Full path: expenses/$userId")

                    if (!snapshot.exists()) {
                        Log.d("Categories", "No expenses found for user $userId")
                        // Still load goals in case they exist
                        loadCategoryGoals(userId)
                        return
                    }

                    // Debug: Print all expense entries
                    for (snap in snapshot.children) {
                        val expenseId = snap.key
                        val rawCategory = snap.child("category").getValue(String::class.java)

                        // Try multiple ways to get the amount value
                        val amountValue = snap.child("amount").value
                        Log.d("Categories", "Raw amount value type: ${amountValue?.javaClass?.simpleName}")
                        Log.d("Categories", "Raw amount value: $amountValue")

                        val amount = when (amountValue) {
                            is Long -> amountValue.toDouble()
                            is Double -> amountValue
                            is String -> amountValue.toDoubleOrNull() ?: 0.0
                            is Int -> amountValue.toDouble()
                            else -> {
                                Log.w("Categories", "Unknown amount type: ${amountValue?.javaClass}")
                                0.0
                            }
                        }

                        val description = snap.child("description").getValue(String::class.java)
                        val date = snap.child("date").getValue(String::class.java)

                        Log.d("Categories", "Raw expense entry:")
                        Log.d("Categories", "  ID: $expenseId")
                        Log.d("Categories", "  Raw Category: '$rawCategory'")
                        Log.d("Categories", "  Parsed Amount: $amount")
                        Log.d("Categories", "  Description: '$description'")
                        Log.d("Categories", "  Date: '$date'")

                        val categoryUpper = rawCategory?.uppercase() ?: "OTHERS"

                        // Map old categories to new ones if needed
                        val category = when(categoryUpper) {
                            "GROCERIES" -> "FOOD"
                            "WATER & LIGHTS" -> "LIGHTS"
                            "CLOTHING" -> "CLOTHES"
                            "OTHER" -> "OTHERS"
                            "TRANSPORT" -> "TRANSPORT" // Keep transport separate from CAR
                            "CAR" -> "CAR" // Keep CAR separate from transport
                            else -> categoryUpper
                        }

                        Log.d("Categories", "  Mapped Category: '$category'")

                        val currentTotal = spending[category] ?: 0.0
                        spending[category] = currentTotal + amount

                        Log.d("Categories", "  Previous total for $category: R$currentTotal")
                        Log.d("Categories", "  New total for $category: R${spending[category]}")
                        Log.d("Categories", "  -------------------------")
                    }

                    // Print final totals
                    Log.d("Categories", "=== FINAL CATEGORY TOTALS ===")
                    spending.forEach { (category, total) ->
                        Log.d("Categories", "$category: R$total")
                    }
                    Log.d("Categories", "============================")

                    // Load goals after spending data is loaded
                    loadCategoryGoals(userId)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Categories", "Failed to load spending data: ${error.message}")
                    Toast.makeText(applicationContext, "Failed to load spending data", Toast.LENGTH_SHORT).show()
                    // Try to load goals anyway
                    loadCategoryGoals(userId)
                }
            })
    }

    private fun loadCategoryGoals(userId: String) {
        db.getReference("categoryGoals").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("Categories", "Goals snapshot exists: ${snapshot.exists()}")
                    Log.d("Categories", "Goals children count: ${snapshot.childrenCount}")

                    if (snapshot.exists()) {
                        for (snap in snapshot.children) {
                            val category = snap.key?.uppercase() ?: continue
                            val minGoal = snap.child("minGoal").getValue(Double::class.java) ?: 0.0
                            val maxGoal = snap.child("maxGoal").getValue(Double::class.java) ?: 0.0

                            categoryGoals[category] = Pair(minGoal, maxGoal)
                            Log.d("Categories", "Loaded goals for $category: min=$minGoal, max=$maxGoal")
                        }
                    } else {
                        Log.d("Categories", "No goals found for user $userId")
                    }

                    updateBarChart()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Categories", "Failed to load goal data: ${error.message}")
                    Toast.makeText(applicationContext, "Failed to load goal data", Toast.LENGTH_SHORT).show()
                    // Still update chart with spending data only
                    updateBarChart()
                }
            })
    }

    private fun updateBarChart() {
        val spendingEntries = ArrayList<BarEntry>()
        val minGoalEntries = ArrayList<BarEntry>()
        val maxGoalEntries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        var index = 0f

        // Get all categories that have either spending or goals
        val allCategories = (spending.keys + categoryGoals.keys).distinct().sorted()

        // If no data exists, show empty chart
        if (allCategories.isEmpty()) {
            Log.d("Categories", "No categories found - showing empty chart")
            barChart.clear()
            title.text = "No data available - Add expenses or set goals to see chart"
            return
        }

        allCategories.forEach { category ->
            val spendAmount = spending[category] ?: 0.0
            val goals = categoryGoals[category]
            val minGoal = goals?.first ?: 0.0
            val maxGoal = goals?.second ?: 0.0

            spendingEntries.add(BarEntry(index, spendAmount.toFloat()))
            minGoalEntries.add(BarEntry(index + 0.25f, minGoal.toFloat()))
            maxGoalEntries.add(BarEntry(index + 0.5f, maxGoal.toFloat()))

            labels.add(category)
            index += 1f
        }

        // Create datasets
        val spendingDataSet = BarDataSet(spendingEntries, "Spending").apply {
            colors = allCategories.map { categoryColors[it] ?: Color.LTGRAY }
            valueTextColor = Color.WHITE
            valueTextSize = 9f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    if (value > 0) "R${"%.0f".format(value)}" else ""
            }
        }

        val minGoalDataSet = BarDataSet(minGoalEntries, "Min Goal").apply {
            color = Color.parseColor("#66FF9800") // Semi-transparent orange
            valueTextColor = Color.WHITE
            valueTextSize = 8f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    if (value > 0) "Min: R${"%.0f".format(value)}" else ""
            }
        }

        val maxGoalDataSet = BarDataSet(maxGoalEntries, "Max Goal").apply {
            color = Color.parseColor("#66F44336") // Semi-transparent red
            valueTextColor = Color.WHITE
            valueTextSize = 8f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    if (value > 0) "Max: R${"%.0f".format(value)}" else ""
            }
        }

        val barData = BarData(spendingDataSet, minGoalDataSet, maxGoalDataSet).apply {
            barWidth = 0.2f // Thinner bars to fit all three series
        }

        barChart.apply {
            data = barData
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = labels.size

            // Group bars together
            groupBars(-0.5f, 0.3f, 0.05f)

            // Animate the chart
            animateY(1200)

            // Refresh the chart
            invalidate()
        }

        // Update title with spending summary
        val totalSpending = spending.values.sum()
        val totalMinGoals = categoryGoals.values.sumOf { it.first }
        val totalMaxGoals = categoryGoals.values.sumOf { it.second }

        title.text = if (totalSpending > 0 || totalMinGoals > 0 || totalMaxGoals > 0) {
            "Total Spending: R${"%.0f".format(totalSpending)} | " +
                    "Min Goals: R${"%.0f".format(totalMinGoals)} | " +
                    "Max Goals: R${"%.0f".format(totalMaxGoals)}"
        } else {
            "No spending data or goals found"
        }
    }

    private fun onCategoryClick(cat: String) {
        val intent = Intent(this, AddExpense::class.java)
        intent.putExtra("CATEGORY", cat)
        startActivity(intent)
    }
}