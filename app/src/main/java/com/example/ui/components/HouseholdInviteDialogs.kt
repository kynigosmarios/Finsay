package com.example.ui.components

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.model.HouseholdInvite
import com.example.data.model.HouseholdRole
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import com.example.util.QrCodeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun HouseholdInviteQrDialog(
    invite: HouseholdInvite,
    appLanguage: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lang = Localization.Language.fromCode(appLanguage)
    val qrBitmap = remember(invite.url) { QrCodeHelper.generateQrBitmap(invite.url, 560) }
    val role = remember(invite.role) { HouseholdRole.fromRaw(invite.role, isCreator = false) }

    fun copyToClipboard(text: String) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Finsay Invite", text))
            Toast.makeText(
                context,
                Localization.tr(lang, "Invite link copied!", "Ο σύνδεσμος πρόσκλησης αντιγράφηκε!"),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Throwable) {}
    }

    fun shareInviteLink() {
        val shareText = Localization.trFormat(
            lang,
            "Join my household book \"%1\$s\" on Finsay!\n\nLink: %2\$s\nCode: %3\$s",
            "Συνδέσου στο εξοδολόγιο «%1\$s» στο Finsay!\n\nΣύνδεσμος: %2\$s\nΚωδικός: %3\$s",
            invite.householdName,
            invite.url,
            invite.code
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Finsay — ${invite.householdName}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        try {
            context.startActivity(
                Intent.createChooser(
                    intent,
                    Localization.tr(lang, "Share Invite", "Κοινοποίηση Πρόσκλησης")
                )
            )
        } catch (_: Throwable) {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Localization.tr(lang, "Invite Link & QR", "Πρόσκληση με Link & QR"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = Localization.translateHouseholdName(invite.householdName, lang),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HouseholdRoleBadge(role = role, lang = lang)

                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = Localization.tr(lang, "Household invite QR code", "Κωδικός QR πρόσκλησης"),
                            modifier = Modifier
                                .size(210.dp)
                                .padding(12.dp)
                                .testTag("household_invite_qr_image")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Emerald700.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = Localization.tr(lang, "INVITE CODE", "ΚΩΔΙΚΟΣ ΠΡΟΣΚΛΗΣΗΣ"),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald700,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = invite.code,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("household_invite_code_text")
                        )
                    }
                }

                Text(
                    text = Localization.tr(
                        lang,
                        "Valid for 7 days. Scan the QR code or share the link so another person can join directly.",
                        "Ισχύει για 7 ημέρες. Σκανάρετε το QR ή στείλτε τον σύνδεσμο για άμεση σύνδεση στο εξοδολόγιο."
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { copyToClipboard(invite.url) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.45f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("household_invite_copy_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Localization.tr(lang, "Copy Link", "Αντιγραφή"),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald700
                        )
                    }

                    Button(
                        onClick = { shareInviteLink() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald700,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("household_invite_share_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Localization.share(lang),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Localization.close(lang), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun JoinHouseholdByCodeOrQrDialog(
    appLanguage: String,
    onJoinWithCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = Localization.Language.fromCode(appLanguage)

    var codeInput by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    fun scanFromUri(uri: Uri, tempFileToDelete: File? = null) {
        scope.launch {
            isScanning = true
            scanError = null
            val extracted = try {
                withContext(Dispatchers.IO) {
                    QrCodeHelper.scanInviteCodeFromUri(context, uri)
                }
            } finally {
                try { tempFileToDelete?.delete() } catch (_: Throwable) {}
            }
            isScanning = false
            if (!extracted.isNullOrBlank()) {
                codeInput = extracted
                onJoinWithCode(extracted)
                onDismiss()
            } else {
                scanError = Localization.tr(
                    lang,
                    "No valid Finsay invite QR code found",
                    "Δεν βρέθηκε έγκυρος κωδικός QR πρόσκλησης Finsay"
                )
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCaptureFile
        val uri = pendingCaptureUri
        pendingCaptureFile = null
        pendingCaptureUri = null
        if (success && file != null && file.exists()) {
            val targetUri = uri ?: Uri.fromFile(file)
            scanFromUri(targetUri, tempFileToDelete = file)
        } else {
            try { file?.delete() } catch (_: Throwable) {}
        }
    }

    fun launchCameraQrCapture() {
        try {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val tempFile = File(dir, "qr_invite_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            pendingCaptureFile = tempFile
            pendingCaptureUri = uri
            takePictureLauncher.launch(uri)
        } catch (_: Throwable) {
            scanError = Localization.tr(lang, "Camera unavailable", "Η κάμερα δεν είναι διαθέσιμη")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraQrCapture()
        } else {
            scanError = Localization.tr(
                lang,
                "Camera permission is required to scan QR codes",
                "Απαιτείται άδεια κάμερας για σάρωση κωδικών QR"
            )
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scanFromUri(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = Localization.tr(lang, "Join via Link or QR", "Σύνδεση με Link ή QR"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Localization.tr(
                        lang,
                        "Paste an invite link, type the invite code, or scan a Finsay QR code.",
                        "Επικολλήστε τον σύνδεσμο πρόσκλησης, πληκτρολογήστε τον κωδικό ή σκανάρετε το QR."
                    ),
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it
                        scanError = null
                    },
                    placeholder = { Text("FSY-XXXXXXXX", fontSize = 13.5.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Emerald700),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_household_code_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasCam = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCam) {
                                launchCameraQrCapture()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        enabled = !isScanning,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.45f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("join_scan_qr_camera_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = Localization.tr(lang, "Scan QR", "Σάρωση QR"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald700
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isScanning,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.45f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("join_pick_qr_gallery_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = Localization.tr(lang, "From Photo", "Από Φωτό"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald700
                        )
                    }
                }

                if (isScanning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Emerald700
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.tr(lang, "Scanning QR code...", "Σάρωση QR..."),
                            fontSize = 12.sp,
                            color = Emerald700
                        )
                    }
                }

                scanError?.let { err ->
                    Text(
                        text = err,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = codeInput.trim()
                    if (trimmed.isNotBlank()) {
                        onJoinWithCode(trimmed)
                        onDismiss()
                    }
                },
                enabled = codeInput.isNotBlank() && !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("join_household_confirm_btn")
            ) {
                Text(Localization.tr(lang, "Join", "Σύνδεση"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Localization.cancel(lang))
            }
        }
    )
}

@Composable
fun IncomingInviteConfirmDialog(
    inviteCode: String,
    appLanguage: String,
    onConfirmJoin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = Localization.tr(lang, "Household Invitation", "Πρόσκληση Εξοδολογίου"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = Localization.tr(
                        lang,
                        "You received an invitation to join a shared Finsay household book.",
                        "Λάβατε πρόσκληση για σύνδεση σε ένα κοινό εξοδολόγιο Finsay."
                    ),
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Emerald700.copy(alpha = 0.10f))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inviteCode,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Emerald700
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmJoin(inviteCode) },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("incoming_invite_join_btn")
            ) {
                Text(Localization.tr(lang, "Join", "Σύνδεση"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Localization.cancel(lang))
            }
        }
    )
}
