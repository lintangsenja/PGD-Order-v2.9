import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

quick_mutation_dialog_code = '''
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

                // 3. Akun Selector (Single or Source + Target)
                if (accounts.isNotEmpty()) {
                    if (mutationType == "Pindah Saldo") {
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
                }

                // 4. Nominal Mutasi
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
                                idAkunTujuan = targetAccount.idAkun,
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
}
'''

new_wallet_envelopes_code = '''@Composable
fun WalletEnvelopesSection(
    rows: List<AccountDashboardRow>,
    viewModel: FinanceViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle(emptyList())
    var activeQuickMutation by remember { mutableStateOf<QuickMutationConfig?>(null) }
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
                        text = "Alokasi otomatis & aksi cepat mutasi/pengeluaran",
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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

                                Text(
                                    text = formatRupiah(row.sisaSaldoRiil),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (row.sisaSaldoRiil >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }

                            // Quick Action Buttons (Masuk, Keluar, Mutasi)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. Masuk
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            activeQuickMutation = QuickMutationConfig(
                                                type = "Uang Masuk",
                                                accountId = row.idAkun
                                            )
                                        }
                                        .testTag("btn_quick_masuk_${row.namaAkun.replace(" ", "_").lowercase()}"),
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Masuk", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }
                                }

                                // 2. Keluar
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            activeQuickMutation = QuickMutationConfig(
                                                type = "Uang Keluar",
                                                accountId = row.idAkun
                                            )
                                        }
                                        .testTag("btn_quick_keluar_${row.namaAkun.replace(" ", "_").lowercase()}"),
                                    color = Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.RemoveCircle, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Keluar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                    }
                                }

                                // 3. Mutasi (Pindah Saldo)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            activeQuickMutation = QuickMutationConfig(
                                                type = "Pindah Saldo",
                                                accountId = row.idAkun
                                            )
                                        }
                                        .testTag("btn_quick_transfer_${row.namaAkun.replace(" ", "_").lowercase()}"),
                                    color = colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Mutasi", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeQuickMutation != null) {
        QuickMutationDialog(
            initialType = activeQuickMutation!!.type,
            initialAccountId = activeQuickMutation!!.accountId,
            accounts = allAccounts,
            viewModel = viewModel,
            onDismiss = { activeQuickMutation = null }
        )
    }

    if (selectedPosKasDetail != null) {
        DetailLedgerDialog(
            account = selectedPosKasDetail!!,
            viewModel = viewModel,
            onDismiss = { selectedPosKasDetail = null }
        )
    }
}
'''

# 1. Insert QuickMutationDialog before DompetScreen if not present
if 'data class QuickMutationConfig' not in text:
    text = text.replace('@Composable\nfun DompetScreen(', quick_mutation_dialog_code + '\n@Composable\nfun DompetScreen(')

# 2. Replace WalletEnvelopesSection
pattern = r'@Composable\s+fun WalletEnvelopesSection\(rows: List<AccountDashboardRow>,\s*viewModel: FinanceViewModel\)\s*\{.*?if \(selectedPosKasDetail != null\) \{.*?\}\s*\}'
match = re.search(pattern, text, re.DOTALL)
if match:
    text = text[:match.start()] + new_wallet_envelopes_code + text[match.end():]
    print('WalletEnvelopesSection replaced!')
else:
    print('WalletEnvelopesSection regex did not match, trying fallback')

# 3. Update DetailLedgerDialog:
# Replace quickActionType state with activeQuickMutation
text = text.replace('var quickActionType by remember { mutableStateOf<String?>(null) }', 'var activeQuickMutation by remember { mutableStateOf<QuickMutationConfig?>(null) }')

# Replace the 3 buttons in DetailLedgerDialog
text = text.replace('onClick = { quickActionType = "Masuk" }', 'onClick = { activeQuickMutation = QuickMutationConfig("Uang Masuk", account.idAkun) }')
text = text.replace('onClick = { quickActionType = "Keluar" }', 'onClick = { activeQuickMutation = QuickMutationConfig("Uang Keluar", account.idAkun) }')
text = text.replace('onClick = { quickActionType = "Transfer" }', 'onClick = { activeQuickMutation = QuickMutationConfig("Pindah Saldo", account.idAkun) }')

# Replace old quickActionType dialog in DetailLedgerDialog with QuickMutationDialog
old_detail_dialog_pattern = r'if \(quickActionType != null\) \{.*?TextButton\(onClick = \{ quickActionType = null \}\) \{\s*Text\("Batal"\)\s*\}\s*\}\s*\)\s*\}'
new_detail_dialog = '''if (activeQuickMutation != null) {
        QuickMutationDialog(
            initialType = activeQuickMutation!!.type,
            initialAccountId = activeQuickMutation!!.accountId,
            accounts = allAccounts,
            viewModel = viewModel,
            onDismiss = { activeQuickMutation = null }
        )
    }'''

match_dialog = re.search(old_detail_dialog_pattern, text, re.DOTALL)
if match_dialog:
    text = text[:match_dialog.start()] + new_detail_dialog + text[match_dialog.end():]
    print('DetailLedgerDialog updated to QuickMutationDialog!')
else:
    print('DetailLedgerDialog regex did not match directly')

with open('app/src/main/java/com/example/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print('Update complete!')
