package com.azmisava.budgetguard.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azmisava.budgetguard.BackgroundSoftLilac
import com.azmisava.budgetguard.DangerRed
import com.azmisava.budgetguard.PrimaryLavender
import com.azmisava.budgetguard.SuccessGreen
import com.azmisava.budgetguard.TextMain
import com.azmisava.budgetguard.TextMuted
import com.azmisava.budgetguard.WarningYellow
import com.azmisava.budgetguard.util.PrivacyState
import com.azmisava.budgetguard.util.formatMoney
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun InsightScreen(
    auth: FirebaseAuth
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val userId = auth.currentUser?.uid

    var transactions by remember { mutableStateOf<List<InsightTransactionItem>>(emptyList()) }
    var selectedPeriod by rememberSaveable { mutableStateOf("Bulan") }
    var periodOffset by rememberSaveable { mutableStateOf(0) }
    var monthlyLimit by rememberSaveable { mutableStateOf(0.0) }
    var message by rememberSaveable { mutableStateOf("") }

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
                    InsightTransactionItem(
                        id = document.id,
                        type = document.getString("type") ?: "",
                        amount = document.getDouble("amount") ?: 0.0,
                        category = document.getString("category") ?: "",
                        note = document.getString("note") ?: "",
                        reason = document.getString("reason") ?: "",
                        selectedDate = document.getString("selectedDate") ?: "",
                        createdAt = document.getLong("createdAt") ?: 0L
                    )
                }

                message = ""
            }
            .addOnFailureListener { error ->
                message = error.message ?: "Gagal mengambil data insight"
            }
    }

    fun loadBudgetLimit() {
        if (userId == null) {
            return
        }

        db.collection("users")
            .document(userId)
            .collection("settings")
            .document("budget")
            .get()
            .addOnSuccessListener { document ->
                monthlyLimit = document.getDouble("monthlyLimit") ?: 0.0
            }
    }

    LaunchedEffect(userId) {
        loadTransactions()
        loadBudgetLimit()
    }

    val privacyMode = PrivacyState.enabled
    val referenceDate = buildReferenceDate(
        selectedPeriod = selectedPeriod,
        periodOffset = periodOffset
    )

    val periodTransactions = transactions.filter { transaction ->
        isTransactionInSelectedPeriod(
            transaction = transaction,
            selectedPeriod = selectedPeriod,
            referenceDate = referenceDate
        )
    }

    val totalIncome = periodTransactions
        .filter { it.type == "income" }
        .sumOf { it.amount }

    val totalExpense = periodTransactions
        .filter { it.type == "expense" }
        .sumOf { it.amount }

    val balance = totalIncome - totalExpense

    val expenseByCategory = periodTransactions
        .filter { it.type == "expense" }
        .groupBy { it.category.ifBlank { "Lainnya" } }
        .map { entry ->
            CategoryExpense(
                category = entry.key,
                total = entry.value.sumOf { it.amount }
            )
        }
        .sortedByDescending { it.total }

    val biggestExpenseCategory = expenseByCategory.firstOrNull()

    val limitReference = if (selectedPeriod == "Bulan" && periodOffset == 0) monthlyLimit else 0.0
    val limitPercentage = if (limitReference > 0) {
        (totalExpense / limitReference * 100).coerceAtMost(100.0)
    } else {
        0.0
    }

    val savingRate = if (totalIncome > 0) {
        (balance / totalIncome * 100).coerceAtLeast(0.0).coerceAtMost(100.0)
    } else {
        0.0
    }

    val antiBorosStatus = when {
        periodTransactions.isEmpty() -> "Belum Ada Data"
        selectedPeriod == "Bulan" && periodOffset == 0 && monthlyLimit > 0 && limitPercentage >= 100 -> "Boros"
        selectedPeriod == "Bulan" && periodOffset == 0 && monthlyLimit > 0 && limitPercentage >= 80 -> "Waspada"
        balance >= 0 -> "Terkontrol"
        else -> "Perlu Perhatian"
    }

    val antiBorosColor = when (antiBorosStatus) {
        "Terkontrol" -> SuccessGreen
        "Waspada" -> WarningYellow
        "Boros", "Perlu Perhatian" -> DangerRed
        else -> TextMuted
    }

    val antiBorosMessage = when {
        periodTransactions.isEmpty() -> "Belum ada transaksi pada periode ini. Geser periode atau tambahkan transaksi agar insight bisa dihitung."
        biggestExpenseCategory != null && balance >= 0 -> "Pengeluaran terbesar ada di kategori ${biggestExpenseCategory.category}. Keuangan periode ini masih cukup aman."
        biggestExpenseCategory != null -> "Pengeluaran terbesar ada di kategori ${biggestExpenseCategory.category}. Coba kurangi pengeluaran yang tidak mendesak."
        else -> "Catat transaksi secara rutin agar pola keuangan lebih mudah dianalisis."
    }

    val moodText = when {
        periodTransactions.isEmpty() -> "Belum Dinilai"
        balance >= 0 && savingRate >= 30 -> "Sangat Hemat"
        balance >= 0 -> "Hemat"
        else -> "Boros"
    }

    val moodColor = when (moodText) {
        "Sangat Hemat", "Hemat" -> SuccessGreen
        "Boros" -> DangerRed
        else -> TextMuted
    }

    val healthScore = when {
        periodTransactions.isEmpty() -> 0
        balance >= 0 && savingRate >= 30 -> 100
        balance >= 0 && savingRate >= 10 -> 85
        balance >= 0 -> 70
        else -> 40
    }

    val healthLabel = when {
        healthScore >= 90 -> "Sangat Sehat"
        healthScore >= 75 -> "Sehat"
        healthScore >= 60 -> "Cukup"
        healthScore > 0 -> "Perlu Perhatian"
        else -> "Belum Dinilai"
    }

    val advice = when {
        periodTransactions.isEmpty() -> "Belum ada data pada periode ini. Gunakan tombol kiri/kanan untuk melihat minggu, bulan, atau tahun sebelumnya."
        totalIncome <= 0 -> "Tambahkan data pemasukan agar selisih dan tingkat tabungan bisa dihitung."
        totalExpense <= 0 -> "Belum ada pengeluaran pada periode ini. Kondisi budget terlihat aman."
        balance >= 0 && biggestExpenseCategory != null -> "Kategori ${biggestExpenseCategory.category} menjadi pengeluaran terbesar. Pantau kategori ini agar budget tetap terkendali."
        else -> "Pengeluaran lebih besar dari pemasukan. Fokus pada kebutuhan utama dan kurangi transaksi yang sifatnya keinginan."
    }

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

            Text(
                text = "Insight Budget",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = buildInsightPeriodTitle(
                    selectedPeriod = selectedPeriod,
                    referenceDate = referenceDate
                ),
                fontSize = 14.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(18.dp))

            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { period ->
                    selectedPeriod = period
                    periodOffset = 0
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PeriodNavigator(
                selectedPeriod = selectedPeriod,
                referenceDate = referenceDate,
                canGoNext = periodOffset < 0,
                onPreviousClick = {
                    periodOffset -= 1
                },
                onNextClick = {
                    if (periodOffset < 0) {
                        periodOffset += 1
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            SummaryCards(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = balance,
                privacyMode = privacyMode
            )

            Spacer(modifier = Modifier.height(14.dp))

            IncomeExpenseChartCard(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                privacyMode = privacyMode
            )

            Spacer(modifier = Modifier.height(14.dp))

            ExpenseCategoryChartCard(
                expenses = expenseByCategory,
                privacyMode = privacyMode
            )

            Spacer(modifier = Modifier.height(14.dp))

            AntiBorosCard(
                status = antiBorosStatus,
                message = antiBorosMessage,
                statusColor = antiBorosColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BudgetMoodCard(
                    modifier = Modifier.weight(1f),
                    mood = moodText,
                    moodColor = moodColor
                )

                HealthScoreCard(
                    modifier = Modifier.weight(1f),
                    score = healthScore,
                    label = healthLabel,
                    labelColor = moodColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            SavingRateCard(
                savingRate = savingRate
            )

            Spacer(modifier = Modifier.height(14.dp))

            AdviceCard(
                advice = advice
            )

            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = DangerRed
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class InsightTransactionItem(
    val id: String = "",
    val type: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val note: String = "",
    val reason: String = "",
    val selectedDate: String = "",
    val createdAt: Long = 0L
)

private data class CategoryExpense(
    val category: String,
    val total: Double
)

@Composable
private fun PeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("Minggu", "Bulan", "Tahun").forEach { period ->
            PeriodChip(
                text = period,
                selected = selectedPeriod == period,
                modifier = Modifier.weight(1f),
                onClick = {
                    onPeriodSelected(period)
                }
            )
        }
    }
}

@Composable
private fun PeriodChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = {
            onClick()
        },
        modifier = modifier
            .defaultMinSize(minWidth = 0.dp, minHeight = 0.dp)
            .height(38.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) PrimaryLavender else Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else TextMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun PeriodNavigator(
    selectedPeriod: String,
    referenceDate: Date,
    canGoNext: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PeriodArrowButton(
                text = "‹",
                enabled = true,
                onClick = onPreviousClick
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = buildShortPeriodTitle(
                        selectedPeriod = selectedPeriod,
                        referenceDate = referenceDate
                    ),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Geser untuk melihat periode sebelumnya",
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1
                )
            }

            PeriodArrowButton(
                text = "›",
                enabled = canGoNext,
                onClick = onNextClick
            )
        }
    }
}

@Composable
private fun PeriodArrowButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = {
            onClick()
        },
        enabled = enabled,
        modifier = Modifier
            .size(38.dp)
            .defaultMinSize(minWidth = 0.dp, minHeight = 0.dp),
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) Color(0xFFF3EDFF) else Color(0xFFF7F2FF),
            disabledContainerColor = Color(0xFFF7F2FF)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) PrimaryLavender else Color(0xFFD5C8EA)
        )
    }
}

@Composable
private fun SummaryCards(
    totalIncome: Double,
    totalExpense: Double,
    balance: Double,
    privacyMode: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryMiniCard(
                title = "Pemasukan",
                value = formatMoney(totalIncome, privacyMode),
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )

            SummaryMiniCard(
                title = "Pengeluaran",
                value = formatMoney(totalExpense, privacyMode),
                color = DangerRed,
                modifier = Modifier.weight(1f)
            )
        }

        SummaryMiniCard(
            title = "Selisih",
            value = formatMoney(balance, privacyMode),
            color = if (balance >= 0) SuccessGreen else DangerRed,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SummaryMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IncomeExpenseChartCard(
    totalIncome: Double,
    totalExpense: Double,
    privacyMode: Boolean
) {
    val maxValue = maxOf(totalIncome, totalExpense, 1.0)

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
                text = "Diagram Pemasukan & Pengeluaran",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(14.dp))

            ChartBarRow(
                label = "Pemasukan",
                value = totalIncome,
                maxValue = maxValue,
                barColor = SuccessGreen,
                privacyMode = privacyMode
            )

            Spacer(modifier = Modifier.height(12.dp))

            ChartBarRow(
                label = "Pengeluaran",
                value = totalExpense,
                maxValue = maxValue,
                barColor = DangerRed,
                privacyMode = privacyMode
            )
        }
    }
}

@Composable
private fun ChartBarRow(
    label: String,
    value: Double,
    maxValue: Double,
    barColor: Color,
    privacyMode: Boolean
) {
    val progress = if (maxValue > 0) {
        (value / maxValue).toFloat().coerceIn(0.03f, 1f)
    } else {
        0.03f
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = TextMuted
            )

            Text(
                text = formatMoney(value, privacyMode),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = barColor,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0xFFF1EBFA), RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(10.dp)
                    .background(barColor, RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun ExpenseCategoryChartCard(
    expenses: List<CategoryExpense>,
    privacyMode: Boolean
) {
    val totalExpense = expenses.sumOf { it.total }

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
                text = "Pie Chart Pengeluaran",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Menunjukkan uang habis untuk kategori apa saja",
                fontSize = 13.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (expenses.isEmpty()) {
                EmptyInsightBox(
                    text = "Belum ada pengeluaran pada periode ini"
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpensePieChart(
                        expenses = expenses,
                        modifier = Modifier.size(150.dp)
                    )

                    Spacer(modifier = Modifier.padding(8.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        expenses.take(5).forEachIndexed { index, item ->
                            PieLegendRow(
                                item = item,
                                totalExpense = totalExpense,
                                privacyMode = privacyMode,
                                color = pieChartColors[index % pieChartColors.size]
                            )
                        }

                        if (expenses.size > 5) {
                            Text(
                                text = "+${expenses.size - 5} kategori lain",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Rincian kategori",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Spacer(modifier = Modifier.height(10.dp))

                expenses.forEachIndexed { index, item ->
                    CategoryExpenseDetailRow(
                        item = item,
                        totalExpense = totalExpense,
                        privacyMode = privacyMode,
                        color = pieChartColors[index % pieChartColors.size]
                    )

                    if (index != expenses.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

private val pieChartColors = listOf(
    PrimaryLavender,
    DangerRed,
    SuccessGreen,
    WarningYellow,
    Color(0xFF9EC5FE),
    Color(0xFFFFB3C7),
    Color(0xFFB8E0D2),
    Color(0xFFD7BDE2)
)

@Composable
private fun ExpensePieChart(
    expenses: List<CategoryExpense>,
    modifier: Modifier = Modifier
) {
    val total = expenses.sumOf { it.total }.coerceAtLeast(1.0)

    Canvas(
        modifier = modifier
    ) {
        val diameter = size.minDimension
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f
        )

        var startAngle = -90f

        expenses.forEachIndexed { index, item ->
            val sweepAngle = ((item.total / total) * 360f).toFloat()
            drawArc(
                color = pieChartColors[index % pieChartColors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = topLeft,
                size = Size(diameter, diameter)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun PieLegendRow(
    item: CategoryExpense,
    totalExpense: Double,
    privacyMode: Boolean,
    color: Color
) {
    val percentage = if (totalExpense > 0) {
        (item.total / totalExpense * 100).toInt()
    } else {
        0
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(999.dp))
        )

        Spacer(modifier = Modifier.padding(4.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.category,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "$percentage% • ${formatMoney(item.total, privacyMode)}",
                fontSize = 10.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryExpenseDetailRow(
    item: CategoryExpense,
    totalExpense: Double,
    privacyMode: Boolean,
    color: Color
) {
    val percentage = if (totalExpense > 0) {
        (item.total / totalExpense * 100).toInt()
    } else {
        0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F4FF), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, RoundedCornerShape(999.dp))
        )

        Spacer(modifier = Modifier.padding(6.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.category,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "$percentage% dari total pengeluaran",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        Text(
            text = formatMoney(item.total, privacyMode),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DangerRed,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyInsightBox(
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F4FF), RoundedCornerShape(18.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun AntiBorosCard(
    status: String,
    message: String,
    statusColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                InsightIconBox(
                    icon = "♡",
                    backgroundColor = Color(0xFFEFFFF4),
                    iconColor = SuccessGreen
                )

                Spacer(modifier = Modifier.padding(7.dp))

                Column {
                    Text(
                        text = "Mode Anti Boros",
                        fontSize = 13.sp,
                        color = TextMuted
                    )

                    Text(
                        text = status,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = message,
                fontSize = 14.sp,
                color = TextMuted,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun BudgetMoodCard(
    modifier: Modifier,
    mood: String,
    moodColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            InsightIconBox(
                icon = "☺",
                backgroundColor = Color(0xFFEFFFF4),
                iconColor = SuccessGreen
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Budget Mood",
                fontSize = 13.sp,
                color = TextMuted
            )

            Text(
                text = mood,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = moodColor
            )
        }
    }
}

@Composable
private fun HealthScoreCard(
    modifier: Modifier,
    score: Int,
    label: String,
    labelColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$score",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Text(
                    text = "/100",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Skor Kesehatan",
                fontSize = 13.sp,
                color = TextMuted
            )

            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )
        }
    }
}

@Composable
private fun SavingRateCard(
    savingRate: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                InsightIconBox(
                    icon = "↗",
                    backgroundColor = Color(0xFFEFFFF4),
                    iconColor = SuccessGreen
                )

                Spacer(modifier = Modifier.padding(7.dp))

                Column {
                    Text(
                        text = "Tingkat Tabungan",
                        fontSize = 13.sp,
                        color = TextMuted
                    )

                    Text(
                        text = "${savingRate.toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningYellow
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            LinearProgressIndicator(
                progress = {
                    (savingRate / 100).toFloat()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                color = WarningYellow,
                trackColor = Color(0xFFF0EAF8)
            )
        }
    }
}

@Composable
private fun AdviceCard(
    advice: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4ECFF)),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFE4D4FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "♡",
                    fontSize = 18.sp,
                    color = PrimaryLavender
                )

                Spacer(modifier = Modifier.padding(6.dp))

                Text(
                    text = "Saran",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryLavender
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = advice,
                fontSize = 14.sp,
                color = TextMain,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun InsightIconBox(
    icon: String,
    backgroundColor: Color,
    iconColor: Color
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 22.sp,
            color = iconColor
        )
    }
}

private fun buildReferenceDate(
    selectedPeriod: String,
    periodOffset: Int
): Date {
    val calendar = Calendar.getInstance(Locale("id", "ID"))

    when (selectedPeriod) {
        "Minggu" -> {
            calendar.add(Calendar.WEEK_OF_YEAR, periodOffset)
        }

        "Tahun" -> {
            calendar.add(Calendar.YEAR, periodOffset)
        }

        else -> {
            calendar.add(Calendar.MONTH, periodOffset)
        }
    }

    return calendar.time
}

private fun isTransactionInSelectedPeriod(
    transaction: InsightTransactionItem,
    selectedPeriod: String,
    referenceDate: Date
): Boolean {
    val transactionDate = getInsightTransactionDate(transaction)

    return when (selectedPeriod) {
        "Minggu" -> isSameWeek(transactionDate, referenceDate)
        "Tahun" -> isSameYear(transactionDate, referenceDate)
        else -> isSameMonth(transactionDate, referenceDate)
    }
}

private fun getInsightTransactionDate(
    transaction: InsightTransactionItem
): Date {
    if (transaction.selectedDate.isNotBlank()) {
        try {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
            return formatter.parse(transaction.selectedDate) ?: Date(transaction.createdAt)
        } catch (_: Exception) {
            // fallback ke createdAt
        }
    }

    return if (transaction.createdAt > 0L) {
        Date(transaction.createdAt)
    } else {
        Date(0L)
    }
}

private fun isSameWeek(
    firstDate: Date,
    secondDate: Date
): Boolean {
    val locale = Locale("id", "ID")
    val firstCalendar = Calendar.getInstance(locale)
    val secondCalendar = Calendar.getInstance(locale)

    firstCalendar.firstDayOfWeek = Calendar.MONDAY
    secondCalendar.firstDayOfWeek = Calendar.MONDAY

    firstCalendar.time = firstDate
    secondCalendar.time = secondDate

    return firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR) &&
            firstCalendar.get(Calendar.WEEK_OF_YEAR) == secondCalendar.get(Calendar.WEEK_OF_YEAR)
}

private fun isSameMonth(
    firstDate: Date,
    secondDate: Date
): Boolean {
    val firstCalendar = Calendar.getInstance()
    val secondCalendar = Calendar.getInstance()

    firstCalendar.time = firstDate
    secondCalendar.time = secondDate

    return firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR) &&
            firstCalendar.get(Calendar.MONTH) == secondCalendar.get(Calendar.MONTH)
}

private fun isSameYear(
    firstDate: Date,
    secondDate: Date
): Boolean {
    val firstCalendar = Calendar.getInstance()
    val secondCalendar = Calendar.getInstance()

    firstCalendar.time = firstDate
    secondCalendar.time = secondDate

    return firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR)
}

private fun buildInsightPeriodTitle(
    selectedPeriod: String,
    referenceDate: Date
): String {
    return when (selectedPeriod) {
        "Minggu" -> {
            "Ringkasan ${buildShortPeriodTitle(selectedPeriod, referenceDate)}"
        }

        "Tahun" -> {
            "Ringkasan ${buildShortPeriodTitle(selectedPeriod, referenceDate)}"
        }

        else -> {
            "Ringkasan ${buildShortPeriodTitle(selectedPeriod, referenceDate)}"
        }
    }
}

private fun buildShortPeriodTitle(
    selectedPeriod: String,
    referenceDate: Date
): String {
    val locale = Locale("id", "ID")

    return when (selectedPeriod) {
        "Minggu" -> {
            val calendar = Calendar.getInstance(locale)
            calendar.time = referenceDate
            calendar.firstDayOfWeek = Calendar.MONDAY

            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val startDate = calendar.time

            calendar.add(Calendar.DAY_OF_MONTH, 6)
            val endDate = calendar.time

            val formatter = SimpleDateFormat("dd MMM yyyy", locale)
            "${formatter.format(startDate)} - ${formatter.format(endDate)}"
        }

        "Tahun" -> {
            SimpleDateFormat("yyyy", locale).format(referenceDate)
        }

        else -> {
            SimpleDateFormat("MMMM yyyy", locale).format(referenceDate)
        }
    }
}
