package com.example.expensetracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.expensetracker.ui.screens.ExpenseDashboard
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {

    private var transactionsToExport: List<Transaction> = emptyList()

    private val exportCsvLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            val success = contentResolver.openOutputStream(it)?.use { os ->
                CsvExporter.exportTransactions(transactionsToExport, os)
            } ?: false
            if (success) {
                Toast.makeText(this, "CSV exported successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to export CSV", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this).get(ExpenseViewModel::class.java)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            ExpenseTrackerTheme(darkTheme = darkTheme) {
                ExpenseDashboard(
                    viewModel = viewModel,
                    onExportCsv = { txs ->
                        transactionsToExport = txs
                        exportCsvLauncher.launch("expenses_export.csv")
                    }
                )
            }
        }
    }
}