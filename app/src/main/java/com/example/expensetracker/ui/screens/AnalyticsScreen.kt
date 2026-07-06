package com.example.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensetracker.ExpenseViewModel
import com.example.expensetracker.ui.components.DailyBarChart
import com.example.expensetracker.ui.components.MonthlyBarChart
import com.example.expensetracker.ui.components.PieChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: ExpenseViewModel,
    navController: NavController
) {
    val categoryData by viewModel.categorySpending.collectAsState(initial = emptyList())
    val dailyData by viewModel.dailySpending.collectAsState(initial = emptyList())
    val monthlyData by viewModel.monthlySpending.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = Color(0xFF050816),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050816)
                ),
                title = {
                    Text(
                        "Analytics",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Text(
                            "← Back",
                            color = Color(0xFF60A5FA)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFF050816))
                .padding(16.dp)
        ) {
            Text(
                text = "Spending Analysis",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CATEGORY CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111827)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Category Breakdown",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryData.isEmpty()) {
                        Text(
                            "No transactions yet",
                            color = Color.Gray
                        )
                    } else {
                        PieChart(data = categoryData)

                        Spacer(modifier = Modifier.height(16.dp))

                        categoryData.forEach { item ->
                            Text(
                                "${item.category}: ₹${item.totalAmount}",
                                color = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // DAILY CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111827)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Daily Spending",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (dailyData.isEmpty()) {
                        Text(
                            "No daily spending data",
                            color = Color.Gray
                        )
                    } else {
                        DailyBarChart(data = dailyData)

                        Spacer(modifier = Modifier.height(16.dp))

                        dailyData.forEach { item ->
                            Text(
                                "${item.day}: ₹${item.totalAmount}",
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // MONTHLY CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111827)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Monthly Spending",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (monthlyData.isEmpty()) {
                        Text(
                            "No monthly spending data",
                            color = Color.Gray
                        )
                    } else {
                        MonthlyBarChart(data = monthlyData)

                        Spacer(modifier = Modifier.height(16.dp))

                        monthlyData.forEach { item ->
                            Text(
                                "${item.month}: ₹${item.totalAmount}",
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}