package com.fake.wastingmoney

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.textfield.TextInputEditText
import android.widget.Button
import android.widget.TextView
import java.util.*

class BudgetGoal : AppCompatActivity() {

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // UI components
    private lateinit var btnSelectMonthYear: Button
    private lateinit var tvSelectedMonthYear: TextView
    private lateinit var etMinGoal: TextInputEditText
    private lateinit var etMaxGoal: TextInputEditText
    private lateinit var btnSaveGoal: Button

    // Selected date variables
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_goal)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        initViews()

        // Set up click listeners
        setupClickListeners()

        // Set initial month/year display
        updateMonthYearDisplay()
    }

    private fun initViews() {
        btnSelectMonthYear = findViewById(R.id.btnSelectMonthYear)
        tvSelectedMonthYear = findViewById(R.id.tvSelectedMonthYear)
        etMinGoal = findViewById(R.id.etMinGoal)
        etMaxGoal = findViewById(R.id.etMaxGoal)
        btnSaveGoal = findViewById(R.id.btnSaveGoal)
    }

    private fun setupClickListeners() {
        btnSelectMonthYear.setOnClickListener {
            showMonthYearPicker()
        }

        btnSaveGoal.setOnClickListener {
            saveBudgetGoal()
        }
    }

    private fun showMonthYearPicker() {
        val calendar = Calendar.getInstance()
        calendar.set(selectedYear, selectedMonth, 1)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, _ ->
                selectedYear = year
                selectedMonth = month
                updateMonthYearDisplay()
            },
            selectedYear,
            selectedMonth,
            1
        )

        // Hide day picker to show only month and year
        datePickerDialog.datePicker.findViewById<android.widget.CalendarView>(
            resources.getIdentifier("calendar_view", "id", "android")
        )?.visibility = android.view.View.GONE

        datePickerDialog.show()
    }

    private fun updateMonthYearDisplay() {
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        val displayText = "${monthNames[selectedMonth]} $selectedYear"
        tvSelectedMonthYear.text = displayText
    }

    private fun saveBudgetGoal() {
        val minGoalText = etMinGoal.text.toString().trim()
        val maxGoalText = etMaxGoal.text.toString().trim()

        // Validate inputs
        if (!validateInputs(minGoalText, maxGoalText)) {
            return
        }

        val minGoal = minGoalText.toDouble()
        val maxGoal = maxGoalText.toDouble()

        // Validate that min is not greater than max
        if (minGoal > maxGoal) {
            Toast.makeText(this, "Minimum goal cannot be greater than maximum goal", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if user is authenticated
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to save budget goals", Toast.LENGTH_SHORT).show()
            return
        }

        // Create budget goal data - explicitly specify type to avoid inference issues
        val budgetGoalData: HashMap<String, Any> = hashMapOf(
            "userId" to currentUser.uid,
            "month" to selectedMonth,
            "year" to selectedYear,
            "monthYear" to "${selectedMonth + 1}/$selectedYear",
            "minGoal" to minGoal,
            "maxGoal" to maxGoal,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        // Save to Firestore
        saveBudgetGoalToFirestore(budgetGoalData)
    }

    private fun validateInputs(minGoalText: String, maxGoalText: String): Boolean {
        if (minGoalText.isEmpty()) {
            etMinGoal.error = "Please enter minimum goal"
            etMinGoal.requestFocus()
            return false
        }

        if (maxGoalText.isEmpty()) {
            etMaxGoal.error = "Please enter maximum goal"
            etMaxGoal.requestFocus()
            return false
        }

        try {
            val minGoal = minGoalText.toDouble()
            val maxGoal = maxGoalText.toDouble()

            if (minGoal < 0) {
                etMinGoal.error = "Minimum goal must be positive"
                etMinGoal.requestFocus()
                return false
            }

            if (maxGoal < 0) {
                etMaxGoal.error = "Maximum goal must be positive"
                etMaxGoal.requestFocus()
                return false
            }

        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter valid numeric values", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveBudgetGoalToFirestore(budgetGoalData: HashMap<String, Any>) {
        val currentUser = auth.currentUser ?: return

        // Create document ID based on user ID and month/year for easy updates
        val documentId = "${currentUser.uid}_${selectedMonth + 1}_$selectedYear"

        firestore.collection("budgetGoals")
            .document(documentId)
            .set(budgetGoalData)
            .addOnSuccessListener {
                Toast.makeText(this, "Budget goal saved successfully!", Toast.LENGTH_SHORT).show()
                clearInputs()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save budget goal: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearInputs() {
        etMinGoal.text?.clear()
        etMaxGoal.text?.clear()
    }

    // Optional: Load existing budget goal for the selected month/year
    private fun loadExistingBudgetGoal() {
        val currentUser = auth.currentUser ?: return
        val documentId = "${currentUser.uid}_${selectedMonth + 1}_$selectedYear"

        firestore.collection("budgetGoals")
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val minGoal = document.getDouble("minGoal")
                    val maxGoal = document.getDouble("maxGoal")

                    etMinGoal.setText(minGoal?.toString() ?: "")
                    etMaxGoal.setText(maxGoal?.toString() ?: "")

                    Toast.makeText(this, "Loaded existing budget goal", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // Handle error silently or show message if needed
            }
    }

    // Call this method when month/year is changed to load existing data
    private fun onMonthYearChanged() {
        updateMonthYearDisplay()
        loadExistingBudgetGoal()
    }
}