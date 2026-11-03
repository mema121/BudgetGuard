package com.azmisava.budgetguard.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azmisava.budgetguard.BackgroundSoftLilac
import com.azmisava.budgetguard.DangerRed
import com.azmisava.budgetguard.PrimaryLavender
import com.azmisava.budgetguard.SuccessGreen
import com.azmisava.budgetguard.TextMain
import com.azmisava.budgetguard.TextMuted
import com.azmisava.budgetguard.WarningYellow
import com.azmisava.budgetguard.model.BudgetTab
import com.azmisava.budgetguard.model.TransactionItem
import com.azmisava.budgetguard.util.formatMoney
import com.azmisava.budgetguard.util.PrivacyState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun DashboardScreen(
    auth: FirebaseAuth,
    onNavigateToTab: (BudgetTab) -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val userId = auth.currentUser?.uid

    var transactions by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }
    var monthlyLimit by rememberSaveable { mutableStateOf(0.0) }
    val privacyMode = PrivacyState.enabled
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
                    TransactionItem(
                        id = document.id,
                        type = document.getString("type") ?: "",
                        amount = document.getDouble("amount") ?: 0.0,
                        category = document.getString("category") ?: "",
                        note = document.getString("note") ?: "",
                        createdAt = document.getLong("createdAt") ?: 0L
                    )
                }.sortedByDescending {
                    it.createdAt
                }

                message = ""
            }
            .addOnFailureListener { error ->
                message = error.message ?: "Gagal mengambil transaksi"
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

    val totalIncome = transactions
        .filter { it.type == "income" }
        .sumOf { it.amount }

    val totalExpense = transactions
        .filter { it.type == "expense" }
        .sumOf { it.amount }

    val balance = totalIncome - totalExpense

    val limitPercentage = if (monthlyLimit > 0) {
        (totalExpense / monthlyLimit * 100).coerceAtMost(100.0)
    } else {
        0.0
    }

    val moodText = when {
        monthlyLimit <= 0 -> "Belum Ada Limit"
        limitPercentage < 50 -> "Sangat Hemat"
        limitPercentage < 70 -> "Hemat"
        limitPercentage < 100 -> "Waspada"
        else -> "Melebihi Limit"
    }

    val moodColor = when (moodText) {
        "Sangat Hemat", "Hemat" -> SuccessGreen
        "Waspada" -> WarningYellow
        "Melebihi Limit" -> DangerRed
        else -> TextMuted
    }

    val budgetScore = when {
        monthlyLimit <= 0 -> 0
        limitPercentage < 50 -> 100
        limitPercentage < 70 -> 80
        limitPercentage < 100 -> 60
        else -> 35
    }

    val scoreLabel = when {
        monthlyLimit <= 0 -> "Belum Dinilai"
        budgetScore >= 90 -> "Hemat"
        budgetScore >= 70 -> "Cukup"
        budgetScore >= 50 -> "Waspada"
        else -> "Boros"
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

            DashboardHeader(
                privacyMode = privacyMode,
                onPrivacyClick = {
                    PrivacyState.enabled = !PrivacyState.enabled
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            BalanceSummaryCard(
                balance = balance,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                moodText = moodText,
                moodColor = moodColor,
                privacyMode = privacyMode
            )

            Spacer(modifier = Modifier.height(14.dp))

            LimitSummaryCard(
                totalExpense = totalExpense,
                monthlyLimit = monthlyLimit,
                limitPercentage = limitPercentage,
                privacyMode = privacyMode
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BudgetMoodSmallCard(
                    modifier = Modifier.weight(1f),
                    moodText = moodText,
                    moodColor = moodColor
                )

                HealthScoreSmallCard(
                    modifier = Modifier.weight(1f),
                    score = budgetScore,
                    scoreLabel = scoreLabel,
                    scoreColor = moodColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            QuickActionCard(
                onNavigateToTab = onNavigateToTab
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Transaksi Terbaru",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(10.dp))

            RecentTransactionCard(
                transactions = transactions.take(5),
                privacyMode = privacyMode
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

@Composable
fun DashboardHeader(
    privacyMode: Boolean,
    onPrivacyClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Selamat datang",
                fontSize = 14.sp,
                color = TextMuted
            )

            Text(
                text = "BudgetGuard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            TextButton(
                onClick = {
                    onPrivacyClick()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    text = if (privacyMode) "🙈" else "👁",
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun BalanceSummaryCard(
    balance: Double,
    totalIncome: Double,
    totalExpense: Double,
    moodText: String,
    moodColor: Color,
    privacyMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Sisa Saldo",
                fontSize = 14.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatMoney(balance, privacyMode),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0FFF4)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = moodText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = moodColor
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IncomeExpenseMiniBox(
                    modifier = Modifier.weight(1f),
                    icon = "↓",
                    title = "Pemasukan",
                    amount = totalIncome,
                    privacyMode = privacyMode,
                    iconBackground = Color(0xFFEFFFF4),
                    iconColor = SuccessGreen
                )

                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .padding(horizontal = 8.dp)
                        .background(Color(0xFFEDE7F6))
                )

                IncomeExpenseMiniBox(
                    modifier = Modifier.weight(1f),
                    icon = "↑",
                    title = "Pengeluaran",
                    amount = totalExpense,
                    privacyMode = privacyMode,
                    iconBackground = Color(0xFFFFF0F0),
                    iconColor = DangerRed
                )
            }
        }
    }
}

@Composable
fun IncomeExpenseMiniBox(
    modifier: Modifier,
    icon: String,
    title: String,
    amount: Double,
    privacyMode: Boolean,
    iconBackground: Color,
    iconColor: Color
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = iconBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier.size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 18.sp,
                    color = iconColor
                )
            }
        }

        Spacer(modifier = Modifier.padding(6.dp))

        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextMuted
            )

            Text(
                text = formatMoney(amount, privacyMode),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
        }
    }
}

@Composable
fun LimitSummaryCard(
    totalExpense: Double,
    monthlyLimit: Double,
    limitPercentage: Double,
    privacyMode: Boolean
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Limit Bulanan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Text(
                    text = "${limitPercentage.toInt()}%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryLavender
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = {
                    (limitPercentage / 100).toFloat()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = PrimaryLavender,
                trackColor = Color(0xFFF0EAF8)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Terpakai ${formatMoney(totalExpense, privacyMode)}",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Text(
                    text = if (monthlyLimit > 0) {
                        "Limit ${formatMoney(monthlyLimit, privacyMode)}"
                    } else {
                        "Limit belum diatur"
                    },
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun BudgetMoodSmallCard(
    modifier: Modifier,
    moodText: String,
    moodColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "☺",
                fontSize = 30.sp,
                color = SuccessGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Budget Mood",
                fontSize = 13.sp,
                color = TextMuted
            )

            Text(
                text = moodText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = moodColor
            )
        }
    }
}

@Composable
fun HealthScoreSmallCard(
    modifier: Modifier,
    score: Int,
    scoreLabel: String,
    scoreColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "$score",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Text(
                text = "Skor Kesehatan",
                fontSize = 13.sp,
                color = TextMuted
            )

            Text(
                text = scoreLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
        }
    }
}

@Composable
fun QuickActionCard(
    onNavigateToTab: (BudgetTab) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionItem(
                icon = "+",
                label = "Tambah",
                onClick = {
                    onNavigateToTab(BudgetTab.Transaksi)
                }
            )

            QuickActionItem(
                icon = "◷",
                label = "Riwayat",
                onClick = {
                    onNavigateToTab(BudgetTab.Riwayat)
                }
            )

            QuickActionItem(
                icon = "◔",
                label = "Insight",
                onClick = {
                    onNavigateToTab(BudgetTab.Insight)
                }
            )

            QuickActionItem(
                icon = "♙",
                label = "Profil",
                onClick = {
                    onNavigateToTab(BudgetTab.Profil)
                }
            )
        }
    }
}

@Composable
fun QuickActionItem(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = {
            onClick()
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3EDFF)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 22.sp,
                        color = PrimaryLavender
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RecentTransactionCard(
    transactions: List<TransactionItem>,
    privacyMode: Boolean
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
            if (transactions.isEmpty()) {
                Text(
                    text = "Belum ada transaksi terbaru",
                    fontSize = 14.sp,
                    color = TextMuted
                )
            } else {
                transactions.forEachIndexed { index, transaction ->
                    DashboardRecentTransactionRow(
                        transaction = transaction,
                        privacyMode = privacyMode
                    )

                    if (index != transactions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Color(0xFFF0EAF8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardRecentTransactionRow(
    transaction: TransactionItem,
    privacyMode: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = transaction.category,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            if (transaction.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = transaction.note,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Text(
            text = if (transaction.type == "income") {
                "+${formatMoney(transaction.amount, privacyMode)}"
            } else {
                "-${formatMoney(transaction.amount, privacyMode)}"
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (transaction.type == "income") SuccessGreen else DangerRed
        )
    }
}
