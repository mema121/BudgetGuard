package com.azmisava.budgetguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azmisava.budgetguard.component.BudgetBottomNavigation
import com.azmisava.budgetguard.model.BudgetTab
import com.azmisava.budgetguard.screen.CreatePinScreen
import com.azmisava.budgetguard.screen.DashboardScreen
import com.azmisava.budgetguard.screen.InsightScreen
import com.azmisava.budgetguard.screen.PinScreen
import com.azmisava.budgetguard.screen.ProfileScreen
import com.azmisava.budgetguard.screen.RiwayatScreen
import com.azmisava.budgetguard.screen.TransactionScreen
import com.azmisava.budgetguard.screen.WelcomeScreen
import com.azmisava.budgetguard.ui.theme.BudgetGuardTheme
import com.azmisava.budgetguard.util.PrivacyState
import com.azmisava.budgetguard.viewmodel.SecurityViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BudgetGuardTheme {
                BudgetGuardApp()
            }
        }
    }
}

@Composable
fun BudgetGuardApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    val securityViewModel = remember { SecurityViewModel() }

    var isLoggedIn by remember {
        mutableStateOf(auth.currentUser != null)
    }

    var showWelcome by rememberSaveable {
        mutableStateOf(true)
    }

    var securityStep by rememberSaveable {
        mutableStateOf("checking")
    }

    val userId = auth.currentUser?.uid

    LaunchedEffect(isLoggedIn, userId) {
        if (isLoggedIn && userId != null) {
            securityStep = "checking"

            securityViewModel.checkPinExists(
                userId = userId,
                onResult = { hasPin ->
                    securityStep = if (hasPin) {
                        "verify_pin"
                    } else {
                        "create_pin"
                    }
                },
                onError = {
                    securityStep = "create_pin"
                }
            )
        } else {
            securityStep = "checking"
        }
    }

    if (showWelcome) {
        WelcomeScreen(
            onStart = {
                showWelcome = false
            }
        )
    } else if (!isLoggedIn) {
        AuthScreen(
            auth = auth,
            onLoginSuccess = {
                isLoggedIn = true
                securityStep = "checking"
            }
        )
    } else {
        when (securityStep) {
            "checking" -> {
                LoadingSecurityScreen()
            }

            "create_pin" -> {
                CreatePinScreen(
                    auth = auth,
                    onPinCreated = {
                        securityStep = "verified"
                    }
                )
            }

            "verify_pin" -> {
                PinScreen(
                    auth = auth,
                    onPinSuccess = {
                        securityStep = "verified"
                    },
                    onForgotPin = {
                        auth.signOut()
                        isLoggedIn = false
                        securityStep = "checking"
                        showWelcome = false
                    }
                )
            }

            else -> {
                MainAppScreen(
                    auth = auth,
                    onLogout = {
                        auth.signOut()
                        isLoggedIn = false
                        securityStep = "checking"
                        showWelcome = true
                    }
                )
            }
        }
    }
}

@Composable
fun LoadingSecurityScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoftLilac),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Memeriksa keamanan akun...",
            fontSize = 15.sp,
            color = TextMuted
        )
    }
}

@Composable
fun AuthScreen(
    auth: FirebaseAuth,
    onLoginSuccess: () -> Unit
) {
    var isLoginMode by rememberSaveable { mutableStateOf(true) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoftLilac)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "BudgetGuard",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isLoginMode) {
                        "Masuk untuk mengelola budget pribadi Anda"
                    } else {
                        "Buat akun baru untuk mulai menggunakan BudgetGuard"
                    },
                    fontSize = 14.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                    },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            message = "Email dan password tidak boleh kosong"
                            return@Button
                        }

                        if (isLoginMode) {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    message = "Login berhasil"
                                    onLoginSuccess()
                                }
                                .addOnFailureListener { error ->
                                    message = error.message ?: "Login gagal"
                                }
                        } else {
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    message = "Registrasi berhasil"
                                    onLoginSuccess()
                                }
                                .addOnFailureListener { error ->
                                    message = error.message ?: "Registrasi gagal"
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLavender),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = if (isLoginMode) "Masuk" else "Daftar",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        isLoginMode = !isLoginMode
                        message = ""
                    }
                ) {
                    Text(
                        text = if (isLoginMode) {
                            "Belum punya akun? Daftar"
                        } else {
                            "Sudah punya akun? Masuk"
                        },
                        color = PrimaryLavender
                    )
                }

                if (message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = message,
                        color = if (message.contains("berhasil", ignoreCase = true)) {
                            TextMain
                        } else {
                            DangerRed
                        },
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(
    auth: FirebaseAuth,
    onLogout: () -> Unit
) {
    val securityViewModel = remember { SecurityViewModel() }
    val userId = auth.currentUser?.uid

    var selectedTab by rememberSaveable {
        mutableStateOf(BudgetTab.Dashboard)
    }

    LaunchedEffect(userId) {
        if (userId != null) {
            securityViewModel.loadPrivacyMode(
                userId = userId,
                onResult = { enabled ->
                    PrivacyState.enabled = enabled
                },
                onError = {
                    PrivacyState.enabled = false
                }
            )
        }
    }

    Scaffold(
        containerColor = BackgroundSoftLilac,
        bottomBar = {
            BudgetBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundSoftLilac)
        ) {
            when (selectedTab) {
                BudgetTab.Dashboard -> {
                    DashboardScreen(
                        auth = auth,
                        onNavigateToTab = { tab ->
                            selectedTab = tab
                        }
                    )
                }

                BudgetTab.Transaksi -> {
                    TransactionScreen(
                        auth = auth,
                        onBack = {
                            selectedTab = BudgetTab.Dashboard
                        }
                    )
                }

                BudgetTab.Riwayat -> {
                    RiwayatScreen(
                        auth = auth
                    )
                }

                BudgetTab.Insight -> {
                    InsightScreen(
                        auth = auth
                    )
                }

                BudgetTab.Profil -> {
                    ProfileScreen(
                        auth = auth,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
