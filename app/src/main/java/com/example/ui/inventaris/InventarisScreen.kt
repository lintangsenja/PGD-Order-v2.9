package com.example.ui.inventaris

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InventarisBahanBaku
import com.example.data.model.MasterAkunSaldo
import com.example.data.model.RiwayatPemakaianBahan
import com.example.data.model.TransaksiBelanjaInventaris
import com.example.ui.viewmodel.FinanceViewModel
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

private fun formatRupiah(amount: Double): String {
    return "Rp " + String.format(Locale.GERMANY, "%,.0f", amount)
}

private fun formatNumber(num: Double): String {
    return if (num % 1.0 == 0.0) {
        String.format(Locale.GERMANY, "%,.0f", num)
    } else {
        String.format(Locale.GERMANY, "%,.1f", num)
    }
}

@Composable
fun InventarisScreen(
    viewModel: FinanceViewModel,
    accounts: List<MasterAkunSaldo>,
    modifier: Modifier = Modifier
) {
    val inventarisList by viewModel.allInventaris.collectAsStateWithLifecycle()
    val belanjaList by viewModel.allBelanjaInventaris.collectAsStateWithLifecycle()
    val pemakaianList by viewModel.allPemakaianBahan.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableIntStateOf(0) }
    // 0: Ringkasan Aset, 1: Stok & Valuasi, 2: Pembelian & Belanja, 3: Pengeluaran & Koreksi

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<InventarisBahanBaku?>(null) }
    var itemToKoreksi by remember { mutableStateOf<InventarisBahanBaku?>(null) }
    var itemToDelete by remember { mutableStateOf<InventarisBahanBaku?>(null) }

    Scaffold(
        containerColor = PgdLilacBg,
        floatingActionButton = {
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
                    .testTag("fab_tambah_bahan_baku")
                    .padding(bottom = 8.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Bahan Baku",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    selectedTabIndex = activeSubTab,
                    edgePadding = 12.dp,
                    containerColor = Color.White,
                    contentColor = PgdPurple,
                    indicator = {},
                    divider = {}
                ) {
                    val tabs = listOf(
                        Pair("Ringkasan Aset", Icons.Default.Analytics),
                        Pair("Stok & Valuasi", Icons.Default.Inventory2),
                        Pair("Pembelian & Belanja", Icons.Default.ShoppingCart),
                        Pair("Pengeluaran & Koreksi", Icons.Default.Tune)
                    )

                    tabs.forEachIndexed { index, (title, icon) ->
                        val isSelected = activeSubTab == index
                        Tab(
                            selected = isSelected,
                            onClick = { activeSubTab = index },
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

            // Konten Dinamis Berdasarkan Tab Aktif
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (activeSubTab) {
                    0 -> RingkasanAsetTab(
                        inventarisList = inventarisList,
                        belanjaList = belanjaList,
                        onNavigateToStok = { activeSubTab = 1 },
                        onNavigateToBelanja = { activeSubTab = 2 }
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
                        accounts = accounts,
                        inventarisList = inventarisList,
                        belanjaList = belanjaList
                    )
                    3 -> PengeluaranKoreksiTab(
                        viewModel = viewModel,
                        inventarisList = inventarisList,
                        pemakaianList = pemakaianList,
                        onKoreksiItem = { itemToKoreksi = it }
                    )
                }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                        style = MaterialTheme.typography.headlineLarge,
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
                style = MaterialTheme.typography.titleLarge,
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
                item {
                    Spacer(modifier = Modifier.height(72.dp)) // Ruang untuk FAB
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
                style = MaterialTheme.typography.titleLarge,
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
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF718096)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatNumber(item.stokUtuh),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = PgdPurpleDark
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.satuanUtuh,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4A5568)
                        )
                    }
                    Text(
                        text = "@ ${formatRupiah(item.hargaSatuanUtuh)} / ${item.satuanUtuh}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF718096)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Nilai Aset",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF718096)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatRupiah(item.nilaiTotalAset),
                        style = MaterialTheme.typography.titleLarge,
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
    var jumlahTambahStokText by remember { mutableStateOf("") }
    var potongKasOtomatis by remember { mutableStateOf(true) }

    val uangKeluar = uangKeluarText.toDoubleOrNull() ?: 0.0
    val notaToko = notaTokoText.toDoubleOrNull() ?: 0.0
    val selisih = (uangKeluar - notaToko).coerceAtLeast(0.0)

    val selectedAccount = accounts.find { it.idAkun == selectedAccountId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                            Text("Form Belanja Bahan Baku", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PgdPurpleDark)
                            Text("Pemisahan uang kas dompet vs nota toko", style = MaterialTheme.typography.bodySmall, color = Color(0xFF718096))
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
                                onClick = { selectedAccountId = acc.idAkun },
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
                            label = { Text("Uang Keluar Kas") },
                            placeholder = { Text("1.000.000") },
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
                            label = { Text("Realisasi Nota") },
                            placeholder = { Text("900.000") },
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
                                Text("Sisa Uang Belanja (Selisih):", style = MaterialTheme.typography.labelSmall, color = Color(0xFF718096))
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
                            placeholder = { Text("Misal: Bensin motor, makan siang, uang parkir, simpan di saku...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("catatan_selisih_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Hubungkan dengan Stok Inventaris
                    Text("2. Alokasi Stok Bahan Baku (Opsional):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2D3748))

                    // Dropdown / Chips Barang Terdaftar
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedBarangId == null,
                                onClick = { selectedBarangId = null },
                                label = { Text("Barang Bebas / Baru") },
                                shape = RoundedCornerShape(12.dp)
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
                            placeholder = { Text("Kertas HVS A4 SiDU 70gr") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = jumlahTambahStokText,
                            onValueChange = { jumlahTambahStokText = it },
                            label = { Text("Jumlah Tambah Stok") },
                            placeholder = { Text("10") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        val satuanDisplay = inventarisList.find { it.idBarang == selectedBarangId }?.satuanUtuh ?: "Rim/Pcs"
                        OutlinedTextField(
                            value = satuanDisplay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Satuan") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
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
                            text = "Otomatis catat pengeluaran di kas utama (${selectedAccount?.namaAkun ?: "Dompet Kas"})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2D3748)
                        )
                    }

                    // Tombol Submit Form Belanja
                    Button(
                        onClick = {
                            val finalNamaBarang = if (selectedBarangId != null) {
                                inventarisList.find { it.idBarang == selectedBarangId }?.namaBarang ?: namaBarangManual
                            } else {
                                namaBarangManual.ifBlank { "Belanja Bahan Baku" }
                            }
                            val finalSatuan = inventarisList.find { it.idBarang == selectedBarangId }?.satuanUtuh ?: "Pcs"
                            val qty = jumlahTambahStokText.toDoubleOrNull() ?: 0.0

                            viewModel.recordBelanjaInventaris(
                                tanggal = today,
                                idAkunKas = selectedAccountId,
                                namaAkunKas = selectedAccount?.namaAkun ?: "Dompet Kertas",
                                uangKeluarDompet = uangKeluar,
                                realisasiNotaToko = if (notaToko > 0.0) notaToko else uangKeluar,
                                catatanSelisih = catatanSelisih,
                                idBarangTerkait = selectedBarangId,
                                namaBarang = finalNamaBarang,
                                jumlahTambahStok = qty,
                                satuan = finalSatuan,
                                potongKasOtomatis = potongKasOtomatis
                            )

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
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Catat Realisasi Belanja", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Riwayat Belanja Inventaris
        item {
            Text(
                text = "Riwayat Realisasi Belanja (${belanjaList.size})",
                style = MaterialTheme.typography.titleLarge,
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
                BelanjaRecordCard(record = record, onDelete = { viewModel.deleteBelanjaInventaris(record) })
            }
        }
    }
}

@Composable
private fun BelanjaRecordCard(
    record: TransaksiBelanjaInventaris,
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

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color(0xFFE53E3E), modifier = Modifier.size(20.dp))
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PgdPurpleDark
            )
        }

        items(inventarisList, key = { it.idBarang }) { item ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, PgdSoftBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onKoreksiItem(item) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.namaBarang, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A202C))
                        Text(
                            "Stok: ${formatNumber(item.stokUtuh)} ${item.satuanUtuh} • Kondisi: ${item.statusKondisiText}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF718096)
                        )
                    }

                    Button(
                        onClick = { onKoreksiItem(item) },
                        colors = ButtonDefaults.buttonColors(containerColor = PgdPurpleLight),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text("Koreksi", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = PgdPurple)
                    }
                }
            }
        }

        // Riwayat Log Pemakaian & Koreksi Bahan
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Riwayat Koreksi & Pemakaian (${pemakaianList.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PgdPurpleDark
            )
        }

        if (pemakaianList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada catatan log pemakaian", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF718096))
                }
            }
        } else {
            items(pemakaianList, key = { it.idPemakaian }) { log ->
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
                                Text(log.namaBarang, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1A202C))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PgdPurpleLight
                                ) {
                                    Text(log.jenisKoreksi, style = MaterialTheme.typography.labelMedium, color = PgdPurple, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            Text(
                                text = "${log.tanggal} • ${log.keterangan.ifBlank { "Pemakaian operasional" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF718096)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (log.nilaiPerubahan.startsWith("+")) Color(0xFFE8F5E9) else Color(0xFFFFF1F2)
                        ) {
                            Text(
                                text = log.nilaiPerubahan,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (log.nilaiPerubahan.startsWith("+")) Color(0xFF2E7D32) else Color(0xFFBE123C),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
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
// DIALOG: KOREKSI STOK PRAKTIS (PERSENTASE & SATUAN UTUH)
// -------------------------------------------------------------
@Composable
private fun KoreksiStokPraktisDialog(
    item: InventarisBahanBaku,
    onDismiss: () -> Unit,
    onApply: (jenis: String, jumlah: Double, persentase: Int, keterangan: String) -> Unit
) {
    var modeKoreksi by remember { mutableStateOf("Persentase") } // "Persentase" vs "Kurangi Utuh" vs "Tambah Utuh"
    var selectedPersentase by remember { mutableIntStateOf(item.persentaseKondisi) }
    var jumlahUtuhText by remember { mutableStateOf("1") }
    var keterangan by remember { mutableStateOf("") }

    val persentaseOptions = listOf(
        Pair("100% (Penuh/Utuh)", 100),
        Pair("75% (Sisa 3/4)", 75),
        Pair("50% (Sisa Separuh)", 50),
        Pair("25% (Sisa 1/4)", 25),
        Pair("0% (Habis Total)", 0)
    )

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        Text("Stok Saat Ini: ${formatNumber(item.stokUtuh)} ${item.satuanUtuh}", fontWeight = FontWeight.Bold, color = PgdPurpleDark)
                        Text(item.statusKondisiText, fontWeight = FontWeight.SemiBold, color = PgdPurple)
                    }
                }

                // Pilihan Mode Koreksi
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Persentase", "Kurangi Utuh", "Tambah Utuh").forEach { mode ->
                        val isSelected = modeKoreksi == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { modeKoreksi = mode },
                            label = { Text(mode, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PgdPurple,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                when (modeKoreksi) {
                    "Persentase" -> {
                        Text(
                            text = "Pilih Persentase Pemakaian Praktis:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A5568)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            persentaseOptions.forEach { (label, valInt) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPersentase = valInt }
                                        .padding(vertical = 4.dp),
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
                    "Kurangi Utuh" -> {
                        OutlinedTextField(
                            value = jumlahUtuhText,
                            onValueChange = { jumlahUtuhText = it },
                            label = { Text("Jumlah Pengurangan (${item.satuanUtuh})") },
                            placeholder = { Text("1") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    "Tambah Utuh" -> {
                        OutlinedTextField(
                            value = jumlahUtuhText,
                            onValueChange = { jumlahUtuhText = it },
                            label = { Text("Jumlah Penambahan (${item.satuanUtuh})") },
                            placeholder = { Text("5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    label = { Text("Keterangan Pemakaian / Keperluan") },
                    placeholder = { Text("Misal: Dipakai untuk cetak brosur SMKN 1, tes cetak...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val jumlah = jumlahUtuhText.toDoubleOrNull() ?: 0.0
                    val jenis = when (modeKoreksi) {
                        "Persentase" -> "Ubah Persentase"
                        "Kurangi Utuh" -> "Kurangi Satuan Utuh"
                        else -> "Tambah Stok Fisik"
                    }
                    onApply(jenis, jumlah, selectedPersentase, keterangan)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PgdPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("apply_koreksi_stok_btn")
            ) {
                Text("Terapkan Koreksi", fontWeight = FontWeight.Bold, color = Color.White)
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
