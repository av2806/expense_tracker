package com.example.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ExpenseViewModel
import com.example.expensetracker.Transaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpenseDashboard(viewModel: ExpenseViewModel) {

    val transactions by viewModel.transactions.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Expense Tracker", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            // Balance Card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .background(Color(0xFF6750A4))
                        .padding(24.dp)
                ) {
                    Text("Total Expenses", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("₹$totalExpenses", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Recent Transactions", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))

            if (transactions.isEmpty()) {
                Text("No transactions yet", color = Color.Gray)
            } else {
                LazyColumn {
                    items(transactions) { transaction ->
                        TransactionCard(transaction) { viewModel.deleteTransaction(transaction) }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddDialog(
            onDismiss = { showDialog = false },
            onAdd = { title, amount, category ->
                viewModel.addTransaction(title, amount, category)
                showDialog = false
            }
        )
    }
}

@Composable
fun TransactionCard(transaction: Transaction, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(transaction.category, color = Color.Gray, fontSize = 12.sp)
                Text(formatDate(transaction.timestamp), color = Color.Gray, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${transaction.amount}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                TextButton(onClick = onDelete) {
                    Text("Delete", fontSize = 10.sp, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun AddDialog(onDismiss: () -> Unit, onAdd: (String, Int, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Shopping") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column {
                TextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank() && amount.isNotBlank()) {
                    onAdd(title, amount.toInt(), category)
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}