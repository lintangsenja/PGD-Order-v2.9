package com.example.data.repository

import android.util.Log
import com.example.data.dao.FinanceDao
import com.example.data.firebase.FirestoreSyncManager
import com.example.data.model.MasterAkunSaldo
import com.example.data.model.MutasiManualKeluarMasuk
import com.example.data.model.TransaksiOrderMasuk
import com.example.data.model.MasterPelanggan
import com.example.data.model.MasterSatuanHarga
import com.example.data.model.CustomerFrequency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

class FinanceRepository(
    private val financeDao: FinanceDao,
    private val syncManager: FirestoreSyncManager? = null
) {
    val isSyncing: StateFlow<Boolean> = syncManager?.isSyncing ?: MutableStateFlow(false)
    val isCloudOnline: StateFlow<Boolean> = syncManager?.isCloudOnline ?: MutableStateFlow(false)
    val syncStatusText: StateFlow<String> = syncManager?.syncStatusText ?: MutableStateFlow("Mode Lokal")
    val cloudLastSyncTime: StateFlow<String> = syncManager?.cloudLastSyncTime ?: MutableStateFlow("Baru Saja")

    val allAccounts: Flow<List<MasterAkunSaldo>> = financeDao.getAllAccountsFlow()

    suspend fun getAllAccountsDirect(): List<MasterAkunSaldo> {
        return financeDao.getAllAccountsDirect()
    }

    val allOrders: Flow<List<TransaksiOrderMasuk>> = financeDao.getAllOrdersFlow()
    val allMutations: Flow<List<MutasiManualKeluarMasuk>> = financeDao.getAllMutationsFlow()
    val allPelanggan: Flow<List<MasterPelanggan>> = financeDao.getAllPelangganFlow()
    val allSatuanHarga: Flow<List<MasterSatuanHarga>> = financeDao.getAllSatuanHargaFlow()

    fun getOrdersByDateRangeFlow(startDate: String, endDate: String): Flow<List<TransaksiOrderMasuk>> {
        return financeDao.getOrdersByDateRangeFlow(startDate, endDate)
    }

    suspend fun getOrdersByDateRangeDirect(startDate: String, endDate: String): List<TransaksiOrderMasuk> {
        return financeDao.getOrdersByDateRangeDirect(startDate, endDate)
    }

    fun getCustomerOrderFrequency(startDate: String, endDate: String): Flow<List<CustomerFrequency>> {
        return financeDao.getCustomerOrderFrequency(startDate, endDate)
    }

    fun getMutationsByDateRangeFlow(startDate: String, endDate: String): Flow<List<MutasiManualKeluarMasuk>> {
        return financeDao.getMutationsByDateRangeFlow(startDate, endDate)
    }

    suspend fun getMutationsByDateRangeDirect(startDate: String, endDate: String): List<MutasiManualKeluarMasuk> {
        return financeDao.getMutationsByDateRangeDirect(startDate, endDate)
    }

    suspend fun insertAccount(account: MasterAkunSaldo) {
        val rowId = financeDao.insertAccount(account)
        val finalId = if (account.idAkun == 0) rowId.toInt() else account.idAkun
        val updatedAccount = account.copy(idAkun = finalId)
        try {
            syncManager?.syncWalletToCloud(updatedAccount)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Wallet cloud sync notice: ${e.message}")
        }
    }

    suspend fun updateAccount(account: MasterAkunSaldo) {
        insertAccount(account)
    }

    suspend fun setSaldoAwal(idAkun: Int, saldoAwal: Double) {
        val existingList = financeDao.getAllAccountsDirect()
        val account = existingList.find { it.idAkun == idAkun }
        if (account != null) {
            val updated = account.copy(saldoAwal = saldoAwal)
            insertAccount(updated)
        }
    }

    suspend fun insertOrder(order: TransaksiOrderMasuk): Int {
        val rowId = financeDao.insertOrder(order)
        val finalId = if (order.idOrder == 0) rowId.toInt() else order.idOrder
        val updatedOrder = order.copy(idOrder = finalId)
        try {
            syncManager?.syncOrderToCloud(updatedOrder)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Order cloud sync notice: ${e.message}")
        }
        return finalId
    }

    suspend fun updateOrder(order: TransaksiOrderMasuk) {
        financeDao.updateOrder(order)
        try {
            syncManager?.syncOrderToCloud(order)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Order cloud sync notice: ${e.message}")
        }
    }

    suspend fun deleteOrder(order: TransaksiOrderMasuk) {
        financeDao.deleteOrder(order)
        try {
            syncManager?.deleteOrderFromCloud(order.idOrder)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Order cloud delete notice: ${e.message}")
        }
    }

    suspend fun insertMutation(mutation: MutasiManualKeluarMasuk): Int {
        val rowId = financeDao.insertMutation(mutation)
        val finalId = if (mutation.idMutasi == 0) rowId.toInt() else mutation.idMutasi
        val updatedMutation = mutation.copy(idMutasi = finalId)
        try {
            syncManager?.syncMutationToCloud(updatedMutation)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Mutation cloud sync notice: ${e.message}")
        }
        return finalId
    }

    suspend fun deleteMutation(mutation: MutasiManualKeluarMasuk) {
        financeDao.deleteMutation(mutation)
        try {
            syncManager?.deleteMutationFromCloud(mutation.idMutasi)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Mutation cloud delete notice: ${e.message}")
        }
    }

    suspend fun getAllPelangganDirect(): List<MasterPelanggan> {
        return financeDao.getAllPelangganDirect()
    }

    suspend fun deleteAllPelanggan() {
        financeDao.deleteAllPelanggan()
    }

    suspend fun seedDefaultCustomers(forceOverwrite: Boolean = false) {
        val defaultCustomers = listOf(
            MasterPelanggan(
                idPelanggan = 1,
                namaPelanggan = "Bu Titi",
                instansi = "SMKN 1 Kaligondang",
                kontak = "-",
                alamatInstansi = "SMKN 1 Kaligondang",
                npwp = "-"
            ),
            MasterPelanggan(
                idPelanggan = 2,
                namaPelanggan = "Bu Anggit",
                instansi = "SMKN 1 Kaligondang",
                kontak = "-",
                alamatInstansi = "SMKN 1 Kaligondang",
                npwp = "-"
            ),
            MasterPelanggan(
                idPelanggan = 3,
                namaPelanggan = "Bu Ratri",
                instansi = "SMKN 1 Kaligondang",
                kontak = "-",
                alamatInstansi = "SMKN 1 Kaligondang",
                npwp = "-"
            ),
            MasterPelanggan(
                idPelanggan = 4,
                namaPelanggan = "Bu Widi",
                instansi = "SMKN 1 Kaligondang",
                kontak = "-",
                alamatInstansi = "SMKN 1 Kaligondang",
                npwp = "-"
            ),
            MasterPelanggan(
                idPelanggan = 5,
                namaPelanggan = "Akuntansii",
                instansi = "SMKN 1 Kaligondang",
                kontak = "-",
                alamatInstansi = "SMKN 1 Kaligondang",
                npwp = "-"
            )
        )

        // 1. Seed to Cloud and local sync manager if online
        try {
            syncManager?.seedDefaultCustomersToCloud(forceOverwrite)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Seed to cloud notice: ${e.message}")
        }

        // 2. Local Room DB fallback check & ensure default customers
        try {
            val existing = financeDao.getAllPelangganDirect()
            val hasLegacyNames = existing.any { it.namaPelanggan in listOf("AkL", "TiTi", "RatRi", "WiDi") || it.namaPelanggan.contains("Budi", ignoreCase = true) || it.namaPelanggan.contains("Grafika", ignoreCase = true) }
            val hasAllExpected = listOf("Bu Titi", "Bu Anggit", "Bu Ratri", "Bu Widi", "Akuntansii").all { expected ->
                existing.any { it.namaPelanggan.equals(expected, ignoreCase = true) }
            }

            if (forceOverwrite || existing.isEmpty() || hasLegacyNames || !hasAllExpected) {
                if (forceOverwrite || hasLegacyNames) {
                    financeDao.deleteAllPelanggan()
                }
                for (customer in defaultCustomers) {
                    financeDao.insertPelanggan(customer)
                }
            }
        } catch (e: Throwable) {
            Log.e("FinanceRepository", "Error ensuring local default customers: ${e.message}")
        }
    }

    suspend fun insertPelanggan(pelanggan: MasterPelanggan): Int {
        val rowId = financeDao.insertPelanggan(pelanggan)
        val finalId = if (pelanggan.idPelanggan == 0) rowId.toInt() else pelanggan.idPelanggan
        val updatedPelanggan = pelanggan.copy(idPelanggan = finalId)
        try {
            syncManager?.syncCustomerToCloud(updatedPelanggan)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Customer cloud sync notice: ${e.message}")
        }
        return finalId
    }

    suspend fun deletePelanggan(pelanggan: MasterPelanggan) {
        financeDao.deletePelanggan(pelanggan)
        try {
            syncManager?.deleteCustomerFromCloud(pelanggan.idPelanggan)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Customer cloud delete notice: ${e.message}")
        }
    }

    suspend fun insertSatuanHarga(satuanHarga: MasterSatuanHarga) {
        financeDao.insertSatuanHarga(satuanHarga)
    }

    suspend fun deleteSatuanHarga(satuanHarga: MasterSatuanHarga) {
        financeDao.deleteSatuanHarga(satuanHarga)
    }

    suspend fun syncAllToCloud() {
        syncManager?.syncAllToCloud()
    }

    suspend fun syncProfileToCloud(adminName: String, tagline: String, avatarType: String, avatarUri: String) {
        syncManager?.syncProfileToCloud(adminName, tagline, avatarType, avatarUri)
    }
}
