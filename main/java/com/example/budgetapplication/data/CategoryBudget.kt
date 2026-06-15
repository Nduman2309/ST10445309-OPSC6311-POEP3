package com.example.budgetapplication.data

// KEY: MISSION_OBJECTIVE_MODEL
data class CategoryBudget(
    val id: String = "",
    val userId: String = "",
    val category: String = "",
    val limitAmount: Double = 0.0,
    val currentSaved: Double = 0.0,
    val objective: String = "",
    val timelineDate: Long = 0,
    val isCompleted: Boolean = false
)
