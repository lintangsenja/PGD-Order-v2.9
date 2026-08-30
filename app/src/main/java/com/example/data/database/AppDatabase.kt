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

@Database(
    entities = [
        MasterAkunSaldo::class,
        TransaksiOrderMasuk::class,
        MutasiManualKeluarMasuk::class,
        MasterPelanggan::class,
        MasterSatuanHarga::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "envelope_budgeting_db"
                )
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

                // Seed the 5 default customers (SMKN 1 Kaligondang)
                db.execSQL("INSERT OR REPLACE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (1, 'Bu Titi', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR REPLACE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (2, 'Bu Anggit', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR REPLACE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (3, 'Bu Ratri', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR REPLACE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (4, 'Bu Widi', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR REPLACE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (5, 'Akuntansii', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Clean up any remaining dummy/mock data or legacy names from previous test runs
                db.execSQL("DELETE FROM master_pelanggan WHERE nama_pelanggan IN ('AkL', 'TiTi', 'RatRi', 'WiDi') OR nama_pelanggan LIKE '%Budi%' OR nama_pelanggan LIKE '%CV Grafika%';")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (1, 'Bu Titi', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (2, 'Bu Anggit', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (3, 'Bu Ratri', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (4, 'Bu Widi', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan, instansi, kontak, alamat_instansi, npwp) VALUES (5, 'Akuntansii', 'SMKN 1 Kaligondang', '-', 'SMKN 1 Kaligondang', '-');")
                db.execSQL("INSERT OR IGNORE INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit, saldo_awal) VALUES (9, 'Me GpS', 0.0, 0.0, 0.0);")
            }
        }
    }
}
