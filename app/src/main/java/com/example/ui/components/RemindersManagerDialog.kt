package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
    onSuggestCategory: ((String, String) -> ExpenseCategory?)? = null,
    onLearnCategory: ((String, Long, String, String) -> Unit)? = null,
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
    val isGreek = !appLanguage.equals("EN", ignoreCase = true)

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
                        contentDescription = if (isGreek) "Επιστροφή" else "Back",
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isGreek) "Υπενθυμίσεις" else "Bill Reminders",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isGreek) "Πάγιοι λογαριασμοί & ειδοποιήσεις" else "Scheduled expenses & push alerts",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Bar: "+ Νέα Υπενθύμιση"
        Button(
            onClick = {
                reminderToEdit = null
                showEditor = true
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
                text = if (isGreek) "Νέα Υπενθύμιση Εξόδου" else "New Bill Reminder",
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
                            text = if (isGreek) "Δεν υπάρχουν ακόμη υπενθυμίσεις" else "No reminders yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isGreek)
                                "Προσθέστε πάγιους λογαριασμούς (ενοίκιο, ρεύμα, συνδρομές) για να σας εμφανίζεται κάρτα υπενθύμισης και να τα καταγράφετε με 1 πάτημα."
                            else
                                "Add recurring bills (rent, utilities, subscriptions) to get in-app reminder alerts and log them in 1 tap.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Quick suggestion chips
                        Text(
                            text = if (isGreek) "Προτεινόμενα πάγια έξοδα:" else "Suggested recurring bills:",
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
                            val suggestions = listOf(
                                "🏠 " + (if (isGreek) "Ενοίκιο" else "Rent"),
                                "⚡ " + (if (isGreek) "ΔΕΗ / Ρεύμα" else "Electricity"),
                                "📱 " + (if (isGreek) "Κινητή & Internet" else "Mobile & Internet"),
                                "🏋️ " + (if (isGreek) "Γυμναστήριο" else "Gym"),
                                "📺 " + (if (isGreek) "Netflix / TV" else "Streaming")
                            )
                            suggestions.forEach { name ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val cleanName = name.substringAfter(" ")
                                        val matchedCat = categories.find { cat ->
                                            val cn = cat.name.lowercase()
                                            when {
                                                cleanName.contains("Ενοίκιο") || cleanName.contains("Rent") -> cn.contains("σπίτι") || cn.contains("home")
                                                cleanName.contains("Ρεύμα") || cleanName.contains("Electricity") -> cn.contains("λογαριασμ") || cn.contains("bill")
                                                cleanName.contains("Internet") -> cn.contains("λογαριασμ") || cn.contains("bill")
                                                cleanName.contains("Γυμναστήριο") || cleanName.contains("Gym") -> cn.contains("υγεία") || cn.contains("διασκέδαση")
                                                else -> false
                                            }
                                        } ?: categories.firstOrNull()

                                        reminderToEdit = RecurringExpense(
                                            id = 0L,
                                            title = cleanName,
                                            amount = when {
                                                cleanName.contains("Ενοίκιο") || cleanName.contains("Rent") -> 450.0
                                                cleanName.contains("Ρεύμα") || cleanName.contains("Electricity") -> 80.0
                                                cleanName.contains("Internet") -> 30.0
                                                cleanName.contains("Γυμναστήριο") || cleanName.contains("Gym") -> 35.0
                                                else -> 15.0
                                            },
                                            categoryId = matchedCat?.id ?: 1L,
                                            categoryName = matchedCat?.name ?: "Σπίτι",
                                            dayOfMonth = 1,
                                            note = if (isGreek) "Πάγιος λογαριασμός" else "Recurring bill"
                                        )
                                        showEditor = true
                                    },
                                    label = { Text(name, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        reminders.forEach { reminder ->
                            ReminderItemCard(
                                reminder = reminder,
                                isGreek = isGreek,
                                onToggleActive = { onToggleActive(reminder) },
                                onEdit = {
                                    reminderToEdit = reminder
                                    showEditor = true
                                },
                                onDelete = { onDeleteReminder(reminder) },
                                onLogNow = { onLogExpenseNow(reminder) }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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
                    if (isGreek) "Κοινό Οικογένειας" else "Family Shared"
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
    isGreek: Boolean,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLogNow: () -> Unit
) {
    val formattedTime = String.format(Locale.US, "%02d:%02d", reminder.reminderHour, reminder.reminderMinute)
    val scheduleText = RecurringScheduleHelper.scheduleSummary(
        reminder,
        if (isGreek) Localization.Language.GREEK else Localization.Language.ENGLISH
    )

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
        modifier = Modifier.fillMaxWidth()
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
                            text = reminder.categoryName + payerPart + beneficiaryPart,
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

                Text(
                    text = String.format(Locale.GERMANY, "%.2f €", reminder.amount),
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
                        text = if (isGreek) "Καταγραφή Τώρα" else "Record Now",
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
                        contentDescription = if (isGreek) "Επεξεργασία" else "Edit",
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
                        contentDescription = if (isGreek) "Διαγραφή" else "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
