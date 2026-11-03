package com.azmisava.budgetguard.screen

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.azmisava.budgetguard.util.PrivacyState
import com.azmisava.budgetguard.util.InputValidationUtil
import com.azmisava.budgetguard.util.formatMoney
import com.azmisava.budgetguard.viewmodel.SecurityViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.ByteArrayOutputStream
import java.util.Calendar

@Composable
fun ProfileScreen(
    auth: FirebaseAuth,
    onLogout: () -> Unit = {}
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val securityViewModel = remember { SecurityViewModel() }
    val userId = auth.currentUser?.uid
    val context = LocalContext.current
    val userEmail = auth.currentUser?.email ?: "Akun Pribadi"

    var fullName by rememberSaveable { mutableStateOf("Pengguna BudgetGuard") }
    var birthDate by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var profileImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var profileImageBase64 by rememberSaveable { mutableStateOf("") }

    var monthlyLimit by rememberSaveable { mutableStateOf(0.0) }
    var privacyMode by rememberSaveable { mutableStateOf(false) }
    var pinSaved by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }

    var showLimitDialog by rememberSaveable { mutableStateOf(false) }
    var showPinDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showEditProfilePage by rememberSaveable { mutableStateOf(false) }
    var showPhotoSourceDialog by rememberSaveable { mutableStateOf(false) }

    var limitInput by rememberSaveable { mutableStateOf("") }
    var pinInput by rememberSaveable { mutableStateOf("") }

    fun savePersonalProfile(
        onSuccess: () -> Unit = {}
    ) {
        if (userId == null) {
            message = "User belum login"
            return
        }

        if (fullName.isBlank()) {
            message = "Nama tidak boleh kosong"
            return
        }

        if (fullName.length > InputValidationUtil.MAX_PROFILE_NAME_LENGTH) {
            message = "Nama maksimal ${InputValidationUtil.MAX_PROFILE_NAME_LENGTH} karakter"
            return
        }

        db.collection("users")
            .document(userId)
            .collection("settings")
            .document("profile")
            .set(
                mapOf(
                    "fullName" to InputValidationUtil.sanitizeProfileName(fullName).trim(),
                    "birthDate" to birthDate,
                    "gender" to gender,
                    "profileImageUri" to (profileImageUri ?: ""),
                    "profileImageBase64" to profileImageBase64,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                message = "Profil berhasil diperbarui"
                onSuccess()
            }
            .addOnFailureListener {
                message = it.message ?: "Gagal menyimpan profil"
            }
    }

    fun setProfilePhotoFromBitmap(bitmap: Bitmap) {
        val resizedBitmap = resizeBitmap(bitmap, 512)
        profileImageBase64 = bitmapToBase64(resizedBitmap)
        profileImageUri = null
        savePersonalProfile()
    }

    fun setProfilePhotoFromUri(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Beberapa device tidak memberi persistable permission.
        }

        val bitmap = loadBitmapFromUri(context, uri)
        if (bitmap != null) {
            val resizedBitmap = resizeBitmap(bitmap, 512)
            profileImageBase64 = bitmapToBase64(resizedBitmap)
            profileImageUri = uri.toString()
            savePersonalProfile()
        } else {
            message = "Gagal membaca foto"
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            setProfilePhotoFromUri(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            setProfilePhotoFromBitmap(bitmap)
        } else {
            message = "Foto tidak berhasil diambil"
        }
    }

    fun loadProfileSettings() {
        if (userId == null) {
            message = "User belum login"
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

        db.collection("users")
            .document(userId)
            .collection("settings")
            .document("security")
            .get()
            .addOnSuccessListener { document ->
                privacyMode = document.getBoolean("privacyMode") ?: false
                PrivacyState.enabled = privacyMode
                val pinHash = document.getString("pinHash")
                val legacyPin = document.getString("pin")
                pinSaved = !pinHash.isNullOrBlank() || !legacyPin.isNullOrBlank()
            }

        db.collection("users")
            .document(userId)
            .collection("settings")
            .document("profile")
            .get()
            .addOnSuccessListener { document ->
                fullName = document.getString("fullName")?.ifBlank { "Pengguna BudgetGuard" }
                    ?: "Pengguna BudgetGuard"
                birthDate = document.getString("birthDate") ?: ""
                gender = document.getString("gender") ?: ""
                profileImageUri = document.getString("profileImageUri")?.ifBlank { null }
                profileImageBase64 = document.getString("profileImageBase64") ?: ""
            }
    }

    fun saveLimit() {
        if (userId == null) {
            message = "User belum login"
            return
        }

        val value = limitInput.toDoubleOrNull()
        if (value == null || value <= 0) {
            message = "Limit harus angka lebih dari 0"
            return
        }

        db.collection("users")
            .document(userId)
            .collection("settings")
            .document("budget")
            .set(mapOf("monthlyLimit" to value), SetOptions.merge())
            .addOnSuccessListener {
                monthlyLimit = value
                showLimitDialog = false
                limitInput = ""
                message = "Limit berhasil diperbarui"
            }
            .addOnFailureListener {
                message = it.message ?: "Gagal menyimpan limit"
            }
    }

    fun saveSecuritySetting(newPrivacy: Boolean = privacyMode, newPin: String? = null) {
        if (userId == null) {
            message = "User belum login"
            return
        }

        if (newPin != null) {
            securityViewModel.savePin(
                userId = userId,
                pin = newPin,
                onSuccess = {
                    pinSaved = true
                    message = "PIN berhasil diperbarui"
                },
                onError = { errorMessage ->
                    message = errorMessage
                }
            )
            return
        }

        db.collection("users")
            .document(userId)
            .collection("settings")
            .document("security")
            .set(
                mapOf(
                    "privacyMode" to newPrivacy,
                    "pin" to FieldValue.delete()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                message = "Pengaturan berhasil disimpan"
            }
            .addOnFailureListener {
                message = it.message ?: "Gagal menyimpan pengaturan"
            }
    }

    fun savePin() {
        if (pinInput.length != 4) {
            message = "PIN harus 4 digit"
            return
        }

        pinSaved = true
        saveSecuritySetting(newPin = pinInput)
        showPinDialog = false
        pinInput = ""
    }

    LaunchedEffect(userId) {
        loadProfileSettings()
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            confirmButton = {},
            title = {
                Text("Ganti Foto Profil", color = TextMain)
            },
            text = {
                Column {
                    Button(
                        onClick = {
                            showPhotoSourceDialog = false
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryLavender)
                    ) {
                        Text("Ambil dari Kamera", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            showPhotoSourceDialog = false
                            galleryLauncher.launch(arrayOf("image/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0EAF8)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Pilih dari Galeri", color = PrimaryLavender)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            confirmButton = {
                Button(
                    onClick = { saveLimit() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLavender)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            },
            title = { Text("Ubah Limit Bulanan", color = TextMain) },
            text = {
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Limit Baru") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            confirmButton = {
                Button(
                    onClick = { savePin() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLavender)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Batal", color = TextMuted)
                }
            },
            title = { Text("Ubah PIN", color = TextMain) },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("PIN 4 Digit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Tutup", color = PrimaryLavender)
                }
            },
            title = { Text("Tentang Aplikasi", color = TextMain) },
            text = {
                Text(
                    text = "BudgetGuard adalah aplikasi pengendalian anggaran pribadi untuk membantu mencatat pemasukan, pengeluaran, limit bulanan, dan insight keuangan.",
                    color = TextMuted,
                    lineHeight = 20.sp
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showEditProfilePage) {
        EditProfilePage(
            fullName = fullName,
            onFullNameChange = { fullName = InputValidationUtil.sanitizeProfileName(it) },
            email = userEmail,
            birthDate = birthDate,
            onBirthDateChange = { birthDate = it },
            gender = gender,
            onGenderChange = { gender = it },
            profileImageBase64 = profileImageBase64,
            profileImageUri = profileImageUri,
            onBack = {
                showEditProfilePage = false
            },
            onPhotoClick = {
                showPhotoSourceDialog = true
            },
            onSave = {
                savePersonalProfile(
                    onSuccess = {
                        showEditProfilePage = false
                    }
                )
            },
            message = message
        )
    } else {
        ProfileMainPage(
            fullName = fullName,
            email = userEmail,
            birthDate = birthDate,
            gender = gender,
            profileImageBase64 = profileImageBase64,
            profileImageUri = profileImageUri,
            monthlyLimit = monthlyLimit,
            message = message,
            onEditProfile = {
                showEditProfilePage = true
            },
            onChangeLimit = {
                limitInput = if (monthlyLimit > 0) monthlyLimit.toLong().toString() else ""
                showLimitDialog = true
            },
            onPrivacyChange = {
                privacyMode = it
                PrivacyState.enabled = it
                saveSecuritySetting(newPrivacy = it)
            },
            onShowPinDialog = {
                showPinDialog = true
            },
            onShowAboutDialog = {
                showAboutDialog = true
            },
            onLogoutClick = {
                auth.signOut()
                onLogout()
            }
        )
    }
}

@Composable
private fun ProfileMainPage(
    fullName: String,
    email: String,
    birthDate: String,
    gender: String,
    profileImageBase64: String,
    profileImageUri: String?,
    monthlyLimit: Double,
    message: String,
    onEditProfile: () -> Unit,
    onChangeLimit: () -> Unit,
    onPrivacyChange: (Boolean) -> Unit,
    onShowPinDialog: () -> Unit,
    onShowAboutDialog: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoftLilac)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Profil",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditProfile() },
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(
                        profileImageBase64 = profileImageBase64,
                        profileImageUri = profileImageUri,
                        size = 56,
                        onClick = onEditProfile
                    )

                    Spacer(modifier = Modifier.padding(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fullName.ifBlank { "Pengguna BudgetGuard" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        Text(
                            text = if (email.isBlank()) "Akun Pribadi" else email,
                            fontSize = 14.sp,
                            color = TextMuted
                        )

                        if (birthDate.isNotBlank() || gender.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = listOf(birthDate, gender).filter { it.isNotBlank() }
                                    .joinToString(" • "),
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Text(
                        text = "›",
                        fontSize = 22.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF2FBF4), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛡",
                        fontSize = 16.sp,
                        color = SuccessGreen
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = "Data keuangan Anda terlindungi",
                        fontSize = 14.sp,
                        color = SuccessGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Limit Bulanan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMain
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (monthlyLimit > 0) formatMoney(monthlyLimit, false) else "Belum diatur",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }

                Button(
                    onClick = onChangeLimit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0EAF8)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Ubah",
                        color = PrimaryLavender,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                SettingRow(
                    icon = "♙",
                    title = "Edit Profil",
                    trailingText = "›",
                    onClick = onEditProfile
                )

                DividerLight()

                SettingRowWithSwitch(
                    icon = "◉",
                    title = "Mode Privasi",
                    checked = PrivacyState.enabled,
                    onCheckedChange = onPrivacyChange
                )

                DividerLight()

                SettingRow(
                    icon = "⎔",
                    title = "Ubah PIN",
                    trailingText = "›",
                    onClick = onShowPinDialog
                )

                DividerLight()

                SettingRowStatus(
                    icon = "□",
                    title = "Perlindungan Data",
                    statusText = "Aktif"
                )

                DividerLight()

                SettingRow(
                    icon = "i",
                    title = "Tentang Aplikasi",
                    trailingText = "›",
                    onClick = onShowAboutDialog
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = "⎋  Logout",
                fontSize = 16.sp,
                color = DangerRed,
                fontWeight = FontWeight.Medium
            )
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = if (message.contains("berhasil", ignoreCase = true)) SuccessGreen else DangerRed
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EditProfilePage(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    birthDate: String,
    onBirthDateChange: (String) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    profileImageBase64: String,
    profileImageUri: String?,
    onBack: () -> Unit,
    onPhotoClick: () -> Unit,
    onSave: () -> Unit,
    message: String
) {
    val context = LocalContext.current

    fun showDatePicker() {
        val calendar = Calendar.getInstance()

        if (birthDate.isNotBlank()) {
            try {
                val parts = birthDate.split("/")
                if (parts.size == 3) {
                    calendar.set(
                        parts[2].toInt(),
                        parts[1].toInt() - 1,
                        parts[0].toInt()
                    )
                }
            } catch (_: Exception) {
                // Jika format tanggal tidak valid, gunakan tanggal hari ini.
            }
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val dateText = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
                onBirthDateChange(dateText)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoftLilac)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryLavender,
                modifier = Modifier.clickable { onBack() }
            )

            Spacer(modifier = Modifier.padding(6.dp))

            Text(
                text = "Edit Profil",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileAvatar(
                    profileImageBase64 = profileImageBase64,
                    profileImageUri = profileImageUri,
                    size = 92,
                    onClick = onPhotoClick
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Ganti Foto Profil",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryLavender,
                    modifier = Modifier.clickable { onPhotoClick() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    label = { Text("Email") },
                    singleLine = true,
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ProfileDateField(
                    birthDate = birthDate,
                    onClick = { showDatePicker() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Jenis Kelamin",
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileChoiceChip(
                        text = "Perempuan",
                        selected = gender == "Perempuan",
                        modifier = Modifier.weight(1f),
                        onClick = { onGenderChange("Perempuan") }
                    )

                    ProfileChoiceChip(
                        text = "Laki-laki",
                        selected = gender == "Laki-laki",
                        modifier = Modifier.weight(1f),
                        onClick = { onGenderChange("Laki-laki") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfileChoiceChip(
                        text = "Lainnya",
                        selected = gender == "Lainnya",
                        modifier = Modifier.weight(1f),
                        onClick = { onGenderChange("Lainnya") }
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0EAF8)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Batal", color = TextMuted)
                    }

                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryLavender),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Simpan", color = Color.White)
                    }
                }
            }
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = if (message.contains("berhasil", ignoreCase = true)) SuccessGreen else DangerRed
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileDateField(
    birthDate: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(Color.White, RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        OutlinedTextField(
            value = birthDate.ifBlank { "Pilih tanggal lahir" },
            onValueChange = {},
            label = { Text("Tanggal Lahir") },
            singleLine = true,
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProfileChoiceChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) PrimaryLavender else Color(0xFFF8F4FF)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (selected) Color.White else TextMuted,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProfileAvatar(
    profileImageBase64: String,
    profileImageUri: String?,
    size: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val imageBitmap = remember(profileImageBase64, profileImageUri) {
        decodeProfileImage(
            context = context,
            base64Text = profileImageBase64,
            uriText = profileImageUri
        )
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(Color(0xFFF3EDFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Foto Profil",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "♙",
                fontSize = (size / 2.5).sp,
                color = PrimaryLavender
            )
        }
    }
}

@Composable
private fun SettingRow(
    icon: String,
    title: String,
    trailingText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon)
        Spacer(modifier = Modifier.padding(6.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = TextMain,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = trailingText,
            fontSize = 18.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun SettingRowStatus(
    icon: String,
    title: String,
    statusText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon)
        Spacer(modifier = Modifier.padding(6.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = TextMain,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .background(Color(0xFFF2FBF4), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = statusText,
                fontSize = 13.sp,
                color = SuccessGreen,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SettingRowWithSwitch(
    icon: String,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon)
        Spacer(modifier = Modifier.padding(6.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = TextMain,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryLavender,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE6DEF3),
                uncheckedBorderColor = Color.Transparent,
                checkedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun IconBadge(icon: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color(0xFFF3EDFF), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 18.sp,
            color = PrimaryLavender
        )
    }
}

@Composable
private fun DividerLight() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFF0EAF8))
    )
}

private fun decodeProfileImage(
    context: Context,
    base64Text: String,
    uriText: String?
): ImageBitmap? {
    try {
        if (base64Text.isNotBlank()) {
            val bytes = Base64.decode(base64Text, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            return bitmap?.asImageBitmap()
        }

        if (!uriText.isNullOrBlank()) {
            val bitmap = loadBitmapFromUri(context, Uri.parse(uriText))
            return bitmap?.asImageBitmap()
        }
    } catch (_: Exception) {
        return null
    }

    return null
}

private fun loadBitmapFromUri(
    context: Context,
    uri: Uri
): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (_: Exception) {
        null
    }
}

private fun resizeBitmap(
    bitmap: Bitmap,
    maxSize: Int
): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    val largestSide = maxOf(width, height)
    if (largestSide <= maxSize) {
        return bitmap
    }

    val scale = maxSize.toFloat() / largestSide.toFloat()
    val newWidth = (width * scale).toInt().coerceAtLeast(1)
    val newHeight = (height * scale).toInt().coerceAtLeast(1)

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

private fun bitmapToBase64(
    bitmap: Bitmap
): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
}
