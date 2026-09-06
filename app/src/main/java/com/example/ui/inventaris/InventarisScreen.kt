package com.example.ui.inventaris

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InventarisBahanBaku
import com.example.data.model.MasterAkunSaldo
import com.example.data.model.RiwayatPemakaianBahan
import com.example.data.model.TransaksiBelanjaInventaris
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Palet Ungu Minimalis Khas PGD Order
private val PgdPurple = Color(0xFF6A4C93)
private val PgdPurpleLight = Color(0xFFEDE4FF)
private val PgdPurpleDark = Color(0xFF3B2369)
private val PgdSoftBorder = Color(0xFFE4DAF7)
private val PgdLilacBg = Color(0xFFFAF7FD)
private val PgdGreen = Color(0xFF2E7D32)
private val PgdGreenLight = Color(0xFFE8F5E9)
private val PgdOrange = Color(0xFFE65100)
private val PgdOrangeLight = Color(0xFFFFF3E0)

private fun formatRupiah(amount: Double?): String {
    if (amount == null || amount.isNaN() || amount.isInfinite()) return "Rp 0"
    return try {
        "Rp " + String.format(Locale.GERMANY, "%,.0f", amount)
    } catch (e: Exception) {
        "Rp 0"
    }
}

private fun formatNumber(num: Double?): String {
    if (num == null || num.isNaN() || num.isInfinite()) return "0"
    return try {
        if (num % 1.0 == 0.0) {
            String.format(Locale.GERMANY, "%,.0f", num)
        } else {
            String.format(Locale.GERMANY, "%,.1f", num)
        }
    } catch (e: Exception) {
        "0"
    }
}

@Composable
fun InventarisScreen(
    viewModel: FinanceViewModel,
    accounts: List<MasterAkunSaldo>,
    modifier: Modifier = Modifier
) {
    val rawInventarisList by viewModel.allInventaris.collectAsStateWithLifecycle(initialValue = emptyList())
    val rawBelanjaList by viewModel.allBelanjaInventaris.collectAsStateWithLifecycle(initialValue = emptyList())
    val rawPemakaianList by viewModel.allPemakaianBahan.collectAsStateWithLifecycle(initialValue = emptyList())

    val inventarisList = remember(rawInventarisList) { (rawInventarisList ?: emptyList()).filterNotNull() }
    val belanjaList = remember(rawBelanjaList) { (rawBelanjaList ?: emptyList()).filterNotNull() }
    val pemakaianList = remember(rawPemakaianList) { (rawPemakaianList ?: emptyList()).filterNotNull() }
    val safeAccounts = remember(accounts) { (accounts ?: emptyList()).filterNotNull() }

    val tabs = remember {
        listOf(
            Pair("Ringkasan Aset", Icons.Default.Analytics),
            Pair("Stok & Valuasi", Icons.Default.Inventory2),
            Pair("Pembelian & Belanja", Icons.Default.ShoppingCart),
            Pair("Pengeluaran & Koreksi", Icons.Default.Tune)
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // Sinkronisasi tab saat pager digeser (swipe) oleh pengguna
    LaunchedEffect(pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage.coerceIn(0, tabs.size - 1)
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<InventarisBahanBaku?>(null) }
    var itemToKoreksi by remember { mutableStateOf<InventarisBahanBaku?>(null) }
    var itemToDelete by remember { mutableStateOf<InventarisBahanBaku?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PgdLilacBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PgdLilacBg)
        ) {
            // Sub-Menu Navigation Bar (4 Tab Independen & Praktis)
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, PgdSoftBorder),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex.coerceIn(0, tabs.size - 1),
                    edgePadding = 12.dp,
                    containerColor = Color.White,
                    contentColor = PgdPurple,
                    indicator = {},
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, (title, icon) ->
                        val isSelected = selectedTabIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = {
                                selectedTabIndex = index
                                coroutineScope.launch {
                                    try {
                                        pagerState.animateScrollToPage(index)
                                    } catch (_: Exception) {
                                        try {
                                            pagerState.scrollToPage(index)
                                        } catch (_: Exception) {}
                                    }
                                }
                            },
                            modifier = Modifier
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("subtab_inventaris_$index"),
                            text = {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) PgdPurple else Color.Transparent,
                                    border = if (isSelected) null else BorderStroke(1.dp, PgdSoftBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            tint = if (isSelected) Color.White else PgdPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else Color(0xFF4A5568)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Konten Dinamis Berdasarkan Tab Aktif (HorizontalPager dengan sinkronisasi gestur swipe & tap yang aman)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> RingkasanAsetTab(
                        inventarisList = inventarisList,
                        belanjaList = belanjaList,
                        onNavigateToStok = {
                            selectedTabIndex = 1
                            coroutineScope.launch {
                                try {
                                    pagerState.animateScrollToPage(1)
                                } catch (_: Exception) {
                                    try { pagerState.scrollToPage(1) } catch (_: Exception) {}
                                }
                            }
                        },
                        onNavigateToBelanja = {
                            selectedTabIndex = 2
                            coroutineScope.launch {
                                try {
                                    pagerState.animateScrollToPage(2)
                                } catch (_: Exception) {
                                    try { pagerState.scrollToPage(2) } catch (_: Exception) {}
                                }
                            }
                        }
                    )
                    1 -> StokValuasiTab(
                        inventarisList = inventarisList,
                        onAddNewItem = { showAddDialog = true },
                        onEditItem = { itemToEdit = it },
                        onKoreksiItem = { itemToKoreksi = it },
                        onDeleteItem = { itemToDelete = it }
                    )
                    2 -> PembelianBelanjaTab(
                        viewModel = viewModel,
                        accounts = safeAccounts,
                        inventarisList = inventarisList,
                        belanjaList = belanjaList
                    )
                    3 -> PengeluaranKoreksiTab(
                        viewModel = viewModel,
                        inventarisList = inventarisList,
                        pemakaianList = pemakaianList,
                        onKoreksiItem = { itemToKoreksi = it }
                    )
                    else -> Box(Modifier.fillMaxSize())
                }
            }
        }

        // Floating Action Button (Hanya di tab Stok & Valuasi agar rapi dan tidak menghalangi tab lain)
        if (pagerState.currentPage == 1) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PgdPurple,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .testTag("fab_tambah_bahan_baku")
                    .padding(bottom = 16.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Bahan Baku",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // Dialog Tambah Bahan Baku Baru
    if (showAddDialog) {
        FormItemInventarisDialog(
            title = "Tambah Bahan Baku Baru",
            item = null,
            onDismiss = { showAddDialog = false },
            onSave = { nama, kategori, stok, satuan, harga, kondisi, catatan ->
                viewModel.insertInventaris(nama, kategori, stok, satuan, harga, kondisi, catatan)
                showAddDialog = false
            }
        )
    }

    // Dialog Edit Bahan Baku
    itemToEdit?.let { item ->
        FormItemInventarisDialog(
            title = "Edit Data Bahan Baku",
            item = item,
            onDismiss = { itemToEdit = null },
            onSave = { nama, kategori, stok, satuan, harga, kondisi, catatan ->
                viewModel.updateInventaris(
                    item.copy(
                        namaBarang = nama,
                        kategori = kategori,
                        stokUtuh = stok,
                        satuanUtuh = satuan,
                        hargaSatuanUtuh = harga,
                        persentaseKondisi = kondisi,
                        catatan = catatan
                    )
                )
                itemToEdit = null
            }
        )
    }

    // Dialog Koreksi Stok Praktis
    itemToKoreksi?.let { item ->
        KoreksiStokPraktisDialog(
            item = item,
            onDismiss = { itemToKoreksi = null },
            onApply = { jenis, jumlah, persentase, ket ->
                viewModel.koreksiStokInventaris(
                    item = item,
                    jenisKoreksi = jenis,
                    jumlahPerubahan = jumlah,
                    persentaseBaru = persentase,
                    keterangan = ket
                )
                itemToKoreksi = null
            }
        )
    }

    // Dialog Konfirmasi Hapus Item
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Hapus Bahan Baku?", fontWeight = FontWeight.Bold, color = PgdPurpleDark) },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus '${item.namaBarang}' dari daftar inventaris? Data yang dihapus tidak dapat dikembalikan.",
                    color = Color(0xFF4A5568)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInventaris(item)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_inventaris_btn")
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToDelete = null }) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

// -------------------------------------------------------------
// 1. SUB-MENU: RINGKASAN ASET
// -------------------------------------------------------------
@Composable
private fun RingkasanAsetTab(
    inventarisList: List<InventarisBahanBaku>,
    belanjaList: List<TransaksiBelanjaInventaris>,
    onNavigateToStok: () -> Unit,
    onNavigateToBelanja: () -> Unit
) {
    val totalNilaiAset = inventarisList.sumOf { it.nilaiTotalAset }
    val totalUangKeluarBelanja = belanjaList.sumOf { it.uangKeluarDompet }
    val totalRealisasiNota = belanjaList.sumOf { it.realisasiNotaToko }
    val totalSelisihSisa = belanjaList.sumOf { it.selisihUang }

    val kategoriBreakdown = remember(inventarisList) {
        listOf("Kertas", "Tinta", "Plastik & Pengemasan", "Operasional & Lainnya").map { kat ->
            val items = inventarisList.filter { it.kategori.equals(kat, ignoreCase = true) }
            val nilai = items.sumOf { it.nilaiTotalAset }
            val count = items.size
            Triple(kat, nilai, count)
        }
    }

    val stokKritisCount = inventarisList.count { it.persentaseKondisi <= 25 || it.stokUtuh <= 1.0 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kartu Utama: Total Nilai Aset Fisik
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, PgdSoftBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(PgdPurpleLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = PgdPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Valuasi Aset Bahan Baku",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PgdPurpleDark
                                )
                                Text(
                                    text = "Total nilai fisik barang di gudang",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF718096)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PgdPurpleLight,
                            border = BorderStroke(1.dp, PgdSoftBorder)
                        ) {
                            Text(
                                text = "${inventarisList.size} Jenis Barang",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PgdPurple,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = formatRupiah(totalNilaiAset),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = PgdPurple
                    )

                    HorizontalDivider(color = PgdSoftBorder.copy(alpha = 0.6f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Kondisi Menipis / Kritis", style = MaterialTheme.typography.labelSmall, color = Color(0xFF718096))
                            Text(
                                text = if (stokKritisCount > 0) "$stokKritisCount Barang Perlu Dicek" else "Semua Stok Aman",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (stokKritisCount > 0) PgdOrange else PgdGreen
                            )
                        }
                        Button(
                            onClick = onNavigateToStok,
                            colors = ButtonDefaults.buttonColors(containerColor = PgdPurple),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lihat Stok", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Kartu Ringkasan Realisasi Belanja & Pemisahan Dompet
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, PgdSoftBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(PgdGreenLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = PgdGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Realisasi Belanja & Selisih Kas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PgdPurpleDark
                                )
                                Text(
                                    text = "Pemisahan uang dompet vs nota toko",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF718096)
                                )
                            }
                        }

                        IconButton(onClick = onNavigateToBelanja) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Input Belanja", tint = PgdPurple)
                        }
                    }

                    // 3 Kolom Metrik Belanja
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Uang Keluar Dompet
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = PgdPurpleLight.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, PgdSoftBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Ambil Kas", style = MaterialTheme.typography.labelSmall, color = Color(0xFF718096))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatRupiah(totalUangKeluarBelanja),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PgdPurpleDark
                                )
                            }
                        }

                        // Realisasi Nota
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Nota Toko", style = MaterialTheme.typography.labelSmall, color = Color(0xFF718096))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatRupiah(totalRealisasiNota),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PgdGreen
                                )
                            }
                        }

                        // Selisih Fleksibel
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Sisa Fleksibel", style = MaterialTheme.typography.labelSmall, color = Color(0xFF718096))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatRupiah(totalSelisihSisa),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF7FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PgdPurple, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Sisa uang belanja otomatis tercatat fleksibel (bensin/makan/lainnya) tanpa merusak keseimbangan dompet kas.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4A5568)
                            )
                        }
                    }
                }
            }
        }

        // Breakdown Nilai Aset per Kategori
        item {
            Text(
                text = "Valuasi per Kategori Bahan",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = PgdPurpleDark
            )
        }

        items(kategoriBreakdown) { (kat, nilai, count) ->
            val ratio = if (totalNilaiAset > 0) (nilai / totalNilaiAset).toFloat() else 0f
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, PgdSoftBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(kat, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))
                        Text(formatRupiah(nilai), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = PgdPurple)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$count item terdaftar", style = MaterialTheme.typography.bodySmall, color = Color(0xFF718096))
                        Text("${(ratio * 100).toInt()}% dari total aset", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = PgdPurpleDark)
                    }

                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PgdPurple,
                        trackColor = PgdPurpleLight
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. SUB-MENU: STOK & VALUASI
// -------------------------------------------------------------
@Composable
private fun StokValuasiTab(
    inventarisList: List<InventarisBahanBaku>,
    onAddNewItem: () -> Unit,
    onEditItem: (InventarisBahanBaku) -> Unit,
    onKoreksiItem: (InventarisBahanBaku) -> Unit,
    onDeleteItem: (InventarisBahanBaku) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }

    val categories = listOf("Semua", "Kertas", "Tinta", "Plastik & Pengemasan", "Operasional & Lainnya")

    val filteredList = remember(inventarisList, searchQuery, selectedCategoryFilter) {
        inventarisList.filter { item ->
            val matchCategory = selectedCategoryFilter == "Semua" || item.kategori.equals(selectedCategoryFilter, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() || item.namaBarang.contains(searchQuery, ignoreCase = true) || item.catatan.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kolom Pencarian Cepat
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_inventaris_input"),
            placeholder = { Text("Cari bahan baku (kertas, tinta, plastik)...", color = Color(0xFFA0AEC0), style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PgdPurple) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PgdPurple,
                unfocusedBorderColor = PgdSoftBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        // Kategori Chips Filter
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { kat ->
                val isSelected = selectedCategoryFilter == kat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryFilter = kat },
                    label = {
                        Text(
                            text = kat,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PgdPurple,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = Color(0xFF4A5568)
                    ),
                    border = BorderStroke(1.dp, if (isSelected) PgdPurple else PgdSoftBorder),
                    modifier = Modifier.testTag("filter_chip_$kat")
                )
            }
        }

        // Ringkasan Baris Hasil Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredList.size} item ditemukan",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF718096),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Subtotal: " + formatRupiah(filteredList.sumOf { it.nilaiTotalAset }),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = PgdPurple
            )
        }

        // Daftar Kartu Bahan Baku
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = PgdPurple.copy(alpha = 0.35f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        "Belum ada data bahan baku",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A5568)
                    )
                    Text(
                        "Klik tombol (+) di sudut bawah untuk menambahkan stok barang",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF718096),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.idBarang }) { item ->
                    BahanBakuItemCard(
                        item = item,
                        onEdit = { onEditItem(item) },
                        onKoreksi = { onKoreksiItem(item) },
                        onDelete = { onDeleteItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BahanBakuItemCard(
    item: InventarisBahanBaku,
    onEdit: () -> Unit,
    onKoreksi: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, PgdSoftBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bahan_baku_card_${item.idBarang}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Baris: Kategori & Status Kondisi (Sub-judul Jelas & Proporsional)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PgdPurpleLight,
                    border = BorderStroke(1.dp, PgdPurple.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = PgdPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = item.kategori,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PgdPurple
                        )
                    }
                }

                // Badge Kondisi Persentase
                val (kondisiBg, kondisiText) = when {
                    item.persentaseKondisi >= 100 -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
                    item.persentaseKondisi >= 75 -> Pair(Color(0xFFE0F2FE), Color(0xFF0369A1))
                    item.persentaseKondisi >= 50 -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
                    item.persentaseKondisi >= 25 -> Pair(Color(0xFFFFEDD5), Color(0xFFC2410C))
                    else -> Pair(Color(0xFFFFE4E6), Color(0xFFBE123C))
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = kondisiBg,
                    border = BorderStroke(0.8.dp, kondisiText.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(modifier = Modifier.size(7.dp).background(kondisiText, CircleShape))
                        Text(
                            text = "${item.statusKondisiText} (${item.persentaseKondisi}%)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = kondisiText
                        )
                    }
                }
            }

            // Nama Barang
            Text(
                text = item.namaBarang,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A202C)
            )

            // Baris Stok Utuh & Valuasi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PgdLilacBg, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Stok Fisik Tersedia",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF718096)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.statusStokGabungan,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = PgdPurpleDark
                    )
                    if (item.satuanUtuh.equals("Rim", ignoreCase = true) && item.stokUtuh % 1.0 != 0.0) {
                        Text(
                            text = "(~${formatNumber(item.stokUtuh)} Rim)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF718096)
                        )
                    }
                    Text(
                        text = "@ ${formatRupiah(item.hargaSatuanUtuh)} / ${item.satuanUtuh}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = Color(0xFF718096)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Nilai Aset",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF718096)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatRupiah(item.nilaiTotalAset),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = PgdPurple
                    )
                    if (item.persentaseKondisi < 100) {
                        Text(
                            text = "(Terkoreksi ${item.persentaseKondisi}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (item.catatan.isNotBlank()) {
                Text(
                    text = "Catatan: ${item.catatan}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF718096),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tombol Aksi Praktis
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onKoreksi,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PgdPurple),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PgdPurple),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    modifier = Modifier.testTag("koreksi_stok_btn_${item.idBarang}")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Koreksi Pemakaian", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF4A5568))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color(0xFFE53E3E))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. SUB-MENU: PEMBELIAN & REALISASI BELANJA
// -------------------------------------------------------------
@Composable
private fun PembelianBelanjaTab(
    viewModel: FinanceViewModel,
    accounts: List<MasterAkunSaldo>,
    inventarisList: List<InventarisBahanBaku>,
    belanjaList: List<TransaksiBelanjaInventaris>
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var selectedAccountId by remember { mutableIntStateOf(accounts.firstOrNull { it.namaAkun.contains("Kertas", ignoreCase = true) }?.idAkun ?: 1) }
    var uangKeluarText by remember { mutableStateOf("") }
    var notaTokoText by remember { mutableStateOf("") }
    var catatanSelisih by remember { mutableStateOf("") }
    var selectedBarangId by remember { mutableStateOf<Int?>(null) }
    var namaBarangManual by remember { mutableStateOf("") }
    var selectedKategori by remember { mutableStateOf("Kertas") }
    var manualSatuan by remember { mutableStateOf("Rim") }
    var jumlahTambahStokText by remember { mutableStateOf("") }
    var potongKasOtomatis by remember { mutableStateOf(true) }
    var deletingBelanja by remember { mutableStateOf<TransaksiBelanjaInventaris?>(null) }
    var editingBelanja by remember { mutableStateOf<TransaksiBelanjaInventaris?>(null) }

    val uangKeluar = uangKeluarText.toDoubleOrNull() ?: 0.0
    val notaToko = notaTokoText.toDoubleOrNull() ?: 0.0
    val selisih = (uangKeluar - notaToko).coerceAtLeast(0.0)

    val selectedAccount = accounts.find { it.idAkun == selectedAccountId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Form Input Pembelian & Realisasi Belanja
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, PgdSoftBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PgdPurpleLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, tint = PgdPurple, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = if (editingBelanja != null) "Edit Realisasi Belanja Bahan Baku" else "Form Pembelian & Belanja Bahan Baku",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PgdPurpleDark
                            )
                            Text("Terintegrasi otomatis ke Stok & Valuasi serta Mutasi Kas", style = MaterialTheme.typography.bodySmall, color = Color(0xFF718096))
                        }
                    }

                    // Banner Mode Edit jika sedang mengedit
                    AnimatedVisibility(visible = editingBelanja != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PgdPurpleLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = PgdPurple, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Mode Edit: ${editingBelanja?.namaBarang}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = PgdPurpleDark
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        editingBelanja = null
                                        uangKeluarText = ""
                                        notaTokoText = ""
                                        catatanSelisih = ""
                                        jumlahTambahStokText = ""
                                        namaBarangManual = ""
                                        selectedBarangId = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Batal Edit", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE53E3E), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = PgdSoftBorder.copy(alpha = 0.6f))

                    // Pilihan Dompet Kas Asal
                    Text("1. Pilih Dompet Kas Pengeluaran:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(accounts) { acc ->
                            val isSelected = acc.idAkun == selectedAccountId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedAccountId = acc.idAkun
                                    // Auto adjust kategori & satuan default based on selected dompet
                                    if (acc.namaAkun.contains("Kertas", ignoreCase = true)) {
                                        selectedKategori = "Kertas"
                                        manualSatuan = "Rim"
                                    } else if (acc.namaAkun.contains("Tinta", ignoreCase = true)) {
                                        selectedKategori = "Tinta"
                                        manualSatuan = "Botol"
                                    } else if (acc.namaAkun.contains("Pengemasan", ignoreCase = true)) {
                                        selectedKategori = "Plastik & Pengemasan"
                                        manualSatuan = "Pack"
                                    }
                                },
                                label = { Text(acc.namaAkun) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PgdPurple,
                                    selectedLabelColor = Color.White,
                                    containerColor = PgdLilacBg,
                                    labelColor = Color(0xFF4A5568)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) PgdPurple else PgdSoftBorder)
                            )
                        }
                    }

                    // Baris Dua Input: Uang Keluar Kas vs Realisasi Nota Toko
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = uangKeluarText,
                            onValueChange = { uangKeluarText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Uang Keluar Dompet") },
                            placeholder = { Text("920.000") },
                            prefix = { Text("Rp ", color = PgdPurple) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("uang_keluar_kas_input")
                        )

                        OutlinedTextField(
                            value = notaTokoText,
                            onValueChange = { notaTokoText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Total Realisasi Nota") },
                            placeholder = { Text("920.000") },
                            prefix = { Text("Rp ", color = PgdGreen) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("realisasi_nota_input")
                        )
                    }

                    // Tampilan Otomatis Selisih Sisa Uang
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selisih > 0) Color(0xFFFFFBEB) else PgdPurpleLight.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (selisih > 0) Color(0xFFFDE68A) else PgdSoftBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Sisa Uang Belanja (Kembalian):", style = MaterialTheme.typography.labelSmall, color = Color(0xFF718096))
                                Text(
                                    text = formatRupiah(selisih),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selisih > 0) Color(0xFFB45309) else PgdPurpleDark
                                )
                            }
                            if (selisih > 0) {
                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFEF3C7)) {
                                    Text("Fleksibel", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFB45309), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }

                    // Input Catatan Penggunaan Selisih Fleksibel
                    if (selisih > 0) {
                        OutlinedTextField(
                            value = catatanSelisih,
                            onValueChange = { catatanSelisih = it },
                            label = { Text("Catatan Penggunaan Sisa Uang") },
                            placeholder = { Text("Misal: Bensin motor, makan siang, simpan di saku...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("catatan_selisih_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Hubungkan langsung dengan Stok & Valuasi
                    Text("2. Alokasi Barang Fisik (Otomatis Masuk ke Stok & Valuasi):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))

                    // Dropdown / Chips Barang Terdaftar vs Barang Baru
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedBarangId == null,
                                onClick = { selectedBarangId = null },
                                label = { Text("+ Barang Baru / Manual") },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PgdPurple,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(inventarisList) { item ->
                            val isSelected = selectedBarangId == item.idBarang
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedBarangId = item.idBarang
                                    namaBarangManual = item.namaBarang
                                },
                                label = { Text(item.namaBarang) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PgdPurple,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (selectedBarangId == null) {
                        OutlinedTextField(
                            value = namaBarangManual,
                            onValueChange = { namaBarangManual = it },
                            label = { Text("Nama Barang yang Dibeli") },
                            placeholder = { Text("Contoh: SIDU A4S") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Kategori Barang Baru
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Kategori Barang:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF718096))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val kategoriList = listOf("Kertas", "Tinta", "Plastik & Pengemasan", "Operasional & Lainnya")
                                items(kategoriList) { kat ->
                                    val isKatSel = selectedKategori == kat
                                    FilterChip(
                                        selected = isKatSel,
                                        onClick = { selectedKategori = kat },
                                        label = { Text(kat, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PgdPurpleLight,
                                            selectedLabelColor = PgdPurpleDark
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = jumlahTambahStokText,
                            onValueChange = { jumlahTambahStokText = it },
                            label = { Text("Jumlah Fisik (Stok Masuk)") },
                            placeholder = { Text("Contoh: 20") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        )

                        if (selectedBarangId != null) {
                            val satuanDisplay = inventarisList.find { it.idBarang == selectedBarangId }?.satuanUtuh ?: "Pcs"
                            OutlinedTextField(
                                value = satuanDisplay,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Satuan") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            OutlinedTextField(
                                value = manualSatuan,
                                onValueChange = { manualSatuan = it },
                                label = { Text("Satuan (Rim/Pcs)") },
                                placeholder = { Text("Rim") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (selectedBarangId == null) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val quickSatuans = listOf("Rim", "Pcs", "Pack", "Botol", "Dus", "Roll")
                            items(quickSatuans) { sat ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (manualSatuan.equals(sat, ignoreCase = true)) PgdPurpleLight else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (manualSatuan.equals(sat, ignoreCase = true)) PgdPurple else Color.Transparent),
                                    modifier = Modifier.clickable { manualSatuan = sat }
                                ) {
                                    Text(
                                        sat,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (manualSatuan.equals(sat, ignoreCase = true)) PgdPurple else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Checkbox Potong Kas Otomatis
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { potongKasOtomatis = !potongKasOtomatis }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = potongKasOtomatis,
                            onCheckedChange = { potongKasOtomatis = it },
                            colors = CheckboxDefaults.colors(checkedColor = PgdPurple)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Otomatis potong saldo dompet kas & catat mutasi keluar (${selectedAccount?.namaAkun ?: "Dompet Kas"})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2D3748)
                        )
                    }

                    // Sinkronisasi Info Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = PgdGreen, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Otomatis menghubungkan mutasi kas keluar, pembaruan stok fisik di Stok & Valuasi, dan riwayat belanja secara real-time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF166534)
                            )
                        }
                    }

                    // Tombol Submit Form Belanja
                    Button(
                        onClick = {
                            val finalNamaBarang = if (selectedBarangId != null) {
                                inventarisList.find { it.idBarang == selectedBarangId }?.namaBarang ?: namaBarangManual
                            } else {
                                namaBarangManual.ifBlank { "Belanja Bahan Baku" }
                            }
                            val finalSatuan = if (selectedBarangId != null) {
                                inventarisList.find { it.idBarang == selectedBarangId }?.satuanUtuh ?: manualSatuan
                            } else {
                                manualSatuan.ifBlank { "Pcs" }
                            }
                            val qty = jumlahTambahStokText.toDoubleOrNull() ?: 0.0

                            if (editingBelanja != null) {
                                viewModel.updateBelanjaInventaris(
                                    oldRecord = editingBelanja!!,
                                    tanggal = editingBelanja!!.tanggal,
                                    idAkunKas = selectedAccountId,
                                    namaAkunKas = selectedAccount?.namaAkun ?: "Dompet Kas",
                                    uangKeluarDompet = uangKeluar,
                                    realisasiNotaToko = if (notaToko > 0.0) notaToko else uangKeluar,
                                    catatanSelisih = catatanSelisih,
                                    idBarangTerkait = selectedBarangId,
                                    namaBarang = finalNamaBarang,
                                    kategoriBarang = selectedKategori,
                                    jumlahTambahStok = qty,
                                    satuan = finalSatuan,
                                    potongKasOtomatis = potongKasOtomatis
                                )
                                editingBelanja = null
                            } else {
                                viewModel.recordBelanjaInventaris(
                                    tanggal = today,
                                    idAkunKas = selectedAccountId,
                                    namaAkunKas = selectedAccount?.namaAkun ?: "Dompet Kas",
                                    uangKeluarDompet = uangKeluar,
                                    realisasiNotaToko = if (notaToko > 0.0) notaToko else uangKeluar,
                                    catatanSelisih = catatanSelisih,
                                    idBarangTerkait = selectedBarangId,
                                    namaBarang = finalNamaBarang,
                                    kategoriBarang = selectedKategori,
                                    jumlahTambahStok = qty,
                                    satuan = finalSatuan,
                                    potongKasOtomatis = potongKasOtomatis
                                )
                            }

                            // Reset input
                            uangKeluarText = ""
                            notaTokoText = ""
                            catatanSelisih = ""
                            jumlahTambahStokText = ""
                            namaBarangManual = ""
                            selectedBarangId = null
                        },
                        enabled = uangKeluar > 0.0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_belanja_inventaris_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PgdPurple)
                    ) {
                        Icon(
                            imageVector = if (editingBelanja != null) Icons.Default.Check else Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (editingBelanja != null) "Simpan Perubahan Belanja" else "Catat Realisasi Belanja & Tambah Stok",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Riwayat Belanja Inventaris
        item {
            Text(
                text = "Riwayat Realisasi Belanja (${belanjaList.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PgdPurpleDark
            )
        }

        if (belanjaList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada catatan belanja bahan baku", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF718096))
                }
            }
        } else {
            items(belanjaList, key = { it.idBelanja }) { record ->
                BelanjaRecordCard(
                    record = record,
                    onEdit = {
                        editingBelanja = record
                        selectedAccountId = record.idAkunKas
                        uangKeluarText = if (record.uangKeluarDompet > 0) record.uangKeluarDompet.toLong().toString() else ""
                        notaTokoText = if (record.realisasiNotaToko > 0) record.realisasiNotaToko.toLong().toString() else ""
                        catatanSelisih = record.catatanSelisih
                        selectedBarangId = record.idBarangTerkait
                        namaBarangManual = record.namaBarang
                        manualSatuan = record.satuan
                        jumlahTambahStokText = if (record.jumlahTambahStok > 0) formatNumber(record.jumlahTambahStok) else ""
                        potongKasOtomatis = record.potongKasOtomatis
                    },
                    onDelete = { deletingBelanja = record }
                )
            }
        }
    }

    if (deletingBelanja != null) {
        val recordToDelete = deletingBelanja!!
        AlertDialog(
            onDismissRequest = { deletingBelanja = null },
            title = {
                Text(
                    "Hapus Catatan Belanja & Rollback",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Apakah Anda yakin ingin menghapus catatan belanja '${recordToDelete.namaBarang}' senilai ${formatRupiah(recordToDelete.realisasiNotaToko)}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFFBEB),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Sinkronisasi Pembatalan (Rollback):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            if (recordToDelete.potongKasOtomatis && recordToDelete.uangKeluarDompet > 0.0) {
                                Text(
                                    "• Mutasi kas keluar otomatis dihapus dan saldo kas dompet (${recordToDelete.namaAkunKas}) dikembalikan sebesar ${formatRupiah(recordToDelete.uangKeluarDompet)}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF78350F)
                                )
                            }
                            if (recordToDelete.jumlahTambahStok > 0.0) {
                                Text(
                                    "• Jumlah fisik barang (${recordToDelete.namaBarang}) di Stok & Valuasi akan di-rollback (dikurangi ${formatNumber(recordToDelete.jumlahTambahStok)} ${recordToDelete.satuan}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF78350F)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBelanjaInventaris(recordToDelete)
                        deletingBelanja = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hapus & Rollback", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingBelanja = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun BelanjaRecordCard(
    record: TransaksiBelanjaInventaris,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PgdSoftBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PgdPurpleLight
                ) {
                    Text(
                        text = record.namaAkunKas,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PgdPurple,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(record.tanggal, style = MaterialTheme.typography.bodySmall, color = Color(0xFF718096))
            }

            Text(
                text = record.namaBarang.ifBlank { "Belanja Bahan Baku" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A202C)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Uang Ambil Kas", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color(0xFF718096))
                    Text(formatRupiah(record.uangKeluarDompet), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PgdPurpleDark)
                }
                Column {
                    Text("Realisasi Nota", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color(0xFF718096))
                    Text(formatRupiah(record.realisasiNotaToko), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PgdGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sisa Kas", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color(0xFF718096))
                    Text(
                        formatRupiah(record.selisihUang),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (record.selisihUang > 0) Color(0xFFB45309) else Color.Gray
                    )
                }
            }

            if (record.catatanSelisih.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFFBEB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Penggunaan Sisa: ${record.catatanSelisih}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (record.jumlahTambahStok > 0.0) {
                    Text(
                        text = "Stok bertambah: +${formatNumber(record.jumlahTambahStok)} ${record.satuan}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = PgdPurple
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Belanja",
                            tint = PgdPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus",
                            tint = Color(0xFFE53E3E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. SUB-MENU: PENGELUARAN & KOREKSI STOK
// -------------------------------------------------------------
@Composable
private fun PengeluaranKoreksiTab(
    viewModel: FinanceViewModel,
    inventarisList: List<InventarisBahanBaku>,
    pemakaianList: List<RiwayatPemakaianBahan>,
    onKoreksiItem: (InventarisBahanBaku) -> Unit
) {
    val safeInventarisList = remember(inventarisList) {
        try {
            (inventarisList ?: emptyList()).filterNotNull()
        } catch (_: Exception) {
            emptyList()
        }
    }
    val safePemakaianList = remember(pemakaianList) {
        try {
            (pemakaianList ?: emptyList()).filterNotNull()
        } catch (_: Exception) {
            emptyList()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner Info Prinsip Independen & Praktis
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, PgdSoftBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(PgdPurpleLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = PgdPurple, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Koreksi Stok Praktis Skala Pribadi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PgdPurpleDark
                        )
                        Text(
                            text = "Gunakan persentase praktis (Utuh 100%, 75%, 50%, 25%, Habis 0%) atau satuan utuh langsung tanpa perlu hitungan eceran rumit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF718096)
                        )
                    }
                }
            }
        }

        // Daftar Cepat Pilih Bahan Baku untuk Dikoreksi
        item {
            Text(
                text = "Pilih Bahan Baku untuk Koreksi / Pemakaian",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = PgdPurpleDark
            )
        }

        if (safeInventarisList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, PgdSoftBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada item bahan baku terdaftar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF718096)
                        )
                    }
                }
            }
        } else {
            itemsIndexed(
                safeInventarisList,
                key = { idx, item ->
                    val safeId = try { item.idBarang } catch (_: Exception) { idx }
                    "koreksi_inv_${safeId}_$idx"
                }
            ) { _, item ->
                val nama = try { (item.namaBarang as? String).orEmpty().ifBlank { "Bahan Baku" } } catch (_: Exception) { "Bahan Baku" }
                val stokStr = try { item.statusStokGabungan } catch (_: Exception) { "0 Pcs" }
                val kondisiStr = try { item.statusKondisiText } catch (_: Exception) { "100%" }
                val valuasiVal = try { item.nilaiTotalAset } catch (_: Exception) { 0.0 }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, PgdSoftBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                onKoreksiItem(item)
                            } catch (e: Exception) {
                                android.util.Log.e("InventarisScreen", "Error onKoreksiItem: ${e.message}")
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nama,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A202C)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Stok: $stokStr • Kondisi: $kondisiStr",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF718096)
                            )
                            Text(
                                text = "Valuasi: ${formatRupiah(valuasiVal)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = PgdPurple
                            )
                        }

                        Button(
                            onClick = {
                                try {
                                    onKoreksiItem(item)
                                } catch (e: Exception) {
                                    android.util.Log.e("InventarisScreen", "Error onKoreksiItem: ${e.message}")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PgdPurpleLight),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text("Koreksi", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = PgdPurple)
                        }
                    }
                }
            }
        }

        // Riwayat Log Pemakaian & Koreksi Bahan
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Riwayat Koreksi & Pemakaian (${safePemakaianList.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = PgdPurpleDark
            )
        }

        if (safePemakaianList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, PgdSoftBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Belum ada catatan log pemakaian", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF718096))
                    }
                }
            }
        } else {
            itemsIndexed(
                safePemakaianList,
                key = { idx, log ->
                    val safeId = try { log.idPemakaian } catch (_: Exception) { idx }
                    "pemakaian_${safeId}_$idx"
                }
            ) { _, log ->
                val perubahan = try { (log.nilaiPerubahan as? String).orEmpty() } catch (_: Exception) { "" }
                val isPositive = perubahan.startsWith("+")
                val namaBarang = try { (log.namaBarang as? String).orEmpty().ifBlank { "Bahan Baku" } } catch (_: Exception) { "Bahan Baku" }
                val jenisKoreksi = try { (log.jenisKoreksi as? String).orEmpty().ifBlank { "Pemakaian" } } catch (_: Exception) { "Pemakaian" }
                val tanggal = try { (log.tanggal as? String).orEmpty().ifBlank { "-" } } catch (_: Exception) { "-" }
                val ket = try { (log.keterangan as? String).orEmpty().ifBlank { "Pemakaian operasional" } } catch (_: Exception) { "Pemakaian operasional" }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, PgdSoftBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(namaBarang, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1A202C))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PgdPurpleLight
                                ) {
                                    Text(jenisKoreksi, style = MaterialTheme.typography.labelMedium, color = PgdPurple, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$tanggal • $ket",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF718096)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFF1F2)
                            ) {
                                Text(
                                    text = perubahan,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFBE123C),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    try {
                                        viewModel.deletePemakaianBahan(log)
                                    } catch (e: Exception) {
                                        android.util.Log.e("InventarisScreen", "Error deleting log: ${e.message}")
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Hapus Log",
                                    tint = Color(0xFFE53E3E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOG: FORM TAMBAH / EDIT ITEM BAHAN BAKU
// -------------------------------------------------------------
@Composable
private fun FormItemInventarisDialog(
    title: String,
    item: InventarisBahanBaku?,
    onDismiss: () -> Unit,
    onSave: (nama: String, kategori: String, stok: Double, satuan: String, harga: Double, kondisi: Int, catatan: String) -> Unit
) {
    var namaBarang by remember { mutableStateOf(item?.namaBarang ?: "") }
    var selectedKategori by remember { mutableStateOf(item?.kategori ?: "Kertas") }
    var stokUtuhText by remember { mutableStateOf(item?.stokUtuh?.let { formatNumber(it) } ?: "") }
    var selectedSatuan by remember { mutableStateOf(item?.satuanUtuh ?: "Rim") }
    var hargaSatuanText by remember { mutableStateOf(item?.hargaSatuanUtuh?.let { String.format(Locale.US, "%.0f", it) } ?: "") }
    var selectedKondisi by remember { mutableIntStateOf(item?.persentaseKondisi ?: 100) }
    var catatan by remember { mutableStateOf(item?.catatan ?: "") }

    val kategoriOptions = listOf("Kertas", "Tinta", "Plastik & Pengemasan", "Operasional & Lainnya")
    val satuanOptions = listOf("Rim", "Dus", "Botol", "Pack", "Roll", "Pcs", "Set")
    val kondisiOptions = listOf(Pair("100% (Utuh)", 100), Pair("75% (3/4)", 75), Pair("50% (1/2)", 50), Pair("25% (1/4)", 25), Pair("0% (Habis)", 0))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = PgdPurpleDark) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = namaBarang,
                    onValueChange = { namaBarang = it },
                    label = { Text("Nama Bahan Baku") },
                    placeholder = { Text("Contoh: Kertas HVS A4 SiDU 70gr") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_nama_barang"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Kategori:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(kategoriOptions) { kat ->
                        val isSelected = selectedKategori == kat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedKategori = kat },
                            label = { Text(kat, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PgdPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stokUtuhText,
                        onValueChange = { stokUtuhText = it },
                        label = { Text("Stok Utuh") },
                        placeholder = { Text("10") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_input_stok"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = hargaSatuanText,
                        onValueChange = { hargaSatuanText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Harga Beli / Satuan") },
                        placeholder = { Text("48000") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("dialog_input_harga"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Text("Satuan Utuh:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(satuanOptions) { sat ->
                        val isSelected = selectedSatuan == sat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSatuan = sat },
                            label = { Text(sat, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PgdPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Text("Kondisi Fisik Saat Ini:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(kondisiOptions) { (label, valInt) ->
                        val isSelected = selectedKondisi == valInt
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedKondisi = valInt },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PgdPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text("Catatan / Lokasi Simpan (Opsional)") },
                    placeholder = { Text("Rak nomor 2, dekat mesin printer...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stok = stokUtuhText.toDoubleOrNull() ?: 0.0
                    val harga = hargaSatuanText.toDoubleOrNull() ?: 0.0
                    onSave(namaBarang.ifBlank { "Bahan Baku" }, selectedKategori, stok, selectedSatuan, harga, selectedKondisi, catatan)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PgdPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("dialog_simpan_bahan_baku_btn")
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

// -------------------------------------------------------------
// DIALOG: KOREKSI STOK PRAKTIS (PERSENTASE & SATUAN UTUH / LEMBARAN)
// -------------------------------------------------------------
private data class KoreksiCalculationResult(
    val sisaStokLabel: String,
    val sisaDesimalLabel: String,
    val penurunanValuasi: Double,
    val valuasiBaru: Double
)

@Composable
private fun KoreksiStokPraktisDialog(
    item: InventarisBahanBaku,
    onDismiss: () -> Unit,
    onApply: (jenis: String, jumlah: Double, persentase: Int, keterangan: String) -> Unit
) {
    val isRim = (item.satuanUtuh as String?).orEmpty().equals("Rim", ignoreCase = true)
    val availableModes = listOf("Ambil Stok", "Tambah Stok", "Kondisi Fisik")

    var modeKoreksi by remember { mutableStateOf("Ambil Stok") }
    var selectedPersentase by remember { mutableIntStateOf(item.persentaseKondisi) }
    
    // States untuk pemakaian bahan
    // Jika kertas Rim: input Rim & Lembar eceran
    val currentStokUtuh = item.stokUtuh
    var rimAmbilText by remember { 
        mutableStateOf(if (currentStokUtuh >= 2.4) "2" else if (currentStokUtuh >= 1.0) "1" else "0") 
    }
    var lembarAmbilText by remember { 
        mutableStateOf(if (currentStokUtuh >= 2.4) "200" else "0") 
    }
    var jumlahAmbilNonRimText by remember { mutableStateOf("1") }

    // States untuk penambahan bahan
    var rimTambahText by remember { mutableStateOf("5") }
    var lembarTambahText by remember { mutableStateOf("0") }
    var jumlahTambahNonRimText by remember { mutableStateOf("5") }

    var keterangan by remember { mutableStateOf("") }

    val persentaseOptions = listOf(
        Pair("100% (Penuh / Utuh)", 100),
        Pair("75% (Sisa 3/4)", 75),
        Pair("50% (Sisa Separuh)", 50),
        Pair("25% (Sisa 1/4)", 25),
        Pair("0% (Habis Total)", 0)
    )

    // Perhitungan Live Real-Time
    // 1 Rim = 500 lembar
    val rimAmbil = rimAmbilText.toDoubleOrNull() ?: 0.0
    val lembarAmbil = lembarAmbilText.toDoubleOrNull() ?: 0.0
    val totalLembarAmbil = (rimAmbil * 500.0) + lembarAmbil
    val totalRimAmbil = totalLembarAmbil / 500.0
    val totalLembarAwal = Math.round(item.stokUtuh * 500.0).toDouble()

    val rimTambah = rimTambahText.toDoubleOrNull() ?: 0.0
    val lembarTambah = lembarTambahText.toDoubleOrNull() ?: 0.0
    val totalLembarTambah = (rimTambah * 500.0) + lembarTambah
    val totalRimTambah = totalLembarTambah / 500.0

    val jumlahAmbilNonRim = jumlahAmbilNonRimText.toDoubleOrNull() ?: 0.0
    val jumlahTambahNonRim = jumlahTambahNonRimText.toDoubleOrNull() ?: 0.0

    // Validasi Kelayakan Input
    val isInputValid = when (modeKoreksi) {
        "Ambil Stok" -> {
            if (isRim) {
                totalLembarAmbil > 0 && totalLembarAmbil <= totalLembarAwal
            } else {
                jumlahAmbilNonRim > 0 && jumlahAmbilNonRim <= item.stokUtuh
            }
        }
        "Tambah Stok" -> {
            if (isRim) {
                totalLembarTambah > 0
            } else {
                jumlahTambahNonRim > 0
            }
        }
        "Kondisi Fisik" -> true
        else -> false
    }

    // Hitung Sisa Fisik & Valuasi Baru secara Real-Time dengan tipe data aman
    val calcResult = remember(
        modeKoreksi, totalLembarAmbil, totalLembarTambah, jumlahAmbilNonRim, jumlahTambahNonRim, selectedPersentase, item
    ) {
        when (modeKoreksi) {
            "Ambil Stok" -> {
                if (isRim) {
                    val sisaLembar = maxOf(0.0, totalLembarAwal - totalLembarAmbil)
                    val sisaRim = sisaLembar / 500.0
                    val penurunan = totalRimAmbil * (item.persentaseKondisi / 100.0) * item.hargaSatuanUtuh
                    val valuasiBaru = maxOf(0.0, item.nilaiTotalAset - penurunan)
                    val formatted = InventarisBahanBaku.formatStokGabungan(sisaRim, "Rim")
                    val sisaRimDesimal = Math.round(sisaRim * 10.0) / 10.0
                    val desimalText = if (sisaRimDesimal % 1.0 == 0.0) {
                        String.format(Locale.GERMANY, "%,.0f", sisaRimDesimal)
                    } else {
                        String.format(Locale.GERMANY, "%,.1f", sisaRimDesimal)
                    }
                    KoreksiCalculationResult(formatted, desimalText, penurunan, valuasiBaru)
                } else {
                    val sisa = maxOf(0.0, item.stokUtuh - jumlahAmbilNonRim)
                    val penurunan = jumlahAmbilNonRim * (item.persentaseKondisi / 100.0) * item.hargaSatuanUtuh
                    val valuasiBaru = maxOf(0.0, item.nilaiTotalAset - penurunan)
                    KoreksiCalculationResult(InventarisBahanBaku.formatStokGabungan(sisa, item.satuanUtuh), formatNumber(sisa), penurunan, valuasiBaru)
                }
            }
            "Tambah Stok" -> {
                if (isRim) {
                    val totalLembarBaru = totalLembarAwal + totalLembarTambah
                    val rimBaru = totalLembarBaru / 500.0
                    val kenaikan = totalRimTambah * (item.persentaseKondisi / 100.0) * item.hargaSatuanUtuh
                    val valuasiBaru = item.nilaiTotalAset + kenaikan
                    val formatted = InventarisBahanBaku.formatStokGabungan(rimBaru, "Rim")
                    val rimDesimal = Math.round(rimBaru * 10.0) / 10.0
                    val desimalText = if (rimDesimal % 1.0 == 0.0) {
                        String.format(Locale.GERMANY, "%,.0f", rimDesimal)
                    } else {
                        String.format(Locale.GERMANY, "%,.1f", rimDesimal)
                    }
                    KoreksiCalculationResult(formatted, desimalText, -kenaikan, valuasiBaru)
                } else {
                    val stokBaru = item.stokUtuh + jumlahTambahNonRim
                    val kenaikan = jumlahTambahNonRim * (item.persentaseKondisi / 100.0) * item.hargaSatuanUtuh
                    val valuasiBaru = item.nilaiTotalAset + kenaikan
                    KoreksiCalculationResult(InventarisBahanBaku.formatStokGabungan(stokBaru, item.satuanUtuh), formatNumber(stokBaru), -kenaikan, valuasiBaru)
                }
            }
            else -> { // "Kondisi Fisik"
                val valuasiBaru = item.stokUtuh * (selectedPersentase / 100.0) * item.hargaSatuanUtuh
                val penurunan = item.nilaiTotalAset - valuasiBaru
                KoreksiCalculationResult(item.statusStokGabungan, formatNumber(item.stokUtuh), penurunan, valuasiBaru)
            }
        }
    }

    val sisaStokLabel = calcResult.sisaStokLabel
    val sisaDesimalLabel = calcResult.sisaDesimalLabel
    val penurunanValuasi = calcResult.penurunanValuasi
    val valuasiBaru = calcResult.valuasiBaru

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Koreksi Pemakaian Stok", fontWeight = FontWeight.Bold, color = PgdPurpleDark)
                Text(item.namaBarang, style = MaterialTheme.typography.bodySmall, color = Color(0xFF718096))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info Kartu Stok Saat Ini
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PgdPurpleLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Stok Fisik Awal:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF718096))
                            Text(item.statusStokGabungan, fontWeight = FontWeight.ExtraBold, color = PgdPurpleDark)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Valuasi Awal:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF718096))
                            Text(formatRupiah(item.nilaiTotalAset), fontWeight = FontWeight.Bold, color = PgdPurple)
                        }
                    }
                }

                // Pilihan Mode Koreksi (Chips)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableModes.forEach { mode ->
                        val isSelected = modeKoreksi == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { modeKoreksi = mode },
                            label = { 
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PgdPurple,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Input Dinamis Berdasarkan Mode
                when (modeKoreksi) {
                    "Ambil Stok" -> {
                        if (isRim) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Bahan yang Diambil / Dipakai:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4A5568)
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = rimAmbilText,
                                        onValueChange = { rimAmbilText = it },
                                        label = { Text("Rim (Utuh)") },
                                        placeholder = { Text("2") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = lembarAmbilText,
                                        onValueChange = { lembarAmbilText = it },
                                        label = { Text("Lembar (Eceran)") },
                                        placeholder = { Text("200") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                
                                if (totalLembarAmbil > 0) {
                                    val ambilGabungan = InventarisBahanBaku.formatStokGabungan(totalRimAmbil, "Rim")
                                    val rimDesimalStr = if (totalRimAmbil % 1.0 == 0.0) {
                                        String.format(Locale.GERMANY, "%,.0f", totalRimAmbil)
                                    } else {
                                        String.format(Locale.GERMANY, "%,.1f", totalRimAmbil)
                                    }
                                    Text(
                                        text = "📦 Total diambil: $ambilGabungan (${totalLembarAmbil.toLong()} Lembar atau $rimDesimalStr Rim)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = PgdPurpleDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (totalLembarAmbil > totalLembarAwal) {
                                    Text(
                                        text = "⚠️ Pengambilan (${totalLembarAmbil.toLong()} lbr) melebihi stok yang ada di rak gudang (${item.statusStokGabungan}).",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFFC53030),
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = "💡 Standar: 1 Rim = 500 Lembar. Bisa diisi Rim saja, Lembar saja, atau kombinasi keduanya.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = Color(0xFF718096)
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = jumlahAmbilNonRimText,
                                    onValueChange = { jumlahAmbilNonRimText = it },
                                    label = { Text("Jumlah yang Diambil (${item.satuanUtuh})") },
                                    placeholder = { Text("1") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                if (jumlahAmbilNonRim > item.stokUtuh) {
                                    Text(
                                        text = "⚠️ Jumlah diambil (${formatNumber(jumlahAmbilNonRim)} ${item.satuanUtuh}) melebihi stok gudang (${item.statusStokGabungan}).",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFFC53030),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    "Tambah Stok" -> {
                        if (isRim) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Bahan yang Ditambahkan:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4A5568)
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = rimTambahText,
                                        onValueChange = { rimTambahText = it },
                                        label = { Text("Rim (Utuh)") },
                                        placeholder = { Text("5") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = lembarTambahText,
                                        onValueChange = { lembarTambahText = it },
                                        label = { Text("Lembar (Eceran)") },
                                        placeholder = { Text("0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                if (totalLembarTambah > 0) {
                                    val tambahGabungan = InventarisBahanBaku.formatStokGabungan(totalRimTambah, "Rim")
                                    Text(
                                        text = "📦 Total ditambah: $tambahGabungan (${totalLembarTambah.toLong()} Lembar)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = PgdPurpleDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = jumlahTambahNonRimText,
                                onValueChange = { jumlahTambahNonRimText = it },
                                label = { Text("Jumlah Penambahan (${item.satuanUtuh})") },
                                placeholder = { Text("5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    "Kondisi Fisik" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Pilih Persentase Kondisi Fisik Riil:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A5568)
                            )
                            persentaseOptions.forEach { (label, valInt) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPersentase = valInt }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedPersentase == valInt,
                                        onClick = { selectedPersentase = valInt },
                                        colors = RadioButtonDefaults.colors(selectedColor = PgdPurple)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2D3748))
                                }
                            }
                        }
                    }
                }

                // Kotak Live Preview Hasil Perhitungan & Valuasi Aset
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PgdLilacBg),
                    border = BorderStroke(1.dp, PgdPurpleLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PgdPurpleDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Live Hasil Perhitungan & Valuasi",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PgdPurpleDark
                            )
                        }

                        HorizontalDivider(color = PgdPurpleLight, thickness = 1.dp)

                        // 1. Sisa Stok di Rak Gudang
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Sisa Stok di Rak Gudang:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A5568)
                            )
                            Text(
                                text = sisaStokLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = PgdPurpleDark
                            )
                            if (modeKoreksi == "Ambil Stok") {
                                val rincianAmbil = if (isRim) {
                                    val ambilStr = InventarisBahanBaku.formatStokGabungan(totalRimAmbil, "Rim")
                                    "Stok awal ${item.statusStokGabungan} − $ambilStr (${totalLembarAmbil.toLong()} lbr) = $sisaStokLabel (setara $sisaDesimalLabel Rim)"
                                } else {
                                    "Stok awal ${item.statusStokGabungan} − diambil ${formatNumber(jumlahAmbilNonRim)} ${item.satuanUtuh}"
                                }
                                Text(
                                    text = rincianAmbil,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF718096)
                                )
                            } else if (modeKoreksi == "Tambah Stok") {
                                val rincianTambah = if (isRim) {
                                    val tambahStr = InventarisBahanBaku.formatStokGabungan(totalRimTambah, "Rim")
                                    "Stok awal ${item.statusStokGabungan} + $tambahStr (${totalLembarTambah.toLong()} lbr) = $sisaStokLabel (setara $sisaDesimalLabel Rim)"
                                } else {
                                    "Stok awal ${item.statusStokGabungan} + ditambah ${formatNumber(jumlahTambahNonRim)} ${item.satuanUtuh}"
                                }
                                Text(
                                    text = rincianTambah,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF718096)
                                )
                            } else {
                                Text(
                                    text = "Kondisi fisik barang disesuaikan menjadi $selectedPersentase%",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF718096)
                                )
                            }
                        }

                        // 2. Valuasi Aset Gudang
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Valuasi Aset Gudang:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A5568)
                            )
                            val diffSign = if (penurunanValuasi > 0) {
                                "-${formatRupiah(penurunanValuasi)}"
                            } else if (penurunanValuasi < 0) {
                                "+${formatRupiah(-penurunanValuasi)}"
                            } else {
                                "Rp 0"
                            }
                            Text(
                                text = "${formatRupiah(item.nilaiTotalAset)} ➔ ${formatRupiah(valuasiBaru)} ($diffSign)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (penurunanValuasi > 0) Color(0xFFC53030) else if (penurunanValuasi < 0) Color(0xFF2F855A) else Color(0xFF2D3748)
                            )
                            if (modeKoreksi == "Ambil Stok" && isRim && totalLembarAmbil > 0) {
                                val hargaPerLembar = item.hargaSatuanUtuh / 500.0
                                Text(
                                    text = "Pengurangan nilai: ${totalLembarAmbil.toLong()} lbr x ${formatRupiah(hargaPerLembar)}/lbr (${formatRupiah(item.hargaSatuanUtuh)}/Rim)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                    color = Color(0xFF718096)
                                )
                            }
                        }

                        // 3. Penegasan Catatan Dompet Kas Utama
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🔒", fontSize = 11.sp)
                                Text(
                                    text = "Perubahan ini murni untuk pembukuan internal inventaris gudang dan tidak mengubah atau mengganggu saldo riil Dompet Kas utama.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp),
                                    color = Color(0xFF718096)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    label = { Text("Keterangan Tambahan (Opsional)") },
                    placeholder = { Text("Misal: Dipakai untuk cetak pesanan SMKN 1, tes mesin...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = isInputValid,
                onClick = {
                    when (modeKoreksi) {
                        "Ambil Stok" -> {
                            if (isRim) {
                                val ambilStr = InventarisBahanBaku.formatStokGabungan(totalRimAmbil, "Rim")
                                val autoKet = "Ambil $ambilStr (${totalLembarAmbil.toLong()} lbr). Sisa di rak gudang: $sisaStokLabel" + if (keterangan.isNotBlank()) " • $keterangan" else ""
                                onApply("Pemakaian Lembaran", totalRimAmbil, item.persentaseKondisi, autoKet)
                            } else {
                                val autoKet = "Ambil ${formatNumber(jumlahAmbilNonRim)} ${item.satuanUtuh}. Sisa di rak gudang: $sisaStokLabel" + if (keterangan.isNotBlank()) " • $keterangan" else ""
                                onApply("Pemakaian Lembaran", jumlahAmbilNonRim, item.persentaseKondisi, autoKet)
                            }
                        }
                        "Tambah Stok" -> {
                            if (isRim) {
                                val tambahStr = InventarisBahanBaku.formatStokGabungan(totalRimTambah, "Rim")
                                val autoKet = "Tambah fisik +$tambahStr (${totalLembarTambah.toLong()} lbr). Sisa di rak gudang: $sisaStokLabel" + if (keterangan.isNotBlank()) " • $keterangan" else ""
                                onApply("Tambah Stok Fisik", totalRimTambah, item.persentaseKondisi, autoKet)
                            } else {
                                val autoKet = "Tambah fisik +${formatNumber(jumlahTambahNonRim)} ${item.satuanUtuh}. Sisa di rak gudang: $sisaStokLabel" + if (keterangan.isNotBlank()) " • $keterangan" else ""
                                onApply("Tambah Stok Fisik", jumlahTambahNonRim, item.persentaseKondisi, autoKet)
                            }
                        }
                        else -> { // "Kondisi Fisik"
                            val autoKet = "Koreksi kondisi fisik menjadi $selectedPersentase%" + if (keterangan.isNotBlank()) " • $keterangan" else ""
                            onApply("Ubah Persentase", 0.0, selectedPersentase, autoKet)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PgdPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("apply_koreksi_stok_btn")
            ) {
                Text("Terapkan", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}
