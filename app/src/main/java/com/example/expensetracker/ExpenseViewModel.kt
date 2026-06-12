package com.example.expensetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val allTransactions: Flow<List<Transaction>>
    val totalExpenses: Flow<Int?>
    val allCategories: Flow<List<Category>>

    init {
        val db = AppDatabase.getDatabase(application)
        val transactionDao = db.transactionDao()
        val categoryDao = db.categoryDao()
        val merchantMappingDao = db.merchantMappingDao()

        repository = TransactionRepository(transactionDao, categoryDao, merchantMappingDao)
        allTransactions = repository.allTransactions
        totalExpenses = repository.totalExpenses
        allCategories = repository.allCategories
    }

    fun addTransaction(title: String, amount: Int, category: String, paymentMethod: String = "Cash") {
        viewModelScope.launch(Dispatchers.IO) {
            val transaction = Transaction(
                title = title,
                amount = amount,
                category = category,
                paymentMethod = paymentMethod
            )
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transaction)
        }
    }

    fun getTotalByPaymentMethod(method: String): Flow<Int?> {
        return repository.getTotalByPaymentMethod(method)
    }

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> {
        return repository.getTransactionsByCategory(category)
    }

    fun getCategoryIdByKeyword(keyword: String): Int? {
        return repository.getCategoryIdByKeyword(keyword)
    }

    fun getConfidenceByKeyword(keyword: String): Int? {
        return repository.getConfidenceByKeyword(keyword)
    }
}