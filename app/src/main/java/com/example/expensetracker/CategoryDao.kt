package com.example.expensetracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert
    fun insertCategory(category: Category)

    @Delete
    fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE isCustom = 0")
    fun getDefaultCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE isCustom = 1")
    fun getCustomCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryById(id: Int): Category?
}