package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
        val jsonStr = prefs.getString(KEY_AUDIT_HISTORY, null) ?: return emptyList()
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
        } catch (_: Exception) {}
        return list.reversed() // newest first
    }

    fun saveAuditRecord(context: Context, record: AuditRecord) {
        val currentList = loadAuditHistory(context).reversed().toMutableList()
        currentList.add(record)
        // Keep max 50 records
        val trimmed = if (currentList.size > 50) currentList.takeLast(50) else currentList

        val arr = JSONArray()
        for (item in trimmed) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("timestamp", item.timestamp)
            obj.put("saldoSistem", item.saldoSistem)
            obj.put("saldoFisik", item.saldoFisik)
            obj.put("selisih", item.selisih)
            obj.put("isAdjusted", item.isAdjusted)
            obj.put("keterangan", item.keterangan)
            obj.put("detailPenyesuaian", item.detailPenyesuaian)
            arr.put(obj)
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AUDIT_HISTORY, arr.toString()).apply()
    }
}

// Local helper to format currency
private fun formatAuditRupiah(value: Double): String {
    val isNeg = value < 0
    val absVal = abs(value).toLong()
    val formatted = String.format(Locale.GERMANY, "%,d", absVal)
    return if (isNeg) "-Rp $formatted" else "Rp $formatted"
}

// Helper to parse input safely
private fun parseAuditDouble(input: String): Double? {
    if (input.isBlank()) return null
    val clean = input.replace("Rp", "", ignoreCase = true)
        .replace(".", "")
        .replace(",", ".")
        .replace(" ", "")
        .trim()
    return clean.toDoubleOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditSelisihKasScreen(
    viewModel: FinanceViewModel,
    summary: DashboardSummary,
    accounts: List<MasterAkunSaldo>
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // State for System recorded balance (default from summary.grandTotalSisaRiil)
    val defaultSistem = summary.grandTotalSisaRiil
    var saldoSistemInput by remember(defaultSistem) { mutableStateOf(defaultSistem.toLong().toString()) }
    var useSystemLiveBalance by remember { mutableStateOf(true) }

    val actualSaldoSistem = if (useSystemLiveBalance) defaultSistem else (parseAuditDouble(saldoSistemInput) ?: defaultSistem)

    // State for Physical Cash Input
    var saldoFisikInput by remember { mutableStateOf("") }
    var auditNote by remember { mutableStateOf("") }

    // Physical Denomination Calculator Dialog State
    var showDenominationCalculator by remember { mutableStateOf(false) }

    val actualSaldoFisik = parseAuditDouble(saldoFisikInput) ?: 0.0
    val selisih = actualSaldoFisik - actualSaldoSistem

    // Adjustment toggle state (Ya / Tidak)
    var isAdjustmentChosen by remember { mutableStateOf<Boolean?>(null) } // null = not chosen yet, false = hanya catat, true = penyesuaian dompet

    // Adjustment allocation map per accountId -> Double nominal (+ for surplus added, - for deficit deducted)
    val walletAdjustments = remember { mutableStateMapOf<Int, String>() }

    // Audit History state
    var auditHistory by remember { mutableStateOf(AuditStorageHelper.loadAuditHistory(context)) }

    // Calculate total adjustments allocated
    val totalAllocated = walletAdjustments.entries.sumOf { entry ->
        parseAuditDouble(entry.value) ?: 0.0
    }
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
                border = BorderStroke(1.dp, Color(0xFFE2D9F3)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
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
                                modifier = Modifier.size(24.dp)
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
                    border = BorderStroke(1.dp, Color(0xFFE2D9F3)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "1. PENGHITUNGAN & PERBANDINGAN SALDO",
                            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B2369)
                        )

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
                                        text = if (useSystemLiveBalance) "Gunakan Otomatis" else "Ubah Manual",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF6A4C93)
                                    )
                                }
                            }

                            if (useSystemLiveBalance) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF7F3FB),
                                    border = BorderStroke(1.dp, Color(0xFFE6DCF5))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Total Kas Fisik Riil Sistem",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF554B6E)
                                        )
                                        Text(
                                            text = formatAuditRupiah(actualSaldoSistem),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF3B2369)
                                        )
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = saldoSistemInput,
                                    onValueChange = { saldoSistemInput = it },
                                    label = { Text("Nominal Saldo Sistem (Rp)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_saldo_sistem_manual"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Saldo Fisik Riil di Tangan / Bank
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Jumlah Hitung Fisik Uang (Di Tangan / Bank):",
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
                                        color = Color(0xFF6A4C93),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = saldoFisikInput,
                                onValueChange = { saldoFisikInput = it },
                                label = { Text("Masukkan Total Uang Fisik Hasil Hitung (Rp)") },
                                placeholder = { Text("Contoh: 15000000") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_saldo_fisik_riil"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                trailingIcon = {
                                    if (saldoFisikInput.isNotBlank()) {
                                        IconButton(onClick = { saldoFisikInput = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                        }
                                    }
                                }
                            )

                            // Quick Fill Helper Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { saldoFisikInput = actualSaldoSistem.toLong().toString() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text("Set Sesuai Sistem", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { saldoFisikInput = "" },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text("Reset Hitung", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Keterangan / Catatan Audit
                        OutlinedTextField(
                            value = auditNote,
                            onValueChange = { auditNote = it },
                            label = { Text("Catatan / Alasan Audit (Opsional)") },
                            placeholder = { Text("Contoh: Audit mingguan tutup buku, ada selisih uang kembalian...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_catatan_audit"),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 2
                        )

                        HorizontalDivider(color = Color(0xFFEDE4F8))

                        // ==========================================
                        // KARTU RINGKASAN REKONSILIASI
                        // ==========================================
                        val isSurplus = selisih > 0
                        val isDefisit = selisih < 0
                        val isBalance = selisih == 0.0

                        val badgeBg = when {
                            saldoFisikInput.isBlank() -> Color(0xFFF7F3FB)
                            isBalance -> Color(0xFFE8F5E9)
                            isSurplus -> Color(0xFFE8F5E9)
                            else -> Color(0xFFFFEBEE)
                        }

                        val badgeBorder = when {
                            saldoFisikInput.isBlank() -> Color(0xFFD7C9EB)
                            isBalance -> Color(0xFFA5D6A7)
                            isSurplus -> Color(0xFF81C784)
                            else -> Color(0xFFEF9A9A)
                        }

                        val badgeTextColor = when {
                            saldoFisikInput.isBlank() -> Color(0xFF554B6E)
                            isBalance -> Color(0xFF2E7D32)
                            isSurplus -> Color(0xFF1B5E20)
                            else -> Color(0xFFC62828)
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = badgeBg,
                            border = BorderStroke(1.2.dp, badgeBorder),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "HASIL REKONSILIASI KAS",
                                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = badgeTextColor
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (saldoFisikInput.isBlank()) Color(0xFFE2D9F3) else if (isSurplus) Color(0xFFC8E6C9) else if (isDefisit) Color(0xFFFFCDD2) else Color(0xFFC8E6C9)
                                    ) {
                                        Text(
                                            text = when {
                                                saldoFisikInput.isBlank() -> "Menunggu Input"
                                                isBalance -> "KLOP / PAS"
                                                isSurplus -> "SURPLUS (+)"
                                                else -> "DEFISIT (-)"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeTextColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Saldo Awal / Tercatat:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A3B66))
                                    Text(text = formatAuditRupiah(actualSaldoSistem), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF2D1E4B))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Saldo Fisik Riil:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A3B66))
                                    Text(text = formatAuditRupiah(actualSaldoFisik), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF2D1E4B))
                                }

                                HorizontalDivider(color = badgeBorder.copy(alpha = 0.6f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Selisih Tercatat:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeTextColor
                                        )
                                        if (saldoFisikInput.isNotBlank()) {
                                            Text(
                                                text = when {
                                                    isBalance -> "Tidak ada selisih, kas sangat akurat!"
                                                    isSurplus -> "Fisik lebih banyak dari sistem (Ada pemasukan tak tercatat)"
                                                    else -> "Fisik kurang dari sistem (Ada pengeluaran tercecer / lupa catat)"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = badgeTextColor.copy(alpha = 0.85f)
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (saldoFisikInput.isBlank()) "Rp 0" else formatAuditRupiah(selisih),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // BAGIAN 2: OPSI TINDAKAN PENYESUAIAN
            // ==========================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2D9F3)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "2. TINDAKAN REKONSILIASI",
                            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B2369)
                        )

                        Text(
                            text = "Apakah Anda ingin melakukan penyesuaian saldo ke pos dompet?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2D1E4B)
                        )

                        Text(
                            text = "Pilih 'Tidak' jika Anda hanya ingin menyimpan catatan audit sebagai evaluasi tanpa mengubah saldo dompet apapun. Pilih 'Ya' jika ingin menyeimbangkan saldo pos dompet.",
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
                                        text = "Hanya Catat Evaluasi Disiplin (Saldo Dompet Tetap)",
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
                                        text = "Alokasikan Selisih ke Kantong Dompet Tertentu",
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
                                    selectedSubTab = 1
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("button_save_audit_evaluation_only"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simpan Riwayat Audit (Evaluasi Saja)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // BAGIAN 3: FORM ALOKASI PENYESUAIAN (JIKA YA)
            // ==========================================
            if (isAdjustmentChosen == true) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2D9F3)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "3. ALOKASI PENYESUAIAN KANTONG DOMPET",
                                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B2369)
                            )

                            Text(
                                text = "Masukkan nominal penyesuaian (+ untuk menambah atau - untuk mengurangi) pada masing-masing pos dompet hingga totalnya seimbang dengan selisih (${formatAuditRupiah(selisih)}).",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF554B6E)
                            )

                            // Status Klop Tracker
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isAllocationBalanced) Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                                border = BorderStroke(1.dp, if (isAllocationBalanced) Color(0xFF81C784) else Color(0xFFFFE082))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Status Alokasi:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAllocationBalanced) Color(0xFF1B5E20) else Color(0xFF8D6E63)
                                        )
                                        Text(
                                            text = if (isAllocationBalanced) "SEIMBANG / KLOP" else "BELUM SEIMBANG",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAllocationBalanced) Color(0xFF1B5E20) else Color(0xFFE65100)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Target Selisih:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A3B66))
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
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Sisa yang Belum Dialokasikan:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A3B66))
                                        Text(
                                            text = formatAuditRupiah(remainingToAllocate),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isAllocationBalanced) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFEDE4F8))

                            // List of Envelopes / Accounts
                            Text(
                                text = "Daftar Kantong Dompet & Saldo Saat Ini:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D1E4B)
                            )

                            summary.rows.forEach { row ->
                                val currentVal = walletAdjustments[row.idAkun] ?: ""

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF9F6FC),
                                    border = BorderStroke(1.dp, Color(0xFFE5DDF3))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = row.namaAkun,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF3B2369)
                                                )
                                                Text(
                                                    text = "Saldo Riil: ${formatAuditRupiah(row.sisaSaldoRiil)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF6A5C80)
                                                )
                                            }

                                            // Quick fill remaining button
                                            if (!isAllocationBalanced && remainingToAllocate != 0.0) {
                                                TextButton(
                                                    onClick = {
                                                        val currentAlloc = parseAuditDouble(currentVal) ?: 0.0
                                                        val newVal = currentAlloc + remainingToAllocate
                                                        walletAdjustments[row.idAkun] = newVal.toLong().toString()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "+ Alokasikan Sisa",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF6A4C93),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = currentVal,
                                                onValueChange = { walletAdjustments[row.idAkun] = it },
                                                label = { Text("Nominal Penyesuaian (+ / -)") },
                                                placeholder = { Text("0") },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("input_adjustment_account_${row.idAkun}"),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true
                                            )

                                            if (currentVal.isNotBlank()) {
                                                IconButton(
                                                    onClick = { walletAdjustments.remove(row.idAkun) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Hapus Alokasi",
                                                        tint = Color(0xFFC62828)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Tombol Eksekusi Penyesuaian
                            Button(
                                onClick = {
                                    if (saldoFisikInput.isBlank()) {
                                        Toast.makeText(context, "Silakan masukkan hitungan saldo fisik terlebih dahulu.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!isAllocationBalanced) {
                                        Toast.makeText(context, "Alokasi belum seimbang dengan selisih! Sisa belum dialokasikan: ${formatAuditRupiah(remainingToAllocate)}", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }

                                    // Execute mutations for each allocated wallet
                                    val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                    val detailLogBuilder = StringBuilder()

                                    walletAdjustments.forEach { (accountId, amountStr) ->
                                        val amount = parseAuditDouble(amountStr) ?: 0.0
                                        if (amount != 0.0) {
                                            val accountName = summary.rows.find { it.idAkun == accountId }?.namaAkun ?: "Akun #$accountId"
                                            val isMasuk = amount > 0
                                            val absAmount = abs(amount)
                                            val jenis = if (isMasuk) "Uang Masuk" else "Uang Keluar"
                                            val note = "[Audit Kas] Penyesuaian Rekonsiliasi (${if (isMasuk) "Surplus" else "Defisit"} ${formatAuditRupiah(absAmount)})"

                                            viewModel.insertMutation(
                                                tanggal = now,
                                                idAkun = accountId,
                                                jenis = jenis,
                                                nominal = absAmount,
                                                keterangan = note,
                                                waktu = timeNow
                                            )

                                            detailLogBuilder.append("• $accountName: ${if (isMasuk) "+" else "-"}${formatAuditRupiah(absAmount)}\n")
                                        }
                                    }

                                    // Save Audit Record
                                    val record = AuditRecord(
                                        timestamp = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date()),
                                        saldoSistem = actualSaldoSistem,
                                        saldoFisik = actualSaldoFisik,
                                        selisih = selisih,
                                        isAdjusted = true,
                                        keterangan = auditNote.ifBlank { "Penyesuaian saldo kas langsung ke kantong dompet" },
                                        detailPenyesuaian = detailLogBuilder.toString().trim()
                                    )
                                    AuditStorageHelper.saveAuditRecord(context, record)
                                    auditHistory = AuditStorageHelper.loadAuditHistory(context)

                                    Toast.makeText(context, "Penyesuaian berhasil dieksekusi ke pos dompet & dicatat ke riwayat mutasi!", Toast.LENGTH_LONG).show()
                                    walletAdjustments.clear()
                                    isAdjustmentChosen = null
                                    selectedSubTab = 1
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("button_execute_adjustment"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAllocationBalanced) Color(0xFF2E7D32) else Color(0xFF6A4C93)
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simpan & Eksekusi Penyesuaian ke Dompet", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // RIWAYAT AUDIT KAS & EVALUASI
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
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Color(0xFF9E8FB2),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Belum Ada Riwayat Audit Kas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B2369)
                            )
                            Text(
                                text = "Hasil pengecekan selisih fisik kas akan tercatat di sini sebagai bahan evaluasi kedisiplinan pencatatan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7A6E91),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(auditHistory, key = { it.id }) { record ->
                    val isSurplus = record.selisih > 0
                    val isDefisit = record.selisih < 0
                    val isBalance = record.selisih == 0.0

                    val statusColor = when {
                        isBalance -> Color(0xFF2E7D32)
                        isSurplus -> Color(0xFF1B5E20)
                        else -> Color(0xFFC62828)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2D9F3)),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = Color(0xFF6A4C93),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = record.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF554B6E)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (record.isAdjusted) Color(0xFFEDE4FF) else Color(0xFFF0F0F0)
                                ) {
                                    Text(
                                        text = if (record.isAdjusted) "Disesuaikan ke Dompet" else "Evaluasi Saja (Tanpa Penyesuaian)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (record.isAdjusted) Color(0xFF4A148C) else Color(0xFF616161),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Sistem: ${formatAuditRupiah(record.saldoSistem)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF554B6E))
                                    Text(text = "Fisik: ${formatAuditRupiah(record.saldoFisik)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF554B6E))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Selisih:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(
                                        text = formatAuditRupiah(record.selisih),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }

                            if (record.keterangan.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF9F7FC)
                                ) {
                                    Text(
                                        text = "Catatan: ${record.keterangan}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF4A3B66),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
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
            Text(
                text = "Kalkulator Pecahan Fisik Kas",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D1E4B)
            )
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
                        text = "Hitung jumlah lembaran / keping uang tunai:",
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
                        color = Color(0xFFEDE4FF)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Total Hitung Gabungan:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4A3B66))
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
                shape = RoundedCornerShape(10.dp)
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
