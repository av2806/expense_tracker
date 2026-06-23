package com.example.expensetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val color: String = "#6750A4",
    val isCustom: Boolean = false
) {
    companion object {
        fun getBeautifulColorForCategory(categoryName: String): String {
            val premiumColors = listOf(
                "#FF4081", // Coral Pink
                "#00F0FF", // Laser Cyan
                "#FF007F", // Hot Purple-Pink
                "#00E676", // Neon Green
                "#FF9100", // Vibrant Orange
                "#7C4DFF", // Deep Violet
                "#2979FF", // Electric Blue
                "#FFD740", // Amber Gold
                "#1DE9B6", // Turquoise Teal
                "#FF1744"  // Vibrant Red
            )
            val index = Math.abs(categoryName.trim().lowercase().hashCode()) % premiumColors.size
            return premiumColors[index]
        }
    }
}