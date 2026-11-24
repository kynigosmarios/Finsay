package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.SavingsGoal
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object GoalDeadlineDateHelper {
    private val dateFormats = listOf("dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "yyyy-MM-dd")

    fun parseDeadlineToUtcMillis(rawDate: String): Long? {
        val clean = rawDate.trim()
        if (clean.isBlank()) return null
        for (pattern in dateFormats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(clean)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return null
    }

    fun formatUtcMillisToDeadline(utcMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(utcMillis))
    }

    fun defaultSuggestedUtcMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val local = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
            add(Calendar.MONTH, 6)
        }.timeInMillis
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalDialog(
    goalToEdit: SavingsGoal? = null,
    appLanguage: String = "EL",
    onDismiss: () -> Unit,
    onSave: (id: Long, title: String, targetAmount: Double, currentAmount: Double, deadline: String, note: String, recordAsExpense: Boolean) -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val isGreek = lang != Localization.Language.ENGLISH

    var title by remember(goalToEdit) { mutableStateOf(goalToEdit?.title ?: "") }
    var targetText by remember(goalToEdit) {
        mutableStateOf(
            goalToEdit?.takeIf { it.targetAmount > 0.0 }?.let { String.format(Locale.US, "%.0f", it.targetAmount) } ?: ""
        )
    }
    var currentText by remember(goalToEdit) {
        mutableStateOf(goalToEdit?.let { String.format(Locale.US, "%.0f", it.currentAmount) } ?: "0")
    }
    var deadline by remember(goalToEdit) {
        mutableStateOf(goalToEdit?.deadlineDate?.trim().orEmpty())
    }
    var note by remember(goalToEdit) { mutableStateOf(goalToEdit?.note ?: "") }
    var recordDeltaAsExpense by remember(goalToEdit) { mutableStateOf(true) }
    var isSubmitting by remember(goalToEdit) { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val parsedCurrent = currentText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val previousCurrent = goalToEdit?.currentAmount ?: 0.0
    val positiveAmountDelta = parsedCurrent - previousCurrent

    val initialPickerMillis = remember(deadline) {
        GoalDeadlineDateHelper.parseDeadlineToUtcMillis(deadline)
            ?: GoalDeadlineDateHelper.defaultSuggestedUtcMillis()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialPickerMillis
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            deadline = GoalDeadlineDateHelper.formatUtcMillisToDeadline(utcMillis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = Localization.select(lang),
                        color = Emerald700,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(Localization.cancel(lang))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = Emerald700,
                    selectedYearContainerColor = Emerald700,
                    todayDateBorderColor = Emerald700,
                    todayContentColor = Emerald700
                )
            )
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Emerald700,
        focusedLabelColor = Emerald700,
        cursorColor = Emerald700,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SmoothDialogContent {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .testTag("savings_goal_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (goalToEdit == null || goalToEdit.id == 0L) Localization.newGoal(lang) else Localization.editGoalTitle(lang),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = Localization.close(lang))
                        }
                    }

                Spacer(modifier = Modifier.height(18.dp))

                // Field 1: Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Localization.goalTitleLabel(lang)) },
                    placeholder = { Text(Localization.goalTitlePlaceholder(lang)) },
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Emerald700) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Field 2 & 3: Amounts Row
                val currencySymbol = com.example.util.LocalCurrencySymbol.current
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text(if (lang == Localization.Language.ENGLISH) "Goal ($currencySymbol)" else "Στόχος ($currencySymbol)") },
                        placeholder = { Text("0") },
                        trailingIcon = { Text(currencySymbol, fontWeight = FontWeight.Bold, color = Emerald700, modifier = Modifier.padding(end = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = currentText,
                        onValueChange = { currentText = it },
                        label = { Text(if (lang == Localization.Language.ENGLISH) "Saved ($currencySymbol)" else "Μαζεμένα ($currencySymbol)") },
                        placeholder = { Text("0") },
                        trailingIcon = { Text(currencySymbol, fontWeight = FontWeight.Bold, color = Emerald700, modifier = Modifier.padding(end = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Field 4: Calendar Date Picker Trigger (no free-text input)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable { showDatePicker = true }
                        .testTag("goal_deadline_picker_row")
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isGreek) "Προθεσμία" else "Deadline",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = deadline.ifBlank {
                                        if (isGreek) "Επιλογή από ημερολόγιο" else "Select from calendar"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = if (deadline.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (deadline.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (deadline.isNotBlank()) {
                                IconButton(
                                    onClick = { deadline = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isGreek) "Καθαρισμός ημερομηνίας" else "Clear date",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = if (deadline.isNotBlank()) Localization.changeAction(lang) else Localization.select(lang),
                                tint = Emerald700,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Field 5: Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(Localization.noteLabel(lang)) },
                    placeholder = { Text(Localization.notePlaceholder(lang)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = positiveAmountDelta > 0.009,
                    enter = FinsayMotion.ExpandEnter,
                    exit = FinsayMotion.ShrinkExit
                ) {
                    val deltaStr = if (kotlin.math.abs(positiveAmountDelta - positiveAmountDelta.toLong()) < 0.005) {
                        "${String.format(Locale.US, "%.0f", positiveAmountDelta)}$currencySymbol"
                    } else {
                        "${String.format(Locale.US, "%.2f", positiveAmountDelta)}$currencySymbol"
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Emerald700.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isGreek) "Καταχώρηση (+$deltaStr) στα Έξοδα" else "Log (+$deltaStr) as Expense",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Switch(
                                    checked = recordDeltaAsExpense,
                                    onCheckedChange = { recordDeltaAsExpense = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Emerald700
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save button
                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        isSubmitting = true
                        val target = targetText.replace(',', '.').toDoubleOrNull() ?: 100.0
                        val current = currentText.replace(',', '.').toDoubleOrNull() ?: 0.0
                        onSave(
                            goalToEdit?.id ?: 0L,
                            title.ifBlank { Localization.newGoal(lang) },
                            target,
                            current,
                            deadline,
                            note,
                            recordDeltaAsExpense && positiveAmountDelta > 0.009
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSubmitting && title.isNotBlank() && targetText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.save(lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
        }
    }
}
