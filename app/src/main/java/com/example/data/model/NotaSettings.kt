package com.example.data.model

/**
 * Pengaturan khusus tarif operasional dan master harga pengemasan untuk modul Order Nota.
 * Beroperasi sebagai ekosistem mandiri yang terisolasi dari pengaturan finansial HVS umum.
 */
data class NotaSettings(
    val wastePct: Double = 0.05,             // Default 5.0%
    val tintaPct: Double = 0.05,             // Default 5.0%
    val tenagaKerjaPct: Double = 0.07,       // Default 7.0%
    val listrikPct: Double = 0.02,           // Default 2.0%
    val maintenancePct: Double = 0.05,       // Default 5.0%
    val masterHargaPengemasan: Double = 300.0 // Default Rp 300 / plastik
)
