package com.example.expensetracker

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ExpenseViewModel : ViewModel() {

    private var nextId = 1

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _totalExpenses = MutableStateFlow(0)
    val totalExpenses: StateFlow<Int> = _totalExpenses

    fun addTransaction(title: String, amount: Int, category: String) {
        val newTransaction = Transaction(
            id = nextId++,
            title = title,
            amount = amount,
            category = category
        )
        val currentList = _transactions.value.toMutableList()
        currentList.add(0, newTransaction)
        _transactions.value = currentList
        updateTotal()
    }

    fun deleteTransaction(transaction: Transaction) {
        val currentList = _transactions.value.toMutableList()
        currentList.remove(transaction)
        _transactions.value = currentList
        updateTotal()
    }

    private fun updateTotal() {
        _totalExpenses.value = _transactions.value.sumOf { it.amount }
    }
}