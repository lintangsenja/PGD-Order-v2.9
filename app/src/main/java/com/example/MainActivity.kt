package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.viewmodel.UserProfile
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.LightGradientStart
import com.example.ui.theme.LightGradientEnd
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Context
import java.util.Calendar
import java.text.SimpleDateFormat
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.SisaLabaBg
import com.example.ui.theme.SisaLabaText
import com.example.ui.theme.SisaLabaBorder
import com.example.ui.theme.SeaBlue
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintAurora
import com.example.ui.theme.HijauGelap
import com.example.ui.theme.PinkAurora
import com.example.ui.theme.MagentaLembut
import com.example.ui.theme.SkyBluePastel
import com.example.ui.theme.SkyBlueBorder
import com.example.ui.theme.LilacPastel
import com.example.ui.theme.LilacBorder
import com.example.ui.theme.MintPastel
import com.example.ui.theme.MintBorder
import com.example.ui.theme.LemonPastel
import com.example.ui.theme.LemonBorder
import com.example.data.model.MasterAkunSaldo
import com.example.data.model.MutasiManualKeluarMasuk
import com.example.data.model.TransaksiOrderMasuk
import com.example.data.model.MasterPelanggan
import com.example.data.model.MasterSatuanHarga
import com.example.data.model.CustomerFrequency
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.ui.viewmodel.BackupFile
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.FinanceViewModelFactory
import com.example.ui.viewmodel.DashboardSummary
import com.example.ui.viewmodel.AccountDashboardRow
import com.example.ui.viewmodel.PosAllocationSummary
import com.example.ui.viewmodel.AllocationComparisonItem
import java.text.NumberFormat
import java.util.Locale
import java.util.Date
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import android.media.MediaScannerConnection
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.FileInputStream
import java.io.OutputStream

fun getAuroraBorder(): BorderStroke {
    return BorderStroke(
        width = 1.dp,
        color = Color(0xFFE4DAF7)
    )
}

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels {
        FinanceViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}

// Helper to format numbers with Indonesian locale thousand separators and no decimals (e.g., 10.000)
fun formatAngka(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "0"
    return try {
        val locale = Locale.forLanguageTag("id-ID")
        val formatter = NumberFormat.getNumberInstance(locale)
        val absVal = kotlin.math.abs(value)
        if (absVal % 1.0 == 0.0) {
            formatter.maximumFractionDigits = 0
            formatter.minimumFractionDigits = 0
            formatter.format(value.toLong())
        } else {
            formatter.maximumFractionDigits = 2
            formatter.minimumFractionDigits = 0
            formatter.format(value)
        }
    } catch (e: Exception) {
        val absVal = kotlin.math.abs(value)
        if (absVal % 1.0 == 0.0) {
            val rounded = try { value.toLong() } catch (_: Exception) { 0L }
            String.format(Locale.US, "%,d", rounded).replace(',', '.')
        } else {
            String.format(Locale.US, "%,.2f", value).replace(',', 'X').replace('.', ',').replace('X', '.')
        }
    }
}

fun formatAngka(value: Long): String = formatAngka(value.toDouble())
fun formatAngka(value: Int): String = formatAngka(value.toDouble())

// Helper to clean currency/formatted input string and parse to Double safely (handles Indonesian Rupiah format with dots or commas)
fun parseDoubleInput(input: String?): Double? {
    if (input.isNullOrBlank()) return null
    val raw = input.replace("Rp", "", ignoreCase = true).trim()
    if (raw.isEmpty()) return null

    val noSpaces = raw.replace(" ", "")

    val clean = if (noSpaces.contains('.') && noSpaces.contains(',')) {
        val lastDot = noSpaces.lastIndexOf('.')
        val lastComma = noSpaces.lastIndexOf(',')
        if (lastComma > lastDot) {
            noSpaces.replace(".", "").replace(',', '.')
        } else {
            noSpaces.replace(",", "")
        }
    } else if (noSpaces.contains(',')) {
        noSpaces.replace(',', '.')
    } else if (noSpaces.contains('.')) {
        val dotCount = noSpaces.count { it == '.' }
        if (dotCount > 1) {
            noSpaces.replace(".", "")
        } else {
            val parts = noSpaces.split('.')
            val afterDot = parts.getOrNull(1) ?: ""
            if (afterDot.length == 3 && parts[0].isNotEmpty() && parts[0] != "0") {
                noSpaces.replace(".", "")
            } else {
                noSpaces
            }
        }
    } else {
        noSpaces
    }
    return clean.toDoubleOrNull()
}

// Helper to parse percentage / decimal numbers like 5.5% or 5,5%
fun parseDecimalDouble(input: String?): Double? {
    if (input.isNullOrBlank()) return null
    val clean = input.replace("%", "").replace(',', '.').trim()
    if (clean.isEmpty()) return null
    return clean.toDoubleOrNull()
}

// Helper to clean formatted input string and parse to Int safely
fun parseIntInput(input: String?): Int? {
    if (input.isNullOrBlank()) return null
    val clean = input.replace("Rp", "", ignoreCase = true)
        .replace(".", "")
        .replace(",", "")
        .trim()
    if (clean.isEmpty()) return null
    return clean.toIntOrNull()
}

// Helper to format currency to Rupiah without decimals (e.g., Rp 10.000)
fun formatRupiah(value: Double): String {
    return "Rp " + formatAngka(value)
}

fun formatRupiah(value: Long): String = formatRupiah(value.toDouble())
fun formatRupiah(value: Int): String = formatRupiah(value.toDouble())

data class NavigationDrawerItemData(val label: String, val id: String, val icon: ImageVector)

@Composable
fun MainAppScreen(
    viewModel: FinanceViewModel,
    onOpenLogin: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dashboard, 1: Transaksi, 2: Riwayat Kas
    var activeDrawerScreen by remember { mutableStateOf<String?>(null) } // null when bottom bar is active, otherwise drawer item ID

    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val orders by viewModel.allOrders.collectAsStateWithLifecycle()
    val mutations by viewModel.allMutations.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isCloudOnline by viewModel.isCloudOnline.collectAsStateWithLifecycle()
    val syncStatusText by viewModel.syncStatusText.collectAsStateWithLifecycle()
    val cloudLastSyncTime by viewModel.cloudLastSyncTime.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var showExitDialog by remember { mutableStateOf(false) }
    val sadQuotes = remember {
        listOf(
            "Pergi bukan berarti melupakan, tapi terkadang tinggal hanya akan menambah beban kenangan... Apakah kamu yakin ingin meninggalkan aplikasi ini? 😢",
            "Setiap pertemuan pasti ada perpisahan, tapi apakah kita harus berpisah secepat ini? Catatan keuanganmu akan merindukanmu... 💔",
            "Kamu mau pergi? Padahal baru saja kita mulai merapikan masa depan keuanganmu bersama-sama... 🥺",
            "Jangan pergi dulu... Masih banyak mimpi dan saldo yang perlu kita jaga bersama di PGD Order. 🌧️",
            "Langkahmu untuk keluar terasa begitu berat bagi kami. Yakin tidak ingin bertahan sebentar lagi? 😭"
        )
    }
    var currentQuote by remember { mutableStateOf(sadQuotes.first()) }

    LaunchedEffect(showExitDialog) {
        if (showExitDialog) {
            currentQuote = sadQuotes.random()
        }
    }

    BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (activeDrawerScreen != null) {
            activeDrawerScreen = null
        } else if (selectedTab != 0) {
            selectedTab = 0
        } else {
            showExitDialog = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                drawerContainerColor = colorScheme.surface,
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.surface)
                        .padding(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeDrawerScreen = "profil"
                                scope.launch { drawerState.close() }
                            }
                            .padding(vertical = 16.dp, horizontal = 8.dp)
                    ) {
                        ProfileAvatar(
                            userProfile = userProfile,
                            size = 52.dp,
                            modifier = Modifier.testTag("drawer_avatar_icon")
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = userProfile.adminName.ifBlank { "PGD Order" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = userProfile.tagline.ifBlank { "Pradipta Graha Digital" },
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Badge Status Firestore Realtime
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSyncing) Color(0xFFEDE4FF) else if (isCloudOnline) Color(0xFFF3EEFA) else Color(0xFFFFEBEE),
                                border = BorderStroke(1.dp, if (isSyncing) Color(0xFF6A4C93) else if (isCloudOnline) Color(0xFFE4DAF7) else Color(0xFFFFCDD2)),
                                modifier = Modifier.testTag("drawer_header_firestore_status_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = if (isSyncing) Color(0xFF6A4C93) else if (isCloudOnline) Color(0xFF6A4C93) else Color(0xFFD32F2F),
                                                shape = CircleShape
                                            )
                                    )
                                    Text(
                                        text = if (isSyncing) "Menyinkronkan..." else if (isCloudOnline) "Firestore Online" else "Firestore Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSyncing) Color(0xFF3B2369) else if (isCloudOnline) Color(0xFF3B2369) else Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = colorScheme.outlineVariant.copy(alpha = 0.7f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Menu items
                    val menuItems = listOf(
                        NavigationDrawerItemData("Order Nota", "order_nota", Icons.Default.ReceiptLong),
                        NavigationDrawerItemData("Master Data", "master_data", Icons.Default.Storage),
                        NavigationDrawerItemData("Backup Data", "backup", Icons.Default.Backup),
                        NavigationDrawerItemData("Profil", "profil", Icons.Default.AccountCircle),
                        NavigationDrawerItemData("Pengaturan Finansial", "pengaturan", Icons.Default.Settings)
                    )

                    menuItems.forEachIndexed { index, item ->
                        val isSelected = activeDrawerScreen == item.id
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = item.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                activeDrawerScreen = item.id
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = Color.Transparent,
                                selectedTextColor = colorScheme.primary,
                                unselectedTextColor = colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.background(
                                            color = Color(0xFFEDE4FF),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        )

                        if (index < menuItems.lastIndex) {
                            HorizontalDivider(
                                color = colorScheme.outlineVariant.copy(alpha = 0.2f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Indikator Waktu Sinkron Terakhir (Firebase Realtime Status)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("drawer_last_sync_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF3EEFA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSyncing) Icons.Default.Sync else if (isCloudOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = "Indikator Sinkronisasi",
                                    tint = if (isCloudOnline || isSyncing) Color(0xFF6A4C93) else Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isSyncing) "Menyinkronkan..." else if (isCloudOnline) "Tersambung Firestore" else "Mode Offline",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCloudOnline || isSyncing) Color(0xFF3B2369) else Color(0xFFD32F2F)
                                )
                                Text(
                                    text = "Terakhir Sinkron: $cloudLastSyncTime",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF554B6E)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = colorScheme.outlineVariant.copy(alpha = 0.3f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Tombol Keluar (Logout)
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = "Keluar",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.error
                            )
                        },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showExitDialog = true
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Keluar",
                                tint = colorScheme.error
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = colorScheme.error
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("drawer_btn_keluar")
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3EEFA))
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    Column(
                        modifier = Modifier
                            .background(Color.Transparent)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu Drawer",
                                    tint = colorScheme.onBackground
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        activeDrawerScreen == "order_nota" -> "Order Nota"
                                        activeDrawerScreen == "master_data" -> "Master Data"
                                        activeDrawerScreen == "backup" -> "Backup Data"
                                        activeDrawerScreen == "profil" -> "Profil Administrator"
                                        activeDrawerScreen == "pengaturan" -> "Pengaturan Finansial"
                                        selectedTab == 0 -> userProfile.adminName.ifBlank { "PGD Order" }
                                        selectedTab == 1 -> "Manajemen Dompet"
                                        selectedTab == 2 -> "Transaksi Operasional"
                                        selectedTab == 3 -> "Laporan Pembukuan"
                                        selectedTab == 4 -> "Riwayat Kas"
                                        else -> userProfile.adminName.ifBlank { "PGD Order" }
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = userProfile.tagline.ifBlank { "Pradipta Graha Digital" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Cloud Firestore Sync Badge Indicator
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSyncing) Color(0xFFEDE4FF) else if (isCloudOnline) Color(0xFFF3EEFA) else Color(0xFFFFEBEE),
                                border = BorderStroke(1.dp, if (isSyncing) Color(0xFF6A4C93) else if (isCloudOnline) Color(0xFFE4DAF7) else Color(0xFFFFCDD2)),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("topbar_sync_status_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(10.dp),
                                            strokeWidth = 1.8.dp,
                                            color = Color(0xFF6A4C93)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isCloudOnline) Color(0xFF6A4C93) else Color(0xFFD32F2F))
                                        )
                                    }
                                    Text(
                                        text = if (isSyncing) "Sync..." else if (isCloudOnline) "Online" else "Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSyncing) Color(0xFF3B2369) else if (isCloudOnline) Color(0xFF3B2369) else Color(0xFFD32F2F)
                                    )
                                }
                            }

                            // Profile Avatar Badge
                            Box(
                                modifier = Modifier
                                    .clickable { activeDrawerScreen = "profil" }
                                    .testTag("topbar_profile_avatar"),
                                contentAlignment = Alignment.Center
                            ) {
                                ProfileAvatar(
                                    userProfile = userProfile,
                                    size = 38.dp
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        color = colorScheme.background,
                        border = BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val navTabs = listOf(
                                Triple("Dashboard", Icons.Default.Dashboard, 0),
                                Triple("Dompet", Icons.Default.AccountBalanceWallet, 1),
                                Triple("Transaksi", Icons.Default.AddShoppingCart, 2),
                                Triple("Laporan", Icons.Default.Assessment, 3),
                                Triple("Riwayat Kas", Icons.Default.History, 4)
                            )

                            navTabs.forEach { (label, icon, index) ->
                                val isSelected = activeDrawerScreen == null && selectedTab == index
                                val interactionSource = remember { MutableInteractionSource() }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) colorScheme.primaryContainer else Color.Transparent)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) {
                                            activeDrawerScreen = null
                                            selectedTab = index
                                        }
                                        .padding(vertical = 3.dp)
                                        .testTag(when(index) {
                                            0 -> "nav_tab_dashboard"
                                            1 -> "nav_tab_dompet"
                                            2 -> "nav_tab_orders"
                                            3 -> "nav_tab_laporan"
                                            else -> "nav_tab_riwayat"
                                        }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color.Transparent)
                ) {
                    if (activeDrawerScreen != null) {
                        when (activeDrawerScreen) {
                            "order_nota" -> OrderNotaScreen(viewModel = viewModel, orders = orders, accounts = accounts)
                            "master_data" -> MasterDataTab(viewModel)
                            "backup" -> BackupRestoreTab(viewModel)
                            "profil" -> ProfileScreen(viewModel = viewModel, onOpenLogin = onOpenLogin)
                            "pengaturan" -> FinancialSettingsTab(viewModel, summary)
                        }
                    } else {
                        when (selectedTab) {
                            0 -> DashboardTab(viewModel, summary, orders, mutations, accounts, userProfile, onNavigateToOrderNota = { activeDrawerScreen = "order_nota" })
                            1 -> DompetScreen(summary.rows, viewModel)
                            2 -> TransaksiTab(viewModel, orders, mutations, accounts)
                            3 -> LaporanTab(viewModel)
                            4 -> RiwayatKasTab(viewModel, orders)
                        }
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.SentimentVeryDissatisfied,
                    contentDescription = "Keluar",
                    tint = colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Yakin Mau Pergi?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = currentQuote,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error
                    ),
                    modifier = Modifier.testTag("btn_confirm_exit")
                ) {
                    Text("Ya, Keluar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitDialog = false },
                    modifier = Modifier.testTag("btn_cancel_exit")
                ) {
                    Text("Batal, Tetap Disini", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun TransaksiTab(
    viewModel: FinanceViewModel,
    orders: List<TransaksiOrderMasuk>,
    mutations: List<MutasiManualKeluarMasuk>,
    accounts: List<MasterAkunSaldo>
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Order Baru, 1: Pending Bayar, 2: Mutasi Kas
    val pendingCount = remember(orders) { orders.count { it.status == "Belum Lunas" } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .background(colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                Triple("Order Baru", Icons.Default.AddShoppingCart, 0),
                Triple(if (pendingCount > 0) "Pending ($pendingCount)" else "Pending Bayar", Icons.Default.PendingActions, 1),
                Triple("Mutasi Kas", Icons.Default.CompareArrows, 2)
            )
            tabs.forEach { (title, icon, index) ->
                val isSelected = selectedSubTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colorScheme.primary else Color.Transparent)
                        .clickable { selectedSubTab = index }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedSubTab) {
                0 -> OrdersTab(orders, viewModel)
                1 -> PendingBayarTab(orders, accounts, viewModel)
                2 -> MutationsTab(mutations, accounts, viewModel)
            }
        }
    }
}

@Composable
fun PendingBayarTab(
    orders: List<TransaksiOrderMasuk>,
    accounts: List<MasterAkunSaldo>,
    viewModel: FinanceViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val pendingOrders = remember(orders) { orders.filter { it.status == "Belum Lunas" } }
    val totalPendingAmount = remember(pendingOrders) { pendingOrders.sumOf { it.qtyOrder.toDouble() * it.hargaSatuan } }

    val kertasHpp = remember(accounts) { accounts.find { it.namaAkun == "Kertas" || it.namaAkun == "Dompet Kertas" }?.konstanHppUnit?.toDouble() ?: 106.0 }
    val tintaHpp = remember(accounts) { accounts.find { it.namaAkun == "Tinta" || it.namaAkun == "Dompet Tinta" }?.konstanHppUnit?.toDouble() ?: 25.0 }
    val pengemasanHpp = remember(accounts) { accounts.find { it.namaAkun == "Pengemasan" || it.namaAkun == "Dompet Pengemasan" }?.konstanHppUnit?.toDouble() ?: 300.0 }
    val wastePct = remember(accounts) { accounts.find { it.namaAkun == "Waste" || it.namaAkun == "Dompet Waste / Rusak" }?.persentaseOperasional?.toDouble() ?: 0.05 }
    val tenagaKerjaPct = remember(accounts) { accounts.find { it.namaAkun == "Tenaga Kerja" || it.namaAkun == "Dompet Tenaga Kerja" }?.persentaseOperasional?.toDouble() ?: 0.07 }
    val listrikPct = remember(accounts) { accounts.find { it.namaAkun == "Listrik" || it.namaAkun == "Dompet Listrik" }?.persentaseOperasional?.toDouble() ?: 0.02 }
    val maintenancePct = remember(accounts) { accounts.find { it.namaAkun == "Maintenance Alat" || it.namaAkun == "Dompet Maintenance" }?.persentaseOperasional?.toDouble() ?: 0.05 }

    val totalLabaBersihTertahan = remember(pendingOrders, accounts) {
        if (pendingOrders.isEmpty()) 0.0
        else {
            pendingOrders.sumOf { order ->
                val qty = order.qtyOrder.toDouble()
                val totalPendapatan = qty * order.hargaSatuan
                val dynamicKertas = qty * kertasHpp
                val dynamicTinta = qty * tintaHpp
                val dynamicPengemasan = order.jumlahPlastikPengemasan.toDouble() * pengemasanHpp
                val dynamicWaste = wastePct * totalPendapatan
                val dynamicTenagaKerja = tenagaKerjaPct * totalPendapatan
                val dynamicListrik = listrikPct * totalPendapatan
                val dynamicMaintenance = maintenancePct * totalPendapatan
                val dynamicTotalModal = dynamicKertas + dynamicTinta + dynamicPengemasan + dynamicWaste + dynamicTenagaKerja + dynamicListrik + dynamicMaintenance
                totalPendapatan - dynamicTotalModal
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(pendingOrders, searchQuery) {
        if (searchQuery.isBlank()) pendingOrders
        else pendingOrders.filter { it.namaPesanan.contains(searchQuery, ignoreCase = true) || it.tanggalOrder.contains(searchQuery) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PinkAurora.copy(alpha = 0.4f)),
                border = BorderStroke(1.2.dp, PinkAurora)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(PinkAurora, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PendingActions,
                                    contentDescription = null,
                                    tint = MagentaLembut,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "PESANAN PENDING BAYAR",
                                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MagentaLembut
                                )
                                Text(
                                    text = "${pendingOrders.size} pesanan menunggu pelunasan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = formatRupiah(totalPendingAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MagentaLembut
                        )
                    }

                    Text(
                        text = "💡 Klik tombol 'Bayar & Plotting Sekarang' untuk melunasi nota. Dana akan otomatis dialirkan ke masing-masing dompet alokasi kas (Kertas, Tinta, Pengemasan, Operasional, Laba Bersih).",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Dua Kotak Ringkasan Berdampingan (Row / 2 Kolom)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Kotak 1: Total Pending Bayar - Soft Lavender/Lilac (#F3E8FF)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
                    border = BorderStroke(1.2.dp, Color(0xFFD8B4FE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFE9D5FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(0xFF6B46C1),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Total Pending",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF581C87)
                            )
                        }

                        Text(
                            text = formatRupiah(totalPendingAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4C1D95)
                        )
                        Text(
                            text = "${pendingOrders.size} Nota Tertunda",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B46C1),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Kotak 2: Laba Bersih Tertahan - Soft Mint/Green (#E6F4EA)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F4EA)),
                    border = BorderStroke(1.2.dp, Color(0xFF86EFAC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFBBF7D0), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF166534),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Laba Tertahan",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14532D)
                            )
                        }

                        Text(
                            text = formatRupiah(totalLabaBersihTertahan),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF15803D)
                        )
                        Text(
                            text = "Estimasi Hak Laba",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (pendingOrders.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari pelanggan atau nama nota...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface
                    )
                )
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MintAurora.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MintGreen.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MintAurora, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = HijauGelap,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Tidak ada pesanan cocok" else "Semua Pesanan Sudah Lunas!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HijauGelap
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "Coba kata kunci lain" else "Tidak ada transaksi berstatus pending saat ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.idOrder }) { order ->
                var showDeleteConfirm by remember { mutableStateOf(false) }
                var showPayConfirm by remember { mutableStateOf(false) }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("Konfirmasi Hapus Pesanan") },
                        text = { Text("Hapus riwayat pesanan '${order.namaPesanan}'?") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deleteOrder(order)
                                showDeleteConfirm = false
                            }) {
                                Text("Ya, Hapus", color = colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
                        }
                    )
                }

                if (showPayConfirm) {
                    AlertDialog(
                        onDismissRequest = { showPayConfirm = false },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MintAurora, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = HijauGelap,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        title = {
                            Text(
                                "Pelunasan & Auto-Plotting",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Apakah Anda yakin ingin menandai pesanan ini sebagai LUNAS?")
                                Surface(
                                    color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(order.namaPesanan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Nominal: ${formatRupiah(order.totalPendapatan)}", color = colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                Text(
                                    "✨ Dana akan otomatis dialirkan ke pos kas (Kertas, Tinta, Pengemasan, Operasional, dan Laba Bersih).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.quickPayOrder(order)
                                    showPayConfirm = false
                                    Toast.makeText(context, "Pesanan '${order.namaPesanan}' berhasil dilunasi & dana terplotting!", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Ya, Lunasi Sekarang", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPayConfirm = false }) {
                                Text("Batal")
                            }
                        }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    border = BorderStroke(1.2.dp, LilacBorder.copy(alpha = 0.8f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = order.namaPesanan,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "📅 Tanggal Order: ${order.tanggalOrder}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = PinkAurora,
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(
                                    text = "BELUM LUNAS",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MagentaLembut
                                )
                            }
                        }

                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Volume Order", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                Text("${order.qtyOrder} ${order.satuan}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Harga Satuan", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                Text(formatRupiah(order.hargaSatuan), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Tagihan", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                Text(
                                    formatRupiah(order.totalPendapatan),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colorScheme.primary
                                )
                            }
                        }

                        // Estimasi Alokasi Hasil Plotting Bar
                        val kertasHpp = accounts.find { it.namaAkun.contains("Kertas") }?.konstanHppUnit?.toDouble() ?: 106.0
                        val tintaHpp = accounts.find { it.namaAkun.contains("Tinta") }?.konstanHppUnit?.toDouble() ?: 25.0
                        val alokasiKertasEst = order.qtyOrder.toDouble() * kertasHpp
                        val alokasiTintaEst = order.qtyOrder.toDouble() * tintaHpp
                        val alokasiLabaEst = (order.qtyOrder.toDouble() * order.hargaSatuan) - (alokasiKertasEst + alokasiTintaEst + (order.jumlahPlastikPengemasan * 300.0))

                        Surface(
                            color = colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Text("📦 Kertas: ${formatRupiah(alokasiKertasEst)}", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                                Text("💧 Tinta: ${formatRupiah(alokasiTintaEst)}", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                                Text("✨ Laba: ${formatRupiah(if (alokasiLabaEst > 0) alokasiLabaEst else 0.0)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = colorScheme.error.copy(alpha = 0.8f)
                                )
                            }

                            Button(
                                onClick = { showPayConfirm = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lunasi & Plotting Sekarang", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
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
}

@Composable
fun RiwayatKasTab(
    viewModel: FinanceViewModel,
    allOrdersList: List<TransaksiOrderMasuk>
) {
    val colorScheme = MaterialTheme.colorScheme
    val filteredList = remember(allOrdersList) {
        allOrdersList.filter { it.status == "Lunas" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.8.dp, colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Riwayat Transaksi Lunas",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onBackground
                            )
                            Text(
                                text = "${filteredList.size} pesanan telah selesai diproses",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada transaksi lunas",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredList) { order ->
                OrderCardItem(order, viewModel, colorScheme)
            }
        }

        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
fun OrderCardItem(
    order: TransaksiOrderMasuk,
    viewModel: FinanceViewModel,
    colorScheme: ColorScheme
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Konfirmasi Hapus Riwayat") },
            text = { Text("Apakah Anda yakin ingin menghapus riwayat nota ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteOrder(order)
                        showDeleteConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_delete_kas_order_button")
                ) {
                    Text("Ya, Hapus", color = colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    modifier = Modifier.testTag("cancel_delete_kas_order_button")
                ) {
                    Text("Batal")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(0.8.dp, colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.namaPesanan,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tanggal: ${order.tanggalOrder}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                val isLunas = order.status == "Lunas"
                val badgeColor = if (isLunas) MintAurora else PinkAurora
                val badgeTextColor = if (isLunas) HijauGelap else MagentaLembut
                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = if (isLunas) "LUNAS" else "PENDING",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeTextColor
                    )
                }
            }

            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Rincian Paket", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                    Text("${order.qtyOrder} ${order.satuan}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Harga Satuan", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                    Text(formatRupiah(order.hargaSatuan), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Bayar", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                    Text(formatRupiah(order.totalPendapatan), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Transaksi",
                        tint = colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                FilledTonalButton(
                    onClick = {
                        val nextStatus = if (order.status == "Lunas") "Belum Lunas" else "Lunas"
                        viewModel.updateOrder(order.copy(status = nextStatus))
                    },
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (order.status == "Lunas") Icons.Default.Close else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (order.status == "Lunas") "Ubah Status" else "Tandai Lunas",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FuturePlaceholderScreen(title: String, icon: ImageVector, description: String) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Fitur Masa Depan",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary,
                    modifier = Modifier
                        .background(colorScheme.primaryContainer, RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ==========================================
// TAB 1: DASHBOARD FINANSIAL (MINIMALIST)
// ==========================================
@Composable
fun DashboardTab(
    viewModel: FinanceViewModel,
    summary: DashboardSummary,
    orders: List<TransaksiOrderMasuk>,
    mutations: List<MutasiManualKeluarMasuk>,
    accounts: List<MasterAkunSaldo>,
    userProfile: UserProfile,
    onNavigateToOrderNota: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val chartFilter by viewModel.customerChartFilter.collectAsStateWithLifecycle()
    val customerFrequencyList by viewModel.customerOrderFrequency.collectAsStateWithLifecycle()
    val allocationSummary by viewModel.allocationComparisonSummary.collectAsStateWithLifecycle()
    
    val totalOrdersCount = orders.size
    val totalUnitsCount = orders.sumOf { it.qtyOrder }
    val totalLabaKotor = orders.sumOf { it.qtyOrder.toDouble() * it.hargaSatuan }
    val avgOrderValue = if (totalOrdersCount > 0) totalLabaKotor / totalOrdersCount else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Executive Summary Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "TOTAL KAS FISIK RIIL",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF554B6E)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Rp",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B2369)
                        )
                        Text(
                            text = formatRupiah(summary.grandTotalSisaRiil).removePrefix("Rp ").trim(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF3B2369)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Kotak 1: Terplotting
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF5F0FB), RoundedCornerShape(14.dp))
                                .border(BorderStroke(1.dp, Color(0xFFE4DAF7)), RoundedCornerShape(14.dp))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Terplotting",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF554B6E)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatRupiah(summary.grandTotalPlotting),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF3B2369)
                                )
                            }
                        }
                        
                        // Kotak 2: Penyesuaian
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF5F0FB), RoundedCornerShape(14.dp))
                                .border(BorderStroke(1.dp, Color(0xFFE4DAF7)), RoundedCornerShape(14.dp))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Penyesuaian",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF554B6E)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = (if (summary.grandTotalMutasi >= 0) "+" else "") + formatRupiah(summary.grandTotalMutasi),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (summary.grandTotalMutasi >= 0) Color(0xFF166534) else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Action Chip: Order Nota (Slim UI)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigateToOrderNota() }
                    .testTag("dashboard_order_nota_card"),
                shape = RoundedCornerShape(14.dp),
                color = colorScheme.surface,
                border = BorderStroke(0.8.dp, colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Order Nota",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Hitung HPP & ploting sub-anggaran pos",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Section: Visualisasi Pemasukan (Plotting) vs Pengeluaran (Riil) Pos Alokasi Kas
        item {
            PosAllocationComparisonSection(
                viewModel = viewModel,
                allocationSummary = allocationSummary
            )
        }

        // Section: Beautiful native Allocation Progress Bar Chart
        item {
            AllocationBarChart(summary.rows)
        }

        // Section: Grafik Frekuensi Pemesanan Pelanggan
        item {
            CustomerOrderFrequencyChart(
                selectedFilter = chartFilter,
                frequencies = customerFrequencyList,
                onFilterSelected = { viewModel.setCustomerChartFilter(it) }
            )
        }

        // Section: Metrik Performa Ringkas
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "METRIK PERFORMA OPERASIONAL",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Metric 1: Total Orders
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = Color(0xFFF3EEFA),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Color(0xFF6A4C93),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Total Order",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF554B6E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$totalOrdersCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B2369),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Metric 2: Total Units
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = Color(0xFFF3EEFA),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = Color(0xFF6A4C93),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Unit Terproduksi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF554B6E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$totalUnitsCount pcs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B2369),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Metric 3: Total Gross Earnings
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = Color(0xFFF3EEFA),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF6A4C93),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Pendapatan Kotor",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF554B6E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatRupiah(totalLabaKotor),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Metric 4: Average Order
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = Color(0xFFF3EEFA),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Functions,
                                    contentDescription = null,
                                    tint = Color(0xFF6A4C93),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Rata-rata Order",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF554B6E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatRupiah(avgOrderValue),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B2369),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Aktivitas Terkini (Recent Mutations) Widget
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_recent_mutations_widget"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AKTIVITAS TERKINI (MUTASI KAS)",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    val recentMutations = mutations.sortedByDescending { it.idMutasi }.take(5)

                    if (recentMutations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada riwayat mutasi kas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            recentMutations.forEachIndexed { index, mutation ->
                                if (index > 0) {
                                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }

                                val isMasuk = mutation.jenisMutasi == "Uang Masuk"
                                val isTransfer = mutation.jenisMutasi == "Pindah Saldo"
                                val sourceName = accounts.find { it.idAkun == mutation.idAkun }?.namaAkun?.replace("DOMPET ", "") ?: "Akun"
                                val targetName = if (isTransfer) accounts.find { it.idAkun == mutation.idAkunTujuan }?.namaAkun?.replace("DOMPET ", "") else null

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                isMasuk -> Icons.Default.AddCircle
                                                isTransfer -> Icons.Default.SwapHoriz
                                                else -> Icons.Default.RemoveCircle
                                            },
                                            contentDescription = null,
                                            tint = when {
                                                isMasuk -> Color(0xFF2E7D32)
                                                isTransfer -> colorScheme.primary
                                                else -> Color(0xFFC62828)
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Column {
                                            val labelText = if (isTransfer) {
                                                "$sourceName ➔ $targetName"
                                            } else {
                                                sourceName
                                            }
                                            Text(
                                                text = labelText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${mutation.tanggalMutasi} - ${mutation.keterangan}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Text(
                                        text = when {
                                            isMasuk -> "+" + formatRupiah(mutation.nominal)
                                            isTransfer -> formatRupiah(mutation.nominal)
                                            else -> "-" + formatRupiah(mutation.nominal)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            isMasuk -> Color(0xFF2E7D32)
                                            isTransfer -> colorScheme.primary
                                            else -> Color(0xFFC62828)
                                        }
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

@Composable
fun AllocationBarChart(rows: List<AccountDashboardRow>) {
    val colorScheme = MaterialTheme.colorScheme
    val nonZeroRows = rows.filter { it.sisaSaldoRiil != 0.0 }
    val total = rows.sumOf { if (it.sisaSaldoRiil > 0.0) it.sisaSaldoRiil else 0.0 }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "KOMPOSISI ALOKASI KAS RIIL",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (total <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(colorScheme.surfaceVariant, RoundedCornerShape(100.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada alokasi kas terdata", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
                }
            } else {
                // Stacked Bar Layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(colorScheme.surfaceVariant)
                ) {
                    val colors = listOf(
                        Color(0xFF2196F3), // Kertas
                        Color(0xFF9C27B0), // Tinta
                        Color(0xFFFF9800), // Pengemasan
                        Color(0xFFE91E63), // Waste
                        Color(0xFF4CAF50), // Tenaga Kerja
                        Color(0xFFFFEB3B), // Listrik
                        Color(0xFF795548), // Maintenance
                        Color(0xFF009688)  // Sisa Laba
                    )
                    
                    rows.forEachIndexed { index, row ->
                        if (row.sisaSaldoRiil > 0.0) {
                            val weight = (row.sisaSaldoRiil / total).toFloat()
                            val color = colors.getOrElse(index % colors.size) { colorScheme.primary }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(weight)
                                    .background(color)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legends Grid
            val colors = listOf(
                Color(0xFF2196F3),
                Color(0xFF9C27B0),
                Color(0xFFFF9800),
                Color(0xFFE91E63),
                Color(0xFF4CAF50),
                Color(0xFFFFEB3B),
                Color(0xFF795548),
                Color(0xFF009688)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.chunked(2).forEach { rowPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowPair.forEach { row ->
                            val index = rows.indexOf(row)
                            val color = colors.getOrElse(index % colors.size) { colorScheme.primary }
                            val percentage = if (total > 0 && row.sisaSaldoRiil > 0) (row.sisaSaldoRiil / total * 100.0) else 0.0
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(color)
                                )
                                Text(
                                    text = "${row.namaAkun.replace("Dompet ", "")} (${String.format(Locale.US, "%.1f", percentage)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosAllocationComparisonSection(
    viewModel: FinanceViewModel,
    allocationSummary: PosAllocationSummary
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var activeDebitItem by remember { mutableStateOf<AllocationComparisonItem?>(null) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("section_pos_allocation_comparison"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.2.dp, LilacBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(LilacPastel, CircleShape)
                            .border(BorderStroke(1.dp, LilacBorder), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = Color(0xFF6B46C1),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "PERBANDINGAN ANGGARAN POS KAS",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF553C9A)
                        )
                        Text(
                            text = "Pemasukan (Plotting) vs Pengeluaran (Riil)",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Period Filter Chips & Active Range Banner (Clean Single-Layer Flat Design)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1. Deretan Tombol Filter Rata Penuh (Equal Width & Flat Single Layer)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf("Bulan Ini", "Bulan Lalu", "Semua Waktu", "Kustom")
                    filters.forEach { filter ->
                        val isSelected = allocationSummary.filterLabel == filter
                        Surface(
                            selected = isSelected,
                            onClick = {
                                if (filter == "Kustom") {
                                    showCustomDateDialog = true
                                } else {
                                    viewModel.setAllocationFilter(filter)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF6B46C1) else LilacPastel.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF6B46C1) else LilacBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (filter == "Kustom") {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = if (isSelected) Color.White else Color(0xFF553C9A)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = filter,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF553C9A),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 2. Active Date Range Banner (Aligned & Flat Single Layer)
                val activeDateText = when {
                    allocationSummary.filterLabel == "Semua Waktu" -> "Semua Riwayat Transaksi"
                    allocationSummary.startDate.isNotBlank() && allocationSummary.endDate.isNotBlank() ->
                        "${allocationSummary.startDate} s/d ${allocationSummary.endDate}"
                    else -> "${viewModel.getStartOfMonthString()} s/d ${viewModel.getEndOfMonthString()}"
                }

                Surface(
                    color = LilacPastel.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LilacBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomDateDialog = true }
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
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color(0xFF553C9A),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = activeDateText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF553C9A)
                            )
                        }
                        Text(
                            text = "Ubah Rentang",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B46C1)
                        )
                    }
                }
            }

            // Summary 3-Column Cards: Total Masuk, Total Keluar, Sisa Kas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card Masuk Plotting
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF5F0FB), RoundedCornerShape(14.dp))
                        .border(BorderStroke(1.dp, Color(0xFFE4DAF7)), RoundedCornerShape(14.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF6A4C93), CircleShape))
                            Text("Masuk (Plotting)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF554B6E))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRupiah(allocationSummary.grandTotalMasuk),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B2369),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Card Keluar Riil
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF5F0FB), RoundedCornerShape(14.dp))
                        .border(BorderStroke(1.dp, Color(0xFFE4DAF7)), RoundedCornerShape(14.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFC62828), CircleShape))
                            Text("Keluar (Riil)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF554B6E))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRupiah(allocationSummary.grandTotalKeluar),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Card Sisa Kas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF5F0FB), RoundedCornerShape(14.dp))
                        .border(BorderStroke(1.dp, Color(0xFFE4DAF7)), RoundedCornerShape(14.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF166534), CircleShape))
                            Text("Sisa Kas", style = MaterialTheme.typography.labelSmall, color = Color(0xFF554B6E))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRupiah(allocationSummary.grandTotalSisa),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (allocationSummary.grandTotalSisa >= 0) Color(0xFF166534) else Color(0xFFC62828),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Comparative Dual-Bar Visualizer for each pos
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val maxBarValue = maxOf(
                    allocationSummary.items.maxOfOrNull { maxOf(it.totalMasukPlotting, it.totalKeluarRiil) } ?: 1.0,
                    1.0
                )

                allocationSummary.items.forEach { item ->
                    val cleanName = item.namaAkun.replace("Dompet ", "")
                    val icon = when {
                        cleanName.contains("Kertas") -> Icons.Default.Description
                        cleanName.contains("Tinta") -> Icons.Default.InvertColors
                        cleanName.contains("Pengemasan") -> Icons.Default.Inventory2
                        cleanName.contains("Waste") -> Icons.Default.DeleteOutline
                        cleanName.contains("Tenaga") -> Icons.Default.Badge
                        cleanName.contains("Listrik") -> Icons.Default.FlashOn
                        cleanName.contains("Maintenance") -> Icons.Default.Build
                        else -> Icons.Default.MonetizationOn
                    }

                    val posAccentColor = when {
                        cleanName.contains("Kertas") -> Color(0xFF2196F3)
                        cleanName.contains("Tinta") -> Color(0xFF9C27B0)
                        cleanName.contains("Pengemasan") -> Color(0xFFFF9800)
                        cleanName.contains("Waste") -> Color(0xFFE91E63)
                        cleanName.contains("Tenaga") -> Color(0xFF4CAF50)
                        cleanName.contains("Listrik") -> Color(0xFFFBC02D)
                        cleanName.contains("Maintenance") -> Color(0xFF795548)
                        else -> Color(0xFF009688)
                    }

                    val masukRatio = ((item.totalMasukPlotting / maxBarValue).coerceIn(0.0, 1.0)).toFloat()
                    val keluarRatio = ((item.totalKeluarRiil / maxBarValue).coerceIn(0.0, 1.0)).toFloat()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row Header: Icon + Name + Serapan Badge + Debit Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(posAccentColor.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = posAccentColor, modifier = Modifier.size(18.dp))
                                    }
                                    Column {
                                        Text(
                                            text = cleanName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Sisa: ${formatRupiah(item.sisaSaldo)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (item.sisaSaldo >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Serapan Badge
                                    val isOverbudget = item.totalKeluarRiil > item.totalMasukPlotting && item.totalMasukPlotting > 0
                                    Surface(
                                        color = if (isOverbudget) Color(0xFFFFEBEE) else LilacPastel,
                                        shape = RoundedCornerShape(100.dp)
                                    ) {
                                        Text(
                                            text = if (isOverbudget) "Overbudget" else String.format(Locale.US, "%.0f%% Serap", item.persentaseSerapan),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOverbudget) Color(0xFFC62828) else Color(0xFF6B46C1)
                                        )
                                    }

                                    // Quick Debit Button
                                    Button(
                                        onClick = { activeDebitItem = item },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(100.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colorScheme.primaryContainer,
                                            contentColor = colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Debet", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Dual Horizontal Bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Top Bar: Masuk (Plotting)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.width(66.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Masuk",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Masuk",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(masukRatio.coerceAtLeast(0.02f))
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(Color(0xFF166534))
                                        )
                                    }
                                    Text(
                                        text = formatRupiah(item.totalMasukPlotting),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20),
                                        modifier = Modifier.width(90.dp),
                                        textAlign = TextAlign.End
                                    )
                                }

                                // Bottom Bar: Keluar (Riil)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.width(66.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Keluar",
                                            tint = Color(0xFFE53E3E),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Keluar",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFE53E3E)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(keluarRatio.coerceAtLeast(if (item.totalKeluarRiil > 0) 0.02f else 0f))
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(Color(0xFFC62828))
                                        )
                                    }
                                    Text(
                                        text = formatRupiah(item.totalKeluarRiil),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC53030),
                                        modifier = Modifier.width(90.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Debit Dialog for recording immediate expense against the chosen Pos
    activeDebitItem?.let { item ->
        var nominalStr by remember { mutableStateOf("") }
        var descriptionStr by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { activeDebitItem = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.RemoveCircle,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Debet ${item.namaAkun}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Saldo saat ini: ${formatRupiah(item.sisaSaldo)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = nominalStr,
                        onValueChange = {
                            nominalStr = it
                            errorMessage = null
                        },
                        label = { Text("Nominal Pengeluaran (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF7F5FC),
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant,
                            focusedLabelColor = colorScheme.primary,
                            unfocusedLabelColor = colorScheme.onSurfaceVariant,
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            cursorColor = colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = descriptionStr,
                        onValueChange = {
                            descriptionStr = it
                            errorMessage = null
                        },
                        label = { Text("Keterangan Pengeluaran") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: Pembelian stok, Penggunaan operasional") },
                        singleLine = false,
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF7F5FC),
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant,
                            focusedLabelColor = colorScheme.primary,
                            unfocusedLabelColor = colorScheme.onSurfaceVariant,
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            cursorColor = colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = parseDoubleInput(nominalStr)
                        if (amount == null || amount <= 0.0) {
                            errorMessage = "Harap masukkan nominal angka yang valid!"
                            return@Button
                        }
                        if (descriptionStr.isBlank()) {
                            errorMessage = "Harap isi keterangan pengeluaran!"
                            return@Button
                        }

                        viewModel.insertMutation(
                            tanggal = viewModel.getTodayString(),
                            idAkun = item.idAkun,
                            jenis = "Uang Keluar",
                            nominal = amount,
                            keterangan = descriptionStr
                        )

                        Toast.makeText(context, "Pengeluaran ${item.namaAkun} berhasil dicatat & disinkronkan!", Toast.LENGTH_SHORT).show()
                        activeDebitItem = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC62828),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("Catat Pengeluaran", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDebitItem = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Custom Date Range Dialog using native Material 3 DateRangePicker
    if (showCustomDateDialog) {
        val formatter = remember { java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd") }
        val initialStartMillis = remember(allocationSummary.startDate) {
            if (allocationSummary.startDate.isNotBlank()) {
                runCatching {
                    java.time.LocalDate.parse(allocationSummary.startDate, formatter)
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                }.getOrNull()
            } else {
                runCatching {
                    java.time.LocalDate.parse(viewModel.getStartOfMonthString(), formatter)
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                }.getOrNull()
            }
        }
        val initialEndMillis = remember(allocationSummary.endDate) {
            if (allocationSummary.endDate.isNotBlank()) {
                runCatching {
                    java.time.LocalDate.parse(allocationSummary.endDate, formatter)
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                }.getOrNull()
            } else {
                runCatching {
                    java.time.LocalDate.parse(viewModel.getEndOfMonthString(), formatter)
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                }.getOrNull()
            }
        }

        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialStartMillis,
            initialSelectedEndDateMillis = initialEndMillis
        )

        fun formatMillisToDate(millis: Long): String {
            return java.time.Instant.ofEpochMilli(millis)
                .atZone(java.time.ZoneId.of("UTC"))
                .toLocalDate()
                .format(formatter)
        }

        DatePickerDialog(
            onDismissRequest = { showCustomDateDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val startMillis = dateRangePickerState.selectedStartDateMillis
                        val endMillis = dateRangePickerState.selectedEndDateMillis ?: startMillis
                        if (startMillis != null) {
                            val startStr = formatMillisToDate(startMillis)
                            val endStr = formatMillisToDate(endMillis!!)
                            viewModel.setAllocationCustomDateRange(startStr, endStr)
                        }
                        showCustomDateDialog = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B46C1),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF6B46C1).copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Terapkan Rentang", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateDialog = false }) {
                    Text("Batal", color = colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = DatePickerDefaults.colors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Quick shortcut chips inside header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.setAllocationFilter("Bulan Ini")
                            showCustomDateDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = LilacPastel.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, LilacBorder),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text("Bulan Ini", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B46C1), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.setAllocationFilter("Bulan Lalu")
                            showCustomDateDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = LilacPastel.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, LilacBorder),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text("Bulan Lalu", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B46C1), fontWeight = FontWeight.Bold)
                    }
                }

                DateRangePicker(
                    state = dateRangePickerState,
                    title = {
                        Text(
                            text = "Pilih Rentang Tanggal",
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 2.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF553C9A)
                        )
                    },
                    headline = {
                        DateRangePickerDefaults.DateRangePickerHeadline(
                            selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis,
                            selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis,
                            displayMode = dateRangePickerState.displayMode,
                            dateFormatter = remember { DatePickerDefaults.dateFormatter() },
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                        )
                    },
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.White,
                        selectedDayContainerColor = Color(0xFF6B46C1),
                        selectedDayContentColor = Color.White,
                        dayInSelectionRangeContainerColor = LilacPastel,
                        dayInSelectionRangeContentColor = Color(0xFF553C9A),
                        todayDateBorderColor = Color(0xFF6B46C1),
                        todayContentColor = Color(0xFF6B46C1),
                        headlineContentColor = Color(0xFF553C9A),
                        titleContentColor = Color(0xFF553C9A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                )
            }
        }
    }
}

@Composable
fun DompetScreen(
    rows: List<AccountDashboardRow>,
    viewModel: FinanceViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WalletEnvelopesSection(rows = rows, viewModel = viewModel)
        }
    }
}

@Composable
fun WalletEnvelopesSection(
    rows: List<AccountDashboardRow>,
    viewModel: FinanceViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    var activeWalletToDebit by remember { mutableStateOf<AccountDashboardRow?>(null) }
    var selectedPosKasDetail by remember { mutableStateOf<AccountDashboardRow?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MANAJEMEN DOMPET & POS KAS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Alokasi otomatis & potong saldo pengeluaran (Klik untuk detail & aksi cepat)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.outline
                    )
                }
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { row ->
                    val icon = when {
                        row.namaAkun.contains("Kertas") -> Icons.Default.Description
                        row.namaAkun.contains("Tinta") -> Icons.Default.InvertColors
                        row.namaAkun.contains("Pengemasan") -> Icons.Default.Inventory2
                        row.namaAkun.contains("Waste") -> Icons.Default.DeleteOutline
                        row.namaAkun.contains("Tenaga") -> Icons.Default.Badge
                        row.namaAkun.contains("Listrik") -> Icons.Default.FlashOn
                        row.namaAkun.contains("Maintenance") -> Icons.Default.Build
                        row.namaAkun.contains("Me GpS", ignoreCase = true) -> Icons.Default.GpsFixed
                        else -> Icons.Default.MonetizationOn
                    }

                    val avatarBg = when {
                        row.namaAkun.contains("Kertas") -> Color(0xFFE3F2FD)
                        row.namaAkun.contains("Tinta") -> Color(0xFFF3E5F5)
                        row.namaAkun.contains("Pengemasan") -> Color(0xFFFFF3E0)
                        row.namaAkun.contains("Waste") -> Color(0xFFFCE4EC)
                        row.namaAkun.contains("Tenaga") -> Color(0xFFE8F5E9)
                        row.namaAkun.contains("Listrik") -> Color(0xFFFFFDE7)
                        row.namaAkun.contains("Maintenance") -> Color(0xFFEFEBE9)
                        row.namaAkun.contains("Me GpS", ignoreCase = true) -> Color(0xFFEDE7F6)
                        else -> Color(0xFFE0F2F1)
                    }

                    val avatarTint = when {
                        row.namaAkun.contains("Kertas") -> Color(0xFF1E88E5)
                        row.namaAkun.contains("Tinta") -> Color(0xFF8E24AA)
                        row.namaAkun.contains("Pengemasan") -> Color(0xFFF57C00)
                        row.namaAkun.contains("Waste") -> Color(0xFFD81B60)
                        row.namaAkun.contains("Tenaga") -> Color(0xFF43A047)
                        row.namaAkun.contains("Listrik") -> Color(0xFFFBC02D)
                        row.namaAkun.contains("Maintenance") -> Color(0xFF6D4C41)
                        row.namaAkun.contains("Me GpS", ignoreCase = true) -> Color(0xFF5E35B1)
                        else -> Color(0xFF00897B)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPosKasDetail = row },
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(avatarBg, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = avatarTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = row.namaAkun.replace("Dompet ", "", ignoreCase = true),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Alokasi: ${formatRupiah(row.saldoTerplotting)} • Mutasi: ${if (row.mutasiPenyesuain >= 0) "+" else ""}${formatRupiah(row.mutasiPenyesuain)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = formatRupiah(row.sisaSaldoRiil),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (row.sisaSaldoRiil >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )

                                Button(
                                    onClick = { activeWalletToDebit = row },
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("btn_debit_${row.namaAkun.replace(" ", "_").lowercase()}"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(100.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.primaryContainer,
                                        contentColor = colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.RemoveCircleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Debet", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal/Dialog Form Pengeluaran
    activeWalletToDebit?.let { wallet ->
        var nominalStr by remember { mutableStateOf("") }
        var descriptionStr by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { activeWalletToDebit = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.RemoveCircle,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Debet ${wallet.namaAkun}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Saldo saat ini: ${formatRupiah(wallet.sisaSaldoRiil)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = nominalStr,
                        onValueChange = {
                            nominalStr = it
                            errorMessage = null
                        },
                        label = { Text("Nominal Pengeluaran (Rp)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_wallet_expense_nominal"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF7F5FC),
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant,
                            focusedLabelColor = colorScheme.primary,
                            unfocusedLabelColor = colorScheme.onSurfaceVariant,
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            cursorColor = colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = descriptionStr,
                        onValueChange = {
                            descriptionStr = it
                            errorMessage = null
                        },
                        label = { Text("Keterangan Pengeluaran") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_wallet_expense_keterangan"),
                        placeholder = { Text("Contoh: Beli kemasan baru, Bayar token") },
                        singleLine = false,
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF7F5FC),
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant,
                            focusedLabelColor = colorScheme.primary,
                            unfocusedLabelColor = colorScheme.onSurfaceVariant,
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            cursorColor = colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = parseDoubleInput(nominalStr)
                        if (amount == null || amount <= 0.0) {
                            errorMessage = "Harap masukkan nominal angka yang valid!"
                            return@Button
                        }
                        if (descriptionStr.isBlank()) {
                            errorMessage = "Harap isi keterangan pengeluaran!"
                            return@Button
                        }

                        // Save "Uang Keluar" mutation (negative adjustment on real-time balance)
                        viewModel.insertMutation(
                            tanggal = viewModel.getTodayString(),
                            idAkun = wallet.idAkun,
                            jenis = "Uang Keluar",
                            nominal = amount,
                            keterangan = descriptionStr
                        )

                        Toast.makeText(context, "Berhasil memotong saldo ${wallet.namaAkun}!", Toast.LENGTH_SHORT).show()
                        activeWalletToDebit = null
                    },
                    modifier = Modifier.testTag("btn_submit_wallet_expense"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC62828),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("Potong Saldo", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { activeWalletToDebit = null }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    selectedPosKasDetail?.let { row ->
        DetailLedgerDialog(
            account = row,
            viewModel = viewModel,
            showQuickActions = true,
            onDismiss = { selectedPosKasDetail = null }
        )
    }
}

@Composable
fun CustomerOrderFrequencyChart(
    selectedFilter: String,
    frequencies: List<CustomerFrequency>,
    onFilterSelected: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_order_frequency_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FREKUENSI PEMESANAN PELANGGAN",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Pelanggan terbanyak & jumlah order",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Tabs/Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("Minggu Ini", "Bulan Ini", "Tahun Ini")
                filters.forEach { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) colorScheme.primary else colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .clickable { onFilterSelected(filter) }
                            .testTag("filter_chip_$filter"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (frequencies.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Tidak ada data pemesanan",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Belum ada pesanan terdata untuk periode ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                // Beautiful Chart Rows
                val maxCount = frequencies.maxOf { it.orderCount }.toDouble()
                val topFrequencies = frequencies.take(5) // show top 5 customers

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    topFrequencies.forEachIndexed { index, item ->
                        val ratio = if (maxCount > 0) item.orderCount.toDouble() / maxCount else 0.0
                        
                        // Row with customer name and value
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rank Badge
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (index) {
                                                    0 -> Color(0xFFFFD700) // Gold
                                                    1 -> Color(0xFFC0C0C0) // Silver
                                                    2 -> Color(0xFFCD7F32) // Bronze
                                                    else -> colorScheme.surfaceVariant
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (index < 3) Color(0xFF1E1E1E) else colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = item.customerName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${item.orderCount} Order",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )
                            }
                            
                            // Visual progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(ratio.toFloat())
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: INPUT TRANSAKSI (Form Order Masuk)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersTab(orders: List<TransaksiOrderMasuk>, viewModel: FinanceViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val customFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color(0xFFF7F5FC),
        focusedBorderColor = colorScheme.primary,
        unfocusedBorderColor = colorScheme.outlineVariant,
        focusedLabelColor = colorScheme.primary,
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        focusedTextColor = colorScheme.onSurface,
        unfocusedTextColor = colorScheme.onSurface,
        cursorColor = colorScheme.primary
    )
    val customFieldShape = RoundedCornerShape(12.dp)
    var editingOrderId by remember { mutableStateOf<Int?>(null) }
    var tanggal by remember { mutableStateOf(viewModel.getTodayString()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var namaPelangganText by remember { mutableStateOf("") }
    var pelangganDropdownExpanded by remember { mutableStateOf(false) }

    var namaPesanan by remember { mutableStateOf("") }
    var qtyOrderText by remember { mutableStateOf("") }

    var selectedSatuanName by remember { mutableStateOf("Default") }
    var satuanExpanded by remember { mutableStateOf(false) }

    var hargaSatuanText by remember { mutableStateOf("") }
    var jumlahPlastikText by remember { mutableStateOf("") }
    var isLunas by remember { mutableStateOf(true) } // true: Lunas, false: Belum Lunas

    var showErrorAlert by remember { mutableStateOf(false) }

    var showNewCustomerDialog by remember { mutableStateOf(false) }
    var newCustomerNameInput by remember { mutableStateOf("") }
    var newCustomerContactInput by remember { mutableStateOf("") }
    var isPelangganFocused by remember { mutableStateOf(false) }

    // Master data from ViewModel
    val pelangganList by viewModel.allPelanggan.collectAsStateWithLifecycle(emptyList())
    val satuanHargaList by viewModel.allSatuanHarga.collectAsStateWithLifecycle(emptyList())
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle(emptyList())
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isCloudOnline by viewModel.isCloudOnline.collectAsStateWithLifecycle()
    val syncStatusText by viewModel.syncStatusText.collectAsStateWithLifecycle()

    val onEditOrder: (TransaksiOrderMasuk) -> Unit = { order ->
        editingOrderId = order.idOrder
        tanggal = order.tanggalOrder
        val fullTitle = order.namaPesanan
        val dashIndex = fullTitle.indexOf(" - ")
        if (dashIndex != -1) {
            namaPelangganText = fullTitle.substring(0, dashIndex)
            namaPesanan = fullTitle.substring(dashIndex + 3)
        } else {
            namaPelangganText = fullTitle
            namaPesanan = ""
        }
        qtyOrderText = order.qtyOrder.toString()
        selectedSatuanName = if (order.satuan.isBlank()) "Default" else order.satuan
        hargaSatuanText = formatDouble(order.hargaSatuan)
        jumlahPlastikText = order.jumlahPlastikPengemasan.toString()
        isLunas = (order.status == "Lunas")
        showErrorAlert = false
    }

    if (showDatePicker) {
        OrderDatePickerDialog(
            onDateSelected = { tanggal = it },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showNewCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showNewCustomerDialog = false },
            title = { Text("Pelanggan Baru Terdeteksi") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text("Apakah Anda ingin mendaftarkannya ke Master Data?")
                    OutlinedTextField(
                        value = newCustomerNameInput,
                        onValueChange = { newCustomerNameInput = it },
                        label = { Text("Nama Pelanggan") },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_new_pelanggan_nama"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCustomerContactInput,
                        onValueChange = { newCustomerContactInput = it },
                        label = { Text("Nomor Telepon/Kontak (Opsional)") },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_new_pelanggan_kontak"),
                        placeholder = { Text("Contoh: 0812345678") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustomerNameInput.isNotBlank()) {
                            viewModel.insertPelanggan(
                                newCustomerNameInput.trim(),
                                if (newCustomerContactInput.isBlank()) null else newCustomerContactInput.trim()
                            )
                            namaPelangganText = newCustomerNameInput.trim()
                            showNewCustomerDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_new_pelanggan_submit")
                ) {
                    Text("Simpan & Daftarkan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNewCustomerDialog = false },
                    modifier = Modifier.testTag("dialog_new_pelanggan_dismiss")
                ) {
                    Text("Batal")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (editingOrderId != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_mode_transaksi_banner"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    "Mode Edit Transaksi #${editingOrderId}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Ubah data transaksi lalu tekan Perbarui Transaksi untuk memperbarui tanpa duplikasi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                editingOrderId = null
                                namaPelangganText = ""
                                namaPesanan = ""
                                qtyOrderText = ""
                                hargaSatuanText = ""
                                jumlahPlastikText = ""
                                selectedSatuanName = "Default"
                                showErrorAlert = false
                            }
                        ) {
                            Text("Batal Edit", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Form Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (editingOrderId != null) "Edit Transaksi Order #${editingOrderId}" else "Input Transaksi Order Baru",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )

                    // Date Input with Picker
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = tanggal,
                            onValueChange = { tanggal = it },
                            label = { Text("Tanggal (YYYY-MM-DD)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_order_tanggal"),
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                                }
                            },
                            colors = customFieldColors,
                            shape = customFieldShape,
                            singleLine = true
                        )
                    }

                    // Pelanggan Row (Text input with trailing icon trigger)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            val typedName = namaPelangganText.trim()
                            val exists = typedName.isNotEmpty() && pelangganList.any { it.namaPelanggan.trim().equals(typedName, ignoreCase = true) }

                            OutlinedTextField(
                                value = namaPelangganText,
                                onValueChange = { namaPelangganText = it },
                                label = { Text("Nama Pelanggan") },
                                placeholder = { Text("Ketik nama atau pilih...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (isPelangganFocused && !focusState.isFocused) {
                                            val typed = namaPelangganText.trim()
                                            if (typed.isNotEmpty()) {
                                                val ex = pelangganList.any { it.namaPelanggan.trim().equals(typed, ignoreCase = true) }
                                                if (!ex) {
                                                    newCustomerNameInput = typed
                                                    newCustomerContactInput = ""
                                                    showNewCustomerDialog = true
                                                }
                                            }
                                        }
                                        isPelangganFocused = focusState.isFocused
                                    }
                                    .testTag("input_order_pelanggan_text"),
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                trailingIcon = {
                                    if (typedName.isNotEmpty() && !exists) {
                                        IconButton(
                                            onClick = {
                                                newCustomerNameInput = typedName
                                                newCustomerContactInput = ""
                                                showNewCustomerDialog = true
                                            },
                                            modifier = Modifier.testTag("input_order_pelanggan_add_quick")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Tambah Pelanggan Baru",
                                                tint = androidx.compose.ui.graphics.Color(0xFF8E24AA) // Purple
                                            )
                                        }
                                    } else {
                                        IconButton(
                                            onClick = { pelangganDropdownExpanded = !pelangganDropdownExpanded },
                                            modifier = Modifier.testTag("input_order_pelanggan_dropdown")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Pilih dari Master"
                                            )
                                        }
                                    }
                                },
                                colors = customFieldColors,
                                shape = customFieldShape,
                                singleLine = true
                            )

                            DropdownMenu(
                                expanded = pelangganDropdownExpanded,
                                onDismissRequest = { pelangganDropdownExpanded = false }
                            ) {
                                if (pelangganList.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Belum ada pelanggan terdaftar") },
                                        onClick = { pelangganDropdownExpanded = false }
                                    )
                                } else {
                                    pelangganList.forEach { pelanggan ->
                                        DropdownMenuItem(
                                            text = { Text("${pelanggan.namaPelanggan} (${pelanggan.kontak ?: "-"})") },
                                            onClick = {
                                                namaPelangganText = pelanggan.namaPelanggan
                                                pelangganDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Nama Pesanan Detail
                    OutlinedTextField(
                        value = namaPesanan,
                        onValueChange = { namaPesanan = it },
                        label = { Text("Nama Pesanan / Deskripsi") },
                        placeholder = { Text("Contoh: Cetak Leaflet 1000 lbr") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_order_nama"),
                        leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )

                    // Jenis Paket & Harga Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = satuanExpanded,
                            onExpandedChange = { satuanExpanded = !satuanExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedSatuanName,
                                onValueChange = {},
                                label = { Text("Jenis Paket") },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("input_order_jenis_paket"),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = satuanExpanded) },
                                colors = customFieldColors,
                                shape = customFieldShape,
                                singleLine = true,
                                readOnly = true
                            )
                            ExposedDropdownMenu(
                                expanded = satuanExpanded,
                                onDismissRequest = { satuanExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Default") },
                                    onClick = {
                                        selectedSatuanName = "Default"
                                        satuanExpanded = false
                                    }
                                )
                                satuanHargaList.forEach { sHarga ->
                                    DropdownMenuItem(
                                        text = { Text("${sHarga.namaSatuan} (${formatRupiah(sHarga.opsiHargaDefault)})") },
                                        onClick = {
                                            selectedSatuanName = sHarga.namaSatuan
                                            hargaSatuanText = String.format(Locale.US, "%.0f", sHarga.opsiHargaDefault)
                                            satuanExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = hargaSatuanText,
                            onValueChange = {
                                if (selectedSatuanName == "Default") {
                                    hargaSatuanText = it
                                }
                            },
                            label = { Text("Harga Satuan (Rp)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_order_harga"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = customFieldColors,
                            shape = customFieldShape,
                            singleLine = true,
                            readOnly = selectedSatuanName != "Default"
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = qtyOrderText,
                            onValueChange = { qtyOrderText = it },
                            label = { Text("Qty Order") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_order_qty"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = customFieldColors,
                            shape = customFieldShape,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = jumlahPlastikText,
                            onValueChange = { jumlahPlastikText = it },
                            label = { Text("Jumlah Plastik") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_order_plastik"),
                            placeholder = { Text("Contoh: 2 (dikali Rp300)") },
                            leadingIcon = { Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = customFieldColors,
                            shape = customFieldShape,
                            singleLine = true
                        )
                    }

                    // REAL-TIME Total Harga Jual Card dengan perhitungan Qty * Harga
                    val qtyVal = parseIntInput(qtyOrderText) ?: 0
                    val hargaVal = parseDoubleInput(hargaSatuanText) ?: 0.0
                    val realTimeTotal = qtyVal.toDouble() * hargaVal

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF3EEFA)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL: ${formatRupiah(realTimeTotal)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF3B2369)
                            )
                        }
                    }

                    // Status segment
                    Column {
                        Text(
                            "Status Pembayaran",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ElevatedFilterChip(
                                selected = isLunas,
                                onClick = { isLunas = true },
                                label = { Text("Lunas") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chip_status_lunas"),
                                leadingIcon = if (isLunas) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                            ElevatedFilterChip(
                                selected = !isLunas,
                                onClick = { isLunas = false },
                                label = { Text("Belum Lunas") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chip_status_belum"),
                                leadingIcon = if (!isLunas) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

                    if (showErrorAlert) {
                        Text(
                            text = "Harap isi Pelanggan, Jenis Paket, dan Qty/Harga Jual dengan benar!",
                            color = colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val qty = parseIntInput(qtyOrderText)
                            val harga = parseDoubleInput(hargaSatuanText)
                            val plastik = parseIntInput(jumlahPlastikText) ?: 0
                            val customerName = namaPelangganText

                            if (customerName.isNotBlank() && qty != null && qty > 0 && harga != null && harga > 0.0 && plastik >= 0) {
                                val isExistingCustomer = pelangganList.any { it.namaPelanggan.trim().equals(customerName.trim(), ignoreCase = true) }
                                if (!isExistingCustomer && editingOrderId == null) {
                                    newCustomerNameInput = customerName.trim()
                                    newCustomerContactInput = ""
                                    showNewCustomerDialog = true
                                } else {
                                    val finalOrderName = if (namaPesanan.isNotBlank()) "$customerName - $namaPesanan" else customerName
                                    if (editingOrderId != null) {
                                        val existingOrder = orders.find { it.idOrder == editingOrderId }
                                        val updatedOrder = (existingOrder ?: TransaksiOrderMasuk(
                                            idOrder = editingOrderId!!,
                                            tanggalOrder = tanggal,
                                            namaPesanan = finalOrderName,
                                            qtyOrder = qty,
                                            satuan = selectedSatuanName,
                                            hargaSatuan = harga,
                                            jumlahPlastikPengemasan = plastik,
                                            status = if (isLunas) "Lunas" else "Belum Lunas"
                                        )).copy(
                                            tanggalOrder = tanggal,
                                            namaPesanan = finalOrderName,
                                            qtyOrder = qty,
                                            satuan = selectedSatuanName,
                                            hargaSatuan = harga,
                                            jumlahPlastikPengemasan = plastik,
                                            status = if (isLunas) "Lunas" else "Belum Lunas"
                                        )
                                        viewModel.updateOrder(updatedOrder)
                                        editingOrderId = null
                                    } else {
                                        viewModel.insertOrder(
                                            tanggal = tanggal,
                                            nama = finalOrderName,
                                            qty = qty,
                                            satuan = selectedSatuanName,
                                            harga = harga,
                                            plastik = plastik,
                                            status = if (isLunas) "Lunas" else "Belum Lunas"
                                        )
                                    }
                                    // Clear Form
                                    namaPelangganText = ""
                                    namaPesanan = ""
                                    qtyOrderText = ""
                                    hargaSatuanText = ""
                                    jumlahPlastikText = ""
                                    selectedSatuanName = "Default"
                                    showErrorAlert = false
                                }
                            } else {
                                showErrorAlert = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_order_button"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(if (editingOrderId != null) Icons.Default.Edit else Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (editingOrderId != null) "Perbarui Transaksi & Auto-Plotting" else "Simpan Transaksi & Auto-Plotting",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Transaksi Order",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                )
                Box(
                    modifier = Modifier
                        .background(colorScheme.secondaryContainer, RoundedCornerShape(100.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${orders.size} Total",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Belum ada transaksi order masuk.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.secondary
                            )
                        }
                    }
                }
            }
        } else {
            items(orders, key = { it.idOrder }) { order ->
                OrderHistoryCard(
                    order = order,
                    accounts = accounts,
                    onDelete = { viewModel.deleteOrder(order) },
                    onEdit = { onEditOrder(order) }
                )
            }
        }
    }
}

@Composable
fun OrderHistoryCard(
    order: TransaksiOrderMasuk,
    accounts: List<MasterAkunSaldo>,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Konfirmasi Hapus Riwayat") },
            text = { Text("Apakah Anda yakin ingin menghapus riwayat order '${order.namaPesanan}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_delete_history_card_button")
                ) {
                    Text("Ya, Hapus", color = colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    modifier = Modifier.testTag("cancel_delete_history_card_button")
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Retrieve global settings with proper default fallbacks
    val kertasHpp = accounts.find { it.namaAkun == "Kertas" || it.namaAkun == "Dompet Kertas" }?.konstanHppUnit?.toDouble() ?: 106.0
    val tintaHpp = accounts.find { it.namaAkun == "Tinta" || it.namaAkun == "Dompet Tinta" }?.konstanHppUnit?.toDouble() ?: 25.0
    val pengemasanHpp = accounts.find { it.namaAkun == "Pengemasan" || it.namaAkun == "Dompet Pengemasan" }?.konstanHppUnit?.toDouble() ?: 300.0
    val wastePct = accounts.find { it.namaAkun == "Waste" || it.namaAkun == "Dompet Waste / Rusak" }?.persentaseOperasional?.toDouble() ?: 0.05
    val tenagaKerjaPct = accounts.find { it.namaAkun == "Tenaga Kerja" || it.namaAkun == "Dompet Tenaga Kerja" }?.persentaseOperasional?.toDouble() ?: 0.07
    val listrikPct = accounts.find { it.namaAkun == "Listrik" || it.namaAkun == "Dompet Listrik" }?.persentaseOperasional?.toDouble() ?: 0.02
    val maintenancePct = accounts.find { it.namaAkun == "Maintenance Alat" || it.namaAkun == "Dompet Maintenance" }?.persentaseOperasional?.toDouble() ?: 0.05

    val qty = order.qtyOrder.toDouble()
    val totalPendapatan = qty * order.hargaSatuan
    val dynamicKertas = qty * kertasHpp
    val dynamicTinta = qty * tintaHpp
    val dynamicPengemasan = order.jumlahPlastikPengemasan.toDouble() * pengemasanHpp
    val dynamicWaste = wastePct * totalPendapatan
    val dynamicTenagaKerja = tenagaKerjaPct * totalPendapatan
    val dynamicListrik = listrikPct * totalPendapatan
    val dynamicMaintenance = maintenancePct * totalPendapatan
    val dynamicTotalModal = dynamicKertas + dynamicTinta + dynamicPengemasan + dynamicWaste + dynamicTenagaKerja + dynamicListrik + dynamicMaintenance
    val dynamicSisaLaba = totalPendapatan - dynamicTotalModal

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        border = getAuroraBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.namaPesanan,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = order.tanggalOrder,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.outline
                        )
                        Box(
                            modifier = Modifier
                                .background(colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${order.qtyOrder} ${order.satuan}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatRupiah(order.totalPendapatan),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (order.status == "Lunas") MintAurora else PinkAurora
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (order.status == "Lunas") "SUDAH LUNAS" else "PENDING BAYAR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (order.status == "Lunas") HijauGelap else MagentaLembut,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("edit_order_header_button_${order.idOrder}")
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Transaksi",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("delete_order_header_button_${order.idOrder}")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus Transaksi",
                                tint = colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = colorScheme.outlineVariant)
                    
                    Text(
                        text = "Rincian Autoplotting Envelopes:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )

                    val itemsList = listOf(
                        Triple("Kertas (Qty * ${formatDouble(kertasHpp)})", dynamicKertas, Icons.Default.Description),
                        Triple("Tinta (Qty * ${formatDouble(tintaHpp)})", dynamicTinta, Icons.Default.InvertColors),
                        Triple("Pengemasan (Plastik * ${formatDouble(pengemasanHpp)})", dynamicPengemasan, Icons.Default.Inventory2),
                        Triple("Waste (${formatDouble(wastePct * 100.0)}%)", dynamicWaste, Icons.Default.DeleteOutline),
                        Triple("Tenaga Kerja (${formatDouble(tenagaKerjaPct * 100.0)}%)", dynamicTenagaKerja, Icons.Default.Badge),
                        Triple("Listrik (${formatDouble(listrikPct * 100.0)}%)", dynamicListrik, Icons.Default.FlashOn),
                        Triple("Maintenance Alat (${formatDouble(maintenancePct * 100.0)}%)", dynamicMaintenance, Icons.Default.Build),
                        Triple("Sisa Laba Bersih", dynamicSisaLaba, Icons.Default.MonetizationOn)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsList.forEach { (label, value, icon) ->
                            val isLaba = label.startsWith("Sisa Laba")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isLaba) Color(0x26FFEB3B) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(vertical = 6.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isLaba) Color(0xFFD84315) else colorScheme.secondary
                                    )
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isLaba) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isLaba) colorScheme.primary else colorScheme.onSurface
                                    )
                                }
                                Text(
                                    formatRupiah(value),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLaba) Color(0xFFD84315) else colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onEdit,
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("edit_order_button_${order.idOrder}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Transaksi", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Transaksi", style = MaterialTheme.typography.labelMedium)
                        }

                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = colorScheme.error
                            ),
                            modifier = Modifier.testTag("delete_order_button_${order.idOrder}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hapus Transaksi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: MUTASI MANUAL KAS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutationsTab(
    mutations: List<MutasiManualKeluarMasuk>,
    accounts: List<MasterAkunSaldo>,
    viewModel: FinanceViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val customFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color(0xFFF7F5FC),
        focusedBorderColor = colorScheme.primary,
        unfocusedBorderColor = colorScheme.outlineVariant,
        focusedLabelColor = colorScheme.primary,
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        focusedTextColor = colorScheme.onSurface,
        unfocusedTextColor = colorScheme.onSurface,
        cursorColor = colorScheme.primary
    )
    val customFieldShape = RoundedCornerShape(12.dp)
    var tanggal by remember { mutableStateOf(viewModel.getTodayString()) }
    var selectedAccountIndex by remember { mutableIntStateOf(0) }
    var selectedSourceAccountIndex by remember { mutableIntStateOf(0) }
    var selectedTargetAccountIndex by remember { mutableIntStateOf(1) }
    var mutationType by remember { mutableStateOf("Uang Keluar") } // "Uang Keluar", "Uang Masuk", "Pindah Saldo"
    var nominalText by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }

    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var showErrorAlert by remember { mutableStateOf(false) }
    var editingMutation by remember { mutableStateOf<MutasiManualKeluarMasuk?>(null) }
    var deletingMutation by remember { mutableStateOf<MutasiManualKeluarMasuk?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Form Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Input Mutasi Manual Kas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )

                    OutlinedTextField(
                        value = tanggal,
                        onValueChange = { tanggal = it },
                        label = { Text("Tanggal (YYYY-MM-DD)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_mutation_tanggal"),
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )

                    // Account Selection Dropdown
                    if (accounts.isNotEmpty()) {
                        if (mutationType == "Pindah Saldo") {
                            var sourceDropdownExpanded by remember { mutableStateOf(false) }
                            var targetDropdownExpanded by remember { mutableStateOf(false) }
                            val sourceAccount = accounts.getOrNull(selectedSourceAccountIndex) ?: accounts.first()
                            val targetAccount = accounts.getOrNull(selectedTargetAccountIndex) ?: accounts.getOrNull(1) ?: accounts.first()

                            // Dompet Asal
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = sourceAccount.namaAkun,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Pilih Dompet Asal (Dikurangi)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { sourceDropdownExpanded = true }
                                        .testTag("input_mutation_asal"),
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    trailingIcon = {
                                        IconButton(onClick = { sourceDropdownExpanded = !sourceDropdownExpanded }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    colors = customFieldColors,
                                    shape = customFieldShape
                                )

                                DropdownMenu(
                                    expanded = sourceDropdownExpanded,
                                    onDismissRequest = { sourceDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    accounts.forEachIndexed { index, account ->
                                        DropdownMenuItem(
                                            text = { Text(account.namaAkun) },
                                            onClick = {
                                                selectedSourceAccountIndex = index
                                                sourceDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Dompet Tujuan
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = targetAccount.namaAkun,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Pilih Dompet Tujuan (Ditambah)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { targetDropdownExpanded = true }
                                        .testTag("input_mutation_tujuan"),
                                    leadingIcon = { Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    trailingIcon = {
                                        IconButton(onClick = { targetDropdownExpanded = !targetDropdownExpanded }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    colors = customFieldColors,
                                    shape = customFieldShape
                                )

                                DropdownMenu(
                                    expanded = targetDropdownExpanded,
                                    onDismissRequest = { targetDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    accounts.forEachIndexed { index, account ->
                                        DropdownMenuItem(
                                            text = { Text(account.namaAkun) },
                                            onClick = {
                                                selectedTargetAccountIndex = index
                                                targetDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            val currentAccount = accounts.getOrNull(selectedAccountIndex) ?: accounts.first()
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = currentAccount.namaAkun,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Pilih Pos Akun Saldo") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { accountDropdownExpanded = true }
                                        .testTag("input_mutation_akun"),
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    trailingIcon = {
                                        IconButton(onClick = { accountDropdownExpanded = !accountDropdownExpanded }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    colors = customFieldColors,
                                    shape = customFieldShape
                                )

                                DropdownMenu(
                                    expanded = accountDropdownExpanded,
                                    onDismissRequest = { accountDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    accounts.forEachIndexed { index, account ->
                                        DropdownMenuItem(
                                            text = { Text(account.namaAkun) },
                                            onClick = {
                                                selectedAccountIndex = index
                                                accountDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }                    // Jenis Mutasi Selector
                    Column {
                        Text(
                            "Jenis Mutasi",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ElevatedFilterChip(
                                selected = mutationType == "Uang Keluar",
                                onClick = { mutationType = "Uang Keluar" },
                                label = { Text("Uang Keluar", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chip_mutasi_keluar"),
                                leadingIcon = if (mutationType == "Uang Keluar") {
                                    { Icon(Icons.Default.RemoveCircle, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                            ElevatedFilterChip(
                                selected = mutationType == "Uang Masuk",
                                onClick = { mutationType = "Uang Masuk" },
                                label = { Text("Uang Masuk", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chip_mutasi_masuk"),
                                leadingIcon = if (mutationType == "Uang Masuk") {
                                    { Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                            ElevatedFilterChip(
                                selected = mutationType == "Pindah Saldo",
                                onClick = { mutationType = "Pindah Saldo" },
                                label = { Text("Pindah Saldo", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chip_mutasi_transfer"),
                                leadingIcon = if (mutationType == "Pindah Saldo") {
                                    { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    OutlinedTextField(
                        value = nominalText,
                        onValueChange = { nominalText = it },
                        label = { Text("Nominal Mutasi (Rp)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_mutation_nominal"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = keterangan,
                        onValueChange = { keterangan = it },
                        label = { Text("Keterangan Mutasi") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_mutation_keterangan"),
                        placeholder = { Text("Contoh: Beli kertas eceran, Pindah sisa laba ke kas") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = false,
                        maxLines = 2
                    )

                    if (showErrorAlert) {
                        val errText = if (mutationType == "Pindah Saldo" && selectedSourceAccountIndex == selectedTargetAccountIndex) {
                            "Dompet asal dan tujuan tidak boleh sama!"
                        } else {
                            "Harap isi nominal angka dengan valid dan keterangan!"
                        }
                        Text(
                            text = errText,
                            color = colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val nominal = parseDoubleInput(nominalText)
                            if (mutationType == "Pindah Saldo") {
                                val sourceAccount = accounts.getOrNull(selectedSourceAccountIndex)
                                val targetAccount = accounts.getOrNull(selectedTargetAccountIndex)
                                if (sourceAccount != null && targetAccount != null && nominal != null && nominal > 0.0 && keterangan.isNotBlank()) {
                                    if (sourceAccount.idAkun == targetAccount.idAkun) {
                                        showErrorAlert = true
                                        return@Button
                                    }
                                    viewModel.insertMutation(
                                        tanggal = tanggal,
                                        idAkun = sourceAccount.idAkun,
                                        jenis = "Pindah Saldo",
                                        nominal = nominal,
                                        keterangan = keterangan,
                                        idAkunTujuan = targetAccount.idAkun
                                    )
                                    // Clear Form
                                    nominalText = ""
                                    keterangan = ""
                                    showErrorAlert = false
                                } else {
                                    showErrorAlert = true
                                }
                            } else {
                                val selectedAccount = accounts.getOrNull(selectedAccountIndex)
                                if (selectedAccount != null && nominal != null && nominal > 0.0 && keterangan.isNotBlank()) {
                                    viewModel.insertMutation(
                                        tanggal = tanggal,
                                        idAkun = selectedAccount.idAkun,
                                        jenis = mutationType,
                                        nominal = nominal,
                                        keterangan = keterangan
                                    )
                                    // Clear Form
                                    nominalText = ""
                                    keterangan = ""
                                    showErrorAlert = false
                                } else {
                                    showErrorAlert = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_mutation_button"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Mutasi Penyesuaian", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Mutasi Manual Kas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                )
                Box(
                    modifier = Modifier
                        .background(colorScheme.secondaryContainer, RoundedCornerShape(100.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${mutations.size} Total",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (mutations.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Belum ada mutasi kas manual.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.secondary
                            )
                        }
                    }
                }
            }
        } else {
            items(mutations, key = { it.idMutasi }) { mutation ->
                val associatedAccount = accounts.find { it.idAkun == mutation.idAkun }?.namaAkun ?: "Akun Tidak Dikenal"
                val targetAccount = if (mutation.jenisMutasi == "Pindah Saldo") {
                    accounts.find { it.idAkun == mutation.idAkunTujuan }?.namaAkun ?: "Akun Tidak Dikenal"
                } else null
                MutationHistoryCard(
                    mutation = mutation,
                    accountName = associatedAccount,
                    targetAccountName = targetAccount,
                    onEdit = { editingMutation = mutation },
                    onDelete = { deletingMutation = mutation }
                )
            }
        }
    }

    if (editingMutation != null) {
        EditMutationDialog(
            mutation = editingMutation!!,
            accounts = accounts,
            onDismiss = { editingMutation = null },
            onSave = { updatedMutation ->
                viewModel.updateMutation(updatedMutation)
                editingMutation = null
            }
        )
    }

    if (deletingMutation != null) {
        val mutToDelete = deletingMutation!!
        AlertDialog(
            onDismissRequest = { deletingMutation = null },
            title = {
                Text(
                    "Hapus Mutasi Kas",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text("Apakah Anda yakin ingin menghapus mutasi kas Rp ${formatRupiah(mutToDelete.nominal)} (${mutToDelete.keterangan})?\nData akan dihapus dari database lokal Room dan Cloud Firestore secara real-time.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMutation(mutToDelete)
                        deletingMutation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hapus", color = colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMutation = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun MutationHistoryCard(
    mutation: MutasiManualKeluarMasuk,
    accountName: String,
    targetAccountName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isMasuk = mutation.jenisMutasi == "Uang Masuk"
    val isTransfer = mutation.jenisMutasi == "Pindah Saldo"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        border = getAuroraBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            isMasuk -> Icons.Default.AddCircle
                            isTransfer -> Icons.Default.SwapHoriz
                            else -> Icons.Default.RemoveCircle
                        },
                        contentDescription = null,
                        tint = when {
                            isMasuk -> Color(0xFF2E7D32)
                            isTransfer -> colorScheme.primary
                            else -> Color(0xFFC62828)
                        },
                        modifier = Modifier.size(24.dp)
                    )

                    Column {
                        val displayText = if (isTransfer) {
                            "${accountName.uppercase().replace("DOMPET ", "")} ➔ ${targetAccountName?.uppercase()?.replace("DOMPET ", "")}"
                        } else {
                            accountName.uppercase().replace("DOMPET ", "")
                        }
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${mutation.tanggalMutasi} • ${mutation.waktuMutasi}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.outline
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when {
                            isMasuk -> "+" + formatRupiah(mutation.nominal)
                            isTransfer -> formatRupiah(mutation.nominal)
                            else -> "-" + formatRupiah(mutation.nominal)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isMasuk -> Color(0xFF2E7D32)
                            isTransfer -> colorScheme.primary
                            else -> Color(0xFFC62828)
                        }
                    )

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("button_edit_mutation_${mutation.idMutasi}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Mutasi",
                            tint = colorScheme.primary.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("button_delete_mutation_${mutation.idMutasi}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus Mutasi",
                            tint = colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = if (isTransfer) "Transfer: ${mutation.keterangan}" else mutation.keterangan,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMutationDialog(
    mutation: MutasiManualKeluarMasuk,
    accounts: List<MasterAkunSaldo>,
    onDismiss: () -> Unit,
    onSave: (MutasiManualKeluarMasuk) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val customFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color(0xFFF7F5FC),
        focusedBorderColor = colorScheme.primary,
        unfocusedBorderColor = colorScheme.outlineVariant,
        focusedLabelColor = colorScheme.primary,
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        focusedTextColor = colorScheme.onSurface,
        unfocusedTextColor = colorScheme.onSurface,
        cursorColor = colorScheme.primary
    )
    val customFieldShape = RoundedCornerShape(12.dp)

    var editTanggal by remember { mutableStateOf(mutation.tanggalMutasi) }
    var editWaktu by remember { mutableStateOf(mutation.waktuMutasi) }
    var editJenis by remember { mutableStateOf(mutation.jenisMutasi) }
    var editNominalText by remember { mutableStateOf(if (mutation.nominal > 0) formatAngka(mutation.nominal) else "") }
    var editKeterangan by remember { mutableStateOf(mutation.keterangan) }

    var selectedSourceIdx by remember {
        mutableIntStateOf(accounts.indexOfFirst { it.idAkun == mutation.idAkun }.coerceAtLeast(0))
    }
    var selectedTargetIdx by remember {
        mutableIntStateOf(accounts.indexOfFirst { it.idAkun == mutation.idAkunTujuan }.coerceAtLeast(0))
    }

    var sourceExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = colorScheme.primary)
                Text("Edit Mutasi Kas", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTanggal,
                        onValueChange = { editTanggal = it },
                        label = { Text("Tanggal (YYYY-MM-DD)") },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("edit_mutation_tanggal"),
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editWaktu,
                        onValueChange = { editWaktu = it },
                        label = { Text("Waktu") },
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("edit_mutation_waktu"),
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )
                }

                Column {
                    Text(
                        "Jenis Mutasi",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ElevatedFilterChip(
                            selected = editJenis == "Uang Keluar",
                            onClick = { editJenis = "Uang Keluar" },
                            label = { Text("Keluar", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (editJenis == "Uang Keluar") {
                                { Icon(Icons.Default.RemoveCircle, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(12.dp)) }
                            } else null
                        )
                        ElevatedFilterChip(
                            selected = editJenis == "Uang Masuk",
                            onClick = { editJenis = "Uang Masuk" },
                            label = { Text("Masuk", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (editJenis == "Uang Masuk") {
                                { Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp)) }
                            } else null
                        )
                        ElevatedFilterChip(
                            selected = editJenis == "Pindah Saldo",
                            onClick = { editJenis = "Pindah Saldo" },
                            label = { Text("Pindah", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (editJenis == "Pindah Saldo") {
                                { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(12.dp)) }
                            } else null
                        )
                    }
                }

                if (accounts.isNotEmpty()) {
                    if (editJenis == "Pindah Saldo") {
                        val srcAcc = accounts.getOrNull(selectedSourceIdx) ?: accounts.first()
                        val tgtAcc = accounts.getOrNull(selectedTargetIdx) ?: accounts.getOrNull(1) ?: accounts.first()

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = srcAcc.namaAkun,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Dompet Asal (Dikurangi)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { sourceExpanded = true },
                                trailingIcon = {
                                    IconButton(onClick = { sourceExpanded = !sourceExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                colors = customFieldColors,
                                shape = customFieldShape
                            )
                            DropdownMenu(
                                expanded = sourceExpanded,
                                onDismissRequest = { sourceExpanded = false }
                            ) {
                                accounts.forEachIndexed { idx, acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc.namaAkun) },
                                        onClick = {
                                            selectedSourceIdx = idx
                                            sourceExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = tgtAcc.namaAkun,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Dompet Tujuan (Ditambah)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { targetExpanded = true },
                                trailingIcon = {
                                    IconButton(onClick = { targetExpanded = !targetExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                colors = customFieldColors,
                                shape = customFieldShape
                            )
                            DropdownMenu(
                                expanded = targetExpanded,
                                onDismissRequest = { targetExpanded = false }
                            ) {
                                accounts.forEachIndexed { idx, acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc.namaAkun) },
                                        onClick = {
                                            selectedTargetIdx = idx
                                            targetExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        val currAcc = accounts.getOrNull(selectedSourceIdx) ?: accounts.first()
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = currAcc.namaAkun,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Pos Akun Saldo") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { accountExpanded = true },
                                trailingIcon = {
                                    IconButton(onClick = { accountExpanded = !accountExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                colors = customFieldColors,
                                shape = customFieldShape
                            )
                            DropdownMenu(
                                expanded = accountExpanded,
                                onDismissRequest = { accountExpanded = false }
                            ) {
                                accounts.forEachIndexed { idx, acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc.namaAkun) },
                                        onClick = {
                                            selectedSourceIdx = idx
                                            accountExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = editNominalText,
                    onValueChange = { editNominalText = it },
                    label = { Text("Nominal Mutasi (Rp)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_mutation_nominal"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = customFieldColors,
                    shape = customFieldShape,
                    singleLine = true
                )

                OutlinedTextField(
                    value = editKeterangan,
                    onValueChange = { editKeterangan = it },
                    label = { Text("Keterangan Mutasi") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_mutation_keterangan"),
                    colors = customFieldColors,
                    shape = customFieldShape,
                    singleLine = false,
                    maxLines = 2
                )

                if (showError) {
                    val errText = if (editJenis == "Pindah Saldo" && selectedSourceIdx == selectedTargetIdx) {
                        "Dompet asal dan tujuan tidak boleh sama!"
                    } else {
                        "Harap isi nominal angka yang valid dan keterangan!"
                    }
                    Text(
                        text = errText,
                        color = colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nominal = parseDoubleInput(editNominalText)
                    val validAccount = accounts.isNotEmpty()
                    val validTransfer = editJenis != "Pindah Saldo" || selectedSourceIdx != selectedTargetIdx

                    if (validAccount && nominal != null && nominal > 0.0 && editKeterangan.isNotBlank() && validTransfer) {
                        val srcAcc = accounts.getOrNull(selectedSourceIdx) ?: accounts.first()
                        val tgtAcc = if (editJenis == "Pindah Saldo") (accounts.getOrNull(selectedTargetIdx) ?: accounts.first()) else null

                        val updated = mutation.copy(
                            tanggalMutasi = editTanggal,
                            waktuMutasi = editWaktu,
                            idAkun = srcAcc.idAkun,
                            jenisMutasi = editJenis,
                            nominal = nominal,
                            keterangan = editKeterangan,
                            idAkunTujuan = tgtAcc?.idAkun
                        )
                        onSave(updated)
                    } else {
                        showError = true
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// ==========================================
// TAB 4: MASTER DATA MANAGEMENT
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataTab(viewModel: FinanceViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val customFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color(0xFFF7F5FC),
        focusedBorderColor = colorScheme.primary,
        unfocusedBorderColor = colorScheme.outlineVariant,
        focusedLabelColor = colorScheme.primary,
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        focusedTextColor = colorScheme.onSurface,
        unfocusedTextColor = colorScheme.onSurface,
        cursorColor = colorScheme.primary
    )
    val customFieldShape = RoundedCornerShape(12.dp)

    // Internal state for navigation
    var activeSubTab by remember { mutableStateOf<String?>(null) }

    // Master data from ViewModel
    val pelangganList by viewModel.allPelanggan.collectAsStateWithLifecycle(emptyList())
    val satuanHargaList by viewModel.allSatuanHarga.collectAsStateWithLifecycle(emptyList())

    // Pelanggan form states
    var namaPelanggan by remember { mutableStateOf("") }
    var kontakPelanggan by remember { mutableStateOf("") }
    var instansiPelanggan by remember { mutableStateOf("") }
    var alamatInstansiPelanggan by remember { mutableStateOf("") }
    var npwpPelanggan by remember { mutableStateOf("") }

    // Satuan form states
    var namaSatuan by remember { mutableStateOf("") }
    var opsiHargaDefaultText by remember { mutableStateOf("") }

    // Edit states for Pelanggan Dialog
    var editingPelanggan by remember { mutableStateOf<MasterPelanggan?>(null) }
    var editNamaPelanggan by remember { mutableStateOf("") }
    var editKontakPelanggan by remember { mutableStateOf("") }
    var editInstansiPelanggan by remember { mutableStateOf("") }
    var editAlamatInstansiPelanggan by remember { mutableStateOf("") }
    var editNpwpPelanggan by remember { mutableStateOf("") }

    // Edit states for SatuanHarga Dialog
    var editingSatuanHarga by remember { mutableStateOf<MasterSatuanHarga?>(null) }
    var editNamaSatuan by remember { mutableStateOf("") }
    var editOpsiHargaDefaultText by remember { mutableStateOf("") }

    // Edit Pelanggan Dialog
    if (editingPelanggan != null) {
        AlertDialog(
            onDismissRequest = { editingPelanggan = null },
            title = { Text("Edit Pelanggan", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = editNamaPelanggan,
                        onValueChange = { editNamaPelanggan = it },
                        label = { Text("Nama Pelanggan") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_pelanggan_nama"),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editKontakPelanggan,
                        onValueChange = { editKontakPelanggan = it },
                        label = { Text("Kontak / WhatsApp (Opsional)") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_pelanggan_kontak"),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editInstansiPelanggan,
                        onValueChange = { editInstansiPelanggan = it },
                        label = { Text("Nama Instansi / Perusahaan (Opsional)") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_pelanggan_instansi"),
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editAlamatInstansiPelanggan,
                        onValueChange = { editAlamatInstansiPelanggan = it },
                        label = { Text("Alamat Instansi / Perusahaan (Opsional)") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_pelanggan_alamat_instansi"),
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editNpwpPelanggan,
                        onValueChange = { editNpwpPelanggan = it },
                        label = { Text("NPWP (Opsional)") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_pelanggan_npwp"),
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = editingPelanggan
                        if (current != null && editNamaPelanggan.isNotBlank()) {
                            viewModel.updatePelanggan(
                                current.copy(
                                    namaPelanggan = editNamaPelanggan.trim(),
                                    kontak = if (editKontakPelanggan.isBlank()) null else editKontakPelanggan.trim(),
                                    instansi = if (editInstansiPelanggan.isBlank()) null else editInstansiPelanggan.trim(),
                                    alamatInstansi = if (editAlamatInstansiPelanggan.isBlank()) null else editAlamatInstansiPelanggan.trim(),
                                    npwp = if (editNpwpPelanggan.isBlank()) null else editNpwpPelanggan.trim()
                                )
                            )
                            editingPelanggan = null
                        }
                    },
                    modifier = Modifier.testTag("edit_pelanggan_submit")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingPelanggan = null },
                    modifier = Modifier.testTag("edit_pelanggan_dismiss")
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit SatuanHarga Dialog
    if (editingSatuanHarga != null) {
        AlertDialog(
            onDismissRequest = { editingSatuanHarga = null },
            title = { Text("Edit Jenis Paket & Harga", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = editNamaSatuan,
                        onValueChange = { editNamaSatuan = it },
                        label = { Text("Nama Satuan/Paket") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_satuan_nama"),
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editOpsiHargaDefaultText,
                        onValueChange = { editOpsiHargaDefaultText = it },
                        label = { Text("Harga Default (Rp)") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_satuan_harga"),
                        leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = editingSatuanHarga
                        val newPrice = parseDoubleInput(editOpsiHargaDefaultText)
                        if (current != null && editNamaSatuan.isNotBlank() && newPrice != null && newPrice >= 0) {
                            viewModel.updateSatuanHarga(
                                current.copy(
                                    namaSatuan = editNamaSatuan.trim(),
                                    opsiHargaDefault = newPrice
                                )
                            )
                            editingSatuanHarga = null
                        }
                    },
                    modifier = Modifier.testTag("edit_satuan_submit")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingSatuanHarga = null },
                    modifier = Modifier.testTag("edit_satuan_dismiss")
                ) {
                    Text("Batal")
                }
            }
        )
    }

    Crossfade(targetState = activeSubTab, label = "master_data_tab_fade") { subTab ->
        when (subTab) {
            null -> {
                // HUB TAMPILAN UTAMA (MENU GRID)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section header
                    item {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(
                                text = "Grup Master Data PGD Order",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Kelola referensi pelanggan dan opsi harga default untuk form order secara terpusat.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Grid Menus
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Menu 1: Master Pelanggan
                            Card(
                                onClick = { activeSubTab = "pelanggan" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("menu_master_pelanggan"),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = colorScheme.primary,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = colorScheme.onPrimary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Master Pelanggan",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Kelola data kontak & identitas pelanggan",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier.wrapContentSize()
                                        ) {
                                            Text(
                                                text = "${pelangganList.size} Terdaftar",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Menu 2: Master Jenis Paket & Harga
                            Card(
                                onClick = { activeSubTab = "paket" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("menu_master_paket"),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = colorScheme.secondary,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalOffer,
                                                contentDescription = null,
                                                tint = colorScheme.onSecondary,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Master Jenis Paket & Harga",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Kelola opsi harga satuan default",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier.wrapContentSize()
                                        ) {
                                            Text(
                                                text = "${satuanHargaList.size} Paket Aktif",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "pelanggan" -> {
                // SUB-HALAMAN: MASTER PELANGGAN
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Back and Header Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { activeSubTab = null },
                                modifier = Modifier.testTag("back_from_pelanggan")
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Master Pelanggan",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onBackground
                                )
                                Text(
                                    text = "Tambah & kelola referensi pelanggan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Form Tambah Pelanggan
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Tambah Pelanggan Baru",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )

                                OutlinedTextField(
                                    value = namaPelanggan,
                                    onValueChange = { namaPelanggan = it },
                                    label = { Text("Nama Pelanggan") },
                                    modifier = Modifier.fillMaxWidth().testTag("master_pelanggan_nama"),
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    colors = customFieldColors,
                                    shape = customFieldShape,
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = kontakPelanggan,
                                    onValueChange = { kontakPelanggan = it },
                                    label = { Text("Kontak / WhatsApp (Opsional)") },
                                    placeholder = { Text("Contoh: 0812345678") },
                                    modifier = Modifier.fillMaxWidth().testTag("master_pelanggan_kontak"),
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    colors = customFieldColors,
                                    shape = customFieldShape,
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = instansiPelanggan,
                                    onValueChange = { instansiPelanggan = it },
                                    label = { Text("Nama Instansi / Perusahaan (Opsional)") },
                                    placeholder = { Text("Contoh: PT Angin Ribut") },
                                    modifier = Modifier.fillMaxWidth().testTag("master_pelanggan_instansi"),
                                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    colors = customFieldColors,
                                    shape = customFieldShape,
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = alamatInstansiPelanggan,
                                    onValueChange = { alamatInstansiPelanggan = it },
                                    label = { Text("Alamat Instansi / Perusahaan (Opsional)") },
                                    placeholder = { Text("Contoh: Jl. Sudirman No. 123") },
                                    modifier = Modifier.fillMaxWidth().testTag("master_pelanggan_alamat_instansi"),
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    colors = customFieldColors,
                                    shape = customFieldShape,
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = npwpPelanggan,
                                    onValueChange = { npwpPelanggan = it },
                                    label = { Text("NPWP (Opsional)") },
                                    placeholder = { Text("Contoh: 01.234.567.8-901.234") },
                                    modifier = Modifier.fillMaxWidth().testTag("master_pelanggan_npwp"),
                                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    colors = customFieldColors,
                                    shape = customFieldShape,
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        if (namaPelanggan.isNotBlank()) {
                                            viewModel.insertPelanggan(
                                                nama = namaPelanggan.trim(),
                                                kontak = if (kontakPelanggan.isBlank()) null else kontakPelanggan.trim(),
                                                instansi = if (instansiPelanggan.isBlank()) null else instansiPelanggan.trim(),
                                                alamatInstansi = if (alamatInstansiPelanggan.isBlank()) null else alamatInstansiPelanggan.trim(),
                                                npwp = if (npwpPelanggan.isBlank()) null else npwpPelanggan.trim()
                                            )
                                            namaPelanggan = ""
                                            kontakPelanggan = ""
                                            instansiPelanggan = ""
                                            alamatInstansiPelanggan = ""
                                            npwpPelanggan = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("master_pelanggan_submit"),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tambah Pelanggan")
                                }
                            }
                        }
                    }

                    // Daftar Pelanggan
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pelanggan Terdaftar (${pelangganList.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                    Surface(
                                        color = Color(0xFFF3E8FF),
                                        shape = RoundedCornerShape(50),
                                    ) {
                                        Text(
                                            text = "${pelangganList.size} Total",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6B46C1),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                if (pelangganList.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Belum ada data pelanggan.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.outline
                                        )
                                    }
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        pelangganList.forEachIndexed { index, pelanggan ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7FF)),
                                                border = BorderStroke(1.dp, Color(0xFFE4DAF7))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .background(Color(0xFFF3E8FF), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = pelanggan.namaPelanggan.take(2).uppercase(),
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color(0xFF6B46C1)
                                                            )
                                                        }

                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = pelanggan.namaPelanggan,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF1E192B)
                                                            )
                                                            if (!pelanggan.kontak.isNullOrBlank()) {
                                                                Text(
                                                                    text = "Kontak: ${pelanggan.kontak}",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = Color(0xFF5A5270)
                                                                )
                                                            }
                                                            if (!pelanggan.instansi.isNullOrBlank() || !pelanggan.alamatInstansi.isNullOrBlank() || !pelanggan.npwp.isNullOrBlank()) {
                                                                var infoText = ""
                                                                if (!pelanggan.instansi.isNullOrBlank()) infoText += pelanggan.instansi
                                                                if (!pelanggan.alamatInstansi.isNullOrBlank()) {
                                                                    if (infoText.isNotEmpty()) infoText += " • "
                                                                    infoText += pelanggan.alamatInstansi
                                                                }
                                                                if (!pelanggan.npwp.isNullOrBlank()) {
                                                                    if (infoText.isNotEmpty()) infoText += " • "
                                                                    infoText += "NPWP: ${pelanggan.npwp}"
                                                                }
                                                                Text(
                                                                    text = infoText,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = Color(0xFF5A5270).copy(alpha = 0.85f),
                                                                    maxLines = 2,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                editingPelanggan = pelanggan
                                                                editNamaPelanggan = pelanggan.namaPelanggan
                                                                editKontakPelanggan = pelanggan.kontak ?: ""
                                                                editInstansiPelanggan = pelanggan.instansi ?: ""
                                                                editAlamatInstansiPelanggan = pelanggan.alamatInstansi ?: ""
                                                                editNpwpPelanggan = pelanggan.npwp ?: ""
                                                            },
                                                            modifier = Modifier.size(36.dp).testTag("edit_pelanggan_btn_$index")
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Edit,
                                                                contentDescription = "Edit",
                                                                tint = Color(0xFF6B46C1),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = { viewModel.deletePelanggan(pelanggan) },
                                                            modifier = Modifier.size(36.dp).testTag("delete_pelanggan_btn_$index")
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Hapus",
                                                                tint = Color(0xFFC62828),
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
                        }
                    }
                }
            }
            "paket" -> {
                // SUB-HALAMAN: MASTER JENIS PAKET & HARGA
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Back and Header Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { activeSubTab = null },
                                modifier = Modifier.testTag("back_from_paket")
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Master Jenis Paket & Harga",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onBackground
                                )
                                Text(
                                    text = "Tambah & kelola opsi harga satuan default",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Form Tambah Satuan
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Tambah Jenis Paket Baru",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )

                                OutlinedTextField(
                                    value = namaSatuan,
                                    onValueChange = { namaSatuan = it },
                                    label = { Text("Nama Satuan (misal: Buku, Paket, Lembar)") },
                                    modifier = Modifier.fillMaxWidth().testTag("master_satuan_nama"),
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    colors = customFieldColors,
                                    shape = customFieldShape,
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = opsiHargaDefaultText,
                                    onValueChange = { opsiHargaDefaultText = it },
                                    label = { Text("Harga Default (Rp)") },
                                    modifier = Modifier.fillMaxWidth().testTag("master_satuan_harga"),
                                    leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = customFieldColors,
                                    shape = customFieldShape,
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        val defaultHarga = parseDoubleInput(opsiHargaDefaultText)
                                        if (namaSatuan.isNotBlank() && defaultHarga != null && defaultHarga >= 0) {
                                            viewModel.insertSatuanHarga(namaSatuan.trim(), defaultHarga)
                                            namaSatuan = ""
                                            opsiHargaDefaultText = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("master_satuan_submit"),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tambah Satuan Harga")
                                }
                            }
                        }
                    }

                    // Daftar Satuan Harga
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Paket Aktif (${satuanHargaList.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                if (satuanHargaList.isEmpty()) {
                                    Text(
                                        text = "Belum ada data satuan harga.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.outline,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    satuanHargaList.forEachIndexed { index, sHarga ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sHarga.namaSatuan,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Harga Default: ${formatRupiah(sHarga.opsiHargaDefault)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        editingSatuanHarga = sHarga
                                                        editNamaSatuan = sHarga.namaSatuan
                                                        editOpsiHargaDefaultText = String.format(Locale.US, "%.0f", sHarga.opsiHargaDefault)
                                                    },
                                                    modifier = Modifier.size(36.dp).testTag("edit_satuan_btn_$index")
                                                ) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = colorScheme.primary.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteSatuanHarga(sHarga) },
                                                    modifier = Modifier.size(36.dp).testTag("delete_satuan_btn_$index")
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Hapus",
                                                        tint = colorScheme.error.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (index < satuanHargaList.lastIndex) {
                                            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerSaldoPosCard(
    summary: DashboardSummary,
    onAccountClick: (AccountDashboardRow) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = colorScheme.primary)
                    Text(
                        text = "Ledger Saldo Pos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .background(colorScheme.primaryContainer, RoundedCornerShape(100.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "REAL-TIME",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "AKUN",
                    modifier = Modifier.weight(1.3f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.outline
                )
                Text(
                    "PLOTTING",
                    modifier = Modifier.weight(1.1f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.outline,
                    textAlign = TextAlign.End
                )
                Text(
                    "MUTASI",
                    modifier = Modifier.weight(1.0f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.outline,
                    textAlign = TextAlign.End
                )
                Text(
                    "RIIL",
                    modifier = Modifier.weight(1.3f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.outline,
                    textAlign = TextAlign.End
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = colorScheme.outlineVariant)

            // Rows Mapping
            summary.rows.forEach { row ->
                val isSisaLaba = row.namaAkun == "Sisa Laba" || row.namaAkun == "Dompet Laba Bersih"
                val cleanAccountName = row.namaAkun.replace("Dompet ", "", ignoreCase = true)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSisaLaba) SisaLabaBg.copy(alpha = 0.5f) else Color.Transparent
                        )
                        .clickable { onAccountClick(row) }
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cleanAccountName.uppercase(),
                        modifier = Modifier.weight(1.3f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSisaLaba) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSisaLaba) SisaLabaText else colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatRupiah(row.saldoTerplotting),
                        modifier = Modifier.weight(1.1f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSisaLaba) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSisaLaba) SisaLabaText else colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                    
                    val mutasiText = if (row.mutasiPenyesuain == 0.0) {
                        "0"
                    } else if (row.mutasiPenyesuain > 0.0) {
                        "+" + formatRupiah(row.mutasiPenyesuain)
                    } else {
                        "-" + formatRupiah(-row.mutasiPenyesuain)
                    }
                    val mutasiColor = if (row.mutasiPenyesuain > 0.0) {
                        Color(0xFF2E7D32)
                    } else if (row.mutasiPenyesuain < 0.0) {
                        Color(0xFFC62828)
                    } else {
                        colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                    Text(
                        text = mutasiText,
                        modifier = Modifier.weight(1.0f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSisaLaba) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSisaLaba) SisaLabaText else mutasiColor,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = formatRupiah(row.sisaSaldoRiil),
                        modifier = Modifier.weight(1.3f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isSisaLaba) Color(0xFFBF360C) else colorScheme.onSurface,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun DetailLedgerDialog(
    account: AccountDashboardRow,
    viewModel: FinanceViewModel,
    showQuickActions: Boolean = true,
    onDismiss: () -> Unit
) {
    val allMutations by viewModel.allMutations.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()

    val cleanAccountName = remember(account.namaAkun) {
        account.namaAkun.replace("Dompet ", "", ignoreCase = true)
    }

    val isMeGps = remember(cleanAccountName) {
        cleanAccountName.equals("Me GpS", ignoreCase = true)
    }

    val accountMutations = remember(allMutations, account.idAkun) {
        allMutations.filter { it.idAkun == account.idAkun || it.idAkunTujuan == account.idAkun }
            .sortedByDescending { "${it.tanggalMutasi} ${it.waktuMutasi}" }
    }

    val totalPengeluaran = remember(accountMutations, account.idAkun) {
        accountMutations.filter { it.jenisMutasi == "Uang Keluar" && it.idAkun == account.idAkun }
            .sumOf { it.nominal }
    }

    val lunasOrders = remember(allOrders) {
        allOrders.filter { it.status == "Lunas" }
    }

    var selectedDetailTab by remember { mutableIntStateOf(0) }
    var quickActionType by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFFF3E8FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMeGps) Icons.Default.GpsFixed else Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFF6B46C1),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isMeGps) "Kantong Independen" else "Detail Ledger Pos",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF5A5270)
                            )
                            Text(
                                text = cleanAccountName.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF553C9A)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF5A5270))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Card (High Contrast & Clean Border)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7FF)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE4DAF7))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Saldo Terplotting",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF5A5270)
                                )
                                Text(
                                    text = if (isMeGps) "Rp 0 (Independen)" else formatRupiah(account.saldoTerplotting),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E192B)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Pengeluaran",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFC62828)
                                )
                                Text(
                                    text = formatRupiah(totalPengeluaran),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE4DAF7).copy(alpha = 0.8f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Mutasi Penyesuaian",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF5A5270)
                                )
                                val mutText = if (account.mutasiPenyesuain >= 0) "+${formatRupiah(account.mutasiPenyesuain)}" else "-${formatRupiah(-account.mutasiPenyesuain)}"
                                Text(
                                    text = mutText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (account.mutasiPenyesuain >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Sisa Saldo Riil",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF553C9A)
                                )
                                Text(
                                    text = formatRupiah(account.sisaSaldoRiil),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (account.sisaSaldoRiil >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Access Actions (only shown if showQuickActions is true)
                if (showQuickActions) {
                    Text(
                        text = "AKSI PINTAS CEPAT POS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5A5270)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Masuk
                        Button(
                            onClick = { quickActionType = "Masuk" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_btn_masuk_${cleanAccountName.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE8F5E9),
                                contentColor = Color(0xFF2E7D32)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Masuk", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Keluar
                        Button(
                            onClick = { quickActionType = "Keluar" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_btn_keluar_${cleanAccountName.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFEBEE),
                                contentColor = Color(0xFFC62828)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.RemoveCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Keluar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Mutasi / Transfer
                        Button(
                            onClick = { quickActionType = "Transfer" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_btn_transfer_${cleanAccountName.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF3E8FF),
                                contentColor = Color(0xFF6B46C1)
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Mutasi", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (isMeGps) {
                    Text(
                        text = "RIWAYAT TRANSAKSI MANUAL (${accountMutations.size})",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5A5270)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    // Tabs: Mutasi Kas vs Alokasi Order (Slim UI)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3E8FF).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf(
                            Pair("Mutasi Kas (${accountMutations.size})", 0),
                            Pair("Alokasi Order (${lunasOrders.size})", 1)
                        )
                        tabs.forEach { (title, index) ->
                            val isSelected = selectedDetailTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSelected) Color(0xFF6B46C1) else Color.Transparent)
                                    .clickable { selectedDetailTab = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else Color(0xFF5A5270)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isMeGps || selectedDetailTab == 0) {
                    if (accountMutations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color(0xFF5A5270).copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Belum ada mutasi/pengeluaran kas untuk pos ini",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF5A5270),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(accountMutations) { mut ->
                                val isKeluar = mut.jenisMutasi == "Uang Keluar" && mut.idAkun == account.idAkun
                                val isMasuk = mut.jenisMutasi == "Uang Masuk" && mut.idAkun == account.idAkun
                                val isPindahFrom = mut.jenisMutasi == "Pindah Saldo" && mut.idAkun == account.idAkun
                                val isPindahTo = mut.jenisMutasi == "Pindah Saldo" && mut.idAkunTujuan == account.idAkun

                                val sign = if (isKeluar || isPindahFrom) "-" else "+"
                                val badgeColor = when {
                                    isKeluar -> Color(0xFFC62828)
                                    isMasuk -> Color(0xFF2E7D32)
                                    else -> Color(0xFF1565C0)
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE4DAF7))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = when {
                                                            isKeluar -> "PENGELUARAN"
                                                            isMasuk -> "MASUK"
                                                            isPindahFrom -> "TRANSFER KELUAR"
                                                            isPindahTo -> "TRANSFER MASUK"
                                                            else -> mut.jenisMutasi.uppercase()
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = badgeColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    text = "${mut.tanggalMutasi} ${mut.waktuMutasi}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF5A5270)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = mut.keterangan.ifBlank { "Mutasi Tanpa Keterangan" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF1E192B),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$sign${formatRupiah(mut.nominal)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (lunasOrders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Belum ada transaksi order lunas yang memplotting ke pos ini",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF5A5270),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val kertasHpp = allAccounts.find { it.namaAkun == "Kertas" || it.namaAkun == "Dompet Kertas" }?.konstanHppUnit?.toDouble() ?: 106.0
                        val tintaHpp = allAccounts.find { it.namaAkun == "Tinta" || it.namaAkun == "Dompet Tinta" }?.konstanHppUnit?.toDouble() ?: 25.0
                        val pengemasanHpp = allAccounts.find { it.namaAkun == "Pengemasan" || it.namaAkun == "Dompet Pengemasan" }?.konstanHppUnit?.toDouble() ?: 300.0
                        val wastePct = allAccounts.find { it.namaAkun == "Waste" || it.namaAkun == "Dompet Waste / Rusak" }?.persentaseOperasional?.toDouble() ?: 0.05
                        val tenagaKerjaPct = allAccounts.find { it.namaAkun == "Tenaga Kerja" || it.namaAkun == "Dompet Tenaga Kerja" }?.persentaseOperasional?.toDouble() ?: 0.07
                        val listrikPct = allAccounts.find { it.namaAkun == "Listrik" || it.namaAkun == "Dompet Listrik" }?.persentaseOperasional?.toDouble() ?: 0.02
                        val maintenancePct = allAccounts.find { it.namaAkun == "Maintenance Alat" || it.namaAkun == "Dompet Maintenance" }?.persentaseOperasional?.toDouble() ?: 0.05

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(lunasOrders) { order ->
                                val totalRevenue = order.qtyOrder.toDouble() * order.hargaSatuan
                                val allocation = when (account.namaAkun) {
                                    "Kertas", "Dompet Kertas" -> order.qtyOrder.toDouble() * kertasHpp
                                    "Tinta", "Dompet Tinta" -> order.qtyOrder.toDouble() * tintaHpp
                                    "Pengemasan", "Dompet Pengemasan" -> order.jumlahPlastikPengemasan.toDouble() * pengemasanHpp
                                    "Waste", "Dompet Waste / Rusak" -> wastePct * totalRevenue
                                    "Tenaga Kerja", "Dompet Tenaga Kerja" -> tenagaKerjaPct * totalRevenue
                                    "Listrik", "Dompet Listrik" -> listrikPct * totalRevenue
                                    "Maintenance Alat", "Dompet Maintenance" -> maintenancePct * totalRevenue
                                    "Sisa Laba", "Dompet Laba Bersih" -> {
                                        val kVal = order.qtyOrder.toDouble() * kertasHpp
                                        val tVal = order.qtyOrder.toDouble() * tintaHpp
                                        val pVal = order.jumlahPlastikPengemasan.toDouble() * pengemasanHpp
                                        val wVal = wastePct * totalRevenue
                                        val tkVal = tenagaKerjaPct * totalRevenue
                                        val lVal = listrikPct * totalRevenue
                                        val mVal = maintenancePct * totalRevenue
                                        totalRevenue - (kVal + tVal + pVal + wVal + tkVal + lVal + mVal)
                                    }
                                    else -> 0.0
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE4DAF7))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = order.namaPesanan.ifBlank { "Order #${order.idOrder}" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E192B)
                                            )
                                            Text(
                                                text = "${order.tanggalOrder} • ${order.qtyOrder} ${order.satuan} • ${formatRupiah(order.qtyOrder.toDouble() * order.hargaSatuan)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF5A5270)
                                            )
                                        }
                                        Text(
                                            text = "+${formatRupiah(allocation)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B46C1)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (quickActionType != null) {
        var nominalInput by remember { mutableStateOf("") }
        var keteranganInput by remember { mutableStateOf("") }
        var selectedTargetAccId by remember { mutableIntStateOf(allAccounts.firstOrNull { it.idAkun != account.idAkun }?.idAkun ?: 0) }
        var targetExpanded by remember { mutableStateOf(false) }
        var quickError by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { quickActionType = null },
            title = {
                Text(
                    text = when (quickActionType) {
                        "Masuk" -> "Tambah Dana Masuk - $cleanAccountName"
                        "Keluar" -> "Catat Pengeluaran - $cleanAccountName"
                        else -> "Pindah Saldo - $cleanAccountName"
                    },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (quickError != null) {
                        Text(
                            text = quickError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (quickActionType == "Transfer") {
                        Text(
                            text = "Pos Kas Tujuan Transfer:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val targetAcc = allAccounts.find { it.idAkun == selectedTargetAccId }
                        Box {
                            OutlinedButton(
                                onClick = { targetExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(targetAcc?.namaAkun ?: "Pilih Pos Tujuan")
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = targetExpanded,
                                onDismissRequest = { targetExpanded = false }
                            ) {
                                allAccounts.filter { it.idAkun != account.idAkun }.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc.namaAkun) },
                                        onClick = {
                                            selectedTargetAccId = acc.idAkun
                                            targetExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = nominalInput,
                        onValueChange = { nominalInput = it; quickError = null },
                        label = { Text("Nominal (Rp)") },
                        placeholder = { Text("Misal: 275 atau 500.000") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_quick_action_nominal"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = keteranganInput,
                        onValueChange = { keteranganInput = it },
                        label = { Text("Keterangan Mutasi") },
                        placeholder = { Text("Catatan singkat mutasi") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_quick_action_keterangan"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = parseDoubleInput(nominalInput)
                        if (amt == null || amt <= 0.0) {
                            quickError = "Harap masukkan nominal angka yang valid (> 0)"
                            return@Button
                        }

                        val today = viewModel.getTodayString()
                        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                        when (quickActionType) {
                            "Masuk" -> {
                                viewModel.insertMutation(
                                    tanggal = today,
                                    idAkun = account.idAkun,
                                    jenis = "Uang Masuk",
                                    nominal = amt,
                                    keterangan = keteranganInput.ifBlank { "Tambah dana $cleanAccountName" },
                                    waktu = now
                                )
                                Toast.makeText(context, "Berhasil menambah dana $cleanAccountName!", Toast.LENGTH_SHORT).show()
                            }
                            "Keluar" -> {
                                viewModel.insertMutation(
                                    tanggal = today,
                                    idAkun = account.idAkun,
                                    jenis = "Uang Keluar",
                                    nominal = amt,
                                    keterangan = keteranganInput.ifBlank { "Pengeluaran $cleanAccountName" },
                                    waktu = now
                                )
                                Toast.makeText(context, "Berhasil mencatat pengeluaran $cleanAccountName!", Toast.LENGTH_SHORT).show()
                            }
                            "Transfer" -> {
                                val targetAcc = allAccounts.find { it.idAkun == selectedTargetAccId }
                                if (targetAcc == null || targetAcc.idAkun == account.idAkun) {
                                    quickError = "Pilih pos kas tujuan yang berbeda"
                                    return@Button
                                }
                                viewModel.insertMutation(
                                    tanggal = today,
                                    idAkun = account.idAkun,
                                    jenis = "Pindah Saldo",
                                    nominal = amt,
                                    keterangan = keteranganInput.ifBlank { "Mutasi $cleanAccountName ke ${targetAcc.namaAkun}" },
                                    idAkunTujuan = targetAcc.idAkun,
                                    waktu = now
                                )
                                Toast.makeText(context, "Pindah saldo ke ${targetAcc.namaAkun} berhasil!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        quickActionType = null
                    }
                ) {
                    Text("Simpan Mutasi")
                }
            },
            dismissButton = {
                TextButton(onClick = { quickActionType = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

// ==========================================
// TAB 5: PENGATURAN FINANSIAL (Financial Settings)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSettingsTab(viewModel: FinanceViewModel, summary: DashboardSummary) {
    val colorScheme = MaterialTheme.colorScheme
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle(emptyList())

    var selectedLedgerAccount by remember { mutableStateOf<AccountDashboardRow?>(null) }

    // Define states for our inputs
    var kertasHppText by remember { mutableStateOf("") }
    var tintaHppText by remember { mutableStateOf("") }
    var pengemasanHppText by remember { mutableStateOf("") }
    var wastePctText by remember { mutableStateOf("") }
    var tenagaKerjaPctText by remember { mutableStateOf("") }
    var listrikPctText by remember { mutableStateOf("") }
    var maintenancePctText by remember { mutableStateOf("") }

    // When accounts list loads, populate form inputs
    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty()) {
            val kertas = accounts.find { it.namaAkun == "Kertas" || it.namaAkun == "Dompet Kertas" }
            val tinta = accounts.find { it.namaAkun == "Tinta" || it.namaAkun == "Dompet Tinta" }
            val pengemasan = accounts.find { it.namaAkun == "Pengemasan" || it.namaAkun == "Dompet Pengemasan" }
            val waste = accounts.find { it.namaAkun == "Waste" || it.namaAkun == "Dompet Waste / Rusak" }
            val tenagaKerja = accounts.find { it.namaAkun == "Tenaga Kerja" || it.namaAkun == "Dompet Tenaga Kerja" }
            val listrik = accounts.find { it.namaAkun == "Listrik" || it.namaAkun == "Dompet Listrik" }
            val maintenance = accounts.find { it.namaAkun == "Maintenance Alat" || it.namaAkun == "Dompet Maintenance" }

            if (kertasHppText.isEmpty()) kertasHppText = formatFloatValue(kertas?.konstanHppUnit ?: 106.0f)
            if (tintaHppText.isEmpty()) tintaHppText = formatFloatValue(tinta?.konstanHppUnit ?: 25.0f)
            if (pengemasanHppText.isEmpty()) {
                val pVal = pengemasan?.konstanHppUnit ?: 300.0f
                pengemasanHppText = formatFloatValue(if (pVal <= 0.0f) 300.0f else pVal)
            }
            if (wastePctText.isEmpty()) wastePctText = formatPercentValue(waste?.persentaseOperasional ?: 0.05f)
            if (tenagaKerjaPctText.isEmpty()) tenagaKerjaPctText = formatPercentValue(tenagaKerja?.persentaseOperasional ?: 0.07f)
            if (listrikPctText.isEmpty()) listrikPctText = formatPercentValue(listrik?.persentaseOperasional ?: 0.02f)
            if (maintenancePctText.isEmpty()) maintenancePctText = formatPercentValue(maintenance?.persentaseOperasional ?: 0.05f)
        }
    }

    val context = LocalContext.current
    val settingsFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SeaBlue,
        unfocusedBorderColor = colorScheme.outlineVariant,
        focusedLabelColor = SeaBlue,
        focusedLeadingIconColor = SeaBlue,
        focusedSuffixColor = SeaBlue,
        cursorColor = SeaBlue
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Pengaturan Finansial",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )
            Text(
                text = "Sesuaikan nilai default HPP dan persentase operasional untuk amplop secara global. Perubahan akan langsung memengaruhi kalkulasi pembagian amplop di Dashboard secara real-time.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        // Section: LEDGER SALDO POS (Real-time)
        item {
            LedgerSaldoPosCard(
                summary = summary,
                onAccountClick = { selectedLedgerAccount = it }
            )
        }

        // Section 1: HPP Konstan (Unit Based)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, tint = colorScheme.primary)
                        Text(
                            text = "HPP Konstan per Unit (Rupiah)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant)

                    OutlinedTextField(
                        value = kertasHppText,
                        onValueChange = { kertasHppText = it },
                        label = { Text("HPP Kertas per Lembar (Rp)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_settings_kertas"),
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = settingsFieldColors
                    )

                    OutlinedTextField(
                        value = tintaHppText,
                        onValueChange = { tintaHppText = it },
                        label = { Text("HPP Tinta per Lembar (Rp)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_settings_tinta"),
                        leadingIcon = { Icon(Icons.Default.InvertColors, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = settingsFieldColors
                    )

                    OutlinedTextField(
                        value = pengemasanHppText,
                        onValueChange = { pengemasanHppText = it },
                        label = { Text("Biaya Pengemasan per Plastik (Rp)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_settings_pengemasan"),
                        leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = settingsFieldColors
                    )
                }
            }
        }

        // Section 2: Persentase Alokasi Operasional
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = colorScheme.primary)
                        Text(
                            text = "Persentase Alokasi Operasional (%)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant)

                    OutlinedTextField(
                        value = wastePctText,
                        onValueChange = { wastePctText = it },
                        label = { Text("Persentase Dana Waste (%)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_settings_waste"),
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = settingsFieldColors
                    )

                    OutlinedTextField(
                        value = tenagaKerjaPctText,
                        onValueChange = { tenagaKerjaPctText = it },
                        label = { Text("Persentase Tenaga Kerja (%)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_settings_tenagakerja"),
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = settingsFieldColors
                    )

                    OutlinedTextField(
                        value = listrikPctText,
                        onValueChange = { listrikPctText = it },
                        label = { Text("Persentase Listrik (%)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_settings_listrik"),
                        leadingIcon = { Icon(Icons.Default.FlashOn, contentDescription = null) },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = settingsFieldColors
                    )

                    OutlinedTextField(
                        value = maintenancePctText,
                        onValueChange = { maintenancePctText = it },
                        label = { Text("Persentase Maintenance Alat (%)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_settings_maintenance"),
                        leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = settingsFieldColors
                    )
                }
            }
        }

        // Section 3: Save Button
        item {
            Button(
                onClick = {
                    val kertasHpp = parseDoubleSafe(kertasHppText, 106.0)
                    val tintaHpp = parseDoubleSafe(tintaHppText, 25.0)
                    val pengemasanHpp = parseDoubleSafe(pengemasanHppText, 300.0)
                    val wastePct = parseDoubleSafe(wastePctText, 5.0) / 100.0
                    val tenagaKerjaPct = parseDoubleSafe(tenagaKerjaPctText, 7.0) / 100.0
                    val listrikPct = parseDoubleSafe(listrikPctText, 2.0) / 100.0
                    val maintenancePct = parseDoubleSafe(maintenancePctText, 5.0) / 100.0

                    viewModel.updateFinancialSettings(
                        kertasHpp = kertasHpp,
                        tintaHpp = tintaHpp,
                        pengemasanHpp = pengemasanHpp,
                        wastePct = wastePct,
                        tenagaKerjaPct = tenagaKerjaPct,
                        listrikPct = listrikPct,
                        maintenancePct = maintenancePct
                    )
                    Toast.makeText(context, "Pengaturan finansial berhasil disimpan!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_settings_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A4C93)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simpan Pengaturan Finansial",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
        
        // Extra padding at the bottom for navigation bar clearance
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (selectedLedgerAccount != null) {
        DetailLedgerDialog(
            account = selectedLedgerAccount!!,
            viewModel = viewModel,
            showQuickActions = false,
            onDismiss = { selectedLedgerAccount = null }
        )
    }
}

private fun formatDouble(value: Double): String {
    return formatAngka(value)
}

private fun formatFloatValue(value: Float?): String {
    if (value == null) return ""
    return formatAngka(value.toDouble())
}

private fun formatPercentValue(value: Float?): String {
    if (value == null) return ""
    val pct = value * 100f
    return formatAngka(pct.toDouble())
}

private fun parseDoubleSafe(input: String, default: Double): Double {
    return parseDoubleInput(input) ?: default
}

// ==========================================
// TAB 4: LAPORAN PEMBUKUAN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanTab(viewModel: FinanceViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    var selectedSubTab by remember { mutableIntStateOf(0) }
    var selectedLedgerAccount by remember { mutableStateOf<AccountDashboardRow?>(null) }

    val startDate by viewModel.reportStartDate.collectAsStateWithLifecycle()
    val endDate by viewModel.reportEndDate.collectAsStateWithLifecycle()

    val orders by viewModel.filteredOrders.collectAsStateWithLifecycle(emptyList())
    val mutations by viewModel.filteredMutations.collectAsStateWithLifecycle(emptyList())
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle(emptyList())
    val allOrdersList by viewModel.allOrders.collectAsStateWithLifecycle(emptyList())
    val pelangganList by viewModel.allPelanggan.collectAsStateWithLifecycle(emptyList())
    val satuanHargaList by viewModel.allSatuanHarga.collectAsStateWithLifecycle(emptyList())
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val allocationSummary by viewModel.allocationComparisonSummary.collectAsStateWithLifecycle()

    val accountMap = remember(accounts) { accounts.associateBy { it.idAkun } }

    var startDateText by remember(startDate) { mutableStateOf(startDate) }
    var endDateText by remember(endDate) { mutableStateOf(endDate) }
    var exportSuccessDialogInfo by remember { mutableStateOf<ExportDialogInfo?>(null) }

    val showStartDatePickerDialog = {
        val parts = startDateText.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) - 1
        val day = parts.getOrNull(2)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        val dpd = android.app.DatePickerDialog(context, { _, y, m, d ->
            val selectedDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
            startDateText = selectedDate
            viewModel.reportStartDate.value = selectedDate
        }, year, month, day)

        // Lock to active accounting range 2020 - 2026
        val calMin = Calendar.getInstance().apply { set(2020, Calendar.JANUARY, 1, 0, 0, 0) }
        val calMax = Calendar.getInstance().apply { set(2026, Calendar.DECEMBER, 31, 23, 59, 59) }
        dpd.datePicker.minDate = calMin.timeInMillis
        dpd.datePicker.maxDate = calMax.timeInMillis
        dpd.show()
    }

    val showEndDatePickerDialog = {
        val parts = endDateText.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) - 1
        val day = parts.getOrNull(2)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        val dpd = android.app.DatePickerDialog(context, { _, y, m, d ->
            val selectedDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
            endDateText = selectedDate
            viewModel.reportEndDate.value = selectedDate
        }, year, month, day)

        // Lock to active accounting range 2020 - 2026
        val calMin = Calendar.getInstance().apply { set(2020, Calendar.JANUARY, 1, 0, 0, 0) }
        val calMax = Calendar.getInstance().apply { set(2026, Calendar.DECEMBER, 31, 23, 59, 59) }
        dpd.datePicker.minDate = calMin.timeInMillis
        dpd.datePicker.maxDate = calMax.timeInMillis
        dpd.show()
    }

    // Aggregate statistics
    val totalUnits = orders.sumOf { it.qtyOrder }
    val totalOmzet = orders.sumOf { it.qtyOrder.toDouble() * it.hargaSatuan }
    val totalMutationOut = mutations.filter { it.jenisMutasi == "Uang Keluar" }.sumOf { it.nominal }
    val totalMutationIn = mutations.filter { it.jenisMutasi == "Uang Masuk" }.sumOf { it.nominal }

    // Merge history items
    val combinedList = remember(orders, mutations, accountMap) {
        val items = mutableListOf<CombinedHistoryItem>()
        orders.forEach { order ->
            items.add(
                CombinedHistoryItem(
                    tanggal = order.tanggalOrder,
                    jenis = "Order",
                    deskripsi = "Pesanan: ${order.namaPesanan} (${order.qtyOrder} ${order.satuan})",
                    nominal = order.qtyOrder.toDouble() * order.hargaSatuan
                )
            )
        }
        mutations.forEach { mut ->
            val accountName = accountMap[mut.idAkun]?.namaAkun ?: "Pos ${mut.idAkun}"
            val prefix = if (mut.jenisMutasi == "Uang Masuk") "Mutasi Masuk" else "Mutasi Keluar"
            items.add(
                CombinedHistoryItem(
                    tanggal = mut.tanggalMutasi,
                    jenis = prefix,
                    deskripsi = "(${accountName}) ${mut.keterangan}",
                    nominal = if (mut.jenisMutasi == "Uang Masuk") mut.nominal else -mut.nominal
                )
            )
        }
        // Sort descending by date
        items.sortByDescending { it.tanggal }
        items
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = when (selectedSubTab) {
                0 -> 0
                4 -> 1
                1 -> 2
                2 -> 3
                3 -> 4
                else -> 0
            },
            containerColor = Color.Transparent,
            divider = {},
            indicator = {},
            edgePadding = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .background(colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(3.dp)
        ) {
            val tabs = listOf(
                Triple("Laporan", Icons.Default.Assessment, 0),
                Triple("Analisis Pos", Icons.Default.BarChart, 4),
                Triple("Status", Icons.Default.Payment, 1),
                Triple("Ledger", Icons.Default.AccountBalanceWallet, 2),
                Triple("Mutasi Kas", Icons.Default.SwapHoriz, 3)
            )
            tabs.forEach { (title, icon, index) ->
                val isSelected = selectedSubTab == index
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colorScheme.primary else Color.Transparent)
                        .clickable { selectedSubTab = index }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        when (selectedSubTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
        item {
            Text(
                text = "Laporan Pembukuan (2020–2026)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )
            Text(
                text = "Pantau dan ekspor laporan transaksi terplotting & penyesuaian mutasi kas riil periode 2020–2026 sesuai rentang kalender pilihan Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        // Section: Date Range Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = colorScheme.primary)
                        Text(
                            text = "Filter Rentang Kalender (2020–2026)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showStartDatePickerDialog() }
                        ) {
                            OutlinedTextField(
                                value = startDateText,
                                onValueChange = {},
                                label = { Text("Dari Tanggal") },
                                placeholder = { Text("YYYY-MM-DD") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Pilih Tanggal Mulai",
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                readOnly = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = colorScheme.onSurface,
                                    disabledBorderColor = colorScheme.outline,
                                    disabledLabelColor = colorScheme.onSurfaceVariant,
                                    disabledLeadingIconColor = colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = colorScheme.onSurfaceVariant,
                                    disabledPlaceholderColor = colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showEndDatePickerDialog() }
                        ) {
                            OutlinedTextField(
                                value = endDateText,
                                onValueChange = {},
                                label = { Text("Sampai Tanggal") },
                                placeholder = { Text("YYYY-MM-DD") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Pilih Tanggal Selesai",
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                readOnly = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = colorScheme.onSurface,
                                    disabledBorderColor = colorScheme.outline,
                                    disabledLabelColor = colorScheme.onSurfaceVariant,
                                    disabledLeadingIconColor = colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = colorScheme.onSurfaceVariant,
                                    disabledPlaceholderColor = colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Quick presets row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Preset 1: Bulan Ini
                        FilterPresetChip(
                            text = "Bulan Ini",
                            onClick = {
                                val s = viewModel.getStartOfMonthString()
                                val e = viewModel.getEndOfMonthString()
                                startDateText = s
                                endDateText = e
                                viewModel.reportStartDate.value = s
                                viewModel.reportEndDate.value = e
                            }
                        )
                        // Preset 2: Tahun 2026
                        FilterPresetChip(
                            text = "Tahun 2026",
                            onClick = {
                                val s = "2026-01-01"
                                val e = "2026-12-31"
                                startDateText = s
                                endDateText = e
                                viewModel.reportStartDate.value = s
                                viewModel.reportEndDate.value = e
                            }
                        )
                        // Preset 3: Semua (2020-2026)
                        FilterPresetChip(
                            text = "Semua (2020–2026)",
                            onClick = {
                                val s = "2020-01-01"
                                val e = "2026-12-31"
                                startDateText = s
                                endDateText = e
                                viewModel.reportStartDate.value = s
                                viewModel.reportEndDate.value = e
                            }
                        )
                        // Preset 4: 30 Hari Terakhir
                        FilterPresetChip(
                            text = "30 Hari",
                            onClick = {
                                val cal = Calendar.getInstance()
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                val todayStr = sdf.format(cal.time)
                                cal.add(Calendar.DAY_OF_MONTH, -30)
                                val thirtyDaysAgoStr = sdf.format(cal.time)
                                startDateText = thirtyDaysAgoStr
                                endDateText = todayStr
                                viewModel.reportStartDate.value = thirtyDaysAgoStr
                                viewModel.reportEndDate.value = todayStr
                            }
                        )
                    }
                }
            }
        }

        // Section: Rekapitulasi Data Performa (Date-filtered Summary Metrics in a Single Clean Card)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Bar with Icon & Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFFF3EEFA), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = Color(0xFF6A4C93),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "REKAPITULASI KINERJA KEUANGAN & PRODUKSI",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B2369)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE4DAF7).copy(alpha = 0.7f))

                    // Row 1: Produksi & Omzet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Unit Terproduksi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF554B6E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$totalUnits pcs",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF3B2369)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Total Omzet Transaksi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF554B6E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatRupiah(totalOmzet),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF166534)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF3EEFA))

                    // Row 2: Pengeluaran & Pemasukan Mutasi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Pengeluaran Mutasi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF554B6E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatRupiah(totalMutationOut),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Total Pemasukan Mutasi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF554B6E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatRupiah(totalMutationIn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                }
            }
        }

        // Section: Export buttons (Excel & PDF Only)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "EKSPOR LAPORAN",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tombol 1: EXCEL (.xlsx)
                    Button(
                        onClick = {
                            exportToExcel(context, startDate, endDate, totalUnits, totalOmzet, totalMutationOut, combinedList) { name, uri, type ->
                                exportSuccessDialogInfo = ExportDialogInfo(name, uri, type)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("export_excel_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)), // Khas Excel Green (#107C41)
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.BorderOuter,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "EXCEL (.xlsx)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    // Tombol 2: CETAK PDF
                    Button(
                        onClick = {
                            exportToPdf(context, startDate, endDate, totalUnits, totalOmzet, totalMutationOut, combinedList) { name, uri, type ->
                                exportSuccessDialogInfo = ExportDialogInfo(name, uri, type)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("export_pdf_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)), // Khas PDF Red (#C62828)
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "CETAK PDF",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        // Section: Table Title
        item {
            Text(
                text = "HISTORI TRANSAKSI GABUNGAN (${combinedList.size})",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        // Table Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TANGGAL",
                    modifier = Modifier.weight(1.2f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.outline
                )
                Text(
                    "JENIS",
                    modifier = Modifier.weight(1.2f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.outline
                )
                Text(
                    "KETERANGAN",
                    modifier = Modifier.weight(2.0f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.outline
                )
                Text(
                    "NOMINAL",
                    modifier = Modifier.weight(1.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.outline,
                    textAlign = TextAlign.End
                )
            }
        }

        // Table Rows mapping
        if (combinedList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada transaksi dalam rentang tanggal ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(combinedList) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.tanggal,
                            modifier = Modifier.weight(1.2f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurfaceVariant
                        )

                        // Badge for transaction type
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .padding(end = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = when (item.jenis) {
                                            "Order" -> Color(0xFFE8F5E9)
                                            "Mutasi Masuk" -> Color(0xFFE3F2FD)
                                            else -> Color(0xFFFFEBEE)
                                        },
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.jenis.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when (item.jenis) {
                                        "Order" -> Color(0xFF2E7D32)
                                        "Mutasi Masuk" -> Color(0xFF1565C0)
                                        else -> Color(0xFFC62828)
                                    }
                                )
                            }
                        }

                        Text(
                            text = item.deskripsi,
                            modifier = Modifier.weight(2.0f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        val nominalColor = when {
                            item.nominal > 0 -> Color(0xFF2E7D32)
                            item.nominal < 0 -> Color(0xFFC62828)
                            else -> colorScheme.onSurface
                        }
                        val prefix = if (item.nominal >= 0) "+" else ""

                        Text(
                            text = prefix + formatRupiah(item.nominal),
                            modifier = Modifier.weight(1.6f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = nominalColor,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

            1 -> {
                var statusFilter by remember { mutableStateOf("Semua") }
                val filteredList = remember(allOrdersList, statusFilter) {
                    when (statusFilter) {
                        "Lunas" -> allOrdersList.filter { it.status == "Lunas" }
                        "Belum Lunas" -> allOrdersList.filter { it.status == "Belum Lunas" }
                        else -> allOrdersList
                    }
                }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Status Transaksi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onBackground
                        )
                        Text(
                            text = "Pantau status pembayaran setiap order masuk dan perbarui pembayaran secara real-time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Semua", "Lunas", "Belum Lunas").forEach { statusLabel ->
                                ElevatedFilterChip(
                                    selected = statusFilter == statusLabel,
                                    onClick = { statusFilter = statusLabel },
                                    label = { Text(statusLabel) },
                                    leadingIcon = if (statusFilter == statusLabel) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    if (filteredList.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tidak ada transaksi dengan status ini.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredList) { order ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                                border = BorderStroke(1.dp, colorScheme.outlineVariant)
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
                                        Column {
                                            Text(
                                                text = order.namaPesanan,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Tanggal: ${order.tanggalOrder}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }

                                        val isLunas = order.status == "Lunas"
                                        val badgeColor = if (isLunas) MintAurora else PinkAurora
                                        val badgeTextColor = if (isLunas) HijauGelap else MagentaLembut
                                        Surface(
                                            color = badgeColor,
                                            shape = RoundedCornerShape(100.dp)
                                        ) {
                                            Text(
                                                text = if (isLunas) "SUDAH LUNAS" else "PENDING BAYAR",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = badgeTextColor
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.7f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Rincian Paket", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                            Text("${order.qtyOrder} ${order.satuan}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Harga Satuan", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                            Text(formatRupiah(order.hargaSatuan), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Total Bayar", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                            Text(formatRupiah(order.totalPendapatan), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.deleteOrder(order) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Transaksi",
                                                tint = colorScheme.error.copy(alpha = 0.8f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                val nextStatus = if (order.status == "Lunas") "Belum Lunas" else "Lunas"
                                                viewModel.updateOrder(order.copy(status = nextStatus))
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (order.status == "Lunas") colorScheme.surfaceVariant else colorScheme.primary,
                                                contentColor = if (order.status == "Lunas") colorScheme.onSurfaceVariant else colorScheme.onPrimary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = if (order.status == "Lunas") Icons.Default.Close else Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (order.status == "Lunas") "Ubah ke Belum Lunas" else "Tandai Lunas",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }

            2 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Ledger Saldo Pos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onBackground
                        )
                        Text(
                            text = "Pantau pembagian otomatis (plotting) ke amplop modal dasar dan sisa laba berdasarkan transaksi lunas, serta mutasi kas penyesuaian secara real-time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }

                    item {
                        LedgerSaldoPosCard(
                            summary = summary,
                            onAccountClick = { selectedLedgerAccount = it }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }

            3 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Laporan Mutasi Kas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onBackground
                        )
                        Text(
                            text = "Rekapitulasi detail seluruh transaksi penyesuaian kas manual, penarikan/pembayaran, dan transfer saldo antar dompet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }

                    // Header Tabel
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "WAKTU & TIPE",
                                    modifier = Modifier.weight(1.5f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "DOMPET ASAL/TUJUAN",
                                    modifier = Modifier.weight(2.0f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "NOMINAL & AUDIT",
                                    modifier = Modifier.weight(2.0f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    if (mutations.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tidak ada riwayat mutasi kas dalam rentang tanggal ini.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(mutations) { mut ->
                            val isMasuk = mut.jenisMutasi == "Uang Masuk"
                            val isTransfer = mut.jenisMutasi == "Pindah Saldo"
                            
                            val sourceAccountName = accountMap[mut.idAkun]?.namaAkun?.replace("Dompet ", "") ?: "Kas"
                            val targetAccountName = if (isTransfer) {
                                accountMap[mut.idAkunTujuan]?.namaAkun?.replace("Dompet ", "") ?: "Kas"
                            } else null

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                                border = BorderStroke(1.dp, colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Column 1: Date, Time & Type Badge
                                        Column(modifier = Modifier.weight(1.5f)) {
                                            Text(
                                                text = mut.tanggalMutasi,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = mut.waktuMutasi,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = when {
                                                            isMasuk -> Color(0xFFE8F5E9)
                                                            isTransfer -> Color(0xFFE3F2FD)
                                                            else -> Color(0xFFFFEBEE)
                                                        },
                                                        shape = RoundedCornerShape(100.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = when {
                                                        isMasuk -> "MASUK"
                                                        isTransfer -> "TRANSFER"
                                                        else -> "KELUAR"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = when {
                                                        isMasuk -> Color(0xFF2E7D32)
                                                        isTransfer -> Color(0xFF1565C0)
                                                        else -> Color(0xFFC62828)
                                                    }
                                                )
                                            }
                                        }

                                        // Column 2: Wallets
                                        Column(modifier = Modifier.weight(2.0f)) {
                                            if (isTransfer) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.RemoveCircle, 
                                                        contentDescription = null, 
                                                        tint = Color(0xFFC62828), 
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = sourceAccountName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.AddCircle, 
                                                        contentDescription = null, 
                                                        tint = Color(0xFF2E7D32), 
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = targetAccountName ?: "",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = sourceAccountName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (isMasuk) "Sebagai Dompet Penerima" else "Sebagai Dompet Pengirim",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }

                                        // Column 3: Nominal & Audit note
                                        Column(
                                            modifier = Modifier.weight(2.0f),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = when {
                                                    isMasuk -> "+" + formatRupiah(mut.nominal)
                                                    isTransfer -> formatRupiah(mut.nominal)
                                                    else -> "-" + formatRupiah(mut.nominal)
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = when {
                                                    isMasuk -> Color(0xFF2E7D32)
                                                    isTransfer -> colorScheme.primary
                                                    else -> Color(0xFFC62828)
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = mut.keterangan,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.End,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }

            4 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Analisis Pos Alokasi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onBackground
                        )
                        Text(
                            text = "Perbandingan visual komprehensif pemasukan hasil plotting pesanan vs pengeluaran riil setiap pos kas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }

                    item {
                        PosAllocationComparisonSection(
                            viewModel = viewModel,
                            allocationSummary = allocationSummary
                        )
                    }

                    item {
                        AllocationBarChart(summary.rows)
                    }

                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
    }

    if (exportSuccessDialogInfo != null) {
        val info = exportSuccessDialogInfo!!
        AlertDialog(
            onDismissRequest = { exportSuccessDialogInfo = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Ekspor Berhasil",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "File laporan telah berhasil diekspor dan disimpan di folder Unduhan/Download perangkat Anda.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    info.fileType.contains("pdf") -> Icons.Default.PictureAsPdf
                                    info.fileType.contains("csv") -> Icons.Default.FileDownload
                                    else -> Icons.Default.BorderOuter
                                },
                                contentDescription = null,
                                tint = when {
                                    info.fileType.contains("pdf") -> Color(0xFFC62828)
                                    info.fileType.contains("csv") -> MaterialTheme.colorScheme.primary
                                    else -> Color(0xFF2E7D32)
                                }
                            )
                            Text(
                                text = info.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(info.fileUri, info.fileType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Buka Laporan"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Tidak ada aplikasi untuk membuka file ini.", Toast.LENGTH_SHORT).show()
                        }
                        exportSuccessDialogInfo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Buka File")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = info.fileType
                            putExtra(Intent.EXTRA_STREAM, info.fileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Bagikan Laporan"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membagikan file.", Toast.LENGTH_SHORT).show()
                        }
                        exportSuccessDialogInfo = null
                    }
                ) {
                    Text("Bagikan")
                }
            }
        )
    }

    if (selectedLedgerAccount != null) {
        DetailLedgerDialog(
            account = selectedLedgerAccount!!,
            viewModel = viewModel,
            showQuickActions = false,
            onDismiss = { selectedLedgerAccount = null }
        )
    }
}

@Composable
fun FilterPresetChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

data class CombinedHistoryItem(
    val tanggal: String,
    val jenis: String,
    val deskripsi: String,
    val nominal: Double
)

data class ExportDialogInfo(
    val fileName: String,
    val fileUri: Uri,
    val fileType: String
)

fun saveExportedFile(
    context: Context,
    fileName: String,
    mimeType: String,
    writeBlock: (OutputStream) -> Unit
): Uri? {
    val exportsDir = File(context.filesDir, "exports")
    if (!exportsDir.exists()) {
        exportsDir.mkdirs()
    }
    val localFile = File(exportsDir, fileName)
    try {
        FileOutputStream(localFile).use { fos ->
            writeBlock(fos)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Gagal menulis berkas lokal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        return null
    }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PGD Order")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { os ->
                    FileInputStream(localFile).use { fis ->
                        fis.copyTo(os)
                    }
                }
            }
        } else {
            val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PGD Order")
            if (!publicDir.exists()) {
                publicDir.mkdirs()
            }
            val publicFile = File(publicDir, fileName)
            FileOutputStream(publicFile).use { fos ->
                FileInputStream(localFile).use { fis ->
                    fis.copyTo(fos)
                }
            }
            MediaScannerConnection.scanFile(context, arrayOf(publicFile.absolutePath), null, null)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun exportToCsv(
    context: Context,
    startDate: String,
    endDate: String,
    totalUnits: Int,
    totalOmzet: Double,
    totalMutationOut: Double,
    combinedList: List<CombinedHistoryItem>,
    onExportSuccess: (String, Uri, String) -> Unit
) {
    val fileName = "Laporan_Pembukuan_${startDate}_to_${endDate}.csv"
    val uri = saveExportedFile(context, fileName, "text/csv") { outputStream ->
        val writer = OutputStreamWriter(outputStream, "UTF-8")
        writer.write("LAPORAN PEMBUKUAN PGD ORDER\n")
        writer.write("Rentang Tanggal; $startDate s.d $endDate\n\n")
        writer.write("REKAPITULASI DATA PERFORMA\n")
        writer.write("Total Unit Terproduksi; $totalUnits pcs\n")
        writer.write("Total Omzet; ${formatRupiah(totalOmzet)}\n")
        writer.write("Total Pengeluaran Mutasi Kas; ${formatRupiah(totalMutationOut)}\n\n")
        
        writer.write("HISTORI TRANSAKSI GABUNGAN\n")
        writer.write("Tanggal;Jenis Transaksi;Deskripsi/Keterangan;Nominal Kas\n")
        
        combinedList.forEach { item ->
            val nomStr = if (item.nominal >= 0) "+${formatRupiah(item.nominal)}" else "-${formatRupiah(-item.nominal)}"
            writer.write("${item.tanggal};${item.jenis};${item.deskripsi};$nomStr\n")
        }
        writer.flush()
    }
    
    if (uri != null) {
        onExportSuccess(fileName, uri, "text/csv")
    } else {
        Toast.makeText(context, "Gagal mengekspor CSV", Toast.LENGTH_SHORT).show()
    }
}

fun exportToExcel(
    context: Context,
    startDate: String,
    endDate: String,
    totalUnits: Int,
    totalOmzet: Double,
    totalMutationOut: Double,
    combinedList: List<CombinedHistoryItem>,
    onExportSuccess: (String, Uri, String) -> Unit
) {
    val fileName = "Laporan_Pembukuan_${startDate}_to_${endDate}.xlsx"
    val uri = saveExportedFile(context, fileName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") { outputStream ->
        val writer = OutputStreamWriter(outputStream, "UTF-8")
        writer.write("LAPORAN PEMBUKUAN PGD ORDER (2020–2026)\n")
        writer.write("Rentang Tanggal:\t$startDate s.d $endDate (Periode Aktif 2020–2026)\n\n")
        writer.write("REKAPITULASI DATA PERFORMA\n")
        writer.write("Total Unit Terproduksi:\t$totalUnits pcs\n")
        writer.write("Total Omzet:\t${formatRupiah(totalOmzet)}\n")
        writer.write("Total Pengeluaran Mutasi:\t${formatRupiah(totalMutationOut)}\n\n")
        
        writer.write("HISTORI TRANSAKSI GABUNGAN\n")
        writer.write("Tanggal\tJenis Transaksi\tDeskripsi/Keterangan\tNominal Kas\n")
        
        combinedList.forEach { item ->
            val nomStr = if (item.nominal >= 0) "+${formatRupiah(item.nominal)}" else "-${formatRupiah(-item.nominal)}"
            writer.write("${item.tanggal}\t${item.jenis}\t${item.deskripsi}\t$nomStr\n")
        }
        writer.flush()
    }
    
    if (uri != null) {
        onExportSuccess(fileName, uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    } else {
        Toast.makeText(context, "Gagal mengekspor Excel", Toast.LENGTH_SHORT).show()
    }
}

fun exportToPdf(
    context: Context,
    startDate: String,
    endDate: String,
    totalUnits: Int,
    totalOmzet: Double,
    totalMutationOut: Double,
    combinedList: List<CombinedHistoryItem>,
    onExportSuccess: (String, Uri, String) -> Unit
) {
    val fileName = "Laporan_Pembukuan_${startDate}_to_${endDate}.pdf"
    val uri = saveExportedFile(context, fileName, "application/pdf") { outputStream ->
        val pdfDocument = PdfDocument()
        val titlePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }
        val subTitlePaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 12f
        }
        val headerPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 1f
        }

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var y = 40f
        canvas.drawText("LAPORAN PEMBUKUAN PGD ORDER (2020–2026)", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("Rentang Tanggal: $startDate s.d $endDate (Periode Aktif 2020–2026)", 40f, y, subTitlePaint)
        y += 30f

        // Rekapitulasi Section
        canvas.drawText("REKAPITULASI PERFORMA (2020–2026)", 40f, y, headerPaint)
        y += 15f
        canvas.drawText("• Total Unit Terproduksi: $totalUnits pcs", 50f, y, bodyPaint)
        y += 15f
        canvas.drawText("• Total Omzet: ${formatRupiah(totalOmzet)}", 50f, y, bodyPaint)
        y += 15f
        canvas.drawText("• Total Pengeluaran Mutasi: ${formatRupiah(totalMutationOut)}", 50f, y, bodyPaint)
        y += 35f

        // Table Title
        canvas.drawText("HISTORI TRANSAKSI GABUNGAN", 40f, y, headerPaint)
        y += 20f

        // Table Header
        canvas.drawText("Tanggal", 40f, y, headerPaint)
        canvas.drawText("Jenis", 120f, y, headerPaint)
        canvas.drawText("Deskripsi / Keterangan", 200f, y, headerPaint)
        canvas.drawText("Nominal Kas", 460f, y, headerPaint)
        y += 5f
        canvas.drawLine(40f, y, 550f, y, linePaint)
        y += 15f

        // Table Rows
        combinedList.forEach { item ->
            if (y > 800f) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
                
                canvas.drawText("Tanggal", 40f, y, headerPaint)
                canvas.drawText("Jenis", 120f, y, headerPaint)
                canvas.drawText("Deskripsi / Keterangan", 200f, y, headerPaint)
                canvas.drawText("Nominal Kas", 460f, y, headerPaint)
                y += 5f
                canvas.drawLine(40f, y, 550f, y, linePaint)
                y += 15f
            }

            canvas.drawText(item.tanggal, 40f, y, bodyPaint)
            canvas.drawText(item.jenis, 120f, y, bodyPaint)
            
            val desc = if (item.deskripsi.length > 35) item.deskripsi.take(32) + "..." else item.deskripsi
            canvas.drawText(desc, 200f, y, bodyPaint)
            
            val nomText = if (item.nominal >= 0) "+" + formatRupiah(item.nominal) else "-" + formatRupiah(-item.nominal)
            canvas.drawText(nomText, 460f, y, bodyPaint)
            
            y += 18f
            canvas.drawLine(40f, y - 12f, 550f, y - 12f, linePaint)
        }

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
    
    if (uri != null) {
        onExportSuccess(fileName, uri, "application/pdf")
    } else {
        Toast.makeText(context, "Gagal mengekspor PDF", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        yearRange = 2020..2026
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selectedDateMillis = datePickerState.selectedDateMillis
                if (selectedDateMillis != null) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val dateStr = sdf.format(Date(selectedDateMillis))
                    onDateSelected(dateStr)
                }
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun BackupRestoreTab(viewModel: FinanceViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val backupsList by viewModel.backupsList.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isCloudOnline by viewModel.isCloudOnline.collectAsStateWithLifecycle()
    val cloudLastSyncTime by viewModel.cloudLastSyncTime.collectAsStateWithLifecycle()
    val syncStatusText by viewModel.syncStatusText.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadBackupFiles(context)
    }
    
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonStr = viewModel.generateBackupJsonString()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonStr.toByteArray())
                    }
                    android.widget.Toast.makeText(context, "Cadangan berhasil disimpan secara eksternal!", android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.createLocalBackup(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Gagal menyimpan cadangan: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonStr = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    } ?: ""
                    
                    if (jsonStr.isNotEmpty()) {
                        val success = viewModel.restoreFromJsonString(jsonStr)
                        if (success) {
                            android.widget.Toast.makeText(context, "Data berhasil dipulihkan!", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.loadBackupFiles(context)
                        } else {
                            android.widget.Toast.makeText(context, "Format berkas cadangan tidak valid!", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Gagal memulihkan data: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    var fileToDelete by remember { mutableStateOf<BackupFile?>(null) }
    var fileToRestoreDirectly by remember { mutableStateOf<BackupFile?>(null) }
    
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Hapus Cadangan", fontWeight = FontWeight.Bold, color = Color(0xFF3B2369)) },
            text = { Text("Apakah Anda yakin ingin menghapus berkas cadangan '${fileToDelete?.name}'? Berkas akan dihapus permanen dari penyimpanan lokal.", color = Color(0xFF554B6E)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileToDelete?.let {
                            viewModel.deleteBackupFile(context, it)
                            android.widget.Toast.makeText(context, "Cadangan lokal dihapus.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        fileToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Batal", color = Color(0xFF554B6E))
                }
            }
        )
    }

    if (fileToRestoreDirectly != null) {
        AlertDialog(
            onDismissRequest = { fileToRestoreDirectly = null },
            title = { Text("Pulihkan Data", fontWeight = FontWeight.Bold, color = Color(0xFF3B2369)) },
            text = { Text("Apakah Anda yakin ingin memulihkan seluruh data dari berkas '${fileToRestoreDirectly?.name}'? Seluruh tabel saat ini akan digabungkan/digantikan dengan isi berkas cadangan.", color = Color(0xFF554B6E)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileToRestoreDirectly?.let { backupFile ->
                            coroutineScope.launch {
                                try {
                                    val file = java.io.File(backupFile.absolutePath)
                                    val jsonStr = file.readText()
                                    val success = viewModel.restoreFromJsonString(jsonStr)
                                    if (success) {
                                        android.widget.Toast.makeText(context, "Berhasil memulihkan data dari cadangan lokal!", android.widget.Toast.LENGTH_SHORT).show()
                                        viewModel.loadBackupFiles(context)
                                    } else {
                                        android.widget.Toast.makeText(context, "Gagal memulihkan, format berkas tidak valid.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    android.widget.Toast.makeText(context, "Gagal: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        fileToRestoreDirectly = null
                    }
                ) {
                    Text("Pulihkan", color = Color(0xFF6A4C93), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRestoreDirectly = null }) {
                    Text("Batal", color = Color(0xFF554B6E))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Firebase Cloud Synchronization Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("backup_cloud_sync_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF3EEFA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSyncing) Icons.Default.Sync else if (isCloudOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = "Firebase Sync",
                                tint = Color(0xFF6A4C93),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Sinkronisasi Cloud",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B2369)
                            )
                            Text(
                                text = if (isSyncing) "Sedang menyelaraskan..." else if (isCloudOnline) "Firestore Real-time Aktif" else "Mode Offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF554B6E)
                            )
                        }
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSyncing) Color(0xFFEDE4FF) else if (isCloudOnline) Color(0xFFF3EEFA) else Color(0xFFFFEBEE),
                        border = BorderStroke(1.dp, if (isSyncing) Color(0xFF6A4C93) else if (isCloudOnline) Color(0xFFE4DAF7) else Color(0xFFFFCDD2))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isSyncing) Color(0xFF6A4C93) else if (isCloudOnline) Color(0xFF6A4C93) else Color(0xFFD32F2F))
                            )
                            Text(
                                text = if (isSyncing) "Syncing" else if (isCloudOnline) "Online" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSyncing) Color(0xFF3B2369) else if (isCloudOnline) Color(0xFF3B2369) else Color(0xFFD32F2F)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFEDE4FF), thickness = 1.dp)

                // Info waktu sinkron terakhir
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Terakhir Disinkronkan",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF554B6E)
                        )
                        Text(
                            text = cloudLastSyncTime,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B2369)
                        )
                    }

                    // Tombol Sinkronkan Sekarang
                    Button(
                        onClick = { viewModel.triggerCloudSync() },
                        enabled = !isSyncing,
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("btn_sync_now"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6A4C93),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFEDE4FF),
                            disabledContentColor = Color(0xFF6A4C93)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF6A4C93)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Proses...",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sinkronkan Sekarang",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sinkronkan Sekarang",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Two primary backup/restore action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val formattedDate = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    createDocumentLauncher.launch("PGD_Order_Backup_$formattedDate.json")
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("btn_create_backup"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A4C93),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Buat Cadangan Baru",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cadangkan Baru",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    openDocumentLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("btn_restore_data"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFF6A4C93)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFF3EEFA),
                    contentColor = Color(0xFF6A4C93)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Pulihkan Data",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pulihkan Data",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Section Title
        Text(
            text = "Riwayat Cadangan Lokal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3B2369),
            modifier = Modifier.padding(top = 4.dp)
        )

        // Backups List
        if (backupsList.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFFE4DAF7))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF3EEFA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "No Backup",
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFF6A4C93).copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = "Belum ada riwayat cadangan lokal.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF554B6E),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(backupsList) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_item_${item.name}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF3EEFA)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backup,
                                        contentDescription = "JSON Backup",
                                        tint = Color(0xFF6A4C93),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B2369),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", item.sizeKb)} KB",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF554B6E)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE4DAF7))
                                        )
                                        Text(
                                            text = item.dateFormatted,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF554B6E)
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { fileToRestoreDirectly = item },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = "Pulihkan",
                                        tint = Color(0xFF6A4C93),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { shareBackupFile(context, item) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Bagikan",
                                        tint = Color(0xFF6A4C93),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { fileToDelete = item },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(20.dp)
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

private fun shareBackupFile(context: android.content.Context, backupFile: BackupFile) {
    try {
        val file = java.io.File(backupFile.absolutePath)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Bagikan Cadangan"))
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Gagal membagikan berkas: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

// ==========================================
// COMPONENT: PROFILE AVATAR
// ==========================================
@Composable
fun ProfileAvatar(
    userProfile: UserProfile,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(size / 3),
        color = when (userProfile.avatarType) {
            "avatar_executive" -> Color(0xFF006A60)
            "avatar_store" -> Color(0xFFB45309)
            "avatar_finance" -> Color(0xFF15803D)
            "avatar_badge" -> Color(0xFF6B21A8)
            "avatar_person" -> Color(0xFFBE123C)
            else -> colorScheme.primaryContainer
        },
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (userProfile.avatarType == "custom" && userProfile.avatarUri.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(userProfile.avatarUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto Profil ${userProfile.adminName}",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val icon = when (userProfile.avatarType) {
                    "avatar_executive" -> Icons.Default.BusinessCenter
                    "avatar_store" -> Icons.Default.Storefront
                    "avatar_finance" -> Icons.Default.AccountBalance
                    "avatar_badge" -> Icons.Default.Badge
                    "avatar_person" -> Icons.Default.Person
                    else -> Icons.Default.AdminPanelSettings
                }
                val tintColor = when (userProfile.avatarType) {
                    "avatar_admin" -> colorScheme.primary
                    else -> Color.White
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }
}

// ==========================================
// SCREEN: MENU PROFIL ADMINISTRATOR
// ==========================================
@Composable
fun ProfileScreen(
    viewModel: FinanceViewModel,
    onOpenLogin: () -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isGuest by viewModel.isGuest.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    var adminNameText by remember(userProfile.adminName) { mutableStateOf(userProfile.adminName) }
    var taglineText by remember(userProfile.tagline) { mutableStateOf(userProfile.tagline) }
    var selectedAvatarType by remember(userProfile.avatarType) { mutableStateOf(userProfile.avatarType) }
    var customUriString by remember(userProfile.avatarUri) { mutableStateOf(userProfile.avatarUri) }
    var isSaved by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            customUriString = it.toString()
            selectedAvatarType = "custom"
            isSaved = false
        }
    }

    val customFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colorScheme.primary,
        unfocusedBorderColor = colorScheme.outlineVariant,
        focusedContainerColor = colorScheme.surface,
        unfocusedContainerColor = colorScheme.surface
    )
    val customFieldShape = RoundedCornerShape(16.dp)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Profile Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_card_header"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val previewProfile = UserProfile(
                        adminName = adminNameText,
                        tagline = taglineText,
                        avatarType = selectedAvatarType,
                        avatarUri = customUriString
                    )

                    ProfileAvatar(
                        userProfile = previewProfile,
                        size = 80.dp,
                        modifier = Modifier.testTag("profile_avatar_preview")
                    )

                    Text(
                        text = adminNameText.ifBlank { "PGD Order" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        color = colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = taglineText.ifBlank { "Pradipta Graha Digital" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Section: Opsi Foto Profil
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "FOTO PROFIL ADMINISTRATOR",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button_pick_image"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pilih Foto dari Galeri (Image Picker)")
                    }

                    Text(
                        text = "Atau Pilih Avatar Preset:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant
                    )

                    val presets = listOf(
                        "avatar_admin" to "Admin",
                        "avatar_executive" to "Direktur",
                        "avatar_store" to "Toko",
                        "avatar_finance" to "Keuangan",
                        "avatar_badge" to "Manajer",
                        "avatar_person" to "Personal"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.take(3).forEach { (type, label) ->
                            val isSelected = selectedAvatarType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedAvatarType = type
                                    isSaved = false
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chip_avatar_$type"),
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.drop(3).forEach { (type, label) ->
                            val isSelected = selectedAvatarType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedAvatarType = type
                                    isSaved = false
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chip_avatar_$type"),
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // Section: Form Details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "IDENTITAS PENGELOLA",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = adminNameText,
                        onValueChange = {
                            adminNameText = it
                            isSaved = false
                        },
                        label = { Text("Nama Administrator") },
                        placeholder = { Text("Contoh: PGD Order / Budi Santoso") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_profile_name"),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = taglineText,
                        onValueChange = {
                            taglineText = it
                            isSaved = false
                        },
                        label = { Text("Quotes / Tagline") },
                        placeholder = { Text("Contoh: Kelola Keuangan Lebih Bijak") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_profile_tagline"),
                        leadingIcon = { Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )

                    if (isSaved) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Data profil berhasil disimpan permanen!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF1B5E20),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.saveUserProfile(
                                adminName = adminNameText,
                                tagline = taglineText,
                                avatarType = selectedAvatarType,
                                avatarUri = customUriString
                            )
                            isSaved = true
                            Toast.makeText(context, "Profil Administrator Diperbarui!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_profile_button"),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SIMPAN PERUBAHAN PROFIL",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// MENU KHUSUS: ORDER NOTA (KALKULATOR & HISTORI)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderNotaScreen(
    viewModel: FinanceViewModel,
    orders: List<TransaksiOrderMasuk>,
    accounts: List<MasterAkunSaldo>
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Form Kalkulator & Input Order, 1: Riwayat Pesanan Nota

    val customFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color(0xFFF7F5FC),
        focusedBorderColor = colorScheme.primary,
        unfocusedBorderColor = colorScheme.outlineVariant,
        focusedLabelColor = colorScheme.primary,
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        focusedTextColor = colorScheme.onSurface,
        unfocusedTextColor = colorScheme.onSurface,
        cursorColor = colorScheme.primary
    )
    val customFieldShape = RoundedCornerShape(12.dp)

    // Master settings rates prefilled from database
    val kertasHppSys = accounts.find { it.namaAkun == "Kertas" || it.namaAkun == "Dompet Kertas" }?.konstanHppUnit?.toDouble() ?: 106.0
    val tintaHppSys = accounts.find { it.namaAkun == "Tinta" || it.namaAkun == "Dompet Tinta" }?.konstanHppUnit?.toDouble() ?: 25.0
    val pengemasanHppSys = accounts.find { it.namaAkun == "Pengemasan" || it.namaAkun == "Dompet Pengemasan" }?.konstanHppUnit?.toDouble() ?: 300.0
    val wastePctSys = accounts.find { it.namaAkun == "Waste" || it.namaAkun == "Dompet Waste / Rusak" }?.persentaseOperasional?.toDouble() ?: 0.05
    val tenagaKerjaPctSys = accounts.find { it.namaAkun == "Tenaga Kerja" || it.namaAkun == "Dompet Tenaga Kerja" }?.persentaseOperasional?.toDouble() ?: 0.07
    val listrikPctSys = accounts.find { it.namaAkun == "Listrik" || it.namaAkun == "Dompet Listrik" }?.persentaseOperasional?.toDouble() ?: 0.02
    val maintenancePctSys = accounts.find { it.namaAkun == "Maintenance Alat" || it.namaAkun == "Dompet Maintenance" }?.persentaseOperasional?.toDouble() ?: 0.05

    // Component HPP Inputs (allowing 0 - Standar Nota Paket: Kertas 120.000, Pengemasan 24.000)
    var kertasHppText by remember(accounts) { mutableStateOf("120000") }
    var tintaHppText by remember(accounts) { mutableStateOf("0") }
    var pengemasanHppText by remember(accounts) { mutableStateOf("24000") }
    var wastePctText by remember(accounts) { mutableStateOf(formatDouble(wastePctSys * 100)) }
    var tenagaKerjaPctText by remember(accounts) { mutableStateOf(formatDouble(tenagaKerjaPctSys * 100)) }
    var listrikPctText by remember(accounts) { mutableStateOf(formatDouble(listrikPctSys * 100)) }
    var maintenancePctText by remember(accounts) { mutableStateOf(formatDouble(maintenancePctSys * 100)) }

    // Order detail inputs
    var editingOrderId by remember { mutableStateOf<Int?>(null) }
    var tanggal by remember { mutableStateOf(viewModel.getTodayString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var namaPesananText by remember { mutableStateOf("") }
    var qtyOrderText by remember { mutableStateOf("") }
    var satuanText by remember { mutableStateOf("Paket") }
    var totalHargaJualText by remember { mutableStateOf("") }
    var hargaSatuanText by remember { mutableStateOf("") }
    var jumlahPlastikText by remember { mutableStateOf("") }
    var isLunas by remember { mutableStateOf(true) }
    var syncGlobalSettings by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val onEditOrder: (TransaksiOrderMasuk) -> Unit = { order ->
        editingOrderId = order.idOrder
        tanggal = order.tanggalOrder
        namaPesananText = order.namaPesanan
        qtyOrderText = order.qtyOrder.toString()
        satuanText = if (order.satuan.isBlank()) "Paket" else order.satuan
        hargaSatuanText = formatDouble(order.hargaSatuan)
        totalHargaJualText = formatDouble(order.qtyOrder * order.hargaSatuan)
        jumlahPlastikText = order.jumlahPlastikPengemasan.toString()
        isLunas = (order.status == "Lunas")
        errorMessage = null
        selectedTab = 0
    }

    if (showDatePicker) {
        OrderDatePickerDialog(
            onDateSelected = { tanggal = it },
            onDismiss = { showDatePicker = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row Header (Slim UI)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .background(colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                Pair("Kalkulasi Nota", Icons.Default.Calculate),
                Pair("Riwayat Pesanan Nota", Icons.Default.ReceiptLong)
            )
            tabs.forEachIndexed { index, (title, icon) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colorScheme.primary else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                // TAB 1: FORM KALKULATOR & INPUT ORDER
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (editingOrderId != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_mode_banner_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Column {
                                            Text(
                                                "Mode Edit Pesanan Nota #${editingOrderId}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                "Ubah rincian pesanan lalu simpan untuk memperbarui tanpa duplikasi.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            editingOrderId = null
                                            namaPesananText = ""
                                            qtyOrderText = ""
                                            totalHargaJualText = ""
                                            hargaSatuanText = ""
                                            jumlahPlastikText = ""
                                        }
                                    ) {
                                        Text("Batal Edit", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Header Banner Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EEFA)),
                            border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF6A4C93), modifier = Modifier.size(32.dp))
                                Column {
                                    Text(
                                        text = "Ploting Pos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B2369)
                                    )
                                    Text(
                                        text = "Sesuaikan rincian HPP (termasuk nilai 0). Sistem otomatis memploting nominal ke sub-anggaran pos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF554B6E)
                                    )
                                }
                            }
                        }
                    }

                    // Section: Rincian HPP / Komponen Anggaran (Bisa diisi 0)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
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
                                        Icon(Icons.Default.Tune, contentDescription = null, tint = colorScheme.primary)
                                        Text(
                                            text = "Rincian Komponen Anggaran / HPP",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.primary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(MintGreen.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Bolehkah Nilai 0",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }

                                Text(
                                    text = "Atur tarif modal & persentase operasional. Nilai 0 diperbolehkan jika komponen tidak digunakan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )

                                // Unit Cost HPPs
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = kertasHppText,
                                        onValueChange = { kertasHppText = it },
                                        label = { Text("HPP Kertas (Rp/Unit)") },
                                        modifier = Modifier.weight(1f).testTag("input_nota_hpp_kertas"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = customFieldColors,
                                        shape = customFieldShape,
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = tintaHppText,
                                        onValueChange = { tintaHppText = it },
                                        label = { Text("HPP Tinta (Rp/Unit)") },
                                        modifier = Modifier.weight(1f).testTag("input_nota_hpp_tinta"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = customFieldColors,
                                        shape = customFieldShape,
                                        singleLine = true
                                    )
                                }

                                OutlinedTextField(
                                    value = pengemasanHppText,
                                    onValueChange = { pengemasanHppText = it },
                                    label = { Text("HPP Pengemasan/Plastik (Rp/Plastik)") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_nota_hpp_pengemasan"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = customFieldColors,
                                    shape = customFieldShape,
                                    singleLine = true
                                )

                                // Percentage HPPs
                                Text(
                                    text = "Persentase Alokasi Operasional (%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = wastePctText,
                                        onValueChange = { wastePctText = it },
                                        label = { Text("Waste (%)") },
                                        modifier = Modifier.weight(1f).testTag("input_nota_pct_waste"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = customFieldColors,
                                        shape = customFieldShape,
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = tenagaKerjaPctText,
                                        onValueChange = { tenagaKerjaPctText = it },
                                        label = { Text("Tenaga Kerja (%)") },
                                        modifier = Modifier.weight(1f).testTag("input_nota_pct_tk"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = customFieldColors,
                                        shape = customFieldShape,
                                        singleLine = true
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = listrikPctText,
                                        onValueChange = { listrikPctText = it },
                                        label = { Text("Listrik (%)") },
                                        modifier = Modifier.weight(1f).testTag("input_nota_pct_listrik"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = customFieldColors,
                                        shape = customFieldShape,
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = maintenancePctText,
                                        onValueChange = { maintenancePctText = it },
                                        label = { Text("Maintenance (%)") },
                                        modifier = Modifier.weight(1f).testTag("input_nota_pct_maint"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = customFieldColors,
                                        shape = customFieldShape,
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    // Section: Input Data Pesanan Nota
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Rincian Transaksi Pesanan Nota",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )

                                // Tanggal
                                OutlinedTextField(
                                    value = tanggal,
                                    onValueChange = { tanggal = it },
                                    label = { Text("Tanggal Transaksi (YYYY-MM-DD)") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_nota_tanggal"),
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    trailingIcon = {
                                        IconButton(onClick = { showDatePicker = true }) {
                                            Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                                        }
                                    },
                                    colors = customFieldColors,
                                    shape = customFieldShape,
                                    singleLine = true
                                )

                                // Nama Pesanan / Deskripsi
                                OutlinedTextField(
                                    value = namaPesananText,
                                    onValueChange = { namaPesananText = it },
                                    label = { Text("Nama Pemesan / Deskripsi Order Nota") },
                                    placeholder = { Text("Contoh: Toko Barokah - Nota 2 Rangkap NCR (100 Buku)") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_nota_nama"),
                                    leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    colors = customFieldColors,
                                    shape = customFieldShape,
                                    singleLine = true
                                )

                                // Qty & Plastik
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = qtyOrderText,
                                        onValueChange = {
                                            qtyOrderText = it
                                            val qty = parseIntInput(it) ?: 0
                                            val totalVal = parseDoubleInput(totalHargaJualText)
                                            if (qty > 0 && totalVal != null) {
                                                hargaSatuanText = formatDouble(totalVal / qty)
                                            }
                                        },
                                        label = { Text("Jumlah Pesanan") },
                                        modifier = Modifier.weight(1f).testTag("input_nota_qty"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = customFieldColors,
                                        shape = customFieldShape,
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = jumlahPlastikText,
                                        onValueChange = { jumlahPlastikText = it },
                                        label = { Text("Plastik Bungkus") },
                                        placeholder = { Text("0") },
                                        modifier = Modifier.weight(1f).testTag("input_nota_plastik"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = customFieldColors,
                                        shape = customFieldShape,
                                        singleLine = true
                                    )
                                }

                                // Total Harga Jual & Harga Satuan
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = totalHargaJualText,
                                        onValueChange = {
                                            totalHargaJualText = it
                                            val qty = parseIntInput(qtyOrderText) ?: 0
                                            val totalVal = parseDoubleInput(it)
                                            if (qty > 0 && totalVal != null) {
                                                hargaSatuanText = formatDouble(totalVal / qty)
                                            }
                                        },
                                        label = { Text("Total Harga Jual (Rp)") },
                                        modifier = Modifier.weight(1f).testTag("input_nota_total_jual"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = customFieldColors,
                                        shape = customFieldShape,
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = hargaSatuanText,
                                        onValueChange = {
                                            hargaSatuanText = it
                                            val qty = parseIntInput(qtyOrderText) ?: 0
                                            val hargaVal = parseDoubleInput(it)
                                            if (qty > 0 && hargaVal != null) {
                                                totalHargaJualText = formatDouble(qty * hargaVal)
                                            }
                                        },
                                        label = { Text("Harga Satuan (Rp)") },
                                        modifier = Modifier.weight(1f).testTag("input_nota_harga_satuan"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = customFieldColors,
                                        shape = customFieldShape,
                                        singleLine = true
                                    )
                                }

                                // Status Pembayaran
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Status:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    ElevatedFilterChip(
                                        selected = isLunas,
                                        onClick = { isLunas = true },
                                        label = { Text("Lunas") },
                                        modifier = Modifier.weight(1f).testTag("chip_nota_lunas"),
                                        leadingIcon = if (isLunas) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                    ElevatedFilterChip(
                                        selected = !isLunas,
                                        onClick = { isLunas = false },
                                        label = { Text("Belum Lunas") },
                                        modifier = Modifier.weight(1f).testTag("chip_nota_belum"),
                                        leadingIcon = if (!isLunas) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }

                                // Sync global switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Terapkan acuan HPP ini ke sistem global",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Switch(
                                        checked = syncGlobalSettings,
                                        onCheckedChange = { syncGlobalSettings = it },
                                        modifier = Modifier.testTag("switch_nota_sync_global")
                                    )
                                }
                            }
                        }
                    }

                    // Section: LIVE REAL-TIME PLOTTING BREAKDOWN CARD
                    item {
                        val qtyVal = parseIntInput(qtyOrderText) ?: 0
                        val totalJualVal = parseDoubleInput(totalHargaJualText) ?: 0.0
                        val plastikVal = parseIntInput(jumlahPlastikText) ?: 0

                        val kHpp = parseDoubleInput(kertasHppText) ?: 0.0
                        val tHpp = parseDoubleInput(tintaHppText) ?: 0.0
                        val pHpp = parseDoubleInput(pengemasanHppText) ?: 0.0
                        val wPct = (parseDecimalDouble(wastePctText) ?: 0.0) / 100.0
                        val tkPct = (parseDecimalDouble(tenagaKerjaPctText) ?: 0.0) / 100.0
                        val lPct = (parseDecimalDouble(listrikPctText) ?: 0.0) / 100.0
                        val mPct = (parseDecimalDouble(maintenancePctText) ?: 0.0) / 100.0

                        val calcKertas = qtyVal.toDouble() * kHpp
                        val calcTinta = qtyVal.toDouble() * tHpp
                        val calcPengemasan = plastikVal.toDouble() * pHpp
                        val calcWaste = wPct * totalJualVal
                        val calcTK = tkPct * totalJualVal
                        val calcListrik = lPct * totalJualVal
                        val calcMaint = mPct * totalJualVal

                        val totalModal = calcKertas + calcTinta + calcPengemasan + calcWaste + calcTK + calcListrik + calcMaint
                        val calcSisaLaba = totalJualVal - totalModal

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                            border = BorderStroke(1.5.dp, colorScheme.primary.copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
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
                                        Icon(Icons.Default.PieChart, contentDescription = null, tint = colorScheme.primary)
                                        Text(
                                            text = "Kalkulasi Real-Time Plotting Pos",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = formatRupiah(totalJualVal),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colorScheme.primary
                                    )
                                }

                                HorizontalDivider(color = colorScheme.outlineVariant)

                                val items = listOf(
                                    Triple("Dompet Kertas", calcKertas, "${formatRupiah(kHpp)}/unit"),
                                    Triple("Dompet Tinta", calcTinta, "${formatRupiah(tHpp)}/unit"),
                                    Triple("Dompet Pengemasan", calcPengemasan, "${formatRupiah(pHpp)}/plastik"),
                                    Triple("Dompet Waste / Rusak", calcWaste, "${formatAngka(wPct * 100)}%"),
                                    Triple("Dompet Tenaga Kerja", calcTK, "${formatAngka(tkPct * 100)}%"),
                                    Triple("Dompet Listrik", calcListrik, "${formatAngka(lPct * 100)}%"),
                                    Triple("Dompet Maintenance", calcMaint, "${formatAngka(mPct * 100)}%")
                                )

                                items.forEach { (label, value, rateStr) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = "($rateStr)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colorScheme.outline
                                            )
                                        }
                                        Text(
                                            text = formatRupiah(value),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (value == 0.0) colorScheme.outline else colorScheme.onSurface
                                        )
                                    }
                                }

                                HorizontalDivider(color = colorScheme.outlineVariant)

                                // Sisa Laba Bersih
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (calcSisaLaba >= 0) SisaLabaBg else Color(0xFFFFEBEE),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "DOMPET LABA BERSIH (SISA)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (calcSisaLaba >= 0) SisaLabaText else Color(0xFFC62828)
                                        )
                                        Text(
                                            text = "Total Jual - Total Modal HPP",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (calcSisaLaba >= 0) SisaLabaText.copy(alpha = 0.8f) else Color(0xFFC62828).copy(alpha = 0.8f)
                                        )
                                    }
                                    Text(
                                        text = formatRupiah(calcSisaLaba),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (calcSisaLaba >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        item {
                            Text(
                                text = errorMessage!!,
                                color = colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    // Button Process & Simpan
                    item {
                        Button(
                            onClick = {
                                val qty = parseIntInput(qtyOrderText)
                                val totalJual = parseDoubleInput(totalHargaJualText)
                                val hargaSatuan = parseDoubleInput(hargaSatuanText) ?: if (qty != null && qty > 0 && totalJual != null) totalJual / qty else 0.0
                                val plastik = parseIntInput(jumlahPlastikText) ?: 0

                                val kHpp = parseDoubleInput(kertasHppText) ?: 0.0
                                val tHpp = parseDoubleInput(tintaHppText) ?: 0.0
                                val pHpp = parseDoubleInput(pengemasanHppText) ?: 0.0
                                val wPct = (parseDecimalDouble(wastePctText) ?: 0.0) / 100.0
                                val tkPct = (parseDecimalDouble(tenagaKerjaPctText) ?: 0.0) / 100.0
                                val lPct = (parseDecimalDouble(listrikPctText) ?: 0.0) / 100.0
                                val mPct = (parseDecimalDouble(maintenancePctText) ?: 0.0) / 100.0

                                if (namaPesananText.isBlank()) {
                                    errorMessage = "Harap isi nama pemesan atau deskripsi order nota!"
                                    return@Button
                                }
                                if (qty == null || qty <= 0) {
                                    errorMessage = "Jumlah pesanan harus lebih dari 0!"
                                    return@Button
                                }
                                if (totalJual == null || totalJual <= 0.0) {
                                    errorMessage = "Total harga jual harus lebih dari 0!"
                                    return@Button
                                }

                                errorMessage = null

                                if (syncGlobalSettings) {
                                    viewModel.updateFinancialSettings(
                                        kertasHpp = kHpp,
                                        tintaHpp = tHpp,
                                        pengemasanHpp = pHpp,
                                        wastePct = wPct,
                                        tenagaKerjaPct = tkPct,
                                        listrikPct = lPct,
                                        maintenancePct = mPct
                                    )
                                }

                                if (editingOrderId != null) {
                                    val existingOrder = orders.find { it.idOrder == editingOrderId }
                                    val updatedOrder = (existingOrder ?: TransaksiOrderMasuk(
                                        idOrder = editingOrderId!!,
                                        tanggalOrder = tanggal,
                                        namaPesanan = namaPesananText.trim(),
                                        qtyOrder = qty,
                                        satuan = satuanText.ifBlank { "Paket" },
                                        hargaSatuan = hargaSatuan,
                                        jumlahPlastikPengemasan = plastik,
                                        status = if (isLunas) "Lunas" else "Belum Lunas"
                                    )).copy(
                                        tanggalOrder = tanggal,
                                        namaPesanan = namaPesananText.trim(),
                                        qtyOrder = qty,
                                        satuan = satuanText.ifBlank { "Paket" },
                                        hargaSatuan = hargaSatuan,
                                        jumlahPlastikPengemasan = plastik,
                                        status = if (isLunas) "Lunas" else "Belum Lunas"
                                    )
                                    viewModel.updateOrder(updatedOrder)
                                    editingOrderId = null
                                } else {
                                    viewModel.insertOrder(
                                        tanggal = tanggal,
                                        nama = namaPesananText.trim(),
                                        qty = qty,
                                        satuan = satuanText.ifBlank { "Paket" },
                                        harga = hargaSatuan,
                                        plastik = plastik,
                                        status = if (isLunas) "Lunas" else "Belum Lunas"
                                    )
                                }

                                namaPesananText = ""
                                qtyOrderText = ""
                                totalHargaJualText = ""
                                hargaSatuanText = ""
                                jumlahPlastikText = ""

                                selectedTab = 1 // Switch to Riwayat tab to review
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_nota_order_button"),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(if (editingOrderId != null) Icons.Default.Edit else Icons.Default.Save, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (editingOrderId != null) "Perbarui & Simpan Perubahan" else "Proses & Simpan Order Nota",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // Section: Educational Notes & Guidelines Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("nota_educational_notes_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.8f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Catatan & Panduan Edukasi Order Nota",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.primary
                                    )
                                }

                                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Text(
                                    text = "• Acuan Standar Nota Paket:\n" +
                                           "  - HPP Kertas Nota: Rp 120.000 / paket\n" +
                                           "  - HPP Pengemasan / Plastik: Rp 24.000 / paket\n" +
                                           "  - Persentase Operasional: Tetap mengikuti persentase sistem fotokopi/operasional umum (Waste 5%, Tenaga Kerja 7%, Listrik 2%, Maintenance 5%).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "• Fleksibilitas Nilai 0:\n" +
                                           "  Setiap komponen HPP dapat diisi nilai 0 jika tidak digunakan (misalnya Tinta atau Tenaga Kerja). Sisa dana dari komponen yang diisi 0 akan dialihkan penuh ke Dompet Laba Bersih.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "• Sakelar Pengaman Global:\n" +
                                           "  Toggle 'Terapkan acuan HPP ini ke sistem global' secara default diset NONAKTIF (OFF) agar perhitungan khusus order nota paket ini tidak mengganggu atau mengubah acuan persentase harian toko/transaksi umum lainnya.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            } else {
                // TAB 2: RIWAYAT PESANAN NOTA
                RiwayatNotaTab(
                    orders = orders,
                    accounts = accounts,
                    viewModel = viewModel,
                    onEditOrder = onEditOrder
                )
            }
        }
    }
}

@Composable
fun RiwayatNotaTab(
    orders: List<TransaksiOrderMasuk>,
    accounts: List<MasterAkunSaldo>,
    viewModel: FinanceViewModel,
    onEditOrder: (TransaksiOrderMasuk) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("Semua") } // "Semua", "Lunas", "Belum Lunas"

    val filteredOrders = remember(orders, searchQuery, statusFilter) {
        orders.filter { order ->
            val matchQuery = searchQuery.isBlank() ||
                    order.namaPesanan.contains(searchQuery, ignoreCase = true) ||
                    order.tanggalOrder.contains(searchQuery, ignoreCase = true)
            val matchStatus = when (statusFilter) {
                "Lunas" -> order.status == "Lunas"
                "Belum Lunas" -> order.status == "Belum Lunas"
                else -> true
            }
            matchQuery && matchStatus
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Search & Filter Card (Soft Lavender Aesthetic)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7FF)),
                border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari nota pemesan / tanggal...", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.fillMaxWidth().testTag("search_nota_input"),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF6B46C1)) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF6B46C1))
                                }
                            }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF6B46C1),
                            unfocusedBorderColor = Color(0xFFD8B4FE)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Semua", "Lunas", "Belum Lunas").forEach { statusLabel ->
                            val isSelected = statusFilter == statusLabel
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { statusFilter = statusLabel }
                                    .testTag("chip_filter_nota_$statusLabel"),
                                color = if (isSelected) Color(0xFF6B46C1) else Color.White,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF6B46C1) else Color(0xFFE4DAF7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF553C9A)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (filteredOrders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFF9E8DB5))
                        Text(
                            text = "Belum Ada Riwayat Pesanan Nota",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF7A6B94),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Semua order nota yang dibuat akan tercatat rapi di sini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9E8DB5)
                        )
                    }
                }
            }
        } else {
            items(filteredOrders, key = { it.idOrder }) { order ->
                NotaOrderItemCard(
                    order = order,
                    accounts = accounts,
                    viewModel = viewModel,
                    onEditOrder = { onEditOrder(order) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun NotaOrderItemCard(
    order: TransaksiOrderMasuk,
    accounts: List<MasterAkunSaldo>,
    viewModel: FinanceViewModel,
    onEditOrder: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val kertasHpp = accounts.find { it.namaAkun == "Kertas" || it.namaAkun == "Dompet Kertas" }?.konstanHppUnit?.toDouble() ?: 106.0
    val tintaHpp = accounts.find { it.namaAkun == "Tinta" || it.namaAkun == "Dompet Tinta" }?.konstanHppUnit?.toDouble() ?: 25.0
    val pengemasanHpp = accounts.find { it.namaAkun == "Pengemasan" || it.namaAkun == "Dompet Pengemasan" }?.konstanHppUnit?.toDouble() ?: 300.0
    val wastePct = accounts.find { it.namaAkun == "Waste" || it.namaAkun == "Dompet Waste / Rusak" }?.persentaseOperasional?.toDouble() ?: 0.05
    val tenagaKerjaPct = accounts.find { it.namaAkun == "Tenaga Kerja" || it.namaAkun == "Dompet Tenaga Kerja" }?.persentaseOperasional?.toDouble() ?: 0.07
    val listrikPct = accounts.find { it.namaAkun == "Listrik" || it.namaAkun == "Dompet Listrik" }?.persentaseOperasional?.toDouble() ?: 0.02
    val maintenancePct = accounts.find { it.namaAkun == "Maintenance Alat" || it.namaAkun == "Dompet Maintenance" }?.persentaseOperasional?.toDouble() ?: 0.05

    val qty = order.qtyOrder.toDouble()
    val totalRevenue = qty * order.hargaSatuan
    val kertasVal = qty * kertasHpp
    val tintaVal = qty * tintaHpp
    val pengemasanVal = order.jumlahPlastikPengemasan.toDouble() * pengemasanHpp
    val wasteVal = wastePct * totalRevenue
    val tkVal = tenagaKerjaPct * totalRevenue
    val listrikVal = listrikPct * totalRevenue
    val maintVal = maintenancePct * totalRevenue
    val totalModal = kertasVal + tintaVal + pengemasanVal + wasteVal + tkVal + listrikVal + maintVal
    val sisaLabaVal = totalRevenue - totalModal

    val isLunas = order.status == "Lunas"

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Konfirmasi Hapus Riwayat Nota", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus riwayat nota pesanan '${order.namaPesanan}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteOrder(order)
                        showDeleteConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Ya, Hapus", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("Batal")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE4DAF7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Bar: Tanggal & Badge Status Lunas / Belum Lunas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFF3E8FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color(0xFF6B46C1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = order.tanggalOrder,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF553C9A)
                    )
                }

                Surface(
                    color = if (isLunas) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    border = BorderStroke(1.dp, if (isLunas) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = if (isLunas) "LUNAS" else "BELUM LUNAS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isLunas) Color(0xFF166534) else Color(0xFF991B1B),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            // Nama Pelanggan / Pesanan
            Text(
                text = order.namaPesanan,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E192B)
            )

            // Rincian Qty & Total Pendapatan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8F5FF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rincian Pesanan",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7A6B94)
                    )
                    Text(
                        text = "${order.qtyOrder} ${order.satuan} • ${formatRupiah(order.hargaSatuan)} / unit",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3B2D54)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Tagihan",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7A6B94)
                    )
                    Text(
                        text = formatRupiah(totalRevenue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF6B46C1)
                    )
                }
            }

            // Toggle Expand Button: Detail Alokasi Plotting Pos
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { expanded = !expanded },
                color = Color(0xFFF3E8FF).copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (expanded) "Sembunyikan Rincian Pos" else "Lihat Detail Alokasi Plotting Pos",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B46C1)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF6B46C1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (expanded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7FF)),
                    border = BorderStroke(1.dp, Color(0xFFE4DAF7))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Rincian Plotting Ke Pos Keuangan:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B46C1)
                        )
                        
                        val allocs = listOf(
                            Pair("Kertas", kertasVal),
                            Pair("Tinta", tintaVal),
                            Pair("Pengemasan", pengemasanVal),
                            Pair("Waste / Rusak", wasteVal),
                            Pair("Tenaga Kerja", tkVal),
                            Pair("Listrik", listrikVal),
                            Pair("Maintenance", maintVal),
                            Pair("Sisa Laba Bersih", sisaLabaVal)
                        )

                        allocs.forEach { (label, valAmount) ->
                            val isLaba = label == "Sisa Laba Bersih"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isLaba) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isLaba) Color(0xFF166534) else Color(0xFF553C9A)
                                )
                                Text(
                                    text = formatRupiah(valAmount),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLaba) Color(0xFF166534) else Color(0xFF1E192B)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE4DAF7).copy(alpha = 0.6f))

            // Action Buttons: Ubah Status, Edit, Hapus
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.updateOrder(order.copy(status = if (isLunas) "Belum Lunas" else "Lunas"))
                    },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isLunas) Color(0xFF991B1B) else Color(0xFF166534)
                    ),
                    border = BorderStroke(1.dp, if (isLunas) Color(0xFFFCA5A5) else Color(0xFF86EFAC)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        if (isLunas) Icons.Default.HourglassTop else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLunas) "Ubah ke Pending" else "Tandai Lunas",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditOrder,
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6B46C1)),
                        border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("edit_nota_button_${order.idOrder}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Nota", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFFFEBEE), CircleShape)
                            .testTag("delete_nota_button_${order.idOrder}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus Nota", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN: HALAMAN LOGIN & MASUK SEBAGAI TAMU
// ==========================================
@Composable
fun LoginScreen(
    viewModel: FinanceViewModel,
    onLoginSuccess: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme

    val customFieldShape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3EEFA))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Logo / Icon
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "PGD Order Logo",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = "PGD Order",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )

                Text(
                    text = "Aplikasi Manajemen Keuangan & Budgeting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Error Notification Card
                if (authError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = authError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearAuthError() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup Error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Email Input
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it; viewModel.clearAuthError() },
                    label = { Text("Email Administrator") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    singleLine = true,
                    shape = customFieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_login_email"),
                    enabled = !isAuthLoading
                )

                // Password Input
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it; viewModel.clearAuthError() },
                    label = { Text("Kata Sandi") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    singleLine = true,
                    shape = customFieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_login_password"),
                    enabled = !isAuthLoading
                )

                // Login Button
                Button(
                    onClick = {
                        viewModel.loginWithEmail(emailInput, passwordInput, onSuccess = onLoginSuccess)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_email_login"),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isAuthLoading
                ) {
                    if (isAuthLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Masuk dengan Akun", fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = colorScheme.outlineVariant
                )

                // Requirement 1: Tombol Masuk sebagai Tamu (Anonymous Login)
                FilledTonalButton(
                    onClick = {
                        viewModel.loginAsGuest(onSuccess = onLoginSuccess)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_anonymous_login"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer
                    ),
                    enabled = !isAuthLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = "Tamu"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Masuk sebagai Tamu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = "Akses instan tanpa akun. Data transaksi tersimpan otomatis di sistem lokal dan cloud Firestore secara anonim.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
