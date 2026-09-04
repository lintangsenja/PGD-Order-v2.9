package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaksi_belanja_inventaris")
data class TransaksiBelanjaInventaris(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_belanja")
    val idBelanja: Int = 0,

    @ColumnInfo(name = "tanggal")
    val tanggal: String,

    @ColumnInfo(name = "id_akun_kas")
    val idAkunKas: Int,

    @ColumnInfo(name = "nama_akun_kas")
    val namaAkunKas: String,

    @ColumnInfo(name = "uang_keluar_dompet")
    val uangKeluarDompet: Double,

    @ColumnInfo(name = "realisasi_nota_toko")
    val realisasiNotaToko: Double,

    @ColumnInfo(name = "selisih_uang")
    val selisihUang: Double, // uangKeluarDompet - realisasiNotaToko

    @ColumnInfo(name = "catatan_selisih")
    val catatanSelisih: String = "", // fleksibel: misal bensin, makan harian, kebutuhan mendadak

    @ColumnInfo(name = "id_barang_terkait")
    val idBarangTerkait: Int? = null,

    @ColumnInfo(name = "nama_barang")
    val namaBarang: String = "",

    @ColumnInfo(name = "jumlah_tambah_stok")
    val jumlahTambahStok: Double = 0.0,

    @ColumnInfo(name = "satuan")
    val satuan: String = "",

    @ColumnInfo(name = "potong_kas_otomatis")
    val potongKasOtomatis: Boolean = true
)
