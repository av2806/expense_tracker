package com.example.expensetracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    fun insertTransaction(transaction: Transaction)

    @Delete
    fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions")
    fun getTotalExpenses(): Flow<Int?>

    @Query("SELECT * FROM transactions WHERE category = :categoryName ORDER BY timestamp DESC")
    fun getTransactionsByCategory(categoryName: String): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE paymentMethod = :paymentMethod")
    fun getTotalByPaymentMethod(paymentMethod: String): Flow<Int?>
}