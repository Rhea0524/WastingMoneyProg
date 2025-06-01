package com.fake.wastingmoney

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLoginLink: TextView

    // Add Firebase Auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        setContentView(R.layout.activity_register)

        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLoginLink = findViewById(R.id.tvLoginLink)

        tvLoginLink.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // Debug logging
            Log.d("RegisterDebug", "=== REGISTRATION ATTEMPT ===")
            Log.d("RegisterDebug", "Username: '$username'")
            Log.d("RegisterDebug", "Email: '$email'")
            Log.d("RegisterDebug", "Password length: ${password.length}")

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate password length
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading and disable button
            Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()
            btnRegister.isEnabled = false

            // Create user with Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    btnRegister.isEnabled = true

                    Log.d("RegisterDebug", "=== REGISTRATION RESULT ===")
                    Log.d("RegisterDebug", "Task successful: ${task.isSuccessful}")

                    if (task.isSuccessful) {
                        // Registration successful
                        val user = auth.currentUser
                        Log.d("RegisterDebug", "Registration successful!")
                        Log.d("RegisterDebug", "User ID: ${user?.uid}")
                        Log.d("RegisterDebug", "User email: ${user?.email}")

                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

                        // Go back to login screen
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()

                    } else {
                        // Registration failed
                        val exception = task.exception
                        Log.e("RegisterDebug", "Registration failed!")
                        Log.e("RegisterDebug", "Exception: ${exception?.javaClass?.simpleName}")
                        Log.e("RegisterDebug", "Message: ${exception?.message}")

                        val userMessage = when (exception) {
                            is FirebaseAuthException -> {
                                Log.e("RegisterDebug", "Firebase Auth Error Code: ${exception.errorCode}")
                                when (exception.errorCode) {
                                    "ERROR_WEAK_PASSWORD" -> "Password is too weak. Use at least 6 characters."
                                    "ERROR_INVALID_EMAIL" -> "Invalid email format"
                                    "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists"
                                    "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection"
                                    "ERROR_INTERNAL_ERROR" -> "Internal error. Check your Firebase configuration"
                                    else -> "Registration failed: ${exception.errorCode}"
                                }
                            }
                            else -> "Registration failed: ${exception?.message ?: "Unknown error"}"
                        }

                        Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { exception ->
                    btnRegister.isEnabled = true
                    Log.e("RegisterDebug", "Registration failure: ${exception.message}")
                    Toast.makeText(this, "Registration failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}