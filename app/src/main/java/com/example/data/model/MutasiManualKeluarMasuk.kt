package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mutasi_manual_keluar_masuk")
data class MutasiManualKeluarMasuk(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_mutasi")
    val idMutasi: Int = 0,
    
    @ColumnInfo(name = "tanggal_mutasi")
    val tanggalMutasi: String, // format "YYYY-MM-DD"
    
    @ColumnInfo(name = "id_akun")
    val idAkun: Int,
    
    @ColumnInfo(name = "jenis_mutasi")
    val jenisMutasi: String, // "Uang Masuk" or "Uang Keluar" or "Pindah Saldo"
    
    @ColumnInfo(name = "nominal")
    val nominal: Double,
    
    @ColumnInfo(name = "keterangan")
    val keterangan: String,

    @ColumnInfo(name = "id_akun_tujuan")
    val idAkunTujuan: Int? = null,

    @ColumnInfo(name = "waktu_mutasi")
    val waktuMutasi: String = "12:00"
)
