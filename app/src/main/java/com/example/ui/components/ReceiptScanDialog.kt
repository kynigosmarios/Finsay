package com.example.ui.components

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald700
import com.example.util.DefaultPayerHelper
import com.example.util.Localization
import com.example.util.ParsedReceiptData
import com.example.util.ReceiptParser
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptScanDialog(
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
    isPro: Boolean,
    appLanguage: String,
    defaultPayerMember: FamilyMember? = null,
    defaultPayerMembers: List<FamilyMember> = listOfNotNull(defaultPayerMember),
    onSuggestCategory: ((String, String) -> ExpenseCategory?)? = null,
    onLearnCategory: ((String, Long, String, String) -> Unit)? = null,
    onShowPremiumPaywall: (String) -> Unit,
    onExpenseConfirmed: (
        title: String,
        amount: Double,
        category: ExpenseCategory,
        timestamp: Long,
        member: FamilyMember?,
        note: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lang = Localization.Language.fromCode(appLanguage)

    var isProcessing by remember { mutableStateOf(false) }
    var scannedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var parsedData by remember { mutableStateOf<ParsedReceiptData?>(null) }
    var scanStatusMessage by remember { mutableStateOf<String?>(null) }

    // Editable fields once parsed
    var titleInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    val validDefaultPayers = remember(members, defaultPayerMembers, defaultPayerMember) {
        val activeIds = members.map { it.id }.toSet()
        val list = defaultPayerMembers.filter { it.id in activeIds }.distinctBy { it.id }
        if (list.isNotEmpty()) list else listOfNotNull(defaultPayerMember?.takeIf { it.id in activeIds })
    }
    var useDefaultMultiPayers by remember(members, defaultPayerMembers, defaultPayerMember) {
        mutableStateOf(validDefaultPayers.size >= 2)
    }
    var selectedMember by remember(members, defaultPayerMember) {
        mutableStateOf(defaultPayerMember?.takeIf { dp -> members.any { it.id == dp.id } } ?: members.firstOrNull())
    }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showMemberMenu by remember { mutableStateOf(false) }

    fun processBitmap(bitmap: Bitmap) {
        if (!isPro) {
            onShowPremiumPaywall("ocr_receipt")
            return
        }

        isProcessing = true
        scannedBitmap = bitmap
        scanStatusMessage = Localization.tr(lang, "Analyzing receipt with AI & OCR...", "Ανάλυση απόδειξης με AI & OCR...")

        coroutineScope.launch {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)

                // 1. Scan QR code for AADE verification & seller/amount metadata
                val aadeQr = com.example.util.ReceiptAmountAndQrParser.extractBestQrCode(bitmap)
                val resolvedQrData = if (!aadeQr.isNullOrBlank()) {
                    com.example.util.ReceiptAmountAndQrParser.resolveAadeQrData(aadeQr)
                } else {
                    null
                }

                // 2. OCR Text Recognition (always runs to extract store name & total as fallback/complement)
                val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val visionText = withContext(Dispatchers.IO) {
                    textRecognizer.process(inputImage).await()
                }

                // 3. Parse receipt with full text + spatial bounding boxes + AADE QR metadata + learned category
                val result = ReceiptParser.parseReceipt(
                    visionText = visionText,
                    rawText = visionText.text,
                    aadeQr = aadeQr,
                    availableCategories = categories,
                    learnedCategorySuggester = { merchantTitle, merchantAfm ->
                        onSuggestCategory?.invoke(merchantTitle, merchantAfm)?.name
                    },
                    preResolvedQrData = resolvedQrData
                )

                parsedData = result
                titleInput = result.title
                amountInput = if (result.amount > 0.0) String.format(Locale.US, "%.2f", result.amount) else ""
                selectedTimestamp = result.timestamp
                val matchedCat = categories.find { it.name.equals(result.suggestedCategoryName, ignoreCase = true) }
                    ?: categories.firstOrNull()
                selectedCategory = matchedCat

            } catch (e: Exception) {
                Log.e("ReceiptScan", "OCR failed: ${e.message}", e)
                scanStatusMessage = Localization.tr(lang, "Could not read text. Please retry.", "Δεν ήταν δυνατή η ανάγνωση. Δοκιμάστε ξανά.")
            } finally {
                isProcessing = false
            }
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            processBitmap(bmp)
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            scanStatusMessage = Localization.tr(lang, "Camera permission required", "Απαιτείται άδεια κάμερας για λήψη φωτογραφίας")
        }
    }

    // Photo Picker Launcher (zero-permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                processBitmap(bmp)
            } catch (e: Exception) {
                Log.e("ReceiptScan", "Failed to load picked image: ${e.message}", e)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SmoothDialogContent {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .padding(16.dp)
                .testTag("receipt_scan_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = Localization.tr(lang, "Receipt Scanner", "Σάρωση Απόδειξης"),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Amber700.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "PRO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Amber700,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = Localization.close(lang))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (parsedData == null && !isProcessing) {
                    // Option Buttons: Camera & Gallery
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Localization.tr(
                                    lang,
                                    "Take a photo or choose a receipt from gallery.",
                                    "Φωτογραφίστε ή επιλέξτε απόδειξη από τη συλλογή."
                                ),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!isPro) {
                                            onShowPremiumPaywall("ocr_receipt")
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("scan_camera_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = Localization.tr(lang, "Camera", "Κάμερα"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (!isPro) {
                                            onShowPremiumPaywall("ocr_receipt")
                                        } else {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("scan_gallery_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = Localization.tr(lang, "Gallery", "Συλλογή"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (scanStatusMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = scanStatusMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (isProcessing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = scanStatusMessage ?: Localization.tr(lang, "Reading receipt...", "Ανάγνωση απόδειξης..."),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    // Parsed Results & Form Confirmation
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Thumbnail & Re-scan
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                scannedBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Receipt Preview",
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = Localization.tr(lang, "Review details:", "Ελέγξτε τα στοιχεία:"),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            TextButton(
                                onClick = {
                                    parsedData = null
                                    scannedBitmap = null
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = Localization.tr(lang, "Rescan", "Νέα λήψη"), fontSize = 12.sp)
                            }
                        }

                        // Title field
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text(Localization.tr(lang, "Merchant / Title", "Κατάστημα / Περιγραφή")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Amount field
                        val currencySymbol = com.example.util.LocalCurrencySymbol.current
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it.replace(',', '.') },
                            label = { Text(Localization.trFormat(lang, "Amount (%1\$s)", "Ποσό (%1\$s)", currencySymbol)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )

                        // Category Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showCategoryMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (selectedCategory != null) {
                                        Localization.trFormat(
                                            lang,
                                            "Category: %1\$s",
                                            "Κατηγορία: %1\$s",
                                            Localization.translateCategory(selectedCategory?.name.orEmpty(), lang)
                                        )
                                    } else {
                                        Localization.tr(lang, "Select Category", "Επιλογή Κατηγορίας")
                                    },
                                    fontSize = 13.sp
                                )
                            }
                            DropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(Localization.translateCategory(cat.name, lang)) },
                                        onClick = {
                                            selectedCategory = cat
                                            showCategoryMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Member Dropdown (Who paid)
                        val activeMembers = members
                        if (activeMembers.isNotEmpty()) {
                            val multiDefaultLabel = remember(validDefaultPayers) {
                                if (validDefaultPayers.size >= 2) {
                                    DefaultPayerHelper.buildEqualPayerFields(validDefaultPayers, 0.0).displayName
                                } else ""
                            }
                            val payerDisplayText = when {
                                useDefaultMultiPayers && multiDefaultLabel.isNotBlank() ->
                                    Localization.trFormat(lang, "Paid by: %1\$s", "Πλήρωσαν: %1\$s", multiDefaultLabel)
                                selectedMember != null ->
                                    Localization.trFormat(lang, "Paid by: %1\$s", "Πλήρωσε: %1\$s", selectedMember?.name.orEmpty())
                                else ->
                                    Localization.tr(lang, "Select Member", "Επιλογή Μέλους")
                            }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showMemberMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = payerDisplayText,
                                        fontSize = 13.sp
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMemberMenu,
                                    onDismissRequest = { showMemberMenu = false }
                                ) {
                                    if (validDefaultPayers.size >= 2 && multiDefaultLabel.isNotBlank()) {
                                        DropdownMenuItem(
                                            text = { Text(multiDefaultLabel) },
                                            onClick = {
                                                useDefaultMultiPayers = true
                                                showMemberMenu = false
                                            }
                                        )
                                    }
                                    activeMembers.forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m.name) },
                                            onClick = {
                                                useDefaultMultiPayers = false
                                                selectedMember = m
                                                showMemberMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                val amt = amountInput.toDoubleOrNull() ?: 0.0
                                val cat = selectedCategory ?: categories.firstOrNull() ?: return@Button
                                val title = titleInput.ifBlank { Localization.tr(lang, "Receipt purchase", "Αγορά απόδειξης") }
                                onLearnCategory?.invoke(
                                    title,
                                    cat.id,
                                    cat.name,
                                    parsedData?.merchantAfm.orEmpty()
                                )
                                onExpenseConfirmed(
                                    title,
                                    amt,
                                    cat,
                                    selectedTimestamp,
                                    if (useDefaultMultiPayers && validDefaultPayers.size >= 2) null else selectedMember,
                                    if (parsedData?.merchantAfm?.isNotBlank() == true) {
                                        Localization.trFormat(lang, "Tax ID: %1\$s", "ΑΦΜ: %1\$s", parsedData?.merchantAfm.orEmpty())
                                    } else ""
                                )
                                onDismiss()
                            },
                            enabled = amountInput.toDoubleOrNull() != null && (amountInput.toDoubleOrNull() ?: 0.0) > 0.0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("confirm_scanned_expense_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Localization.tr(lang, "Save Expense", "Καταχώρηση Εξόδου"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
