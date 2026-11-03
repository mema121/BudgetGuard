package com.azmisava.budgetguard.repository

import com.azmisava.budgetguard.util.PinHashUtil
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SecurityRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    data class PinLockState(
        val failedAttempts: Int = 0,
        val lockedUntil: Long = 0L
    )

    companion object {
        const val MAX_PIN_ATTEMPTS = 5
        const val PIN_LOCK_DURATION_MS = 30_000L
    }

    private fun securityDocument(userId: String) = db.collection("users")
        .document(userId)
        .collection("settings")
        .document("security")

    fun checkPinExists(
        userId: String?,
        onResult: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        if (userId == null) {
            onError("User belum login")
            return
        }

        securityDocument(userId)
            .get()
            .addOnSuccessListener { document ->
                val pinHash = document.getString("pinHash")
                val legacyPin = document.getString("pin")
                onResult(!pinHash.isNullOrBlank() || !legacyPin.isNullOrBlank())
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Gagal memeriksa PIN")
            }
    }

    fun savePin(
        userId: String?,
        pin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (userId == null) {
            onError("User belum login")
            return
        }

        if (!PinHashUtil.isValidPin(pin)) {
            onError("PIN harus 4 digit angka")
            return
        }

        if (PinHashUtil.isWeakPin(pin)) {
            onError("PIN terlalu mudah ditebak. Hindari 1234, 0000, atau angka berurutan.")
            return
        }

        val salt = PinHashUtil.generateSalt()
        val pinHash = PinHashUtil.hashPin(pin, salt)

        val data = mapOf(
            "pinHash" to pinHash,
            "pinSalt" to salt,
            "pinAlgorithm" to PinHashUtil.PIN_HASH_ALGORITHM,
            "pinUpdatedAt" to System.currentTimeMillis(),
            "failedPinAttempts" to 0,
            "pinLockedUntil" to 0L,
            "pin" to FieldValue.delete()
        )

        securityDocument(userId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Gagal menyimpan PIN")
            }
    }

    fun verifyPin(
        userId: String?,
        inputPin: String,
        onResult: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        if (userId == null) {
            onError("User belum login")
            return
        }

        securityDocument(userId)
            .get()
            .addOnSuccessListener { document ->
                val lockedUntil = document.getLong("pinLockedUntil") ?: 0L
                if (lockedUntil > System.currentTimeMillis()) {
                    onError("PIN terkunci sementara")
                    return@addOnSuccessListener
                }

                val pinHash = document.getString("pinHash")
                val pinSalt = document.getString("pinSalt")
                val pinAlgorithm = document.getString("pinAlgorithm")
                val legacyPin = document.getString("pin")

                onResult(
                    PinHashUtil.verifyPin(
                        inputPin = inputPin,
                        savedHash = pinHash,
                        savedSalt = pinSalt,
                        savedAlgorithm = pinAlgorithm,
                        legacyPlainPin = legacyPin
                    )
                )
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Gagal memverifikasi PIN")
            }
    }

    fun loadPinLockState(
        userId: String?,
        onResult: (PinLockState) -> Unit,
        onError: (String) -> Unit
    ) {
        if (userId == null) {
            onError("User belum login")
            return
        }

        securityDocument(userId)
            .get()
            .addOnSuccessListener { document ->
                onResult(
                    PinLockState(
                        failedAttempts = (document.getLong("failedPinAttempts") ?: 0L).toInt(),
                        lockedUntil = document.getLong("pinLockedUntil") ?: 0L
                    )
                )
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Gagal memuat status PIN")
            }
    }

    fun recordFailedPinAttempt(
        userId: String?,
        onResult: (PinLockState) -> Unit,
        onError: (String) -> Unit
    ) {
        if (userId == null) {
            onError("User belum login")
            return
        }

        val documentRef = securityDocument(userId)
        documentRef
            .get()
            .addOnSuccessListener { document ->
                val now = System.currentTimeMillis()
                val currentLockedUntil = document.getLong("pinLockedUntil") ?: 0L

                if (currentLockedUntil > now) {
                    onResult(
                        PinLockState(
                            failedAttempts = (document.getLong("failedPinAttempts") ?: 0L).toInt(),
                            lockedUntil = currentLockedUntil
                        )
                    )
                    return@addOnSuccessListener
                }

                val currentAttempts = (document.getLong("failedPinAttempts") ?: 0L).toInt()
                val newAttempts = currentAttempts + 1
                val newLockedUntil = if (newAttempts >= MAX_PIN_ATTEMPTS) {
                    now + PIN_LOCK_DURATION_MS
                } else {
                    0L
                }

                val data = mapOf(
                    "failedPinAttempts" to newAttempts,
                    "lastFailedPinAt" to now,
                    "pinLockedUntil" to newLockedUntil
                )

                documentRef
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        onResult(
                            PinLockState(
                                failedAttempts = newAttempts,
                                lockedUntil = newLockedUntil
                            )
                        )
                    }
                    .addOnFailureListener { error ->
                        onError(error.message ?: "Gagal mencatat percobaan PIN")
                    }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Gagal memeriksa percobaan PIN")
            }
    }

    fun resetFailedPinAttempts(
        userId: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (userId == null) {
            onError("User belum login")
            return
        }

        securityDocument(userId)
            .set(
                mapOf(
                    "failedPinAttempts" to 0,
                    "pinLockedUntil" to 0L
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Gagal mereset percobaan PIN")
            }
    }

    fun loadPrivacyMode(
        userId: String?,
        onResult: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        if (userId == null) {
            onError("User belum login")
            return
        }

        securityDocument(userId)
            .get()
            .addOnSuccessListener { document ->
                onResult(document.getBoolean("privacyMode") ?: false)
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Gagal memuat mode privasi")
            }
    }

    fun savePrivacyMode(
        userId: String?,
        enabled: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (userId == null) {
            onError("User belum login")
            return
        }

        securityDocument(userId)
            .set(
                mapOf(
                    "privacyMode" to enabled,
                    "pin" to FieldValue.delete()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Gagal menyimpan mode privasi")
            }
    }
}
