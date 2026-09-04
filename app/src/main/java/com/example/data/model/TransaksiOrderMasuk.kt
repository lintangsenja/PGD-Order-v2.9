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
    val metodePembayaran: String = "Bayar Penuh" // "DP", "Bayar Sebagian", "Bayar Penuh"
) {
    // Helper to identify if an order is specific to Nota
    val isNota: Boolean
        get() = kategori.equals("nota", ignoreCase = true) ||
                (kategori.isBlank() || kategori.equals("umum", ignoreCase = true)) &&
                namaPesanan.contains("nota", ignoreCase = true)

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

    // Alokasi teoritis penuh (100%)
    val fullAlokasiKertas: Double
        get() = qtyOrder.toDouble() * 106.0

    val fullAlokasiTinta: Double
        get() = qtyOrder.toDouble() * 25.0

    val fullAlokasiPengemasan: Double
        get() = jumlahPlastikPengemasan.toDouble() * 300.0

    val fullAlokasiWaste: Double
        get() = 0.05 * totalPendapatan

    val fullAlokasiTenagaKerja: Double
        get() = 0.07 * totalPendapatan

    val fullAlokasiListrik: Double
        get() = 0.02 * totalPendapatan

    val fullAlokasiMaintenance: Double
        get() = 0.05 * totalPendapatan

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
        get() = 0.05 * effectiveJumlahDibayar

    val alokasiTenagaKerja: Double
        get() = 0.07 * effectiveJumlahDibayar

    val alokasiListrik: Double
        get() = 0.02 * effectiveJumlahDibayar

    val alokasiMaintenance: Double
        get() = 0.05 * effectiveJumlahDibayar

    val totalModalDasar: Double
        get() = alokasiKertas + alokasiTinta + alokasiPengemasan + alokasiWaste + alokasiTenagaKerja + alokasiListrik + alokasiMaintenance

    val alokasiSisaLaba: Double
        get() = effectiveJumlahDibayar - totalModalDasar
}
