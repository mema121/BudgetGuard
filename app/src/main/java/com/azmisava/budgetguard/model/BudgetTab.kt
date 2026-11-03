package com.azmisava.budgetguard.model

// Enum ini digunakan untuk mendefinisikan menu utama aplikasi BudgetGuard
enum class BudgetTab(
    val title: String,
    val icon: String
) {
    Dashboard("Dashboard", "⌂"),
    Transaksi("Transaksi", "+"),
    Riwayat("Riwayat", "◷"),
    Insight("Insight", "▮"),
    Profil("Profil", "♙")
}