package com.azmisava.budgetguard.util

object InputValidationUtil {
    const val MAX_NOTE_LENGTH = 120
    const val MAX_PROFILE_NAME_LENGTH = 50
    const val MAX_AMOUNT = 1_000_000_000.0

    private val dangerousControlChars = Regex("[\\p{Cntrl}&&[^\\n\\t]]")

    fun sanitizeNote(input: String): String {
        return input
            .replace(dangerousControlChars, "")
            .take(MAX_NOTE_LENGTH)
    }

    fun sanitizeProfileName(input: String): String {
        return input
            .replace(dangerousControlChars, "")
            .replace(Regex("\\s+"), " ")
            .take(MAX_PROFILE_NAME_LENGTH)
    }

    fun isValidAmount(amount: Double?): Boolean {
        return amount != null && amount > 0.0 && amount <= MAX_AMOUNT
    }
}
