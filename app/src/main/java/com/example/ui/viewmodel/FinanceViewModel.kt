package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Transaction
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao)
        
        // Populate mock data if database is empty
        viewModelScope.launch {
            repository.allTransactions.first().let { list ->
                if (list.isEmpty()) {
                    populateMockData()
                }
            }
        }
    }

    // All transaction flow
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow("ALL") // "ALL", "INCOME", "EXPENSE"
    val selectedCategoryFilter = MutableStateFlow("ALL")
    
    // Month picker state: e.g. "Juni 2026", "Mei 2026" or "Semua"
    val selectedMonthFilter = MutableStateFlow("ALL") // "ALL" or specific format "MMMM yyyy"

    // Combined live state
    val uiState = combine(
        allTransactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryFilter,
        selectedMonthFilter
    ) { txs, query, type, cat, month ->
        val filtered = txs.filter { tx ->
            val matchQuery = tx.title.contains(query, ignoreCase = true) || 
                             tx.category.contains(query, ignoreCase = true) ||
                             tx.note.contains(query, ignoreCase = true)
            val matchType = type == "ALL" || tx.type == type
            val matchCat = cat == "ALL" || tx.category == cat
            
            val matchMonth = if (month == "ALL") {
                true
            } else {
                getMonthYearString(tx.timestamp) == month
            }
            
            matchQuery && matchType && matchCat && matchMonth
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All distinct months in the dataset for dropdown/options
    val availableMonths: StateFlow<List<String>> = allTransactions.map { txs ->
        txs.map { getMonthYearString(it.timestamp) }
            .distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total income, expense, and balance for current selected month/view
    val financialSummary = combine(
        allTransactions,
        selectedMonthFilter
    ) { txs, month ->
        val currentTxs = if (month == "ALL") {
            txs
        } else {
            txs.filter { getMonthYearString(it.timestamp) == month }
        }

        val totalIncome = currentTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = currentTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        FinancialSummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = balance
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary(0.0, 0.0, 0.0))

    // Data for Monthly comparison chart (Income vs Expense over past 5-6 active months)
    val monthlyChartData: StateFlow<List<MonthlyBarData>> = allTransactions.map { txs ->
        txs.groupBy { getMonthYearString(it.timestamp) }
            .map { (monthName, monthTxs) ->
                val income = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
                val expense = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                MonthlyBarData(
                    monthName = monthName,
                    income = income,
                    expense = expense,
                    // Parse raw date to sort chronologically
                    rawDate = monthTxs.firstOrNull()?.timestamp ?: 0L
                )
            }
            .sortedBy { it.rawDate }
            .takeLast(6) // Take past 6 active months
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Data for Category breakdown pie/donut chart
    val categoryBreakdown: StateFlow<List<CategoryShare>> = combine(
        allTransactions,
        selectedMonthFilter
    ) { txs, month ->
        val currentTxs = if (month == "ALL") {
            txs
        } else {
            txs.filter { getMonthYearString(it.timestamp) == month }
        }

        val expensesOnly = currentTxs.filter { it.type == "EXPENSE" }
        val totalExpense = expensesOnly.sumOf { it.amount }

        if (totalExpense == 0.0) {
            emptyList()
        } else {
            expensesOnly.groupBy { it.category }
                .map { (catName, catTxs) ->
                    val sum = catTxs.sumOf { it.amount }
                    CategoryShare(
                        category = catName,
                        amount = sum,
                        percentage = (sum / totalExpense).toFloat(),
                        color = getCategoryColor(catName)
                    )
                }
                .sortedByDescending { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun insertTransaction(title: String, amount: Double, type: String, category: String, timestamp: Long, note: String) {
        viewModelScope.launch {
            val tx = Transaction(
                title = title,
                amount = amount,
                type = type,
                category = category,
                timestamp = timestamp,
                note = note
            )
            repository.insert(tx)
        }
    }

    fun updateTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.update(tx)
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.delete(tx)
        }
    }

    private suspend fun populateMockData() {
        // Create mock data
        
        // Helper to subtract months
        fun getTimestampForMonthOffset(offset: Int, day: Int): Long {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -offset)
            c.set(Calendar.DAY_OF_MONTH, day)
            c.set(Calendar.HOUR_OF_DAY, 12)
            c.set(Calendar.MINUTE, 0)
            return c.timeInMillis
        }

        // Current Month (0)
        repository.insert(Transaction(title = "Gaji Utama Terkirim", amount = 8500000.0, type = "INCOME", category = "Gaji", timestamp = getTimestampForMonthOffset(0, 1)))
        repository.insert(Transaction(title = "Bonus Projek Sampingan", amount = 1500000.0, type = "INCOME", category = "Gaji", timestamp = getTimestampForMonthOffset(0, 10)))
        repository.insert(Transaction(title = "Belanja Bulanan Sayur & Susu", amount = 1200000.0, type = "EXPENSE", category = "Belanja", timestamp = getTimestampForMonthOffset(0, 2)))
        repository.insert(Transaction(title = "Makan Siang Resto Bebek", amount = 145000.0, type = "EXPENSE", category = "Makanan & Minuman", timestamp = getTimestampForMonthOffset(0, 5)))
        repository.insert(Transaction(title = "Bayar Tagihan Wifi & Listrik", amount = 550000.0, type = "EXPENSE", category = "Tagihan & Pulsa", timestamp = getTimestampForMonthOffset(0, 4)))
        repository.insert(Transaction(title = "Isi Bensin Pertamax Motor", amount = 100000.0, type = "EXPENSE", category = "Transportasi", timestamp = getTimestampForMonthOffset(0, 7)))
        repository.insert(Transaction(title = "Nonton Film XXI bersama teman", amount = 120000.0, type = "EXPENSE", category = "Hiburan", timestamp = getTimestampForMonthOffset(0, 14)))

        // Previous Month (1)
        repository.insert(Transaction(title = "Gaji Utama", amount = 8500000.0, type = "INCOME", category = "Gaji", timestamp = getTimestampForMonthOffset(1, 1)))
        repository.insert(Transaction(title = "Deviden Saham", amount = 450000.0, type = "INCOME", category = "Investasi", timestamp = getTimestampForMonthOffset(1, 15)))
        repository.insert(Transaction(title = "Belanja Bulanan Supermarket", amount = 1800000.0, type = "EXPENSE", category = "Belanja", timestamp = getTimestampForMonthOffset(1, 2)))
        repository.insert(Transaction(title = "Beli Kopi Kafe Gaul", amount = 45000.0, type = "EXPENSE", category = "Makanan & Minuman", timestamp = getTimestampForMonthOffset(1, 8)))
        repository.insert(Transaction(title = "Beli Buku Pemrograman", amount = 200000.0, type = "EXPENSE", category = "Pendidikan", timestamp = getTimestampForMonthOffset(1, 12)))
        repository.insert(Transaction(title = "Bayar Tagihan Wifi & Listrik", amount = 550000.0, type = "EXPENSE", category = "Tagihan & Pulsa", timestamp = getTimestampForMonthOffset(1, 4)))
        repository.insert(Transaction(title = "Service Rutin Yamaha", amount = 350000.0, type = "EXPENSE", category = "Transportasi", timestamp = getTimestampForMonthOffset(1, 20)))

        // Two Months Ago (2)
        repository.insert(Transaction(title = "Gaji Utama", amount = 8500000.0, type = "INCOME", category = "Gaji", timestamp = getTimestampForMonthOffset(2, 1)))
        repository.insert(Transaction(title = "Hasil Jualan Sepatu Bekas", amount = 600000.0, type = "INCOME", category = "Penjualan", timestamp = getTimestampForMonthOffset(2, 6)))
        repository.insert(Transaction(title = "Belanja Bulanan Sayur", amount = 1100000.0, type = "EXPENSE", category = "Belanja", timestamp = getTimestampForMonthOffset(2, 2)))
        repository.insert(Transaction(title = "Berobat ke Klinik (Flu)", amount = 250000.0, type = "EXPENSE", category = "Kesehatan", timestamp = getTimestampForMonthOffset(2, 11)))
        repository.insert(Transaction(title = "Bayar Tagihan Wifi & Listrik", amount = 550000.0, type = "EXPENSE", category = "Tagihan & Pulsa", timestamp = getTimestampForMonthOffset(2, 4)))
        repository.insert(Transaction(title = "Dinner Anniversary Keren", amount = 600000.0, type = "EXPENSE", category = "Makanan & Minuman", timestamp = getTimestampForMonthOffset(2, 25)))
        repository.insert(Transaction(title = "Membeli Langganan Netflix", amount = 186000.0, type = "EXPENSE", category = "Hiburan", timestamp = getTimestampForMonthOffset(2, 18)))
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FinanceViewModel(application) as T
            }
        }
    }
}

data class FinancialSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double
)

data class MonthlyBarData(
    val monthName: String,
    val income: Double,
    val expense: Double,
    val rawDate: Long
)

data class CategoryShare(
    val category: String,
    val amount: Double,
    val percentage: Float, // 0.0f to 1.0f
    val color: Long // Hex color value
)

fun getMonthYearString(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
    return sdf.format(date)
}

fun getCategoryColor(category: String): Long {
    return when (category) {
        "Makanan & Minuman" -> 0xFFFF7043 // Coral
        "Belanja" -> 0xFF42A5F5 // Blue
        "Transportasi" -> 0xFF26A69A // Teal
        "Tagihan & Pulsa" -> 0xFFAB47BC // Purple
        "Hiburan" -> 0xFFEC407A // Pink / Rose
        "Kesehatan" -> 0xFFEF5350 // Red
        "Pendidikan" -> 0xFF8D6E63 // Brown
        "Gaji" -> 0xFF66BB6A // Lime Green
        "Investasi" -> 0xFF81C784 // Green Light
        "Penjualan" -> 0xFFFFCA28 // Yellow / Amber
        else -> 0xFF78909C // Blue Grey
    }
}
