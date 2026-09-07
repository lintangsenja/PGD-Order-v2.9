package com.example.ui.viewmodel

import android.content.Context
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.MasterAkunSaldo
import com.example.data.model.MutasiManualKeluarMasuk
import com.example.data.model.TransaksiOrderMasuk
import com.example.data.model.MasterPelanggan
import com.example.data.model.MasterSatuanHarga
import com.example.data.model.CustomerFrequency
import com.example.data.model.InventarisBahanBaku
import com.example.data.model.TransaksiBelanjaInventaris
import com.example.data.model.RiwayatPemakaianBahan
import com.example.data.repository.FinanceRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

data class UserProfile(
    val adminName: String = "PGD Order",
    val tagline: String = "Sistem Manajemen Keuangan",
    val avatarType: String = "avatar_admin",
    val avatarUri: String = ""
)

data class AccountDashboardRow(
    val idAkun: Int,
    val namaAkun: String,
    val saldoTerplotting: Double,
    val mutasiPenyesuain: Double,
    val sisaSaldoRiil: Double,
    val mutasiMasuk: Double = 0.0,
    val mutasiKeluar: Double = 0.0,
    val totalAlokasiMasuk: Double = saldoTerplotting + mutasiMasuk,
    val saldoAwal: Double = 0.0
)

data class AllocationComparisonItem(
    val idAkun: Int,
    val namaAkun: String,
    val totalMasukPlotting: Double,
    val totalKeluarRiil: Double,
    val sisaSaldo: Double,
    val persentaseSerapan: Double,
    val saldoAwal: Double = 0.0,
    val mutasiMasuk: Double = 0.0
)

data class PosAllocationSummary(
    val items: List<AllocationComparisonItem>,
    val grandTotalMasuk: Double,
    val grandTotalKeluar: Double,
    val grandTotalSisa: Double,
    val averageSerapan: Double,
    val filterLabel: String,
    val startDate: String,
    val endDate: String
)

data class DashboardSummary(
    val rows: List<AccountDashboardRow>,
    val grandTotalPlotting: Double,
    val grandTotalMutasi: Double,
    val grandTotalSisaRiil: Double,
    val grandTotalMutasiMasuk: Double = 0.0,
    val grandTotalMutasiKeluar: Double = 0.0,
    val grandTotalAlokasiDanMasuk: Double = grandTotalPlotting + grandTotalMutasiMasuk,
    val grandTotalSaldoAwal: Double = 0.0
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository

    val isSyncing: StateFlow<Boolean>
    val isCloudOnline: StateFlow<Boolean>
    val syncStatusText: StateFlow<String>
    val cloudLastSyncTime: StateFlow<String>

    val allAccounts: StateFlow<List<MasterAkunSaldo>>
    val allOrders: StateFlow<List<TransaksiOrderMasuk>>
    val notaOrders: StateFlow<List<TransaksiOrderMasuk>>
    val allMutations: StateFlow<List<MutasiManualKeluarMasuk>>
    val allPelanggan: StateFlow<List<MasterPelanggan>>
    val allSatuanHarga: StateFlow<List<MasterSatuanHarga>>
    val allInventaris: StateFlow<List<InventarisBahanBaku>>
    val allBelanjaInventaris: StateFlow<List<TransaksiBelanjaInventaris>>
    val allPemakaianBahan: StateFlow<List<RiwayatPemakaianBahan>>

    // Date range for reports
    val reportStartDate = MutableStateFlow(getStartOfMonthString())
    val reportEndDate = MutableStateFlow(getEndOfMonthString())

    val filteredOrders: StateFlow<List<TransaksiOrderMasuk>>
    val filteredMutations: StateFlow<List<MutasiManualKeluarMasuk>>

    // Combined live state of our budget envelopes
    val dashboardSummary: StateFlow<DashboardSummary>

    // Allocation Comparison State
    val allocationChartFilter = MutableStateFlow("Bulan Ini")
    val allocationCustomStartDate = MutableStateFlow(getStartOfMonthString())
    val allocationCustomEndDate = MutableStateFlow(getEndOfMonthString())
    val allocationComparisonSummary: StateFlow<PosAllocationSummary>

    val customerChartFilter = MutableStateFlow("Bulan Ini")
    val customerOrderFrequency: StateFlow<List<CustomerFrequency>>

    private val prefs = application.getSharedPreferences("vintrack_profile_prefs", Context.MODE_PRIVATE)
    private val _userProfile = MutableStateFlow(loadUserProfileFromPrefs())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Firebase Auth & Anonymous Guest Auth State
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (e: Throwable) { null }

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isGuest = MutableStateFlow(true)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private fun checkAuthSession() {
        val firebaseUser = auth?.currentUser
        val savedIsLoggedIn = prefs.getBoolean("is_logged_in", true)
        val savedIsGuest = prefs.getBoolean("is_guest_mode", true)
        val savedEmail = prefs.getString("user_email", null)

        if (firebaseUser != null) {
            _isLoggedIn.value = true
            _isGuest.value = firebaseUser.isAnonymous
            _userEmail.value = firebaseUser.email ?: if (firebaseUser.isAnonymous) "tamu@pgdorder.app" else null
        } else {
            // Status sesi otomatis dianggap aktif sebagai pengguna tamu (guest) jika belum ada akun yang masuk
            _isLoggedIn.value = savedIsLoggedIn
            _isGuest.value = savedIsGuest
            _userEmail.value = if (savedIsGuest) "tamu@pgdorder.app" else savedEmail
        }

        if (_userProfile.value.adminName == "Pengguna Tamu" || _userProfile.value.tagline == "Akses Tamu (Mode Anonim)") {
            _userProfile.value = UserProfile(
                adminName = "PGD Order",
                tagline = "Pradipta Graha Digital",
                avatarType = _userProfile.value.avatarType,
                avatarUri = _userProfile.value.avatarUri
            )
        }
    }

    fun loginAsGuest(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            try {
                if (auth != null) {
                    try {
                        auth.signInAnonymously().await()
                    } catch (e: Throwable) {
                        Log.w("FinanceViewModel", "Firebase anonymous auth notice: ${e.message}")
                    }
                }
                prefs.edit()
                    .putBoolean("is_guest_mode", true)
                    .putBoolean("is_logged_in", true)
                    .putString("user_email", "tamu@pgdorder.app")
                    .apply()

                _isGuest.value = true
                _isLoggedIn.value = true
                _userEmail.value = "tamu@pgdorder.app"

                saveUserProfile("Pengguna Tamu", "Akses Tamu (Mode Anonim)", "avatar_admin")
                _isAuthLoading.value = false
                onSuccess?.invoke()
            } catch (e: Exception) {
                _isAuthLoading.value = false
                _authError.value = e.localizedMessage ?: "Gagal masuk sebagai tamu"
            }
        }
    }

    fun loginWithEmail(email: String, pass: String, onSuccess: (() -> Unit)? = null) {
        if (email.isBlank() || pass.isBlank()) {
            _authError.value = "Email dan kata sandi wajib diisi"
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            try {
                if (auth != null) {
                    try {
                        auth.signInWithEmailAndPassword(email, pass).await()
                    } catch (e: Throwable) {
                        Log.w("FinanceViewModel", "Firebase email auth notice: ${e.message}")
                    }
                }
                val username = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                prefs.edit()
                    .putBoolean("is_guest_mode", false)
                    .putBoolean("is_logged_in", true)
                    .putString("user_email", email)
                    .apply()

                _isGuest.value = false
                _isLoggedIn.value = true
                _userEmail.value = email

                saveUserProfile(username, "Pengguna Terverifikasi ($email)", "avatar_admin")
                _isAuthLoading.value = false
                onSuccess?.invoke()
            } catch (e: Exception) {
                _isAuthLoading.value = false
                _authError.value = e.localizedMessage ?: "Gagal masuk. Periksa email dan password."
            }
        }
    }

    fun logout() {
        try {
            auth?.signOut()
        } catch (_: Throwable) {}
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .apply()
        _isLoggedIn.value = false
        _authError.value = null
    }

    fun clearAuthError() {
        _authError.value = null
    }

    private fun loadUserProfileFromPrefs(): UserProfile {
        val savedName = prefs.getString("admin_name", "PGD Order") ?: "PGD Order"
        val savedTagline = prefs.getString("tagline", "Pradipta Graha Digital") ?: "Pradipta Graha Digital"
        val cleanName = if (savedName == "Pengguna Tamu" || savedName.isBlank()) "PGD Order" else savedName
        val cleanTagline = if (savedTagline == "Akses Tamu (Mode Anonim)" || savedTagline == "Sistem Manajemen Keuangan" || savedTagline.isBlank()) "Pradipta Graha Digital" else savedTagline
        return UserProfile(
            adminName = cleanName,
            tagline = cleanTagline,
            avatarType = prefs.getString("avatar_type", "avatar_admin") ?: "avatar_admin",
            avatarUri = prefs.getString("avatar_uri", "") ?: ""
        )
    }

    fun saveUserProfile(adminName: String, tagline: String, avatarType: String, avatarUri: String = "") {
        val cleanName = adminName.ifBlank { "PGD Order" }
        val cleanTagline = tagline.ifBlank { "Pradipta Graha Digital" }
        prefs.edit()
            .putString("admin_name", cleanName)
            .putString("tagline", cleanTagline)
            .putString("avatar_type", avatarType)
            .putString("avatar_uri", avatarUri)
            .apply()

        _userProfile.value = UserProfile(
            adminName = cleanName,
            tagline = cleanTagline,
            avatarType = avatarType,
            avatarUri = avatarUri
        )

        // Real-time synchronization to Cloud Firestore
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.syncProfileToCloud(cleanName, cleanTagline, avatarType, avatarUri)
            } catch (e: Throwable) {
                Log.w("FinanceViewModel", "Error syncing profile to Firestore: ${e.message}")
            }
        }
    }

    init {
        checkAuthSession()
        val database = AppDatabase.getDatabase(application)
        val syncManager = com.example.data.firebase.FirestoreSyncManager(application, database.financeDao())
        repository = FinanceRepository(database.financeDao(), syncManager)
        syncManager.startRealtimeListeners(viewModelScope) { adminName, tagline, avatarType, avatarUri ->
            // Synchronize cloud updates in real-time to local StateFlow & Preferences
            val current = _userProfile.value
            if (current.adminName != adminName || current.tagline != tagline || current.avatarType != avatarType || current.avatarUri != avatarUri) {
                prefs.edit()
                    .putString("admin_name", adminName)
                    .putString("tagline", tagline)
                    .putString("avatar_type", avatarType)
                    .putString("avatar_uri", avatarUri)
                    .apply()
                _userProfile.value = UserProfile(
                    adminName = adminName,
                    tagline = tagline,
                    avatarType = avatarType,
                    avatarUri = avatarUri
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = repository.getAllAccountsDirect()
                if (existing.none { it.namaAkun == "Me GpS" }) {
                    repository.insertAccount(
                        MasterAkunSaldo(
                            idAkun = 9,
                            namaAkun = "Me GpS",
                            persentaseOperasional = 0.0f,
                            konstanHppUnit = 0.0f
                        )
                    )
                }

                // Inisialisasi seed data default untuk Master Pelanggan
                repository.seedDefaultCustomers(forceOverwrite = false)

                // Bersihkan total seluruh data sampel inventaris agar modul inventaris bersih tanpa dummy
                repository.cleanSampleInventaris()
                syncManager.cleanSampleInventarisFromCloud()
            } catch (e: Exception) {
                Log.e("FinanceViewModel", "Error initializing master data: ${e.message}")
            }
        }

        isSyncing = repository.isSyncing
        isCloudOnline = repository.isCloudOnline
        syncStatusText = repository.syncStatusText
        cloudLastSyncTime = repository.cloudLastSyncTime

        allAccounts = repository.allAccounts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allOrders = repository.allOrders
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        notaOrders = repository.allOrders
            .map { list -> list.filter { it.isNota } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allMutations = repository.allMutations
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allPelanggan = repository.allPelanggan
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allSatuanHarga = repository.allSatuanHarga
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allInventaris = repository.allInventaris
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allBelanjaInventaris = repository.allBelanjaInventaris
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allPemakaianBahan = repository.allPemakaianBahan
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        filteredOrders = combine(reportStartDate, reportEndDate) { start, end ->
            repository.getOrdersByDateRangeDirect(start, end)
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        filteredMutations = combine(reportStartDate, reportEndDate) { start, end ->
            repository.getMutationsByDateRangeDirect(start, end)
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        dashboardSummary = combine(allAccounts, allOrders, allMutations) { accounts, orders, mutations ->
            // Extract the dynamic rates from MasterAkunSaldo table (with default fallbacks)
            val kertasHpp = accounts.find { it.namaAkun.contains("Kertas", ignoreCase = true) }?.konstanHppUnit?.toDouble() ?: 106.0
            val tintaHpp = accounts.find { it.namaAkun.contains("Tinta", ignoreCase = true) }?.konstanHppUnit?.toDouble() ?: 25.0
            val pengemasanHpp = accounts.find { it.namaAkun.contains("Pengemasan", ignoreCase = true) }?.konstanHppUnit?.toDouble() ?: 300.0
            val wastePct = accounts.find { it.namaAkun.contains("Waste", ignoreCase = true) || it.namaAkun.contains("Rusak", ignoreCase = true) }?.persentaseOperasional?.toDouble() ?: 0.05
            val tenagaKerjaPct = accounts.find { it.namaAkun.contains("Tenaga", ignoreCase = true) || it.namaAkun.contains("Gaji", ignoreCase = true) }?.persentaseOperasional?.toDouble() ?: 0.07
            val listrikPct = accounts.find { it.namaAkun.contains("Listrik", ignoreCase = true) }?.persentaseOperasional?.toDouble() ?: 0.02
            val maintenancePct = accounts.find { it.namaAkun.contains("Maintenance", ignoreCase = true) || it.namaAkun.contains("Alat", ignoreCase = true) }?.persentaseOperasional?.toDouble() ?: 0.05

            val rows = accounts.map { account ->
                // Uang riil yang dibayarkan ke kas menjadi acuan utama autoplotting ke pos dompet secara proporsional
                val ordersWithPayment = orders.filter { it.effectiveJumlahDibayar > 0.0 }
                val name = account.namaAkun

                // 1. Calculate Saldo Terplotting based on autoplotting triggers with dynamic configuration
                val rawSaldoTerplotting = when {
                    name.contains("Kertas", ignoreCase = true) -> ordersWithPayment.sumOf { it.qtyOrder.toDouble() * kertasHpp * it.paymentRatio }
                    name.contains("Tinta", ignoreCase = true) -> ordersWithPayment.sumOf { it.qtyOrder.toDouble() * tintaHpp * it.paymentRatio }
                    name.contains("Pengemasan", ignoreCase = true) -> ordersWithPayment.sumOf { it.jumlahPlastikPengemasan.toDouble() * pengemasanHpp * it.paymentRatio }
                    name.contains("Waste", ignoreCase = true) || name.contains("Rusak", ignoreCase = true) -> ordersWithPayment.sumOf { wastePct * it.effectiveJumlahDibayar }
                    name.contains("Tenaga", ignoreCase = true) || name.contains("Gaji", ignoreCase = true) -> ordersWithPayment.sumOf { tenagaKerjaPct * it.effectiveJumlahDibayar }
                    name.contains("Listrik", ignoreCase = true) -> ordersWithPayment.sumOf { listrikPct * it.effectiveJumlahDibayar }
                    name.contains("Maintenance", ignoreCase = true) || name.contains("Alat", ignoreCase = true) -> ordersWithPayment.sumOf { maintenancePct * it.effectiveJumlahDibayar }
                    name.contains("Laba", ignoreCase = true) -> ordersWithPayment.sumOf { order ->
                        val paid = order.effectiveJumlahDibayar
                        val ratio = order.paymentRatio
                        val alokasiKertasVal = order.qtyOrder.toDouble() * kertasHpp * ratio
                        val alokasiTintaVal = order.qtyOrder.toDouble() * tintaHpp * ratio
                        val alokasiPengemasanVal = order.jumlahPlastikPengemasan.toDouble() * pengemasanHpp * ratio
                        val alokasiWasteVal = wastePct * paid
                        val alokasiTenagaKerjaVal = tenagaKerjaPct * paid
                        val alokasiListrikVal = listrikPct * paid
                        val alokasiMaintenanceVal = maintenancePct * paid
                        val totalModalDasar = alokasiKertasVal + alokasiTintaVal + alokasiPengemasanVal + alokasiWasteVal + alokasiTenagaKerjaVal + alokasiListrikVal + alokasiMaintenanceVal
                        paid - totalModalDasar
                    }
                    else -> 0.0 // Account like 'Me UP GpS' starts at 0 and is adjusted manually
                }
                val saldoTerplotting = kotlin.math.round(rawSaldoTerplotting)

                // 2. Calculate Mutasi Masuk and Mutasi Keluar
                val mutasiMasuk = kotlin.math.round(mutations.filter {
                    ((it.jenisMutasi == "Uang Masuk" || it.jenisMutasi.equals("Masuk", ignoreCase = true)) && it.idAkun == account.idAkun) ||
                    (it.jenisMutasi == "Pindah Saldo" && it.idAkunTujuan == account.idAkun)
                }.sumOf { it.nominal })

                val mutasiKeluar = kotlin.math.round(mutations.filter {
                    ((it.jenisMutasi == "Uang Keluar" || it.jenisMutasi.equals("Keluar", ignoreCase = true)) && it.idAkun == account.idAkun) ||
                    (it.jenisMutasi == "Pindah Saldo" && it.idAkun == account.idAkun)
                }.sumOf { it.nominal })

                val mutasiPenyesuain = mutasiMasuk - mutasiKeluar

                // 3. Saldo Awal + Saldo Terplotting + Mutasi Masuk - Mutasi Keluar
                val saldoAwal = kotlin.math.round(account.saldoAwal)
                val totalAlokasiMasuk = kotlin.math.round(saldoAwal + saldoTerplotting + mutasiMasuk)
                val sisaSaldoRiil = kotlin.math.round(totalAlokasiMasuk - mutasiKeluar)

                AccountDashboardRow(
                    idAkun = account.idAkun,
                    namaAkun = account.namaAkun,
                    saldoTerplotting = saldoTerplotting,
                    mutasiPenyesuain = mutasiPenyesuain,
                    sisaSaldoRiil = sisaSaldoRiil,
                    mutasiMasuk = mutasiMasuk,
                    mutasiKeluar = mutasiKeluar,
                    totalAlokasiMasuk = totalAlokasiMasuk,
                    saldoAwal = saldoAwal
                )
            }

            val grandTotalPlotting = rows.sumOf { it.saldoTerplotting }
            val grandTotalSaldoAwal = rows.sumOf { it.saldoAwal }
            val grandTotalMutasiMasuk = rows.sumOf { it.mutasiMasuk }
            val grandTotalMutasiKeluar = rows.sumOf { it.mutasiKeluar }
            val grandTotalMutasi = grandTotalMutasiMasuk - grandTotalMutasiKeluar
            val grandTotalAlokasiDanMasuk = grandTotalSaldoAwal + grandTotalPlotting + grandTotalMutasiMasuk
            val grandTotalSisaRiil = grandTotalAlokasiDanMasuk - grandTotalMutasiKeluar

            DashboardSummary(
                rows = rows,
                grandTotalPlotting = grandTotalPlotting,
                grandTotalMutasi = grandTotalMutasi,
                grandTotalSisaRiil = grandTotalSisaRiil,
                grandTotalMutasiMasuk = grandTotalMutasiMasuk,
                grandTotalMutasiKeluar = grandTotalMutasiKeluar,
                grandTotalAlokasiDanMasuk = grandTotalAlokasiDanMasuk,
                grandTotalSaldoAwal = grandTotalSaldoAwal
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DashboardSummary(emptyList(), 0.0, 0.0, 0.0)
        )

        allocationComparisonSummary = combine(
            allAccounts,
            allOrders,
            allMutations,
            combine(allocationChartFilter, allocationCustomStartDate, allocationCustomEndDate) { f, s, e ->
                Triple(f, s, e)
            }
        ) { accounts, orders, mutations, dateFilterInfo ->
            val (filter, customStart, customEnd) = dateFilterInfo
            val (startDate, endDate) = when (filter) {
                "Bulan Ini" -> Pair(getStartOfMonthString(), getEndOfMonthString())
                "Bulan Lalu" -> Pair(getStartOfLastMonthString(), getEndOfLastMonthString())
                "Semua Waktu" -> Pair("", "")
                "Kustom" -> Pair(customStart, customEnd)
                else -> Pair(getStartOfMonthString(), getEndOfMonthString())
            }

            fun inRange(dateStr: String): Boolean {
                if (startDate.isBlank() && endDate.isBlank()) return true
                if (startDate.isNotBlank() && dateStr < startDate) return false
                if (endDate.isNotBlank() && dateStr > endDate) return false
                return true
            }

            val filteredOrdersWithPayment = orders.filter { inRange(it.tanggalOrder) && it.effectiveJumlahDibayar > 0.0 }
            val filteredMuts = mutations.filter { inRange(it.tanggalMutasi) }

            val kertasHpp = accounts.find { it.namaAkun.contains("Kertas", ignoreCase = true) }?.konstanHppUnit?.toDouble() ?: 106.0
            val tintaHpp = accounts.find { it.namaAkun.contains("Tinta", ignoreCase = true) }?.konstanHppUnit?.toDouble() ?: 25.0
            val pengemasanHpp = accounts.find { it.namaAkun.contains("Pengemasan", ignoreCase = true) }?.konstanHppUnit?.toDouble() ?: 300.0
            val wastePct = accounts.find { it.namaAkun.contains("Waste", ignoreCase = true) || it.namaAkun.contains("Rusak", ignoreCase = true) }?.persentaseOperasional?.toDouble() ?: 0.05
            val tenagaKerjaPct = accounts.find { it.namaAkun.contains("Tenaga", ignoreCase = true) || it.namaAkun.contains("Gaji", ignoreCase = true) }?.persentaseOperasional?.toDouble() ?: 0.07
            val listrikPct = accounts.find { it.namaAkun.contains("Listrik", ignoreCase = true) }?.persentaseOperasional?.toDouble() ?: 0.02
            val maintenancePct = accounts.find { it.namaAkun.contains("Maintenance", ignoreCase = true) || it.namaAkun.contains("Alat", ignoreCase = true) }?.persentaseOperasional?.toDouble() ?: 0.05

            val items = accounts.map { account ->
                val name = account.namaAkun

                // Plotting within period (or all time if "Semua Waktu") proporsional berdasarkan uang riil yang dibayarkan
                val rawMasukPlotting = when {
                    name.contains("Kertas", ignoreCase = true) -> filteredOrdersWithPayment.sumOf { it.qtyOrder.toDouble() * kertasHpp * it.paymentRatio }
                    name.contains("Tinta", ignoreCase = true) -> filteredOrdersWithPayment.sumOf { it.qtyOrder.toDouble() * tintaHpp * it.paymentRatio }
                    name.contains("Pengemasan", ignoreCase = true) -> filteredOrdersWithPayment.sumOf { it.jumlahPlastikPengemasan.toDouble() * pengemasanHpp * it.paymentRatio }
                    name.contains("Waste", ignoreCase = true) || name.contains("Rusak", ignoreCase = true) -> filteredOrdersWithPayment.sumOf { wastePct * it.effectiveJumlahDibayar }
                    name.contains("Tenaga", ignoreCase = true) || name.contains("Gaji", ignoreCase = true) -> filteredOrdersWithPayment.sumOf { tenagaKerjaPct * it.effectiveJumlahDibayar }
                    name.contains("Listrik", ignoreCase = true) -> filteredOrdersWithPayment.sumOf { listrikPct * it.effectiveJumlahDibayar }
                    name.contains("Maintenance", ignoreCase = true) || name.contains("Alat", ignoreCase = true) -> filteredOrdersWithPayment.sumOf { maintenancePct * it.effectiveJumlahDibayar }
                    name.contains("Laba", ignoreCase = true) -> filteredOrdersWithPayment.sumOf { order ->
                        val paid = order.effectiveJumlahDibayar
                        val ratio = order.paymentRatio
                        val alokasiKertasVal = order.qtyOrder.toDouble() * kertasHpp * ratio
                        val alokasiTintaVal = order.qtyOrder.toDouble() * tintaHpp * ratio
                        val alokasiPengemasanVal = order.jumlahPlastikPengemasan.toDouble() * pengemasanHpp * ratio
                        val alokasiWasteVal = wastePct * paid
                        val alokasiTenagaKerjaVal = tenagaKerjaPct * paid
                        val alokasiListrikVal = listrikPct * paid
                        val alokasiMaintenanceVal = maintenancePct * paid
                        val totalModalDasar = alokasiKertasVal + alokasiTintaVal + alokasiPengemasanVal + alokasiWasteVal + alokasiTenagaKerjaVal + alokasiListrikVal + alokasiMaintenanceVal
                        paid - totalModalDasar
                    }
                    else -> 0.0
                }
                val masukPlotting = kotlin.math.round(rawMasukPlotting)

                // Mutasi Masuk: Uang Masuk ke akun ini + Pindah Saldo yang masuk ke akun ini (idAkunTujuan)
                val mutasiMasuk = kotlin.math.round(filteredMuts.filter {
                    ((it.jenisMutasi == "Uang Masuk" || it.jenisMutasi.equals("Masuk", ignoreCase = true)) && it.idAkun == account.idAkun) ||
                    (it.jenisMutasi == "Pindah Saldo" && it.idAkunTujuan == account.idAkun)
                }.sumOf { it.nominal })

                // Total Pemasukan / Alokasi = Saldo Awal + Alokasi Plotting Nota + Mutasi Masuk Manual
                val saldoAwal = kotlin.math.round(account.saldoAwal)
                val totalMasuk = kotlin.math.round(saldoAwal + masukPlotting + mutasiMasuk)

                // Mutasi Keluar: Uang Keluar dari akun ini + Pindah Saldo keluar dari akun ini (idAkun)
                val keluarRiil = kotlin.math.round(filteredMuts.filter {
                    ((it.jenisMutasi == "Uang Keluar" || it.jenisMutasi.equals("Keluar", ignoreCase = true)) && it.idAkun == account.idAkun) ||
                    (it.jenisMutasi == "Pindah Saldo" && it.idAkun == account.idAkun)
                }.sumOf { it.nominal })

                // Sisa Saldo Riil = Total Masuk (Saldo Awal + Alokasi + Mutasi Masuk) - Mutasi Keluar
                val sisa = kotlin.math.round(totalMasuk - keluarRiil)
                val serapanPct = if (totalMasuk > 0.0) (keluarRiil / totalMasuk) * 100.0 else if (keluarRiil > 0.0) 100.0 else 0.0

                AllocationComparisonItem(
                    idAkun = account.idAkun,
                    namaAkun = account.namaAkun,
                    totalMasukPlotting = totalMasuk,
                    totalKeluarRiil = keluarRiil,
                    sisaSaldo = sisa,
                    persentaseSerapan = serapanPct,
                    saldoAwal = saldoAwal,
                    mutasiMasuk = mutasiMasuk
                )
            }

            val grandMasuk = items.sumOf { it.totalMasukPlotting }
            val grandKeluar = items.sumOf { it.totalKeluarRiil }
            val grandSisa = grandMasuk - grandKeluar
            val avgSerapan = if (grandMasuk > 0.0) (grandKeluar / grandMasuk) * 100.0 else 0.0

            PosAllocationSummary(
                items = items,
                grandTotalMasuk = grandMasuk,
                grandTotalKeluar = grandKeluar,
                grandTotalSisa = grandSisa,
                averageSerapan = avgSerapan,
                filterLabel = filter,
                startDate = startDate,
                endDate = endDate
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PosAllocationSummary(emptyList(), 0.0, 0.0, 0.0, 0.0, "Bulan Ini", "", "")
        )

        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        customerOrderFrequency = customerChartFilter
            .flatMapLatest { filter ->
                val (start, end) = when (filter) {
                    "Minggu Ini" -> Pair(getStartOfWeekString(), getEndOfWeekString())
                    "Bulan Ini" -> Pair(getStartOfMonthString(), getEndOfMonthString())
                    "Tahun Ini" -> Pair(getStartOfYearString(), getEndOfYearString())
                    else -> Pair(getStartOfMonthString(), getEndOfMonthString())
                }
                repository.getCustomerOrderFrequency(start, end)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
    }

    // Insert order in database with flexible payment (DP, Bayar Sebagian, Bayar Penuh)
    fun insertOrder(
        tanggal: String,
        nama: String,
        qty: Int,
        satuan: String,
        harga: Double,
        plastik: Int,
        status: String,
        kategori: String = "Umum",
        jumlahDibayar: Double = 0.0,
        metodePembayaran: String = "Bayar Penuh"
    ) {
        viewModelScope.launch {
            val total = qty.toDouble() * harga
            val actualPaid = when (metodePembayaran) {
                "Bayar Penuh" -> total
                else -> jumlahDibayar.coerceIn(0.0, total)
            }
            val finalStatus = if (actualPaid >= total && total > 0.0) "Lunas" else if (status.equals("Lunas", ignoreCase = true) && actualPaid >= total) "Lunas" else "Belum Lunas"
            val order = TransaksiOrderMasuk(
                tanggalOrder = tanggal,
                namaPesanan = nama,
                qtyOrder = qty,
                satuan = satuan,
                hargaSatuan = harga,
                jumlahPlastikPengemasan = plastik,
                status = finalStatus,
                kategori = kategori,
                jumlahDibayar = actualPaid,
                metodePembayaran = metodePembayaran
            )
            repository.insertOrder(order)
        }
    }

    // Process follow-up payment (pembayaran susulan / cicilan berikutnya)
    fun addOrderPayment(order: TransaksiOrderMasuk, additionalPayment: Double) {
        viewModelScope.launch {
            val total = order.totalPendapatan
            val currentPaid = order.effectiveJumlahDibayar
            val newPaid = (currentPaid + additionalPayment).coerceIn(0.0, total)
            val finalStatus = if (newPaid >= total) "Lunas" else "Belum Lunas"
            val updatedOrder = order.copy(
                jumlahDibayar = newPaid,
                status = finalStatus,
                metodePembayaran = if (finalStatus == "Lunas") "Bayar Penuh" else "Bayar Sebagian"
            )
            repository.updateOrder(updatedOrder)
        }
    }

    // Delete order
    fun deleteOrder(order: TransaksiOrderMasuk) {
        viewModelScope.launch {
            repository.deleteOrder(order)
        }
    }

    // Update order
    fun updateOrder(order: TransaksiOrderMasuk) {
        viewModelScope.launch {
            repository.updateOrder(order)
        }
    }

    // Insert or update mutation in database
    fun insertMutation(
        tanggal: String,
        idAkun: Int,
        jenis: String,
        nominal: Double,
        keterangan: String,
        idAkunTujuan: Int? = null,
        waktu: String? = null,
        idMutasi: Int = 0
    ) {
        viewModelScope.launch {
            val finalWaktu = waktu ?: SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val normalizedJenis = when {
                jenis.equals("Masuk", ignoreCase = true) || jenis.equals("Uang Masuk", ignoreCase = true) -> "Uang Masuk"
                jenis.equals("Keluar", ignoreCase = true) || jenis.equals("Uang Keluar", ignoreCase = true) -> "Uang Keluar"
                else -> jenis
            }
            val mutation = MutasiManualKeluarMasuk(
                idMutasi = idMutasi,
                tanggalMutasi = tanggal,
                idAkun = idAkun,
                jenisMutasi = normalizedJenis,
                nominal = nominal,
                keterangan = keterangan,
                idAkunTujuan = idAkunTujuan,
                waktuMutasi = finalWaktu
            )
            repository.insertMutation(mutation)
        }
    }

    // Eksekusi penyesuaian audit kas langsung ke pos dompet secara nyata di database
    fun applyAuditAdjustment(
        auditId: String,
        auditNote: String,
        adjustments: List<Triple<Int, String, Double>>, // idAkun, namaAkun, delta
        tanggal: String = getTodayString(),
        oldAuditNote: String = ""
    ) {
        viewModelScope.launch {
            // Bersihkan mutasi lama terkait audit ini jika ada (agar tidak dobel/duplikasi)
            repository.deleteAuditMutations(
                auditId = auditId,
                fallbackNote = auditNote,
                secondaryFallbackNote = oldAuditNote
            )

            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            adjustments.forEach { (idAkun, _, delta) ->
                if (delta != 0.0) {
                    val jenis = if (delta > 0) "Uang Masuk" else "Uang Keluar"
                    val nominal = kotlin.math.abs(delta)
                    val prefixKet = if (delta > 0) "Surplus" else "Defisit"
                    val ket = "[AUDIT_ID:$auditId] Audit Selisih Kas ($prefixKet): ${auditNote.ifBlank { "Penyesuaian fisik kas mandiri" }}"
                    val mutation = MutasiManualKeluarMasuk(
                        tanggalMutasi = tanggal,
                        idAkun = idAkun,
                        jenisMutasi = jenis,
                        nominal = nominal,
                        keterangan = ket,
                        waktuMutasi = timeStr
                    )
                    repository.insertMutation(mutation)
                }
            }
        }
    }

    // Menghitung saldo murni dompet sebelum terdampak oleh penyesuaian audit sesi ini
    fun getPureWalletBalanceBeforeAudit(
        idAkun: Int,
        auditId: String,
        isAdjusted: Boolean,
        fallbackDelta: Double = 0.0,
        auditNote: String = ""
    ): Double {
        val currentAccountRow = dashboardSummary.value.rows.find { it.idAkun == idAkun }
        val currentSystemBalance = currentAccountRow?.sisaSaldoRiil ?: 0.0

        if (!isAdjusted) return currentSystemBalance

        val mutations = allMutations.value
        val linkedMutations = mutations.filter { m ->
            m.idAkun == idAkun && (
                (auditId.isNotBlank() && m.keterangan.contains("[AUDIT_ID:$auditId]")) ||
                (auditNote.isNotBlank() && m.keterangan.contains("Audit Selisih Kas") && m.keterangan.contains(auditNote))
            )
        }

        val appliedAuditDelta = if (linkedMutations.isNotEmpty()) {
            linkedMutations.sumOf { m ->
                if (m.jenisMutasi.equals("Uang Masuk", ignoreCase = true)) m.nominal else -m.nominal
            }
        } else {
            fallbackDelta
        }

        return kotlin.math.round(currentSystemBalance - appliedAuditDelta)
    }

    // Rollback penyesuaian audit kas sehingga saldo dompet dan total kas fisik beranda kembali seimbang
    fun rollbackAuditAdjustment(auditId: String, fallbackNote: String = "") {
        viewModelScope.launch {
            repository.deleteAuditMutations(auditId, fallbackNote = fallbackNote)
        }
    }

    fun updateMutation(mutation: MutasiManualKeluarMasuk) {
        viewModelScope.launch {
            repository.insertMutation(mutation)
        }
    }

    private fun formatCurrency(amount: Double): String {
        val rounded = kotlin.math.round(amount).toLong()
        return "Rp " + String.format(Locale.GERMANY, "%,d", rounded)
    }

    // Delete mutation with bidirectional cascade sync to Belanja Inventaris and InventarisBahanBaku
    fun deleteMutation(mutation: MutasiManualKeluarMasuk) {
        viewModelScope.launch {
            repository.deleteMutation(mutation)

            // Cascade: periksa apakah mutasi ini berasal dari transaksi belanja inventaris
            try {
                val belanjaList = repository.getAllBelanjaInventarisDirect()

                // Cek ID belanja dari tag [BELANJA_INV:X]
                val taggedId = if (mutation.keterangan.contains("[BELANJA_INV:")) {
                    mutation.keterangan.substringAfter("[BELANJA_INV:").substringBefore("]").trim().toIntOrNull()
                } else null

                val matchingBelanja = if (taggedId != null) {
                    belanjaList.find { it.idBelanja == taggedId }
                } else {
                    // Fallback: pencocokan berdasarkan akun, nominal nota/uang keluar, dan kata kunci belanja/nama barang
                    belanjaList.find { b ->
                        b.idAkunKas == mutation.idAkun &&
                        (Math.abs(b.uangKeluarDompet - mutation.nominal) < 1.0 || Math.abs(b.realisasiNotaToko - mutation.nominal) < 1.0) &&
                        (mutation.keterangan.contains(b.namaBarang, ignoreCase = true) || mutation.keterangan.contains("Belanja", ignoreCase = true))
                    }
                }

                if (matchingBelanja != null) {
                    // 1. Hapus riwayat belanja inventaris
                    repository.deleteBelanjaInventaris(matchingBelanja)

                    // 2. Rollback stok fisik di Stok & Valuasi
                    if (matchingBelanja.jumlahTambahStok > 0.0) {
                        val invList = allInventaris.value
                        val targetItem = (if (matchingBelanja.idBarangTerkait != null && matchingBelanja.idBarangTerkait > 0) {
                            invList.find { it.idBarang == matchingBelanja.idBarangTerkait }
                        } else null) ?: invList.find { it.namaBarang.equals(matchingBelanja.namaBarang.trim(), ignoreCase = true) }

                        if (targetItem != null) {
                            val newStok = (targetItem.stokUtuh - matchingBelanja.jumlahTambahStok).coerceAtLeast(0.0)
                            repository.updateInventaris(targetItem.copy(stokUtuh = newStok, updatedAt = getTodayString()))
                            repository.insertPemakaianBahan(
                                RiwayatPemakaianBahan(
                                    tanggal = getTodayString(),
                                    idBarang = targetItem.idBarang,
                                    namaBarang = targetItem.namaBarang,
                                    jenisKoreksi = "Rollback Hapus Mutasi",
                                    nilaiPerubahan = "-${matchingBelanja.jumlahTambahStok} ${targetItem.satuanUtuh}",
                                    keterangan = "Batal belanja via hapus mutasi kas (Rp ${formatCurrency(mutation.nominal)})"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Error cascading delete mutation: ${e.message}")
            }
        }
    }

    // CRUD for MasterPelanggan
    fun insertPelanggan(nama: String, kontak: String?, instansi: String? = null, alamatInstansi: String? = null, npwp: String? = null) {
        viewModelScope.launch {
            repository.insertPelanggan(MasterPelanggan(namaPelanggan = nama, kontak = kontak, instansi = instansi, alamatInstansi = alamatInstansi, npwp = npwp))
        }
    }

    fun updatePelanggan(pelanggan: MasterPelanggan) {
        viewModelScope.launch {
            repository.insertPelanggan(pelanggan)
        }
    }

    fun deletePelanggan(pelanggan: MasterPelanggan) {
        viewModelScope.launch {
            repository.deletePelanggan(pelanggan)
        }
    }

    fun deleteAllPelanggan() {
        viewModelScope.launch {
            repository.deleteAllPelanggan()
        }
    }

    fun resetAndSeedCustomers(forceOverwrite: Boolean = true, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.seedDefaultCustomers(forceOverwrite = forceOverwrite)
            } catch (e: Exception) {
                Log.e("FinanceViewModel", "Error resetting & seeding customers: ${e.message}")
            }
            onComplete?.invoke()
        }
    }

    // CRUD for MasterSatuanHarga
    fun insertSatuanHarga(nama: String, harga: Double) {
        viewModelScope.launch {
            repository.insertSatuanHarga(MasterSatuanHarga(namaSatuan = nama, opsiHargaDefault = harga))
        }
    }

    fun updateSatuanHarga(satuanHarga: MasterSatuanHarga) {
        viewModelScope.launch {
            repository.insertSatuanHarga(satuanHarga)
        }
    }

    fun deleteSatuanHarga(satuanHarga: MasterSatuanHarga) {
        viewModelScope.launch {
            repository.deleteSatuanHarga(satuanHarga)
        }
    }

    // Inventaris & Aset Bahan Baku Methods
    fun insertInventaris(
        nama: String,
        kategori: String,
        stok: Double,
        satuan: String,
        harga: Double,
        kondisi: Int = 100,
        catatan: String = ""
    ) {
        viewModelScope.launch {
            val today = getTodayString()
            val item = InventarisBahanBaku(
                namaBarang = nama,
                kategori = kategori,
                stokUtuh = stok,
                satuanUtuh = satuan,
                hargaSatuanUtuh = harga,
                persentaseKondisi = kondisi,
                catatan = catatan,
                updatedAt = today
            )
            repository.insertInventaris(item)
        }
    }

    fun updateInventaris(item: InventarisBahanBaku) {
        viewModelScope.launch {
            repository.updateInventaris(item.copy(updatedAt = getTodayString()))
        }
    }

    fun deleteInventaris(item: InventarisBahanBaku) {
        viewModelScope.launch {
            repository.deleteInventaris(item)
        }
    }

    fun recordBelanjaInventaris(
        tanggal: String,
        idAkunKas: Int,
        namaAkunKas: String,
        uangKeluarDompet: Double,
        realisasiNotaToko: Double,
        catatanSelisih: String,
        idBarangTerkait: Int?,
        namaBarang: String,
        kategoriBarang: String = "",
        jumlahTambahStok: Double,
        satuan: String,
        potongKasOtomatis: Boolean
    ) {
        viewModelScope.launch {
            val cleanNamaBarang = namaBarang.trim().ifBlank { "Bahan Baku" }
            val selisih = (uangKeluarDompet - realisasiNotaToko).coerceAtLeast(0.0)

            // 1. Integrasi langsung ke Stok & Valuasi (inventaris_bahan_baku)
            val invList = allInventaris.value

            var targetBarangId = idBarangTerkait
            val existing = (if (targetBarangId != null && targetBarangId > 0) {
                invList.find { it.idBarang == targetBarangId }
            } else null) ?: invList.find { it.namaBarang.equals(cleanNamaBarang, ignoreCase = true) }

            val effectiveSatuan = satuan.trim().ifBlank {
                existing?.satuanUtuh ?: when {
                    namaAkunKas.contains("Kertas", ignoreCase = true) || cleanNamaBarang.contains("Kertas", ignoreCase = true) || cleanNamaBarang.contains("HVS", ignoreCase = true) || cleanNamaBarang.contains("SIDU", ignoreCase = true) -> "Rim"
                    namaAkunKas.contains("Tinta", ignoreCase = true) || cleanNamaBarang.contains("Tinta", ignoreCase = true) -> "Botol"
                    namaAkunKas.contains("Pengemasan", ignoreCase = true) || cleanNamaBarang.contains("Plastik", ignoreCase = true) || cleanNamaBarang.contains("Kardus", ignoreCase = true) -> "Pack"
                    else -> "Pcs"
                }
            }

            val effectiveKategori = when {
                kategoriBarang.isNotBlank() -> kategoriBarang
                existing != null -> existing.kategori
                namaAkunKas.contains("Kertas", ignoreCase = true) || cleanNamaBarang.contains("Kertas", ignoreCase = true) || cleanNamaBarang.contains("HVS", ignoreCase = true) || cleanNamaBarang.contains("SIDU", ignoreCase = true) -> "Kertas"
                namaAkunKas.contains("Tinta", ignoreCase = true) || cleanNamaBarang.contains("Tinta", ignoreCase = true) -> "Tinta"
                namaAkunKas.contains("Pengemasan", ignoreCase = true) || cleanNamaBarang.contains("Plastik", ignoreCase = true) || cleanNamaBarang.contains("Kardus", ignoreCase = true) -> "Plastik & Pengemasan"
                else -> "Operasional & Lainnya"
            }

            val effectiveQty = if (jumlahTambahStok > 0.0) jumlahTambahStok else 1.0
            val hargaPerUnit = if (effectiveQty > 0.0 && realisasiNotaToko > 0.0) {
                realisasiNotaToko / effectiveQty
            } else if (existing != null) {
                existing.hargaSatuanUtuh
            } else {
                realisasiNotaToko
            }

            if (existing != null) {
                val newStok = existing.stokUtuh + effectiveQty
                val updatedItem = existing.copy(
                    stokUtuh = newStok,
                    hargaSatuanUtuh = if (hargaPerUnit > 0.0) hargaPerUnit else existing.hargaSatuanUtuh,
                    satuanUtuh = if (existing.satuanUtuh.isNotBlank()) existing.satuanUtuh else effectiveSatuan,
                    updatedAt = tanggal
                )
                repository.updateInventaris(updatedItem)
                targetBarangId = existing.idBarang
                repository.insertPemakaianBahan(
                    RiwayatPemakaianBahan(
                        tanggal = tanggal,
                        idBarang = existing.idBarang,
                        namaBarang = existing.namaBarang,
                        jenisKoreksi = "Tambah Stok Belanja",
                        nilaiPerubahan = "+$effectiveQty ${updatedItem.satuanUtuh}",
                        keterangan = "Pembelian dari $namaAkunKas (Nota: ${formatCurrency(realisasiNotaToko)})"
                    )
                )
            } else {
                val newItem = InventarisBahanBaku(
                    idBarang = 0,
                    namaBarang = cleanNamaBarang,
                    kategori = effectiveKategori,
                    stokUtuh = effectiveQty,
                    satuanUtuh = effectiveSatuan,
                    hargaSatuanUtuh = hargaPerUnit,
                    persentaseKondisi = 100,
                    catatan = "Pembelian awal via $namaAkunKas",
                    updatedAt = tanggal
                )
                val newId = repository.insertInventaris(newItem).toInt()
                targetBarangId = newId
                repository.insertPemakaianBahan(
                    RiwayatPemakaianBahan(
                        tanggal = tanggal,
                        idBarang = newId,
                        namaBarang = cleanNamaBarang,
                        jenisKoreksi = "Stok Awal Belanja",
                        nilaiPerubahan = "+$effectiveQty $effectiveSatuan",
                        keterangan = "Pembelian baru dari $namaAkunKas (Nota: ${formatCurrency(realisasiNotaToko)})"
                    )
                )
            }

            // 2. Simpan Transaksi Belanja Inventaris
            val record = TransaksiBelanjaInventaris(
                tanggal = tanggal,
                idAkunKas = idAkunKas,
                namaAkunKas = namaAkunKas,
                uangKeluarDompet = uangKeluarDompet,
                realisasiNotaToko = realisasiNotaToko,
                selisihUang = selisih,
                catatanSelisih = catatanSelisih,
                idBarangTerkait = targetBarangId,
                namaBarang = cleanNamaBarang,
                jumlahTambahStok = effectiveQty,
                satuan = effectiveSatuan,
                potongKasOtomatis = potongKasOtomatis
            )
            val generatedBelanjaId = repository.insertBelanjaInventaris(record).toInt()

            // 3. Potong Kas Otomatis & Catat Mutasi Kas Keluar
            if (potongKasOtomatis && uangKeluarDompet > 0.0) {
                val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val tagBelanja = "[BELANJA_INV:$generatedBelanjaId]"
                val ket = buildString {
                    append("Belanja Inventaris: $cleanNamaBarang")
                    if (effectiveQty > 0.0) append(" ($effectiveQty $effectiveSatuan)")
                    append(" | Nota: ${formatCurrency(realisasiNotaToko)}")
                    if (selisih > 0.0) {
                        append(" | Sisa: ${formatCurrency(selisih)}")
                        if (catatanSelisih.isNotBlank()) append(" ($catatanSelisih)")
                    }
                    append(" $tagBelanja")
                }
                val mutasi = MutasiManualKeluarMasuk(
                    tanggalMutasi = tanggal,
                    idAkun = idAkunKas,
                    jenisMutasi = "Uang Keluar",
                    nominal = uangKeluarDompet,
                    keterangan = ket,
                    waktuMutasi = now
                )
                repository.insertMutation(mutasi)
            }
        }
    }

    // Update Belanja Inventaris with synchronization of Wallet Balance, Mutation, and Stock & Valuation
    fun updateBelanjaInventaris(
        oldRecord: TransaksiBelanjaInventaris,
        tanggal: String,
        idAkunKas: Int,
        namaAkunKas: String,
        uangKeluarDompet: Double,
        realisasiNotaToko: Double,
        catatanSelisih: String,
        idBarangTerkait: Int?,
        namaBarang: String,
        kategoriBarang: String = "",
        jumlahTambahStok: Double,
        satuan: String,
        potongKasOtomatis: Boolean
    ) {
        viewModelScope.launch {
            val cleanNamaBarang = namaBarang.trim().ifBlank { "Bahan Baku" }
            val selisih = (uangKeluarDompet - realisasiNotaToko).coerceAtLeast(0.0)
            val invList = allInventaris.value

            // 1. Sinkronisasi Stok Fisik di Stok & Valuasi
            val oldItem = (if (oldRecord.idBarangTerkait != null && oldRecord.idBarangTerkait > 0) {
                invList.find { it.idBarang == oldRecord.idBarangTerkait }
            } else null) ?: invList.find { it.namaBarang.equals(oldRecord.namaBarang.trim(), ignoreCase = true) }

            var newTargetBarangId = idBarangTerkait
            val newItem = (if (newTargetBarangId != null && newTargetBarangId > 0) {
                invList.find { it.idBarang == newTargetBarangId }
            } else null) ?: invList.find { it.namaBarang.equals(cleanNamaBarang, ignoreCase = true) }

            val effectiveSatuan = satuan.trim().ifBlank {
                newItem?.satuanUtuh ?: oldItem?.satuanUtuh ?: when {
                    namaAkunKas.contains("Kertas", ignoreCase = true) || cleanNamaBarang.contains("Kertas", ignoreCase = true) || cleanNamaBarang.contains("HVS", ignoreCase = true) || cleanNamaBarang.contains("SIDU", ignoreCase = true) -> "Rim"
                    namaAkunKas.contains("Tinta", ignoreCase = true) || cleanNamaBarang.contains("Tinta", ignoreCase = true) -> "Botol"
                    namaAkunKas.contains("Pengemasan", ignoreCase = true) || cleanNamaBarang.contains("Plastik", ignoreCase = true) || cleanNamaBarang.contains("Kardus", ignoreCase = true) -> "Pack"
                    else -> "Pcs"
                }
            }

            val effectiveKategori = when {
                kategoriBarang.isNotBlank() -> kategoriBarang
                newItem != null -> newItem.kategori
                oldItem != null -> oldItem.kategori
                namaAkunKas.contains("Kertas", ignoreCase = true) || cleanNamaBarang.contains("Kertas", ignoreCase = true) -> "Kertas"
                namaAkunKas.contains("Tinta", ignoreCase = true) || cleanNamaBarang.contains("Tinta", ignoreCase = true) -> "Tinta"
                else -> "Operasional & Lainnya"
            }

            val newQty = if (jumlahTambahStok > 0.0) jumlahTambahStok else 1.0
            val oldQty = oldRecord.jumlahTambahStok

            if (oldItem != null && newItem != null && oldItem.idBarang == newItem.idBarang) {
                // Barang sama: sesuaikan delta kuantitas
                val delta = newQty - oldQty
                val newStok = (oldItem.stokUtuh + delta).coerceAtLeast(0.0)
                val hargaPerUnit = if (newQty > 0.0 && realisasiNotaToko > 0.0) realisasiNotaToko / newQty else oldItem.hargaSatuanUtuh
                val updatedInv = oldItem.copy(
                    stokUtuh = newStok,
                    hargaSatuanUtuh = if (hargaPerUnit > 0.0) hargaPerUnit else oldItem.hargaSatuanUtuh,
                    satuanUtuh = if (oldItem.satuanUtuh.isNotBlank()) oldItem.satuanUtuh else effectiveSatuan,
                    updatedAt = tanggal
                )
                repository.updateInventaris(updatedInv)
                newTargetBarangId = oldItem.idBarang

                if (delta != 0.0) {
                    repository.insertPemakaianBahan(
                        RiwayatPemakaianBahan(
                            tanggal = tanggal,
                            idBarang = oldItem.idBarang,
                            namaBarang = oldItem.namaBarang,
                            jenisKoreksi = "Penyesuaian Edit Belanja",
                            nilaiPerubahan = "${if (delta > 0) "+$delta" else "$delta"} ${updatedInv.satuanUtuh}",
                            keterangan = "Edit kuantitas belanja dari $oldQty menjadi $newQty"
                        )
                    )
                }
            } else {
                // Barang berbeda: rollback barang lama, tambahkan ke barang baru
                if (oldItem != null && oldQty > 0.0) {
                    val rolledBackStok = (oldItem.stokUtuh - oldQty).coerceAtLeast(0.0)
                    repository.updateInventaris(oldItem.copy(stokUtuh = rolledBackStok, updatedAt = getTodayString()))
                    repository.insertPemakaianBahan(
                        RiwayatPemakaianBahan(
                            tanggal = getTodayString(),
                            idBarang = oldItem.idBarang,
                            namaBarang = oldItem.namaBarang,
                            jenisKoreksi = "Rollback Edit Belanja",
                            nilaiPerubahan = "-$oldQty ${oldItem.satuanUtuh}",
                            keterangan = "Pengalihan barang belanja ke $cleanNamaBarang"
                        )
                    )
                }

                if (newItem != null) {
                    val newStok = newItem.stokUtuh + newQty
                    val hargaPerUnit = if (newQty > 0.0 && realisasiNotaToko > 0.0) realisasiNotaToko / newQty else newItem.hargaSatuanUtuh
                    val updatedInv = newItem.copy(
                        stokUtuh = newStok,
                        hargaSatuanUtuh = if (hargaPerUnit > 0.0) hargaPerUnit else newItem.hargaSatuanUtuh,
                        satuanUtuh = if (newItem.satuanUtuh.isNotBlank()) newItem.satuanUtuh else effectiveSatuan,
                        updatedAt = tanggal
                    )
                    repository.updateInventaris(updatedInv)
                    newTargetBarangId = newItem.idBarang
                    repository.insertPemakaianBahan(
                        RiwayatPemakaianBahan(
                            tanggal = tanggal,
                            idBarang = newItem.idBarang,
                            namaBarang = newItem.namaBarang,
                            jenisKoreksi = "Tambah Stok Belanja (Edit)",
                            nilaiPerubahan = "+$newQty ${updatedInv.satuanUtuh}",
                            keterangan = "Pembelian hasil edit riwayat belanja via $namaAkunKas"
                        )
                    )
                } else {
                    val hargaPerUnit = if (newQty > 0.0 && realisasiNotaToko > 0.0) realisasiNotaToko / newQty else realisasiNotaToko
                    val newBarang = InventarisBahanBaku(
                        idBarang = 0,
                        namaBarang = cleanNamaBarang,
                        kategori = effectiveKategori,
                        stokUtuh = newQty,
                        satuanUtuh = effectiveSatuan,
                        hargaSatuanUtuh = hargaPerUnit,
                        persentaseKondisi = 100,
                        catatan = "Pembelian via edit riwayat belanja $namaAkunKas",
                        updatedAt = tanggal
                    )
                    val insertedId = repository.insertInventaris(newBarang).toInt()
                    newTargetBarangId = insertedId
                    repository.insertPemakaianBahan(
                        RiwayatPemakaianBahan(
                            tanggal = tanggal,
                            idBarang = insertedId,
                            namaBarang = cleanNamaBarang,
                            jenisKoreksi = "Stok Awal Belanja (Edit)",
                            nilaiPerubahan = "+$newQty $effectiveSatuan",
                            keterangan = "Bahan baru hasil edit belanja via $namaAkunKas"
                        )
                    )
                }
            }

            // 2. Sinkronisasi Mutasi Kas Dompet
            val tagBelanja = "[BELANJA_INV:${oldRecord.idBelanja}]"
            val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val ket = buildString {
                append("Belanja Inventaris: $cleanNamaBarang")
                if (newQty > 0.0) append(" ($newQty $effectiveSatuan)")
                append(" | Nota: ${formatCurrency(realisasiNotaToko)}")
                if (selisih > 0.0) {
                    append(" | Sisa: ${formatCurrency(selisih)}")
                    if (catatanSelisih.isNotBlank()) append(" ($catatanSelisih)")
                }
                append(" $tagBelanja")
            }

            val mutations = allMutations.value
            val existingMutasi = mutations.find { it.keterangan.contains(tagBelanja) } ?: mutations.find { m ->
                m.jenisMutasi == "Uang Keluar" &&
                m.idAkun == oldRecord.idAkunKas &&
                (Math.abs(m.nominal - oldRecord.uangKeluarDompet) < 1.0 || Math.abs(m.nominal - oldRecord.realisasiNotaToko) < 1.0) &&
                (m.keterangan.contains(oldRecord.namaBarang, ignoreCase = true) || m.keterangan.contains("Belanja", ignoreCase = true))
            }

            if (potongKasOtomatis && uangKeluarDompet > 0.0) {
                if (existingMutasi != null) {
                    repository.updateMutation(
                        existingMutasi.copy(
                            idAkun = idAkunKas,
                            nominal = uangKeluarDompet,
                            tanggalMutasi = tanggal,
                            keterangan = ket
                        )
                    )
                } else {
                    repository.insertMutation(
                        MutasiManualKeluarMasuk(
                            tanggalMutasi = tanggal,
                            idAkun = idAkunKas,
                            jenisMutasi = "Uang Keluar",
                            nominal = uangKeluarDompet,
                            keterangan = ket,
                            waktuMutasi = now
                        )
                    )
                }
            } else {
                if (existingMutasi != null) {
                    repository.deleteMutation(existingMutasi)
                }
            }

            // 3. Update Record Transaksi Belanja
            val updatedRecord = oldRecord.copy(
                tanggal = tanggal,
                idAkunKas = idAkunKas,
                namaAkunKas = namaAkunKas,
                uangKeluarDompet = uangKeluarDompet,
                realisasiNotaToko = realisasiNotaToko,
                selisihUang = selisih,
                catatanSelisih = catatanSelisih,
                idBarangTerkait = newTargetBarangId,
                namaBarang = cleanNamaBarang,
                jumlahTambahStok = newQty,
                satuan = effectiveSatuan,
                potongKasOtomatis = potongKasOtomatis
            )
            repository.updateBelanjaInventaris(updatedRecord)
        }
    }

    // Delete Belanja Inventaris with cascade rollback to Mutasi Kas and Stok & Valuasi
    fun deleteBelanjaInventaris(item: TransaksiBelanjaInventaris) {
        viewModelScope.launch {
            // 1. Hapus entri belanja inventaris
            repository.deleteBelanjaInventaris(item)

            // 2. Cascade: cari dan hapus mutasi kas keluar terkait (refund saldo dompet otomatis)
            try {
                val mutations = allMutations.value
                val tag = "[BELANJA_INV:${item.idBelanja}]"
                val matchingMutasi = mutations.find { it.keterangan.contains(tag) } ?: mutations.find { m ->
                    m.jenisMutasi == "Uang Keluar" &&
                    m.idAkun == item.idAkunKas &&
                    (Math.abs(m.nominal - item.uangKeluarDompet) < 1.0 || Math.abs(m.nominal - item.realisasiNotaToko) < 1.0) &&
                    (m.keterangan.contains(item.namaBarang, ignoreCase = true) || m.keterangan.contains("Belanja", ignoreCase = true))
                }

                if (matchingMutasi != null) {
                    repository.deleteMutation(matchingMutasi)
                }
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Error deleting linked mutation: ${e.message}")
            }

            // 3. Rollback jumlah fisik barang di Stok & Valuasi
            if (item.jumlahTambahStok > 0.0) {
                try {
                    val invList = allInventaris.value
                    val targetInv = (if (item.idBarangTerkait != null && item.idBarangTerkait > 0) {
                        invList.find { it.idBarang == item.idBarangTerkait }
                    } else null) ?: invList.find { it.namaBarang.equals(item.namaBarang.trim(), ignoreCase = true) }

                    if (targetInv != null) {
                        val rolledBackStok = (targetInv.stokUtuh - item.jumlahTambahStok).coerceAtLeast(0.0)
                        repository.updateInventaris(targetInv.copy(stokUtuh = rolledBackStok, updatedAt = getTodayString()))
                        repository.insertPemakaianBahan(
                            RiwayatPemakaianBahan(
                                tanggal = getTodayString(),
                                idBarang = targetInv.idBarang,
                                namaBarang = targetInv.namaBarang,
                                jenisKoreksi = "Rollback Hapus Belanja",
                                nilaiPerubahan = "-${item.jumlahTambahStok} ${targetInv.satuanUtuh}",
                                keterangan = "Pembatalan riwayat belanja ${item.namaBarang} (Refund kas dompet)"
                            )
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FinanceViewModel", "Error rolling back inventory stock: ${e.message}")
                }
            }
        }
    }

    fun koreksiStokInventaris(
        item: InventarisBahanBaku?,
        jenisKoreksi: String?,
        jumlahPerubahan: Double?,
        persentaseBaru: Int?,
        keterangan: String?,
        tanggal: String = getTodayString()
    ) {
        if (item == null) return
        viewModelScope.launch {
            try {
                val safeJenisKoreksi = (jenisKoreksi ?: "Pemakaian Lembaran").trim()
                val safeJumlah = jumlahPerubahan ?: 0.0
                val safePersentase = persentaseBaru ?: item.persentaseKondisi
                val safeKeterangan = (keterangan ?: "").trim()
                val safeTanggal = if (tanggal.isNullOrBlank()) getTodayString() else tanggal
                val safeItemNama = item.namaBarang.ifBlank { "Bahan Baku" }
                val safeSatuan = item.satuanUtuh.ifBlank { "Pcs" }

                when (safeJenisKoreksi) {
                    "Pemakaian Lembaran", "Pemakaian Parsial" -> {
                        val newStok = maxOf(0.0, item.stokUtuh - safeJumlah)
                        repository.updateInventaris(item.copy(stokUtuh = newStok, updatedAt = safeTanggal))
                        val formattedPerubahan = InventarisBahanBaku.formatStokGabungan(safeJumlah, safeSatuan)
                        val penurunanValuasi = (safeJumlah * (item.persentaseKondisi / 100.0) * item.hargaSatuanUtuh).toLong()
                        repository.insertPemakaianBahan(
                            RiwayatPemakaianBahan(
                                tanggal = safeTanggal,
                                idBarang = item.idBarang,
                                namaBarang = safeItemNama,
                                jenisKoreksi = "Pemakaian Lembaran",
                                nilaiPerubahan = "-$formattedPerubahan (-Rp ${String.format(java.util.Locale.US, "%,d", penurunanValuasi).replace(',', '.')})",
                                keterangan = safeKeterangan.ifBlank { "Pemakaian sebagian operasional" }
                            )
                        )
                    }
                    "Kurangi Satuan Utuh" -> {
                        val newStok = maxOf(0.0, item.stokUtuh - safeJumlah)
                        repository.updateInventaris(item.copy(stokUtuh = newStok, updatedAt = safeTanggal))
                        val penurunanValuasi = (safeJumlah * (item.persentaseKondisi / 100.0) * item.hargaSatuanUtuh).toLong()
                        val unitStr = if (safeJumlah % 1.0 == 0.0) String.format(java.util.Locale.US, "%.0f", safeJumlah) else String.format(java.util.Locale.US, "%.2f", safeJumlah)
                        repository.insertPemakaianBahan(
                            RiwayatPemakaianBahan(
                                tanggal = safeTanggal,
                                idBarang = item.idBarang,
                                namaBarang = safeItemNama,
                                jenisKoreksi = "Kurangi Satuan Utuh",
                                nilaiPerubahan = "-$unitStr $safeSatuan (-Rp ${String.format(java.util.Locale.US, "%,d", penurunanValuasi).replace(',', '.')})",
                                keterangan = safeKeterangan.ifBlank { "Pengurangan stok fisik" }
                            )
                        )
                    }
                    "Ubah Persentase" -> {
                        val prev = item.persentaseKondisi
                        repository.updateInventaris(item.copy(persentaseKondisi = safePersentase, updatedAt = safeTanggal))
                        val selisihValuasi = ((safePersentase - prev) / 100.0 * item.stokUtuh * item.hargaSatuanUtuh).toLong()
                        val diffValStr = if (selisihValuasi < 0) "-Rp ${String.format(java.util.Locale.US, "%,d", -selisihValuasi).replace(',', '.')}" else "+Rp ${String.format(java.util.Locale.US, "%,d", selisihValuasi).replace(',', '.')}"
                        repository.insertPemakaianBahan(
                            RiwayatPemakaianBahan(
                                tanggal = safeTanggal,
                                idBarang = item.idBarang,
                                namaBarang = safeItemNama,
                                jenisKoreksi = "Ubah Persentase",
                                nilaiPerubahan = "$prev% -> $safePersentase% ($diffValStr)",
                                keterangan = safeKeterangan.ifBlank { "Koreksi sisa pemakaian" }
                            )
                        )
                    }
                    "Tambah Stok Fisik" -> {
                        val newStok = item.stokUtuh + safeJumlah
                        repository.updateInventaris(item.copy(stokUtuh = newStok, updatedAt = safeTanggal))
                        val kenaikanValuasi = (safeJumlah * (item.persentaseKondisi / 100.0) * item.hargaSatuanUtuh).toLong()
                        val unitStr = if (safeJumlah % 1.0 == 0.0) String.format(java.util.Locale.US, "%.0f", safeJumlah) else String.format(java.util.Locale.US, "%.2f", safeJumlah)
                        repository.insertPemakaianBahan(
                            RiwayatPemakaianBahan(
                                tanggal = safeTanggal,
                                idBarang = item.idBarang,
                                namaBarang = safeItemNama,
                                jenisKoreksi = "Tambah Stok Fisik",
                                nilaiPerubahan = "+$unitStr $safeSatuan (+Rp ${String.format(java.util.Locale.US, "%,d", kenaikanValuasi).replace(',', '.')})",
                                keterangan = safeKeterangan.ifBlank { "Penyesuaian stok gudang" }
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("FinanceViewModel", "Error executing koreksiStokInventaris: ${e.message}", e)
            }
        }
    }

    fun deletePemakaianBahan(item: RiwayatPemakaianBahan?) {
        if (item == null) return
        viewModelScope.launch {
            try {
                repository.deletePemakaianBahan(item)
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Error deleting pemakaian bahan: ${e.message}", e)
            }
        }
    }

    // Update all global financial settings in MasterAkunSaldo table
    // Set Saldo Awal for specific cash pos with real-time Firestore persistence
    fun setSaldoAwalAkun(
        idAkun: Int,
        saldoAwal: Double,
        catatMutasi: Boolean = true,
        keterangan: String = "Saldo Awal Modal",
        tanggal: String = getTodayString()
    ) {
        viewModelScope.launch {
            repository.setSaldoAwal(idAkun, saldoAwal)
            if (catatMutasi && saldoAwal > 0.0) {
                val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val mutation = MutasiManualKeluarMasuk(
                    tanggalMutasi = tanggal,
                    idAkun = idAkun,
                    jenisMutasi = "Uang Masuk",
                    nominal = saldoAwal,
                    keterangan = keterangan,
                    waktuMutasi = now
                )
                repository.insertMutation(mutation)
            }
        }
    }

    // Update all global financial settings in MasterAkunSaldo table with preserved saldoAwal
    fun updateFinancialSettings(
        kertasHpp: Double,
        tintaHpp: Double,
        pengemasanHpp: Double,
        wastePct: Double,
        tenagaKerjaPct: Double,
        listrikPct: Double,
        maintenancePct: Double
    ) {
        viewModelScope.launch {
            val existing = repository.getAllAccountsDirect().associateBy { it.idAkun }
            repository.insertAccount(MasterAkunSaldo(idAkun = 1, namaAkun = "Dompet Kertas", persentaseOperasional = 0.0f, konstanHppUnit = kertasHpp.toFloat(), saldoAwal = existing[1]?.saldoAwal ?: 0.0))
            repository.insertAccount(MasterAkunSaldo(idAkun = 2, namaAkun = "Dompet Tinta", persentaseOperasional = 0.0f, konstanHppUnit = tintaHpp.toFloat(), saldoAwal = existing[2]?.saldoAwal ?: 0.0))
            repository.insertAccount(MasterAkunSaldo(idAkun = 3, namaAkun = "Dompet Pengemasan", persentaseOperasional = 0.0f, konstanHppUnit = pengemasanHpp.toFloat(), saldoAwal = existing[3]?.saldoAwal ?: 0.0))
            repository.insertAccount(MasterAkunSaldo(idAkun = 4, namaAkun = "Dompet Waste / Rusak", persentaseOperasional = wastePct.toFloat(), konstanHppUnit = 0.0f, saldoAwal = existing[4]?.saldoAwal ?: 0.0))
            repository.insertAccount(MasterAkunSaldo(idAkun = 5, namaAkun = "Dompet Tenaga Kerja", persentaseOperasional = tenagaKerjaPct.toFloat(), konstanHppUnit = 0.0f, saldoAwal = existing[5]?.saldoAwal ?: 0.0))
            repository.insertAccount(MasterAkunSaldo(idAkun = 6, namaAkun = "Dompet Listrik", persentaseOperasional = listrikPct.toFloat(), konstanHppUnit = 0.0f, saldoAwal = existing[6]?.saldoAwal ?: 0.0))
            repository.insertAccount(MasterAkunSaldo(idAkun = 7, namaAkun = "Dompet Maintenance", persentaseOperasional = maintenancePct.toFloat(), konstanHppUnit = 0.0f, saldoAwal = existing[7]?.saldoAwal ?: 0.0))
        }
    }

    fun quickPayOrder(order: TransaksiOrderMasuk) {
        viewModelScope.launch {
            repository.updateOrder(
                order.copy(
                    status = "Lunas",
                    jumlahDibayar = order.totalPendapatan,
                    metodePembayaran = "Bayar Penuh"
                )
            )
        }
    }

    fun setAllocationFilter(filter: String) {
        allocationChartFilter.value = filter
    }

    fun setAllocationCustomDateRange(start: String, end: String) {
        allocationCustomStartDate.value = start
        allocationCustomEndDate.value = end
        allocationChartFilter.value = "Kustom"
    }

    // Helper to format date
    fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getStartOfMonthString(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun getEndOfMonthString(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun getStartOfLastMonthString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun getEndOfLastMonthString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun getStartOfWeekString(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun getEndOfWeekString(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.add(Calendar.DATE, 6)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun getStartOfYearString(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun getEndOfYearString(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun setCustomerChartFilter(filter: String) {
        customerChartFilter.value = filter
    }

    fun setReportStartDate(date: String) {
        reportStartDate.value = date
    }

    fun setReportEndDate(date: String) {
        reportEndDate.value = date
    }

    // BACKUP & RESTORE METHODS
    val backupsList = MutableStateFlow<List<BackupFile>>(emptyList())
    val lastSyncTime = MutableStateFlow<String>("Belum Pernah")

    fun triggerCloudSync() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.syncAllToCloud()
        }
    }

    fun loadBackupFiles(context: android.content.Context) {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val files = dir.listFiles()?.filter { it.name.endsWith(".json") } ?: emptyList()
        val list = files.map { file ->
            val sizeKb = file.length() / 1024.0
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateFormatted = sdf.format(Date(file.lastModified()))
            BackupFile(file.name, sizeKb, dateFormatted, file.absolutePath)
        }.sortedByDescending { it.dateFormatted }
        
        backupsList.value = list
        if (list.isNotEmpty()) {
            lastSyncTime.value = list.first().dateFormatted
        }
    }

    fun generateBackupJsonString(): String {
        val json = org.json.JSONObject()
        
        // Orders
        val ordersArray = org.json.JSONArray()
        allOrders.value.forEach { o ->
            val obj = org.json.JSONObject()
            obj.put("idOrder", o.idOrder)
            obj.put("tanggalOrder", o.tanggalOrder)
            obj.put("namaPesanan", o.namaPesanan)
            obj.put("qtyOrder", o.qtyOrder)
            obj.put("satuan", o.satuan)
            obj.put("hargaSatuan", o.hargaSatuan)
            obj.put("jumlahPlastikPengemasan", o.jumlahPlastikPengemasan)
            obj.put("status", o.status)
            obj.put("kategori", o.kategori)
            obj.put("jumlahDibayar", o.effectiveJumlahDibayar)
            obj.put("metodePembayaran", o.metodePembayaran)
            ordersArray.put(obj)
        }
        json.put("transaksi", ordersArray)

        // Pelanggan
        val pelangganArray = org.json.JSONArray()
        allPelanggan.value.forEach { p ->
            val obj = org.json.JSONObject()
            obj.put("idPelanggan", p.idPelanggan)
            obj.put("namaPelanggan", p.namaPelanggan)
            obj.put("kontak", p.kontak ?: "")
            obj.put("instansi", p.instansi ?: "")
            obj.put("alamatInstansi", p.alamatInstansi ?: "")
            obj.put("npwp", p.npwp ?: "")
            pelangganArray.put(obj)
        }
        json.put("pelanggan", pelangganArray)

        // Paket / Satuan Harga
        val paketArray = org.json.JSONArray()
        allSatuanHarga.value.forEach { s ->
            val obj = org.json.JSONObject()
            obj.put("idSatuan", s.idSatuan)
            obj.put("namaSatuan", s.namaSatuan)
            obj.put("opsiHargaDefault", s.opsiHargaDefault)
            paketArray.put(obj)
        }
        json.put("paket", paketArray)

        // Mutations
        val mutasiArray = org.json.JSONArray()
        allMutations.value.forEach { m ->
            val obj = org.json.JSONObject()
            obj.put("idMutasi", m.idMutasi)
            obj.put("tanggalMutasi", m.tanggalMutasi)
            obj.put("idAkun", m.idAkun)
            obj.put("jenisMutasi", m.jenisMutasi)
            obj.put("nominal", m.nominal)
            obj.put("keterangan", m.keterangan)
            if (m.idAkunTujuan != null) {
                obj.put("idAkunTujuan", m.idAkunTujuan)
            }
            obj.put("waktuMutasi", m.waktuMutasi)
            mutasiArray.put(obj)
        }
        json.put("mutasi", mutasiArray)

        // Accounts
        val akunArray = org.json.JSONArray()
        allAccounts.value.forEach { a ->
            val obj = org.json.JSONObject()
            obj.put("idAkun", a.idAkun)
            obj.put("namaAkun", a.namaAkun)
            obj.put("persentaseOperasional", a.persentaseOperasional)
            obj.put("konstanHppUnit", a.konstanHppUnit)
            akunArray.put(obj)
        }
        json.put("akun", akunArray)

        return json.toString(4)
    }

    fun createLocalBackup(context: android.content.Context): Boolean {
        return try {
            val jsonStr = generateBackupJsonString()
            val dir = File(context.filesDir, "backups")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "PGD_Order_Backup_${sdf.format(Date())}.json"
            val file = File(dir, fileName)
            file.writeText(jsonStr)
            loadBackupFiles(context)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteBackupFile(context: android.content.Context, backupFile: BackupFile): Boolean {
        return try {
            val file = File(backupFile.absolutePath)
            if (file.exists()) {
                file.delete()
            }
            loadBackupFiles(context)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFromJsonString(jsonStr: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonStr)
            
            // Restore accounts first
            if (json.has("akun")) {
                val arr = json.getJSONArray("akun")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val item = MasterAkunSaldo(
                        idAkun = obj.getInt("idAkun"),
                        namaAkun = obj.getString("namaAkun"),
                        persentaseOperasional = obj.getDouble("persentaseOperasional").toFloat(),
                        konstanHppUnit = obj.getDouble("konstanHppUnit").toFloat()
                    )
                    repository.insertAccount(item)
                }
            }

            // Restore Pelanggan
            if (json.has("pelanggan")) {
                val arr = json.getJSONArray("pelanggan")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val kontakVal = obj.optString("kontak", "")
                    val instansiVal = obj.optString("instansi", "")
                    val alamatInstansiVal = obj.optString("alamatInstansi", "")
                    val npwpVal = obj.optString("npwp", "")
                    val item = MasterPelanggan(
                        idPelanggan = obj.getInt("idPelanggan"),
                        namaPelanggan = obj.getString("namaPelanggan"),
                        kontak = if (kontakVal.isEmpty()) null else kontakVal,
                        instansi = if (instansiVal.isEmpty()) null else instansiVal,
                        alamatInstansi = if (alamatInstansiVal.isEmpty()) null else alamatInstansiVal,
                        npwp = if (npwpVal.isEmpty()) null else npwpVal
                    )
                    repository.insertPelanggan(item)
                }
            }

            // Restore Paket (MasterSatuanHarga)
            if (json.has("paket")) {
                val arr = json.getJSONArray("paket")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val item = MasterSatuanHarga(
                        idSatuan = obj.getInt("idSatuan"),
                        namaSatuan = obj.getString("namaSatuan"),
                        opsiHargaDefault = obj.getDouble("opsiHargaDefault")
                    )
                    repository.insertSatuanHarga(item)
                }
            }

            // Restore Transaksi
            if (json.has("transaksi")) {
                val arr = json.getJSONArray("transaksi")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val item = TransaksiOrderMasuk(
                        idOrder = obj.getInt("idOrder"),
                        tanggalOrder = obj.getString("tanggalOrder"),
                        namaPesanan = obj.getString("namaPesanan"),
                        qtyOrder = obj.getInt("qtyOrder"),
                        satuan = obj.optString("satuan", "Lembar"),
                        hargaSatuan = obj.getDouble("hargaSatuan"),
                        jumlahPlastikPengemasan = obj.optInt("jumlahPlastikPengemasan", 0),
                        status = obj.getString("status"),
                        kategori = obj.optString("kategori", "Umum"),
                        jumlahDibayar = obj.optDouble("jumlahDibayar", if (obj.optString("status").equals("Lunas", ignoreCase = true)) obj.getDouble("qtyOrder") * obj.getDouble("hargaSatuan") else 0.0),
                        metodePembayaran = obj.optString("metodePembayaran", if (obj.optString("status").equals("Lunas", ignoreCase = true)) "Bayar Penuh" else "Bayar Sebagian")
                    )
                    repository.insertOrder(item)
                }
            }

            // Restore Mutasi
            if (json.has("mutasi")) {
                val arr = json.getJSONArray("mutasi")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val item = MutasiManualKeluarMasuk(
                        idMutasi = obj.getInt("idMutasi"),
                        tanggalMutasi = obj.getString("tanggalMutasi"),
                        idAkun = obj.getInt("idAkun"),
                        jenisMutasi = obj.getString("jenisMutasi"),
                        nominal = obj.getDouble("nominal"),
                        keterangan = obj.getString("keterangan"),
                        idAkunTujuan = if (obj.has("idAkunTujuan")) obj.getInt("idAkunTujuan") else null,
                        waktuMutasi = obj.optString("waktuMutasi", "12:00")
                    )
                    repository.insertMutation(item)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

data class BackupFile(
    val name: String,
    val sizeKb: Double,
    val dateFormatted: String,
    val absolutePath: String
)

class FinanceViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
