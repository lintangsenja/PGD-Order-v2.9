package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaksi_order_masuk")
data class TransaksiOrderMasuk(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_order")
    val idOrder: Int = 0,
    
    @ColumnInfo(name = "tanggal_order")
    val tanggalOrder: String, // format "YYYY-MM-DD"
    
    @ColumnInfo(name = "nama_pesanan")
    val namaPesanan: String,
    
    @ColumnInfo(name = "qty_order")
    val qtyOrder: Int,
    
    @ColumnInfo(name = "satuan")
    val satuan: String = "Lembar",
    
    @ColumnInfo(name = "harga_satuan")
    val hargaSatuan: Double,
    
    @ColumnInfo(name = "jumlah_plastik_pengemasan")
    val jumlahPlastikPengemasan: Int = 0,
    
    @ColumnInfo(name = "status")
    val status: String, // "Lunas" or "Belum Lunas"
    
    @ColumnInfo(name = "kategori", defaultValue = "Umum")
    val kategori: String = "Umum", // "Nota", "Umum", etc.

    @ColumnInfo(name = "jumlah_dibayar", defaultValue = "0.0")
    val jumlahDibayar: Double = 0.0,

    @ColumnInfo(name = "metode_pembayaran", defaultValue = "Bayar Penuh")
    val metodePembayaran: String = "Bayar Penuh", // "DP", "Bayar Sebagian", "Bayar Penuh"

    @ColumnInfo(name = "hpp_kertas_snapshot", defaultValue = "0.0")
    val hppKertasSnapshot: Double = 0.0,

    @ColumnInfo(name = "hpp_tinta_snapshot", defaultValue = "0.0")
    val hppTintaSnapshot: Double = 0.0,

    @ColumnInfo(name = "hpp_pengemasan_snapshot", defaultValue = "0.0")
    val hppPengemasanSnapshot: Double = 0.0,

    @ColumnInfo(name = "waste_pct_snapshot", defaultValue = "0.0")
    val wastePctSnapshot: Double = 0.0,

    @ColumnInfo(name = "tenaga_kerja_pct_snapshot", defaultValue = "0.0")
    val tenagaKerjaPctSnapshot: Double = 0.0,

    @ColumnInfo(name = "listrik_pct_snapshot", defaultValue = "0.0")
    val listrikPctSnapshot: Double = 0.0,

    @ColumnInfo(name = "maintenance_pct_snapshot", defaultValue = "0.0")
    val maintenancePctSnapshot: Double = 0.0
) {
    // Helper to identify if an order is specific to Nota
    val isNota: Boolean
        get() = kategori.equals("nota", ignoreCase = true) ||
                (kategori.isBlank() || kategori.equals("umum", ignoreCase = true)) &&
                namaPesanan.contains("nota", ignoreCase = true)

    // Helper untuk mendeteksi skenario Paket / Borongan
    val isBorongan: Boolean
        get() = namaPesanan.contains("[Borongan", ignoreCase = true) || 
                namaPesanan.contains("• Borongan", ignoreCase = true) ||
                namaPesanan.contains("Paket Borongan", ignoreCase = true)

    // Helper untuk mengekstrak catatan hasil jadi paket borongan sebagai acuan repeat order
    val catatanHasilJadi: String?
        get() {
            val regex = Regex("""\[(?:Borongan:\s*|Hasil Jadi:\s*)([^\]]+)\]""", RegexOption.IGNORE_CASE)
            val match = regex.find(namaPesanan)
            if (match != null) return match.groupValues[1].trim()
            val altRegex = Regex("""Hasil Jadi:\s*([^•\n\r\]]+)""", RegexOption.IGNORE_CASE)
            val altMatch = altRegex.find(namaPesanan)
            return altMatch?.groupValues?.get(1)?.trim()
        }

    // Helper untuk menampilkan judul pesanan yang bersih tanpa tag kurung
    val cleanOrderTitle: String
        get() {
            val regex = Regex("""\s*\[(?:Borongan|Hasil Jadi)[^\]]*\]""", RegexOption.IGNORE_CASE)
            return namaPesanan.replace(regex, "").trim()
        }

    // Total Tagihan Keseluruhan
    val totalPendapatan: Double
        get() = qtyOrder.toDouble() * hargaSatuan

    // Uang riil yang sudah dibayarkan ke kas
    val effectiveJumlahDibayar: Double
        get() = if (status.equals("Lunas", ignoreCase = true) && jumlahDibayar <= 0.0) {
            totalPendapatan
        } else {
            jumlahDibayar.coerceIn(0.0, totalPendapatan)
        }

    // Sisa kekurangan tagihan
    val sisaKekurangan: Double
        get() = (totalPendapatan - effectiveJumlahDibayar).coerceAtLeast(0.0)

    // Rasio pembayaran aktual (0.0 .. 1.0) untuk autoplotting proporsional
    val paymentRatio: Double
        get() = if (totalPendapatan > 0.0) {
            (effectiveJumlahDibayar / totalPendapatan).coerceIn(0.0, 1.0)
        } else {
            1.0
        }

    // Resolusi tarif HPP/persentase:
    // Jika transaksi Nota: selalu gunakan snapshot khusus nota yang telah dikalkulasi (termasuk nilai 0.0 untuk pos opsional).
    // Jika transaksi umum sudah LUNAS dan memiliki snapshot yang valid (> 0), kunci (immutable) menggunakan snapshot.
    // Jika transaksi umum masih PENDING / BELUM LUNAS, gunakan tarif aktif terbaru (activeFallback).
    fun getEffectiveKertasHpp(activeFallback: Double = 106.0): Double =
        if (isNota) hppKertasSnapshot else if (status.equals("Lunas", ignoreCase = true) && hppKertasSnapshot > 0.0) hppKertasSnapshot else activeFallback

    fun getEffectiveTintaHpp(activeFallback: Double = 25.0): Double =
        if (isNota) hppTintaSnapshot else if (status.equals("Lunas", ignoreCase = true) && hppTintaSnapshot > 0.0) hppTintaSnapshot else activeFallback

    fun getEffectivePengemasanHpp(activeFallback: Double = 300.0): Double =
        if (isNota) hppPengemasanSnapshot else if (status.equals("Lunas", ignoreCase = true) && hppPengemasanSnapshot > 0.0) hppPengemasanSnapshot else activeFallback

    fun getEffectiveWastePct(activeFallback: Double = 0.05): Double =
        if (isNota) wastePctSnapshot else if (status.equals("Lunas", ignoreCase = true) && wastePctSnapshot > 0.0) wastePctSnapshot else activeFallback

    fun getEffectiveTenagaKerjaPct(activeFallback: Double = 0.07): Double =
        if (isNota) tenagaKerjaPctSnapshot else if (status.equals("Lunas", ignoreCase = true) && tenagaKerjaPctSnapshot > 0.0) tenagaKerjaPctSnapshot else activeFallback

    fun getEffectiveListrikPct(activeFallback: Double = 0.02): Double =
        if (isNota) listrikPctSnapshot else if (status.equals("Lunas", ignoreCase = true) && listrikPctSnapshot > 0.0) listrikPctSnapshot else activeFallback

    fun getEffectiveMaintenancePct(activeFallback: Double = 0.05): Double =
        if (isNota) maintenancePctSnapshot else if (status.equals("Lunas", ignoreCase = true) && maintenancePctSnapshot > 0.0) maintenancePctSnapshot else activeFallback

    // Alokasi teoritis penuh (100%)
    val fullAlokasiKertas: Double
        get() = qtyOrder.toDouble() * getEffectiveKertasHpp(106.0)

    val fullAlokasiTinta: Double
        get() = qtyOrder.toDouble() * getEffectiveTintaHpp(25.0)

    val fullAlokasiPengemasan: Double
        get() = jumlahPlastikPengemasan.toDouble() * getEffectivePengemasanHpp(300.0)

    val fullAlokasiWaste: Double
        get() = getEffectiveWastePct(0.05) * totalPendapatan

    val fullAlokasiTenagaKerja: Double
        get() = getEffectiveTenagaKerjaPct(0.07) * totalPendapatan

    val fullAlokasiListrik: Double
        get() = getEffectiveListrikPct(0.02) * totalPendapatan

    val fullAlokasiMaintenance: Double
        get() = getEffectiveMaintenancePct(0.05) * totalPendapatan

    val fullTotalModalDasar: Double
        get() = fullAlokasiKertas + fullAlokasiTinta + fullAlokasiPengemasan + fullAlokasiWaste + fullAlokasiTenagaKerja + fullAlokasiListrik + fullAlokasiMaintenance

    val fullAlokasiSisaLaba: Double
        get() = totalPendapatan - fullTotalModalDasar

    // Alokasi proporsional yang riil terplotting ke dompet berdasarkan uang riil yang sudah dibayarkan saat ini:
    val alokasiKertas: Double
        get() = fullAlokasiKertas * paymentRatio

    val alokasiTinta: Double
        get() = fullAlokasiTinta * paymentRatio

    val alokasiPengemasan: Double
        get() = fullAlokasiPengemasan * paymentRatio

    val alokasiWaste: Double
        get() = getEffectiveWastePct(0.05) * effectiveJumlahDibayar

    val alokasiTenagaKerja: Double
        get() = getEffectiveTenagaKerjaPct(0.07) * effectiveJumlahDibayar

    val alokasiListrik: Double
        get() = getEffectiveListrikPct(0.02) * effectiveJumlahDibayar

    val alokasiMaintenance: Double
        get() = getEffectiveMaintenancePct(0.05) * effectiveJumlahDibayar

    val totalModalDasar: Double
        get() = alokasiKertas + alokasiTinta + alokasiPengemasan + alokasiWaste + alokasiTenagaKerja + alokasiListrik + alokasiMaintenance

    val alokasiSisaLaba: Double
        get() = effectiveJumlahDibayar - totalModalDasar
}
