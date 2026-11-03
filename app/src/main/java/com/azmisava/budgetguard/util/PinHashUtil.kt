package com.azmisava.budgetguard.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHashUtil {
    const val PIN_HASH_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val LEGACY_HASH_ALGORITHM = "SHA-256"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    fun isValidPin(pin: String): Boolean {
        return pin.length == 4 && pin.all { it.isDigit() }
    }

    fun isWeakPin(pin: String): Boolean {
        if (!isValidPin(pin)) {
            return true
        }

        val commonPins = setOf(
            "0000", "1111", "2222", "3333", "4444",
            "5555", "6666", "7777", "8888", "9999",
            "1234", "4321", "0123", "9876"
        )

        if (pin in commonPins) {
            return true
        }

        val digits = pin.map { it.digitToInt() }
        val ascending = digits.zipWithNext().all { (a, b) -> b == a + 1 }
        val descending = digits.zipWithNext().all { (a, b) -> b == a - 1 }

        return ascending || descending
    }

    fun generateSalt(): String {
        val randomBytes = ByteArray(16)
        SecureRandom().nextBytes(randomBytes)
        return Base64.encodeToString(randomBytes, Base64.NO_WRAP)
    }

    fun hashPin(pin: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val spec = PBEKeySpec(
            pin.toCharArray(),
            saltBytes,
            PBKDF2_ITERATIONS,
            KEY_LENGTH_BITS
        )

        val secretKey = SecretKeyFactory
            .getInstance(PIN_HASH_ALGORITHM)
            .generateSecret(spec)
            .encoded

        return Base64.encodeToString(secretKey, Base64.NO_WRAP)
    }

    private fun legacySha256Hash(pin: String, salt: String): String {
        val input = "$salt:$pin"
        val digest = MessageDigest.getInstance(LEGACY_HASH_ALGORITHM)
            .digest(input.toByteArray(Charsets.UTF_8))

        return digest.joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    fun verifyPin(
        inputPin: String,
        savedHash: String?,
        savedSalt: String?,
        savedAlgorithm: String? = null,
        legacyPlainPin: String? = null
    ): Boolean {
        if (!isValidPin(inputPin)) {
            return false
        }

        if (!savedHash.isNullOrBlank() && !savedSalt.isNullOrBlank()) {
            val calculatedHash = if (savedAlgorithm == PIN_HASH_ALGORITHM) {
                hashPin(inputPin, savedSalt)
            } else {
                // Fallback untuk PIN lama yang sempat disimpan memakai SHA-256 sederhana.
                legacySha256Hash(inputPin, savedSalt)
            }

            return constantTimeEquals(calculatedHash, savedHash)
        }

        // Fallback akun lama yang masih menyimpan PIN plaintext.
        return !legacyPlainPin.isNullOrBlank() && constantTimeEquals(inputPin, legacyPlainPin)
    }

    private fun constantTimeEquals(first: String, second: String): Boolean {
        return MessageDigest.isEqual(
            first.toByteArray(Charsets.UTF_8),
            second.toByteArray(Charsets.UTF_8)
        )
    }
}
