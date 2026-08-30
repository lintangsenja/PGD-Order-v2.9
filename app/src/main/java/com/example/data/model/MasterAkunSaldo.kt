package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_akun_saldo")
data class MasterAkunSaldo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_akun")
    val idAkun: Int = 0,
    
    @ColumnInfo(name = "nama_akun")
    val namaAkun: String,
    
    @ColumnInfo(name = "persentase_operasional")
    val persentaseOperasional: Float = 0.0f,
    
    @ColumnInfo(name = "konstan_hpp_unit")
    val konstanHppUnit: Float = 0.0f,

    @ColumnInfo(name = "saldo_awal", defaultValue = "0.0")
    val saldoAwal: Double = 0.0
)
