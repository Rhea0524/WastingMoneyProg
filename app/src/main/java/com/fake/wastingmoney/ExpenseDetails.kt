package com.fake.wastingmoney

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// import com.bumptech.glide.Glide // Optional - add Glide dependency if needed
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ExpenseDetails : AppCompatActivity() {

    private lateinit var tvAmount: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var ivExpensePhoto: ImageView
    private lateinit var progressBar: ProgressBar

    private val db = Firebase.firestore
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "ExpenseDetails"
        const val EXTRA_EXPENSE_ID = "expenseId"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_DATE = "date"
        const val EXTRA_TIME = "time"
        const val EXTRA_PHOTO_VISIBLE = "photoVisible"
        const val EXTRA_PHOTO_URL = "photoUrl"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        setContentView(R.layout.activity_expense_details)

        initializeViews()
        loadExpenseData()
    }

    private fun initializeViews() {
        tvAmount = findViewById(R.id.tvAmount)
        tvDescription = findViewById(R.id.tvDescription)
        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)
        ivExpensePhoto = findViewById(R.id.ivExpensePhoto)

        // Add a progress bar to your layout if not already present
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }
    }

    private fun loadExpenseData() {
        val expenseId = intent.getStringExtra(EXTRA_EXPENSE_ID)

        if (!expenseId.isNullOrEmpty()) {
            showLoading(true)
            loadExpenseDetailsFromFirestore(expenseId)
        } else {
            loadExpenseDetailsFromIntent()
        }
    }

    private fun loadExpenseDetailsFromFirestore(expenseId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "User not authenticated")
            showError("Please sign in to view expense details")
            loadExpenseDetailsFromIntent()
            return
        }

        val docRef = db.collection("users")
            .document(currentUser.uid)
            .collection("expenses")
            .document(expenseId)

        docRef.get()
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document != null && document.exists()) {
                    try {
                        populateExpenseDetails(document.data ?: emptyMap())
                        Log.d(TAG, "Expense details loaded successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing expense data", e)
                        showError("Error loading expense details")
                        loadExpenseDetailsFromIntent()
                    }
                } else {
                    Log.w(TAG, "Expense document does not exist: $expenseId")
                    showError("Expense not found")
                    loadExpenseDetailsFromIntent()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.e(TAG, "Error loading expense from Firestore", exception)
                showError("Failed to load expense details")
                loadExpenseDetailsFromIntent()
            }
    }

    private fun populateExpenseDetails(data: Map<String, Any>) {
        // Format amount with currency
        val amount = data["amount"] as? Double ?: 0.0
        val formattedAmount = NumberFormat.getCurrencyInstance().format(amount)
        tvAmount.text = formattedAmount

        // Set description
        tvDescription.text = data["description"] as? String ?: "No description"

        // Format date
        val timestamp = data["timestamp"] as? com.google.firebase.Timestamp
        if (timestamp != null) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = timestamp.toDate()

            tvDate.text = dateFormat.format(date)
            tvTime.text = timeFormat.format(date)
        } else {
            // Fallback to string fields
            tvDate.text = data["date"] as? String ?: "Unknown date"
            tvTime.text = data["time"] as? String ?: "Unknown time"
        }

        // Handle expense photo
        val photoUrl = data["photoUrl"] as? String
        val hasPhoto = data["hasPhoto"] as? Boolean ?: false

        if (hasPhoto && !photoUrl.isNullOrEmpty()) {
            loadExpensePhoto(photoUrl)
        } else {
            ivExpensePhoto.visibility = View.GONE
        }

        // Additional fields that might be useful
        val category = data["category"] as? String
        val notes = data["notes"] as? String

        // You can add these to your layout if needed
        Log.d(TAG, "Category: $category, Notes: $notes")
    }

    private fun loadExpensePhoto(photoUrl: String) {
        ivExpensePhoto.visibility = View.VISIBLE

        // Option 1: Using Picasso (alternative to Glide)
        // Add to build.gradle: implementation 'com.squareup.picasso:picasso:2.8'
        /*
        Picasso.get()
            .load(photoUrl)
            .centerCrop()
            .fit()
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_close_clear_cancel)
            .into(ivExpensePhoto)
        */

        // Option 2: Using basic approach with Firebase Storage reference
        try {
            val imageRef = storage.getReferenceFromUrl(photoUrl)
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                // You can use any image loading library here or implement custom loading
                // For now, just set a placeholder
                ivExpensePhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                Log.d(TAG, "Image URL retrieved: $uri")
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Error getting image download URL", exception)
                ivExpensePhoto.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image", e)
            ivExpensePhoto.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        }
    }

    private fun loadExpenseDetailsFromIntent() {
        val amount = intent.getStringExtra(EXTRA_AMOUNT) ?: "$100.00"
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: "Grocery shopping at the supermarket"
        val date = intent.getStringExtra(EXTRA_DATE) ?: "May 7, 2025"
        val time = intent.getStringExtra(EXTRA_TIME) ?: "10:00 AM - 11:00 AM"
        val photoVisible = intent.getBooleanExtra(EXTRA_PHOTO_VISIBLE, false)
        val photoUrl = intent.getStringExtra(EXTRA_PHOTO_URL)

        tvAmount.text = amount
        tvDescription.text = description
        tvDate.text = date
        tvTime.text = time

        if (photoVisible && !photoUrl.isNullOrEmpty()) {
            loadExpensePhoto(photoUrl)
        } else {
            ivExpensePhoto.visibility = View.GONE
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE

        // Optionally disable other views while loading
        tvAmount.alpha = if (show) 0.5f else 1.0f
        tvDescription.alpha = if (show) 0.5f else 1.0f
        tvDate.alpha = if (show) 0.5f else 1.0f
        tvTime.alpha = if (show) 0.5f else 1.0f
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, message)
    }

    // Method to update expense (if needed)
    fun updateExpense(expenseId: String, updates: Map<String, Any>) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showError("Please sign in to update expense")
            return
        }

        showLoading(true)

        val docRef = db.collection("users")
            .document(currentUser.uid)
            .collection("expenses")
            .document(expenseId)

        docRef.update(updates)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Expense updated successfully", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Expense updated successfully")
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.e(TAG, "Error updating expense", exception)
                showError("Failed to update expense")
            }
    }

    // Method to delete expense (if needed)
    fun deleteExpense(expenseId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showError("Please sign in to delete expense")
            return
        }

        showLoading(true)

        val docRef = db.collection("users")
            .document(currentUser.uid)
            .collection("expenses")
            .document(expenseId)

        docRef.delete()
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Expense deleted successfully", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Expense deleted successfully")
                finish() // Close the activity
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.e(TAG, "Error deleting expense", exception)
                showError("Failed to delete expense")
            }
    }
}