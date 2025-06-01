package com.fake.wastingmoney

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fake.wastingmoney.model.Income
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddIncome : AppCompatActivity() {

    // Changed from TextInputEditText to EditText
    private lateinit var etAmount: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerSource: AutoCompleteTextView
    private lateinit var btnDatePicker: Button
    private lateinit var btnSaveIncome: Button

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_add_income)
            initializeViews()
            setupSourceDropdown()
            setupDatePicker()
            setupClickListeners()
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading AddIncome: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            etAmount = findViewById(R.id.et_amount)
            etDescription = findViewById(R.id.et_description)
            spinnerSource = findViewById(R.id.spinner_source)
            btnDatePicker = findViewById(R.id.btn_date_picker)
            btnSaveIncome = findViewById(R.id.btn_save_income)
        } catch (e: Exception) {
            throw Exception("Failed to initialize views. Please check your layout file has all required IDs: ${e.message}")
        }
    }

    private fun setupClickListeners() {
        btnSaveIncome.setOnClickListener {
            saveIncome()
        }
    }

    private fun setupSourceDropdown() {
        val sources = listOf("Salary", "Freelance", "Gift", "Interest", "Investment", "Bonus", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sources)
        spinnerSource.setAdapter(adapter)

        // Set threshold to show dropdown immediately
        spinnerSource.threshold = 1
    }

    private fun setupDatePicker() {
        // Set initial date
        btnDatePicker.text = dateFormat.format(calendar.time)

        btnDatePicker.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                btnDatePicker.text = dateFormat.format(calendar.time)
            },
            year, month, day
        )

        // Set max date to today (can't add future income)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun saveIncome() {
        if (!validateInputs()) {
            return
        }

        val amountStr = etAmount.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val source = spinnerSource.text.toString().trim()
        val date = btnDatePicker.text.toString()

        val amount = amountStr.toDouble()

        // Check if user is authenticated
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Please login to save income", Toast.LENGTH_SHORT).show()
            return
        }

        // Create income object
        val income = Income(
            amount = amount,
            description = description,
            source = source,
            date = date,
            timestamp = System.currentTimeMillis()
        )

        // Save to Firebase
        saveToFirebase(uid, income)
    }

    private fun validateInputs(): Boolean {
        val amountStr = etAmount.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val source = spinnerSource.text.toString().trim()

        // Validate amount
        if (amountStr.isEmpty()) {
            etAmount.error = "Amount is required"
            etAmount.requestFocus()
            return false
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            etAmount.error = "Enter a valid amount greater than 0"
            etAmount.requestFocus()
            return false
        }

        if (amount > 1000000) {
            etAmount.error = "Amount seems too large. Please verify."
            etAmount.requestFocus()
            return false
        }

        // Validate description
        if (description.isEmpty()) {
            etDescription.error = "Description is required"
            etDescription.requestFocus()
            return false
        }

        if (description.length < 3) {
            etDescription.error = "Description must be at least 3 characters"
            etDescription.requestFocus()
            return false
        }

        // Validate source
        if (source.isEmpty()) {
            spinnerSource.error = "Please select or enter a source"
            spinnerSource.requestFocus()
            return false
        }

        // Clear any previous errors
        etAmount.error = null
        etDescription.error = null
        spinnerSource.error = null

        return true
    }

    private fun saveToFirebase(uid: String, income: Income) {
        val dbRef = FirebaseDatabase.getInstance().getReference("incomes").child(uid)
        val key = dbRef.push().key

        if (key == null) {
            Toast.makeText(this, "Failed to generate database key", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        btnSaveIncome.isEnabled = false
        btnSaveIncome.text = "Saving..."

        dbRef.child(key).setValue(income)
            .addOnSuccessListener {
                Toast.makeText(this, "Income saved successfully!", Toast.LENGTH_SHORT).show()

                // Clear form
                clearForm()

                // Set result to indicate success and redirect to transactions
                setResult(RESULT_OK)

                // Navigate back to transactions page
                redirectToTransactions()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Failed to save income: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnCompleteListener {
                // Reset button state
                btnSaveIncome.isEnabled = true
                btnSaveIncome.text = "ADD"
            }
    }

    private fun redirectToTransactions() {
        // Option 1: If you want to go back to the previous activity (recommended)
        finish()

        //Option 2: If you want to explicitly navigate to transactions activity
        // Uncomment the lines below and replace "TransactionsActivity" with your actual activity name

        val intent = Intent(this, Transaction::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()

    }

    private fun clearForm() {
        etAmount.text.clear()
        etDescription.text.clear()
        spinnerSource.setText("", false)
        calendar.time = Calendar.getInstance().time
        btnDatePicker.text = dateFormat.format(calendar.time)

        // Clear focus and hide keyboard
        etAmount.clearFocus()
        etDescription.clearFocus()
        spinnerSource.clearFocus()
    }

    override fun onBackPressed() {
        // Check if there's unsaved data
        val hasUnsavedData = etAmount.text.isNotEmpty() ||
                etDescription.text.isNotEmpty() ||
                spinnerSource.text.isNotEmpty()

        if (hasUnsavedData) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Are you sure you want to go back?")
                .setPositiveButton("Yes") { _, _ ->
                    super.onBackPressed()
                }
                .setNegativeButton("No", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}