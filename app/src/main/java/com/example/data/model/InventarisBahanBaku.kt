package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventaris_bahan_baku")
data class InventarisBahanBaku(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_barang")
    val idBarang: Int = 0,

    @ColumnInfo(name = "nama_barang")
    val namaBarang: String,

    @ColumnInfo(name = "kategori")
    val kategori: String, // "Kertas", "Tinta", "Plastik & Pengemasan", "Operasional & Lainnya"

    @ColumnInfo(name = "stok_utuh")
    val stokUtuh: Double, // misal 10.0

    @ColumnInfo(name = "satuan_utuh")
    val satuanUtuh: String, // "Rim", "Dus", "Botol", "Pack", "Roll", "Pcs"

    @ColumnInfo(name = "harga_satuan_utuh")
    val hargaSatuanUtuh: Double, // misal 52000.0

    @ColumnInfo(name = "persentase_kondisi")
    val persentaseKondisi: Int = 100, // 100%, 75%, 50%, 25%, 0%

    @ColumnInfo(name = "catatan")
    val catatan: String = "",

    @ColumnInfo(name = "updated_at")
    val updatedAt: String = ""
) {
    val nilaiTotalAset: Double
        get() = (stokUtuh * (persentaseKondisi.toDouble() / 100.0)) * hargaSatuanUtuh

    val statusKondisiText: String
        get() = when (persentaseKondisi) {
            100 -> "Utuh (100%)"
            75 -> "Sisa 3/4 (75%)"
            50 -> "Sisa 1/2 (50%)"
            25 -> "Sisa 1/4 (25%)"
            0 -> "Habis (0%)"
            else -> "$persentaseKondisi%"
        }
}
