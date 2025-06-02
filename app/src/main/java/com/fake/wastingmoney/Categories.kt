package com.fake.wastingmoney

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class Categories : AppCompatActivity() {

    companion object {
        private const val TAG = "Categories"
    }

    // Firebase instances
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    // UI Components
    private lateinit var barChart: BarChart
    private lateinit var lineChart: LineChart
    private lateinit var periodSpinner: Spinner
    private lateinit var refreshButton: Button
    private lateinit var monthlyGraphTitle: TextView

    // Data storage
    private val categorySpending = mutableMapOf<String, Double>()
    private val categoryIncomes = mutableMapOf<String, Double>()
    private val categoryGoals = mutableMapOf<String, Pair<Double, Double>>() // min, max goals
    private val monthlyData = mutableMapOf<String, MutableMap<String, Double>>() // month -> category -> amount
    private val periodOptions = listOf("Last 30 Days", "Last 3 Months", "Last 6 Months", "This Year")

    // Data loading flags
    private var expensesLoaded = false
    private var incomesLoaded = false
    private var goalsLoaded = false

    // Category configuration
    private val categoryNames = listOf("TOILETRIES", "CAR", "WATER & LIGHTS", "GROCERIES", "CLOTHES", "OTHER")
    private val categoryColors = mapOf(
        "GROCERIES" to Color.parseColor("#4CAF50"),
        "CAR" to Color.parseColor("#2196F3"),
        "CLOTHES" to Color.parseColor("#9C27B0"),
        "CLOTHING" to Color.parseColor("#9C27B0"),
        "WATER & LIGHTS" to Color.parseColor("#FF9800"),
        "TOILETRIES" to Color.parseColor("#E91E63"),
        "OTHER" to Color.parseColor("#607D8B")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_categories)

            initFirebase()
            initViews()
            setupSpinner()
            setupClickListeners()

            if (auth.currentUser != null) {
                showLoadingState()
                loadAllData()
            } else {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing activity", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initFirebase() {
        try {
            database = Firebase.database
            auth = Firebase.auth

            // Enable offline persistence for better performance
            database.setPersistenceEnabled(true)

            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
            throw e
        }
    }

    private fun initViews() {
        try {
            barChart = findViewById(R.id.bar_chart)
            lineChart = findViewById(R.id.line_chart)
            periodSpinner = findViewById(R.id.period_spinner)
            refreshButton = findViewById(R.id.refresh_button)
            monthlyGraphTitle = findViewById(R.id.monthly_graph_title)

            setupCharts()

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
        }
    }

    private fun setupCharts() {
        // Setup Bar Chart
        barChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setMaxVisibleValueCount(60)
            legend.isEnabled = true

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.WHITE
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.GRAY
                textColor = Color.WHITE
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
        }

        // Setup Line Chart for trends
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            legend.isEnabled = true

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.WHITE
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.GRAY
                textColor = Color.WHITE
            }

            axisRight.isEnabled = false
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periodOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodSpinner.adapter = adapter

        periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (allDataLoaded()) {
                    updateChartsForPeriod(periodOptions[position])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupClickListeners() {
        try {
            refreshButton.setOnClickListener {
                resetDataLoadingFlags()
                showLoadingState()
                loadAllData()
            }

            val gridLayout = findViewById<GridLayout>(R.id.categories_grid)
            if (gridLayout != null) {
                for (i in 0 until minOf(gridLayout.childCount, categoryNames.size)) {
                    val categoryFrame = gridLayout.getChildAt(i) as? FrameLayout
                    categoryFrame?.setOnClickListener {
                        onCategoryClick(categoryNames[i])
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupClickListeners: ${e.message}", e)
        }
    }

    private fun onCategoryClick(categoryName: String) {
        try {
            Log.d(TAG, "Category clicked: $categoryName")
            val intent = Intent(this, AddExpense::class.java)
            intent.putExtra("CATEGORY", categoryName)
            startActivity(intent)

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCategoryClick: ${e.message}", e)
        }
    }

    private fun showLoadingState() {
        monthlyGraphTitle.text = "Loading data..."
        refreshButton.isEnabled = false
    }

    private fun hideLoadingState() {
        refreshButton.isEnabled = true
    }

    private fun resetDataLoadingFlags() {
        expensesLoaded = false
        incomesLoaded = false
        goalsLoaded = false
    }

    private fun allDataLoaded(): Boolean {
        return expensesLoaded && incomesLoaded && goalsLoaded
    }

    private fun checkAndUpdateUI() {
        if (allDataLoaded()) {
            hideLoadingState()
            updateChartsAndUI()
        }
    }

    private fun loadAllData() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d(TAG, "Loading all data for user: ${currentUser.uid}")

            // Clear existing data
            categorySpending.clear()
            categoryIncomes.clear()
            categoryGoals.clear()
            monthlyData.clear()

            // Load all data types
            loadGoalsFromDashboard()
            loadExpenseData()
            loadIncomeData()

        } catch (e: Exception) {
            Log.e(TAG, "Error loading data: ${e.message}", e)
            hideLoadingState()
        }
    }

    private fun loadGoalsFromDashboard() {
        val userId = auth.currentUser?.uid ?: return

        try {
            database.getReference("goals").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            categoryGoals.clear()

                            for (goalSnapshot in snapshot.children) {
                                val category = goalSnapshot.child("category").getValue(String::class.java) ?: "OTHER"
                                val minGoal = goalSnapshot.child("minGoal").getValue(Double::class.java) ?: 0.0
                                val maxGoal = goalSnapshot.child("maxGoal").getValue(Double::class.java) ?: 0.0

                                categoryGoals[category.uppercase()] = Pair(minGoal, maxGoal)
                            }

                            Log.d(TAG, "Loaded ${categoryGoals.size} category goals")
                            goalsLoaded = true
                            checkAndUpdateUI()

                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing goals data: ${e.message}", e)
                            goalsLoaded = true
                            checkAndUpdateUI()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Goals data loading cancelled: ${error.message}")
                        goalsLoaded = true
                        checkAndUpdateUI()
                    }
                })

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up goals listener: ${e.message}", e)
            goalsLoaded = true
            checkAndUpdateUI()
        }
    }

    private fun loadExpenseData() {
        val userId = auth.currentUser?.uid ?: return

        try {
            database.getReference("expenses").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            categorySpending.clear()

                            // Clear monthly expense data
                            monthlyData.clear()

                            var expenseCount = 0
                            for (expenseSnapshot in snapshot.children) {
                                val amount = expenseSnapshot.child("amount").getValue(Double::class.java) ?: 0.0
                                val category = expenseSnapshot.child("category").getValue(String::class.java) ?: "OTHER"
                                val dateStr = expenseSnapshot.child("date").getValue(String::class.java) ?: ""

                                // Normalize category name
                                val categoryKey = category.uppercase().replace("CLOTHING", "CLOTHES")

                                // Add to total spending
                                categorySpending[categoryKey] = (categorySpending[categoryKey] ?: 0.0) + amount
                                expenseCount++

                                // Add to monthly data for trends
                                processMonthlyExpenseData(dateStr, categoryKey, amount)
                            }

                            Log.d(TAG, "Loaded $expenseCount expenses for ${categorySpending.size} categories across ${monthlyData.size} months")
                            expensesLoaded = true
                            checkAndUpdateUI()

                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing expense data: ${e.message}", e)
                            expensesLoaded = true
                            checkAndUpdateUI()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Expense data loading cancelled: ${error.message}")
                        expensesLoaded = true
                        checkAndUpdateUI()
                    }
                })

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up expense listener: ${e.message}", e)
            expensesLoaded = true
            checkAndUpdateUI()
        }
    }

    private fun processMonthlyExpenseData(dateStr: String, categoryKey: String, amount: Double) {
        try {
            // Try multiple date formats that might be used in AddExpense activity
            val dateFormats = listOf(
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            )

            var date: Date? = null
            for (format in dateFormats) {
                try {
                    date = format.parse(dateStr)
                    if (date != null) break
                } catch (e: Exception) {
                    // Try next format
                }
            }

            if (date != null) {
                val monthKey = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date)

                if (!monthlyData.containsKey(monthKey)) {
                    monthlyData[monthKey] = mutableMapOf()
                }

                val monthData = monthlyData[monthKey]!!
                monthData[categoryKey] = (monthData[categoryKey] ?: 0.0) + amount
            } else {
                Log.w(TAG, "Could not parse date: $dateStr")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error processing monthly data for date: $dateStr", e)
        }
    }

    private fun loadIncomeData() {
        val userId = auth.currentUser?.uid ?: return

        try {
            database.getReference("incomes").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            categoryIncomes.clear()

                            var incomeCount = 0
                            for (incomeSnapshot in snapshot.children) {
                                val amount = incomeSnapshot.child("amount").getValue(Double::class.java) ?: 0.0
                                val source = incomeSnapshot.child("source").getValue(String::class.java) ?: "OTHER"

                                categoryIncomes[source.uppercase()] = (categoryIncomes[source.uppercase()] ?: 0.0) + amount
                                incomeCount++
                            }

                            Log.d(TAG, "Loaded $incomeCount incomes for ${categoryIncomes.size} sources")
                            incomesLoaded = true
                            checkAndUpdateUI()

                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing income data: ${e.message}", e)
                            incomesLoaded = true
                            checkAndUpdateUI()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Income data loading cancelled: ${error.message}")
                        incomesLoaded = true
                        checkAndUpdateUI()
                    }
                })

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up income listener: ${e.message}", e)
            incomesLoaded = true
            checkAndUpdateUI()
        }
    }

    private fun updateChartsForPeriod(period: String) {
        try {
            Log.d(TAG, "Updating charts for period: $period")

            val filteredData = getDataForPeriod(period)
            updateBarChart(filteredData)
            updateLineChart(period)
            updateCategoryGrid()

        } catch (e: Exception) {
            Log.e(TAG, "Error updating charts for period: ${e.message}", e)
        }
    }

    private fun getDataForPeriod(period: String): Map<String, Double> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        val monthsToInclude = when (period) {
            "Last 30 Days" -> 1
            "Last 3 Months" -> 3
            "Last 6 Months" -> 6
            "This Year" -> 12
            else -> 3
        }

        val filteredData = mutableMapOf<String, Double>()

        // For "Last 30 Days", use all data if monthly breakdown isn't detailed enough
        if (period == "Last 30 Days") {
            return categorySpending.toMap()
        }

        // For other periods, filter by months
        for (i in 0 until monthsToInclude) {
            val monthKey = dateFormat.format(calendar.time)

            monthlyData[monthKey]?.forEach { (category, amount) ->
                filteredData[category] = (filteredData[category] ?: 0.0) + amount
            }

            calendar.add(Calendar.MONTH, -1)
        }

        // If no monthly data found, return current totals
        return if (filteredData.isEmpty()) categorySpending.toMap() else filteredData
    }

    private fun updateBarChart(data: Map<String, Double>) {
        try {
            // Clear any existing limit lines
            barChart.axisLeft.removeAllLimitLines()

            val entries = ArrayList<BarEntry>()
            val colors = ArrayList<Int>()
            val labels = ArrayList<String>()

            categoryNames.forEachIndexed { index, category ->
                val amount = data[category] ?: 0.0
                val goals = categoryGoals[category]

                entries.add(BarEntry(index.toFloat(), amount.toFloat()))
                colors.add(categoryColors[category] ?: categoryColors["OTHER"]!!)
                labels.add(category.replace(" & ", "\n&\n"))
            }

            val dataSet = BarDataSet(entries, "Spending by Category")
            dataSet.colors = colors
            dataSet.valueTextColor = Color.WHITE
            dataSet.valueTextSize = 10f
            dataSet.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) "R${formatAmount(value.toDouble())}" else ""
                }
            }

            val barData = BarData(dataSet)
            barData.barWidth = 0.8f

            barChart.data = barData
            barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            barChart.xAxis.setLabelCount(labels.size)
            barChart.invalidate()
            barChart.animateY(1000)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating bar chart: ${e.message}", e)
        }
    }

    private fun updateLineChart(period: String) {
        try {
            val lineEntries = mutableMapOf<String, ArrayList<Entry>>()
            val sortedMonths = monthlyData.keys.sortedWith { month1, month2 ->
                try {
                    val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    val date1 = format.parse(month1)
                    val date2 = format.parse(month2)
                    date1?.compareTo(date2) ?: 0
                } catch (e: Exception) {
                    month1.compareTo(month2)
                }
            }

            // Initialize entries for each category
            categoryNames.forEach { category ->
                lineEntries[category] = ArrayList()
            }

            sortedMonths.forEachIndexed { monthIndex, month ->
                categoryNames.forEach { category ->
                    val amount = monthlyData[month]?.get(category) ?: 0.0
                    lineEntries[category]?.add(Entry(monthIndex.toFloat(), amount.toFloat()))
                }
            }

            val dataSets = ArrayList<ILineDataSet>()

            lineEntries.forEach { (category, entries) ->
                if (entries.isNotEmpty() && entries.any { it.y > 0 }) {
                    val dataSet = LineDataSet(entries, category.replace(" & ", "\n&\n"))
                    dataSet.color = categoryColors[category] ?: categoryColors["OTHER"]!!
                    dataSet.setCircleColor(categoryColors[category] ?: categoryColors["OTHER"]!!)
                    dataSet.lineWidth = 2f
                    dataSet.circleRadius = 3f
                    dataSet.setDrawValues(false)
                    dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

                    dataSets.add(dataSet)
                }
            }

            if (dataSets.isNotEmpty()) {
                val lineData = LineData(dataSets as List<ILineDataSet>)
                lineChart.data = lineData
                lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(sortedMonths.map {
                    it.substring(0, 3) // Show abbreviated month
                })
                lineChart.invalidate()
                lineChart.animateX(1000)
            } else {
                // Clear chart if no data
                lineChart.clear()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating line chart: ${e.message}", e)
        }
    }

    private fun updateCategoryGrid() {
        try {
            val gridLayout = findViewById<GridLayout>(R.id.categories_grid)
            if (gridLayout == null) return

            for (i in 0 until minOf(gridLayout.childCount, categoryNames.size)) {
                val categoryFrame = gridLayout.getChildAt(i) as? FrameLayout
                val textView = categoryFrame?.getChildAt(0) as? TextView

                if (textView != null) {
                    val categoryName = categoryNames[i]
                    val amount = categorySpending[categoryName] ?: 0.0
                    val goals = categoryGoals[categoryName]

                    val displayText = buildString {
                        append(categoryName)
                        if (amount > 0) {
                            append("\nR${formatAmount(amount)}")
                        }
                        goals?.let { (minGoal, maxGoal) ->
                            if (minGoal > 0 || maxGoal > 0) {
                                append("\nGoal: R${formatAmount(minGoal)}-${formatAmount(maxGoal)}")
                            }
                        }
                    }

                    textView.text = displayText

                    // Reset background first
                    categoryFrame?.setBackgroundResource(R.drawable.circle_background)

                    // Color coding based on goal achievement
                    goals?.let { (minGoal, maxGoal) ->
                        when {
                            maxGoal > 0 && amount > maxGoal -> {
                                // Over budget - red tint
                                categoryFrame?.setBackgroundColor(Color.parseColor("#33FF0000"))
                            }
                            minGoal > 0 && amount < minGoal -> {
                                // Under minimum - yellow tint
                                categoryFrame?.setBackgroundColor(Color.parseColor("#33FFFF00"))
                            }
                            minGoal > 0 && maxGoal > 0 && amount >= minGoal && amount <= maxGoal -> {
                                // Within goals - green tint
                                categoryFrame?.setBackgroundColor(Color.parseColor("#3300FF00"))
                            }

                            else -> {}
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating category grid: ${e.message}", e)
        }
    }

    private fun updateChartsAndUI() {
        try {
            val selectedPeriod = periodOptions[periodSpinner.selectedItemPosition]
            updateChartsForPeriod(selectedPeriod)

            // Update title with total spending
            val totalSpent = categorySpending.values.sum()
            val totalIncome = categoryIncomes.values.sum()

            monthlyGraphTitle.text = "Total Spending: R${formatAmount(totalSpent)}" +
                    if (totalIncome > 0) " | Income: R${formatAmount(totalIncome)}" else ""

        } catch (e: Exception) {
            Log.e(TAG, "Error updating charts and UI: ${e.message}", e)
        }
    }

    private fun formatAmount(amount: Double): String {
        return try {
            when {
                amount >= 1000000 -> String.format("%.1fM", amount / 1000000)
                amount >= 1000 -> String.format("%.1fk", amount / 1000)
                amount >= 100 -> String.format("%.0f", amount)
                amount > 0 -> String.format("%.2f", amount)
                else -> "0"
            }
        } catch (e: Exception) {
            "0"
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (auth.currentUser != null) {
                // Reload data when returning to activity to catch any new expenses/incomes
                resetDataLoadingFlags()
                showLoadingState()
                loadAllData()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            // Clean up any listeners if needed
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}", e)
        }
    }
}