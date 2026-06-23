package com.example.expensetracker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogManager {
    
    data class LogEntry(
        val timestamp: String,
        val type: String,
        val message: String
    )

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(type: String, message: String) {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val entry = LogEntry(timestamp, type, message)
        
        val currentList = _logs.value.toMutableList()
        currentList.add(0, entry)
        if (currentList.size > 50) {
            currentList.removeAt(currentList.size - 1)
        }
        _logs.value = currentList
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
