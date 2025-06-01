package com.fake.wastingmoney

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class Home : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSummaryText: TextView

    private lateinit var cardAddExpense: CardView
    private lateinit var cardCategories: CardView
    private lateinit var cardTransactions: CardView
    private lateinit var cardBudgetGoal: CardView
    private lateinit var cardDashboard: CardView
    private lateinit var cardLogout: CardView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        setContentView(R.layout.activity_home)

        // Initialize views
        tvWelcome = findViewById(R.id.tvWelcome)
        progressBar = findViewById(R.id.progressBarSummary)
        tvSummaryText = findViewById(R.id.tvSummaryText)

        cardAddExpense = findViewById(R.id.cardAddExpense)
        cardCategories = findViewById(R.id.cardCategories)
        cardTransactions = findViewById(R.id.cardTransactions)
        cardBudgetGoal = findViewById(R.id.cardBudgetGoal)
        cardDashboard = findViewById(R.id.cardDashboard)
        cardLogout = findViewById(R.id.cardLogout)

        // Set personalized welcome message if user is logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val displayName = currentUser.displayName ?: currentUser.email ?: "User"
            tvWelcome.text = "Welcome, $displayName"
        } else {
            tvWelcome.text = "Welcome, User"
        }

        // Click listeners for navigation
        cardAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpense::class.java))
        }

        cardCategories.setOnClickListener {
            startActivity(Intent(this, Categories::class.java))
        }

        cardTransactions.setOnClickListener {
            startActivity(Intent(this, Transaction::class.java))
        }

        cardBudgetGoal.setOnClickListener {
            startActivity(Intent(this, BudgetGoal::class.java))
        }

        cardDashboard.setOnClickListener {
            // If you have DashboardActivity, uncomment the line below:
            startActivity(Intent(this, DashboardActivity::class.java))

            // Temporary placeholder - remove this when you have the actual activity

        }

        cardLogout.setOnClickListener {
            logout()
        }

        // Show loading/progress simulation
        showLoading()
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        tvSummaryText.visibility = View.GONE

        progressBar.postDelayed({
            progressBar.visibility = View.GONE
            tvSummaryText.visibility = View.VISIBLE
            tvSummaryText.text = "Current Month Summary: R 2,345.00"
        }, 2000)
    }

    private fun logout() {
        auth.signOut()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}