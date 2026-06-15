package com.example.budgetapplication.data

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val date: Long = 0,
    val type: String = "", // "INCOME" or "EXPENSE"
    val receiptBase64: String? = null
)
