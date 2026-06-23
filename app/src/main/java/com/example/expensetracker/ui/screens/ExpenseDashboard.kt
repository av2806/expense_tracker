package com.example.expensetracker.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.example.expensetracker.Category
import com.example.expensetracker.ExpenseViewModel
import com.example.expensetracker.Transaction
import com.example.expensetracker.LogManager
import com.example.expensetracker.receiver.NotificationListener
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDashboard(
    viewModel: ExpenseViewModel,
    onExportCsv: (List<Transaction>) -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val themeMode by viewModel.themeMode.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val geminiModel by viewModel.geminiModel.collectAsState()

    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showApiDialog by remember { mutableStateOf(false) }
    var isNotificationAccessGranted by remember { mutableStateOf(false) }
    var isServiceConnected by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }

    var nlCommandText by remember { mutableStateOf("") }
    var nlResultText by remember { mutableStateOf("") }
    var isNlProcessing by remember { mutableStateOf(false) }

    var selectedInterval by remember { mutableStateOf("Month") } 
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSourceFilter by remember { mutableStateOf("All Sources") }
    var selectedSortOption by remember { mutableStateOf("Date (Newest)") }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    
    LaunchedEffect(Unit) {
        while (true) {
            val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
            isNotificationAccessGranted = packageNames.contains(context.packageName)
            isServiceConnected = com.example.expensetracker.receiver.NotificationListener.isServiceConnected
            kotlinx.coroutines.delay(2000)
        }
    }

    LaunchedEffect(isNotificationAccessGranted) {
        if (isNotificationAccessGranted) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    val componentName = android.content.ComponentName(context, com.example.expensetracker.receiver.NotificationListener::class.java)
                    android.service.notification.NotificationListenerService.requestRebind(componentName)
                    com.example.expensetracker.LogManager.log("INFO", "Requested Notification Listener Service rebind.")
                }
            } catch (e: Exception) {
                com.example.expensetracker.LogManager.log("ERROR", "Failed to rebind notification service: ${e.message}")
            }
        }
    }

    
    val filteredTransactions = remember(transactions, selectedInterval) {
        val now = System.currentTimeMillis()
        val intervalMs = when (selectedInterval) {
            "Week" -> 7 * 24 * 60 * 60 * 1000L
            "Month" -> 30 * 24 * 60 * 60 * 1000L
            "Year" -> 365 * 24 * 60 * 60 * 1000L
            else -> Long.MAX_VALUE
        }
        if (selectedInterval == "All") {
            transactions
        } else {
            transactions.filter { now - it.timestamp <= intervalMs }
        }
    }

    val filteredTx = remember(transactions, selectedInterval, searchQuery, selectedSourceFilter, selectedSortOption) {
        val now = System.currentTimeMillis()
        val intervalMs = when (selectedInterval) {
            "Week" -> 7 * 24 * 60 * 60 * 1000L
            "Month" -> 30 * 24 * 60 * 60 * 1000L
            "Year" -> 365 * 24 * 60 * 60 * 1000L
            else -> Long.MAX_VALUE
        }
        val baseList = if (selectedInterval == "All") {
            transactions
        } else {
            transactions.filter { now - it.timestamp <= intervalMs }
        }
        val searchedList = if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.bankName.contains(searchQuery, ignoreCase = true)
            }
        }

        val sourceFilteredList = when (selectedSourceFilter) {
            "All Sources" -> searchedList
            "Cash" -> searchedList.filter { it.paymentMethod.equals("Cash", ignoreCase = true) }
            else -> searchedList.filter { it.bankName.equals(selectedSourceFilter, ignoreCase = true) }
        }

        when (selectedSortOption) {
            "Date (Newest)" -> sourceFilteredList.sortedByDescending { it.timestamp }
            "Date (Oldest)" -> sourceFilteredList.sortedBy { it.timestamp }
            "Amount (Highest)" -> sourceFilteredList.sortedByDescending { it.amount }
            "Amount (Lowest)" -> sourceFilteredList.sortedBy { it.amount }
            else -> sourceFilteredList
        }
    }

    val bankList = remember(transactions) {
        val banks = transactions.map { it.bankName }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("All Sources", "Cash") + banks
    }

    
    val totalFilteredExpenses = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.amount }
    }

    
    val categoryTotals = remember(filteredTransactions) {
        val map = mutableMapOf<String, Int>()
        filteredTransactions.forEach { tx ->
            map[tx.category] = map.getOrDefault(tx.category, 0) + tx.amount
        }
        map.toList().sortedByDescending { it.second }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Expense Tracker",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))
                
                
                NavigationDrawerItem(
                    icon = { FontAwesomeIcon("\uf013", fontSize = 18.sp) },
                    label = { Text("Gemini AI Settings", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showApiDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))
                
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text("Privacy-First Tracker", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Data is stored locally on Room DB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Dashboard,
                        onClick = { currentScreen = Screen.Dashboard },
                        icon = { FontAwesomeIcon("\uf015", fontSize = 20.sp) },
                        label = { Text("Overview") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Transactions,
                        onClick = { currentScreen = Screen.Transactions },
                        icon = { FontAwesomeIcon("\uf0ca", fontSize = 20.sp) },
                        label = { Text("Transactions") }
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Analytics,
                        onClick = { currentScreen = Screen.Analytics },
                        icon = { FontAwesomeIcon("\uf200", fontSize = 20.sp) },
                        label = { Text("Analytics") }
                    )
                }
            },
            floatingActionButton = {
                if (currentScreen == Screen.Dashboard) {
                    FloatingActionButton(
                        onClick = { showAiSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        FontAwesomeIcon("\uf544", fontSize = 24.sp)
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } }
                    ) {
                        FontAwesomeIcon(
                            "\uf0c9", 
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Expense Tracker", 
                        fontSize = 22.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    
                    
                    IconButton(
                        onClick = {
                            val nextTheme = when (themeMode) {
                                "system" -> "light"
                                "light" -> "dark"
                                else -> "system"
                            }
                            viewModel.setThemeMode(nextTheme)
                        }
                    ) {
                        Text(
                            text = when (themeMode) {
                                "light" -> "☀️"
                                "dark" -> "🌙"
                                else -> "🌓"
                            },
                            fontSize = 20.sp
                        )
                    }
                }

                when (currentScreen) {
                    Screen.Dashboard -> {
                        if (!isNotificationAccessGranted) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable {
                                        try {
                                            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FontAwesomeIcon("\uf071", fontSize = 24.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Notification Access Disabled",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            "Tap here to enable it in Settings so we can auto-read SMS alerts.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        } else if (!isServiceConnected) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable {
                                        try {
                                            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FontAwesomeIcon("\uf1e6", fontSize = 24.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Notification Service Offline",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            "Android has suspended the listener service (common after updates). Tap here, then toggle the permission Off & On to restart it.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        if (transactions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                WelcomeGuidelines(isDark = isDark)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                
                                val cardBrush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                                Card(
                                    modifier = Modifier.fillMaxWidth(), 
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .background(cardBrush)
                                            .padding(20.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Text("Total Expenses", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("₹${transactions.sumOf { it.amount }}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                
                                Text("Overview", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .premiumContainer(isDark = isDark, shape = RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        val totalAmt = transactions.sumOf { it.amount }
                                        val dashboardCategoryTotals = remember(transactions) {
                                            val map = mutableMapOf<String, Int>()
                                            transactions.forEach { tx ->
                                                map[tx.category] = map.getOrDefault(tx.category, 0) + tx.amount
                                            }
                                            map.toList().sortedByDescending { it.second }
                                        }
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(12.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                        ) {
                                            dashboardCategoryTotals.forEach { (catName, amount) ->
                                                val weight = amount.toFloat() / totalAmt
                                                val catColor = categories.find { it.name == catName }?.color ?: "#9E9E9E"
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .weight(if (weight > 0f) weight else 0.0001f)
                                                        .background(parseColor(catColor))
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Recent Transactions", 
                                        fontSize = 16.sp, 
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    TextButton(onClick = { currentScreen = Screen.Transactions }) {
                                        Text("View All", fontSize = 13.sp)
                                    }
                                }
                                
                                LazyColumn(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(transactions.take(5), key = { it.id }) { transaction ->
                                        TransactionCard(
                                            transaction = transaction,
                                            categories = categories,
                                            isDark = isDark,
                                            onEdit = {
                                                editingTransaction = transaction
                                                showEditDialog = true
                                            },
                                            onDelete = {
                                                viewModel.deleteTransaction(transaction)
                                                scope.launch {
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = "Deleted transaction of ₹${transaction.amount}",
                                                        actionLabel = "Undo",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        viewModel.addTransaction(
                                                            title = transaction.title,
                                                            amount = transaction.amount,
                                                            category = transaction.category,
                                                            paymentMethod = transaction.paymentMethod
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Screen.Transactions -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search transactions...", fontSize = 13.sp) },
                                    leadingIcon = { FontAwesomeIcon("\uf002", fontSize = 14.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { onExportCsv(filteredTx) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    FontAwesomeIcon(
                                        iconCode = "\uf56e", // file-export icon
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val intervals = listOf("Week", "Month", "Year", "All")
                                intervals.forEach { interval ->
                                    val isSelected = selectedInterval == interval
                                    OutlinedButton(
                                        onClick = { selectedInterval = interval },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
                                        ),
                                        border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder,
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) {
                                        Text(interval, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Source Dropdown
                                var expandedSourceMenu by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { expandedSourceMenu = true },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.height(36.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                                    ) {
                                        FontAwesomeIcon("\uf19c", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(selectedSourceFilter, fontSize = 12.sp)
                                    }
                                    DropdownMenu(
                                        expanded = expandedSourceMenu,
                                        onDismissRequest = { expandedSourceMenu = false }
                                    ) {
                                        bankList.forEach { sourceName ->
                                            DropdownMenuItem(
                                                text = { Text(sourceName) },
                                                onClick = {
                                                    selectedSourceFilter = sourceName
                                                    expandedSourceMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Sort Dropdown
                                var expandedSortMenu by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { expandedSortMenu = true },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.height(36.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                                    ) {
                                        FontAwesomeIcon("\uf0dc", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(selectedSortOption, fontSize = 12.sp)
                                    }
                                    DropdownMenu(
                                        expanded = expandedSortMenu,
                                        onDismissRequest = { expandedSortMenu = false }
                                    ) {
                                        val sortOptions = listOf("Date (Newest)", "Date (Oldest)", "Amount (Highest)", "Amount (Lowest)")
                                        sortOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    selectedSortOption = option
                                                    expandedSortMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                if (filteredTx.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(40.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No matching transactions found", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                        }
                                    }
                                } else {
                                    items(filteredTx, key = { it.id }) { transaction ->
                                        TransactionCard(
                                            transaction = transaction,
                                            categories = categories,
                                            isDark = isDark,
                                            onEdit = {
                                                editingTransaction = transaction
                                                showEditDialog = true
                                            },
                                            onDelete = {
                                                viewModel.deleteTransaction(transaction)
                                                scope.launch {
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = "Deleted transaction of ₹${transaction.amount}",
                                                        actionLabel = "Undo",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        viewModel.addTransaction(
                                                            title = transaction.title,
                                                            amount = transaction.amount,
                                                            category = transaction.category,
                                                            paymentMethod = transaction.paymentMethod
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Screen.Analytics -> {
                        val analyticsCategoryTotals = remember(transactions, selectedInterval) {
                            val now = System.currentTimeMillis()
                            val intervalMs = when (selectedInterval) {
                                "Week" -> 7 * 24 * 60 * 60 * 1000L
                                "Month" -> 30 * 24 * 60 * 60 * 1000L
                                "Year" -> 365 * 24 * 60 * 60 * 1000L
                                else -> Long.MAX_VALUE
                            }
                            val baseList = if (selectedInterval == "All") {
                                transactions
                            } else {
                                transactions.filter { now - it.timestamp <= intervalMs }
                            }
                            val map = mutableMapOf<String, Int>()
                            baseList.forEach { tx ->
                                map[tx.category] = map.getOrDefault(tx.category, 0) + tx.amount
                            }
                            map.toList().sortedByDescending { it.second }
                        }
                        
                        val totalAnalyticsSpend = analyticsCategoryTotals.sumOf { it.second }

                        Column(modifier = Modifier.fillMaxSize()) {
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val intervals = listOf("Week", "Month", "Year", "All")
                                intervals.forEach { interval ->
                                    val isSelected = selectedInterval == interval
                                    OutlinedButton(
                                        onClick = { selectedInterval = interval },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
                                        ),
                                        border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder,
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) {
                                        Text(interval, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }

                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (totalAnalyticsSpend == 0) {
                                    Canvas(modifier = Modifier.size(160.dp)) {
                                        drawArc(
                                            color = Color.LightGray.copy(alpha = 0.5f),
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 32.dp.toPx(), cap = StrokeCap.Butt)
                                        )
                                    }
                                    Text("₹0", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                                } else {
                                    Canvas(modifier = Modifier.size(160.dp)) {
                                        var startAngle = -90f
                                        analyticsCategoryTotals.forEach { (catName, amount) ->
                                            val sweepAngle = (amount.toFloat() / totalAnalyticsSpend) * 360f
                                            val catColor = categories.find { it.name == catName }?.color ?: "#9E9E9E"
                                            drawArc(
                                                color = parseColor(catColor),
                                                startAngle = startAngle,
                                                sweepAngle = sweepAngle,
                                                useCenter = false,
                                                style = Stroke(width = 32.dp.toPx(), cap = StrokeCap.Butt)
                                            )
                                            startAngle += sweepAngle
                                        }
                                    }
                                    Text("₹$totalAnalyticsSpend", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }

                            
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(analyticsCategoryTotals) { (catName, amount) ->
                                    val pct = if (totalAnalyticsSpend > 0) (amount.toFloat() / totalAnalyticsSpend * 100).toInt() else 0
                                    val catColor = categories.find { it.name == catName }?.color ?: "#9E9E9E"
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .premiumContainer(isDark = isDark, shape = RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(parseColor(catColor))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(catName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text("₹$amount ($pct%)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                
                                if (totalAnalyticsSpend > 0) {
                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Payment Methods", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    
                                    val paymentMethods = listOf("Cash", "Card", "UPI", "Bank Transfer")
                                    items(paymentMethods) { method ->
                                        val now = System.currentTimeMillis()
                                        val intervalMs = when (selectedInterval) {
                                            "Week" -> 7 * 24 * 60 * 60 * 1000L
                                            "Month" -> 30 * 24 * 60 * 60 * 1000L
                                            "Year" -> 365 * 24 * 60 * 60 * 1000L
                                            else -> Long.MAX_VALUE
                                        }
                                        val baseList = if (selectedInterval == "All") {
                                            transactions
                                        } else {
                                            transactions.filter { now - it.timestamp <= intervalMs }
                                        }
                                        val amount = baseList.filter { it.paymentMethod == method }.sumOf { it.amount }
                                        val pct = if (totalAnalyticsSpend > 0) (amount.toFloat() / totalAnalyticsSpend * 100).toInt() else 0
                                        
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .premiumContainer(isDark = isDark, shape = RoundedCornerShape(12.dp)),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = when (method) {
                                                        "Cash" -> "💵"
                                                        "Card" -> "💳"
                                                        "UPI" -> "📱"
                                                        else -> "🏦"
                                                    },
                                                    fontSize = 16.sp
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(method, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text("₹$amount ($pct%)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }

    if (showAiSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showAiSheet = false
                aiErrorMessage = null
            },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    "AI Command Assistant",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Type a command in natural language to manage transactions (e.g., 'add coffee 10 rupee').",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (aiErrorMessage != null) {
                    Text(
                        text = aiErrorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                OutlinedTextField(
                    value = nlCommandText,
                    onValueChange = { nlCommandText = it },
                    placeholder = { Text("Add coffee 10 rupee...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    enabled = !isNlProcessing
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            showAiSheet = false
                            aiErrorMessage = null
                            showAddDialog = true
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FontAwesomeIcon("\uf303", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manual Entry", fontSize = 12.sp)
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (nlCommandText.isNotBlank()) {
                                isNlProcessing = true
                                aiErrorMessage = null
                                viewModel.processNaturalLanguageCommand(
                                    commandText = nlCommandText,
                                    currentTransactions = transactions,
                                    currentCategories = categories,
                                    onResult = { result ->
                                        isNlProcessing = false
                                        if (result.startsWith("Error", ignoreCase = true) || result.startsWith("Failed", ignoreCase = true)) {
                                            aiErrorMessage = result
                                        } else {
                                            aiErrorMessage = null
                                            nlCommandText = ""
                                            showAiSheet = false
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !isNlProcessing && nlCommandText.isNotBlank(),
                        modifier = Modifier.height(40.dp)
                    ) {
                        if (isNlProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Run Command ⚡", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDialog(
            categories = categories,
            onDismiss = { showAddDialog = false },
            onAdd = { title, amount, category, paymentMethod, bankName, bankLast4 ->
                viewModel.addTransaction(title, amount, category, paymentMethod, bankName, bankLast4)
                showAddDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(
                         message = "Transaction added successfully",
                         duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }

    if (showEditDialog && editingTransaction != null) {
        EditDialog(
            transaction = editingTransaction!!,
            categories = categories,
            onDismiss = { 
                showEditDialog = false
                editingTransaction = null
            },
            onConfirm = { updatedTx ->
                viewModel.updateTransaction(updatedTx)
                showEditDialog = false
                editingTransaction = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Transaction updated successfully",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }

    if (showApiDialog) {
        var keyText by remember { mutableStateOf(geminiApiKey) }
        var selectedModel by remember { mutableStateOf(geminiModel) }
        var expandedModelDropdown by remember { mutableStateOf(false) }
        var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }
        var isFetchingModels by remember { mutableStateOf(false) }

        LaunchedEffect(keyText) {
            if (keyText.isNotBlank()) {
                isFetchingModels = true
                viewModel.fetchAvailableModels(keyText) { list ->
                    availableModels = list
                    isFetchingModels = false
                    if (list.isNotEmpty() && !list.contains(selectedModel)) {
                        selectedModel = if (list.contains("gemini-1.5-flash")) "gemini-1.5-flash" else list.first()
                    }
                }
            } else {
                availableModels = emptyList()
            }
        }

        AlertDialog(
            onDismissRequest = { showApiDialog = false },
            title = { Text("Gemini AI Settings") },
            text = {
                Column {
                    Text("Configure your Google AI Studio API key and parsing model for automatic receipt logs and command management.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = keyText,
                        onValueChange = { keyText = it },
                        label = { Text("Gemini API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Select Model", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedButton(
                            onClick = { expandedModelDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isFetchingModels && availableModels.isNotEmpty()
                        ) {
                            Text(
                                if (isFetchingModels) "Fetching models..." 
                                else if (keyText.isBlank()) "Enter API Key to load models"
                                else if (availableModels.isEmpty()) "Failed to load models"
                                else selectedModel
                            )
                        }
                        if (availableModels.isNotEmpty()) {
                            DropdownMenu(
                                expanded = expandedModelDropdown,
                                onDismissRequest = { expandedModelDropdown = false }
                            ) {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        onClick = {
                                            selectedModel = model
                                            expandedModelDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = keyText.isNotBlank(),
                    onClick = {
                        viewModel.setGeminiApiKey(keyText)
                        viewModel.setGeminiModel(selectedModel)
                        showApiDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TransactionCard(
    transaction: Transaction,
    categories: List<Category>,
    isDark: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val catColor = categories.find { it.name == transaction.category }?.color ?: "#9E9E9E"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .premiumContainer(isDark = isDark, shape = RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(parseColor(catColor))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Column {
                        Text(
                            transaction.title, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val bankInfo = if (transaction.bankName.isNotEmpty()) {
                            val suffix = if (transaction.bankLast4.isNotEmpty()) " *${transaction.bankLast4}" else ""
                            " (${transaction.bankName}$suffix)"
                        } else {
                            ""
                        }
                        Text(
                            "${transaction.category} • ${transaction.paymentMethod}$bankInfo • ${transaction.source.uppercase()}", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                            fontSize = 12.sp
                        )
                        Text(
                            formatDate(transaction.timestamp), 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                            fontSize = 10.sp
                        )
                    }
                }
                
                Text(
                    "₹${transaction.amount}", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onEdit() }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FontAwesomeIcon("\uf044", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = { onDelete() }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FontAwesomeIcon("\uf2ed", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeGuidelines(isDark: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .premiumContainer(isDark = isDark, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Welcome to Expense Tracker! 💸",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Get started with these simple steps:",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            GuidelineItem("1️⃣", "Set your Gemini API Key in the Drawer Settings.")
            GuidelineItem("2️⃣", "Grant Notification Access in Settings. (If greyed out on Android 13+, open System App Info, click the 3-dots icon at top-right, choose 'Allow restricted settings', then grant access).")
            GuidelineItem("3️⃣", "Use the AI Assistant bar below to quickly manage expenses (e.g. 'add coffee 10 rupee' or 'delete transaction 1').")
            GuidelineItem("4️⃣", "Simulate bank alerts from the Drawer menu to test the local parsing engine.")
        }
    }
}

@Composable
fun GuidelineItem(num: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(num, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun AddDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAdd: (String, Int, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember(categories) { 
        mutableStateOf(categories.firstOrNull()?.name ?: "Food") 
    }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var bankName by remember { mutableStateOf("") }
    var bankLast4 by remember { mutableStateOf("") }

    val paymentMethods = listOf("Cash", "Card", "UPI", "Bank Transfer")
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedPayment by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                TextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                
                Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    OutlinedButton(onClick = { expandedCategory = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(category)
                    }
                    DropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    category = cat.name
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                
                Text("Payment Method", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    OutlinedButton(onClick = { expandedPayment = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(paymentMethod)
                    }
                    DropdownMenu(expanded = expandedPayment, onDismissRequest = { expandedPayment = false }) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    paymentMethod = method
                                    expandedPayment = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                TextField(value = bankName, onValueChange = { bankName = it }, label = { Text("Bank Name (Optional)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = bankLast4,
                    onValueChange = { bankLast4 = it },
                    label = { Text("Bank Last 4 Digits (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && amount.isNotBlank()) {
                        val amt = amount.toIntOrNull() ?: 0
                        onAdd(title, amt, category, paymentMethod, bankName, bankLast4)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditDialog(
    transaction: Transaction,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit
) {
    var title by remember { mutableStateOf(transaction.title) }
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var category by remember(categories) { mutableStateOf(transaction.category) }
    var paymentMethod by remember { mutableStateOf(transaction.paymentMethod) }
    var bankName by remember { mutableStateOf(transaction.bankName) }
    var bankLast4 by remember { mutableStateOf(transaction.bankLast4) }

    val paymentMethods = listOf("Cash", "Card", "UPI", "Bank Transfer")
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedPayment by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                TextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                
                Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    OutlinedButton(onClick = { expandedCategory = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(category)
                    }
                    DropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    category = cat.name
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                
                Text("Payment Method", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    OutlinedButton(onClick = { expandedPayment = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(paymentMethod)
                    }
                    DropdownMenu(expanded = expandedPayment, onDismissRequest = { expandedPayment = false }) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    paymentMethod = method
                                    expandedPayment = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                TextField(value = bankName, onValueChange = { bankName = it }, label = { Text("Bank Name (Optional)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = bankLast4,
                    onValueChange = { bankLast4 = it },
                    label = { Text("Bank Last 4 Digits (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && amount.isNotBlank()) {
                        val amt = amount.toIntOrNull() ?: transaction.amount
                        onConfirm(
                            transaction.copy(
                                title = title,
                                amount = amt,
                                category = category,
                                paymentMethod = paymentMethod,
                                bankName = bankName,
                                bankLast4 = bankLast4
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun parseColor(colorString: String): Color {
    return try {
        Color(AndroidColor.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFF9E9E9E)
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

enum class Screen {
    Dashboard, Transactions, Analytics
}

@Composable
fun FontAwesomeIcon(
    iconCode: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp
) {
    Text(
        text = iconCode,
        fontFamily = FontFamily(Font(com.example.expensetracker.R.font.fa_solid_900)),
        fontSize = fontSize,
        color = color,
        modifier = modifier
    )
}

fun Modifier.premiumContainer(isDark: Boolean, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)): Modifier {
    return if (isDark) {
        this.border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF00F0FF), Color(0xFFFF007F))
            ),
            shape = shape
        )
    } else {
        this.shadow(
            elevation = 8.dp,
            shape = shape,
            clip = false,
            ambientColor = Color(0xFFFF4081).copy(alpha = 0.2f),
            spotColor = Color(0xFFFF4081).copy(alpha = 0.35f)
        )
    }
}