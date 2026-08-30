package com.example.data.dao

import androidx.room.*
import com.example.data.model.MasterAkunSaldo
import com.example.data.model.MutasiManualKeluarMasuk
import com.example.data.model.TransaksiOrderMasuk
import kotlinx.coroutines.flow.Flow

import com.example.data.model.MasterPelanggan
import com.example.data.model.MasterSatuanHarga
import com.example.data.model.CustomerFrequency

@Dao
interface FinanceDao {
    @Query("SELECT * FROM master_akun_saldo ORDER BY id_akun ASC")
    fun getAllAccountsFlow(): Flow<List<MasterAkunSaldo>>

    @Query("SELECT * FROM master_akun_saldo ORDER BY id_akun ASC")
    suspend fun getAllAccountsDirect(): List<MasterAkunSaldo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: MasterAkunSaldo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<MasterAkunSaldo>)

    @Query("SELECT * FROM transaksi_order_masuk ORDER BY tanggal_order DESC, id_order DESC")
    fun getAllOrdersFlow(): Flow<List<TransaksiOrderMasuk>>

    @Query("SELECT * FROM transaksi_order_masuk ORDER BY tanggal_order DESC, id_order DESC")
    suspend fun getAllOrdersDirect(): List<TransaksiOrderMasuk>

    @Query("SELECT * FROM transaksi_order_masuk WHERE tanggal_order >= :startDate AND tanggal_order <= :endDate ORDER BY tanggal_order DESC, id_order DESC")
    fun getOrdersByDateRangeFlow(startDate: String, endDate: String): Flow<List<TransaksiOrderMasuk>>

    @Query("SELECT * FROM transaksi_order_masuk WHERE tanggal_order >= :startDate AND tanggal_order <= :endDate ORDER BY tanggal_order DESC, id_order DESC")
    suspend fun getOrdersByDateRangeDirect(startDate: String, endDate: String): List<TransaksiOrderMasuk>

    @Query("""
        SELECT p.nama_pelanggan AS customer_name, COUNT(o.id_order) AS order_count 
        FROM master_pelanggan p 
        INNER JOIN transaksi_order_masuk o ON o.nama_pesanan = p.nama_pelanggan OR o.nama_pesanan LIKE p.nama_pelanggan || ' - %'
        WHERE o.tanggal_order >= :startDate AND o.tanggal_order <= :endDate
        GROUP BY p.id_pelanggan
        ORDER BY order_count DESC
    """)
    fun getCustomerOrderFrequency(startDate: String, endDate: String): Flow<List<CustomerFrequency>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: TransaksiOrderMasuk): Long

    @Update
    suspend fun updateOrder(order: TransaksiOrderMasuk)

    @Query("DELETE FROM transaksi_order_masuk WHERE id_order = :idOrder")
    suspend fun deleteOrderById(idOrder: Int)

    @Delete
    suspend fun deleteOrder(order: TransaksiOrderMasuk)

    @Query("SELECT * FROM mutasi_manual_keluar_masuk ORDER BY tanggal_mutasi DESC, id_mutasi DESC")
    fun getAllMutationsFlow(): Flow<List<MutasiManualKeluarMasuk>>

    @Query("SELECT * FROM mutasi_manual_keluar_masuk ORDER BY tanggal_mutasi DESC, id_mutasi DESC")
    suspend fun getAllMutationsDirect(): List<MutasiManualKeluarMasuk>

    @Query("SELECT * FROM mutasi_manual_keluar_masuk WHERE tanggal_mutasi >= :startDate AND tanggal_mutasi <= :endDate ORDER BY tanggal_mutasi DESC, id_mutasi DESC")
    fun getMutationsByDateRangeFlow(startDate: String, endDate: String): Flow<List<MutasiManualKeluarMasuk>>

    @Query("SELECT * FROM mutasi_manual_keluar_masuk WHERE tanggal_mutasi >= :startDate AND tanggal_mutasi <= :endDate ORDER BY tanggal_mutasi DESC, id_mutasi DESC")
    suspend fun getMutationsByDateRangeDirect(startDate: String, endDate: String): List<MutasiManualKeluarMasuk>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMutation(mutation: MutasiManualKeluarMasuk): Long

    @Query("DELETE FROM mutasi_manual_keluar_masuk WHERE id_mutasi = :idMutasi")
    suspend fun deleteMutationById(idMutasi: Int)

    @Delete
    suspend fun deleteMutation(mutation: MutasiManualKeluarMasuk)

    @Query("DELETE FROM master_pelanggan WHERE id_pelanggan = :idPelanggan")
    suspend fun deletePelangganById(idPelanggan: Int)

    @Query("DELETE FROM master_pelanggan")
    suspend fun deleteAllPelanggan()

    // MasterPelanggan Queries
    @Query("SELECT * FROM master_pelanggan ORDER BY nama_pelanggan ASC")
    fun getAllPelangganFlow(): Flow<List<MasterPelanggan>>

    @Query("SELECT * FROM master_pelanggan ORDER BY id_pelanggan ASC")
    suspend fun getAllPelangganDirect(): List<MasterPelanggan>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPelanggan(pelanggan: MasterPelanggan): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPelanggan(pelangganList: List<MasterPelanggan>)

    @Delete
    suspend fun deletePelanggan(pelanggan: MasterPelanggan)

    // MasterSatuanHarga Queries
    @Query("SELECT * FROM master_satuan_harga ORDER BY nama_satuan ASC")
    fun getAllSatuanHargaFlow(): Flow<List<MasterSatuanHarga>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSatuanHarga(satuanHarga: MasterSatuanHarga)

    @Delete
    suspend fun deleteSatuanHarga(satuanHarga: MasterSatuanHarga)
}
