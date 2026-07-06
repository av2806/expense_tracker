package com.example.expensetracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    fun insertTransaction(transaction: Transaction)

    @Update
    fun updateTransaction(transaction: Transaction)

    @Delete
    fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions")
    fun getTotalExpenses(): Flow<Int?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): Transaction?

    @Query("SELECT * FROM transactions WHERE category = :categoryName ORDER BY timestamp DESC")
    fun getTransactionsByCategory(categoryName: String): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE paymentMethod = :paymentMethod")
    fun getTotalByPaymentMethod(paymentMethod: String): Flow<Int?>

    @Query("""
    SELECT category, SUM(amount) AS totalAmount
    FROM transactions
    GROUP BY category
    ORDER BY totalAmount DESC
""")
    fun getCategorySpending(): Flow<List<CategorySpending>>

    @Query("""
    SELECT date(timestamp / 1000, 'unixepoch') AS day,
           SUM(amount) AS totalAmount
    FROM transactions
    GROUP BY day
    ORDER BY day ASC
""")
    fun getDailySpending(): Flow<List<DailySpending>>

    @Query("""
    SELECT strftime('%Y-%m', timestamp / 1000, 'unixepoch') AS month,
           SUM(amount) AS totalAmount
    FROM transactions
    GROUP BY month
    ORDER BY month ASC
""")
    fun getMonthlySpending(): Flow<List<MonthlySpending>>
}