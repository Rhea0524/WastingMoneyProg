package com.fake.wastingmoney

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.*

class ManageCategoriesActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etCategoryName: EditText
    private lateinit var btnAddCategory: Button
    private lateinit var lvCategories: ListView

    private val categoryList = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    // Firebase Realtime Database reference
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_categories)

        // Initialize views
        btnBack = findViewById(R.id.btnBack)
        etCategoryName = findViewById(R.id.etCategoryName)
        btnAddCategory = findViewById(R.id.btnAddCategory)
        lvCategories = findViewById(R.id.lvCategories)

        // Set up custom adapter for white text on dark background
        adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, categoryList) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(android.graphics.Color.WHITE)
                textView.textSize = 16f
                textView.setPadding(16, 16, 16, 16)
                return view
            }
        }
        lvCategories.adapter = adapter

        // Initialize Firebase Realtime Database reference
        database = FirebaseDatabase.getInstance().getReference("categories")

        // Load categories from Firebase
        loadCategoriesFromFirebase()

        // Back button click listener
        btnBack.setOnClickListener {
            finish() // Close current activity and return to previous one
        }

        // Add category button click listener
        btnAddCategory.setOnClickListener {
            val category = etCategoryName.text.toString().trim().uppercase()
            if (category.isEmpty()) {
                showErrorDialog("Please enter a category name")
            } else if (categoryList.contains(category)) {
                showErrorDialog("Category already exists")
            } else {
                addCategoryToFirebase(category)
            }
        }

        // Delete category on long click with confirmation dialog
        lvCategories.setOnItemLongClickListener { _, _, position, _ ->
            val categoryToRemove = categoryList[position]
            showDeleteConfirmationDialog(categoryToRemove)
            true
        }

        // Optional: Regular click to show category details or edit
        lvCategories.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categoryList[position]
            Toast.makeText(this, "Selected: $selectedCategory", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCategoriesFromFirebase() {
        // Show loading state
        showLoadingToast("Loading categories...")

        // Listen for changes in "categories" node
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryList.clear()
                for (categorySnapshot in snapshot.children) {
                    val category = categorySnapshot.getValue(String::class.java)
                    category?.let { categoryList.add(it) }
                }
                // Sort categories alphabetically
                categoryList.sort()
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                showErrorDialog("Failed to load categories: ${error.message}")
            }
        })
    }

    private fun addCategoryToFirebase(category: String) {
        // Use category name as key to avoid duplicates (replace special chars)
        val key = sanitizeFirebaseKey(category)

        database.child(key).setValue(category)
            .addOnSuccessListener {
                etCategoryName.text.clear()
                showSuccessToast("Category '$category' added successfully")
            }
            .addOnFailureListener { exception ->
                showErrorDialog("Failed to add category: ${exception.message}")
            }
    }

    private fun removeCategoryFromFirebase(category: String) {
        val key = sanitizeFirebaseKey(category)

        database.child(key).removeValue()
            .addOnSuccessListener {
                showSuccessToast("Category '$category' deleted successfully")
            }
            .addOnFailureListener { exception ->
                showErrorDialog("Failed to delete category: ${exception.message}")
            }
    }

    private fun sanitizeFirebaseKey(input: String): String {
        return input.replace(".", "_dot_")
            .replace("#", "_hash_")
            .replace("$", "_dollar_")
            .replace("[", "_lbracket_")
            .replace("]", "_rbracket_")
            .replace("/", "_slash_")
    }

    private fun showDeleteConfirmationDialog(category: String) {
        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '$category'?")
            .setPositiveButton("Delete") { _, _ ->
                removeCategoryFromFirebase(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSuccessToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoadingToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove Firebase listeners to prevent memory leaks
        database.removeEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}