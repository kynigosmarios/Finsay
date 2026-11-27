package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.RecurringExpense
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import com.example.util.RecurringScheduleHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RemindersManagerDialog(
    reminders: List<RecurringExpense>,
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
    defaultPayerMember: FamilyMember? = null,
    defaultPayerMembers: List<FamilyMember> = listOfNotNull(defaultPayerMember),
    onSuggestCategory: ((String, String) -> ExpenseCategory?)? = null,
    onLearnCategory: ((String, Long, String, String) -> Unit)? = null,
    isPro: Boolean = false,
    onShowPremiumPaywall: (String) -> Unit = {},
    appLanguage: String = "EL",
    onDismiss: () -> Unit,
    onSaveReminder: (
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
    ) -> Unit,
    onToggleActive: (RecurringExpense) -> Unit,
    onDeleteReminder: (RecurringExpense) -> Unit,
    onLogExpenseNow: (RecurringExpense) -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)

    var showEditor by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<RecurringExpense?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("reminders_manager_dialog")
    ) {
        // Page Header Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reminders_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Localization.tr(lang, "Back", "Επιστροφή"),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Amber700.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Amber700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Localization.tr(lang, "Bill Reminders", "Υπενθυμίσεις"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Bar: "+ Νέα Υπενθύμιση"
        Button(
            onClick = {
                if (!isPro && reminders.size >= 2) {
                    onShowPremiumPaywall("recurring_expense_limit")
                } else {
                    reminderToEdit = null
                    showEditor = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(44.dp)
                .testTag("create_new_reminder_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = Localization.tr(lang, "New Bill Reminder", "Νέα Υπενθύμιση Εξόδου"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )

        // Reminders List or Empty State
        if (reminders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = Amber700,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = Localization.tr(lang, "No reminders yet", "Δεν υπάρχουν ακόμη υπενθυμίσεις"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick suggestion chips
                        Text(
                            text = Localization.tr(lang, "Suggested recurring bills:", "Προτεινόμενα πάγια έξοδα:"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            data class BillSuggestion(val key: String, val emoji: String, val label: String, val defaultAmount: Double)
                            val suggestions = listOf(
                                BillSuggestion("rent", "🏠", Localization.tr(lang, "Rent", "Ενοίκιο"), 450.0),
                                BillSuggestion("electricity", "⚡", Localization.tr(lang, "Electricity", "ΔΕΗ / Ρεύμα"), 80.0),
                                BillSuggestion("internet", "📱", Localization.tr(lang, "Mobile & Internet", "Κινητή & Internet"), 30.0),
                                BillSuggestion("gym", "🏋️", Localization.tr(lang, "Gym", "Γυμναστήριο"), 35.0),
                                BillSuggestion("streaming", "📺", Localization.tr(lang, "Streaming", "Netflix / TV"), 15.0)
                            )
                            suggestions.forEach { item ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        if (!isPro && reminders.size >= 2) {
                                            onShowPremiumPaywall("recurring_expense_limit")
                                            return@FilterChip
                                        }
                                        val matchedCat = categories.find { cat ->
                                            val cn = Localization.translateCategory(cat.name, Localization.Language.ENGLISH).lowercase()
                                            when (item.key) {
                                                "rent" -> cn.contains("home") || cn.contains("housing")
                                                "electricity", "internet" -> cn.contains("bill")
                                                "gym" -> cn.contains("health") || cn.contains("entertainment")
                                                else -> false
                                            }
                                        } ?: categories.firstOrNull()

                                        reminderToEdit = RecurringExpense(
                                            id = 0L,
                                            title = item.label,
                                            amount = item.defaultAmount,
                                            categoryId = matchedCat?.id ?: 1L,
                                            categoryName = matchedCat?.name ?: Localization.translateCategory("Σπίτι", lang),
                                            dayOfMonth = 1,
                                            note = Localization.tr(lang, "Recurring bill", "Πάγιος λογαριασμός")
                                        )
                                        showEditor = true
                                    },
                                    label = { Text("${item.emoji} ${item.label}", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = reminders,
                            key = { it.id },
                            contentType = { "reminder_card" }
                        ) { reminder ->
                            ReminderItemCard(
                                reminder = reminder,
                                lang = lang,
                                onToggleActive = { onToggleActive(reminder) },
                                onEdit = {
                                    reminderToEdit = reminder
                                    showEditor = true
                                },
                                onDelete = { onDeleteReminder(reminder) },
                                onLogNow = { onLogExpenseNow(reminder) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
    }

    // Sub-dialog: Create or Edit Reminder using the full standard Expense Form with Day & Time at the top
    if (showEditor) {
        val currentReminder = reminderToEdit
        val prefilledExpense = currentReminder?.let { rec ->
            Expense(
                id = rec.id,
                title = rec.title,
                amount = rec.amount,
                categoryId = rec.categoryId,
                categoryName = rec.categoryName,
                timestamp = System.currentTimeMillis(),
                splitType = rec.splitType.ifBlank { "ALL_FAMILY" },
                memberIdsCsv = rec.memberIdsCsv,
                memberNamesDisplay = rec.memberNamesDisplay.ifBlank {
                    Localization.familyShared(lang)
                },
                note = rec.note,
                payerMemberId = rec.payerMemberId,
                payerMemberName = rec.payerMemberName,
                payerMemberIdsCsv = rec.payerMemberIdsCsv,
                payerAmountsCsv = rec.payerAmountsCsv
            )
        }
        ManualExpenseDialog(
            expenseToEdit = prefilledExpense,
            categories = categories,
            members = members,
            defaultPayerMember = defaultPayerMember,
            defaultPayerMembers = defaultPayerMembers,
            isPro = isPro,
            onShowPremiumPaywall = onShowPremiumPaywall,
            onSuggestCategory = onSuggestCategory,
            onLearnCategory = onLearnCategory,
            appLanguage = appLanguage,
            onDismiss = {
                showEditor = false
                reminderToEdit = null
            },
            isReminderMode = true,
            isEditingExistingReminder = currentReminder != null && currentReminder.id != 0L,
            initialReminderDayOfMonth = currentReminder?.dayOfMonth ?: 1,
            initialReminderHour = currentReminder?.reminderHour ?: 9,
            initialReminderMinute = currentReminder?.reminderMinute ?: 0,
            initialReminderFrequency = currentReminder?.frequency ?: RecurringScheduleHelper.FREQ_MONTHLY,
            initialReminderAnchorMonth = currentReminder?.anchorMonth ?: 0,
            onSaveReminder = { id, title, amount, categoryId, categoryName, dayOfMonth, hour, minute, splitType, memberIds, memberNamesDisplay, note, payerId, payerName, payerIdsCsv, payerAmountsCsv, freq, anchor ->
                onSaveReminder(
                    id,
                    title,
                    amount,
                    categoryId,
                    categoryName,
                    dayOfMonth,
                    hour,
                    minute,
                    splitType,
                    memberIds,
                    memberNamesDisplay,
                    note,
                    payerId,
                    payerName,
                    payerIdsCsv,
                    payerAmountsCsv,
                    freq,
                    anchor
                )
                showEditor = false
                reminderToEdit = null
            }
        )
    }
}

@Composable
private fun ReminderItemCard(
    reminder: RecurringExpense,
    lang: Localization.Language,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLogNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedTime = String.format(Locale.US, "%02d:%02d", reminder.reminderHour, reminder.reminderMinute)
    val scheduleText = RecurringScheduleHelper.scheduleSummary(reminder, lang)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isActive)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        border = BorderStroke(
            1.dp,
            if (reminder.isActive) Emerald700.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Row 1: Icon + Title & Subtitle + Unobstructed Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (reminder.isActive) Emerald700.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.EventRepeat,
                            contentDescription = null,
                            tint = if (reminder.isActive) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (reminder.categoryName.isNotBlank()) {
                        val payerPart = if (reminder.payerMemberName.isNotBlank()) " • ${reminder.payerMemberName}" else ""
                        val beneficiaryPart = if (reminder.memberNamesDisplay.isNotBlank() && reminder.splitType == "SPECIFIC_MEMBERS") {
                            " → ${reminder.memberNamesDisplay}"
                        } else ""
                        Text(
                            text = Localization.translateCategory(reminder.categoryName, lang) + payerPart + beneficiaryPart,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Emerald700,
                        checkedBorderColor = Emerald700
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Clean Schedule Pill (full available width) + Prominent Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (reminder.isActive) {
                            Emerald700.copy(alpha = 0.10f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (reminder.isActive) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "$scheduleText • $formattedTime",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (reminder.isActive) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                val currencySymbol = com.example.util.LocalCurrencySymbol.current
                Text(
                    text = "${String.format(Locale.GERMANY, "%.2f", reminder.amount)} $currencySymbol",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.5.sp,
                    color = if (reminder.isActive) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Primary Action ("Καταγραφή Τώρα") + Edit + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onLogNow,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.tr(lang, "Record Now", "Καταγραφή Τώρα"),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = Localization.tr(lang, "Edit", "Επεξεργασία"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = Localization.tr(lang, "Delete", "Διαγραφή"),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
