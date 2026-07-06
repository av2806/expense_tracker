package com.example.expensetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Transaction::class, Category::class, MerchantCategoryMapping::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantMappingDao(): MerchantCategoryMappingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance

                // Populate data on first run
                CoroutineScope(Dispatchers.IO).launch {
                    populateDefaultData(instance)
                }

                instance
            }
        }

        private suspend fun populateDefaultData(db: AppDatabase) {
            val categoryDao = db.categoryDao()
            val merchantDao = db.merchantMappingDao()

            // Add default categories (Room will handle duplicates gracefully)
            val defaultCategories = listOf(
                Category(id = 1, name = "Food", color = "#FF6B6B", isCustom = false),
                Category(id = 2, name = "Transport", color = "#4ECDC4", isCustom = false),
                Category(id = 3, name = "Shopping", color = "#FFE66D", isCustom = false),
                Category(id = 4, name = "Entertainment", color = "#95E1D3", isCustom = false),
                Category(id = 5, name = "Bills", color = "#C7CEEA", isCustom = false),
                Category(id = 6, name = "Other", color = "#B0B0B0", isCustom = false)
            )

            for (category in defaultCategories) {
                try {
                    categoryDao.insertCategory(category)
                } catch (e: Exception) {
                    // Already exists, skip
                }
            }

            // Add merchant mappings
            val mappings = listOf(
                MerchantCategoryMapping(keyword = "swiggy", categoryId = 1, confidence = 95),
                MerchantCategoryMapping(keyword = "zomato", categoryId = 1, confidence = 95),
                MerchantCategoryMapping(keyword = "foodpanda", categoryId = 1, confidence = 95),
                MerchantCategoryMapping(keyword = "uber", categoryId = 2, confidence = 90),
                MerchantCategoryMapping(keyword = "ola", categoryId = 2, confidence = 90),
                MerchantCategoryMapping(keyword = "amazon", categoryId = 3, confidence = 92),
                MerchantCategoryMapping(keyword = "flipkart", categoryId = 3, confidence = 92),
                MerchantCategoryMapping(keyword = "zepto", categoryId = 3, confidence = 85),
                MerchantCategoryMapping(keyword = "netflix", categoryId = 4, confidence = 95),
                MerchantCategoryMapping(keyword = "spotify", categoryId = 4, confidence = 95)
            )

            for (mapping in mappings) {
                try {
                    merchantDao.insertMapping(mapping)
                } catch (e: Exception) {
                    // Already exists, skip
                }
            }
        }
    }
}