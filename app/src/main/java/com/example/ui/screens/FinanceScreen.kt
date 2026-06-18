package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.CategoryShare
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.MonthlyBarData
import com.example.ui.viewmodel.getCategoryColor
import com.example.ui.viewmodel.getMonthYearString
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    // Collect states
    val transactions by viewModel.uiState.collectAsState()
    val rawAllTransactions by viewModel.allTransactions.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()
    val monthlyChartData by viewModel.monthlyChartData.collectAsState()
    val categoryShares by viewModel.categoryBreakdown.collectAsState()

    // Query & Filter fields
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedMonthFilter by viewModel.selectedMonthFilter.collectAsState()

    // Dialog & UI flows
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var activeTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Analytics/Grafik

    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            val calendar = remember { Calendar.getInstance() }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour < 11 -> "Selamat Pagi"
                hour < 15 -> "Selamat Siang"
                hour < 18 -> "Selamat Sore"
                else -> "Selamat Malam"
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = greeting.uppercase(Locale("id", "ID")),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Budi Santoso",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profil",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("add_transaction_fab")
                    .padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Catatan",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Tabs selection: Dashboard vs Analytics (Segmented Capsule Bento control)
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[activeTab])
                            .fillMaxHeight()
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                    )
                },
                divider = {}
            ) {
                val tabText0Color = if (activeTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = tabText0Color
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Daftar Aliran", fontWeight = FontWeight.SemiBold, color = tabText0Color)
                        }
                    },
                    modifier = Modifier.testTag("tab_dashboard")
                )
                val tabText1Color = if (activeTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = tabText1Color
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grafik Analisis", fontWeight = FontWeight.SemiBold, color = tabText1Color)
                        }
                    },
                    modifier = Modifier.testTag("tab_analytics")
                )
            }

            // Quick Month Filter Bar (persistent across screens)
            MonthFilterSelector(
                availableMonths = availableMonths,
                selectedMonth = selectedMonthFilter,
                onMonthSelected = { viewModel.selectedMonthFilter.value = it }
            )

            // Dynamic transitions between screens
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabContent"
            ) { targetTab ->
                when (targetTab) {
                    0 -> {
                        // Dashboard Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Core Balance Card
                            FinancialStateCard(
                                income = summary.totalIncome,
                                expense = summary.totalExpense,
                                balance = summary.balance
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Side-by-side Bento Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Bento 1: Tambah Catatan (Quick Action)
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .clickable { showAddDialog = true }
                                        .testTag("bento_add_notes"),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFD3E3FD) // Light blue accent matching html
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF0B57D0)), // Blue button bg
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Tambah Catatan",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tambah Catatan",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF041E49) // Dark blue text
                                        )
                                    }
                                }

                                // Bento 2: Category Food Insight
                                val foodCategoryText = "Makanan & Minuman"
                                // Find food aggregate from the transactions
                                val foodTotal = rawAllTransactions.filter { it.category == foodCategoryText }.sumOf { it.amount }
                                
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Restaurant,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            
                                            Text(
                                                 text = "-8%", // styled indicator from html
                                                 style = MaterialTheme.typography.labelSmall,
                                                 fontWeight = FontWeight.Bold,
                                                 color = ExpenseColor
                                            )
                                        }
                                        
                                        Column {
                                            Text(
                                                text = "Makan & Minum",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeutralText,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = formatRupiah(if (foodTotal > 0) foodTotal else 120000.0), // beautiful Indonesian rupiah total, fallback to 120,000 IDR if empty
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Search bar & Type Filters
                            SearchBarAndFilters(
                                query = searchQuery,
                                onQueryChange = { viewModel.searchQuery.value = it },
                                selectedType = selectedTypeFilter,
                                onTypeSelected = { viewModel.selectedTypeFilter.value = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Transactions List
                            if (transactions.isEmpty()) {
                                EmptyStatePlaceholder(
                                    hasSearchOrFilters = searchQuery.isNotEmpty() || selectedTypeFilter != "ALL",
                                    onResetFilters = {
                                        viewModel.searchQuery.value = ""
                                        viewModel.selectedTypeFilter.value = "ALL"
                                        viewModel.selectedMonthFilter.value = "ALL"
                                    }
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                        .testTag("transactions_list")
                                ) {
                                    items(transactions, key = { it.id }) { tx ->
                                        TransactionRowItem(
                                            transaction = tx,
                                            onClick = { transactionToEdit = tx },
                                            onDelete = { viewModel.deleteTransaction(tx) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Analytics / Grafik Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Perbandingan Bulanan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Grafik total pemasukan vs pengeluaran 6 bulan terakhir",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Past Months Comparison Bar Chart
                            if (monthlyChartData.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Belum ada data untuk memvisualisasikan tren bulanan.",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            } else {
                                MonthlyComparisonChart(monthlyChartData)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Category Breakdown Donut / Pie chart
                            Text(
                                text = "Distribusi Pengeluaran Kategori",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            val displayMonth = if (selectedMonthFilter == "ALL") "Seluruh Waktu" else selectedMonthFilter
                            Text(
                                text = "Breakdown alokasi belanja untuk periode: $displayMonth",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (categoryShares.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Belum ada data pengeluaran (belanja) pada periode ini untuk dianalisis.",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            } else {
                                CategoryDonutChartBlock(categoryShares)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal adding transaction dialog
    if (showAddDialog) {
        TransactionEntryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, type, category, timestamp, note ->
                viewModel.insertTransaction(title, amount, type, category, timestamp, note)
                showAddDialog = false
            }
        )
    }

    // Modal editing transaction dialog
    transactionToEdit?.let { tx ->
        TransactionEntryDialog(
            transactionToEdit = tx,
            onDismiss = { transactionToEdit = null },
            onSave = { title, amount, type, category, timestamp, note ->
                val updated = tx.copy(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    timestamp = timestamp,
                    note = note
                )
                viewModel.updateTransaction(updated)
                transactionToEdit = null
            }
        )
    }
}

// Rupiah visual formatter helper
fun formatRupiah(amount: Double): String {
    val formatter = java.text.DecimalFormat("#,###")
    val symbols = java.text.DecimalFormatSymbols(Locale("id", "ID"))
    symbols.groupingSeparator = '.'
    symbols.decimalSeparator = ','
    formatter.decimalFormatSymbols = symbols
    return "Rp " + formatter.format(amount)
}

// Category mappings with clean icons
fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Makanan & Minuman" -> Icons.Default.Restaurant
        "Belanja" -> Icons.Default.ShoppingCart
        "Transportasi" -> Icons.Default.DirectionsCar
        "Tagihan & Pulsa" -> Icons.Default.ReceiptLong
        "Hiburan" -> Icons.Default.Movie
        "Kesehatan" -> Icons.Default.LocalHospital
        "Pendidikan" -> Icons.Default.School
        "Gaji" -> Icons.Default.Work
        "Investasi" -> Icons.Default.TrendingUp
        "Penjualan" -> Icons.Default.Store
        else -> Icons.Default.Category
    }
}

@Composable
fun MonthFilterSelector(
    availableMonths: List<String>,
    selectedMonth: String,
    onMonthSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = selectedMonth == "ALL",
                    onClick = { onMonthSelected("ALL") },
                    label = { Text("Semua Periode") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            items(availableMonths) { month ->
                FilterChip(
                    selected = selectedMonth == month,
                    onClick = { onMonthSelected(month) },
                    label = { Text(month) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun FinancialStateCard(
    income: Double,
    expense: Double,
    balance: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("financial_state_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Saldo Bersih",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Icon(
                    imageVector = Icons.Default.Wallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formatRupiah(balance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                // Trending Pill
                val isPositive = balance >= 0
                val trendColor = if (isPositive) IncomeColor else ExpenseColor
                val trendBg = if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                val trendIcon = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown
                val pct = if (income > 0) ((balance / income) * 100).toInt().coerceIn(-100, 100) else 12
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(trendBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if (pct >= 0) "+" else ""}$pct%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = trendColor
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBarAndFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Simple elegant text field search input
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Cari catatan finansial...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field"),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Type filter rows: SEMUA, PEMASUKAN, PENGELUARAN
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                "ALL" to "Semua",
                "INCOME" to "Pemasukan",
                "EXPENSE" to "Pengeluaran"
            )

            filters.forEach { (key, display) ->
                val isSelected = selectedType == key
                val backgroundColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                }
                val textColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                val borderStroke = if (isSelected) {
                    null
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(backgroundColor)
                        .clickable { onTypeSelected(key) }
                        .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(20.dp)) else Modifier)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = display,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Block
            val isIncome = transaction.type == "INCOME"
            val circleColor = Color(getCategoryColor(transaction.category)).copy(alpha = 0.15f)
            val iconColor = Color(getCategoryColor(transaction.category))

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(circleColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(transaction.category),
                    contentDescription = transaction.category,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Transaction Details Block
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(transaction.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                if (transaction.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${transaction.note}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount, Sign & Action
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val sign = if (isIncome) "+" else "-"
                val valColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                
                Text(
                    text = "$sign${formatRupiah(transaction.amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = valColor
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_transaction_${transaction.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(
    hasSearchOrFilters: Boolean,
    onResetFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasSearchOrFilters) Icons.Default.SearchOff else Icons.Default.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (hasSearchOrFilters) "Catatan tidak ditemukan" else "Belum Ada Catatan Keuangan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (hasSearchOrFilters) 
                "Tidak ada transaksi yang cocok dengan kriteria pencarian Anda." 
                else "Ketuk tombol + di bawah untuk menambahkan catatan pemasukan atau pengeluaran pertama Anda.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        if (hasSearchOrFilters) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onResetFilters,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Reset Pencarian")
            }
        }
    }
}

// CUSTOM BAR CHART FOR MONTH OVERVIEW
@Composable
fun MonthlyComparisonChart(
    chartData: List<MonthlyBarData>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monthly_comparison_chart"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Chart Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF2E7D32), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pemasukan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFFC62828), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pengeluaran", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas drawing
            val textPaintColor = MaterialTheme.colorScheme.onSurfaceVariant
            val linePaintColor = MaterialTheme.colorScheme.outlineVariant

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height

                    // Margins for axes
                    val bottomMargin = 30.dp.toPx()
                    val leftMargin = 10.dp.toPx()
                    val rightMargin = 10.dp.toPx()
                    val topMargin = 10.dp.toPx()

                    val graphHeight = height - bottomMargin - topMargin
                    val graphWidth = width - leftMargin - rightMargin

                    // Find max value to calibrate height scale (minimum Rp 1,000,000 baseline)
                    val maxVal = chartData.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1000000.0
                    val maxScale = maxOf(maxVal * 1.15, 1000000.0) // 15% clear margin at top

                    // Draw baseline gridlines (3 horizontal rows)
                    for (i in 0..3) {
                        val gridHeight = topMargin + graphHeight * (i / 3f)
                        drawLine(
                            color = linePaintColor,
                            start = Offset(leftMargin, gridHeight),
                            end = Offset(width - rightMargin, gridHeight),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Compute intervals for drawing the columns
                    val columnGroupCount = chartData.size
                    if (columnGroupCount > 0) {
                        val groupWidth = graphWidth / columnGroupCount
                        val barSpacing = 4.dp.toPx()
                        val singleBarWidth = (groupWidth * 0.35f).coerceIn(8.dp.toPx()..24.dp.toPx())

                        chartData.forEachIndexed { index, item ->
                            val cx = leftMargin + (index * groupWidth) + (groupWidth / 2f)

                            // Income bar coordinates
                            val incomePct = (item.income / maxScale).toFloat()
                            val incomeHeight = graphHeight * incomePct
                            val incomeY = topMargin + graphHeight - incomeHeight
                            val incomeX = cx - singleBarWidth - (barSpacing / 2f)

                            // Draw Income bar (Green)
                            if (incomeHeight > 1f) {
                                drawRoundRect(
                                    color = Color(0xFF2E7D32),
                                    topLeft = Offset(incomeX, incomeY),
                                    size = Size(singleBarWidth, incomeHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }

                            // Expense bar coordinates
                            val expensePct = (item.expense / maxScale).toFloat()
                            val expenseHeight = graphHeight * expensePct
                            val expenseY = topMargin + graphHeight - expenseHeight
                            val expenseX = cx + (barSpacing / 2f)

                            // Draw Expense bar (Red)
                            if (expenseHeight > 1f) {
                                drawRoundRect(
                                    color = Color(0xFFC62828),
                                    topLeft = Offset(expenseX, expenseY),
                                    size = Size(singleBarWidth, expenseHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // Month text elements placed beneath the Canvas layout dynamically
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    chartData.forEach { data ->
                        // Shorten e.g. "Juni 2026" to "Jun 26" / "Juni"
                        val formattedLabel = if (data.monthName.contains(" ")) {
                            val parts = data.monthName.split(" ")
                            val monthAbbr = parts[0].take(3)
                            val yrStr = parts[1].takeLast(2)
                            "$monthAbbr '$yrStr"
                        } else {
                            data.monthName.take(3)
                        }

                        Text(
                            text = formattedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.width(42.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// CATEGORY BREAKDOWN BLOCK WITH DONUT CHART & ROW REPRESENTATIONS
@Composable
fun CategoryDonutChartBlock(
    shares: List<CategoryShare>
) {
    val totalExpense = shares.sumOf { it.amount }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_breakdown_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant donut chart canvas
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    shares.forEach { share ->
                        val sweepAngle = share.percentage * 360f
                        drawArc(
                            color = Color(share.color),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }
                
                // Total Expense value centered inside the Donut
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Total Belanja",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatRupiah(totalExpense),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Legend and list item
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                shares.forEach { share ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Colored Circle Indicator
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(share.color))
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = share.category,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${(share.percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp)
                        )

                        Text(
                            text = formatRupiah(share.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// UNIFIED TRANSACTION ENTRY & EDITOR DIALOG (BUILT ON STANDARD COMPOSABLE DIALOG FOR COMPATIBILITY)
@Composable
fun TransactionEntryDialog(
    transactionToEdit: Transaction? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, type: String, category: String, timestamp: Long, note: String) -> Unit
) {
    val isEditMode = transactionToEdit != null

    // Fields
    var title by remember { mutableStateOf(transactionToEdit?.title ?: "") }
    var amountStr by remember { mutableStateOf(transactionToEdit?.amount?.toInt()?.toString() ?: "") }
    var type by remember { mutableStateOf(transactionToEdit?.type ?: "EXPENSE") } // EXPENSE or INCOME
    var selectedCategory by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(transactionToEdit?.note ?: "") }
    var timestamp by remember { mutableStateOf(transactionToEdit?.timestamp ?: System.currentTimeMillis()) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }

    // Constants categorized options
    val expenseCategories = listOf("Makanan & Minuman", "Belanja", "Transportasi", "Tagihan & Pulsa", "Hiburan", "Kesehatan", "Pendidikan", "Lainnya")
    val incomeCategories = listOf("Gaji", "Investasi", "Penjualan", "Lainnya")

    // Automatically set default category when type or mode changes
    LaunchedEffect(type) {
        val lists = if (type == "EXPENSE") expenseCategories else incomeCategories
        if (transactionToEdit != null && transactionToEdit.type == type && lists.contains(transactionToEdit.category)) {
            selectedCategory = transactionToEdit.category
        } else {
            selectedCategory = lists.first()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("entry_dialog_container"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Text(
                    text = if (isEditMode) "Edit Catatan Finansial" else "Tambah Catatan Finansial",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Type Toggle Selector (Pemasukan vs Pengeluaran)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Button(
                        onClick = { type = "EXPENSE" },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_expense"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "EXPENSE") Color(0xFFC62828) else MaterialTheme.colorScheme.outlineVariant,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Pengeluaran")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { type = "INCOME" },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_income"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "INCOME") Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Pemasukan")
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Deskripsi / Nama Catatan") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            amountStr = input
                        }
                    },
                    label = { Text("Jumlah (Rupiah)") },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_amount"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category selection block
                Text(
                    text = "Kategori",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                val currentCategoryList = if (type == "EXPENSE") expenseCategories else incomeCategories
                
                // Flow-row of category buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val chunks = currentCategoryList.chunked(3)
                    chunks.forEach { rowCategories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowCategories.forEach { category ->
                                val isCatSelected = selectedCategory == category
                                val catBg = if (isCatSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                                val bColor = if (isCatSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(catBg)
                                        .border(1.dp, bColor, RoundedCornerShape(8.dp))
                                        .clickable { selectedCategory = category }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            // Fill blank space if chunk row under-populated
                            if (rowCategories.size < 3) {
                                (rowCategories.size until 3).forEach { _ ->
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date Picker Block using native Android DatePickerDialog (Zero experimental APIs)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tanggal Transaksi",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date(timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Button(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, yr, mn, dy ->
                                    val pickerCalendar = Calendar.getInstance()
                                    pickerCalendar.set(Calendar.YEAR, yr)
                                    pickerCalendar.set(Calendar.MONTH, mn)
                                    pickerCalendar.set(Calendar.DAY_OF_MONTH, dy)
                                    timestamp = pickerCalendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pilih")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Note / Memo Input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan tambahan (Opsional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_note")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons Row (Save & Cancel)
                val isFormValid = title.isNotBlank() && amountStr.isNotBlank() && amountStr.toDoubleOrNull() != null && amountStr.toDouble() > 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("btn_cancel")) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isFormValid) {
                                onSave(title, amountStr.toDouble(), type, selectedCategory, timestamp, note)
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier.testTag("btn_save"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isEditMode) "Perbarui" else "Simpan")
                    }
                }
            }
        }
    }
}
