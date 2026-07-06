package com.example.expensetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Int,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val paymentMethod: String = "UPI",
    val source: String = "manual",
    val phoneNumber: String = "",
    val notes: String = ""
)