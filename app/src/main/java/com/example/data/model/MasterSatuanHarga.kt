package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_satuan_harga")
data class MasterSatuanHarga(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_satuan")
    val idSatuan: Int = 0,
    
    @ColumnInfo(name = "nama_satuan")
    val namaSatuan: String,
    
    @ColumnInfo(name = "opsi_harga_default")
    val opsiHargaDefault: Double
)
