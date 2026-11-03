package com.azmisava.budgetguard.viewmodel

import com.azmisava.budgetguard.repository.SecurityRepository

class SecurityViewModel(
    private val repository: SecurityRepository = SecurityRepository()
) {
    fun checkPinExists(
        userId: String?,
        onResult: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        repository.checkPinExists(userId, onResult, onError)
    }

    fun savePin(
        userId: String?,
        pin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.savePin(userId, pin, onSuccess, onError)
    }

    fun verifyPin(
        userId: String?,
        inputPin: String,
        onResult: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        repository.verifyPin(userId, inputPin, onResult, onError)
    }

    fun loadPinLockState(
        userId: String?,
        onResult: (SecurityRepository.PinLockState) -> Unit,
        onError: (String) -> Unit
    ) {
        repository.loadPinLockState(userId, onResult, onError)
    }

    fun recordFailedPinAttempt(
        userId: String?,
        onResult: (SecurityRepository.PinLockState) -> Unit,
        onError: (String) -> Unit
    ) {
        repository.recordFailedPinAttempt(userId, onResult, onError)
    }

    fun resetFailedPinAttempts(
        userId: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository.resetFailedPinAttempts(userId, onSuccess, onError)
    }

    fun loadPrivacyMode(
        userId: String?,
        onResult: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        repository.loadPrivacyMode(userId, onResult, onError)
    }

    fun savePrivacyMode(
        userId: String?,
        enabled: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.savePrivacyMode(userId, enabled, onSuccess, onError)
    }
}
