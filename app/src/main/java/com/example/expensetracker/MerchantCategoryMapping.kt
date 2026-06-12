package com.example.expensetracker

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchant_category_mapping",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MerchantCategoryMapping(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val keyword: String,
    val categoryId: Int,
    val confidence: Int = 80
)