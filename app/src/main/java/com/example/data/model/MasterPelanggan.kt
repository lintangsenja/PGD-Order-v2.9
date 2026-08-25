package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_pelanggan")
data class MasterPelanggan(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_pelanggan")
    val idPelanggan: Int = 0,
    
    @ColumnInfo(name = "nama_pelanggan")
    val namaPelanggan: String,
    
    @ColumnInfo(name = "kontak")
    val kontak: String? = null,
    
    @ColumnInfo(name = "instansi")
    val instansi: String? = null,

    @ColumnInfo(name = "alamat_instansi")
    val alamatInstansi: String? = null,

    @ColumnInfo(name = "npwp")
    val npwp: String? = null
)
