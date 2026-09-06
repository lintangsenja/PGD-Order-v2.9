package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "inventaris_bahan_baku")
data class InventarisBahanBaku(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_barang")
    val idBarang: Int = 0,

    @ColumnInfo(name = "nama_barang")
    val namaBarang: String = "",

    @ColumnInfo(name = "kategori")
    val kategori: String = "Kertas", // "Kertas", "Tinta", "Plastik & Pengemasan", "Operasional & Lainnya"

    @ColumnInfo(name = "stok_utuh")
    val stokUtuh: Double = 0.0, // misal 10.0

    @ColumnInfo(name = "satuan_utuh")
    val satuanUtuh: String = "Pcs", // "Rim", "Dus", "Botol", "Pack", "Roll", "Pcs"

    @ColumnInfo(name = "harga_satuan_utuh")
    val hargaSatuanUtuh: Double = 0.0, // misal 52000.0

    @ColumnInfo(name = "persentase_kondisi")
    val persentaseKondisi: Int = 100, // 100%, 75%, 50%, 25%, 0%

    @ColumnInfo(name = "catatan")
    val catatan: String = "",

    @ColumnInfo(name = "updated_at")
    val updatedAt: String = ""
) {
    val nilaiTotalAset: Double
        get() = try {
            val safeStok = if (stokUtuh.isNaN() || stokUtuh.isInfinite()) 0.0 else stokUtuh.coerceAtLeast(0.0)
            val safeHarga = if (hargaSatuanUtuh.isNaN() || hargaSatuanUtuh.isInfinite()) 0.0 else hargaSatuanUtuh.coerceAtLeast(0.0)
            val safePersen = persentaseKondisi.coerceIn(0, 100).toDouble() / 100.0
            safeStok * safePersen * safeHarga
        } catch (e: Exception) {
            0.0
        }

    val statusKondisiText: String
        get() = try {
            when (persentaseKondisi) {
                100 -> "Utuh (100%)"
                75 -> "Sisa 3/4 (75%)"
                50 -> "Sisa 1/2 (50%)"
                25 -> "Sisa 1/4 (25%)"
                0 -> "Habis (0%)"
                else -> "${persentaseKondisi.coerceIn(0, 100)}%"
            }
        } catch (e: Exception) {
            "100%"
        }

    val statusStokGabungan: String
        get() = formatStokGabungan(stokUtuh, satuanUtuh)

    companion object {
        fun formatStokGabungan(stok: Double?, satuanUtuh: String?): String {
            val satuan = if (satuanUtuh.isNullOrBlank()) "Pcs" else satuanUtuh
            if (stok == null || stok.isNaN() || stok.isInfinite() || stok <= 0.0) return "0 $satuan"
            val isRim = satuan.equals("Rim", ignoreCase = true)
            return try {
                if (isRim) {
                    val totalLembar = Math.round(stok * 500.0)
                    val rimUtuh = (totalLembar / 500).toInt()
                    val lembar = (totalLembar % 500).toInt()
                    when {
                        rimUtuh > 0 && lembar > 0 -> "$rimUtuh Rim & $lembar Lembar"
                        rimUtuh > 0 -> "$rimUtuh Rim"
                        lembar > 0 -> "$lembar Lembar"
                        else -> "0 Rim"
                    }
                } else {
                    if (stok % 1.0 == 0.0) {
                        String.format(Locale.GERMANY, "%,.0f", stok) + " $satuan"
                    } else {
                        String.format(Locale.GERMANY, "%,.2f", stok) + " $satuan"
                    }
                }
            } catch (e: Exception) {
                "$stok $satuan"
            }
        }
    }
}
