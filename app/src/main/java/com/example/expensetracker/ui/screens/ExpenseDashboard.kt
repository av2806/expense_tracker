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
import androidx.navigation.NavController
import androidx.compose.foundation.clickable
import androidx.compose.material3.ModalBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDashboard(viewModel: ExpenseViewModel,
                     navController: NavController) {

    val currentMonthYear =
        SimpleDateFormat("MMMM, yyyy", Locale.getDefault()).format(Date())
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val totalExpenses by viewModel.totalExpenses.collectAsState(initial = 0)
    var showDialog by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf("Newest") }
    var showSortSheet by remember { mutableStateOf(false) }
    val sortedTransactions = when (sortOption) {
        "Newest" -> transactions.sortedByDescending { it.timestamp }
        "Oldest" -> transactions.sortedBy { it.timestamp }

        "Amount ↓" -> transactions.sortedByDescending { it.amount }
        "Amount ↑" -> transactions.sortedBy { it.amount }

        "Title A-Z" -> transactions.sortedBy { it.title.lowercase() }
        "Title Z-A" -> transactions.sortedByDescending { it.title.lowercase() }

        "Category A-Z" -> transactions.sortedBy { it.category.lowercase() }
        "Category Z-A" -> transactions.sortedByDescending { it.category.lowercase() }

        "Payment A-Z" -> transactions.sortedBy { it.paymentMethod.lowercase() }
        "Payment Z-A" -> transactions.sortedByDescending { it.paymentMethod.lowercase() }

        else -> transactions
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFF1E3A8A),
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050816))
                .padding(paddingValues)
                .padding(16.dp)
        ){
            Text(
                text = "Hello, Adithi 👋",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Track your spending",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E3A8A),
                                    Color(0xFF312E81)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Total Expense",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "₹${totalExpenses ?: 0}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "$currentMonthYear",
                        color = Color(0xFF93C5FD),
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate("analytics")
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111827)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Spending Insights",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "View analytics and trends →",
                        color = Color(0xFF60A5FA),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            //Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Box {
                    OutlinedButton(
                        onClick = { showSortSheet = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF111827),
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFF3B82F6)
                        )
                    ) {
                        Text(
                            text = "⇅ Sort",
                            fontSize = 13.sp
                        )
                    }


                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (sortedTransactions.isEmpty()){
                Text("No transactions yet", color = Color.Gray)
            } else {
                LazyColumn {
                    items(sortedTransactions) { transaction ->
                        TransactionCard(transaction = transaction,
                            onClick = {
                                viewModel.selectTransaction(transaction)
                                navController.navigate("transaction_detail")
                            },
                            onDelete = {
                                viewModel.deleteTransaction(transaction)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddDialog(
            onDismiss = { showDialog = false },
            onAdd = { title, amount, category, paymentMethod ->
                viewModel.addTransaction(title, amount, category, paymentMethod)
                showDialog = false
            }
        )
    }
    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            containerColor = Color(0xFF111827)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Sort By",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                val options = listOf(
                    "Newest",
                    "Oldest",
                    "Amount ↓",
                    "Amount ↑",
                    "Title A-Z",
                    "Title Z-A",
                    "Category A-Z",
                    "Category Z-A",
                    "Payment A-Z",
                    "Payment Z-A"
                )

                options.forEach { option ->
                    TextButton(
                        onClick = {
                            sortOption = option
                            showSortSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = option,
                            color = if (sortOption == option)
                                Color(0xFF60A5FA)
                            else
                                Color.White,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

}

@Composable
fun TransactionCard(
    transaction: Transaction,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF111827)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${transaction.category} • ${transaction.paymentMethod}",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatDate(transaction.timestamp),
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Text(
                text = "₹${transaction.amount}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF60A5FA)
            )
        }
    }
}
@Composable
fun AddDialog(onDismiss: () -> Unit, onAdd: (String, Int, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Shopping") }
    var paymentMethod by remember { mutableStateOf("UPI") }

    val categories = listOf("Food", "Transport", "Shopping", "Entertainment", "Bills", "Other")
    val paymentMethods = listOf("Cash", "Card", "UPI", "Bank Transfer")
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedPayment by remember { mutableStateOf(false) }

    AlertDialog(
        containerColor = Color(0xFF111827),
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add Transaction",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text("Title", color = Color(0xFF94A3B8))
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1F2937),
                        unfocusedContainerColor = Color(0xFF1F2937),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color(0xFF60A5FA),
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = {
                        Text("Amount", color = Color(0xFF94A3B8))
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1F2937),
                        unfocusedContainerColor = Color(0xFF1F2937),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color(0xFF60A5FA),
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Category dropdown
                Box {
                    OutlinedButton(
                        onClick = { expandedCategory = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1F2937),
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFF374151)
                        )
                    ) {
                        Text(
                            text = category,
                            color = Color.White
                        )
                    }
                    DropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }, modifier = Modifier.background(Color(0xFF111827))) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = cat,
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    category = cat
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Payment Method dropdown
                Box {
                    OutlinedButton(
                        onClick = { expandedPayment = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1F2937),
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFF374151)
                        )
                    ) {
                        Text(
                            text = paymentMethod,
                            color = Color.White
                        )
                    }
                    DropdownMenu(expanded = expandedPayment, onDismissRequest = { expandedPayment = false }, modifier = Modifier.background(Color(0xFF111827))) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = method,
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    paymentMethod = method
                                    expandedPayment = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                onClick = {
                    if (title.isNotBlank() && amount.isNotBlank()) {
                        try {
                            val amountInt = amount.toInt()
                            if (amountInt > 0) {
                                onAdd(title, amountInt, category, paymentMethod)
                            }
                        } catch (e: NumberFormatException) {
                            // Invalid number, do nothing
                        }
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = Color(0xFF94A3B8)
                )
            }
        }
    )
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}