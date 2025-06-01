package com.fake.wastingmoney.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.fake.wastingmoney.model.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "BudgetTracker.db"
        private const val DATABASE_VERSION = 1

        // Table names
        private const val TABLE_USERS = "users"
        private const val TABLE_CATEGORIES = "categories"
        private const val TABLE_EXPENSES = "expenses"
        private const val TABLE_BUDGET_GOALS = "budget_goals"

        // Common columns
        private const val KEY_ID = "id"

        // User table columns
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"

        // Category table columns
        private const val KEY_CATEGORY_NAME = "name"
        private const val KEY_USER_ID = "user_id"

        // Expense table columns
        private const val KEY_DATE = "date"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_END_TIME = "end_time"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_AMOUNT = "amount"
        private const val KEY_CATEGORY_ID = "category_id"
        private const val KEY_PHOTO_PATH = "photo_path"

        // Budget goal table columns
        private const val KEY_MIN_GOAL = "min_monthly_goal"
        private const val KEY_MAX_GOAL = "max_monthly_goal"
        private const val KEY_MONTH = "month"
        private const val KEY_YEAR = "year"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create users table
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USERNAME TEXT UNIQUE NOT NULL,
                $KEY_PASSWORD TEXT NOT NULL
            )
        """.trimIndent()

        // Create categories table
        val createCategoriesTable = """
            CREATE TABLE $TABLE_CATEGORIES (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_CATEGORY_NAME TEXT NOT NULL,
                $KEY_USER_ID INTEGER NOT NULL,
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID)
            )
        """.trimIndent()

        // Create expenses table
        val createExpensesTable = """
            CREATE TABLE $TABLE_EXPENSES (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_DATE TEXT NOT NULL,
                $KEY_START_TIME TEXT NOT NULL,
                $KEY_END_TIME TEXT NOT NULL,
                $KEY_DESCRIPTION TEXT NOT NULL,
                $KEY_AMOUNT REAL NOT NULL,
                $KEY_CATEGORY_ID INTEGER NOT NULL,
                $KEY_USER_ID INTEGER NOT NULL,
                $KEY_PHOTO_PATH TEXT,
                FOREIGN KEY($KEY_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($KEY_ID),
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID)
            )
        """.trimIndent()

        // Create budget goals table
        val createBudgetGoalsTable = """
            CREATE TABLE $TABLE_BUDGET_GOALS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USER_ID INTEGER NOT NULL,
                $KEY_MIN_GOAL REAL NOT NULL,
                $KEY_MAX_GOAL REAL NOT NULL,
                $KEY_MONTH TEXT NOT NULL,
                $KEY_YEAR INTEGER NOT NULL,
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID)
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createCategoriesTable)
        db.execSQL(createExpensesTable)
        db.execSQL(createBudgetGoalsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BUDGET_GOALS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXPENSES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // User operations
    fun addUser(username: String, password: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USERNAME, username)
            put(KEY_PASSWORD, password)
        }
        val result = db.insert(TABLE_USERS, null, values)
        db.close()
        return result
    }

    fun validateUser(username: String, password: String): User? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(KEY_ID, KEY_USERNAME, KEY_PASSWORD),
            "$KEY_USERNAME = ? AND $KEY_PASSWORD = ?",
            arrayOf(username, password),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val user = User(
                cursor.getLong(0),
                cursor.getString(1),
                cursor.getString(2)
            )
            cursor.close()
            db.close()
            user
        } else {
            cursor.close()
            db.close()
            null
        }
    }

    // Category operations
    fun addCategory(name: String, userId: Long): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_CATEGORY_NAME, name)
            put(KEY_USER_ID, userId)
        }
        val result = db.insert(TABLE_CATEGORIES, null, values)
        db.close()
        return result
    }

    fun getCategoriesForUser(userId: Long): List<Category> {
        val categories = mutableListOf<Category>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_CATEGORIES,
            arrayOf(KEY_ID, KEY_CATEGORY_NAME, KEY_USER_ID),
            "$KEY_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, KEY_CATEGORY_NAME
        )

        if (cursor.moveToFirst()) {
            do {
                categories.add(
                    Category(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getLong(2)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return categories
    }
}