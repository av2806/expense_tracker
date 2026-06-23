package com.example.expensetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, Category::class, MerchantCategoryMapping::class],
    version = 3,
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
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO categories (name, color, isCustom) VALUES ('Food', '#FF9100', 0)")
                        db.execSQL("INSERT INTO categories (name, color, isCustom) VALUES ('Transport', '#2979FF', 0)")
                        db.execSQL("INSERT INTO categories (name, color, isCustom) VALUES ('Shopping', '#FF4081', 0)")
                        db.execSQL("INSERT INTO categories (name, color, isCustom) VALUES ('Entertainment', '#7C4DFF', 0)")
                        db.execSQL("INSERT INTO categories (name, color, isCustom) VALUES ('Bills', '#00E676', 0)")
                        db.execSQL("INSERT INTO categories (name, color, isCustom) VALUES ('Other', '#9E9E9E', 0)")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}