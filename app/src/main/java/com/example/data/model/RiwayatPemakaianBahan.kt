package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "riwayat_pemakaian_bahan")
data class RiwayatPemakaianBahan(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_pemakaian")
    val idPemakaian: Int = 0,

    @ColumnInfo(name = "tanggal")
    val tanggal: String,

    @ColumnInfo(name = "id_barang")
    val idBarang: Int,

    @ColumnInfo(name = "nama_barang")
    val namaBarang: String,

    @ColumnInfo(name = "jenis_koreksi")
    val jenisKoreksi: String, // "Kurangi Satuan Utuh", "Ubah Persentase", "Tambah Stok Fisik"

    @ColumnInfo(name = "nilai_perubahan")
    val nilaiPerubahan: String, // misal "-2 Rim", "100% -> 50%", "+5 Botol"

    @ColumnInfo(name = "keterangan")
    val keterangan: String = ""
)
