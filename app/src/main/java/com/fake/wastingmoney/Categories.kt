package com.fake.wastingmoney

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class Categories : AppCompatActivity() {

    companion object {
        private const val TAG = "Categories"
    }

    // Firebase instances
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    // Data storage
    private val categorySpending = mutableMapOf<String, Double>()
    private val categoryIncomes = mutableMapOf<String, Double>()
    private val monthlyData = mutableMapOf<String, Double>()

    // Direct references to chart elements
    private val chartBars = mutableListOf<View>()
    private val chartLabels = mutableListOf<TextView>()

    // Category colors for better visualization
    private val categoryColors = mapOf(
        "GROCERIES" to "#4CAF50",      // Green
        "CAR" to "#2196F3",            // Blue
        "CLOTHES" to "#9C27B0",        // Purple
        "CLOTHING" to "#9C27B0",       // Purple (alias for compatibility)
        "WATER & LIGHTS" to "#FF9800", // Orange
        "TOILETRIES" to "#E91E63",     // Pink
        "OTHER" to "#607D8B"           // Blue Grey
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_categories)

            initFirebase()
            setupClickListeners()
            setupChartReferences()

            // Only load data if user is authenticated
            if (auth.currentUser != null) {
                loadDataFromFirebase()
            } else {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
                // Optionally redirect to login activity
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
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
            throw e
        }
    }

    private fun setupClickListeners() {
        try {
            // Menu icon click - add null check
            //findViewById<LinearLayout>(R.id.menu_icon)?.setOnClickListener {
              //  Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
          //  }

            // Category clicks with better error handling
            val gridLayout = findViewById<android.widget.GridLayout>(R.id.categories_grid)
            if (gridLayout == null) {
                Log.e(TAG, "categories_grid not found in layout")
                return
            }

            val categoryNames = listOf("TOILETRIES", "CAR", "WATER & LIGHTS", "GROCERIES", "CLOTHES", "OTHER")

            for (i in 0 until minOf(gridLayout.childCount, categoryNames.size)) {
                try {
                    val categoryFrame = gridLayout.getChildAt(i) as? FrameLayout
                    if (categoryFrame != null) {
                        val categoryName = categoryNames[i]
                        categoryFrame.setOnClickListener {
                            onCategoryClick(categoryName)
                        }
                        Log.d(TAG, "Set click listener for category: $categoryName")
                    } else {
                        Log.w(TAG, "Category frame at index $i is not a FrameLayout")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting up click listener for category $i: ${e.message}", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupClickListeners: ${e.message}", e)
        }
    }

    private fun setupChartReferences() {
        try {
            // Get direct references to the chart bars and labels from the XML
            val rootView = findViewById<View>(android.R.id.content)
            if (rootView != null) {
                findChartElements(rootView)
                Log.d(TAG, "Found ${chartBars.size} chart bars and ${chartLabels.size} chart labels")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up chart references: ${e.message}", e)
        }
    }

    private fun findChartElements(view: View) {
        try {
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)

                    // Look for the bar chart container
                    if (child is LinearLayout && child.orientation == LinearLayout.HORIZONTAL) {
                        // Check if this container has the chart bars
                        val hasChartBars = hasBarChartCharacteristics(child)
                        if (hasChartBars) {
                            // Found the bar chart container
                            for (j in 0 until child.childCount) {
                                val bar = child.getChildAt(j)
                                if (bar.background != null) {
                                    chartBars.add(bar)
                                }
                            }

                            // Look for labels in the next sibling
                            val parentContainer = child.parent as? ViewGroup
                            if (parentContainer != null) {
                                val barIndex = parentContainer.indexOfChild(child)
                                if (barIndex + 1 < parentContainer.childCount) {
                                    val labelContainer = parentContainer.getChildAt(barIndex + 1)
                                    if (labelContainer is LinearLayout) {
                                        for (k in 0 until labelContainer.childCount) {
                                            val label = labelContainer.getChildAt(k)
                                            if (label is TextView) {
                                                chartLabels.add(label)
                                            }
                                        }
                                    }
                                }
                            }
                            return // Found what we need
                        }
                    }

                    findChartElements(child)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding chart elements: ${e.message}", e)
        }
    }

    private fun hasBarChartCharacteristics(container: LinearLayout): Boolean {
        return try {
            // Check if this container has multiple views with backgrounds (chart bars)
            var backgroundCount = 0
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child.background != null) {
                    backgroundCount++
                }
            }
            backgroundCount >= 4 // Should have 4 bars
        } catch (e: Exception) {
            Log.e(TAG, "Error checking bar chart characteristics: ${e.message}", e)
            false
        }
    }

    private fun onCategoryClick(categoryName: String) {
        try {
            Log.d(TAG, "Category clicked: $categoryName")
            Toast.makeText(this, "Clicked on $categoryName", Toast.LENGTH_SHORT).show()

            // Check if AddExpense activity exists before starting intent
            val intent = Intent(this, AddExpense::class.java)
            intent.putExtra("CATEGORY", categoryName)

            // Check if the activity can handle this intent
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Log.e(TAG, "AddExpense activity not found")
                Toast.makeText(this, "AddExpense activity not available", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCategoryClick: ${e.message}", e)
            Toast.makeText(this, "Error opening category: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDataFromFirebase() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user found")
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d(TAG, "Loading data for user: ${currentUser.uid}")
            loadExpenseData()
            loadIncomeData()

        } catch (e: Exception) {
            Log.e(TAG, "Error loading data from Firebase: ${e.message}", e)
            Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadExpenseData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User ID is null in loadExpenseData")
            return
        }

        try {
            database.getReference("expenses").child(userId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            categorySpending.clear()
                            monthlyData.clear()

                            val currentMonth = SimpleDateFormat("MMM", Locale.getDefault()).format(Date())
                            Log.d(TAG, "Loading expenses for current month: $currentMonth")

                            for (expenseSnapshot in snapshot.children) {
                                try {
                                    val amount = expenseSnapshot.child("amount").getValue(Double::class.java) ?: 0.0
                                    val category = expenseSnapshot.child("category").getValue(String::class.java) ?: "OTHER"
                                    val date = expenseSnapshot.child("date").getValue(String::class.java) ?: ""

                                    // Add to category totals
                                    val currentAmount = categorySpending[category.uppercase()] ?: 0.0
                                    categorySpending[category.uppercase()] = currentAmount + amount

                                    // Add to monthly data if it's current month
                                    if (date.contains(currentMonth, ignoreCase = true)) {
                                        val monthlyAmount = monthlyData[category.uppercase()] ?: 0.0
                                        monthlyData[category.uppercase()] = monthlyAmount + amount
                                    }

                                } catch (e: Exception) {
                                    Log.w(TAG, "Error processing expense entry: ${e.message}", e)
                                    // Skip invalid entries
                                }
                            }

                            Log.d(TAG, "Loaded ${categorySpending.size} expense categories")
                            updateChartsAndUI()

                        } catch (e: Exception) {
                            Log.e(TAG, "Error in onDataChange for expenses: ${e.message}", e)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Expense data loading cancelled: ${error.message}")
                        Toast.makeText(this@Categories, "Error loading expenses: ${error.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up expense data listener: ${e.message}", e)
        }
    }

    private fun loadIncomeData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User ID is null in loadIncomeData")
            return
        }

        try {
            database.getReference("incomes").child(userId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            categoryIncomes.clear()

                            for (incomeSnapshot in snapshot.children) {
                                try {
                                    val amount = incomeSnapshot.child("amount").getValue(Double::class.java) ?: 0.0
                                    val source = incomeSnapshot.child("source").getValue(String::class.java) ?: "OTHER"

                                    val currentAmount = categoryIncomes[source.uppercase()] ?: 0.0
                                    categoryIncomes[source.uppercase()] = currentAmount + amount

                                } catch (e: Exception) {
                                    Log.w(TAG, "Error processing income entry: ${e.message}", e)
                                    // Skip invalid entries
                                }
                            }

                            Log.d(TAG, "Loaded ${categoryIncomes.size} income categories")
                            updateChartsAndUI()

                        } catch (e: Exception) {
                            Log.e(TAG, "Error in onDataChange for incomes: ${e.message}", e)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Income data loading cancelled: ${error.message}")
                        Toast.makeText(this@Categories, "Error loading incomes: ${error.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up income data listener: ${e.message}", e)
        }
    }

    private fun updateChartsAndUI() {
        try {
            updateMonthlyGraph()
            updateCategoryDisplay()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating charts and UI: ${e.message}", e)
        }
    }

    private fun updateMonthlyGraph() {
        try {
            // Map the XML labels to actual categories with colors
            val chartCategories = listOf(
                Triple("GROCERIES", "GROCERIES", categoryColors["GROCERIES"]!!),
                Triple("CAR", "CAR", categoryColors["CAR"]!!),
                Triple("CLOTHES", "CLOTHES", categoryColors["CLOTHES"]!!), // Fixed to match your expense data
                Triple("LIGHTS", "WATER & LIGHTS", categoryColors["WATER & LIGHTS"]!!)
            )

            if (chartBars.size >= 4 && chartLabels.size >= 4) {
                try {
                    val maxAmount = chartCategories.maxOfOrNull { (_, category, _) ->
                        monthlyData[category] ?: 0.0
                    } ?: 1.0

                    chartCategories.forEachIndexed { index, (label, category, color) ->
                        val amount = monthlyData[category] ?: 0.0

                        // Update bar height (proportional to spending)
                        val heightRatio = if (maxAmount > 0) (amount / maxAmount) else 0.0
                        val minHeight = 20 // Minimum visible height
                        val maxHeight = 120
                        val newHeight = (minHeight + (heightRatio * (maxHeight - minHeight))).toInt()

                        val bar = chartBars[index]
                        val params = bar.layoutParams
                        if (params != null) {
                            params.height = dpToPx(newHeight)
                            bar.layoutParams = params
                        }

                        // Set bar color
                        bar.setBackgroundColor(Color.parseColor(color))

                        // Update label with better formatting
                        val labelText = if (amount > 0) {
                            "$label\nR${formatAmount(amount)}"
                        } else {
                            "$label\nR0"
                        }
                        chartLabels[index].text = labelText

                        // Make labels more readable
                        chartLabels[index].textSize = 10f
                        chartLabels[index].setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    }

                    // Show monthly summary
                    val totalSpent = monthlyData.values.sum()
                    val currentMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())

                    // Find the monthly graph title and update it
                    updateMonthlyGraphTitle("$currentMonth Spending: R${formatAmount(totalSpent)}")

                } catch (e: Exception) {
                    Log.e(TAG, "Error updating chart bars: ${e.message}", e)
                    Toast.makeText(this, "Chart update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Fallback - show data in toast if chart elements not found
                val totalSpent = monthlyData.values.sum()
                val topCategory = monthlyData.maxByOrNull { it.value }

                val message = if (topCategory != null && topCategory.value > 0) {
                    "Monthly: R${formatAmount(totalSpent)} | Top: ${topCategory.key} R${formatAmount(topCategory.value)}"
                } else {
                    "No spending data for this month"
                }

                Log.d(TAG, "Chart elements not found, showing fallback: $message")
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateMonthlyGraph: ${e.message}", e)
        }
    }

    private fun updateMonthlyGraphTitle(newTitle: String) {
        try {
            val titleView = findViewById<TextView>(R.id.monthly_graph_title)
            titleView?.text = newTitle
        } catch (e: Exception) {
            Log.e(TAG, "Error updating monthly graph title: ${e.message}", e)
        }
    }

    private fun updateCategoryDisplay() {
        try {
            val gridLayout = findViewById<android.widget.GridLayout>(R.id.categories_grid)
            if (gridLayout == null) {
                Log.e(TAG, "categories_grid not found for updating display")
                return
            }

            val categoryNames = listOf("TOILETRIES", "CAR", "WATER & LIGHTS", "GROCERIES", "CLOTHES", "OTHER")

            for (i in 0 until minOf(gridLayout.childCount, categoryNames.size)) {
                try {
                    val categoryFrame = gridLayout.getChildAt(i) as? FrameLayout
                    val textView = categoryFrame?.getChildAt(0) as? TextView

                    if (textView != null) {
                        val categoryName = categoryNames[i]
                        val amount = categorySpending[categoryName] ?: 0.0

                        val displayText = if (amount > 0) {
                            "$categoryName\nR${formatAmount(amount)}"
                        } else {
                            categoryName
                        }
                        textView.text = displayText

                        // Add visual indicator for spending level
                        val color = categoryColors[categoryName] ?: categoryColors["OTHER"]!!
                        if (amount > 0) {
                            categoryFrame?.setBackgroundColor(Color.parseColor(color + "33")) // 20% opacity
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating category display for index $i: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateCategoryDisplay: ${e.message}", e)
        }
    }

    private fun formatAmount(amount: Double): String {
        return try {
            when {
                amount >= 1000 -> String.format("%.1fk", amount / 1000)
                amount >= 100 -> String.format("%.0f", amount)
                amount > 0 -> String.format("%.2f", amount)
                else -> "0"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting amount: ${e.message}", e)
            "0"
        }
    }

    private fun dpToPx(dp: Int): Int {
        return try {
            val density = resources.displayMetrics.density
            (dp * density).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error converting dp to px: ${e.message}", e)
            dp // Return original value as fallback
        }
    }

    fun refreshData() {
        try {
            loadDataFromFirebase()
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing data: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (auth.currentUser != null) {
                loadDataFromFirebase()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume: ${e.message}", e)
        }
    }
}