package com.azmisava.budgetguard.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.azmisava.budgetguard.TextMain
import com.azmisava.budgetguard.TextMuted
import com.azmisava.budgetguard.util.InputValidationUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TransactionScreen(
    auth: FirebaseAuth,
    onBack: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val userId = auth.currentUser?.uid
    val context = LocalContext.current

    var selectedType by rememberSaveable { mutableStateOf("income") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("") }
    var selectedReason by rememberSaveable { mutableStateOf("") }
    var selectedDate by rememberSaveable { mutableStateOf(getTodayDateText()) }
    var note by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }

    val incomeCategories = listOf(
        "Gaji",
        "Bonus",
        "Freelance",
        "Investasi",
        "Lainnya"
    )

    val expenseCategories = listOf(
        "Makanan & Minuman",
        "Transportasi",
        "Belanja",
        "Hiburan",
        "Tagihan",
        "Kesehatan",
        "Pendidikan",
        "Lainnya"
    )

    val expenseReasons = listOf(
        "Kebutuhan",
        "Keinginan",
        "Darurat",
        "Hiburan"
    )

    val categories = if (selectedType == "income") {
        incomeCategories
    } else {
        expenseCategories
    }

    fun showDatePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)

                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
                selectedDate = formatter.format(selectedCalendar.time)
                message = ""
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoftLilac)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape)
                        .clickable {
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "←",
                        fontSize = 22.sp,
                        color = TextMain,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.padding(7.dp))

                Text(
                    text = "Tambah Transaksi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TransactionTypeSegment(
                selectedType = selectedType,
                onTypeSelected = { type ->
                    selectedType = type
                    selectedCategory = ""
                    selectedReason = ""
                    message = ""
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            FormLabel(text = "Nominal")

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it.filter { char -> char.isDigit() }
                    message = ""
                },
                placeholder = {
                    Text(
                        text = "Rp 0",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB8B1C7)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                shape = RoundedCornerShape(18.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            FormLabel(text = "Kategori")

            Spacer(modifier = Modifier.height(10.dp))

            CategoryDropdownField(
                selectedCategory = selectedCategory,
                categories = categories,
                onCategorySelected = { category ->
                    selectedCategory = category
                    message = ""
                }
            )

            if (selectedType == "expense") {
                Spacer(modifier = Modifier.height(22.dp))

                FormLabel(text = "Alasan Pengeluaran")

                Spacer(modifier = Modifier.height(10.dp))

                ExpenseReasonSelector(
                    selectedReason = selectedReason,
                    reasons = expenseReasons,
                    onReasonSelected = { reason ->
                        selectedReason = reason
                        message = ""
                    }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            FormLabel(text = "Tanggal")

            Spacer(modifier = Modifier.height(10.dp))

            DateField(
                selectedDate = selectedDate,
                onClick = {
                    showDatePicker()
                }
            )

            Spacer(modifier = Modifier.height(22.dp))

            FormLabel(text = "Catatan (Opsional)")

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = note,
                onValueChange = {
                    note = InputValidationUtil.sanitizeNote(it)
                    message = ""
                },
                placeholder = {
                    Text(
                        text = "Tulis catatan di sini...",
                        color = Color(0xFFB8B1C7)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp),
                shape = RoundedCornerShape(18.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (userId == null) {
                        message = "User belum login"
                        return@Button
                    }

                    val amount = amountText.toDoubleOrNull()

                    if (!InputValidationUtil.isValidAmount(amount)) {
                        message = "Nominal harus diisi dan maksimal Rp1.000.000.000"
                        return@Button
                    }

                    if (selectedCategory.isBlank()) {
                        message = "Kategori harus dipilih"
                        return@Button
                    }

                    if (selectedType == "expense" && selectedReason.isBlank()) {
                        message = "Alasan pengeluaran harus dipilih"
                        return@Button
                    }

                    val transactionData = mapOf(
                        "type" to selectedType,
                        "amount" to amount,
                        "category" to selectedCategory,
                        "note" to InputValidationUtil.sanitizeNote(note),
                        "reason" to if (selectedType == "expense") selectedReason else "",
                        "selectedDate" to selectedDate,
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("users")
                        .document(userId)
                        .collection("transactions")
                        .add(transactionData)
                        .addOnSuccessListener {
                            amountText = ""
                            selectedCategory = ""
                            selectedReason = ""
                            selectedDate = getTodayDateText()
                            note = ""
                            message = "Transaksi berhasil disimpan"
                        }
                        .addOnFailureListener { error ->
                            message = error.message ?: "Gagal menyimpan transaksi"
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryLavender
                )
            ) {
                Text(
                    text = "Simpan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = if (message.contains("berhasil", ignoreCase = true)) {
                        PrimaryLavender
                    } else {
                        DangerRed
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TransactionTypeSegment(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = {
                    onTypeSelected("income")
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "income") {
                        PrimaryLavender
                    } else {
                        Color.Transparent
                    }
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = "Pemasukan",
                    color = if (selectedType == "income") Color.White else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    onTypeSelected("expense")
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "expense") {
                        PrimaryLavender
                    } else {
                        Color.Transparent
                    }
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = "Pengeluaran",
                    color = if (selectedType == "expense") Color.White else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FormLabel(
    text: String
) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = TextMain
    )
}

@Composable
private fun CategoryDropdownField(
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable {
                    expanded = true
                },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFFE0D0FF)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory.isBlank()) "Pilih Kategori" else selectedCategory,
                    fontSize = 16.sp,
                    color = if (selectedCategory.isBlank()) TextMain else TextMain,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "⌄",
                    fontSize = 18.sp,
                    color = TextMuted
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = category,
                            color = TextMain,
                            fontSize = 15.sp
                        )
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ExpenseReasonSelector(
    selectedReason: String,
    reasons: List<String>,
    onReasonSelected: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        reasons.chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { reason ->
                    Button(
                        onClick = {
                            onReasonSelected(reason)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedReason == reason) {
                                PrimaryLavender
                            } else {
                                Color.White
                            }
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selectedReason == reason) {
                                PrimaryLavender
                            } else {
                                Color(0xFFE0D0FF)
                            }
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = reason,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedReason == reason) Color.White else TextMain
                        )
                    }
                }

                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DateField(
    selectedDate: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFE0D0FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDate,
                fontSize = 16.sp,
                color = TextMain,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "▣",
                fontSize = 18.sp,
                color = TextMain
            )
        }
    }
}

private fun getTodayDateText(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
    return formatter.format(Calendar.getInstance().time)
}
