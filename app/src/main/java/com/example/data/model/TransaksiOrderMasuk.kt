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
    val status: String // "Lunas" or "Belum Lunas"
) {
    // Helper calculations inside the model to keep code clean and modular
    val totalPendapatan: Double
        get() = qtyOrder.toDouble() * hargaSatuan

    val alokasiKertas: Double
        get() = qtyOrder.toDouble() * 106.0

    val alokasiTinta: Double
        get() = qtyOrder.toDouble() * 25.0

    val alokasiPengemasan: Double
        get() = jumlahPlastikPengemasan.toDouble() * 300.0

    val alokasiWaste: Double
        get() = 0.05 * totalPendapatan

    val alokasiTenagaKerja: Double
        get() = 0.07 * totalPendapatan

    val alokasiListrik: Double
        get() = 0.02 * totalPendapatan

    val alokasiMaintenance: Double
        get() = 0.05 * totalPendapatan

    val totalModalDasar: Double
        get() = alokasiKertas + alokasiTinta + alokasiPengemasan + alokasiWaste + alokasiTenagaKerja + alokasiListrik + alokasiMaintenance

    val alokasiSisaLaba: Double
        get() = totalPendapatan - totalModalDasar
}
