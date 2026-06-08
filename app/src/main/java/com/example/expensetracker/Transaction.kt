package com.example.expensetracker

data class Transaction(
    val id: Int,
    val title: String,
    val amount: Int,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)