package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MasterAkunSaldo
import com.example.data.model.MasterPelanggan
import com.example.data.model.NotaSettings
import com.example.data.model.TransaksiOrderMasuk
import com.example.formatAngka
import com.example.formatRupiah
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modul Order Nota Mandiri:
 * 1. Order Transaksi (Form Input Pesanan & Live Kalkulasi HPP)
 * 2. Setting Harga (Pengaturan Nilai Persentase Operasional & Master Kemasan Nota Terisolasi)
 * 3. Riwayat Order Nota (Rekam Jejak Transaksi Khusus Nota & Skenario Borongan)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderNotaScreen(
    viewModel: FinanceViewModel,
    orders: List<TransaksiOrderMasuk>,
    accounts: List<MasterAkunSaldo>,
    onNavigateBack: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    // Sub-menu Tab State: 0: Order Transaksi, 1: Setting Harga, 2: Riwayat Order Nota
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var editingOrder by remember { mutableStateOf<TransaksiOrderMasuk?>(null) }

    // Riwayat Order Nota khusus:
    val notaOrders = remember(orders) { orders.filter { it.isNota } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Top Navigation Header
        Surface(
            color = colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onNavigateBack != {}) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Order Nota",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modul Order Nota",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Ekosistem Mandiri Pemesanan Nota & Borongan",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sub-Menu Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val subTabs = listOf(
                        Triple(0, "Order Transaksi", Icons.Default.EditNote),
                        Triple(1, "Setting Harga", Icons.Default.Tune),
                        Triple(2, "Riwayat Nota", Icons.Default.History)
                    )

                    subTabs.forEach { (index, label, icon) ->
                        val isSelected = selectedSubTab == index
                        val animatedBg by animateColorAsState(
                            targetValue = if (isSelected) colorScheme.primary else Color.Transparent,
                            animationSpec = tween(200),
                            label = "tab_bg"
                        )
                        val animatedColor by animateColorAsState(
                            targetValue = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                            animationSpec = tween(200),
                            label = "tab_text"
                        )

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(animatedBg)
                                .clickable {
                                    selectedSubTab = index
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = animatedColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = animatedColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (index == 2 && notaOrders.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) colorScheme.onPrimary.copy(alpha = 0.2f) else colorScheme.primaryContainer)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${notaOrders.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colorScheme.onPrimary else colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Body Content Based on Sub-Tab
        Box(modifier = Modifier.weight(1f)) {
            when (selectedSubTab) {
                0 -> OrderNotaTransaksiTab(
                    viewModel = viewModel,
                    editingOrder = editingOrder,
                    onCancelEdit = { editingOrder = null },
                    onOrderSaved = {
                        editingOrder = null
                        selectedSubTab = 2 // Switch to history tab on save
                    }
                )
                1 -> OrderNotaSettingHargaTab(viewModel = viewModel)
                2 -> OrderNotaRiwayatTab(
                    notaOrders = notaOrders,
                    viewModel = viewModel,
                    onEditOrder = { order ->
                        editingOrder = order
                        selectedSubTab = 0 // Switch to transaction tab in edit mode
                    }
                )
            }
        }
    }
}

/**
 * Sub-Menu 1: Form Input Order Nota & Live HPP Calculation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderNotaTransaksiTab(
    viewModel: FinanceViewModel,
    editingOrder: TransaksiOrderMasuk?,
    onCancelEdit: () -> Unit,
    onOrderSaved: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val notaSettings by viewModel.notaSettings.collectAsStateWithLifecycle()
    val allPelanggan by viewModel.allPelanggan.collectAsStateWithLifecycle()

    // Form Field States
    var tanggalOrder by remember(editingOrder) {
        mutableStateOf(editingOrder?.tanggalOrder ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var namaPelanggan by remember(editingOrder) {
        mutableStateOf(editingOrder?.let { extractCustomerName(it.namaPesanan) } ?: "")
    }
    var deskripsiNota by remember(editingOrder) {
        mutableStateOf(editingOrder?.let { extractOrderDescription(it.namaPesanan) } ?: "Nota Kontan 2 Ply")
    }

    // Skenario Penjualan: Satuan vs Paket / Borongan
    var isBorongan by remember(editingOrder) {
        mutableStateOf(editingOrder?.isBorongan ?: false)
    }
    var catatanHasilJadi by remember(editingOrder) {
        mutableStateOf(editingOrder?.catatanHasilJadi ?: "")
    }

    // Satuan Inputs
    var qtySatuanInput by remember(editingOrder) {
        mutableStateOf(editingOrder?.takeIf { !it.isBorongan }?.qtyOrder?.toString() ?: "10")
    }
    var satuanNamaInput by remember(editingOrder) {
        mutableStateOf(editingOrder?.satuan?.takeIf { it.isNotBlank() } ?: "Buku")
    }
    var hargaPerSatuanInput by remember(editingOrder) {
        mutableStateOf(editingOrder?.takeIf { !it.isBorongan }?.let { formatNominalClean(it.hargaSatuan) } ?: "25000")
    }

    // Paket / Borongan Inputs
    var totalBoronganInput by remember(editingOrder) {
        mutableStateOf(editingOrder?.takeIf { it.isBorongan }?.let { formatNominalClean(it.totalPendapatan) } ?: "260000")
    }
    var qtyUnitEstimasiBorongan by remember(editingOrder) {
        mutableStateOf(editingOrder?.takeIf { it.isBorongan }?.qtyOrder?.toString() ?: "1")
    }

    // Calculations of Sales Total & Effective Qty
    val effectiveQty = if (isBorongan) {
        (qtyUnitEstimasiBorongan.toIntOrNull() ?: 1).coerceAtLeast(1)
    } else {
        (qtySatuanInput.toIntOrNull() ?: 1).coerceAtLeast(1)
    }

    val totalPenjualan = if (isBorongan) {
        parseNominal(totalBoronganInput) ?: 0.0
    } else {
        val q = qtySatuanInput.toIntOrNull() ?: 0
        val p = parseNominal(hargaPerSatuanInput) ?: 0.0
        q.toDouble() * p
    }

    // --- Modal & Alokasi Pos Kas (HPP Nota) ---
    // 1. Kertas NCR: Nominal mutlak manual (plot utuh ke dompet Kertas)
    var nominalKertasInput by remember(editingOrder) {
        mutableStateOf(
            editingOrder?.let { formatNominalClean(it.qtyOrder.toDouble() * it.getEffectiveKertasHpp()) } ?: "120000"
        )
    }
    val nominalKertas = parseNominal(nominalKertasInput) ?: 0.0

    // 2. Tinta: Otomatis dari persentase setting, bisa dioverride/dibulatkan manual
    val autoTinta = kotlin.math.round(totalPenjualan * notaSettings.tintaPct)
    var overrideTintaInput by remember(editingOrder) {
        mutableStateOf(
            editingOrder?.let { formatNominalClean(it.qtyOrder.toDouble() * it.getEffectiveTintaHpp()) } ?: ""
        )
    }
    val nominalTinta = if (overrideTintaInput.isNotBlank()) {
        parseNominal(overrideTintaInput) ?: 0.0
    } else {
        autoTinta
    }

    // 3. Waste Kertas: Otomatis dari persentase setting, bisa dioverride/dibulatkan manual
    val autoWaste = kotlin.math.round(totalPenjualan * notaSettings.wastePct)
    var overrideWasteInput by remember(editingOrder) {
        mutableStateOf(
            editingOrder?.let { formatNominalClean(it.getEffectiveWastePct() * it.totalPendapatan) } ?: ""
        )
    }
    val nominalWaste = if (overrideWasteInput.isNotBlank()) {
        parseNominal(overrideWasteInput) ?: 0.0
    } else {
        autoWaste
    }

    // 4. Pengemasan: Master harga dari setting * Qty Plastik
    var qtyPlastikInput by remember(editingOrder) {
        mutableStateOf(editingOrder?.jumlahPlastikPengemasan?.toString() ?: "2")
    }
    val qtyPlastik = qtyPlastikInput.toIntOrNull() ?: 0
    val nominalPengemasan = qtyPlastik.toDouble() * notaSettings.masterHargaPengemasan

    // 5. Pos Opsional: Tenaga Kerja, Listrik, Maintenance (Bisa diisi 0 atau dikosongkan)
    var tenagaKerjaPctInput by remember(editingOrder) {
        mutableStateOf(
            editingOrder?.let { (it.getEffectiveTenagaKerjaPct() * 100).toString().removeSuffix(".0") }
                ?: (notaSettings.tenagaKerjaPct * 100).toString().removeSuffix(".0")
        )
    }
    val tenagaKerjaPct = (tenagaKerjaPctInput.toDoubleOrNull() ?: 0.0) / 100.0
    val autoTenaga = kotlin.math.round(totalPenjualan * tenagaKerjaPct)
    var overrideTenagaInput by remember(editingOrder) {
        mutableStateOf(
            editingOrder?.let { formatNominalClean(it.getEffectiveTenagaKerjaPct() * it.totalPendapatan) } ?: ""
        )
    }
    val nominalTenaga = if (overrideTenagaInput.isNotBlank()) {
        parseNominal(overrideTenagaInput) ?: 0.0
    } else {
        autoTenaga
    }

    var listrikPctInput by remember(editingOrder) {
        mutableStateOf(
            editingOrder?.let { (it.getEffectiveListrikPct() * 100).toString().removeSuffix(".0") }
                ?: (notaSettings.listrikPct * 100).toString().removeSuffix(".0")
        )
    }
    val listrikPct = (listrikPctInput.toDoubleOrNull() ?: 0.0) / 100.0
    val autoListrik = kotlin.math.round(totalPenjualan * listrikPct)
    var overrideListrikInput by remember(editingOrder) {
        mutableStateOf(
            editingOrder?.let { formatNominalClean(it.getEffectiveListrikPct() * it.totalPendapatan) } ?: ""
        )
    }
    val nominalListrik = if (overrideListrikInput.isNotBlank()) {
        parseNominal(overrideListrikInput) ?: 0.0
    } else {
        autoListrik
    }

    var maintenancePctInput by remember(editingOrder) {
        mutableStateOf(
            editingOrder?.let { (it.getEffectiveMaintenancePct() * 100).toString().removeSuffix(".0") }
                ?: (notaSettings.maintenancePct * 100).toString().removeSuffix(".0")
        )
    }
    val maintenancePct = (maintenancePctInput.toDoubleOrNull() ?: 0.0) / 100.0
    val autoMaintenance = kotlin.math.round(totalPenjualan * maintenancePct)
    var overrideMaintenanceInput by remember(editingOrder) {
        mutableStateOf(
            editingOrder?.let { formatNominalClean(it.getEffectiveMaintenancePct() * it.totalPendapatan) } ?: ""
        )
    }
    val nominalMaintenance = if (overrideMaintenanceInput.isNotBlank()) {
        parseNominal(overrideMaintenanceInput) ?: 0.0
    } else {
        autoMaintenance
    }

    // 6. Logika Sapu Jagat (Laba Bersih):
    // Total modal = kertas + tinta + waste + pengemasan + tenaga + listrik + maintenance
    // Seluruh sisa uang dari harga jual otomatis bermuara ke Laba Bersih
    val totalModalDasar = nominalKertas + nominalTinta + nominalWaste + nominalPengemasan + nominalTenaga + nominalListrik + nominalMaintenance
    val labaBersihSapuJagat = totalPenjualan - totalModalDasar
    val marginLabaPct = if (totalPenjualan > 0) (labaBersihSapuJagat / totalPenjualan) * 100.0 else 0.0

    // Status Pembayaran & Input Uang Masuk
    var statusPembayaran by remember(editingOrder) {
        mutableStateOf(editingOrder?.status ?: "Lunas")
    }
    var metodePembayaran by remember(editingOrder) {
        mutableStateOf(editingOrder?.metodePembayaran ?: "Bayar Penuh")
    }
    var nominalBayarInput by remember(editingOrder) {
        mutableStateOf(editingOrder?.let { formatNominalClean(it.effectiveJumlahDibayar) } ?: "")
    }

    val actualPaid = when (metodePembayaran) {
        "Bayar Penuh" -> totalPenjualan
        else -> (parseNominal(nominalBayarInput) ?: 0.0).coerceIn(0.0, totalPenjualan)
    }
    val sisaKekurangan = (totalPenjualan - actualPaid).coerceAtLeast(0.0)

    // Customer Autocomplete Dropdown State
    var showCustomerDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        // Mode Edit Alert Banner
        if (editingOrder != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mode Edit Order Nota #${editingOrder.idOrder}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = editingOrder.cleanOrderTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = onCancelEdit) {
                            Text("Batal Edit", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Section 1: Informasi Dasar Pesanan
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonOutline,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Informasi Pelanggan & Pesanan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }

                    // Tanggal Order
                    OutlinedTextField(
                        value = tanggalOrder,
                        onValueChange = { tanggalOrder = it },
                        label = { Text("Tanggal Order (YYYY-MM-DD)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Tanggal")
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val calPicked = Calendar.getInstance()
                                        calPicked.set(year, month, dayOfMonth)
                                        tanggalOrder = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calPicked.time)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Autocomplete Nama Pelanggan
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = namaPelanggan,
                            onValueChange = {
                                namaPelanggan = it
                                showCustomerDropdown = it.isNotBlank() && allPelanggan.any { c -> c.namaPelanggan.contains(it, ignoreCase = true) }
                            },
                            label = { Text("Nama Pelanggan") },
                            placeholder = { Text("Ketik atau pilih pelanggan") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Pelanggan")
                            },
                            trailingIcon = {
                                if (allPelanggan.isNotEmpty()) {
                                    IconButton(onClick = { showCustomerDropdown = !showCustomerDropdown }) {
                                        Icon(
                                            imageVector = if (showCustomerDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Pilih Pelanggan"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        DropdownMenu(
                            expanded = showCustomerDropdown,
                            onDismissRequest = { showCustomerDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            val filteredCustomers = allPelanggan.filter {
                                namaPelanggan.isBlank() || it.namaPelanggan.contains(namaPelanggan, ignoreCase = true)
                            }.take(6)

                            filteredCustomers.forEach { cust ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(cust.namaPelanggan, fontWeight = FontWeight.Bold)
                                            if (!cust.kontak.isNullOrBlank()) {
                                                Text(cust.kontak, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    onClick = {
                                        namaPelanggan = cust.namaPelanggan
                                        showCustomerDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Deskripsi Nota
                    OutlinedTextField(
                        value = deskripsiNota,
                        onValueChange = { deskripsiNota = it },
                        label = { Text("Deskripsi Nota / Nama Cetakan") },
                        placeholder = { Text("Contoh: Nota Kontan 2 Ply / Surat Jalan 3 Ply") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Description, contentDescription = "Nota")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Section 2: Skenario Penjualan (Satuan vs Paket/Borongan)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sell,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Skenario Penjualan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }

                        // Badge Mode Terpilih
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isBorongan) Color(0xFF7C3AED).copy(alpha = 0.12f) else colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (isBorongan) "PAKET / BORONGAN" else "SATUAN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isBorongan) Color(0xFF6D28D9) else colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Selector Tipe Penjualan: Satuan vs Paket/Borongan
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Opsi Satuan
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isBorongan = false },
                            color = if (!isBorongan) colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewAgenda,
                                    contentDescription = null,
                                    tint = if (!isBorongan) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Satuan (per Buku/Pcs)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (!isBorongan) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isBorongan) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Opsi Paket / Borongan
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isBorongan = true },
                            color = if (isBorongan) Color(0xFF7C3AED) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AllInclusive,
                                    contentDescription = null,
                                    tint = if (isBorongan) Color.White else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Paket / Borongan",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isBorongan) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isBorongan) Color.White else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Field Input Berdasarkan Mode:
                    if (!isBorongan) {
                        // MODE SATUAN: Qty, Satuan, Harga Satuan
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = qtySatuanInput,
                                onValueChange = { qtySatuanInput = it },
                                label = { Text("Qty Order") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = satuanNamaInput,
                                onValueChange = { satuanNamaInput = it },
                                label = { Text("Satuan") },
                                placeholder = { Text("Buku / Rim / Pcs") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = hargaPerSatuanInput,
                            onValueChange = { hargaPerSatuanInput = it },
                            label = { Text("Harga per Satuan (Rp)") },
                            placeholder = { Text("Contoh: 25000") },
                            leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold, color = colorScheme.primary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    } else {
                        // MODE PAKET / BORONGAN: Total Nilai Borongan Mutlak & Catatan Hasil Jadi Paket
                        OutlinedTextField(
                            value = totalBoronganInput,
                            onValueChange = { totalBoronganInput = it },
                            label = { Text("Total Nilai Borongan (Uang Masuk Mutlak)") },
                            placeholder = { Text("Contoh: 260000") },
                            leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF7C3AED),
                                focusedLabelColor = Color(0xFF7C3AED)
                            )
                        )

                        // Catatan Hasil Jadi Paket (Acuan Repeat Order)
                        OutlinedTextField(
                            value = catatanHasilJadi,
                            onValueChange = { catatanHasilJadi = it },
                            label = { Text("Catatan Hasil Jadi Paket (Wajib Acuan Repeat Order)") },
                            placeholder = { Text("Contoh: Jadi 60 pcs isi 100 lembar") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = "Hasil Jadi",
                                    tint = Color(0xFF7C3AED)
                                )
                            },
                            supportingText = {
                                Text(
                                    text = "Tercatat jelas pada nota dan riwayat agar pelanggan dapat repeat order dengan spesifikasi persis.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6D28D9)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = qtyUnitEstimasiBorongan,
                                onValueChange = { qtyUnitEstimasiBorongan = it },
                                label = { Text("Qty Paket / Unit Terhitung") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Harga Satuan Ref:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    val refPrice = if (effectiveQty > 0) totalPenjualan / effectiveQty else 0.0
                                    Text(
                                        text = "${formatRupiah(refPrice)} / unit",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Total Harga Penjualan Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.primaryContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL HARGA JUAL (OMSET)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )
                                Text(
                                    text = if (isBorongan) "Skenario Borongan Mutlak" else "Kalkulasi Satuan: $effectiveQty × ${formatRupiah(totalPenjualan / effectiveQty)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatRupiah(totalPenjualan),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Struktur Modal & Alokasi Pos Kas (HPP Nota Dinamis)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Kalkulasi Modal & Alokasi Pos Kas (HPP Nota)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Fleksibilitas input manual, pembulatan override & sapu jagat laba",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 1. KERTAS NCR: Nominal Mutlak Manual
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "1. Bahan Kertas NCR",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Plot Utuh Dompet Kertas",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = formatRupiah(nominalKertas),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = nominalKertasInput,
                            onValueChange = { nominalKertasInput = it },
                            label = { Text("Nominal Mutlak Modal Kertas NCR (Rp)") },
                            placeholder = { Text("Contoh: 120000") },
                            leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            supportingText = {
                                Text(
                                    text = "Nominal yang diketik di sini akan dialokasikan utuh 100% ke dompet Kertas.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // 2. TINTA: Otomatis dari setting + Kolom Override / Pembulatan Manual
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "2. Estimasi Tinta Nota",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Otomatis (${(notaSettings.tintaPct * 100).toString().removeSuffix(".0")}%): ${formatRupiah(autoTinta)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = overrideTintaInput,
                                onValueChange = { overrideTintaInput = it },
                                label = { Text("Nominal Override / Bulatkan (Rp)") },
                                placeholder = { Text(formatAngka(autoTinta)) },
                                leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (overrideTintaInput.isNotBlank()) colorScheme.tertiaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.width(130.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = if (overrideTintaInput.isNotBlank()) "Override Terpasang:" else "Pakai Otomatis:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatRupiah(nominalTinta),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (overrideTintaInput.isNotBlank()) colorScheme.onTertiaryContainer else colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // 3. WASTE KERTAS: Otomatis dari setting + Kolom Override / Pembulatan Manual
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "3. Waste / Rusak Kertas",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Otomatis (${(notaSettings.wastePct * 100).toString().removeSuffix(".0")}%): ${formatRupiah(autoWaste)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = overrideWasteInput,
                                onValueChange = { overrideWasteInput = it },
                                label = { Text("Nominal Override / Bulatkan (Rp)") },
                                placeholder = { Text(formatAngka(autoWaste)) },
                                leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (overrideWasteInput.isNotBlank()) colorScheme.tertiaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.width(130.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = if (overrideWasteInput.isNotBlank()) "Override Terpasang:" else "Pakai Otomatis:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatRupiah(nominalWaste),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (overrideWasteInput.isNotBlank()) colorScheme.onTertiaryContainer else colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // 4. PENGEMASAN: Master Harga dari setting * Qty Plastik
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "4. Pengemasan (Plastik)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Master Setting: ${formatRupiah(notaSettings.masterHargaPengemasan)} / plastik",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = qtyPlastikInput,
                                onValueChange = { qtyPlastikInput = it },
                                label = { Text("Qty Plastik Digunakan") },
                                placeholder = { Text("0") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.width(130.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Total Kemasan:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatRupiah(nominalPengemasan),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // 5. POS OPSIONAL (Tenaga Kerja, Listrik, Maintenance)
                    // WAJIB BISA DIISI 0 ATAU DIKOSONGKAN
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "5. Pos Opsional (Wajib Bisa Diisi 0 / Dikosongkan)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Pos yang diisi 0% atau dikosongkan tidak dipotong modal; porsi dananya otomatis disapu menjadi Laba Bersih.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )

                        // Tenaga Kerja
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = tenagaKerjaPctInput,
                                onValueChange = { tenagaKerjaPctInput = it },
                                label = { Text("Tenaga Kerja (%)") },
                                placeholder = { Text("0") },
                                trailingIcon = { Text("%", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = overrideTenagaInput,
                                onValueChange = { overrideTenagaInput = it },
                                label = { Text("Nominal (Rp)") },
                                placeholder = { Text(formatAngka(autoTenaga)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // Listrik
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = listrikPctInput,
                                onValueChange = { listrikPctInput = it },
                                label = { Text("Listrik (%)") },
                                placeholder = { Text("0") },
                                trailingIcon = { Text("%", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = overrideListrikInput,
                                onValueChange = { overrideListrikInput = it },
                                label = { Text("Nominal (Rp)") },
                                placeholder = { Text(formatAngka(autoListrik)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // Maintenance Alat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = maintenancePctInput,
                                onValueChange = { maintenancePctInput = it },
                                label = { Text("Maintenance (%)") },
                                placeholder = { Text("0") },
                                trailingIcon = { Text("%", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = overrideMaintenanceInput,
                                onValueChange = { overrideMaintenanceInput = it },
                                label = { Text("Nominal (Rp)") },
                                placeholder = { Text(formatAngka(autoMaintenance)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Status & Metode Pembayaran
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Status & Pembayaran Kas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }

                    // Pilihan Metode: Bayar Penuh vs DP / Cicilan
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isFull = metodePembayaran == "Bayar Penuh"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    metodePembayaran = "Bayar Penuh"
                                    statusPembayaran = "Lunas"
                                },
                            color = if (isFull) Color(0xFF16A34A) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Bayar Penuh (Lunas)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isFull) FontWeight.Bold else FontWeight.Normal,
                                color = if (isFull) Color.White else colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    metodePembayaran = "Bayar Sebagian"
                                    statusPembayaran = "Belum Lunas"
                                    if (nominalBayarInput.isBlank()) {
                                        nominalBayarInput = formatNominalClean(totalPenjualan * 0.5)
                                    }
                                },
                            color = if (!isFull) Color(0xFFE11D48) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "DP / Cicilan (Pending)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (!isFull) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isFull) Color.White else colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }

                    if (metodePembayaran != "Bayar Penuh") {
                        OutlinedTextField(
                            value = nominalBayarInput,
                            onValueChange = { nominalBayarInput = it },
                            label = { Text("Jumlah Uang Masuk Diterima (Rp)") },
                            placeholder = { Text("0") },
                            leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Shortcut Persentase DP
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.25 to "DP 25%", 0.50 to "DP 50%", 0.75 to "DP 75%", 1.0 to "Penuh").forEach { (ratio, label) ->
                                AssistChip(
                                    onClick = {
                                        nominalBayarInput = formatNominalClean(totalPenjualan * ratio)
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sisa Kekurangan Tagihan:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFB91C1C)
                                )
                                Text(
                                    text = formatRupiah(sisaKekurangan),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 5: LIVE PREVIEW KOTAK ESTIMASI LABA BERSIH (Di Atas Tombol Simpan)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_preview_laba_nota"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)), // Deep Emerald Green
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF059669)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Laba",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Live Preview Plotting & Laba",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF047857)
                        ) {
                            Text(
                                text = "Margin: ${String.format(Locale.US, "%.1f", marginLabaPct)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Rekap Baris: Total Penjualan vs Total Modal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Penjualan", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA7F3D0))
                            Text(formatRupiah(totalPenjualan), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Modal Dasar (HPP)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA7F3D0))
                            Text(formatRupiah(totalModalDasar), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFDE68A))
                        }
                    }

                    HorizontalDivider(color = Color(0xFF047857))

                    // ESTIMASI LABA BERSIH (SAPU JAGAT)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF047857).copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ESTIMASI LABA BERSIH (SAPU JAGAT)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7)
                                )
                                Text(
                                    text = "Seluruh sisa omset setelah modal otomatis bermuara ke sini",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                            Text(
                                text = formatRupiah(labaBersihSapuJagat),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = if (labaBersihSapuJagat >= 0) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
                            )
                        }
                    }

                    // Chips Rincian Alokasi Per Dompet
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NotaAllocationChip("Kertas", nominalKertas)
                        NotaAllocationChip("Tinta", nominalTinta)
                        NotaAllocationChip("Waste", nominalWaste)
                        NotaAllocationChip("Kemasan", nominalPengemasan)
                        if (nominalTenaga > 0) NotaAllocationChip("Tenaga", nominalTenaga)
                        if (nominalListrik > 0) NotaAllocationChip("Listrik", nominalListrik)
                        if (nominalMaintenance > 0) NotaAllocationChip("Maint", nominalMaintenance)
                        NotaAllocationChip("Laba Bersih", labaBersihSapuJagat, isHighlight = true)
                    }
                }
            }
        }

        // Section 6: TOMBOL SIMPAN TRANSAKSI
        item {
            Button(
                onClick = {
                    if (deskripsiNota.isBlank()) {
                        Toast.makeText(context, "Silakan isi nama / deskripsi nota!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (totalPenjualan <= 0.0) {
                        Toast.makeText(context, "Total penjualan tidak boleh 0!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val prefixCustomer = if (namaPelanggan.isNotBlank()) "$namaPelanggan - " else ""
                    val boronganTag = if (isBorongan) {
                        if (catatanHasilJadi.isNotBlank()) " [Borongan: $catatanHasilJadi]" else " [Borongan]"
                    } else ""
                    val savedNamaPesanan = "$prefixCustomer$deskripsiNota$boronganTag"

                    val finalHargaSatuan = if (isBorongan) {
                        totalPenjualan / effectiveQty
                    } else {
                        parseNominal(hargaPerSatuanInput) ?: 0.0
                    }
                    val finalSatuan = if (isBorongan) "Paket" else satuanNamaInput

                    // Kalkulasi Snapshot Per Unit / Persentase:
                    val snapshotKertas = if (effectiveQty > 0) nominalKertas / effectiveQty else nominalKertas
                    val snapshotTinta = if (effectiveQty > 0) nominalTinta / effectiveQty else nominalTinta
                    val snapshotKemasan = if (qtyPlastik > 0) nominalPengemasan / qtyPlastik else notaSettings.masterHargaPengemasan
                    val snapshotWastePct = if (totalPenjualan > 0) nominalWaste / totalPenjualan else 0.0
                    val snapshotTenagaPct = if (totalPenjualan > 0) nominalTenaga / totalPenjualan else 0.0
                    val snapshotListrikPct = if (totalPenjualan > 0) nominalListrik / totalPenjualan else 0.0
                    val snapshotMaintPct = if (totalPenjualan > 0) nominalMaintenance / totalPenjualan else 0.0

                    if (editingOrder != null) {
                        // Perbarui Order yang Sedang Diedit
                        val updated = editingOrder.copy(
                            tanggalOrder = tanggalOrder,
                            namaPesanan = savedNamaPesanan,
                            qtyOrder = effectiveQty,
                            satuan = finalSatuan,
                            hargaSatuan = finalHargaSatuan,
                            jumlahPlastikPengemasan = qtyPlastik,
                            status = if (actualPaid >= totalPenjualan) "Lunas" else "Belum Lunas",
                            jumlahDibayar = actualPaid,
                            metodePembayaran = metodePembayaran,
                            kategori = "Nota",
                            hppKertasSnapshot = snapshotKertas,
                            hppTintaSnapshot = snapshotTinta,
                            hppPengemasanSnapshot = snapshotKemasan,
                            wastePctSnapshot = snapshotWastePct,
                            tenagaKerjaPctSnapshot = snapshotTenagaPct,
                            listrikPctSnapshot = snapshotListrikPct,
                            maintenancePctSnapshot = snapshotMaintPct
                        )
                        viewModel.updateOrder(updated)
                        Toast.makeText(context, "Transaksi Nota berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Simpan Order Nota Baru
                        viewModel.insertNotaOrder(
                            tanggal = tanggalOrder,
                            nama = savedNamaPesanan,
                            qty = effectiveQty,
                            satuan = finalSatuan,
                            harga = finalHargaSatuan,
                            plastik = qtyPlastik,
                            status = if (actualPaid >= totalPenjualan) "Lunas" else "Belum Lunas",
                            jumlahDibayar = actualPaid,
                            metodePembayaran = metodePembayaran,
                            hppKertasSnapshot = snapshotKertas,
                            hppTintaSnapshot = snapshotTinta,
                            hppPengemasanSnapshot = snapshotKemasan,
                            wastePctSnapshot = snapshotWastePct,
                            tenagaKerjaPctSnapshot = snapshotTenagaPct,
                            listrikPctSnapshot = snapshotListrikPct,
                            maintenancePctSnapshot = snapshotMaintPct
                        )
                        Toast.makeText(context, "Order Nota berhasil disimpan & diplot ke dompet kas!", Toast.LENGTH_SHORT).show()
                    }

                    onOrderSaved()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_order_nota_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (editingOrder != null) Icons.Default.CheckCircle else Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingOrder != null) "Perbarui Transaksi Order Nota" else "Simpan Transaksi & Auto-Plotting Kas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Section 7: KARTU CATATAN (FOOTER INFO) DI BAGIAN PALING BAWAH
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("footer_info_card_nota"),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Panduan Sistem & Rumus Auto-Plotting Nota",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "1. Aturan Pembulatan Manual:\n" +
                                "Hasil hitung persentase otomatis untuk Tinta dan Waste Kertas dapat langsung ditimpa (override) pada kolom yang disediakan. Nilai override inilah yang langsung diserap ke snapshot modal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "2. Logika Sapu Jagat (Laba Bersih):\n" +
                                "Seluruh sisa uang dari harga jual setelah dikurangi pengisian modal (Kertas NCR, Tinta, Waste, Pengemasan, dan Pos Opsional)—termasuk pos yang diisi 0 atau dikosongkan—secara otomatis bermuara ke dompet Laba Bersih tanpa kebocoran dana.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "3. Rumus Plotting Transaksi:\n" +
                                "Total Penjualan - (Kertas NCR + Tinta + Waste + Pengemasan + Pos Opsional) = Laba Bersih",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.primary
                    )

                    Text(
                        text = "4. Ekosistem Mandiri:\n" +
                                "Pengaturan harga dan kalkulasi operasional nota terisolasi sepenuhnya dari master HPP cetak dokumen HVS umum.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Sub-Menu 2: Setting Harga Khusus Modul Nota (Terisolasi dari HVS Umum)
 */
@Composable
fun OrderNotaSettingHargaTab(viewModel: FinanceViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val currentSettings by viewModel.notaSettings.collectAsStateWithLifecycle()

    var hargaPengemasanInput by remember(currentSettings) {
        mutableStateOf(currentSettings.masterHargaPengemasan.toInt().toString())
    }
    var wastePctInput by remember(currentSettings) {
        mutableStateOf((currentSettings.wastePct * 100).toString().removeSuffix(".0"))
    }
    var tintaPctInput by remember(currentSettings) {
        mutableStateOf((currentSettings.tintaPct * 100).toString().removeSuffix(".0"))
    }
    var tenagaKerjaPctInput by remember(currentSettings) {
        mutableStateOf((currentSettings.tenagaKerjaPct * 100).toString().removeSuffix(".0"))
    }
    var listrikPctInput by remember(currentSettings) {
        mutableStateOf((currentSettings.listrikPct * 100).toString().removeSuffix(".0"))
    }
    var maintenancePctInput by remember(currentSettings) {
        mutableStateOf((currentSettings.maintenancePct * 100).toString().removeSuffix(".0"))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        // Banner Penjelasan Isolasi Pengaturan
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Pengaturan Harga Nota Terisolasi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                        Text(
                            text = "Nilai persentase dan master kemasan di bawah ini hanya berlaku untuk Modul Order Nota dan tidak mengubah pengaturan finansial dokumen HVS umum.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Card 1: Master Harga Pengemasan
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Master Harga Pengemasan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = hargaPengemasanInput,
                        onValueChange = { hargaPengemasanInput = it },
                        label = { Text("Harga Plastik Pengemasan (Rp / lembar)") },
                        placeholder = { Text("Contoh: 300") },
                        leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        supportingText = {
                            Text("Akan dikalikan dengan jumlah pemakaian plastik pada form order transaksi.")
                        }
                    )
                }
            }
        }

        // Card 2: Persentase Default Operasional Nota
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Persentase Default Operasional Nota",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = wastePctInput,
                        onValueChange = { wastePctInput = it },
                        label = { Text("Waste / Rusak Kertas (%)") },
                        placeholder = { Text("5") },
                        trailingIcon = { Text("%", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tintaPctInput,
                        onValueChange = { tintaPctInput = it },
                        label = { Text("Estimasi Tinta Nota (%)") },
                        placeholder = { Text("5") },
                        trailingIcon = { Text("%", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tenagaKerjaPctInput,
                        onValueChange = { tenagaKerjaPctInput = it },
                        label = { Text("Tenaga Kerja / Tukang Potong (%)") },
                        placeholder = { Text("7") },
                        trailingIcon = { Text("%", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = listrikPctInput,
                        onValueChange = { listrikPctInput = it },
                        label = { Text("Beban Listrik (%)") },
                        placeholder = { Text("2") },
                        trailingIcon = { Text("%", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = maintenancePctInput,
                        onValueChange = { maintenancePctInput = it },
                        label = { Text("Maintenance / Servis Mesin (%)") },
                        placeholder = { Text("5") },
                        trailingIcon = { Text("%", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Action Buttons: Simpan & Reset
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.resetNotaSettingsToDefault()
                        Toast.makeText(context, "Pengaturan nota dikembalikan ke default!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Default")
                }

                Button(
                    onClick = {
                        val newSettings = NotaSettings(
                            masterHargaPengemasan = parseNominal(hargaPengemasanInput) ?: 300.0,
                            wastePct = ((wastePctInput.toDoubleOrNull() ?: 5.0) / 100.0),
                            tintaPct = ((tintaPctInput.toDoubleOrNull() ?: 5.0) / 100.0),
                            tenagaKerjaPct = ((tenagaKerjaPctInput.toDoubleOrNull() ?: 7.0) / 100.0),
                            listrikPct = ((listrikPctInput.toDoubleOrNull() ?: 2.0) / 100.0),
                            maintenancePct = ((maintenancePctInput.toDoubleOrNull() ?: 5.0) / 100.0)
                        )
                        viewModel.saveNotaSettings(newSettings)
                        Toast.makeText(context, "Pengaturan tarif nota berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Setting")
                }
            }
        }
    }
}

/**
 * Sub-Menu 3: Riwayat Order Nota (Rekam Jejak Borongan & Satuan Pelanggan)
 */
@Composable
fun OrderNotaRiwayatTab(
    notaOrders: List<TransaksiOrderMasuk>,
    viewModel: FinanceViewModel,
    onEditOrder: (TransaksiOrderMasuk) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") } // "Semua", "Lunas", "Belum Lunas", "Borongan", "Satuan"

    var orderToDelete by remember { mutableStateOf<TransaksiOrderMasuk?>(null) }
    var orderToPay by remember { mutableStateOf<TransaksiOrderMasuk?>(null) }
    var paymentNominalInput by remember { mutableStateOf("") }

    val filteredOrders = remember(notaOrders, searchQuery, selectedFilter) {
        notaOrders.filter { order ->
            val matchQuery = searchQuery.isBlank() ||
                    order.namaPesanan.contains(searchQuery, ignoreCase = true) ||
                    order.tanggalOrder.contains(searchQuery, ignoreCase = true) ||
                    (order.catatanHasilJadi ?: "").contains(searchQuery, ignoreCase = true)

            val matchFilter = when (selectedFilter) {
                "Lunas" -> order.status.equals("Lunas", ignoreCase = true)
                "Belum Lunas" -> !order.status.equals("Lunas", ignoreCase = true)
                "Borongan" -> order.isBorongan
                "Satuan" -> !order.isBorongan
                else -> true
            }

            matchQuery && matchFilter
        }
    }

    val totalOmset = remember(filteredOrders) { filteredOrders.sumOf { it.totalPendapatan } }
    val totalPending = remember(filteredOrders) { filteredOrders.sumOf { it.sisaKekurangan } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        // Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Order Nota", style = MaterialTheme.typography.labelSmall, color = colorScheme.primary)
                        Text("${filteredOrders.size} Transaksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        Text("Omset: ${formatRupiah(totalOmset)}", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = if (totalPending > 0) Color(0xFFFEF2F2) else colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Belum Lunas (Sisa)", style = MaterialTheme.typography.labelSmall, color = if (totalPending > 0) Color(0xFFDC2626) else colorScheme.onSurfaceVariant)
                        Text(formatRupiah(totalPending), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (totalPending > 0) Color(0xFFDC2626) else colorScheme.onSurface)
                        Text(if (totalPending > 0) "Perlu Follow-up" else "Semua Lunas", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Search & Filter
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari pelanggan, deskripsi, hasil jadi...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Semua", "Borongan", "Satuan", "Belum Lunas", "Lunas").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) }
                        )
                    }
                }
            }
        }

        if (filteredOrders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum Ada Transaksi Nota",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Simpan pesanan nota dari tab 'Order Transaksi' untuk melihat riwayat di sini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredOrders, key = { it.idOrder }) { order ->
                OrderNotaItemCard(
                    order = order,
                    onEdit = { onEditOrder(order) },
                    onPay = {
                        orderToPay = order
                        paymentNominalInput = formatNominalClean(order.sisaKekurangan)
                    },
                    onDelete = { orderToDelete = order }
                )
            }
        }
    }

    // Dialog Konfirmasi Hapus Order
    if (orderToDelete != null) {
        val o = orderToDelete!!
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = colorScheme.error) },
            title = { Text("Hapus Order Nota #${o.idOrder}?") },
            text = {
                Text("Transaksi '${o.cleanOrderTitle}' senilai ${formatRupiah(o.totalPendapatan)} akan dihapus permanen dan saldo kas terplotting akan dikembalikan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteOrder(o)
                        orderToDelete = null
                        Toast.makeText(context, "Order nota berhasil dihapus!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Pelunasan Cepat
    if (orderToPay != null) {
        val o = orderToPay!!
        AlertDialog(
            onDismissRequest = { orderToPay = null },
            icon = { Icon(Icons.Default.PriceCheck, contentDescription = null, tint = Color(0xFF16A34A)) },
            title = { Text("Pembayaran Susulan #${o.idOrder}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pesanan: ${o.cleanOrderTitle}")
                    Text("Sisa Kekurangan: ${formatRupiah(o.sisaKekurangan)}", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    OutlinedTextField(
                        value = paymentNominalInput,
                        onValueChange = { paymentNominalInput = it },
                        label = { Text("Nominal Dibayarkan (Rp)") },
                        leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = parseNominal(paymentNominalInput) ?: 0.0
                        if (amount <= 0.0) {
                            Toast.makeText(context, "Nominal pembayaran tidak valid!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addOrderPayment(o, amount)
                        orderToPay = null
                        Toast.makeText(context, "Pembayaran berhasil dicatat!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Catat Pembayaran")
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToPay = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

/**
 * Kartu Rincian Transaksi Order Nota
 */
@Composable
fun OrderNotaItemCard(
    order: TransaksiOrderMasuk,
    onEdit: () -> Unit,
    onPay: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var expandedDetails by remember { mutableStateOf(false) }

    val isLunas = order.status.equals("Lunas", ignoreCase = true)
    val isBorongan = order.isBorongan
    val hasilJadi = order.catatanHasilJadi

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_nota_card_${order.idOrder}"),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Badges (Borongan / Satuan & Lunas / Belum Lunas) + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge Tipe
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isBorongan) Color(0xFF7C3AED).copy(alpha = 0.15f) else colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (isBorongan) "PAKET BORONGAN" else "SATUAN",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isBorongan) Color(0xFF6D28D9) else colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    // Badge Status
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isLunas) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = if (isLunas) "Lunas" else "Belum Lunas",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (isLunas) Color(0xFF15803D) else Color(0xFFB91C1C),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "${order.tanggalOrder} • #${order.idOrder}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            // Title & Description
            Column {
                Text(
                    text = order.cleanOrderTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "${order.qtyOrder} ${order.satuan} • @${formatRupiah(order.hargaSatuan)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            // Highlight Khusus: Catatan Hasil Jadi Paket (Acuan Repeat Order)
            if (isBorongan && !hasilJadi.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF5F3FF), // Light purple
                    border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Catatan Hasil Jadi (Acuan Repeat Order):",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9)
                            )
                            Text(
                                text = hasilJadi,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4C1D95)
                            )
                        }
                    }
                }
            }

            // Finansial: Total Penjualan, Dibayar, Sisa
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Tagihan", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                        Text(formatRupiah(order.totalPendapatan), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Terbayar", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                        Text(formatRupiah(order.effectiveJumlahDibayar), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF15803D))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Sisa Kekurangan", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                        Text(
                            text = if (order.sisaKekurangan > 0) formatRupiah(order.sisaKekurangan) else "Rp 0",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (order.sisaKekurangan > 0) Color(0xFFB91C1C) else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expandable Plotting Dompet Kas
            AnimatedVisibility(
                visible = expandedDetails,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Plotting Dompet Kas Riil:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kertas NCR:", style = MaterialTheme.typography.bodySmall)
                        Text(formatRupiah(order.alokasiKertas), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tinta Nota:", style = MaterialTheme.typography.bodySmall)
                        Text(formatRupiah(order.alokasiTinta), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Waste Kertas:", style = MaterialTheme.typography.bodySmall)
                        Text(formatRupiah(order.alokasiWaste), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pengemasan (${order.jumlahPlastikPengemasan} plastik):", style = MaterialTheme.typography.bodySmall)
                        Text(formatRupiah(order.alokasiPengemasan), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    if (order.alokasiTenagaKerja > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tenaga Kerja:", style = MaterialTheme.typography.bodySmall)
                            Text(formatRupiah(order.alokasiTenagaKerja), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (order.alokasiListrik > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Listrik:", style = MaterialTheme.typography.bodySmall)
                            Text(formatRupiah(order.alokasiListrik), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (order.alokasiMaintenance > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Maintenance:", style = MaterialTheme.typography.bodySmall)
                            Text(formatRupiah(order.alokasiMaintenance), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Laba Bersih (Sapu Jagat):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        Text(formatRupiah(order.alokasiSisaLaba), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { expandedDetails = !expandedDetails },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = if (expandedDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (expandedDetails) "Tutup Rincian" else "Rincian Plotting Kas")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!isLunas) {
                        FilledTonalButton(
                            onClick = onPay,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pelunasan", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/**
 * Chip pembantu untuk live preview rincian alokasi nota
 */
@Composable
fun NotaAllocationChip(label: String, value: Double, isHighlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isHighlight) Color(0xFF10B981) else Color.White.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isHighlight) Color(0xFF064E3B) else Color(0xFFA7F3D0)
            )
            Text(
                text = formatRupiah(value),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) Color(0xFF064E3B) else Color.White
            )
        }
    }
}

// --- Helper Functions for String Extraction ---
private fun extractCustomerName(fullName: String): String {
    val clean = fullName.split(" - ").firstOrNull() ?: ""
    return clean.replace(Regex("""\s*\[(?:Borongan|Hasil Jadi)[^\]]*\]""", RegexOption.IGNORE_CASE), "").trim()
}

private fun extractOrderDescription(fullName: String): String {
    val parts = fullName.split(" - ")
    val desc = if (parts.size > 1) parts.subList(1, parts.size).joinToString(" - ") else fullName
    return desc.replace(Regex("""\s*\[(?:Borongan|Hasil Jadi)[^\]]*\]""", RegexOption.IGNORE_CASE), "").trim()
}

private fun parseNominal(str: String): Double? {
    val clean = str.replace(".", "").replace(",", "").trim()
    return clean.toDoubleOrNull()
}

private fun formatNominalClean(value: Double): String {
    return if (value <= 0.0) "" else String.format(Locale.US, "%.0f", value)
}
