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
import com.example.data.model.InventarisBahanBaku
import com.example.data.model.TransaksiBelanjaInventaris
import com.example.data.model.RiwayatPemakaianBahan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch

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
    val allInventaris: Flow<List<InventarisBahanBaku>> = financeDao.getAllInventarisFlow()
        .catch { e ->
            Log.e("FinanceRepository", "Error reading allInventaris: ${e.message}")
            emit(emptyList())
        }
    val allBelanjaInventaris: Flow<List<TransaksiBelanjaInventaris>> = financeDao.getAllBelanjaInventarisFlow()
        .catch { e ->
            Log.e("FinanceRepository", "Error reading allBelanjaInventaris: ${e.message}")
            emit(emptyList())
        }
    val allPemakaianBahan: Flow<List<RiwayatPemakaianBahan>> = financeDao.getAllPemakaianBahanFlow()
        .catch { e ->
            Log.e("FinanceRepository", "Error reading allPemakaianBahan: ${e.message}")
            emit(emptyList())
        }

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

    suspend fun updateMutation(mutation: MutasiManualKeluarMasuk) {
        financeDao.updateMutation(mutation)
        try {
            syncManager?.syncMutationToCloud(mutation)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Mutation cloud sync notice: ${e.message}")
        }
    }

    suspend fun deleteMutation(mutation: MutasiManualKeluarMasuk) {
        financeDao.deleteMutation(mutation)
        try {
            syncManager?.deleteMutationFromCloud(mutation.idMutasi)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Mutation cloud delete notice: ${e.message}")
        }
    }

    suspend fun getAllMutationsDirect(): List<MutasiManualKeluarMasuk> {
        return financeDao.getAllMutationsDirect()
    }

    suspend fun deleteAuditMutations(
        auditId: String,
        fallbackNote: String = "",
        secondaryFallbackNote: String = ""
    ) {
        try {
            val all = financeDao.getAllMutationsDirect()
            val toDelete = all.filter { mut ->
                (auditId.isNotBlank() && mut.keterangan.contains("[AUDIT_ID:$auditId]")) ||
                (fallbackNote.isNotBlank() && mut.keterangan.contains("Audit Selisih Kas") && mut.keterangan.contains(fallbackNote)) ||
                (secondaryFallbackNote.isNotBlank() && mut.keterangan.contains("Audit Selisih Kas") && mut.keterangan.contains(secondaryFallbackNote))
            }
            toDelete.forEach { mut ->
                deleteMutation(mut)
            }
        } catch (e: Throwable) {
            Log.e("FinanceRepository", "Error deleting audit mutations: ${e.message}")
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
                namaPelanggan = "AKUNTANSI",
                instansi = "SMKN 1 Kaligondang",
                kontak = "-",
                alamatInstansi = "SMKN 1 Kaligondang",
                npwp = "-"
            ),
            MasterPelanggan(
                idPelanggan = 6,
                namaPelanggan = "Umum",
                instansi = "-",
                kontak = "-",
                alamatInstansi = "-",
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
            val hasLegacyNames = existing.any { it.namaPelanggan in listOf("AkL", "TiTi", "RatRi", "WiDi", "Akuntansii") || it.namaPelanggan.contains("Budi", ignoreCase = true) || it.namaPelanggan.contains("Grafika", ignoreCase = true) }
            val hasAllExpected = listOf("Bu Titi", "Bu Anggit", "Bu Ratri", "Bu Widi", "AKUNTANSI", "Umum").all { expected ->
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

    // Inventaris & Aset Bahan Baku
    suspend fun insertInventaris(item: InventarisBahanBaku): Long {
        val rowId = financeDao.insertInventaris(item)
        val finalId = if (item.idBarang == 0) rowId.toInt() else item.idBarang
        val updatedItem = item.copy(idBarang = finalId)
        try {
            syncManager?.syncInventarisToCloud(updatedItem)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Inventaris cloud sync notice: ${e.message}")
        }
        return rowId
    }

    suspend fun updateInventaris(item: InventarisBahanBaku) {
        financeDao.updateInventaris(item)
        try {
            syncManager?.syncInventarisToCloud(item)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Inventaris cloud update notice: ${e.message}")
        }
    }

    suspend fun deleteInventaris(item: InventarisBahanBaku) {
        financeDao.deleteInventaris(item)
        try {
            syncManager?.deleteInventarisFromCloud(item.idBarang)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Inventaris cloud delete notice: ${e.message}")
        }
    }

    suspend fun cleanSampleInventaris() {
        financeDao.cleanSampleInventaris()
        try {
            syncManager?.cleanSampleInventarisFromCloud()
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Inventaris clean sample cloud notice: ${e.message}")
        }
    }

    suspend fun insertBelanjaInventaris(item: TransaksiBelanjaInventaris): Long {
        val rowId = financeDao.insertBelanjaInventaris(item)
        val finalId = if (item.idBelanja == 0) rowId.toInt() else item.idBelanja
        val updatedItem = item.copy(idBelanja = finalId)
        try {
            syncManager?.syncBelanjaInventarisToCloud(updatedItem)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Belanja cloud sync notice: ${e.message}")
        }
        return rowId
    }

    suspend fun getAllBelanjaInventarisDirect(): List<TransaksiBelanjaInventaris> {
        return financeDao.getAllBelanjaInventarisDirect()
    }

    suspend fun updateBelanjaInventaris(item: TransaksiBelanjaInventaris) {
        financeDao.updateBelanjaInventaris(item)
        try {
            syncManager?.syncBelanjaInventarisToCloud(item)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Belanja cloud update notice: ${e.message}")
        }
    }

    suspend fun deleteBelanjaInventaris(item: TransaksiBelanjaInventaris) {
        financeDao.deleteBelanjaInventaris(item)
        try {
            syncManager?.deleteBelanjaInventarisFromCloud(item.idBelanja)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Belanja cloud delete notice: ${e.message}")
        }
    }

    suspend fun deleteBelanjaInventarisById(idBelanja: Int) {
        financeDao.deleteBelanjaInventarisById(idBelanja)
        try {
            syncManager?.deleteBelanjaInventarisFromCloud(idBelanja)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Belanja cloud delete by id notice: ${e.message}")
        }
    }

    suspend fun insertPemakaianBahan(item: RiwayatPemakaianBahan): Long {
        val rowId = financeDao.insertPemakaianBahan(item)
        val finalId = if (item.idPemakaian == 0) rowId.toInt() else item.idPemakaian
        val updatedItem = item.copy(idPemakaian = finalId)
        try {
            syncManager?.syncPemakaianBahanToCloud(updatedItem)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Pemakaian cloud sync notice: ${e.message}")
        }
        return rowId
    }

    suspend fun deletePemakaianBahan(item: RiwayatPemakaianBahan) {
        financeDao.deletePemakaianBahan(item)
        try {
            syncManager?.deletePemakaianBahanFromCloud(item.idPemakaian)
        } catch (e: Throwable) {
            Log.i("FinanceRepository", "Pemakaian cloud delete notice: ${e.message}")
        }
    }

    suspend fun syncAllToCloud() {
        syncManager?.syncAllToCloud()
    }

    suspend fun syncProfileToCloud(adminName: String, tagline: String, avatarType: String, avatarUri: String) {
        syncManager?.syncProfileToCloud(adminName, tagline, avatarType, avatarUri)
    }
}
