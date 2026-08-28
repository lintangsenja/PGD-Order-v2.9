import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# 1. New helper composables and QuickMutationDialog
helper_and_quick = '''// ==========================================
// REAL-TIME SALDO & KALKULASI SIMULASI
// ==========================================
@Composable
fun AccountBalanceBadge(
    label: String,
    balance: Double,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    val isNegative = balance < 0
    Surface(
        color = tintColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = tintColor
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatRupiah(balance),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isNegative) Color(0xFFDC2626) else tintColor
            )
        }
    }
}

@Composable
fun EstimatedBalanceSimulationCard(
    mutationType: String,
    currentBalance: Double,
    nominal: Double,
    sourceName: String = "",
    targetName: String = "",
    targetBalance: Double = 0.0,
    modifier: Modifier = Modifier
) {
    if (nominal <= 0.0) return

    when (mutationType) {
        "Uang Keluar" -> {
            val remaining = currentBalance - nominal
            val isInsufficient = nominal > currentBalance

            Surface(
                color = if (isInsufficient) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isInsufficient) Color(0xFFFCA5A5) else Color(0xFF86EFAC)),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isInsufficient) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Saldo tidak mencukupi!",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!isInsufficient) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Estimasi Sisa Saldo:",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isInsufficient) Color(0xFF991B1B) else Color(0xFF166534),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = formatRupiah(remaining),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isInsufficient) Color(0xFFDC2626) else Color(0xFF15803D)
                        )
                    }
                    if (isInsufficient) {
                        Text(
                            text = "Defisit: " + formatRupiah(nominal - currentBalance),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        "Pindah Saldo" -> {
            val remainingSource = currentBalance - nominal
            val newTarget = targetBalance + nominal
            val isInsufficient = nominal > currentBalance

            Surface(
                color = if (isInsufficient) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isInsufficient) Color(0xFFFCA5A5) else Color(0xFF86EFAC)),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isInsufficient) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Saldo Dompet Asal tidak mencukupi!",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Simulasi Sisa Perpindahan Saldo:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimasi Sisa Asal ($sourceName):",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isInsufficient) Color(0xFF991B1B) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatRupiah(remainingSource),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isInsufficient) Color(0xFFDC2626) else Color(0xFF0F766E)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimasi Saldo Tujuan ($targetName):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatRupiah(newTarget),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }
        }
        "Uang Masuk" -> {
            val newBalance = currentBalance + nominal
            Surface(
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                modifier = modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Estimasi Saldo Setelah Masuk:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = formatRupiah(newBalance),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                }
            }
        }
    }
}

data class QuickMutationConfig(
    val type: String, // "Uang Masuk", "Uang Keluar", "Pindah Saldo"
    val accountId: Int
)

@Composable
fun QuickMutationDialog(
    initialType: String = "Uang Keluar",
    initialAccountId: Int,
    accounts: List<MasterAkunSaldo>,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val accountBalances = remember(summary) { summary.rows.associate { it.idAkun to it.sisaSaldoRiil } }

    var tanggal by remember { mutableStateOf(viewModel.getTodayString()) }
    var mutationType by remember { mutableStateOf(initialType) }

    val initialAccountIdx = remember(accounts, initialAccountId) {
        val idx = accounts.indexOfFirst { it.idAkun == initialAccountId }
        if (idx >= 0) idx else 0
    }

    var selectedAccountIndex by remember(initialAccountIdx) { mutableIntStateOf(initialAccountIdx) }
    var selectedSourceAccountIndex by remember(initialAccountIdx) { mutableIntStateOf(initialAccountIdx) }
    var selectedTargetAccountIndex by remember(initialAccountIdx, accounts) {
        val targetIdx = if (initialAccountIdx == 0 && accounts.size > 1) 1 else 0
        mutableIntStateOf(targetIdx)
    }

    var nominalText by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }
    var showErrorAlert by remember { mutableStateOf(false) }

    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }
    var targetDropdownExpanded by remember { mutableStateOf(false) }

    val showDatePicker = {
        val parts = tanggal.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) - 1
        val day = parts.getOrNull(2)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        val dpd = android.app.DatePickerDialog(context, { _, y, m, d ->
            tanggal = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
        }, year, month, day)
        dpd.show()
    }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val headerIcon = when (mutationType) {
                    "Uang Masuk" -> Icons.Default.AddCircle
                    "Pindah Saldo" -> Icons.Default.SwapHoriz
                    else -> Icons.Default.RemoveCircle
                }
                val headerColor = when (mutationType) {
                    "Uang Masuk" -> Color(0xFF2E7D32)
                    "Pindah Saldo" -> Color(0xFF6B46C1)
                    else -> Color(0xFFC62828)
                }
                Icon(
                    imageVector = headerIcon,
                    contentDescription = null,
                    tint = headerColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = when (mutationType) {
                        "Uang Masuk" -> "Input Uang Masuk"
                        "Pindah Saldo" -> "Pindah Saldo / Mutasi"
                        else -> "Input Uang Keluar (Pengeluaran)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Tanggal
                OutlinedTextField(
                    value = tanggal,
                    onValueChange = { tanggal = it },
                    label = { Text("Tanggal (YYYY-MM-DD)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker() }
                        .testTag("dialog_mutation_tanggal"),
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { showDatePicker() }
                        )
                    },
                    colors = customFieldColors,
                    shape = customFieldShape,
                    singleLine = true
                )

                // 2. Jenis Mutasi Selector (Filter Chips)
                Column {
                    Text(
                        "Jenis Mutasi",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ElevatedFilterChip(
                            selected = mutationType == "Uang Keluar",
                            onClick = { mutationType = "Uang Keluar" },
                            label = { Text("Keluar", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f).testTag("dialog_chip_keluar"),
                            leadingIcon = if (mutationType == "Uang Keluar") {
                                { Icon(Icons.Default.RemoveCircle, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(12.dp)) }
                            } else null
                        )
                        ElevatedFilterChip(
                            selected = mutationType == "Uang Masuk",
                            onClick = { mutationType = "Uang Masuk" },
                            label = { Text("Masuk", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f).testTag("dialog_chip_masuk"),
                            leadingIcon = if (mutationType == "Uang Masuk") {
                                { Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp)) }
                            } else null
                        )
                        ElevatedFilterChip(
                            selected = mutationType == "Pindah Saldo",
                            onClick = { mutationType = "Pindah Saldo" },
                            label = { Text("Mutasi", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f).testTag("dialog_chip_transfer"),
                            leadingIcon = if (mutationType == "Pindah Saldo") {
                                { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(12.dp)) }
                            } else null
                        )
                    }
                }

                // 3. Akun Selector with Live Balance Badges
                val sourceAccount = accounts.getOrNull(selectedSourceAccountIndex) ?: accounts.firstOrNull()
                val targetAccount = accounts.getOrNull(selectedTargetAccountIndex) ?: accounts.getOrNull(1) ?: accounts.firstOrNull()
                val currentAccount = accounts.getOrNull(selectedAccountIndex) ?: accounts.firstOrNull()

                val sourceBalance = sourceAccount?.let { accountBalances[it.idAkun] } ?: 0.0
                val targetBalance = targetAccount?.let { accountBalances[it.idAkun] } ?: 0.0
                val currentBalance = currentAccount?.let { accountBalances[it.idAkun] } ?: 0.0

                if (accounts.isNotEmpty()) {
                    if (mutationType == "Pindah Saldo" && sourceAccount != null && targetAccount != null) {
                        // Dompet Asal
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = sourceAccount.namaAkun,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Dompet Asal (Dikurangi)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { sourceDropdownExpanded = true }
                                        .testTag("dialog_mutation_asal"),
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
                                    onDismissRequest = { sourceDropdownExpanded = false }
                                ) {
                                    accounts.forEachIndexed { index, account ->
                                        val bal = accountBalances[account.idAkun] ?: 0.0
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(account.namaAkun, fontWeight = FontWeight.Medium)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        formatRupiah(bal),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (bal < 0) Color(0xFFDC2626) else colorScheme.primary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedSourceAccountIndex = index
                                                sourceDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            AccountBalanceBadge(
                                label = "Saldo Tersedia (${sourceAccount.namaAkun}):",
                                balance = sourceBalance,
                                tintColor = Color(0xFFDC2626)
                            )
                        }

                        // Dompet Tujuan
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = targetAccount.namaAkun,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Dompet Tujuan (Ditambah)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { targetDropdownExpanded = true }
                                        .testTag("dialog_mutation_tujuan"),
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
                                    onDismissRequest = { targetDropdownExpanded = false }
                                ) {
                                    accounts.forEachIndexed { index, account ->
                                        val bal = accountBalances[account.idAkun] ?: 0.0
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(account.namaAkun, fontWeight = FontWeight.Medium)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        formatRupiah(bal),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (bal < 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedTargetAccountIndex = index
                                                targetDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            AccountBalanceBadge(
                                label = "Saldo Saat Ini (${targetAccount.namaAkun}):",
                                balance = targetBalance,
                                tintColor = Color(0xFF16A34A)
                            )
                        }
                    } else if (currentAccount != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = currentAccount.namaAkun,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Pilih Pos Akun Saldo") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { accountDropdownExpanded = true }
                                        .testTag("dialog_mutation_akun"),
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
                                    onDismissRequest = { accountDropdownExpanded = false }
                                ) {
                                    accounts.forEachIndexed { index, account ->
                                        val bal = accountBalances[account.idAkun] ?: 0.0
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(account.namaAkun, fontWeight = FontWeight.Medium)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        formatRupiah(bal),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (bal < 0) Color(0xFFDC2626) else colorScheme.primary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedAccountIndex = index
                                                accountDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            AccountBalanceBadge(
                                label = "Saldo Tersedia (${currentAccount.namaAkun}):",
                                balance = currentBalance,
                                tintColor = if (mutationType == "Uang Keluar") Color(0xFFDC2626) else Color(0xFF16A34A)
                            )
                        }
                    }
                }

                // 4. Nominal Mutasi + Real-Time Simulation
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = nominalText,
                        onValueChange = { nominalText = it; showErrorAlert = false },
                        label = { Text("Nominal Mutasi (Rp)") },
                        placeholder = { Text("Contoh: 50.000 atau 150000") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_mutation_nominal"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = customFieldColors,
                        shape = customFieldShape,
                        singleLine = true
                    )

                    val parsedNominal = parseDoubleInput(nominalText) ?: 0.0
                    EstimatedBalanceSimulationCard(
                        mutationType = mutationType,
                        currentBalance = if (mutationType == "Pindah Saldo") sourceBalance else currentBalance,
                        nominal = parsedNominal,
                        sourceName = if (mutationType == "Pindah Saldo") (sourceAccount?.namaAkun ?: "") else (currentAccount?.namaAkun ?: ""),
                        targetName = if (mutationType == "Pindah Saldo") (targetAccount?.namaAkun ?: "") else "",
                        targetBalance = targetBalance
                    )
                }

                // 5. Keterangan Mutasi
                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it; showErrorAlert = false },
                    label = { Text("Keterangan Mutasi") },
                    placeholder = { Text("Contoh: Beli bahan, Pembayaran operasional, Pindah saldo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_mutation_keterangan"),
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
                        "Harap isi nominal angka dengan valid (> 0) dan keterangan!"
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
                    val nominal = parseDoubleInput(nominalText)
                    val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    if (mutationType == "Pindah Saldo") {
                        val srcAcc = accounts.getOrNull(selectedSourceAccountIndex)
                        val tgtAcc = accounts.getOrNull(selectedTargetAccountIndex)
                        if (srcAcc != null && tgtAcc != null && nominal != null && nominal > 0.0 && keterangan.isNotBlank()) {
                            if (srcAcc.idAkun == tgtAcc.idAkun) {
                                showErrorAlert = true
                                return@Button
                            }
                            viewModel.insertMutation(
                                tanggal = tanggal,
                                idAkun = srcAcc.idAkun,
                                jenis = "Pindah Saldo",
                                nominal = nominal,
                                keterangan = keterangan,
                                idAkunTujuan = tgtAcc.idAkun,
                                waktu = now
                            )
                            Toast.makeText(context, "Mutasi kas berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            onDismiss()
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
                                keterangan = keterangan,
                                waktu = now
                            )
                            Toast.makeText(context, "Mutasi kas berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            showErrorAlert = true
                        }
                    }
                },
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.testTag("dialog_submit_mutation_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan Mutasi", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}'''

# Replace QuickMutationDialog in text
quick_pattern = r'data class QuickMutationConfig\(.*?fun QuickMutationDialog\(.*?confirmButton = \{.*?\n\s*\}\s*\n\s*\)'
text = re.sub(quick_pattern, helper_and_quick, text, flags=re.DOTALL)

# 2. MutationsTab replacement
new_mutations_tab = '''@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutationsTab(
    mutations: List<MutasiManualKeluarMasuk>,
    accounts: List<MasterAkunSaldo>,
    viewModel: FinanceViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val accountBalances = remember(summary) { summary.rows.associate { it.idAkun to it.sisaSaldoRiil } }

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

                    // Jenis Mutasi Selector
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

                    // Account Selection with Live Balance Badges
                    val sourceAccount = accounts.getOrNull(selectedSourceAccountIndex) ?: accounts.firstOrNull()
                    val targetAccount = accounts.getOrNull(selectedTargetAccountIndex) ?: accounts.getOrNull(1) ?: accounts.firstOrNull()
                    val currentAccount = accounts.getOrNull(selectedAccountIndex) ?: accounts.firstOrNull()

                    val sourceBalance = sourceAccount?.let { accountBalances[it.idAkun] } ?: 0.0
                    val targetBalance = targetAccount?.let { accountBalances[it.idAkun] } ?: 0.0
                    val currentBalance = currentAccount?.let { accountBalances[it.idAkun] } ?: 0.0

                    if (accounts.isNotEmpty()) {
                        if (mutationType == "Pindah Saldo" && sourceAccount != null && targetAccount != null) {
                            var sourceDropdownExpanded by remember { mutableStateOf(false) }
                            var targetDropdownExpanded by remember { mutableStateOf(false) }

                            // Dompet Asal
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                            val bal = accountBalances[account.idAkun] ?: 0.0
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(account.namaAkun, fontWeight = FontWeight.Medium)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            formatRupiah(bal),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (bal < 0) Color(0xFFDC2626) else colorScheme.primary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedSourceAccountIndex = index
                                                    sourceDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                AccountBalanceBadge(
                                    label = "Saldo Tersedia (${sourceAccount.namaAkun}):",
                                    balance = sourceBalance,
                                    tintColor = Color(0xFFDC2626)
                                )
                            }

                            // Dompet Tujuan
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                            val bal = accountBalances[account.idAkun] ?: 0.0
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(account.namaAkun, fontWeight = FontWeight.Medium)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            formatRupiah(bal),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (bal < 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedTargetAccountIndex = index
                                                    targetDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                AccountBalanceBadge(
                                    label = "Saldo Saat Ini (${targetAccount.namaAkun}):",
                                    balance = targetBalance,
                                    tintColor = Color(0xFF16A34A)
                                )
                            }
                        } else if (currentAccount != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                            val bal = accountBalances[account.idAkun] ?: 0.0
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(account.namaAkun, fontWeight = FontWeight.Medium)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            formatRupiah(bal),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (bal < 0) Color(0xFFDC2626) else colorScheme.primary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedAccountIndex = index
                                                    accountDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                AccountBalanceBadge(
                                    label = "Saldo Tersedia (${currentAccount.namaAkun}):",
                                    balance = currentBalance,
                                    tintColor = if (mutationType == "Uang Keluar") Color(0xFFDC2626) else Color(0xFF16A34A)
                                )
                            }
                        }
                    }

                    // Nominal Mutasi + Simulation
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

                        val parsedNominal = parseDoubleInput(nominalText) ?: 0.0
                        EstimatedBalanceSimulationCard(
                            mutationType = mutationType,
                            currentBalance = if (mutationType == "Pindah Saldo") sourceBalance else currentBalance,
                            nominal = parsedNominal,
                            sourceName = if (mutationType == "Pindah Saldo") (sourceAccount?.namaAkun ?: "") else (currentAccount?.namaAkun ?: ""),
                            targetName = if (mutationType == "Pindah Saldo") (targetAccount?.namaAkun ?: "") else "",
                            targetBalance = targetBalance
                        )
                    }

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
                                val srcAcc = accounts.getOrNull(selectedSourceAccountIndex)
                                val tgtAcc = accounts.getOrNull(selectedTargetAccountIndex)
                                if (srcAcc != null && tgtAcc != null && nominal != null && nominal > 0.0 && keterangan.isNotBlank()) {
                                    if (srcAcc.idAkun == tgtAcc.idAkun) {
                                        showErrorAlert = true
                                        return@Button
                                    }
                                    viewModel.insertMutation(
                                        tanggal = tanggal,
                                        idAkun = srcAcc.idAkun,
                                        jenis = "Pindah Saldo",
                                        nominal = nominal,
                                        keterangan = keterangan,
                                        idAkunTujuan = tgtAcc.idAkun
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
        }'''

mutations_tab_pattern = r'@OptIn\(ExperimentalMaterial3Api::class\)\s*@Composable\s*fun MutationsTab\(.*?// List Header'
text = re.sub(mutations_tab_pattern, new_mutations_tab + '\n\n        // List Header', text, flags=re.DOTALL)

# 3. EditMutationDialog update
new_edit_dialog = '''@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMutationDialog(
    mutation: MutasiManualKeluarMasuk,
    accounts: List<MasterAkunSaldo>,
    viewModel: FinanceViewModel? = null,
    onDismiss: () -> Unit,
    onSave: (MutasiManualKeluarMasuk) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val summary by viewModel?.dashboardSummary?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    val accountBalances = remember(summary) { summary?.rows?.associate { it.idAkun to it.sisaSaldoRiil } ?: emptyMap() }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = colorScheme.primary)
                Text("Edit Mutasi Manual Kas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
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

                val srcAcc = accounts.getOrNull(selectedSourceIdx) ?: accounts.firstOrNull()
                val tgtAcc = accounts.getOrNull(selectedTargetIdx) ?: accounts.getOrNull(1) ?: accounts.firstOrNull()
                val srcBal = srcAcc?.let { accountBalances[it.idAkun] } ?: 0.0
                val tgtBal = tgtAcc?.let { accountBalances[it.idAkun] } ?: 0.0

                if (accounts.isNotEmpty()) {
                    if (editJenis == "Pindah Saldo" && srcAcc != null && tgtAcc != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                        val bal = accountBalances[acc.idAkun] ?: 0.0
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(acc.namaAkun)
                                                    if (accountBalances.isNotEmpty()) {
                                                        Text(
                                                            formatRupiah(bal),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (bal < 0) Color(0xFFDC2626) else colorScheme.primary
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedSourceIdx = idx
                                                sourceExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (accountBalances.isNotEmpty()) {
                                AccountBalanceBadge(
                                    label = "Saldo Tersedia (${srcAcc.namaAkun}):",
                                    balance = srcBal,
                                    tintColor = Color(0xFFDC2626)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                        val bal = accountBalances[acc.idAkun] ?: 0.0
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(acc.namaAkun)
                                                    if (accountBalances.isNotEmpty()) {
                                                        Text(
                                                            formatRupiah(bal),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (bal < 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedTargetIdx = idx
                                                targetExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (accountBalances.isNotEmpty()) {
                                AccountBalanceBadge(
                                    label = "Saldo Saat Ini (${tgtAcc.namaAkun}):",
                                    balance = tgtBal,
                                    tintColor = Color(0xFF16A34A)
                                )
                            }
                        }
                    } else if (srcAcc != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = srcAcc.namaAkun,
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
                                        val bal = accountBalances[acc.idAkun] ?: 0.0
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(acc.namaAkun)
                                                    if (accountBalances.isNotEmpty()) {
                                                        Text(
                                                            formatRupiah(bal),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (bal < 0) Color(0xFFDC2626) else colorScheme.primary
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedSourceIdx = idx
                                                accountExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (accountBalances.isNotEmpty()) {
                                AccountBalanceBadge(
                                    label = "Saldo Tersedia (${srcAcc.namaAkun}):",
                                    balance = srcBal,
                                    tintColor = if (editJenis == "Uang Keluar") Color(0xFFDC2626) else Color(0xFF16A34A)
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

                    val parsedNominal = parseDoubleInput(editNominalText) ?: 0.0
                    if (accountBalances.isNotEmpty()) {
                        EstimatedBalanceSimulationCard(
                            mutationType = editJenis,
                            currentBalance = srcBal,
                            nominal = parsedNominal,
                            sourceName = srcAcc?.namaAkun ?: "",
                            targetName = if (editJenis == "Pindah Saldo") (tgtAcc?.namaAkun ?: "") else "",
                            targetBalance = tgtBal
                        )
                    }
                }

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
}'''

# Replace EditMutationDialog
edit_dialog_pattern = r'@OptIn\(ExperimentalMaterial3Api::class\)\s*@Composable\s*fun EditMutationDialog\(.*?dismissButton = \{.*?\n\s*\}\s*\n\s*\)'
text = re.sub(edit_dialog_pattern, new_edit_dialog, text, flags=re.DOTALL)

# Also update the call to EditMutationDialog in MutationsTab to pass viewModel
text = text.replace('EditMutationDialog(\n            mutation = editingMutation!!,\n            accounts = accounts,\n            onDismiss =', 'EditMutationDialog(\n            mutation = editingMutation!!,\n            accounts = accounts,\n            viewModel = viewModel,\n            onDismiss =')

with open('app/src/main/java/com/example/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print("Update completed successfully!")
