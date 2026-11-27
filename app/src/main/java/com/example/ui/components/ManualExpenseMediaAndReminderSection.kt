package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import com.example.util.RecurringScheduleHelper
import java.util.Locale

@Composable
internal fun ManualExpenseReminderScheduleSection(
    lang: Localization.Language,
    frequency: String,
    onFrequencyChange: (String) -> Unit,
    dayOfMonth: Int,
    onDayOfMonthChange: (Int) -> Unit,
    anchorMonth: Int,
    onAnchorMonthChange: (Int) -> Unit,
    reminderHour: Int,
    reminderMinute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit
) {
    val context = LocalContext.current
    var showFreqDropdown by remember { mutableStateOf(false) }
    var showWeekdayDropdown by remember { mutableStateOf(false) }
    var showReminderDayDropdown by remember { mutableStateOf(false) }
    var showAnchorMonthDropdown by remember { mutableStateOf(false) }

    val normalizedFreq = RecurringScheduleHelper.normalizeFrequency(frequency)
    val isOnceMode = normalizedFreq == RecurringScheduleHelper.FREQ_ONCE
    val isWeeklyMode = normalizedFreq == RecurringScheduleHelper.FREQ_WEEKLY ||
        normalizedFreq == RecurringScheduleHelper.FREQ_BIWEEKLY
    val needsAnchorMonth = normalizedFreq == RecurringScheduleHelper.FREQ_QUARTERLY ||
        normalizedFreq == RecurringScheduleHelper.FREQ_YEARLY

    val pillBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        ),
        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_schedule_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Localization.tr(lang, "Frequency, Day & Time", "Συχνότητα, Ημέρα & Ώρα Υπενθύμισης"),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Row 1: Frequency Dropdown + Time Picker Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1.3f)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, pillBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { showFreqDropdown = true }
                            .testTag("reminder_freq_dropdown")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Localization.frequencyFieldLabel(lang),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = RecurringScheduleHelper.frequencyLabel(normalizedFreq, lang),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = Localization.frequencyFieldLabel(lang),
                                tint = Emerald700,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showFreqDropdown,
                        onDismissRequest = { showFreqDropdown = false }
                    ) {
                        RecurringScheduleHelper.ALL_FREQUENCIES.forEach { freqKey ->
                            val isSelected = normalizedFreq == freqKey
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = RecurringScheduleHelper.frequencyLabel(freqKey, lang),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Emerald700 else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Emerald700,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    onFrequencyChange(freqKey)
                                    when (freqKey) {
                                        RecurringScheduleHelper.FREQ_ONCE -> {
                                            val cal = java.util.Calendar.getInstance()
                                            val (y, m) = RecurringScheduleHelper.decodeOnceAnchor(anchorMonth, cal.timeInMillis)
                                            onAnchorMonthChange(RecurringScheduleHelper.encodeOnceAnchor(y, m))
                                            onDayOfMonthChange(dayOfMonth.coerceIn(1, 31))
                                        }
                                        RecurringScheduleHelper.FREQ_WEEKLY,
                                        RecurringScheduleHelper.FREQ_BIWEEKLY -> {
                                            onDayOfMonthChange(dayOfMonth.coerceIn(1, 7))
                                        }
                                        else -> {
                                            onAnchorMonthChange(RecurringScheduleHelper.sanitizeAnchorMonth(freqKey, anchorMonth))
                                            onDayOfMonthChange(dayOfMonth.coerceIn(1, 31))
                                        }
                                    }
                                    showFreqDropdown = false
                                },
                                modifier = Modifier.testTag("reminder_freq_chip_${freqKey.lowercase()}")
                            )
                        }
                    }
                }

                // Time Picker Pill (HH:mm)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, pillBorderColor),
                    modifier = Modifier
                        .weight(0.9f)
                        .heightIn(min = 48.dp)
                        .clickable {
                            android.app.TimePickerDialog(
                                context,
                                { _, selectedH, selectedM ->
                                    onTimeChange(selectedH, selectedM)
                                },
                                reminderHour,
                                reminderMinute,
                                true
                            ).show()
                        }
                        .testTag("reminder_time_picker_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = Localization.tr(lang, "Reminder time", "Ώρα υπενθύμισης"),
                            tint = Emerald700,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = Localization.tr(lang, "Time", "Ώρα"),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", reminderHour, reminderMinute),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald700,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Row 2: Exact Date Picker (ONCE) OR Weekday Dropdown OR Day-of-Month Stepper + Optional Anchor Month Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOnceMode) {
                    val (onceYear, onceMonth) = RecurringScheduleHelper.decodeOnceAnchor(anchorMonth)
                    val onceDay = dayOfMonth.coerceIn(1, 31)
                    val formattedOnceDate = String.format(Locale.US, "%02d/%02d/%04d", onceDay, onceMonth + 1, onceYear)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, pillBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                android.app.DatePickerDialog(
                                    context,
                                    { _, selectedYear, selectedMonth, selectedDay ->
                                        onAnchorMonthChange(
                                            RecurringScheduleHelper.encodeOnceAnchor(selectedYear, selectedMonth)
                                        )
                                        onDayOfMonthChange(selectedDay.coerceIn(1, 31))
                                    },
                                    onceYear,
                                    onceMonth,
                                    onceDay
                                ).show()
                            }
                            .testTag("reminder_once_date_picker")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = Localization.tr(lang, "Reminder date", "Ημερομηνία υπενθύμισης"),
                                    tint = Emerald700,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = Localization.tr(lang, "Reminder date", "Ημερομηνία υπενθύμισης"),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = formattedOnceDate,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else if (isWeeklyMode) {
                    val selectedIso = dayOfMonth.coerceIn(1, 7)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, pillBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { showWeekdayDropdown = true }
                                .testTag("reminder_weekday_dropdown")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Localization.weekdayLabel(lang),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = RecurringScheduleHelper.weekdayName(selectedIso, lang),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = Localization.weekdayLabel(lang),
                                    tint = Emerald700,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showWeekdayDropdown,
                            onDismissRequest = { showWeekdayDropdown = false }
                        ) {
                            (1..7).forEach { isoDow ->
                                val isSelected = selectedIso == isoDow
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = RecurringScheduleHelper.weekdayName(isoDow, lang),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Emerald700 else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Emerald700,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        onDayOfMonthChange(isoDow)
                                        showWeekdayDropdown = false
                                    },
                                    modifier = Modifier.testTag("reminder_weekday_chip_$isoDow")
                                )
                            }
                        }
                    }
                } else {
                    // Compact Day of Month Stepper + Tap-for-List Dropdown
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, pillBorderColor),
                        modifier = Modifier
                            .weight(if (needsAnchorMonth) 1.15f else 1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = {
                                        onDayOfMonthChange(if (dayOfMonth <= 1) 31 else dayOfMonth - 1)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("reminder_day_minus")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = Localization.tr(lang, "Decrease day", "Μείωση ημέρας"),
                                        tint = Emerald700,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showReminderDayDropdown = true }
                                        .padding(vertical = 4.dp)
                                        .testTag("reminder_day_value")
                                ) {
                                    Text(
                                        text = Localization.tr(lang, "Day", "Ημέρα"),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = Localization.trFormat(lang, "Day %1\$d", "Κάθε %1\$dη", dayOfMonth.coerceIn(1, 31)),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onDayOfMonthChange(if (dayOfMonth >= 31) 1 else dayOfMonth + 1)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("reminder_day_plus")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = Localization.tr(lang, "Increase day", "Αύξηση ημέρας"),
                                        tint = Emerald700,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showReminderDayDropdown,
                                onDismissRequest = { showReminderDayDropdown = false }
                            ) {
                                (1..31).forEach { day ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = Localization.trFormat(lang, "Day %1\$d", "Κάθε %1\$dη", day),
                                                fontWeight = if (day == dayOfMonth) FontWeight.Bold else FontWeight.Normal,
                                                color = if (day == dayOfMonth) Emerald700 else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            onDayOfMonthChange(day)
                                            showReminderDayDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (needsAnchorMonth) {
                        Box(modifier = Modifier.weight(0.95f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, pillBorderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { showAnchorMonthDropdown = true }
                                    .testTag("reminder_anchor_month_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = Localization.anchorMonthLabel(lang),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = RecurringScheduleHelper.monthNameShort(anchorMonth, lang),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = Localization.anchorMonthLabel(lang),
                                        tint = Emerald700,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showAnchorMonthDropdown,
                                onDismissRequest = { showAnchorMonthDropdown = false }
                            ) {
                                (0..11).forEach { mIdx ->
                                    val isSelected = anchorMonth.coerceIn(0, 11) == mIdx
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = RecurringScheduleHelper.monthNameShort(mIdx, lang),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Emerald700 else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            onAnchorMonthChange(mIdx)
                                            showAnchorMonthDropdown = false
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
internal fun ReceiptSourceChooserDialog(
    lang: Localization.Language,
    onDismiss: () -> Unit,
    onChooseCamera: () -> Unit,
    onChooseGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Localization.tr(lang, "Scan Receipt", "Σάρωση Απόδειξης"),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = Localization.tr(lang, "Choose how to scan your receipt:", "Επιλέξτε τρόπο καταχώρησης απόδειξης:"),
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onChooseCamera,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("choose_camera_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.tr(lang, "Camera", "Κάμερα"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Button(
                    onClick = onChooseGallery,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("choose_gallery_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp), tint = Emerald700)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.tr(lang, "Gallery", "Συλλογή"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Localization.cancel(lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
