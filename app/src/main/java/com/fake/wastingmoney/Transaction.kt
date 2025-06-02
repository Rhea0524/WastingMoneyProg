package com.fake.wastingmoney

import android.content.Intent
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Base64
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.fake.wastingmoney.model.Income
import com.fake.wastingmoney.model.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Transaction : AppCompatActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var transactionsContainer: LinearLayout
    private lateinit var tvNoTransactions: TextView
    private lateinit var tvTotalTransactions: TextView
    private lateinit var tvNetTotal: TextView
    private lateinit var menuIcon: LinearLayout

    private val transactionsList = mutableListOf<TransactionItem>()

    // Variables to track totals
    private var totalIncome = 0.0
    private var totalExpenses = 0.0

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "TransactionActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        Log.d(TAG, "onCreate called")
        try {
            initializeViews()
            initializeFirebase()
            setupMenuListener()
            loadAllTransactions()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading transactions: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        try {
            loadAllTransactions()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume: ${e.message}", e)
        }
    }

    private fun initializeViews() {
        try {
            // Initialize basic views that exist in XML
            tvNoTransactions = findViewById(R.id.tvNoTransactions)
            tvTotalTransactions = findViewById(R.id.tvTotalTransactions)

            // Initialize menu icon
            try {
                menuIcon = findViewById(R.id.menuIcon)
            } catch (e: Exception) {
                Log.w(TAG, "Menu icon not found in layout, menu functionality will be disabled")
            }

            // Get the RecyclerView from XML
            val recyclerView = findViewById<RecyclerView>(R.id.rvTransactions)

            // Get the parent layout
            val parentLayout = recyclerView.parent as androidx.constraintlayout.widget.ConstraintLayout

            // Store the layout parameters from RecyclerView
            var layoutParams = recyclerView.layoutParams

            // Remove the RecyclerView
            parentLayout.removeView(recyclerView)

            // Create ScrollView with the same layout parameters
            scrollView = ScrollView(this).apply {
                id = R.id.rvTransactions // Keep the same ID for constraints
                this.layoutParams = layoutParams
            }

            // Create LinearLayout for transactions
            transactionsContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Add LinearLayout to ScrollView
            scrollView.addView(transactionsContainer)

            // Add ScrollView to parent
            parentLayout.addView(scrollView)

            // Create and add net total TextView
            createNetTotalView(parentLayout)

            Log.d(TAG, "Views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            createViewsProgrammatically()
        }
    }

    private fun createNetTotalView(parentLayout: androidx.constraintlayout.widget.ConstraintLayout) {
        // Create TextView for net total
        tvNetTotal = TextView(this).apply {
            id = View.generateViewId()
            text = "Net Total: R0.00"
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(16, 8, 16, 8)
            setBackgroundColor(Color.parseColor("#2d5a47"))
        }

        // Add to parent layout
        parentLayout.addView(tvNetTotal)

        // Set constraints programmatically
        val constraintSet = androidx.constraintlayout.widget.ConstraintSet()
        constraintSet.clone(parentLayout)

        // Position it between the title and the ScrollView
        constraintSet.connect(tvNetTotal.id, androidx.constraintlayout.widget.ConstraintSet.TOP,
            tvTotalTransactions.id, androidx.constraintlayout.widget.ConstraintSet.BOTTOM, 16)
        constraintSet.connect(tvNetTotal.id, androidx.constraintlayout.widget.ConstraintSet.START,
            androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.START, 16)
        constraintSet.connect(tvNetTotal.id, androidx.constraintlayout.widget.ConstraintSet.END,
            androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.END, 16)

        // Update ScrollView constraint to be below the net total
        constraintSet.connect(scrollView.id, androidx.constraintlayout.widget.ConstraintSet.TOP,
            tvNetTotal.id, androidx.constraintlayout.widget.ConstraintSet.BOTTOM, 16)

        constraintSet.applyTo(parentLayout)
    }

    private fun createViewsProgrammatically() {
        try {
            // Create a simple layout structure as fallback
            val mainLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#1a4c3a"))
                setPadding(16, 16, 16, 16)
            }

            tvTotalTransactions = TextView(this).apply {
                text = "TOTAL TRANSACTIONS (0)"
                textSize = 20f
                setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 24, 0, 16)
            }

            // Create net total TextView
            tvNetTotal = TextView(this).apply {
                text = "Net Total: R0.00"
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(16, 8, 16, 8)
                setBackgroundColor(Color.parseColor("#2d5a47"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
            }

            scrollView = ScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }

            transactionsContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            tvNoTransactions = TextView(this).apply {
                text = "No transactions found"
                textSize = 16f
                setTextColor(Color.WHITE)
                setPadding(16, 32, 16, 32)
                visibility = View.GONE
            }

            scrollView.addView(transactionsContainer)
            mainLayout.addView(tvTotalTransactions)
            mainLayout.addView(tvNetTotal)
            mainLayout.addView(scrollView)
            mainLayout.addView(tvNoTransactions)

            setContentView(mainLayout)
            Log.d(TAG, "Views created programmatically")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating views programmatically: ${e.message}", e)
        }
    }

    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        Log.d(TAG, "Firebase initialized")
    }

    private fun setupMenuListener() {
        try {
            if (::menuIcon.isInitialized) {
                menuIcon.setOnClickListener {
                    showMenuDialog()
                }
                Log.d(TAG, "Menu listener set up successfully")
            } else {
                Log.w(TAG, "Menu icon not initialized, skipping menu setup")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up menu listener: ${e.message}", e)
        }
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
                    1 -> navigateToDashboard()
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

    // Navigation methods remain the same as in original code...
    private fun navigateToHome() {
        try {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to Home: ${e.message}", e)
            Toast.makeText(this, "Home activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToDashboard() {
        try {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to Dashboard: ${e.message}", e)
            Toast.makeText(this, "Dashboard activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToAddIncome() {
        try {
            val intent = Intent(this, AddIncome::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to AddIncome: ${e.message}", e)
            Toast.makeText(this, "Error opening Add Income: ${e.message}", Toast.LENGTH_LONG).show()
            showAddIncomeDialog()
        }
    }

    private fun showAddIncomeDialog() {
        AlertDialog.Builder(this)
            .setTitle("Add Income - Quick Entry")
            .setMessage("The Add Income screen couldn't load. Please make sure the AddIncome activity is properly configured in your AndroidManifest.xml")
            .setPositiveButton("OK", null)
            .setNeutralButton("Check Logcat") { _, _ ->
                Toast.makeText(this, "Check Android Studio Logcat for detailed error information", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun navigateToAddExpense() {
        try {
            val intent = Intent(this, AddExpense::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to AddExpense: ${e.message}", e)
            Toast.makeText(this, "Add Expense activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToBudgetGoal() {
        try {
            val intent = Intent(this, BudgetGoal::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to BudgetGoal: ${e.message}", e)
            Toast.makeText(this, "Budget Goal activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToCategories() {
        try {
            val intent = Intent(this, Categories::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to Categories: ${e.message}", e)
            Toast.makeText(this, "Categories activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToManageCategories() {
        try {
            val intent = Intent(this, ManageCategoriesActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to ManageCategoriesActivity: ${e.message}", e)
            Toast.makeText(this, "Manage Categories activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToCategoryDetail() {
        try {
            val intent = Intent(this, CategoryDetail::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to CategoryDetail: ${e.message}", e)
            Toast.makeText(this, "Category Detail activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToTransactions() {
        Toast.makeText(this, "You are already on the Transactions screen", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Error during logout: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAllTransactions() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated")
            Toast.makeText(this, "Please login to view transactions", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = currentUser.uid
        Log.d(TAG, "Loading transactions for UID: $uid")

        transactionsList.clear()
        clearTransactionViews()

        // Reset totals
        totalIncome = 0.0
        totalExpenses = 0.0

        loadIncomes(uid)
        loadExpenses(uid)
    }

    private fun loadIncomes(uid: String) {
        val incomesRef = database.getReference("incomes").child(uid)
        Log.d(TAG, "Loading incomes from path: incomes/$uid")

        incomesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Income snapshot received. Exists: ${snapshot.exists()}, Children count: ${snapshot.childrenCount}")

                transactionsList.removeAll { it.type == "Income" }
                totalIncome = 0.0

                for (incomeSnapshot in snapshot.children) {
                    try {
                        val income = incomeSnapshot.getValue(Income::class.java)
                        income?.let {
                            val transactionItem = TransactionItem(
                                id = incomeSnapshot.key ?: "",
                                amount = it.amount,
                                description = it.description,
                                category = it.source,
                                date = it.date,
                                timestamp = it.timestamp,
                                type = "Income",
                                documentBase64 = it.documentBase64 // Instead of null// Income typically doesn't have documents
                            )
                            transactionsList.add(transactionItem)
                            totalIncome += it.amount
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing income: ${e.message}", e)
                    }
                }

                Log.d(TAG, "Total income calculated: R$totalIncome")
                sortAndUpdateTransactions()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load incomes: ${error.message}")
                Toast.makeText(this@Transaction, "Failed to load incomes: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadExpenses(uid: String) {
        val expensesRef = database.getReference("expenses").child(uid)
        Log.d(TAG, "Loading expenses from path: expenses/$uid")

        expensesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Expense snapshot received. Exists: ${snapshot.exists()}, Children count: ${snapshot.childrenCount}")

                transactionsList.removeAll { it.type == "Expense" }
                totalExpenses = 0.0

                for (expenseSnapshot in snapshot.children) {
                    try {
                        val expense = expenseSnapshot.getValue(Expense::class.java)
                        expense?.let {
                            val transactionItem = TransactionItem(
                                id = expenseSnapshot.key ?: "",
                                amount = it.amount,
                                description = it.description,
                                category = it.category,
                                date = it.date,
                                timestamp = it.timestamp,
                                type = "Expense",
                                documentBase64 = it.documentBase64 // Include the base64 document
                            )
                            transactionsList.add(transactionItem)
                            totalExpenses += it.amount
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing expense: ${e.message}", e)
                    }
                }

                Log.d(TAG, "Total expenses calculated: R$totalExpenses")
                sortAndUpdateTransactions()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load expenses: ${error.message}")
                Toast.makeText(this@Transaction, "Failed to load expenses: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sortAndUpdateTransactions() {
        try {
            Log.d(TAG, "Sorting and updating transactions. Count: ${transactionsList.size}")

            transactionsList.sortByDescending { it.timestamp }

            runOnUiThread {
                displayTransactions()
                updateTransactionsList(transactionsList.isNotEmpty())
                updateTotalDisplays()
            }

            Log.d(TAG, "UI updated with ${transactionsList.size} transactions")
        } catch (e: Exception) {
            Log.e(TAG, "Error sorting and updating transactions: ${e.message}", e)
        }
    }

    private fun updateTotalDisplays() {
        try {
            val netTotal = totalIncome - totalExpenses

            tvTotalTransactions.text = "TOTAL TRANSACTIONS (${transactionsList.size})"

            val netTotalText = if (netTotal >= 0) {
                "Net Total: +R${String.format("%.2f", netTotal)}"
            } else {
                "Net Total: -R${String.format("%.2f", Math.abs(netTotal))}"
            }

            tvNetTotal.text = netTotalText

            val color = if (netTotal >= 0) {
                Color.parseColor("#4CAF50")
            } else {
                Color.parseColor("#FF5722")
            }
            tvNetTotal.setTextColor(color)

            Log.d(TAG, "Updated totals - Income: R$totalIncome, Expenses: R$totalExpenses, Net: R$netTotal")

        } catch (e: Exception) {
            Log.e(TAG, "Error updating total displays: ${e.message}", e)
        }
    }

    private fun displayTransactions() {
        try {
            Log.d(TAG, "Displaying ${transactionsList.size} transactions")

            transactionsContainer.removeAllViews()

            for (transaction in transactionsList) {
                val transactionView = createTransactionView(transaction)
                transactionsContainer.addView(transactionView)
            }

            Log.d(TAG, "Transaction views created and added")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying transactions: ${e.message}", e)
        }
    }

    private fun createTransactionView(transaction: TransactionItem): View {
        val transactionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 20, 24, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            setBackgroundColor(Color.parseColor("#2d5a47"))
        }

        // Main content container
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val leftLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvAmount = TextView(this).apply {
            text = if (transaction.type == "Income") {
                "+R${String.format("%.2f", transaction.amount)}"
            } else {
                "-R${String.format("%.2f", transaction.amount)}"
            }
            setTextColor(
                if (transaction.type == "Income") Color.parseColor("#4CAF50")
                else Color.parseColor("#FF5722")
            )
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val tvType = TextView(this).apply {
            text = transaction.type
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 11f
        }

        leftLayout.addView(tvAmount)
        leftLayout.addView(tvType)

        val rightLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                2f
            )
        }

        val tvDescription = TextView(this).apply {
            text = transaction.description
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val tvCategory = TextView(this).apply {
            text = transaction.category
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 13f
        }

        val tvDate = TextView(this).apply {
            text = transaction.date
            setTextColor(Color.parseColor("#999999"))
            textSize = 11f
        }

        rightLayout.addView(tvDescription)
        rightLayout.addView(tvCategory)
        rightLayout.addView(tvDate)

        contentLayout.addView(leftLayout)
        contentLayout.addView(rightLayout)

        // Add image view for documents (if available)
        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                setMargins(16, 0, 0, 0)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.parseColor("#1a4c3a"))
            setPadding(4, 4, 4, 4)
        }

        // Load base64 image if available
        if (!transaction.documentBase64.isNullOrEmpty()) {
            try {
                val bitmap = decodeBase64ToBitmap(transaction.documentBase64)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE

                    // Add click listener to view full image
                    imageView.setOnClickListener {
                        showFullImageDialog(bitmap)
                    }
                } else {
                    // Show placeholder for failed image decode
                    imageView.setBackgroundColor(Color.parseColor("#666666"))
                    imageView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding base64 image: ${e.message}", e)
                imageView.setBackgroundColor(Color.parseColor("#666666"))
                imageView.visibility = View.VISIBLE
            }
        } else {
            imageView.visibility = View.GONE
        }

        transactionLayout.addView(contentLayout)
        transactionLayout.addView(imageView)

        return transactionLayout
    }

    /**
     * Decode base64 string to bitmap
     */
    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding base64 to bitmap: ${e.message}", e)
            null
        }
    }

    /**
     * Show full image in dialog
     */
    private fun showFullImageDialog(bitmap: Bitmap) {
        val imageView = ImageView(this).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(16, 16, 16, 16)
        }

        AlertDialog.Builder(this)
            .setTitle("Document Image")
            .setView(imageView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun clearTransactionViews() {
        try {
            transactionsContainer.removeAllViews()
            Log.d(TAG, "Transaction views cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing transaction views: ${e.message}", e)
        }
    }

    private fun showEmptyState(isEmpty: Boolean) {
        try {
            Log.d(TAG, "Showing empty state: $isEmpty")
            if (isEmpty) {
                tvNoTransactions.visibility = View.VISIBLE
                scrollView.visibility = View.GONE
            } else {
                tvNoTransactions.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing empty state: ${e.message}", e)
        }
    }

    private fun updateTransactionsList(hasTransactions: Boolean) {
        try {
            Log.d(TAG, "Updating transactions list. Has transactions: $hasTransactions")
            showEmptyState(!hasTransactions)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating transactions list: ${e.message}", e)
        }
    }
}

// Updated TransactionItem data class to include documentBase64
data class TransactionItem(
    val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val date: String = "",
    val timestamp: Long = 0L,
    val type: String = "",
    val documentBase64: String? = null // Add this field for base64 image data
)