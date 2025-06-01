package com.fake.wastingmoney

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth

class Categories : AppCompatActivity() {

    // Firebase instances
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Data storage
    private val categories = mutableListOf<Category>()
    private var monthlySpending = mutableMapOf<String, Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        initFirebase()
        setupClickListeners()
        loadDataFromFirebase()
    }

    private fun initFirebase() {
        // Initialize Firebase
        db = Firebase.firestore
        auth = Firebase.auth
    }

    private fun setupClickListeners() {
        // Find and setup menu icon click
        try {
            val menuIcon = findViewById<LinearLayout>(R.id.menu_icon) // Add this ID to your XML
            menuIcon?.setOnClickListener {
                Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
                // Add your menu logic here
            }
        } catch (e: Exception) {
            // If R.id.menu_icon doesn't exist, we'll handle it differently
            setupMenuIconByPosition()
        }

        // Setup category clicks - find GridLayout and its children
        try {
            val gridLayout = findViewById<android.widget.GridLayout>(R.id.categories_grid) // Add this ID to your XML
            if (gridLayout != null) {
                setupCategoryClicksFromGrid(gridLayout)
            } else {
                setupCategoryClicksByPosition()
            }
        } catch (e: Exception) {
            setupCategoryClicksByPosition()
        }
    }

    private fun setupMenuIconByPosition() {
        try {
            // Find the root view and navigate to menu icon
            val rootView = findViewById<View>(android.R.id.content)
            val menuIcon = findViewByPosition(rootView, "menu")
            menuIcon?.setOnClickListener {
                Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Fallback - just show a message
            Toast.makeText(this, "Menu setup failed, but app is working", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCategoryClicksFromGrid(gridLayout: android.widget.GridLayout) {
        val categoryNames = listOf("TOILETRIES", "CAR", "WATER & LIGHTS", "GROCERIES", "CLOTHING", "OTHER")

        for (i in 0 until gridLayout.childCount.coerceAtMost(categoryNames.size)) {
            val categoryFrame = gridLayout.getChildAt(i) as? FrameLayout
            categoryFrame?.setOnClickListener {
                onCategoryClick(categoryNames[i])
            }
        }
    }

    private fun setupCategoryClicksByPosition() {
        try {
            // Find all FrameLayouts (category circles) in the view hierarchy
            val categoryFrames = mutableListOf<FrameLayout>()
            findAllFrameLayouts(findViewById(android.R.id.content), categoryFrames)

            val categoryNames = listOf("TOILETRIES", "CAR", "WATER & LIGHTS", "GROCERIES", "CLOTHING", "OTHER")

            // Filter to get only the category circles (should be the ones with specific size)
            val categoryCircles = categoryFrames.filter { frame ->
                val params = frame.layoutParams
                params.width == dpToPx(120) && params.height == dpToPx(120)
            }

            categoryCircles.forEachIndexed { index, frame ->
                if (index < categoryNames.size) {
                    frame.setOnClickListener {
                        onCategoryClick(categoryNames[index])
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Category setup completed with limited functionality", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findAllFrameLayouts(view: View, frameLayouts: MutableList<FrameLayout>) {
        if (view is FrameLayout) {
            frameLayouts.add(view)
        }
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                findAllFrameLayouts(view.getChildAt(i), frameLayouts)
            }
        }
    }

    private fun findViewByPosition(view: View, type: String): View? {
        // This is a simplified fallback method
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val result = findViewByPosition(child, type)
                if (result != null) return result
            }
        }
        return null
    }

    private fun onCategoryClick(categoryName: String) {
        Toast.makeText(this, "Clicked on $categoryName", Toast.LENGTH_SHORT).show()
        // Navigate to category details or add expense
        // You can start a new activity here to add expenses to this category
    }

    private fun loadDataFromFirebase() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        // Load categories and spending data
        loadCategories()
        loadMonthlySpending()
    }

    private fun loadCategories() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("categories")
            .get()
            .addOnSuccessListener { documents ->
                categories.clear()
                for (document in documents) {
                    val category = document.toObject(Category::class.java)
                    categories.add(category)
                }
                updateCategoryDisplay()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading categories: ${exception.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMonthlySpending() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("expenses")
            .get()
            .addOnSuccessListener { documents ->
                monthlySpending.clear()
                for (document in documents) {
                    val expense = document.toObject(Expense::class.java)
                    val currentAmount = monthlySpending[expense.category] ?: 0f
                    monthlySpending[expense.category] = currentAmount + expense.amount
                }
                updateMonthlyGraph()
                updateTrends()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading expenses: ${exception.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateCategoryDisplay() {
        // Update category circles with spending amounts
        Toast.makeText(this, "Categories loaded: ${categories.size}", Toast.LENGTH_SHORT).show()
    }

    private fun updateMonthlyGraph() {
        // Update the monthly graph bars
        Toast.makeText(this, "Monthly spending data loaded", Toast.LENGTH_SHORT).show()
    }

    private fun updateTrends() {
        // Update trend information
        Toast.makeText(this, "Trends updated", Toast.LENGTH_SHORT).show()
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    // Function to add a new category to Firebase
    fun addCategoryToFirebase(categoryName: String) {
        val userId = auth.currentUser?.uid ?: return

        val category = Category(categoryName, 0f)

        db.collection("users")
            .document(userId)
            .collection("categories")
            .add(category)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Category added successfully", Toast.LENGTH_SHORT).show()
                loadCategories() // Refresh the display
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error adding category: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    // Function to add an expense to Firebase
    fun addExpenseToFirebase(category: String, amount: Float, description: String) {
        val userId = auth.currentUser?.uid ?: return

        val expense = Expense(
            category = category,
            amount = amount,
            description = description,
            timestamp = System.currentTimeMillis(),
            userId = userId
        )

        db.collection("users")
            .document(userId)
            .collection("expenses")
            .add(expense)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Expense added successfully", Toast.LENGTH_SHORT).show()
                loadMonthlySpending() // Refresh the graphs
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error adding expense: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }
}

// Data classes for Firebase
data class Category(
    val name: String = "",
    val amountSpent: Float = 0f,
    val budget: Float = 0f
)

data class Expense(
    val category: String = "",
    val amount: Float = 0f,
    val description: String = "",
    val timestamp: Long = 0L,
    val userId: String = ""
)