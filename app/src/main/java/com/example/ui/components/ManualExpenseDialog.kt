package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import com.example.util.RecurringScheduleHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManualExpenseDialog(
    expenseToEdit: Expense?,
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
    appLanguage: String = "EL",
    defaultPayerMember: FamilyMember? = null,
    defaultPayerMembers: List<FamilyMember> = listOfNotNull(defaultPayerMember),
    isPro: Boolean = false,
    onShowPremiumPaywall: (String) -> Unit = {},
    onSuggestCategory: ((merchantOrText: String, merchantAfm: String) -> ExpenseCategory?)? = null,
    onLearnCategory: ((merchantOrText: String, categoryId: Long, categoryName: String, merchantAfm: String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        amount: Double,
        categoryId: Long,
        categoryName: String,
        timestamp: Long,
        splitType: String,
        memberIds: List<Long>,
        memberNamesDisplay: String,
        note: String,
        payerMemberId: Long,
        payerMemberName: String,
        payerMemberIdsCsv: String,
        payerAmountsCsv: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    isReminderMode: Boolean = false,
    isEditingExistingReminder: Boolean = false,
    initialReminderDayOfMonth: Int = 1,
    initialReminderHour: Int = 9,
    initialReminderMinute: Int = 0,
    initialReminderFrequency: String = RecurringScheduleHelper.FREQ_MONTHLY,
    initialReminderAnchorMonth: Int = 0,
    onSaveReminder: ((
        id: Long,
        title: String,
        amount: Double,
        categoryId: Long,
        categoryName: String,
        dayOfMonth: Int,
        reminderHour: Int,
        reminderMinute: Int,
        splitType: String,
        memberIds: List<Long>,
        memberNamesDisplay: String,
        note: String,
        payerMemberId: Long,
        payerMemberName: String,
        payerMemberIdsCsv: String,
        payerAmountsCsv: String,
        frequency: String,
        anchorMonth: Int
    ) -> Unit)? = null
) {
    val lang = Localization.Language.fromCode(appLanguage)

    val fallbackDefaultPayers = remember(members, defaultPayerMembers, defaultPayerMember) {
        val validConfigured = defaultPayerMembers
            .filter { dp -> members.any { it.id == dp.id } }
            .distinctBy { it.id }
        if (validConfigured.isNotEmpty()) {
            validConfigured
        } else {
            val single = defaultPayerMember?.takeIf { dp -> members.any { it.id == dp.id } }
                ?: members.firstOrNull { it.role.equals("Γονέας", ignoreCase = true) }
                ?: members.firstOrNull()
            listOfNotNull(single)
        }
    }
    val fallbackDefaultPayer = fallbackDefaultPayers.firstOrNull()

    val selectedPayerIds = remember(expenseToEdit, members, fallbackDefaultPayers) {
        val list = mutableStateListOf<Long>()
        if (expenseToEdit != null) {
            val parsedIds = if (expenseToEdit.payerAmountsCsv.isNotBlank()) {
                expenseToEdit.payerAmountsCsv.split(",").mapNotNull { it.split(":").firstOrNull()?.trim()?.toLongOrNull() }
            } else if (expenseToEdit.payerMemberIdsCsv.isNotBlank()) {
                expenseToEdit.payerMemberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
            } else if (expenseToEdit.payerMemberId != 0L) {
                listOf(expenseToEdit.payerMemberId)
            } else emptyList()

            val valid = parsedIds.filter { id -> members.any { it.id == id } }
            if (valid.isNotEmpty()) {
                list.addAll(valid)
            } else if (expenseToEdit.payerMemberName.isNotBlank()) {
                val cleanPayer = expenseToEdit.payerMemberName.replace(Regex("\\([^)]*\\)"), "").trim()
                if (cleanPayer.equals("Γονείς", ignoreCase = true) || cleanPayer.equals("Parents", ignoreCase = true)) {
                    val parents = members.filter { it.role.equals("Γονέας", ignoreCase = true) }
                    if (parents.isNotEmpty()) list.addAll(parents.map { it.id })
                } else {
                    val names = cleanPayer.split(Regex("[&,/]+")).map { it.trim() }.filter { it.isNotBlank() }
                    val matched = members.filter { m -> names.any { n -> n.equals(m.name, ignoreCase = true) || cleanPayer.contains(m.name, ignoreCase = true) } }
                    if (matched.isNotEmpty()) {
                        list.addAll(matched.map { it.id })
                    } else if (fallbackDefaultPayers.isNotEmpty()) {
                        list.addAll(fallbackDefaultPayers.map { it.id })
                    }
                }
            } else if (fallbackDefaultPayers.isNotEmpty()) {
                list.addAll(fallbackDefaultPayers.map { it.id })
            }
        } else if (fallbackDefaultPayers.isNotEmpty()) {
            list.addAll(fallbackDefaultPayers.map { it.id })
        }
        list
    }

    var isCustomPayerSplit by remember(expenseToEdit) {
        val hasCustom = if (expenseToEdit != null && expenseToEdit.payerAmountsCsv.isNotBlank()) {
            val amounts = expenseToEdit.payerAmountsCsv.split(",")
                .mapNotNull { it.split(":").getOrNull(1)?.trim()?.toDoubleOrNull() }
            if (amounts.size >= 2) {
                val first = amounts[0]
                amounts.any { kotlin.math.abs(it - first) > 0.01 }
            } else false
        } else false
        mutableStateOf(hasCustom)
    }

    val customPayerAmounts = remember(expenseToEdit) {
        val map = mutableStateMapOf<Long, String>()
        if (expenseToEdit != null && expenseToEdit.payerAmountsCsv.isNotBlank()) {
            expenseToEdit.payerAmountsCsv.split(",").forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val id = parts[0].trim().toLongOrNull()
                    val amt = parts[1].trim().toDoubleOrNull()
                    if (id != null && amt != null) {
                        map[id] = String.format(Locale.US, "%.2f", amt)
                    }
                }
            }
        }
        map
    }

    var title by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.title ?: "") }
    var amountText by remember(expenseToEdit) {
        mutableStateOf(
            if (expenseToEdit != null && expenseToEdit.amount > 0.0) {
                String.format(Locale.US, "%.2f", expenseToEdit.amount)
            } else {
                ""
            }
        )
    }
    var selectedCategoryId by remember(expenseToEdit, categories) {
        val initialId = if (expenseToEdit != null) {
            if (expenseToEdit.categoryId == 0L || expenseToEdit.categoryName.isBlank()) 0L
            else if (categories.any { it.id == expenseToEdit.categoryId }) expenseToEdit.categoryId
            else 0L
        } else {
            categories.firstOrNull()?.id ?: 0L
        }
        mutableStateOf(initialId)
    }
    var userManuallyChangedCategory by remember(expenseToEdit) { mutableStateOf(expenseToEdit != null && expenseToEdit.id != 0L) }
    var autoSuggestedCategoryId by remember(expenseToEdit) { mutableStateOf<Long?>(null) }
    var scannedMerchantAfm by remember(expenseToEdit) { mutableStateOf("") }

    LaunchedEffect(title, userManuallyChangedCategory) {
        if ((expenseToEdit == null || expenseToEdit.id == 0L) && !userManuallyChangedCategory && title.trim().length >= 2) {
            val suggested = onSuggestCategory?.invoke(title.trim(), scannedMerchantAfm)
            if (suggested != null) {
                selectedCategoryId = suggested.id
                autoSuggestedCategoryId = suggested.id
            }
        }
    }

    var selectedTimestamp by remember(expenseToEdit) {
        mutableLongStateOf(expenseToEdit?.timestamp ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    var reminderFrequency by remember(expenseToEdit, initialReminderFrequency) {
        mutableStateOf(RecurringScheduleHelper.normalizeFrequency(initialReminderFrequency))
    }
    var reminderDayOfMonth by remember(expenseToEdit, initialReminderDayOfMonth) {
        mutableIntStateOf(initialReminderDayOfMonth.coerceIn(1, 31))
    }
    var reminderAnchorMonth by remember(expenseToEdit, initialReminderAnchorMonth) {
        mutableIntStateOf(initialReminderAnchorMonth.coerceIn(0, 11))
    }
    var reminderHour by remember(expenseToEdit, initialReminderHour) {
        mutableIntStateOf(initialReminderHour.coerceIn(0, 23))
    }
    var reminderMinute by remember(expenseToEdit, initialReminderMinute) {
        mutableIntStateOf(initialReminderMinute.coerceIn(0, 59))
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showCameraChooserDialog by remember { mutableStateOf(false) }
    var isScanningReceipt by remember { mutableStateOf(false) }

    fun processReceiptBitmap(bitmap: Bitmap) {
        if (!isPro) {
            onShowPremiumPaywall("ocr_receipt")
            return
        }
        isScanningReceipt = true
        coroutineScope.launch {
            try {
                val outcome = ManualExpenseReceiptScanHelper.scanReceiptBitmap(
                    bitmap = bitmap,
                    categories = categories,
                    lang = lang,
                    onSuggestCategory = onSuggestCategory
                )
                if (outcome.merchantAfm.isNotBlank()) scannedMerchantAfm = outcome.merchantAfm
                if (outcome.title.isNotBlank()) title = outcome.title
                if (outcome.amountText.isNotBlank()) amountText = outcome.amountText
                if (outcome.timestamp > 0) selectedTimestamp = outcome.timestamp
                if (outcome.matchedCategoryId != null) {
                    selectedCategoryId = outcome.matchedCategoryId
                    autoSuggestedCategoryId = outcome.matchedCategoryId
                }
                Toast.makeText(context, outcome.toastMessage, Toast.LENGTH_SHORT).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ManualExpenseDialog", "Error parsing receipt: ${e.message}", e)
                Toast.makeText(
                    context,
                    if (lang == Localization.Language.ENGLISH) "Receipt scan failed" else "Σφάλμα ανάγνωσης απόδειξης",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isScanningReceipt = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) processReceiptBitmap(bmp)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) cameraLauncher.launch(null)
        else Toast.makeText(context, if (lang == Localization.Language.ENGLISH) "Camera permission is required" else "Απαιτείται άδεια κάμερας", Toast.LENGTH_SHORT).show()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bmp = ManualExpenseReceiptScanHelper.decodeBitmap(context.contentResolver, uri)
                processReceiptBitmap(bmp)
            } catch (e: Exception) {
                Log.e("ManualExpenseDialog", "Failed to load picked image: ${e.message}", e)
            }
        }
    }

    var isAllFamily by remember(expenseToEdit) {
        val isCollectiveDisplay = expenseToEdit != null && (
            expenseToEdit.memberNamesDisplay.equals("Κοινό Οικογένειας", ignoreCase = true) ||
                expenseToEdit.memberNamesDisplay.equals("Family Shared", ignoreCase = true) ||
                expenseToEdit.memberNamesDisplay.equals("Όλοι", ignoreCase = true) ||
                expenseToEdit.memberNamesDisplay.equals("All", ignoreCase = true) ||
                expenseToEdit.memberNamesDisplay.equals("Όλα τα μέλη", ignoreCase = true) ||
                expenseToEdit.memberNamesDisplay.equals("All members", ignoreCase = true) ||
                expenseToEdit.memberNamesDisplay.equals("Οικογένεια", ignoreCase = true) ||
                expenseToEdit.memberNamesDisplay.equals("Family", ignoreCase = true)
            )
        val hasSpecific = expenseToEdit != null && expenseToEdit.splitType != "ALL_FAMILY" && (
            expenseToEdit.splitType == "SPECIFIC_MEMBERS" ||
                expenseToEdit.memberIdsCsv.isNotBlank() ||
                (expenseToEdit.memberNamesDisplay.isNotBlank() && !isCollectiveDisplay)
            )
        val isEmptyMember = expenseToEdit != null && expenseToEdit.memberIdsCsv.isBlank() && expenseToEdit.memberNamesDisplay.isBlank()
        mutableStateOf(if (expenseToEdit != null) (!hasSpecific && !isEmptyMember) else true)
    }

    val selectedMemberIds = remember(expenseToEdit, members) {
        val list = mutableStateListOf<Long>()
        if (expenseToEdit != null) {
            val isCollectiveDisplay =
                expenseToEdit.memberNamesDisplay.equals("Κοινό Οικογένειας", ignoreCase = true) ||
                    expenseToEdit.memberNamesDisplay.equals("Family Shared", ignoreCase = true) ||
                    expenseToEdit.memberNamesDisplay.equals("Όλοι", ignoreCase = true) ||
                    expenseToEdit.memberNamesDisplay.equals("All", ignoreCase = true) ||
                    expenseToEdit.memberNamesDisplay.equals("Όλα τα μέλη", ignoreCase = true) ||
                    expenseToEdit.memberNamesDisplay.equals("All members", ignoreCase = true) ||
                    expenseToEdit.memberNamesDisplay.equals("Οικογένεια", ignoreCase = true) ||
                    expenseToEdit.memberNamesDisplay.equals("Family", ignoreCase = true)
            val parsedIds = expenseToEdit.memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
            val validIds = parsedIds.filter { id -> members.any { it.id == id } }
            if (validIds.isNotEmpty()) {
                list.addAll(validIds)
            } else if (expenseToEdit.memberNamesDisplay.isNotBlank() && !isCollectiveDisplay) {
                val raw = expenseToEdit.memberNamesDisplay.trim()
                if (Localization.isPluralRoleDisplay(raw)) {
                    val roleKey = Localization.canonicalRoleKey(raw)
                    list.addAll(members.filter { Localization.canonicalRoleKey(it.role) == roleKey }.map { it.id })
                } else {
                    list.addAll(members.filter { m -> expenseToEdit.memberNamesDisplay.contains(m.name, ignoreCase = true) }.map { it.id })
                }
            }
        } else {
            list.addAll(members.map { it.id })
        }
        list
    }

    var note by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.note ?: "") }
    var errorText by remember(expenseToEdit) { mutableStateOf<String?>(null) }
    var titleError by remember(expenseToEdit) { mutableStateOf<String?>(null) }
    var amountError by remember(expenseToEdit) { mutableStateOf<String?>(null) }
    var categoryError by remember(expenseToEdit) { mutableStateOf<String?>(null) }
    var payerError by remember(expenseToEdit) { mutableStateOf<String?>(null) }
    var beneficiaryError by remember(expenseToEdit) { mutableStateOf<String?>(null) }

    val currentAmount = amountText.toDoubleOrNull() ?: 0.0
    val activeMemberCount = if (isAllFamily) members.size else selectedMemberIds.size.coerceAtLeast(1)
    val splitPerPerson = currentAmount / activeMemberCount

    val displayDateFormat = remember(lang) { SimpleDateFormat("EEEE, d MMMM yyyy", Localization.getDateLocale(lang)) }
    val todayDateString = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val isSelectedDateToday = remember(selectedTimestamp) {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedTimestamp)) == todayDateString
    }

    if (showDatePicker) {
        ManualExpenseDatePickerDialog(
            initialTimestamp = selectedTimestamp,
            lang = lang,
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedTimestamp = it }
        )
    }

    if (showCameraChooserDialog) {
        ReceiptSourceChooserDialog(
            lang = lang,
            onDismiss = { showCameraChooserDialog = false },
            onChooseCamera = {
                showCameraChooserDialog = false
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraLauncher.launch(null)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onChooseGallery = {
                showCameraChooserDialog = false
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SmoothDialogContent {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.88f)
                    .padding(vertical = 12.dp)
                    .testTag("manual_expense_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dialogTitle = when {
                        isReminderMode && isEditingExistingReminder ->
                            if (lang == Localization.Language.ENGLISH) "Edit Bill Reminder" else "Επεξεργασία Υπενθύμισης"
                        isReminderMode ->
                            if (lang == Localization.Language.ENGLISH) "New Bill Reminder" else "Νέα Υπενθύμιση Εξόδου"
                        expenseToEdit == null -> Localization.recordExpenseTitle(lang)
                        else -> Localization.editExpenseTitle(lang)
                    }
                    Text(
                        text = dialogTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald700
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald100,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    if (isPro) {
                                        showCameraChooserDialog = true
                                    } else {
                                        onShowPremiumPaywall("ocr_receipt")
                                    }
                                }
                                .testTag("receipt_scan_header_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isScanningReceipt) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Emerald700
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Σάρωση Απόδειξης",
                                        tint = Emerald700,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { onDismiss() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = Localization.close(lang),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isReminderMode) {
                        ManualExpenseReminderScheduleSection(
                            lang = lang,
                            frequency = reminderFrequency,
                            onFrequencyChange = { reminderFrequency = it },
                            dayOfMonth = reminderDayOfMonth,
                            onDayOfMonthChange = { reminderDayOfMonth = it },
                            anchorMonth = reminderAnchorMonth,
                            onAnchorMonthChange = { reminderAnchorMonth = it },
                            reminderHour = reminderHour,
                            reminderMinute = reminderMinute,
                            onTimeChange = { h, m ->
                                reminderHour = h
                                reminderMinute = m
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            titleError = null
                            errorText = null
                        },
                        label = { Text(Localization.expenseDescLabel(lang)) },
                        placeholder = { Text(Localization.expenseDescPlaceholder(lang)) },
                        isError = titleError != null,
                        supportingText = titleError?.let { msg ->
                            { Text(text = msg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_title_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Emerald700)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it.replace(',', '.')
                            amountError = null
                            errorText = null
                        },
                        label = { Text(Localization.expenseAmountLabel(lang)) },
                        leadingIcon = {
                            Icon(Icons.Default.Euro, contentDescription = null, tint = Emerald700)
                        },
                        isError = amountError != null,
                        supportingText = amountError?.let { msg ->
                            { Text(text = msg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_amount_input"),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Emerald700)
                    )

                    if (!isReminderMode) {
                        Spacer(modifier = Modifier.height(14.dp))
                        ManualExpenseDateCard(
                            lang = lang,
                            isSelectedDateToday = isSelectedDateToday,
                            formattedDate = displayDateFormat.format(Date(selectedTimestamp)).replaceFirstChar { it.uppercase() },
                            onClick = { showDatePicker = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ManualExpenseCategorySection(
                        lang = lang,
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        autoSuggestedCategoryId = autoSuggestedCategoryId,
                        onSelectCategory = { nextId ->
                            userManuallyChangedCategory = true
                            autoSuggestedCategoryId = null
                            selectedCategoryId = nextId
                            categoryError = null
                            errorText = null
                        },
                        categoryError = categoryError
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ManualExpensePayerAndSplitSection(
                        lang = lang,
                        members = members,
                        selectedPayerIds = selectedPayerIds,
                        isCustomPayerSplit = isCustomPayerSplit,
                        onCustomPayerSplitChange = { isCustomPayerSplit = it },
                        customPayerAmounts = customPayerAmounts,
                        currentAmount = currentAmount,
                        isAllFamily = isAllFamily,
                        onIsAllFamilyChange = { isAllFamily = it },
                        selectedMemberIds = selectedMemberIds,
                        splitPerPerson = splitPerPerson,
                        payerError = payerError,
                        beneficiaryError = beneficiaryError,
                        onPayerSelectionChanged = {
                            payerError = null
                            errorText = null
                        },
                        onBeneficiarySelectionChanged = {
                            beneficiaryError = null
                            errorText = null
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(Localization.noteLabel(lang)) },
                        placeholder = { Text(Localization.notePlaceholder(lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Emerald700)
                    )

                    if (errorText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorText ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(Localization.cancel(lang))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val parsedAmount = amountText.toDoubleOrNull()
                            val chosenCat = categories.find { it.id == selectedCategoryId }
                            val hasValidPayer = selectedPayerIds.any { pid -> members.any { it.id == pid } }
                            val hasValidBeneficiary = if (isAllFamily) {
                                members.isNotEmpty()
                            } else {
                                selectedMemberIds.any { mid -> members.any { it.id == mid } }
                            }

                            val nextTitleError = if (title.isBlank()) Localization.fillDescriptionError(lang) else null
                            val nextAmountError = if (parsedAmount == null || parsedAmount <= 0) Localization.fillAmountError(lang) else null
                            val nextCategoryError = if (chosenCat == null) {
                                if (lang == Localization.Language.ENGLISH) "Please select a category" else "Επιλέξτε κατηγορία εξόδου"
                            } else null
                            val nextPayerError = if (!hasValidPayer) {
                                if (lang == Localization.Language.ENGLISH) "Please select who paid" else "Επιλέξτε ποιος πλήρωσε το έξοδο"
                            } else null
                            val nextBeneficiaryError = if (!hasValidBeneficiary) {
                                if (lang == Localization.Language.ENGLISH) "Please select who this expense is for" else "Επιλέξτε σε ποιον αφορά το έξοδο"
                            } else null

                            titleError = nextTitleError
                            amountError = nextAmountError
                            categoryError = nextCategoryError
                            payerError = nextPayerError
                            beneficiaryError = nextBeneficiaryError

                            val firstError = nextTitleError ?: nextAmountError ?: nextCategoryError ?: nextPayerError ?: nextBeneficiaryError
                            if (firstError != null || parsedAmount == null || chosenCat == null) {
                                errorText = firstError
                                return@Button
                            }
                            errorText = null

                            val catId = chosenCat.id
                            val catName = chosenCat.name
                            if (catId > 0L && catName.isNotBlank()) {
                                onLearnCategory?.invoke(title.trim(), catId, catName, scannedMerchantAfm)
                            }

                            val resolved = ManualExpenseSaveResolver.resolveSplitAndPayers(
                                parsedAmount = parsedAmount,
                                isAllFamily = isAllFamily,
                                selectedMemberIds = selectedMemberIds.toList(),
                                selectedPayerIds = selectedPayerIds.toList(),
                                fallbackDefaultPayer = fallbackDefaultPayer,
                                isCustomPayerSplit = isCustomPayerSplit,
                                customPayerAmounts = customPayerAmounts.toMap(),
                                members = members,
                                lang = lang
                            )

                            val finalTimestamp = if (isSelectedDateToday && expenseToEdit == null) {
                                System.currentTimeMillis()
                            } else {
                                selectedTimestamp
                            }

                            if (isReminderMode && onSaveReminder != null) {
                                val resolvedDay = if (reminderFrequency == RecurringScheduleHelper.FREQ_WEEKLY ||
                                    reminderFrequency == RecurringScheduleHelper.FREQ_BIWEEKLY
                                ) {
                                    reminderDayOfMonth.coerceIn(1, 7)
                                } else {
                                    reminderDayOfMonth.coerceIn(1, 31)
                                }
                                onSaveReminder(
                                    expenseToEdit?.id ?: 0L,
                                    title.trim(),
                                    parsedAmount,
                                    catId,
                                    catName,
                                    resolvedDay,
                                    reminderHour,
                                    reminderMinute,
                                    resolved.splitType,
                                    resolved.finalMemberIds,
                                    resolved.memberDisplay,
                                    note.trim(),
                                    resolved.finalPayerId,
                                    resolved.finalPayerName,
                                    resolved.finalPayerMemberIdsCsv,
                                    resolved.finalPayerAmountsCsv,
                                    reminderFrequency,
                                    reminderAnchorMonth
                                )
                            } else {
                                onSave(
                                    expenseToEdit?.id ?: 0L,
                                    title.trim(),
                                    parsedAmount,
                                    catId,
                                    catName,
                                    finalTimestamp,
                                    resolved.splitType,
                                    resolved.finalMemberIds,
                                    resolved.memberDisplay,
                                    note.trim(),
                                    resolved.finalPayerId,
                                    resolved.finalPayerName,
                                    resolved.finalPayerMemberIdsCsv,
                                    resolved.finalPayerAmountsCsv
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_expense_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Localization.save(lang),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
        }
    }
}
