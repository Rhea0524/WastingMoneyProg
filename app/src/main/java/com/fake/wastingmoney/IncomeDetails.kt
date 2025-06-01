package com.fake.wastingmoney

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class IncomeDetails : AppCompatActivity() {

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // UI elements
    private lateinit var logoImageView: ImageView
    private lateinit var tvAmount: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvSource: TextView

    // Income ID passed from previous activity
    private var incomeId: String? = null

    companion object {
        private const val TAG = "IncomeDetails"
        const val EXTRA_INCOME_ID = "income_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income_details)

        // Initialize Firebase
        initializeFirebase()

        // Initialize UI elements
        initializeViews()

        // Get income ID from intent
        incomeId = intent.getStringExtra(EXTRA_INCOME_ID)

        // Load income details
        if (incomeId != null) {
            loadIncomeDetails(incomeId!!)
        } else {
            Toast.makeText(this, "Error: Income ID not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    private fun initializeViews() {
        logoImageView = findViewById(R.id.logoImageView)
        tvAmount = findViewById(R.id.tvAmount)
        tvDescription = findViewById(R.id.tvDescription)
        tvDate = findViewById(R.id.tvDate)
        tvSource = findViewById(R.id.tvSource)
    }

    private fun loadIncomeDetails(incomeId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Show loading state (you might want to add a progress bar)
        showLoadingState()

        // Get income document from Firestore
        firestore.collection("users")
            .document(currentUser.uid)
            .collection("incomes")
            .document(incomeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        // Extract data from document
                        val amount = document.getDouble("amount") ?: 0.0
                        val description = document.getString("description") ?: "No description"
                        val timestamp = document.getTimestamp("date")
                        val source = document.getString("source") ?: "Unknown"

                        // Update UI with data
                        displayIncomeDetails(amount, description, timestamp, source)

                        Log.d(TAG, "Income details loaded successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing income data", e)
                        Toast.makeText(this, "Error loading income data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w(TAG, "Income document does not exist")
                    Toast.makeText(this, "Income record not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error loading income details", exception)
                Toast.makeText(this, "Failed to load income details", Toast.LENGTH_SHORT).show()
                hideLoadingState()
            }
    }

    private fun displayIncomeDetails(
        amount: Double,
        description: String,
        timestamp: com.google.firebase.Timestamp?,
        source: String
    ) {
        // Format and display amount
        val formattedAmount = NumberFormat.getCurrencyInstance().format(amount)
        tvAmount.text = formattedAmount

        // Display description
        tvDescription.text = description

        // Format and display date
        val dateString = if (timestamp != null) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateFormat.format(timestamp.toDate())
        } else {
            "Date not available"
        }
        tvDate.text = dateString

        // Display source
        tvSource.text = source

        // Hide loading state
        hideLoadingState()
    }

    private fun showLoadingState() {
        // Set placeholder text while loading
        tvAmount.text = "Loading..."
        tvDescription.text = "Loading..."
        tvDate.text = "Loading..."
        tvSource.text = "Loading..."
    }

    private fun hideLoadingState() {
        // This method can be used to hide any loading indicators
        // Currently just used for state management
    }

    // Alternative method to load income details with additional error handling
    private fun loadIncomeDetailsWithRetry(incomeId: String, retryCount: Int = 0) {
        val maxRetries = 3

        if (retryCount >= maxRetries) {
            Toast.makeText(this, "Failed to load data after multiple attempts", Toast.LENGTH_LONG).show()
            return
        }

        val currentUser = auth.currentUser ?: return

        firestore.collection("users")
            .document(currentUser.uid)
            .collection("incomes")
            .document(incomeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        val amount = document.getDouble("amount") ?: 0.0
                        val description = document.getString("description") ?: "No description"
                        val timestamp = document.getTimestamp("date")
                        val source = document.getString("source") ?: "Unknown"

                        displayIncomeDetails(amount, description, timestamp, source)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing income data", e)
                        Toast.makeText(this, "Error loading income data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Income record not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error loading income details (attempt ${retryCount + 1})", exception)

                if (retryCount < maxRetries - 1) {
                    // Retry after a delay
                    android.os.Handler(mainLooper).postDelayed({
                        loadIncomeDetailsWithRetry(incomeId, retryCount + 1)
                    }, 1000) // 1 second delay
                } else {
                    Toast.makeText(this, "Failed to load income details", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Method to update income details (if you want to make it editable)
    private fun updateIncomeDetails(
        incomeId: String,
        amount: Double,
        description: String,
        source: String
    ) {
        val currentUser = auth.currentUser ?: return

        val updates = hashMapOf<String, Any>(
            "amount" to amount,
            "description" to description,
            "source" to source,
            "lastModified" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("users")
            .document(currentUser.uid)
            .collection("incomes")
            .document(incomeId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Income updated successfully", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Income updated successfully")
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to update income", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error updating income", exception)
            }
    }

    // Method to delete income record
    private fun deleteIncomeRecord(incomeId: String) {
        val currentUser = auth.currentUser ?: return

        firestore.collection("users")
            .document(currentUser.uid)
            .collection("incomes")
            .document(incomeId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Income deleted successfully", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Income deleted successfully")
                finish() // Close the activity after deletion
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to delete income", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error deleting income", exception)
            }
    }
}