package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.viewmodel.AccountDashboardRow
import com.example.ui.viewmodel.DashboardSummary
import com.example.ui.viewmodel.FinanceViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// Data model for Audit Session Record
data class AuditRecord(
    val id: String = System.currentTimeMillis().toString(),
    val timestamp: String,
    val saldoSistem: Double,
    val saldoFisik: Double,
    val selisih: Double,
    val isAdjusted: Boolean,
    val keterangan: String = "",
    val detailPenyesuaian: String = ""
)

object AuditStorageHelper {
    private const val PREF_NAME = "pgd_audit_cash_prefs"
    private const val KEY_AUDIT_HISTORY = "audit_history_json"

    fun loadAuditHistory(context: Context): List<AuditRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_AUDIT_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<AuditRecord>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AuditRecord(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        timestamp = obj.optString("timestamp", ""),
                        saldoSistem = obj.optDouble("saldoSistem", 0.0),
                        saldoFisik = obj.optDouble("saldoFisik", 0.0),
                        selisih = obj.optDouble("selisih", 0.0),
                        isAdjusted = obj.optBoolean("isAdjusted", false),
                        keterangan = obj.optString("keterangan", ""),
                        detailPenyesuaian = obj.optString("detailPenyesuaian", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.id.toLongOrNull() ?: 0L }
    }

    fun saveAuditRecord(context: Context, record: AuditRecord) {
        val currentList = loadAuditHistory(context).toMutableList()
        currentList.add(0, record)
        val arr = JSONArray()
        for (item in currentList.take(50)) { // Keep last 50 records
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("saldoSistem", item.saldoSistem)
                put("saldoFisik", item.saldoFisik)
                put("selisih", item.selisih)
                put("isAdjusted", item.isAdjusted)
                put("keterangan", item.keterangan)
                put("detailPenyesuaian", item.detailPenyesuaian)
            }
            arr.put(obj)
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AUDIT_HISTORY, arr.toString())
            .apply()
    }
}

fun formatAuditRupiah(amount: Double): String {
    val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        currencySymbol = "Rp "
        groupingSeparator = '.'
        monetaryDecimalSeparator = ','
    }
    val formatter = DecimalFormat("Rp #,##0", symbols)
    return formatter.format(amount)
}

fun parseAuditDouble(str: String): Double? {
    val clean = str.replace("Rp", "")
        .replace(".", "")
        .replace(",", ".")
        .replace(" ", "")
        .trim()
    return clean.toDoubleOrNull()
}

// Data class for Pos visual styling
data class PosVisualTheme(
    val icon: ImageVector,
    val iconTint: Color,
    val bgPastel: Color,
    val borderAccent: Color
)

fun getPosVisualTheme(rawName: String): PosVisualTheme {
    val clean = rawName.lowercase()
    return when {
        clean.contains("kertas") -> PosVisualTheme(
            icon = Icons.Default.Description,
            iconTint = Color(0xFF1E88E5),
            bgPastel = Color(0xFFE3F2FD),
            borderAccent = Color(0xFF90CAF9)
        )
        clean.contains("tinta") -> PosVisualTheme(
            icon = Icons.Default.InvertColors,
            iconTint = Color(0xFF8E24AA),
            bgPastel = Color(0xFFF3E5F5),
            borderAccent = Color(0xFFCE93D8)
        )
        clean.contains("pengemasan") || clean.contains("kemas") -> PosVisualTheme(
            icon = Icons.Default.Inventory2,
            iconTint = Color(0xFFFB8C00),
            bgPastel = Color(0xFFFFF3E0),
            borderAccent = Color(0xFFFFCC80)
        )
        clean.contains("waste") || clean.contains("rusak") -> PosVisualTheme(
            icon = Icons.Default.DeleteOutline,
            iconTint = Color(0xFFE53935),
            bgPastel = Color(0xFFFFEBEE),
            borderAccent = Color(0xFFEF9A9A)
        )
        clean.contains("tenaga") || clean.contains("gaji") || clean.contains("kerja") -> PosVisualTheme(
            icon = Icons.Default.Badge,
            iconTint = Color(0xFF43A047),
            bgPastel = Color(0xFFE8F5E9),
            borderAccent = Color(0xFFA5D6A7)
        )
        clean.contains("listrik") || clean.contains("pln") -> PosVisualTheme(
            icon = Icons.Default.FlashOn,
            iconTint = Color(0xFFFBC02D),
            bgPastel = Color(0xFFFFFDE7),
            borderAccent = Color(0xFFFFF59D)
        )
        clean.contains("maintenance") || clean.contains("alat") || clean.contains("rawat") -> PosVisualTheme(
            icon = Icons.Default.Build,
            iconTint = Color(0xFF6D4C41),
            bgPastel = Color(0xFFEFEBE9),
            borderAccent = Color(0xFFBCAAA4)
        )
        clean.contains("laba") || clean.contains("untung") -> PosVisualTheme(
            icon = Icons.Default.MonetizationOn,
            iconTint = Color(0xFF00897B),
            bgPastel = Color(0xFFE0F2F1),
            borderAccent = Color(0xFF80CBC4)
        )
        else -> PosVisualTheme(
            icon = Icons.Default.AccountBalanceWallet,
            iconTint = Color(0xFF6A4C93),
            bgPastel = Color(0xFFF3EDFA),
            borderAccent = Color(0xFFD3C5EE)
        )
    }
}

@Composable
fun AuditSelisihKasScreen(
    viewModel: FinanceViewModel,
    summary: DashboardSummary? = null,
    accounts: List<MasterAkunSaldo>? = null
) {
    val context = LocalContext.current
    val liveSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val liveAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val allocationSummary by viewModel.allocationComparisonSummary.collectAsStateWithLifecycle()

    val actualSummary = summary ?: liveSummary
    val actualAccounts = accounts ?: liveAccounts

    // State for Audit Input
    var useSystemLiveBalance by remember { mutableStateOf(true) }
    var manualSaldoSistemInput by remember { mutableStateOf("") }
    var saldoFisikInput by remember { mutableStateOf("") }
    var auditNote by remember { mutableStateOf("") }
    var showDenominationCalculator by remember { mutableStateOf(false) }

    // State for Decision: Apakah ingin melakukan penyesuaian saldo ke dompet?
    // null: belum memilih, false: TIDAK (Hanya Catat), true: YA (Lakukan Penyesuaian)
    var isAdjustmentChosen by remember { mutableStateOf<Boolean?>(null) }

    // Map of idAkun -> Adjustment Amount String (can be positive or negative)
    val walletAdjustments = remember { mutableStateMapOf<Int, String>() }

    // Load audit history
    var auditHistory by remember { mutableStateOf(AuditStorageHelper.loadAuditHistory(context)) }

    // Calculations
    val liveSystemBalance = actualSummary.grandTotalSisaRiil
    val actualSaldoSistem = if (useSystemLiveBalance) {
        liveSystemBalance
    } else {
        parseAuditDouble(manualSaldoSistemInput) ?: liveSystemBalance
    }

    val actualSaldoFisik = parseAuditDouble(saldoFisikInput) ?: 0.0
    val hasEnteredFisik = saldoFisikInput.isNotBlank()
    val selisih = if (hasEnteredFisik) actualSaldoFisik - actualSaldoSistem else 0.0

    // Allocation balancing calculations
    val totalAllocated = walletAdjustments.values.sumOf { parseAuditDouble(it) ?: 0.0 }
    val remainingToAllocate = selisih - totalAllocated
    val isAllocationBalanced = abs(remainingToAllocate) < 1.0 && abs(totalAllocated) > 0.0

    // Tab for Audit Form vs Riwayat Audit
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Form Audit, 1: Riwayat Audit

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("audit_kas_screen_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(1.2.dp, Color(0xFFE2D9F3)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF6A4C93), Color(0xFF8E7AB5))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Audit Selisih Kas & Rekonsiliasi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D1E4B)
                            )
                            Text(
                                text = "Pengecekan fisik kas mandiri & evaluasi disiplin kas tanpa memotong dompet secara sepihak.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6A5C80)
                            )
                        }
                    }

                    // Navigation Tabs (Form Audit vs Riwayat Audit)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3EDFA))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedSubTab = 0 }
                                .testTag("tab_form_audit"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedSubTab == 0) Color.White else Color.Transparent,
                            border = if (selectedSubTab == 0) BorderStroke(1.dp, Color(0xFFD3C5EE)) else null,
                            shadowElevation = if (selectedSubTab == 0) 1.dp else 0.dp
                        ) {
                            Text(
                                text = "Formulir Audit Kas",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedSubTab == 0) Color(0xFF3B2369) else Color(0xFF7A6E91),
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    auditHistory = AuditStorageHelper.loadAuditHistory(context)
                                    selectedSubTab = 1
                                }
                                .testTag("tab_riwayat_audit"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedSubTab == 1) Color.White else Color.Transparent,
                            border = if (selectedSubTab == 1) BorderStroke(1.dp, Color(0xFFD3C5EE)) else null,
                            shadowElevation = if (selectedSubTab == 1) 1.dp else 0.dp
                        ) {
                            Text(
                                text = "Riwayat Audit (${auditHistory.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedSubTab == 1) Color(0xFF3B2369) else Color(0xFF7A6E91),
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        if (selectedSubTab == 0) {
            // ==========================================
            // BAGIAN 1: INPUT & RINGKASAN SELISIH
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.2.dp, Color(0xFFE2D9F3)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFEDE4FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "1",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6A4C93),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Text(
                                text = "PENGHITUNGAN & PERBANDINGAN SALDO",
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B2369)
                            )
                        }

                        // Saldo Tercatat di Sistem
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Saldo Tercatat di Sistem:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2D1E4B)
                                )
                                TextButton(
                                    onClick = { useSystemLiveBalance = !useSystemLiveBalance },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (useSystemLiveBalance) Icons.Default.Lock else Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFF6A4C93)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (useSystemLiveBalance) "Otomatis" else "Ubah Manual",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF6A4C93),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (useSystemLiveBalance) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFFF7F3FC),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE5DDF3))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = Color(0xFF6A4C93),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Total Kas Riil Sistem",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF554B6E)
                                            )
                                        }
                                        Text(
                                            text = formatAuditRupiah(liveSystemBalance),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF3B2369)
                                        )
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = manualSaldoSistemInput,
                                    onValueChange = { manualSaldoSistemInput = it },
                                    label = { Text("Koreksi Saldo Tercatat Sistem (Rp)") },
                                    placeholder = { Text("Contoh: ${liveSystemBalance.toLong()}") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_manual_saldo_sistem"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Jumlah Hitung Fisik Uang
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Jumlah Hitung Fisik Uang (Di Tangan / Kas / Bank):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2D1E4B)
                                )
                                TextButton(
                                    onClick = { showDenominationCalculator = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = Color(0xFF6A4C93)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Kalkulator Pecahan",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6A4C93)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = saldoFisikInput,
                                onValueChange = { saldoFisikInput = it },
                                label = { Text("Total Saldo Fisik Riil (Rp)") },
                                placeholder = { Text("Contoh: 15000000") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_saldo_fisik_audit"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = Color(0xFF6A4C93)
                                    )
                                },
                                trailingIcon = {
                                    if (saldoFisikInput.isNotEmpty()) {
                                        IconButton(onClick = { saldoFisikInput = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // Kartu Ringkasan Selisih
                        if (hasEnteredFisik) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = when {
                                    selisih > 0 -> Color(0xFFE8F5E9) // Surplus (Hijau)
                                    selisih < 0 -> Color(0xFFFFEBEE) // Defisit (Merah)
                                    else -> Color(0xFFF3EDFA) // Seimbang 0 (Ungu Netral)
                                },
                                border = BorderStroke(
                                    1.2.dp,
                                    when {
                                        selisih > 0 -> Color(0xFFA5D6A7)
                                        selisih < 0 -> Color(0xFFEF9A9A)
                                        else -> Color(0xFFD3C5EE)
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Ringkasan Hasil Audit",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                selisih > 0 -> Color(0xFF1B5E20)
                                                selisih < 0 -> Color(0xFFB71C1C)
                                                else -> Color(0xFF3B2369)
                                            }
                                        )
                                        Surface(
                                            color = when {
                                                selisih > 0 -> Color(0xFF2E7D32)
                                                selisih < 0 -> Color(0xFFC62828)
                                                else -> Color(0xFF6A4C93)
                                            },
                                            shape = RoundedCornerShape(100.dp)
                                        ) {
                                            Text(
                                                text = when {
                                                    selisih > 0 -> "SURPLUS (LEBIH)"
                                                    selisih < 0 -> "DEFISIT (KURANG)"
                                                    else -> "SEIMBANG (MATCH)"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        color = when {
                                            selisih > 0 -> Color(0xFFC8E6C9)
                                            selisih < 0 -> Color(0xFFFFCDD2)
                                            else -> Color(0xFFE2D9F3)
                                        }
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Saldo Awal / Tercatat:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF554B6E))
                                        Text(formatAuditRupiah(actualSaldoSistem), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2D1E4B))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Saldo Fisik Riil:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF554B6E))
                                        Text(formatAuditRupiah(actualSaldoFisik), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2D1E4B))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Selisih Tercatat:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2D1E4B)
                                        )
                                        Text(
                                            text = "${if (selisih > 0) "+" else ""}${formatAuditRupiah(selisih)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = when {
                                                selisih > 0 -> Color(0xFF2E7D32)
                                                selisih < 0 -> Color(0xFFC62828)
                                                else -> Color(0xFF6A4C93)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Catatan / Keterangan Audit
                        OutlinedTextField(
                            value = auditNote,
                            onValueChange = { auditNote = it },
                            label = { Text("Catatan Audit / Keterangan Rekonsiliasi (Opsional)") },
                            placeholder = { Text("Contoh: Pemeriksaan kas harian shift malam...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_catatan_audit"),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            maxLines = 3
                        )
                    }
                }
            }

            // ==========================================
            // BAGIAN 2: PILIHAN PENYESUAIAN SALDO KE DOMPET
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.2.dp, Color(0xFFE2D9F3)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFEDE4FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "2",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6A4C93),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Text(
                                text = "TINDAKAN PENYESUAIAN SALDO",
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B2369)
                            )
                        }

                        Text(
                            text = "Apakah Anda ingin melakukan penyesuaian saldo ke pos dompet?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2D1E4B)
                        )

                        Text(
                            text = "Pilih 'Tidak' jika Anda hanya ingin mencatat audit sebagai evaluasi tanpa mengubah saldo dompet apapun. Pilih 'Ya' jika ingin menyeimbangkan saldo pos dompet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B5B95)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Option: TIDAK (Hanya Catat Riwayat)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { isAdjustmentChosen = false }
                                    .testTag("option_audit_hanya_catat"),
                                color = if (isAdjustmentChosen == false) Color(0xFFEDE4FF) else Color(0xFFF9F6FC),
                                border = BorderStroke(
                                    width = if (isAdjustmentChosen == false) 2.dp else 1.dp,
                                    color = if (isAdjustmentChosen == false) Color(0xFF6A4C93) else Color(0xFFDFD4F2)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Tidak",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAdjustmentChosen == false) Color(0xFF3B2369) else Color(0xFF554B6E)
                                        )
                                        RadioButton(
                                            selected = isAdjustmentChosen == false,
                                            onClick = { isAdjustmentChosen = false }
                                        )
                                    }
                                    Text(
                                        text = "Hanya Catat Evaluasi (Saldo Dompet Tetap)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6A5C80)
                                    )
                                }
                            }

                            // Option: YA (Lakukan Penyesuaian ke Dompet)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { isAdjustmentChosen = true }
                                    .testTag("option_audit_lakukan_penyesuaian"),
                                color = if (isAdjustmentChosen == true) Color(0xFFEDE4FF) else Color(0xFFF9F6FC),
                                border = BorderStroke(
                                    width = if (isAdjustmentChosen == true) 2.dp else 1.dp,
                                    color = if (isAdjustmentChosen == true) Color(0xFF6A4C93) else Color(0xFFDFD4F2)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Ya",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAdjustmentChosen == true) Color(0xFF3B2369) else Color(0xFF554B6E)
                                        )
                                        RadioButton(
                                            selected = isAdjustmentChosen == true,
                                            onClick = { isAdjustmentChosen = true }
                                        )
                                    }
                                    Text(
                                        text = "Alokasikan Selisih ke Pos Dompet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6A5C80)
                                    )
                                }
                            }
                        }

                        // Tombol Eksekusi untuk Opsi "Tidak"
                        if (isAdjustmentChosen == false) {
                            Button(
                                onClick = {
                                    if (saldoFisikInput.isBlank()) {
                                        Toast.makeText(context, "Silakan masukkan hitungan saldo fisik terlebih dahulu.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val now = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
                                    val record = AuditRecord(
                                        timestamp = now,
                                        saldoSistem = actualSaldoSistem,
                                        saldoFisik = actualSaldoFisik,
                                        selisih = selisih,
                                        isAdjusted = false,
                                        keterangan = auditNote.ifBlank { "Evaluasi selisih kas fisik tanpa penyesuaian saldo" },
                                        detailPenyesuaian = "Tidak ada mutasi dompet yang dilakukan."
                                    )
                                    AuditStorageHelper.saveAuditRecord(context, record)
                                    auditHistory = AuditStorageHelper.loadAuditHistory(context)
                                    Toast.makeText(context, "Riwayat audit berhasil disimpan sebagai bahan evaluasi!", Toast.LENGTH_LONG).show()
                                    saldoFisikInput = ""
                                    auditNote = ""
                                    isAdjustmentChosen = null
                                    selectedSubTab = 1
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_simpan_audit_hanya_catat"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6A4C93),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simpan Hasil Audit (Bahan Evaluasi)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // BAGIAN 3: ALOKASI PENYESUAIAN POS DOMPET (KONSISTEN DENGAN ANALISIS POS)
            // ==========================================
            if (isAdjustmentChosen == true) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.2.dp, Color(0xFFE2D9F3)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFEDE4FF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "3",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6A4C93),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Text(
                                    text = "FORM ALOKASI PENYESUAIAN POS",
                                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B2369)
                                )
                            }

                            Text(
                                text = "Tentukan pos kas mana saja yang akan disesuaikan (dikurangi atau ditambahi). Total penyesuaian harus seimbang dengan selisih yang ditemukan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B5B95)
                            )

                            // Status Balancing Bar
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isAllocationBalanced) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                border = BorderStroke(1.2.dp, if (isAllocationBalanced) Color(0xFFA5D6A7) else Color(0xFFFFCC80))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Status Keseimbangan Alokasi",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAllocationBalanced) Color(0xFF1B5E20) else Color(0xFFE65100)
                                        )
                                        Surface(
                                            color = if (isAllocationBalanced) Color(0xFF2E7D32) else Color(0xFFF57C00),
                                            shape = RoundedCornerShape(100.dp)
                                        ) {
                                            Text(
                                                text = if (isAllocationBalanced) "SEIMBANG (PAS)" else "BELUM SEIMBANG",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        color = if (isAllocationBalanced) Color(0xFFC8E6C9) else Color(0xFFFFE0B2)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Total Selisih Kas:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A3B66))
                                        Text(text = formatAuditRupiah(selisih), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2D1E4B))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Total Dialokasikan:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A3B66))
                                        Text(text = formatAuditRupiah(totalAllocated), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2D1E4B))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Sisa Belum Dialokasikan:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A3B66))
                                        Text(
                                            text = formatAuditRupiah(remainingToAllocate),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isAllocationBalanced) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFEDE4F8))

                            Text(
                                text = "Daftar Kantong Pos Dompet:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D1E4B)
                            )
                        }
                    }
                }

                // ==========================================
                // REDESIGNED POS CARDS (MATCHING ANALISIS POS UI)
                // ==========================================
                items(actualSummary.rows, key = { it.idAkun }) { row ->
                    val theme = getPosVisualTheme(row.namaAkun)
                    val currentVal = walletAdjustments[row.idAkun] ?: ""
                    val adjustmentAmount = parseAuditDouble(currentVal) ?: 0.0
                    val projectedNewBalance = row.sisaSaldoRiil + adjustmentAmount
                    val cleanPosName = row.namaAkun.replace("Dompet ", "")

                    // Find corresponding item in allocation comparison for serapan
                    val comparisonItem = allocationSummary.items.find { it.idAkun == row.idAkun }
                    val serapanPct = comparisonItem?.persentaseSerapan ?: (if (row.saldoTerplotting > 0) (row.mutasiPenyesuain / row.saldoTerplotting * 100.0) else 0.0)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .testTag("audit_pos_card_${row.idAkun}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(
                            width = if (currentVal.isNotBlank()) 1.6.dp else 1.dp,
                            color = if (currentVal.isNotBlank()) theme.iconTint else Color(0xFFE4DAF7)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header Row: Specific Pastel Circle Icon + Pos Name + Serapan Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Custom Pastel Icon Container
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(theme.bgPastel, CircleShape)
                                            .border(BorderStroke(1.dp, theme.borderAccent), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = theme.icon,
                                            contentDescription = row.namaAkun,
                                            tint = theme.iconTint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = cleanPosName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2D1E4B)
                                        )
                                        Text(
                                            text = "Pos Dompet Kas PGD",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF7A6E91)
                                        )
                                    }
                                }

                                // Serapan status badge
                                Surface(
                                    color = if (serapanPct > 100.0) Color(0xFFFFEBEE) else Color(0xFFEDE7F6),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        0.8.dp,
                                        if (serapanPct > 100.0) Color(0xFFEF9A9A) else Color(0xFFD1C4E9)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    if (serapanPct > 100.0) Color(0xFFD32F2F) else theme.iconTint,
                                                    CircleShape
                                                )
                                        )
                                        Text(
                                            text = "Serapan ${String.format(Locale.US, "%.1f", serapanPct)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (serapanPct > 100.0) Color(0xFFC62828) else Color(0xFF4A3B66)
                                        )
                                    }
                                }
                            }

                            // Balance Display Box (Saldo Riil vs Estimasi Saldo Baru)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF9F7FD),
                                border = BorderStroke(0.8.dp, Color(0xFFEAE2F7))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Saldo Riil Saat Ini",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF7A6E91)
                                        )
                                        Text(
                                            text = formatAuditRupiah(row.sisaSaldoRiil),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF3B2369)
                                        )
                                    }

                                    if (currentVal.isNotBlank() && adjustmentAmount != 0.0) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Estimasi Saldo Baru",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF7A6E91)
                                            )
                                            Text(
                                                text = formatAuditRupiah(projectedNewBalance),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (adjustmentAmount > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                    }
                                }
                            }

                            // Form Input Penyesuaian & Shortcut Button "+ Alokasikan Sisa"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = currentVal,
                                    onValueChange = { walletAdjustments[row.idAkun] = it },
                                    label = { Text("Nominal Penyesuaian (+ / -)") },
                                    placeholder = { Text("0 (cth: 50000 atau -50000)") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_adjustment_account_${row.idAkun}"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = theme.iconTint,
                                        unfocusedBorderColor = Color(0xFFD3C5EE)
                                    ),
                                    singleLine = true
                                )

                                if (currentVal.isNotBlank()) {
                                    IconButton(
                                        onClick = { walletAdjustments.remove(row.idAkun) },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFFFFEBEE), RoundedCornerShape(10.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus Alokasi",
                                            tint = Color(0xFFC62828),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Quick Shortcut Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Smart "+ Alokasikan Sisa" Button
                                if (!isAllocationBalanced && remainingToAllocate != 0.0) {
                                    Button(
                                        onClick = {
                                            val currentAlloc = parseAuditDouble(currentVal) ?: 0.0
                                            val newVal = currentAlloc + remainingToAllocate
                                            walletAdjustments[row.idAkun] = newVal.toLong().toString()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = theme.bgPastel,
                                            contentColor = theme.iconTint
                                        ),
                                        border = BorderStroke(1.dp, theme.borderAccent),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoFixHigh,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "+ Alokasikan Sisa (${formatAuditRupiah(remainingToAllocate)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Quick set full selisih
                                if (currentVal.isBlank() && selisih != 0.0) {
                                    OutlinedButton(
                                        onClick = {
                                            walletAdjustments[row.idAkun] = selisih.toLong().toString()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Set Penuh Selisih",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF6A4C93)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TOMBOL EKSEKUSI PENYESUAIAN SALDO
                // ==========================================
                item {
                    Button(
                        onClick = {
                            if (!isAllocationBalanced) {
                                Toast.makeText(
                                    context,
                                    "Total alokasi belum seimbang dengan selisih kas! Sisa yang harus dialokasikan: ${formatAuditRupiah(remainingToAllocate)}",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@Button
                            }

                            val now = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
                            val detailItems = mutableListOf<String>()

                            walletAdjustments.forEach { (idAkun, amountStr) ->
                                val delta = parseAuditDouble(amountStr) ?: 0.0
                                if (delta != 0.0) {
                                    val account = actualAccounts.find { it.idAkun == idAkun }
                                    val accountName = account?.namaAkun ?: "Akun #$idAkun"
                                    detailItems.add("$accountName: ${if (delta > 0) "+" else ""}${formatAuditRupiah(delta)}")
                                    viewModel.insertMutation(
                                        tanggal = viewModel.getTodayString(),
                                        idAkun = idAkun,
                                        jenis = if (delta > 0) "Masuk" else "Keluar",
                                        nominal = abs(delta),
                                        keterangan = "Audit Selisih Kas (${if (delta > 0) "Surplus" else "Defisit"}): ${auditNote.ifBlank { "Penyesuaian fisik kas mandiri" }}"
                                    )
                                }
                            }

                            val record = AuditRecord(
                                timestamp = now,
                                saldoSistem = actualSaldoSistem,
                                saldoFisik = actualSaldoFisik,
                                selisih = selisih,
                                isAdjusted = true,
                                keterangan = auditNote.ifBlank { "Penyesuaian saldo kas fisik mandiri" },
                                detailPenyesuaian = detailItems.joinToString(", ")
                            )
                            AuditStorageHelper.saveAuditRecord(context, record)
                            auditHistory = AuditStorageHelper.loadAuditHistory(context)

                            Toast.makeText(context, "Penyesuaian saldo berhasil diterapkan ke dompet!", Toast.LENGTH_LONG).show()

                            // Reset state
                            saldoFisikInput = ""
                            auditNote = ""
                            walletAdjustments.clear()
                            isAdjustmentChosen = null
                            selectedSubTab = 1
                        },
                        enabled = isAllocationBalanced,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_eksekusi_penyesuaian_audit"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAllocationBalanced) Color(0xFF2E7D32) else Color.Gray,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAllocationBalanced) "Terapkan Penyesuaian Saldo ke Dompet" else "Alokasikan Selisih Terlebih Dahulu",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            // ==========================================
            // RIWAYAT AUDIT KAS MANDIRI
            // ==========================================
            if (auditHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2D9F3))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = Color(0xFFB39DDB),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Belum Ada Riwayat Audit Kas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B2369)
                            )
                            Text(
                                text = "Lakukan audit berkala untuk mengevaluasi disiplin pencatatan keuangan dan mendeteksi selisih fisik.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6A5C80),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(auditHistory, key = { it.id }) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(
                            1.dp,
                            if (record.isAdjusted) Color(0xFFC8E6C9) else Color(0xFFE2D9F3)
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Event,
                                        contentDescription = null,
                                        tint = Color(0xFF6A4C93),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = record.timestamp,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2D1E4B)
                                    )
                                }

                                Surface(
                                    color = if (record.isAdjusted) Color(0xFFE8F5E9) else Color(0xFFEDE4FF),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text(
                                        text = if (record.isAdjusted) "DIADJUST KE DOMPET" else "HANYA EVALUASI",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (record.isAdjusted) Color(0xFF2E7D32) else Color(0xFF6A4C93),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF0EBF8))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Saldo Sistem", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6A5C80))
                                    Text(text = formatAuditRupiah(record.saldoSistem), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2D1E4B))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Fisik Dihitung", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6A5C80))
                                    Text(text = formatAuditRupiah(record.saldoFisik), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2D1E4B))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Selisih", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6A5C80))
                                    Text(
                                        text = "${if (record.selisih > 0) "+" else ""}${formatAuditRupiah(record.selisih)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            record.selisih > 0 -> Color(0xFF2E7D32)
                                            record.selisih < 0 -> Color(0xFFC62828)
                                            else -> Color(0xFF6A4C93)
                                        }
                                    )
                                }
                            }

                            if (record.keterangan.isNotBlank()) {
                                Text(
                                    text = "Ket: ${record.keterangan}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF554B6E)
                                )
                            }

                            if (record.detailPenyesuaian.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF3EDFA),
                                    border = BorderStroke(1.dp, Color(0xFFE2D9F3))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "Rincian Penyesuaian Dompet:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF3B2369)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = record.detailPenyesuaian,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF2D1E4B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Denomination Calculator Dialog
    if (showDenominationCalculator) {
        DenominationCalculatorDialog(
            onDismiss = { showDenominationCalculator = false },
            onApply = { calculatedTotal ->
                saldoFisikInput = calculatedTotal.toLong().toString()
                showDenominationCalculator = false
            }
        )
    }
}

// Interactive Denomination Counting Dialog
@Composable
fun DenominationCalculatorDialog(
    onDismiss: () -> Unit,
    onApply: (Double) -> Unit
) {
    val denominations = listOf(
        100_000 to "Uang Kertas Rp 100.000",
        50_000 to "Uang Kertas Rp 50.000",
        20_000 to "Uang Kertas Rp 20.000",
        10_000 to "Uang Kertas Rp 10.000",
        5_000 to "Uang Kertas Rp 5.000",
        2_000 to "Uang Kertas Rp 2.000",
        1_000 to "Uang Kertas / Logam Rp 1.000",
        500 to "Uang Logam Rp 500"
    )

    val counts = remember { mutableStateMapOf<Int, String>() }
    var saldoBankInput by remember { mutableStateOf("") }

    val totalCash = denominations.sumOf { (nominal, _) ->
        val count = counts[nominal]?.toIntOrNull() ?: 0
        nominal.toDouble() * count
    }
    val totalBank = parseAuditDouble(saldoBankInput) ?: 0.0
    val grandTotal = totalCash + totalBank

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF6A4C93))
                Text(
                    text = "Kalkulator Pecahan Fisik Kas",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D1E4B)
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Hitung jumlah lembaran / keping uang tunai di laci/brankas:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B5B95)
                    )
                }

                items(denominations) { (denom, label) ->
                    val currentCount = counts[denom] ?: ""
                    val subtotal = (currentCount.toIntOrNull() ?: 0) * denom.toDouble()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text(text = "Subtotal: ${formatAuditRupiah(subtotal)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }

                        OutlinedTextField(
                            value = currentCount,
                            onValueChange = { counts[denom] = it },
                            placeholder = { Text("0") },
                            modifier = Modifier
                                .width(80.dp)
                                .testTag("denom_input_$denom"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                }

                item {
                    HorizontalDivider(color = Color(0xFFE2D9F3))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Saldo Rekening Bank (Opsional):",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2D1E4B)
                    )
                    OutlinedTextField(
                        value = saldoBankInput,
                        onValueChange = { saldoBankInput = it },
                        placeholder = { Text("Contoh: 5000000") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEDE4FF),
                        border = BorderStroke(1.dp, Color(0xFFD3C5EE))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Total Hitung Gabungan Fisik & Bank:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4A3B66))
                            Text(
                                text = formatAuditRupiah(grandTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF3B2369)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(grandTotal) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A4C93))
            ) {
                Text("Terapkan Hasil Hitung")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Batal")
            }
        }
    )
}
