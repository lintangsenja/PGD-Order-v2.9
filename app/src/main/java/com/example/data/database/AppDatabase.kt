package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FinanceDao
import com.example.data.model.MasterAkunSaldo
import com.example.data.model.MutasiManualKeluarMasuk
import com.example.data.model.TransaksiOrderMasuk
import com.example.data.model.MasterPelanggan
import com.example.data.model.MasterSatuanHarga
import com.example.data.model.InventarisBahanBaku
import com.example.data.model.TransaksiBelanjaInventaris
import com.example.data.model.RiwayatPemakaianBahan

@Database(
    entities = [
        MasterAkunSaldo::class,
        TransaksiOrderMasuk::class,
        MutasiManualKeluarMasuk::class,
        MasterPelanggan::class,
        MasterSatuanHarga::class,
        InventarisBahanBaku::class,
        TransaksiBelanjaInventaris::class,
        RiwayatPemakaianBahan::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transaksi_order_masuk ADD COLUMN jumlah_dibayar REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE transaksi_order_masuk ADD COLUMN metode_pembayaran TEXT NOT NULL DEFAULT 'Bayar Penuh'")
                db.execSQL("UPDATE transaksi_order_masuk SET jumlah_dibayar = qty_order * harga_satuan WHERE status = 'Lunas'")
            }
        }

        private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS inventaris_bahan_baku (
                        id_barang INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nama_barang TEXT NOT NULL,
                        kategori TEXT NOT NULL,
                        stok_utuh REAL NOT NULL,
                        satuan_utuh TEXT NOT NULL,
                        harga_satuan_utuh REAL NOT NULL,
                        persentase_kondisi INTEGER NOT NULL DEFAULT 100,
                        catatan TEXT NOT NULL DEFAULT '',
                        updated_at TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS transaksi_belanja_inventaris (
                        id_belanja INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tanggal TEXT NOT NULL,
                        id_akun_kas INTEGER NOT NULL,
                        nama_akun_kas TEXT NOT NULL,
                        uang_keluar_dompet REAL NOT NULL,
                        realisasi_nota_toko REAL NOT NULL,
                        selisih_uang REAL NOT NULL,
                        catatan_selisih TEXT NOT NULL DEFAULT '',
                        id_barang_terkait INTEGER,
                        nama_barang TEXT NOT NULL DEFAULT '',
                        jumlah_tambah_stok REAL NOT NULL DEFAULT 0.0,
                        satuan TEXT NOT NULL DEFAULT '',
                        potong_kas_otomatis INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS riwayat_pemakaian_bahan (
                        id_pemakaian INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tanggal TEXT NOT NULL,
                        id_barang INTEGER NOT NULL,
                        nama_barang TEXT NOT NULL,
                        jenis_koreksi TEXT NOT NULL,
                        nilai_perubahan TEXT NOT NULL,
                        keterangan TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Hapus total seluruh data sampel inventaris agar bersih dan kosong tanpa data bawaan
                db.execSQL("DELETE FROM inventaris_bahan_baku WHERE id_barang IN (1, 2, 3, 4, 5, 6, 7) OR nama_barang LIKE '%HVS%' OR nama_barang LIKE '%Art Paper%' OR nama_barang LIKE '%Tinta Epson%' OR nama_barang LIKE '%Plastik OPP%' OR nama_barang LIKE '%Kardus Packing%';")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "envelope_budgeting_db"
                )
                    .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed the 9 master accounts/wallets required by the system
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit, saldo_awal) VALUES (1, 'Dompet Kertas', 0.0, 106.0, 0.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit, saldo_awal) VALUES (2, 'Dompet Tinta', 0.0, 25.0, 0.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit, saldo_awal) VALUES (3, 'Dompet Pengemasan', 0.0, 0.0, 0.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit, saldo_awal) VALUES (4, 'Dompet Waste / Rusak', 0.05, 0.0, 0.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit, saldo_awal) VALUES (5, 'Dompet Tenaga Kerja', 0.07, 0.0, 0.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit, saldo_awal) VALUES (6, 'Dompet Listrik', 0.02, 0.0, 0.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit, saldo_awal) VALUES (7, 'Dompet Maintenance', 0.05, 0.0, 0.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit, saldo_awal) VALUES (8, 'Dompet Laba Bersih', 0.0, 0.0, 0.0);")
                db.execSQL("INSERT OR IGNORE INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit, saldo_awal) VALUES (9, 'Me GpS', 0.0, 0.0, 0.0);")

                // Seed the 6 default customers (SMKN 1 Kaligondang & Umum)
                db.execSQL("INSERT OR REPLACE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (1, 'Bu Titi', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR REPLACE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (2, 'Bu Anggit', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR REPLACE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (3, 'Bu Ratri', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR REPLACE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (4, 'Bu Widi', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR REPLACE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (5, 'AKUNTANSI', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR REPLACE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (6, 'Umum', '-', '-', '-', '-');")
                // Tidak ada data sampel inventaris: modul dimulai bersih dan kosong
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Clean up any remaining dummy/mock data or legacy names from previous test runs
                db.execSQL("DELETE FROM master_pelanggan WHERE nama_pelanggan IN ('AkL', 'TiTi', 'RatRi', 'WiDi', 'Akuntansii') OR nama_pelanggan LIKE '%Budi%' OR nama_pelanggan LIKE '%CV Grafika%';")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (1, 'Bu Titi', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (2, 'Bu Anggit', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (3, 'Bu Ratri', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (4, 'Bu Widi', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (5, 'AKUNTANSI', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (6, 'Umum', '-', '-', '-', '-');")
                db.execSQL("INSERT OR IGNORE INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit, saldo_awal) VALUES (9, 'Me GpS', 0.0, 0.0, 0.0);")

                // Bersihkan total data sampel bahan baku bawaan
                db.execSQL("DELETE FROM inventaris_bahan_baku WHERE id_barang IN (1, 2, 3, 4, 5, 6, 7) OR nama_barang LIKE '%HVS%' OR nama_barang LIKE '%Art Paper%' OR nama_barang LIKE '%Tinta Epson%' OR nama_barang LIKE '%Plastik OPP%' OR nama_barang LIKE '%Kardus Packing%';")
            }
        }
    }
}
