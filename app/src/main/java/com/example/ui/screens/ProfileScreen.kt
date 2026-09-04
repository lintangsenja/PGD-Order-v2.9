package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.UserProfile
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun ProfileAvatar(
    userProfile: UserProfile,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarBitmap = remember(userProfile.avatarUri) {
        if (userProfile.avatarUri.isNotBlank() && (userProfile.avatarUri.startsWith("data:image") || userProfile.avatarUri.length > 100)) {
            ImageUtils.decodeBase64ToBitmap(userProfile.avatarUri)
        } else null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF8E7AB5), Color(0xFFB5A1E5))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap.asImageBitmap(),
                contentDescription = "Foto Profil ${userProfile.adminName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else if (userProfile.avatarUri.isNotBlank() && !userProfile.avatarUri.startsWith("data:image")) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(userProfile.avatarUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Foto Profil ${userProfile.adminName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val initial = userProfile.adminName.trim().take(1).uppercase(Locale.getDefault()).ifBlank { "P" }
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (size.value * 0.45f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
fun ProfileScreen(
    viewModel: FinanceViewModel,
    onOpenLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isCloudOnline by viewModel.isCloudOnline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncStatusText by viewModel.syncStatusText.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.cloudLastSyncTime.collectAsStateWithLifecycle()
    val currentUserEmail = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: "" }

    var adminName by remember(userProfile.adminName) { mutableStateOf(userProfile.adminName) }
    var tagline by remember(userProfile.tagline) { mutableStateOf(userProfile.tagline) }
    var avatarUri by remember(userProfile.avatarUri) { mutableStateOf(userProfile.avatarUri) }
    var isProcessingImage by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isProcessingImage = true
            scope.launch(Dispatchers.IO) {
                val base64 = ImageUtils.uriToBase64(context, uri, maxDimension = 300, quality = 80)
                withContext(Dispatchers.Main) {
                    isProcessingImage = false
                    if (!base64.isNullOrBlank()) {
                        avatarUri = base64
                        // Instantly update ViewModel and Cloud Firestore
                        viewModel.saveUserProfile(
                            adminName = adminName,
                            tagline = tagline,
                            avatarType = userProfile.avatarType,
                            avatarUri = base64
                        )
                        Toast.makeText(context, "Foto profil berhasil diunggah & disinkronkan!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal mengonversi file gambar.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2D9F3)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        ProfileAvatar(
                            userProfile = userProfile.copy(avatarUri = avatarUri, adminName = adminName),
                            size = 96.dp
                        )
                        if (isProcessingImage) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }
                        IconButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6A4C93))
                                .testTag("profile_change_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Ganti Foto Profil",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (avatarUri.isNotBlank()) {
                        TextButton(
                            onClick = {
                                avatarUri = ""
                                viewModel.saveUserProfile(
                                    adminName = adminName,
                                    tagline = tagline,
                                    avatarType = userProfile.avatarType,
                                    avatarUri = ""
                                )
                                Toast.makeText(context, "Foto profil dihapus.", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Hapus Foto",
                                color = Color(0xFFC62828),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Text(
                        text = adminName.ifBlank { "Profil Admin" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D1E4B)
                    )

                    OutlinedTextField(
                        value = adminName,
                        onValueChange = { adminName = it },
                        label = { Text("Nama Pengguna / Admin") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_profile_admin_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = tagline,
                        onValueChange = { tagline = it },
                        label = { Text("Tagline / Instansi") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_profile_tagline"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.saveUserProfile(
                                adminName = adminName,
                                tagline = tagline,
                                avatarType = userProfile.avatarType,
                                avatarUri = avatarUri
                            )
                            Toast.makeText(context, "Profil berhasil diperbarui & disinkronkan!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("button_save_profile"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2D9F3)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Akun & Sinkronisasi Cloud",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B2369)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (currentUserEmail.isNotBlank()) "Terhubung: $currentUserEmail" else "Mode Offline / Tamu",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2D1E4B)
                            )
                            Text(
                                text = if (isCloudOnline) "Status: Online ($syncStatusText)" else "Status: Offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCloudOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Text(
                                text = "Terakhir Sinkron: $lastSyncTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6B5B95)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("button_account_login_logout"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (currentUserEmail.isNotBlank()) Icons.Default.Logout else Icons.Default.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentUserEmail.isNotBlank()) "Kelola Akun / Keluar" else "Masuk Akun Google",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
