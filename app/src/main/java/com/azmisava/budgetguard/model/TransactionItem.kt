package com.azmisava.budgetguard.model


// Model data transaksi yang digunakan untuk membaca data dari Firestore
data class TransactionItem(
    val id: String = "",

    // type berisi "income" untuk pemasukan atau "expense" untuk pengeluaran
    val type: String = "",

    // amount digunakan untuk menyimpan nominal transaksi
    val amount: Double = 0.0,

    // category digunakan untuk menyimpan kategori transaksi
    val category: String = "",

    // note digunakan untuk menyimpan catatan tambahan transaksi
    val note: String = "",

    // createdAt digunakan untuk menyimpan waktu transaksi dibuat
    val createdAt: Long = 0L
)