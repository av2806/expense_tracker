package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.example.expensetracker.ui.screens.ExpenseDashboard
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this).get(ExpenseViewModel::class.java)

        setContent {
            ExpenseTrackerTheme {
                ExpenseDashboard(viewModel)
            }
        }
    }
}