package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ExpenseTrackerTheme {
                ExpenseDashboard()
            }
        }
    }
}

data class Transaction(
    val title: String,
    val amount: Int,
    val category: String
)

@Composable
fun ExpenseDashboard() {

    val transactions = listOf(
        Transaction("Swiggy", 250, "Food"),
        Transaction("Uber", 120, "Transport"),
        Transaction("Amazon", 999, "Shopping"),
        Transaction("Zomato", 340, "Food"),
        Transaction("Metro", 60, "Transport")
    )

    Scaffold(

        floatingActionButton = {

            FloatingActionButton(
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add"
                )
            }
        }

    ) { paddingValues ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)

        ) {

            Text(
                text = "Expense Tracker",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            BalanceCard()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Recent Transactions",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn {

                items(transactions) { transaction ->

                    TransactionCard(transaction)

                }
            }
        }
    }
}

@Composable
fun BalanceCard() {

    Card(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(24.dp)

    ) {

        Column(

            modifier = Modifier
                .background(Color(0xFF6750A4))
                .padding(24.dp)

        ) {

            Text(
                text = "Current Balance",
                color = Color.White,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "₹12,000",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {

    val icon = when (transaction.category) {
        "Food" -> Icons.Default.Home
        "Transport" -> Icons.Default.Face
        "Shopping" -> Icons.Default.Email
        else -> Icons.Default.Star
    }

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),

        shape = RoundedCornerShape(18.dp)

    ) {

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {

                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Text(
                    text = transaction.category,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "₹${transaction.amount}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}