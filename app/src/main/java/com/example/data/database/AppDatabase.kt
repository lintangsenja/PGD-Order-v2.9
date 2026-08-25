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
    version = 8,
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
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit) VALUES (1, 'Dompet Kertas', 0.0, 106.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit) VALUES (2, 'Dompet Tinta', 0.0, 25.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit) VALUES (3, 'Dompet Pengemasan', 0.0, 0.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit) VALUES (4, 'Dompet Waste / Rusak', 0.05, 0.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit) VALUES (5, 'Dompet Tenaga Kerja', 0.07, 0.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit) VALUES (6, 'Dompet Listrik', 0.02, 0.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit) VALUES (7, 'Dompet Maintenance', 0.05, 0.0);")
                db.execSQL("INSERT INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit) VALUES (8, 'Dompet Laba Bersih', 0.0, 0.0);")
                db.execSQL("INSERT OR IGNORE INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit) VALUES (9, 'Me GpS', 0.0, 0.0);")

                // Seed the 4 default customers
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan) VALUES (1, 'AkL');")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan) VALUES (2, 'TiTi');")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan) VALUES (3, 'RatRi');")
                db.execSQL("INSERT OR IGNORE INTO master_pelanggan (id_pelanggan, nama_pelanggan) VALUES (4, 'WiDi');")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Clean up any remaining dummy/mock data from previous test runs
                db.execSQL("DELETE FROM master_pelanggan WHERE nama_pelanggan LIKE '%Budi%' OR nama_pelanggan LIKE '%CV Grafika%';")
                db.execSQL("INSERT OR IGNORE INTO master_akun_saldo (id_akun, nama_akun, persentase_operasional, konstan_hpp_unit) VALUES (9, 'Me GpS', 0.0, 0.0);")
            }
        }
    }
}
