package com.example.expensetracker

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.ui.screens.ExpenseDashboard
import com.example.expensetracker.ui.screens.TransactionDetailScreen
import com.example.expensetracker.ui.screens.AnalyticsScreen

@Composable
fun AppNavigation(viewModel: ExpenseViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            ExpenseDashboard(
                viewModel = viewModel,
                navController = navController
            )
        }

        composable("transaction_detail") {
            val transaction = viewModel.selectedTransaction.value

            if (transaction != null) {
                TransactionDetailScreen(
                    transaction = transaction,
                    viewModel = viewModel,
                    navController = navController
                )
            } else {
                Text("No transaction selected")
            }
        }
        composable("analytics") {
            AnalyticsScreen(
                viewModel = viewModel,
                navController = navController
            )
        }
    }
}