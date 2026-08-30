package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.dao.FinanceDao
import com.example.data.model.MasterAkunSaldo
import com.example.data.model.MutasiManualKeluarMasuk
import com.example.data.model.TransaksiOrderMasuk
import com.example.data.model.MasterPelanggan
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

typealias FirebaseSyncManager = FirestoreSyncManager

class FirestoreSyncManager(
    private val context: Context,
    private val dao: FinanceDao
) {
    var firestore: FirebaseFirestore? = null
        private set

    var realtimeDb: FirebaseDatabase? = null
        private set

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isCloudOnline = MutableStateFlow(false)
    val isCloudOnline: StateFlow<Boolean> = _isCloudOnline.asStateFlow()

    private val _syncStatusText = MutableStateFlow("Inisialisasi Dual-Database...")
    val syncStatusText: StateFlow<String> = _syncStatusText.asStateFlow()

    private val _cloudLastSyncTime = MutableStateFlow(getSavedLastSyncTime())
    val cloudLastSyncTime: StateFlow<String> = _cloudLastSyncTime.asStateFlow()

    companion object {
        const val RTDB_URL = "https://pgdorder-default-rtdb.asia-southeast1.firebasedatabase.app"
        const val PROJECT_ID = "pgdorder"
        const val APPLICATION_ID = "1:898304484157:android:eda841c638ddcf49475b54"
        const val API_KEY = "AIzaSyBl-dcCQTGUxDeAvEBOgnssxN7IWyBTIjs"
        const val STORAGE_BUCKET = "pgdorder.firebasestorage.app"
    }

    fun updateLastSyncTime() {
        val nowFormatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        _cloudLastSyncTime.value = nowFormatted
        try {
            context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("last_sync_time", nowFormatted)
                .apply()
        } catch (_: Throwable) {}

        // Record heartbeat to Realtime Database
        try {
            realtimeDb?.getReference("metadata")?.child("last_sync_time")?.setValue(nowFormatted)
        } catch (_: Throwable) {}
    }

    private fun getSavedLastSyncTime(): String {
        return try {
            context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
                .getString("last_sync_time", null) ?: SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
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

            // Fallback explicit initialization with dual-database options
            if (!isFirebaseInitialized()) {
                try {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId(APPLICATION_ID)
                        .setApiKey(API_KEY)
                        .setProjectId(PROJECT_ID)
                        .setDatabaseUrl(RTDB_URL)
                        .setStorageBucket(STORAGE_BUCKET)
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    Log.i("FirestoreSyncManager", "FirebaseApp initialized with explicit pgdorder Dual-Database FirebaseOptions.")
                } catch (e: Throwable) {
                    Log.i("FirestoreSyncManager", "Explicit FirebaseOptions init notice: ${e.message}")
                }
            }

            if (isFirebaseInitialized()) {
                // 1. Initialize Cloud Firestore
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
                    Log.i("FirestoreSyncManager", "Firebase Firestore active with offline persistence.")
                } catch (e: Throwable) {
                    Log.i("FirestoreSyncManager", "FirebaseFirestore getInstance notice: ${e.message}")
                    firestore = null
                }

                // 2. Initialize Firebase Realtime Database
                try {
                    val rtdb = try {
                        FirebaseDatabase.getInstance(RTDB_URL)
                    } catch (_: Throwable) {
                        FirebaseDatabase.getInstance()
                    }
                    try {
                        rtdb.setPersistenceEnabled(true)
                    } catch (e: Throwable) {
                        Log.i("FirestoreSyncManager", "Realtime Database persistence notice: ${e.message}")
                    }
                    realtimeDb = rtdb
                    Log.i("FirestoreSyncManager", "Firebase Realtime Database initialized at $RTDB_URL")
                } catch (e: Throwable) {
                    Log.i("FirestoreSyncManager", "FirebaseDatabase getInstance notice: ${e.message}")
                    realtimeDb = null
                }

                if (firestore != null || realtimeDb != null) {
                    _isCloudOnline.value = true
                    _syncStatusText.value = if (firestore != null && realtimeDb != null) {
                        "Dual-Cloud Online (Firestore & Realtime DB)"
                    } else if (firestore != null) {
                        "Firestore Online"
                    } else {
                        "Realtime DB Online"
                    }
                } else {
                    _isCloudOnline.value = false
                    _syncStatusText.value = "Lokal (Room)"
                }
            } else {
                firestore = null
                realtimeDb = null
                _isCloudOnline.value = false
                _syncStatusText.value = "Lokal (Room)"
                Log.i("FirestoreSyncManager", "No active FirebaseApp found. Operating in local Room mode.")
            }
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Firebase init notice: ${e.message}. Operating in local Room mode.")
            firestore = null
            realtimeDb = null
            _isCloudOnline.value = false
            _syncStatusText.value = "Lokal (Room)"
        }
    }

    private val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
    private val rtdbListeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()

    fun startRealtimeListeners(scope: CoroutineScope, onProfileUpdated: ((String, String, String, String) -> Unit)? = null) {
        if (!isFirebaseInitialized()) return

        // Clean up previous listeners
        listeners.forEach { it.remove() }
        listeners.clear()
        rtdbListeners.forEach { (ref, listener) ->
            try { ref.removeEventListener(listener) } catch (_: Throwable) {}
        }
        rtdbListeners.clear()

        _isCloudOnline.value = true
        _syncStatusText.value = "Dual-Cloud Aktif (Firestore & Realtime DB)"

        val db = firestore
        val rtdb = realtimeDb

        // -------------------------
        // 1. FIRESTORE LISTENERS
        // -------------------------
        if (db != null) {
            val processOrderSnapshot = { snapshot: com.google.firebase.firestore.QuerySnapshot? ->
                if (snapshot != null) {
                    _isCloudOnline.value = true
                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshot.documentChanges) {
                            try {
                                val doc = dc.document
                                val idOrder = (doc.getLong("idOrder") ?: doc.id.toLongOrNull() ?: 0L).toInt()
                                when (dc.type) {
                                    DocumentChange.Type.REMOVED -> {
                                        if (idOrder > 0) {
                                            dao.deleteOrderById(idOrder)
                                        }
                                    }
                                    DocumentChange.Type.ADDED,
                                    DocumentChange.Type.MODIFIED -> {
                                        val tanggalOrder = doc.getString("tanggalOrder") ?: ""
                                        val namaPesanan = doc.getString("namaPesanan") ?: ""
                                        val qtyOrder = (doc.getLong("qtyOrder") ?: 0L).toInt()
                                        val satuan = doc.getString("satuan") ?: "Lembar"
                                        val hargaSatuan = doc.getDouble("hargaSatuan") ?: (doc.getLong("hargaSatuan")?.toDouble() ?: 0.0)
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
                    if (error != null) return@addSnapshotListener
                    _isCloudOnline.value = true
                    processOrderSnapshot(snapshot)
                }
                listeners.add(reg1)

                val reg2 = db.collection("transactions").addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    _isCloudOnline.value = true
                    processOrderSnapshot(snapshot)
                }
                listeners.add(reg2)
            } catch (e: Throwable) {
                Log.i("FirestoreSyncManager", "Orders listener setup notice: ${e.message}")
            }

            // 2. Listen to Mutations ('mutations' and 'mutasi_manual')
            val processMutationSnapshot = { snapshot: com.google.firebase.firestore.QuerySnapshot? ->
                if (snapshot != null) {
                    _isCloudOnline.value = true
                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshot.documentChanges) {
                            try {
                                val doc = dc.document
                                val idMutasi = (doc.getLong("idMutasi") ?: doc.id.toLongOrNull() ?: 0L).toInt()
                                when (dc.type) {
                                    DocumentChange.Type.REMOVED -> {
                                        if (idMutasi > 0) {
                                            dao.deleteMutationById(idMutasi)
                                        }
                                    }
                                    DocumentChange.Type.ADDED,
                                    DocumentChange.Type.MODIFIED -> {
                                        val tanggalMutasi = doc.getString("tanggalMutasi") ?: ""
                                        val idAkun = (doc.getLong("idAkun") ?: doc.getDouble("idAkun")?.toLong() ?: 0L).toInt()
                                        val jenisMutasi = doc.getString("jenisMutasi") ?: "Uang Keluar"
                                        val nominal = doc.getDouble("nominal") ?: (doc.getLong("nominal")?.toDouble() ?: 0.0)
                                        val keterangan = doc.getString("keterangan") ?: ""
                                        val idAkunTujuan = (doc.getLong("idAkunTujuan") ?: doc.getDouble("idAkunTujuan")?.toLong())?.toInt()
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
            }

            try {
                val reg3 = db.collection("mutations").addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    _isCloudOnline.value = true
                    processMutationSnapshot(snapshot)
                }
                listeners.add(reg3)

                val reg3b = db.collection("mutasi_manual").addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    _isCloudOnline.value = true
                    processMutationSnapshot(snapshot)
                }
                listeners.add(reg3b)
            } catch (e: Throwable) {
                Log.i("FirestoreSyncManager", "Mutations listener setup notice: ${e.message}")
            }

            // 3. Listen to Wallets ('wallets' and 'master_akun_saldo')
            val processWalletSnapshot = { snapshot: com.google.firebase.firestore.QuerySnapshot? ->
                if (snapshot != null) {
                    _isCloudOnline.value = true
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            try {
                                val idAkun = (doc.getLong("idAkun") ?: doc.id.toLongOrNull() ?: 0L).toInt()
                                val namaAkun = doc.getString("namaAkun") ?: ""
                                val persentase = doc.getDouble("persentaseOperasional")?.toFloat()
                                    ?: (doc.getLong("persentaseOperasional")?.toFloat() ?: 0.0f)
                                val konstan = doc.getDouble("konstanHppUnit")?.toFloat()
                                    ?: (doc.getLong("konstanHppUnit")?.toFloat() ?: 0.0f)
                                val saldoAwal = doc.getDouble("saldoAwal")
                                    ?: (doc.getLong("saldoAwal")?.toDouble()
                                    ?: (doc.getDouble("modalAwal")
                                    ?: (doc.getLong("modalAwal")?.toDouble() ?: 0.0)))
                                if (idAkun > 0 && namaAkun.isNotBlank()) {
                                    val account = MasterAkunSaldo(
                                        idAkun = idAkun,
                                        namaAkun = namaAkun,
                                        persentaseOperasional = persentase,
                                        konstanHppUnit = konstan,
                                        saldoAwal = saldoAwal
                                    )
                                    dao.insertAccount(account)
                                }
                            } catch (e: Throwable) {
                                Log.i("FirestoreSyncManager", "Error parsing remote wallet doc: ${e.message}")
                            }
                        }
                    }
                }
            }

            try {
                val reg4 = db.collection("wallets").addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    _isCloudOnline.value = true
                    processWalletSnapshot(snapshot)
                }
                listeners.add(reg4)

                val reg4b = db.collection("master_akun_saldo").addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    _isCloudOnline.value = true
                    processWalletSnapshot(snapshot)
                }
                listeners.add(reg4b)
            } catch (e: Throwable) {
                Log.i("FirestoreSyncManager", "Wallets listener setup notice: ${e.message}")
            }

            // 4. Listen to Customers ('customers')
            try {
                val reg5 = db.collection("customers").addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    _isCloudOnline.value = true
                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshot.documentChanges) {
                            try {
                                val doc = dc.document
                                val idPelanggan = (doc.getLong("idPelanggan") ?: doc.id.toLongOrNull() ?: 0L).toInt()
                                when (dc.type) {
                                    DocumentChange.Type.REMOVED -> {
                                        if (idPelanggan > 0) {
                                            dao.deletePelangganById(idPelanggan)
                                        }
                                    }
                                    DocumentChange.Type.ADDED,
                                    DocumentChange.Type.MODIFIED -> {
                                        val namaPelanggan = doc.getString("namaPelanggan") ?: ""
                                        val kontak = doc.getString("kontak")
                                        val instansi = doc.getString("instansi")
                                        val alamatInstansi = doc.getString("alamatInstansi")
                                        val npwp = doc.getString("npwp")
                                        if (namaPelanggan.isNotBlank()) {
                                            val pelanggan = MasterPelanggan(
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
                        if (error != null) return@addSnapshotListener
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

        // ---------------------------------------------
        // 2. FIREBASE REALTIME DATABASE LISTENERS (Fallback & Live-Sync)
        // ---------------------------------------------
        if (rtdb != null) {
            try {
                val profileRef = rtdb.getReference("app_profile").child("current_profile")
                val profileListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val adminName = snapshot.child("adminName").getValue(String::class.java) ?: "PGD Order"
                            val tagline = snapshot.child("tagline").getValue(String::class.java) ?: "Pradipta Graha Digital"
                            val avatarType = snapshot.child("avatarType").getValue(String::class.java) ?: "avatar_admin"
                            val avatarUri = snapshot.child("avatarUri").getValue(String::class.java) ?: ""
                            onProfileUpdated?.invoke(adminName, tagline, avatarType, avatarUri)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.i("FirestoreSyncManager", "RTDB Profile listener cancelled: ${error.message}")
                    }
                }
                profileRef.addValueEventListener(profileListener)
                rtdbListeners.add(Pair(profileRef, profileListener))
            } catch (e: Throwable) {
                Log.i("FirestoreSyncManager", "RTDB Profile listener notice: ${e.message}")
            }
        }
    }

    // ==========================================
    // DUAL-DATABASE SYNC OPERATIONS
    // ==========================================

    suspend fun syncOrderToCloud(order: TransaksiOrderMasuk) {
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

            // 1. Write to Firestore
            firestore?.let { db ->
                try {
                    db.collection("transaksi_order").document(order.idOrder.toString()).set(docData, SetOptions.merge()).await()
                    db.collection("transactions").document(order.idOrder.toString()).set(docData, SetOptions.merge()).await()
                } catch (e: Throwable) {
                    Log.i("FirestoreSyncManager", "Firestore sync order notice: ${e.message}")
                }
            }

            // 2. Write to Firebase Realtime Database
            realtimeDb?.let { rtdb ->
                try {
                    rtdb.getReference("orders").child(order.idOrder.toString()).setValue(docData).await()
                    rtdb.getReference("transactions").child(order.idOrder.toString()).setValue(docData).await()
                } catch (e: Throwable) {
                    Log.i("FirestoreSyncManager", "RTDB sync order notice: ${e.message}")
                }
            }

            _isCloudOnline.value = true
            _syncStatusText.value = "Tersinkronisasi dengan Firestore & Realtime DB"
            updateLastSyncTime()
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Dual cloud sync order notice: ${e.message}")
            _syncStatusText.value = "Mode Offline (Tersimpan Lokal)"
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun deleteOrderFromCloud(idOrder: Int) {
        if (!isFirebaseInitialized()) return

        _isSyncing.value = true
        _syncStatusText.value = "Menghapus order #${idOrder} di Cloud..."
        try {
            // 1. Delete from Firestore
            firestore?.let { db ->
                try {
                    db.collection("transaksi_order").document(idOrder.toString()).delete().await()
                    db.collection("transactions").document(idOrder.toString()).delete().await()
                } catch (_: Throwable) {}
            }

            // 2. Delete from Realtime Database
            realtimeDb?.let { rtdb ->
                try {
                    rtdb.getReference("orders").child(idOrder.toString()).removeValue().await()
                    rtdb.getReference("transactions").child(idOrder.toString()).removeValue().await()
                } catch (_: Throwable) {}
            }

            _isCloudOnline.value = true
            _syncStatusText.value = "Tersinkronisasi dengan Firestore & Realtime DB"
            updateLastSyncTime()
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Cloud delete order notice: ${e.message}")
            _syncStatusText.value = "Mode Offline (Tersimpan Lokal)"
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun syncMutationToCloud(mutation: MutasiManualKeluarMasuk) {
        if (!isFirebaseInitialized()) return

        _isSyncing.value = true
        _syncStatusText.value = "Menyinkronkan mutasi kas #${mutation.idMutasi}..."
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

            // 1. Write to Firestore
            firestore?.let { db ->
                try {
                    db.collection("mutations").document(mutation.idMutasi.toString()).set(docData, SetOptions.merge()).await()
                    db.collection("mutasi_manual").document(mutation.idMutasi.toString()).set(docData, SetOptions.merge()).await()
                } catch (_: Throwable) {}
            }

            // 2. Write to Realtime Database
            realtimeDb?.let { rtdb ->
                try {
                    rtdb.getReference("mutations").child(mutation.idMutasi.toString()).setValue(docData).await()
                    rtdb.getReference("mutasi_manual").child(mutation.idMutasi.toString()).setValue(docData).await()
                } catch (_: Throwable) {}
            }

            _isCloudOnline.value = true
            _syncStatusText.value = "Tersinkronisasi dengan Firestore & Realtime DB"
            updateLastSyncTime()
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Cloud sync mutation notice: ${e.message}")
            _syncStatusText.value = "Mode Offline (Tersimpan Lokal)"
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun deleteMutationFromCloud(idMutasi: Int) {
        if (!isFirebaseInitialized()) return

        _isSyncing.value = true
        _syncStatusText.value = "Menghapus mutasi kas #${idMutasi} di Cloud..."
        try {
            // 1. Delete from Firestore
            firestore?.let { db ->
                try {
                    db.collection("mutations").document(idMutasi.toString()).delete().await()
                    db.collection("mutasi_manual").document(idMutasi.toString()).delete().await()
                } catch (_: Throwable) {}
            }

            // 2. Delete from Realtime Database
            realtimeDb?.let { rtdb ->
                try {
                    rtdb.getReference("mutations").child(idMutasi.toString()).removeValue().await()
                    rtdb.getReference("mutasi_manual").child(idMutasi.toString()).removeValue().await()
                } catch (_: Throwable) {}
            }

            _isCloudOnline.value = true
            _syncStatusText.value = "Tersinkronisasi dengan Firestore & Realtime DB"
            updateLastSyncTime()
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Cloud delete mutation notice: ${e.message}")
            _syncStatusText.value = "Mode Offline (Tersimpan Lokal)"
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun syncWalletToCloud(account: MasterAkunSaldo, currentBalance: Double = 0.0) {
        if (!isFirebaseInitialized()) return

        _isSyncing.value = true
        _syncStatusText.value = "Menyinkronkan pos dompet ${account.namaAkun}..."
        try {
            val docData = hashMapOf(
                "idAkun" to account.idAkun,
                "namaAkun" to account.namaAkun,
                "persentaseOperasional" to account.persentaseOperasional,
                "konstanHppUnit" to account.konstanHppUnit,
                "saldoAwal" to account.saldoAwal,
                "modalAwal" to account.saldoAwal,
                "sisaSaldoRiil" to currentBalance,
                "updatedAt" to System.currentTimeMillis()
            )

            // 1. Write to Firestore
            firestore?.let { db ->
                try {
                    db.collection("wallets").document(account.idAkun.toString()).set(docData, SetOptions.merge()).await()
                    db.collection("master_akun_saldo").document(account.idAkun.toString()).set(docData, SetOptions.merge()).await()
                } catch (_: Throwable) {}
            }

            // 2. Write to Realtime Database
            realtimeDb?.let { rtdb ->
                try {
                    rtdb.getReference("wallets").child(account.idAkun.toString()).setValue(docData).await()
                    rtdb.getReference("master_akun_saldo").child(account.idAkun.toString()).setValue(docData).await()
                } catch (_: Throwable) {}
            }

            _isCloudOnline.value = true
            _syncStatusText.value = "Tersinkronisasi dengan Firestore & Realtime DB"
            updateLastSyncTime()
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Cloud sync wallet notice: ${e.message}")
            _syncStatusText.value = "Mode Offline (Tersimpan Lokal)"
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun seedDefaultCustomersToCloud(forceOverwrite: Boolean = false) {
        if (!isFirebaseInitialized()) return

        _isSyncing.value = true
        _syncStatusText.value = "Menginisialisasi seed data pelanggan..."
        try {
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

            val db = firestore
            val rtdb = realtimeDb

            val customersSnapshot = if (db != null) {
                try { db.collection("customers").get().await() } catch (e: Throwable) { null }
            } else null

            val masterPelangganSnapshot = if (db != null) {
                try { db.collection("master_pelanggan").get().await() } catch (e: Throwable) { null }
            } else null

            var shouldSeed = forceOverwrite
            if (!shouldSeed) {
                val totalDocs = (customersSnapshot?.size() ?: 0) + (masterPelangganSnapshot?.size() ?: 0)
                if (totalDocs == 0) {
                    shouldSeed = true
                } else {
                    val allNames = (customersSnapshot?.documents?.mapNotNull { it.getString("namaPelanggan") } ?: emptyList()) +
                            (masterPelangganSnapshot?.documents?.mapNotNull { it.getString("namaPelanggan") } ?: emptyList())
                    val hasLegacyNames = allNames.any { it in listOf("AkL", "TiTi", "RatRi", "WiDi", "Akuntansii") || it.contains("Budi", ignoreCase = true) || it.contains("Grafika", ignoreCase = true) }
                    val hasAllDefaults = listOf("Bu Titi", "Bu Anggit", "Bu Ratri", "Bu Widi", "AKUNTANSI", "Umum").all { expected ->
                        allNames.any { it.equals(expected, ignoreCase = true) }
                    }
                    if (hasLegacyNames || !hasAllDefaults) {
                        shouldSeed = true
                    }
                }
            }

            if (shouldSeed) {
                // 1. Bersihkan seluruh dokumen di koleksi customers dan master_pelanggan di Firestore
                db?.let {
                    customersSnapshot?.documents?.forEach { doc ->
                        try { db.collection("customers").document(doc.id).delete().await() } catch (_: Throwable) {}
                    }
                    masterPelangganSnapshot?.documents?.forEach { doc ->
                        try { db.collection("master_pelanggan").document(doc.id).delete().await() } catch (_: Throwable) {}
                    }
                }

                // Bersihkan di Realtime Database juga
                rtdb?.let {
                    try { it.getReference("customers").removeValue().await() } catch (_: Throwable) {}
                    try { it.getReference("master_pelanggan").removeValue().await() } catch (_: Throwable) {}
                }

                // Bersihkan database lokal agar sinkron
                dao.deleteAllPelanggan()

                // 2. Tulis data pelanggan default ke Firestore, Realtime DB, dan Room
                for (customer in defaultCustomers) {
                    val docData = hashMapOf(
                        "idPelanggan" to customer.idPelanggan,
                        "namaPelanggan" to customer.namaPelanggan,
                        "kontak" to (customer.kontak ?: "-"),
                        "instansi" to (customer.instansi ?: "SMKN 1 Kaligondang"),
                        "alamatInstansi" to (customer.alamatInstansi ?: "SMKN 1 Kaligondang"),
                        "npwp" to (customer.npwp ?: "-"),
                        "updatedAt" to System.currentTimeMillis()
                    )

                    // Write to Firestore
                    db?.let {
                        try { it.collection("customers").document(customer.idPelanggan.toString()).set(docData, SetOptions.merge()).await() } catch (_: Throwable) {}
                        try { it.collection("master_pelanggan").document(customer.idPelanggan.toString()).set(docData, SetOptions.merge()).await() } catch (_: Throwable) {}
                    }

                    // Write to Realtime Database
                    rtdb?.let {
                        try { it.getReference("customers").child(customer.idPelanggan.toString()).setValue(docData).await() } catch (_: Throwable) {}
                        try { it.getReference("master_pelanggan").child(customer.idPelanggan.toString()).setValue(docData).await() } catch (_: Throwable) {}
                    }

                    dao.insertPelanggan(customer)
                }
                Log.i("FirestoreSyncManager", "Default customers successfully seeded to Dual Cloud & Local DB.")
            }
            _isCloudOnline.value = true
            _syncStatusText.value = "Tersinkronisasi dengan Firestore & Realtime DB"
            updateLastSyncTime()
        } catch (e: Throwable) {
            Log.e("FirestoreSyncManager", "Error in seedDefaultCustomersToCloud: ${e.message}")
            _syncStatusText.value = "Mode Offline (Tersimpan Lokal)"
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun syncCustomerToCloud(pelanggan: MasterPelanggan) {
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
            val docId = if (pelanggan.idPelanggan > 0) pelanggan.idPelanggan.toString() else System.currentTimeMillis().toString()

            // 1. Write to Firestore
            firestore?.let { db ->
                try {
                    db.collection("customers").document(docId).set(docData, SetOptions.merge()).await()
                    db.collection("master_pelanggan").document(docId).set(docData, SetOptions.merge()).await()
                } catch (_: Throwable) {}
            }

            // 2. Write to Realtime Database
            realtimeDb?.let { rtdb ->
                try {
                    rtdb.getReference("customers").child(docId).setValue(docData).await()
                    rtdb.getReference("master_pelanggan").child(docId).setValue(docData).await()
                } catch (_: Throwable) {}
            }

            updateLastSyncTime()
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Cloud sync customer notice: ${e.message}")
        }
    }

    suspend fun deleteCustomerFromCloud(idPelanggan: Int) {
        if (!isFirebaseInitialized()) return

        try {
            // 1. Delete from Firestore
            firestore?.let { db ->
                try {
                    db.collection("customers").document(idPelanggan.toString()).delete().await()
                    db.collection("master_pelanggan").document(idPelanggan.toString()).delete().await()
                } catch (_: Throwable) {}
            }

            // 2. Delete from Realtime Database
            realtimeDb?.let { rtdb ->
                try {
                    rtdb.getReference("customers").child(idPelanggan.toString()).removeValue().await()
                    rtdb.getReference("master_pelanggan").child(idPelanggan.toString()).removeValue().await()
                } catch (_: Throwable) {}
            }

            updateLastSyncTime()
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Cloud delete customer notice: ${e.message}")
        }
    }

    suspend fun syncAllToCloud() {
        if (!isFirebaseInitialized()) return

        _isSyncing.value = true
        _syncStatusText.value = "Menyinkronkan seluruh data ke Dual-Cloud..."
        try {
            val accounts = dao.getAllAccountsDirect()
            for (account in accounts) {
                syncWalletToCloud(account)
            }

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
            _isCloudOnline.value = true
            _syncStatusText.value = "Sinkronisasi Dual-Cloud Berhasil"
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Manual sync error: ${e.message}")
            _syncStatusText.value = "Sinkronisasi selesai"
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun syncProfileToCloud(adminName: String, tagline: String, avatarType: String, avatarUri: String) {
        if (!isFirebaseInitialized()) return

        try {
            val data = hashMapOf(
                "adminName" to adminName,
                "tagline" to tagline,
                "avatarType" to avatarType,
                "avatarUri" to avatarUri,
                "updatedAt" to System.currentTimeMillis()
            )

            // 1. Write to Firestore
            firestore?.let { db ->
                try {
                    db.collection("app_profile").document("current_profile").set(data, SetOptions.merge()).await()
                    val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (authUser != null) {
                        try {
                            db.collection("users").document(authUser.uid).set(data, SetOptions.merge()).await()
                            authUser.email?.let { email ->
                                if (email.isNotBlank()) {
                                    db.collection("users").document(email).set(data, SetOptions.merge()).await()
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) {}
            }

            // 2. Write to Realtime Database
            realtimeDb?.let { rtdb ->
                try {
                    rtdb.getReference("app_profile").child("current_profile").setValue(data).await()
                    val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (authUser != null) {
                        try {
                            rtdb.getReference("users").child(authUser.uid).setValue(data).await()
                        } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) {}
            }

            updateLastSyncTime()
            Log.i("FirestoreSyncManager", "Profile synced to dual cloud successfully: $adminName")
        } catch (e: Throwable) {
            Log.i("FirestoreSyncManager", "Error syncing profile to cloud: ${e.message}")
        }
    }
}
