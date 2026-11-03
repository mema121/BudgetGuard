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
import com.azmisava.budgetguard.TextMain
import com.azmisava.budgetguard.TextMuted
import com.google.firebase.auth.FirebaseAuth
import com.azmisava.budgetguard.viewmodel.SecurityViewModel
import kotlinx.coroutines.delay

@Composable
fun PinScreen(
    auth: FirebaseAuth,
    onPinSuccess: () -> Unit,
    onForgotPin: () -> Unit
) {
    val securityViewModel = remember { SecurityViewModel() }
    val userId = auth.currentUser?.uid

    var inputPin by rememberSaveable { mutableStateOf("") }
    var hasPin by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var message by rememberSaveable { mutableStateOf("") }
    var failedAttempts by rememberSaveable { mutableStateOf(0) }
    var lockedUntilMillis by rememberSaveable { mutableStateOf(0L) }
    var currentTimeMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var isVerifying by rememberSaveable { mutableStateOf(false) }

    val isLocked = lockedUntilMillis > currentTimeMillis
    val remainingLockSeconds = if (isLocked) {
        ((lockedUntilMillis - currentTimeMillis) / 1000L).coerceAtLeast(1L)
    } else {
        0L
    }

    LaunchedEffect(lockedUntilMillis) {
        while (lockedUntilMillis > System.currentTimeMillis()) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000)
        }
        currentTimeMillis = System.currentTimeMillis()
    }

    LaunchedEffect(userId) {
        securityViewModel.checkPinExists(
            userId = userId,
            onResult = { result ->
                hasPin = result
                isLoading = false

                if (!result) {
                    message = "PIN belum dibuat"
                } else {
                    securityViewModel.loadPinLockState(
                        userId = userId,
                        onResult = { lockState ->
                            failedAttempts = lockState.failedAttempts
                            lockedUntilMillis = lockState.lockedUntil
                            currentTimeMillis = System.currentTimeMillis()
                        },
                        onError = { errorMessage ->
                            message = errorMessage
                        }
                    )
                }
            },
            onError = { errorMessage ->
                isLoading = false
                message = errorMessage
            }
        )
    }

    fun addNumber(number: String) {
        if (!hasPin) {
            message = "PIN belum tersedia"
            return
        }

        if (isLocked) {
            message = "PIN terkunci sementara. Coba lagi dalam $remainingLockSeconds detik"
            return
        }

        if (isVerifying) {
            return
        }

        if (inputPin.length < 4) {
            inputPin += number
            message = ""
        }

        if (inputPin.length == 4) {
            isVerifying = true

            securityViewModel.verifyPin(
                userId = userId,
                inputPin = inputPin,
                onResult = { isCorrect ->
                    isVerifying = false

                    if (isCorrect) {
                        securityViewModel.resetFailedPinAttempts(userId = userId)
                        failedAttempts = 0
                        lockedUntilMillis = 0L
                        message = ""
                        onPinSuccess()
                    } else {
                        inputPin = ""

                        securityViewModel.recordFailedPinAttempt(
                            userId = userId,
                            onResult = { lockState ->
                                failedAttempts = lockState.failedAttempts
                                lockedUntilMillis = lockState.lockedUntil
                                currentTimeMillis = System.currentTimeMillis()

                                val remainingAttempts = (5 - failedAttempts).coerceAtLeast(0)
                                message = if (lockState.lockedUntil > System.currentTimeMillis()) {
                                    "PIN salah 5 kali. Akses dikunci 30 detik."
                                } else {
                                    "PIN salah. Sisa percobaan: $remainingAttempts"
                                }
                            },
                            onError = { errorMessage ->
                                message = errorMessage
                            }
                        )
                    }
                },
                onError = { errorMessage ->
                    isVerifying = false
                    message = errorMessage
                    inputPin = ""
                }
            )
        }
    }

    fun deleteNumber() {
        if (inputPin.isNotEmpty()) {
            inputPin = inputPin.dropLast(1)
            message = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoftLilac)
            .padding(horizontal = 28.dp)
    ) {
        if (isLoading) {
            Text(
                text = "Memuat keamanan akun...",
                color = TextMuted,
                fontSize = 15.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
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
                    text = "Masuk",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isLocked) {
                        "Terlalu banyak percobaan. Tunggu $remainingLockSeconds detik"
                    } else {
                        "Masukkan PIN Anda"
                    },
                    fontSize = 15.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(36.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    repeat(4) { index ->
                        PinDot(
                            filled = index < inputPin.length
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

                PinKeypad(
                    enabled = !isLocked && !isVerifying,
                    onNumberClick = { number ->
                        addNumber(number)
                    },
                    onDeleteClick = {
                        deleteNumber()
                    }
                )

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "Lupa PIN?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryLavender,
                    modifier = Modifier.clickable {
                        onForgotPin()
                    }
                )
            }
        }
    }
}

@Composable
private fun PinDot(
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
private fun PinKeypad(
    enabled: Boolean,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(70.dp)) {
            PinNumber("1", enabled, onNumberClick)
            PinNumber("2", enabled, onNumberClick)
            PinNumber("3", enabled, onNumberClick)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(70.dp)) {
            PinNumber("4", enabled, onNumberClick)
            PinNumber("5", enabled, onNumberClick)
            PinNumber("6", enabled, onNumberClick)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(70.dp)) {
            PinNumber("7", enabled, onNumberClick)
            PinNumber("8", enabled, onNumberClick)
            PinNumber("9", enabled, onNumberClick)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(70.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp))

            PinNumber("0", enabled, onNumberClick)

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(enabled = enabled) {
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
private fun PinNumber(
    number: String,
    enabled: Boolean,
    onNumberClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(enabled = enabled) {
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