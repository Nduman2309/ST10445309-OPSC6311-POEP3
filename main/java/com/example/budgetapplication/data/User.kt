package com.example.budgetapplication.data

// KEY: ELITE_USER_PROFILE
data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val totalSavings: Double = 0.0,
    val initialIncomeBaseline: Double = 0.0,
    val currentLevel: Int = 0
)
