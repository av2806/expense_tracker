package com.example.expensetracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantCategoryMappingDao {

    @Insert
    fun insertMapping(mapping: MerchantCategoryMapping)

    @Update
    fun updateMapping(mapping: MerchantCategoryMapping)

    @Query("SELECT * FROM merchant_category_mapping ORDER BY confidence DESC")
    fun getAllMappings(): Flow<List<MerchantCategoryMapping>>

    @Query("SELECT * FROM merchant_category_mapping WHERE keyword LIKE :keyword LIMIT 1")
    fun getMappingByKeyword(keyword: String): MerchantCategoryMapping?

    @Query("SELECT categoryId FROM merchant_category_mapping WHERE keyword LIKE :keyword LIMIT 1")
    fun getCategoryIdByKeyword(keyword: String): Int?

    @Query("SELECT confidence FROM merchant_category_mapping WHERE keyword LIKE :keyword")
    fun getConfidenceByKeyword(keyword: String): Int?
}