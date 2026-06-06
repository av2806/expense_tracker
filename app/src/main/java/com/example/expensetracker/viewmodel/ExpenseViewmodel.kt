package com.example.expensetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.database.AppDatabase
import com.example.expensetracker.database.Transaction
import com.example.expensetracker.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val allTransactions: Flow<List<Transaction>>
    val totalExpenses: Flow<Int?>

    init {
        val db = AppDatabase.getDatabase(application)
        val transactionDao = db.transactionDao()
        repository = TransactionRepository(transactionDao)
        allTransactions = repository.allTransactions
        totalExpenses = repository.totalExpenses
    }

    fun addTransaction(title: String, amount: Int, category: String) {
        viewModelScope.launch {
            val transaction = Transaction(
                title = title,
                amount = amount,
                category = category
            )
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}