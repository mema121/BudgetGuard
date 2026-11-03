package com.azmisava.budgetguard.util

import java.text.NumberFormat
import java.util.Locale

// Fungsi untuk mengubah angka menjadi format mata uang Rupiah
fun formatMoney(
    amount: Double,
    privacyMode: Boolean
): String {
    // Jika Mode Privasi aktif, nominal uang disembunyikan
    if (privacyMode) {
        return "Rp•••••••"
    }

    // Mengubah angka menjadi format mata uang Indonesia
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    // Menghapus angka desimal ,00 agar tampilan lebih rapi
    return formatter.format(amount).replace(",00", "")
}