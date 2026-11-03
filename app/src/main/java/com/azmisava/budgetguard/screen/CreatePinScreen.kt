package com.azmisava.budgetguard.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
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
import com.azmisava.budgetguard.TextMain
import com.azmisava.budgetguard.TextMuted
import com.google.firebase.auth.FirebaseAuth
import com.azmisava.budgetguard.viewmodel.SecurityViewModel
import com.azmisava.budgetguard.util.PinHashUtil

@Composable
fun CreatePinScreen(
    auth: FirebaseAuth,
    onPinCreated: () -> Unit
) {
    val securityViewModel = remember { SecurityViewModel() }
    val userId = auth.currentUser?.uid

    var firstPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var step by rememberSaveable { mutableStateOf("create") }
    var message by rememberSaveable { mutableStateOf("") }

    val activePin = if (step == "create") firstPin else confirmPin

    fun addNumber(number: String) {
        if (activePin.length >= 4) {
            return
        }

        if (step == "create") {
            firstPin += number
            message = ""

            if (firstPin.length == 4) {
                if (PinHashUtil.isWeakPin(firstPin)) {
                    message = "PIN terlalu mudah ditebak. Hindari 1234, 0000, atau angka berurutan."
                    firstPin = ""
                    return
                }

                step = "confirm"
            }
        } else {
            confirmPin += number
            message = ""

            if (confirmPin.length == 4) {
                if (confirmPin == firstPin) {
                    if (userId == null) {
                        message = "User belum login"
                        return
                    }

                    securityViewModel.savePin(
                        userId = userId,
                        pin = confirmPin,
                        onSuccess = {
                            message = ""
                            onPinCreated()
                        },
                        onError = { errorMessage ->
                            message = errorMessage
                        }
                    )
                } else {
                    message = "Konfirmasi PIN tidak sama"
                    confirmPin = ""
                }
            }
        }
    }

    fun deleteNumber() {
        if (step == "create") {
            if (firstPin.isNotEmpty()) {
                firstPin = firstPin.dropLast(1)
            }
        } else {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            } else {
                step = "create"
                firstPin = ""
            }
        }

        message = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoftLilac)
            .padding(horizontal = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = Color(0xFFF0E8FF),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryLavender
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (step == "create") "Buat PIN" else "Konfirmasi PIN",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (step == "create") {
                    "Buat PIN 4 digit untuk keamanan akun Anda"
                } else {
                    "Masukkan ulang PIN yang sudah dibuat"
                },
                fontSize = 15.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                repeat(4) { index ->
                    CreatePinDot(
                        filled = index < activePin.length
                    )
                }
            }

            if (message.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    color = DangerRed,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            CreatePinKeypad(
                onNumberClick = { number ->
                    addNumber(number)
                },
                onDeleteClick = {
                    deleteNumber()
                }
            )
        }
    }
}

@Composable
private fun CreatePinDot(
    filled: Boolean
) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .background(
                color = if (filled) PrimaryLavender else Color(0xFFEAE3F5),
                shape = CircleShape
            )
    )
}

@Composable
private fun CreatePinKeypad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(70.dp)) {
            CreatePinNumber("1", onNumberClick)
            CreatePinNumber("2", onNumberClick)
            CreatePinNumber("3", onNumberClick)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(70.dp)) {
            CreatePinNumber("4", onNumberClick)
            CreatePinNumber("5", onNumberClick)
            CreatePinNumber("6", onNumberClick)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(70.dp)) {
            CreatePinNumber("7", onNumberClick)
            CreatePinNumber("8", onNumberClick)
            CreatePinNumber("9", onNumberClick)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(70.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp))

            CreatePinNumber("0", onNumberClick)

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        onDeleteClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌫",
                    fontSize = 24.sp,
                    color = TextMain,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CreatePinNumber(
    number: String,
    onNumberClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable {
                onNumberClick(number)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            color = TextMain
        )
    }
}
