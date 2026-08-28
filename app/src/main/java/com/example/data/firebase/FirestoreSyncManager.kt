package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.dao.FinanceDao
import com.example.data.model.MasterAkunSaldo
import com.example.data.model.MutasiManualKeluarMasuk
import com.example.data.model.TransaksiOrderMasuk
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(
    private val context: Context,
    private val dao: FinanceDao
) {
    private var firestore: FirebaseFirestore? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isCloudOnline = MutableStateFlow(false)
    val isCloudOnline: StateFlow<Boolean> = _isCloudOnline.asStateFlow()

    private val _syncStatusText = MutableStateFlow("Inisialisasi Firestore...")
    val syncStatusText: StateFlow<String> = _syncStatusText.asStateFlow()

    private val _cloudLastSyncTime = MutableStateFlow(getSavedLastSyncTime())
    val cloudLastSyncTime: StateFlow<String> = _cloudLastSyncTime.asStateFlow()

    fun updateLastSyncTime() {
        val nowFormatted = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        _cloudLastSyncTime.value = nowFormatted
        try {
            context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("last_sync_time", nowFormatted)
                .apply()
        } catch (_: Throwable) {}
    }

    private fun getSavedLastSyncTime(): String {
        return try {
            context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
                .getString("last_sync_time", null) ?: java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        } catch (_: Throwable) {
            "Baru Saja"
        }
    }

    private fun isFirebaseInitialized(): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Throwable) {
            false
        }
    }

    init {
        try {
            if (!isFirebaseInitialized()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Throwable) {
                    Log.i("FirestoreSyncManager", "Standard FirebaseApp init notice: ${e.message}")
                }
            }

            // Fallback explicit initialization if resources are not yet merged
            if (!isFirebaseInitialized()) {
                try {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:898304484157:android:eda841c638ddcf49475b54")
                        .setApiKey("AIzaSyBl-dcCQTGUxDeAvEBOgnssxN7IWyBTIjs")
                        .setProjectId("pgdorder")
                        .setStorageBucket("pgdorder.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    Log.i("FirestoreSyncManager", "FirebaseApp initialized with explicit pgdorder FirebaseOptions.")
                } catch (e: Throwable) {
                    Log.i("FirestoreSyncManager", "Explicit FirebaseOptions init notice: ${e.message}")
                }
            }

            if (isFirebaseInitialized()) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    try {
                        val settings = FirebaseFirestoreSettings.Builder()
                            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                            .build()
                        db.firestoreSettings = settings
                    } catch (e: Throwable) {
                        Log.i("FirestoreSyncManager", "Firestore settings notice: ${e.message}")
                    }
                    firestore = db
                    _isCloudOnline.value = true
                    _syncStatusText.value = "Firestore Online & Realtime"
                    Log.i("FirestoreSyncManager", "Firebase Firestore active with offline persistence.")
                } catch (e: Throwable) {
                    Log.i("FirestoreSyncManager", "FirebaseFirestore getInstance notice: ${e.message}. Operating in local Room mode.")
                    firestore = null
                    _isCloudOnline.value = false
                    _syncStatusText.value = "Lokal (Room)"
                }
            } else {
                firestore = null
                _isCloudOnline.value = false
                _syncStatusText.value = "Lokal (Room)"
                Log.i("FirestoreSyncManager", "No active FirebaseApp found. Operating in local Room mode.")
            }
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Firebase init notice: ${e.message}. Operating in local Room mode.")
            firestore = null
            _isCloudOnline.value = false
            _syncStatusText.value = "Lokal (Room)"
        }
    }

    private val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    fun startRealtimeListeners(scope: CoroutineScope, onProfileUpdated: ((String, String, String, String) -> Unit)? = null) {
        val db = firestore ?: return
        if (!isFirebaseInitialized()) return

        // Clean up previous listeners to prevent memory leaks or duplicate processing
        listeners.forEach { it.remove() }
        listeners.clear()

        _isCloudOnline.value = true
        _syncStatusText.value = "Tersinkronisasi dengan Firestore"

        // 1. Listen to Orders ('transaksi_order')
        val processOrderSnapshot = { snapshot: com.google.firebase.firestore.QuerySnapshot? ->
            if (snapshot != null) {
                _isCloudOnline.value = true
                scope.launch(Dispatchers.IO) {
                    for (dc in snapshot.documentChanges) {
                        try {
                            val doc = dc.document
                            val idOrder = (doc.getLong("idOrder") ?: doc.id.toLongOrNull() ?: 0L).toInt()

                            when (dc.type) {
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    if (idOrder > 0) {
                                        dao.deleteOrderById(idOrder)
                                    }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    val tanggalOrder = doc.getString("tanggalOrder") ?: ""
                                    val namaPesanan = doc.getString("namaPesanan") ?: ""
                                    val qtyOrder = (doc.getLong("qtyOrder") ?: 0L).toInt()
                                    val satuan = doc.getString("satuan") ?: "Lembar"
                                    val hargaSatuan = doc.getDouble("hargaSatuan") ?: 0.0
                                    val jumlahPlastik = (doc.getLong("jumlahPlastikPengemasan") ?: 0L).toInt()
                                    val status = doc.getString("status") ?: "Belum Lunas"
                                    val kategori = doc.getString("kategori") ?: doc.getString("type") ?: "Umum"

                                    if (idOrder > 0 && namaPesanan.isNotBlank()) {
                                        val order = TransaksiOrderMasuk(
                                            idOrder = idOrder,
                                            tanggalOrder = tanggalOrder,
                                            namaPesanan = namaPesanan,
                                            qtyOrder = qtyOrder,
                                            satuan = satuan,
                                            hargaSatuan = hargaSatuan,
                                            jumlahPlastikPengemasan = jumlahPlastik,
                                            status = status,
                                            kategori = kategori
                                        )
                                        dao.insertOrder(order)
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            Log.i("FirestoreSyncManager", "Error parsing remote order doc: ${e.message}")
                        }
                    }
                }
            }
        }

        try {
            val reg1 = db.collection("transaksi_order").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.i("FirestoreSyncManager", "transaksi_order listener notice: ${error.message}")
                    // With offline persistence active, keep online state connected
                    return@addSnapshotListener
                }
                _isCloudOnline.value = true
                _syncStatusText.value = "Tersinkronisasi dengan Firestore"
                processOrderSnapshot(snapshot)
            }
            listeners.add(reg1)
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "transaksi_order listener setup notice: ${e.message}")
        }

        try {
            val reg2 = db.collection("transactions").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                _isCloudOnline.value = true
                processOrderSnapshot(snapshot)
            }
            listeners.add(reg2)
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Transactions listener setup notice: ${e.message}")
        }

        // 2. Listen to Mutations ('mutations')
        try {
            val reg3 = db.collection("mutations").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.i("FirestoreSyncManager", "Mutations listener notice: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (dc in snapshot.documentChanges) {
                        try {
                            val doc = dc.document
                            val idMutasi = (doc.getLong("idMutasi") ?: doc.id.toLongOrNull() ?: 0L).toInt()

                            when (dc.type) {
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    if (idMutasi > 0) {
                                        dao.deleteMutationById(idMutasi)
                                    }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    val tanggalMutasi = doc.getString("tanggalMutasi") ?: ""
                                    val idAkun = (doc.getLong("idAkun") ?: 0L).toInt()
                                    val jenisMutasi = doc.getString("jenisMutasi") ?: "Uang Keluar"
                                    val nominal = doc.getDouble("nominal") ?: 0.0
                                    val keterangan = doc.getString("keterangan") ?: ""
                                    val idAkunTujuan = doc.getLong("idAkunTujuan")?.toInt()
                                    val waktuMutasi = doc.getString("waktuMutasi") ?: "12:00"

                                    if (idMutasi > 0) {
                                        val mutation = MutasiManualKeluarMasuk(
                                            idMutasi = idMutasi,
                                            tanggalMutasi = tanggalMutasi,
                                            idAkun = idAkun,
                                            jenisMutasi = jenisMutasi,
                                            nominal = nominal,
                                            keterangan = keterangan,
                                            idAkunTujuan = idAkunTujuan,
                                            waktuMutasi = waktuMutasi
                                        )
                                        dao.insertMutation(mutation)
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            Log.i("FirestoreSyncManager", "Error parsing remote mutation doc: ${e.message}")
                        }
                    }
                }
            }
            listeners.add(reg3)
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Mutations listener setup notice: ${e.message}")
        }

        // 3. Listen to Wallets ('wallets')
        try {
            val reg4 = db.collection("wallets").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.i("FirestoreSyncManager", "Wallets listener notice: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        try {
                            val idAkun = (doc.getLong("idAkun") ?: doc.id.toLongOrNull() ?: 0L).toInt()
                            val namaAkun = doc.getString("namaAkun") ?: ""
                            val persentase = doc.getDouble("persentaseOperasional")?.toFloat() ?: 0.0f
                            val konstan = doc.getDouble("konstanHppUnit")?.toFloat() ?: 0.0f

                            if (idAkun > 0 && namaAkun.isNotBlank()) {
                                val account = MasterAkunSaldo(
                                    idAkun = idAkun,
                                    namaAkun = namaAkun,
                                    persentaseOperasional = persentase,
                                    konstanHppUnit = konstan
                                )
                                dao.insertAccount(account)
                            }
                        } catch (e: Throwable) {
                            Log.i("FirestoreSyncManager", "Error parsing remote wallet doc: ${e.message}")
                        }
                    }
                }
            }
            listeners.add(reg4)
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Wallets listener setup notice: ${e.message}")
        }

        // 4. Listen to Customers ('customers')
        try {
            val reg5 = db.collection("customers").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.i("FirestoreSyncManager", "Customers listener notice: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (dc in snapshot.documentChanges) {
                        try {
                            val doc = dc.document
                            val idPelanggan = (doc.getLong("idPelanggan") ?: doc.id.toLongOrNull() ?: 0L).toInt()

                            when (dc.type) {
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    if (idPelanggan > 0) {
                                        dao.deletePelangganById(idPelanggan)
                                    }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    val namaPelanggan = doc.getString("namaPelanggan") ?: ""
                                    val kontak = doc.getString("kontak")
                                    val instansi = doc.getString("instansi")
                                    val alamatInstansi = doc.getString("alamatInstansi")
                                    val npwp = doc.getString("npwp")

                                    if (namaPelanggan.isNotBlank()) {
                                        val pelanggan = com.example.data.model.MasterPelanggan(
                                            idPelanggan = idPelanggan,
                                            namaPelanggan = namaPelanggan,
                                            kontak = kontak,
                                            instansi = instansi,
                                            alamatInstansi = alamatInstansi,
                                            npwp = npwp
                                        )
                                        dao.insertPelanggan(pelanggan)
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            Log.i("FirestoreSyncManager", "Error parsing remote customer doc: ${e.message}")
                        }
                    }
                }
            }
            listeners.add(reg5)
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Customers listener setup notice: ${e.message}")
        }

        // 5. Listen to App Profile ('app_profile/current_profile')
        try {
            val regProfile = db.collection("app_profile").document("current_profile")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.i("FirestoreSyncManager", "Profile listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val adminName = snapshot.getString("adminName") ?: "PGD Order"
                        val tagline = snapshot.getString("tagline") ?: "Pradipta Graha Digital"
                        val avatarType = snapshot.getString("avatarType") ?: "avatar_admin"
                        val avatarUri = snapshot.getString("avatarUri") ?: ""
                        onProfileUpdated?.invoke(adminName, tagline, avatarType, avatarUri)
                    }
                }
            listeners.add(regProfile)
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Profile listener setup notice: ${e.message}")
        }
    }

    suspend fun syncOrderToCloud(order: TransaksiOrderMasuk) {
        val db = firestore ?: return
        if (!isFirebaseInitialized()) return
        _isSyncing.value = true
        _syncStatusText.value = "Menyinkronkan order #${order.idOrder}..."
        try {
            val docData = hashMapOf(
                "idOrder" to order.idOrder,
                "tanggalOrder" to order.tanggalOrder,
                "namaPesanan" to order.namaPesanan,
                "qtyOrder" to order.qtyOrder,
                "satuan" to order.satuan,
                "hargaSatuan" to order.hargaSatuan,
                "jumlahPlastikPengemasan" to order.jumlahPlastikPengemasan,
                "status" to order.status,
                "kategori" to order.kategori,
                "type" to if (order.isNota) "nota" else "umum",
                "totalPendapatan" to order.totalPendapatan,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("transaksi_order").document(order.idOrder.toString()).set(docData).await()
            try { db.collection("transactions").document(order.idOrder.toString()).set(docData).await() } catch (_: Throwable) {}
            _isCloudOnline.value = true
            _syncStatusText.value = "Tersinkronisasi dengan Firestore"
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Cloud sync order notice: ${e.message}")
            _syncStatusText.value = "Mode Offline (Tersimpan Lokal)"
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun deleteOrderFromCloud(idOrder: Int) {
        val db = firestore ?: return
        if (!isFirebaseInitialized()) return
        _isSyncing.value = true
        _syncStatusText.value = "Menghapus order #${idOrder} di Cloud..."
        try {
            db.collection("transaksi_order").document(idOrder.toString()).delete().await()
            try { db.collection("transactions").document(idOrder.toString()).delete().await() } catch (_: Throwable) {}
            _isCloudOnline.value = true
            _syncStatusText.value = "Tersinkronisasi dengan Firestore"
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Cloud delete order notice: ${e.message}")
            _syncStatusText.value = "Mode Offline (Tersimpan Lokal)"
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun syncMutationToCloud(mutation: MutasiManualKeluarMasuk) {
        val db = firestore ?: return
        if (!isFirebaseInitialized()) return
        try {
            val docData = hashMapOf(
                "idMutasi" to mutation.idMutasi,
                "tanggalMutasi" to mutation.tanggalMutasi,
                "idAkun" to mutation.idAkun,
                "jenisMutasi" to mutation.jenisMutasi,
                "nominal" to mutation.nominal,
                "keterangan" to mutation.keterangan,
                "idAkunTujuan" to mutation.idAkunTujuan,
                "waktuMutasi" to mutation.waktuMutasi,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("mutations").document(mutation.idMutasi.toString()).set(docData).await()
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Cloud sync mutation notice: ${e.message}")
        }
    }

    suspend fun deleteMutationFromCloud(idMutasi: Int) {
        val db = firestore ?: return
        if (!isFirebaseInitialized()) return
        try {
            db.collection("mutations").document(idMutasi.toString()).delete().await()
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Cloud delete mutation notice: ${e.message}")
        }
    }

    suspend fun syncWalletToCloud(account: MasterAkunSaldo, currentBalance: Double = 0.0) {
        val db = firestore ?: return
        if (!isFirebaseInitialized()) return
        try {
            val docData = hashMapOf(
                "idAkun" to account.idAkun,
                "namaAkun" to account.namaAkun,
                "persentaseOperasional" to account.persentaseOperasional,
                "konstanHppUnit" to account.konstanHppUnit,
                "sisaSaldoRiil" to currentBalance,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("wallets").document(account.idAkun.toString()).set(docData).await()
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Cloud sync wallet notice: ${e.message}")
        }
    }

    suspend fun syncCustomerToCloud(pelanggan: com.example.data.model.MasterPelanggan) {
        val db = firestore ?: return
        if (!isFirebaseInitialized()) return
        try {
            val docData = hashMapOf(
                "idPelanggan" to pelanggan.idPelanggan,
                "namaPelanggan" to pelanggan.namaPelanggan,
                "kontak" to (pelanggan.kontak ?: ""),
                "instansi" to (pelanggan.instansi ?: ""),
                "alamatInstansi" to (pelanggan.alamatInstansi ?: ""),
                "npwp" to (pelanggan.npwp ?: ""),
                "updatedAt" to System.currentTimeMillis()
            )
            val docId = if (pelanggan.idPelanggan > 0) pelanggan.idPelanggan.toString() else db.collection("customers").document().id
            db.collection("customers").document(docId).set(docData).await()
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Cloud sync customer notice: ${e.message}")
        }
    }

    suspend fun deleteCustomerFromCloud(idPelanggan: Int) {
        val db = firestore ?: return
        if (!isFirebaseInitialized()) return
        try {
            db.collection("customers").document(idPelanggan.toString()).delete().await()
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Cloud delete customer notice: ${e.message}")
        }
    }

    suspend fun syncAllToCloud() {
        val db = firestore ?: return
        if (!isFirebaseInitialized()) return
        _isSyncing.value = true
        _syncStatusText.value = "Menyinkronkan seluruh data ke Cloud..."
        try {
            val orders = dao.getAllOrdersDirect()
            for (order in orders) {
                syncOrderToCloud(order)
            }
            val mutations = dao.getAllMutationsDirect()
            for (mutation in mutations) {
                syncMutationToCloud(mutation)
            }
            val customers = dao.getAllPelangganDirect()
            for (customer in customers) {
                syncCustomerToCloud(customer)
            }
            updateLastSyncTime()
            _syncStatusText.value = "Sinkronisasi Cloud Berhasil"
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Manual sync error: ${e.message}")
            _syncStatusText.value = "Sinkronisasi selesai"
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun syncProfileToCloud(adminName: String, tagline: String, avatarType: String, avatarUri: String) {
        val db = firestore ?: return
        if (!isFirebaseInitialized()) return
        try {
            val profileDoc = db.collection("app_profile").document("current_profile")
            val data = hashMapOf(
                "adminName" to adminName,
                "tagline" to tagline,
                "avatarType" to avatarType,
                "avatarUri" to avatarUri,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            profileDoc.set(data, com.google.firebase.firestore.SetOptions.merge()).await()

            val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (authUser != null) {
                try {
                    db.collection("users").document(authUser.uid).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
                    authUser.email?.let { email ->
                        if (email.isNotBlank()) {
                            db.collection("users").document(email).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
                        }
                    }
                } catch (_: Throwable) {}
            }

            updateLastSyncTime()
            Log.i("FirestoreSyncManager", "Profile synced to cloud successfully: $adminName")
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Error syncing profile to cloud: ${e.message}")
        }
    }
}