package com.azmisava.budgetguard.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azmisava.budgetguard.BackgroundSoftLilac
import com.azmisava.budgetguard.DangerRed
import com.azmisava.budgetguard.PrimaryLavender
import com.azmisava.budgetguard.SuccessGreen
import com.azmisava.budgetguard.TextMain
import com.azmisava.budgetguard.TextMuted
import com.azmisava.budgetguard.util.formatMoney
import com.azmisava.budgetguard.util.PrivacyState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class HistoryTransactionItem(
    val id: String = "",
    val type: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val note: String = "",
    val reason: String = "",
    val selectedDate: String = "",
    val createdAt: Long = 0L
)

private data class HistoryPeriodGroup(
    val title: String,
    val transactions: List<HistoryTransactionItem>
)

@Composable
fun RiwayatScreen(
    auth: FirebaseAuth
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val userId = auth.currentUser?.uid

    var transactions by remember { mutableStateOf<List<HistoryTransactionItem>>(emptyList()) }
    var selectedFilter by rememberSaveable { mutableStateOf("Semua") }
    var selectedGroupMode by rememberSaveable { mutableStateOf("Hari") }
    val privacyMode = PrivacyState.enabled
    var message by rememberSaveable { mutableStateOf("") }

    var editingTransactionId by rememberSaveable { mutableStateOf<String?>(null) }
    var editType by rememberSaveable { mutableStateOf("expense") }
    var editAmountText by rememberSaveable { mutableStateOf("") }
    var editCategory by rememberSaveable { mutableStateOf("") }
    var editSelectedDate by rememberSaveable { mutableStateOf("") }
    var editNote by rememberSaveable { mutableStateOf("") }
    var editReason by rememberSaveable { mutableStateOf("") }

    val incomeCategories = listOf("Gaji", "Freelance", "Investasi", "Bonus", "Lainnya")

    val expenseCategories = listOf(
        "Makanan",
        "Transportasi",
        "Belanja",
        "Hiburan",
        "Kesehatan",
        "Pendidikan",
        "Tagihan",
        "Lainnya"
    )

    val expenseReasons = listOf("Kebutuhan", "Keinginan", "Darurat", "Hiburan")

    fun loadTransactions() {
        if (userId == null) {
            message = "User belum login"
            return
        }

        db.collection("users")
            .document(userId)
            .collection("transactions")
            .get()
            .addOnSuccessListener { result ->
                transactions = result.documents.map { document ->
                    HistoryTransactionItem(
                        id = document.id,
                        type = document.getString("type") ?: "",
                        amount = document.getDouble("amount") ?: 0.0,
                        category = document.getString("category") ?: "",
                        note = document.getString("note") ?: "",
                        reason = document.getString("reason") ?: "",
                        selectedDate = document.getString("selectedDate") ?: "",
                        createdAt = document.getLong("createdAt") ?: 0L
                    )
                }.sortedByDescending {
                    getTransactionDate(it).time
                }

                message = ""
            }
            .addOnFailureListener { error ->
                message = error.message ?: "Gagal mengambil data riwayat"
            }
    }

    fun deleteTransaction(transactionId: String) {
        if (userId == null) {
            message = "User belum login"
            return
        }

        db.collection("users")
            .document(userId)
            .collection("transactions")
            .document(transactionId)
            .delete()
            .addOnSuccessListener {
                message = "Transaksi berhasil dihapus"
                loadTransactions()
            }
            .addOnFailureListener { error ->
                message = error.message ?: "Gagal menghapus transaksi"
            }
    }

    fun startEditTransaction(transaction: HistoryTransactionItem) {
        editingTransactionId = transaction.id
        editType = transaction.type
        editAmountText = transaction.amount.toLong().toString()
        editCategory = transaction.category
        editSelectedDate = transaction.selectedDate.ifBlank {
            formatEditableDate(transaction.createdAt)
        }
        editNote = transaction.note
        editReason = transaction.reason
        message = ""
    }

    fun cancelEditTransaction() {
        editingTransactionId = null
        editAmountText = ""
        editCategory = ""
        editSelectedDate = ""
        editNote = ""
        editReason = ""
        message = ""
    }

    fun updateTransaction() {
        if (userId == null) {
            message = "User belum login"
            return
        }

        val transactionId = editingTransactionId

        if (transactionId == null) {
            message = "Tidak ada transaksi yang sedang diedit"
            return
        }

        val amount = editAmountText.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            message = "Nominal harus angka lebih dari 0"
            return
        }

        if (editCategory.isBlank()) {
            message = "Kategori wajib dipilih"
            return
        }

        if (editSelectedDate.isBlank()) {
            message = "Tanggal wajib dipilih"
            return
        }

        if (editType == "expense" && editReason.isBlank()) {
            message = "Alasan pengeluaran wajib dipilih"
            return
        }

        val updatedData = mapOf(
            "type" to editType,
            "amount" to amount,
            "category" to editCategory,
            "selectedDate" to editSelectedDate,
            "note" to editNote,
            "reason" to editReason,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .collection("transactions")
            .document(transactionId)
            .update(updatedData)
            .addOnSuccessListener {
                message = "Transaksi berhasil diperbarui"
                editingTransactionId = null
                loadTransactions()
            }
            .addOnFailureListener { error ->
                message = error.message ?: "Gagal memperbarui transaksi"
            }
    }

    LaunchedEffect(userId) {
        loadTransactions()
    }

    val filteredTransactions = when (selectedFilter) {
        "Pemasukan" -> transactions.filter { it.type == "income" }
        "Pengeluaran" -> transactions.filter { it.type == "expense" }
        else -> transactions
    }

    val groupedTransactions = buildPeriodGroups(
        transactions = filteredTransactions,
        groupMode = selectedGroupMode
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoftLilac)
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(34.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                TextButton(
                    onClick = {
                        PrivacyState.enabled = !PrivacyState.enabled
                    }
                ) {
                    Text(
                        text = "Privasi",
                        fontSize = 13.sp,
                        color = PrimaryLavender
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilterChip(
                    text = "Semua",
                    selected = selectedFilter == "Semua",
                    onClick = {
                        selectedFilter = "Semua"
                    }
                )

                HistoryFilterChip(
                    text = "Pemasukan",
                    selected = selectedFilter == "Pemasukan",
                    onClick = {
                        selectedFilter = "Pemasukan"
                    }
                )

                HistoryFilterChip(
                    text = "Pengeluaran",
                    selected = selectedFilter == "Pengeluaran",
                    onClick = {
                        selectedFilter = "Pengeluaran"
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Kelompokkan berdasarkan",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilterChip(
                    text = "Hari",
                    selected = selectedGroupMode == "Hari",
                    onClick = {
                        selectedGroupMode = "Hari"
                    }
                )

                HistoryFilterChip(
                    text = "Minggu",
                    selected = selectedGroupMode == "Minggu",
                    onClick = {
                        selectedGroupMode = "Minggu"
                    }
                )

                HistoryFilterChip(
                    text = "Bulan",
                    selected = selectedGroupMode == "Bulan",
                    onClick = {
                        selectedGroupMode = "Bulan"
                    }
                )

                HistoryFilterChip(
                    text = "Tahun",
                    selected = selectedGroupMode == "Tahun",
                    onClick = {
                        selectedGroupMode = "Tahun"
                    }
                )
            }

            if (editingTransactionId != null) {
                Spacer(modifier = Modifier.height(16.dp))

                EditTransactionCard(
                    editType = editType,
                    onEditTypeChange = {
                        editType = it
                        editCategory = ""
                        editReason = ""
                    },
                    editAmountText = editAmountText,
                    onEditAmountChange = {
                        editAmountText = it.filter { char -> char.isDigit() }
                    },
                    editCategory = editCategory,
                    onEditCategoryChange = {
                        editCategory = it
                    },
                    editSelectedDate = editSelectedDate,
                    onEditSelectedDateChange = {
                        editSelectedDate = it
                    },
                    editNote = editNote,
                    onEditNoteChange = {
                        editNote = it
                    },
                    editReason = editReason,
                    onEditReasonChange = {
                        editReason = it
                    },
                    incomeCategories = incomeCategories,
                    expenseCategories = expenseCategories,
                    expenseReasons = expenseReasons,
                    onCancel = {
                        cancelEditTransaction()
                    },
                    onSave = {
                        updateTransaction()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredTransactions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Belum ada transaksi pada filter ini",
                        modifier = Modifier.padding(18.dp),
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
            } else {
                groupedTransactions.forEach { periodGroup ->
                    PeriodGroupCard(
                        title = periodGroup.title,
                        transactions = periodGroup.transactions,
                        privacyMode = privacyMode,
                        onEdit = { transaction ->
                            startEditTransaction(transaction)
                        },
                        onDelete = { transaction ->
                            deleteTransaction(transaction.id)
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = if (message.contains("berhasil", ignoreCase = true)) TextMain else DangerRed
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HistoryFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = {
            onClick()
        },
        shape = RoundedCornerShape(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) PrimaryLavender else Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else TextMuted
        )
    }
}

@Composable
private fun PeriodGroupCard(
    title: String,
    transactions: List<HistoryTransactionItem>,
    privacyMode: Boolean,
    onEdit: (HistoryTransactionItem) -> Unit,
    onDelete: (HistoryTransactionItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (transactions.isEmpty()) {
                Text(
                    text = "Belum ada transaksi pada periode ini",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            } else {
                transactions.forEachIndexed { index, transaction ->
                    HistoryTransactionCard(
                        transaction = transaction,
                        privacyMode = privacyMode,
                        onEdit = {
                            onEdit(transaction)
                        },
                        onDelete = {
                            onDelete(transaction)
                        }
                    )

                    if (index != transactions.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF0EAF8))
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTransactionCard(
    transaction: HistoryTransactionItem,
    privacyMode: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val amountText = if (transaction.type == "income") {
        "+${formatMoney(transaction.amount, privacyMode)}"
    } else {
        "-${formatMoney(transaction.amount, privacyMode)}"
    }

    val amountColor = if (transaction.type == "income") SuccessGreen else DangerRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        HistoryIconBox(
            type = transaction.type
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = transaction.category.ifBlank {
                    if (transaction.type == "income") "Pemasukan" else "Pengeluaran"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = buildHistorySubtitle(transaction),
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = amountText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        onEdit()
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "Edit",
                        fontSize = 12.sp,
                        color = PrimaryLavender
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        onDelete()
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "Hapus",
                        fontSize = 12.sp,
                        color = DangerRed
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryIconBox(
    type: String
) {
    val backgroundColor = if (type == "income") {
        Color(0xFFEFFFF4)
    } else {
        Color(0xFFF3EDFF)
    }

    val iconColor = if (type == "income") SuccessGreen else PrimaryLavender
    val icon = if (type == "income") "▣" else "◌"

    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 18.sp,
            color = iconColor
        )
    }
}

private fun buildHistorySubtitle(
    transaction: HistoryTransactionItem
): String {
    val dateText = transaction.selectedDate.ifBlank {
        formatHistoryDate(transaction.createdAt)
    }

    return if (transaction.type == "income") {
        if (transaction.note.isNotBlank()) {
            "${transaction.note} · $dateText"
        } else {
            dateText
        }
    } else {
        val reasonText = transaction.reason.ifBlank { "pengeluaran" }

        if (transaction.note.isNotBlank()) {
            "${transaction.note} · $reasonText · $dateText"
        } else {
            "${transaction.category} · $reasonText · $dateText"
        }
    }
}

private fun formatHistoryDate(
    timeMillis: Long
): String {
    if (timeMillis <= 0L) {
        return "-"
    }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
    return formatter.format(Date(timeMillis))
}

private fun formatEditableDate(
    timeMillis: Long
): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))

    return if (timeMillis > 0L) {
        formatter.format(Date(timeMillis))
    } else {
        formatter.format(Date())
    }
}

private fun buildPeriodGroups(
    transactions: List<HistoryTransactionItem>,
    groupMode: String
): List<HistoryPeriodGroup> {
    return transactions
        .sortedByDescending {
            getTransactionDate(it).time
        }
        .groupBy { transaction ->
            buildPeriodTitle(
                date = getTransactionDate(transaction),
                groupMode = groupMode
            )
        }
        .map { entry ->
            val periodTransactions = entry.value

            HistoryPeriodGroup(
                title = entry.key,
                transactions = periodTransactions
            )
        }
}

private fun buildPeriodTitle(
    date: Date,
    groupMode: String
): String {
    val locale = Locale("id", "ID")

    return when (groupMode) {
        "Hari" -> {
            SimpleDateFormat("EEEE, dd MMM yyyy", locale).format(date)
        }

        "Minggu" -> {
            val calendar = Calendar.getInstance(locale)
            calendar.time = date
            calendar.firstDayOfWeek = Calendar.MONDAY

            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val startDate = calendar.time

            calendar.add(Calendar.DAY_OF_MONTH, 6)
            val endDate = calendar.time

            val formatter = SimpleDateFormat("dd MMM yyyy", locale)
            "Minggu, ${formatter.format(startDate)} - ${formatter.format(endDate)}"
        }

        "Tahun" -> {
            SimpleDateFormat("yyyy", locale).format(date)
        }

        else -> {
            SimpleDateFormat("MMMM yyyy", locale).format(date)
        }
    }
}

private fun getTransactionDate(
    transaction: HistoryTransactionItem
): Date {
    if (transaction.selectedDate.isNotBlank()) {
        try {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
            return formatter.parse(transaction.selectedDate) ?: Date(transaction.createdAt)
        } catch (_: Exception) {
            // Jika format tanggal lama berbeda, fallback ke createdAt
        }
    }

    return if (transaction.createdAt > 0L) {
        Date(transaction.createdAt)
    } else {
        Date(0L)
    }
}

@Composable
private fun EditTransactionCard(
    editType: String,
    onEditTypeChange: (String) -> Unit,
    editAmountText: String,
    onEditAmountChange: (String) -> Unit,
    editCategory: String,
    onEditCategoryChange: (String) -> Unit,
    editSelectedDate: String,
    onEditSelectedDateChange: (String) -> Unit,
    editNote: String,
    onEditNoteChange: (String) -> Unit,
    editReason: String,
    onEditReasonChange: (String) -> Unit,
    incomeCategories: List<String>,
    expenseCategories: List<String>,
    expenseReasons: List<String>,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val categories = if (editType == "income") incomeCategories else expenseCategories
    val context = LocalContext.current

    fun openDatePicker() {
        val calendar = Calendar.getInstance(Locale("id", "ID"))

        if (editSelectedDate.isNotBlank()) {
            try {
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
                val parsedDate = formatter.parse(editSelectedDate)

                if (parsedDate != null) {
                    calendar.time = parsedDate
                }
            } catch (_: Exception) {
                // Jika format tanggal tidak terbaca, gunakan tanggal hari ini.
            }
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance(Locale("id", "ID"))
                selectedCalendar.set(year, month, dayOfMonth)

                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
                onEditSelectedDateChange(formatter.format(selectedCalendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Edit Transaksi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilterChip(
                    text = "Pemasukan",
                    selected = editType == "income",
                    onClick = {
                        onEditTypeChange("income")
                    }
                )

                HistoryFilterChip(
                    text = "Pengeluaran",
                    selected = editType == "expense",
                    onClick = {
                        onEditTypeChange("expense")
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = editAmountText,
                onValueChange = {
                    onEditAmountChange(it)
                },
                label = {
                    Text("Nominal")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tanggal",
                fontSize = 13.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    openDatePicker()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF8F4FF)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = editSelectedDate.ifBlank { "Pilih tanggal" },
                    color = TextMain,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Kategori",
                fontSize = 13.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            categories.chunked(3).forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { category ->
                        HistoryFilterChip(
                            text = category,
                            selected = editCategory == category,
                            onClick = {
                                onEditCategoryChange(category)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = editNote,
                onValueChange = {
                    onEditNoteChange(it)
                },
                label = {
                    Text("Catatan")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            if (editType == "expense") {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Alasan Pengeluaran",
                    fontSize = 13.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                expenseReasons.chunked(3).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { reason ->
                            HistoryFilterChip(
                                text = reason,
                                selected = editReason == reason,
                                onClick = {
                                    onEditReasonChange(reason)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onCancel()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF0EAF8)
                    )
                ) {
                    Text(
                        text = "Batal",
                        color = TextMuted
                    )
                }

                Button(
                    onClick = {
                        onSave()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLavender
                    )
                ) {
                    Text(
                        text = "Simpan",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
