package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.Emerald700
import com.example.ui.viewmodel.AdvancedExpenseFilter
import com.example.util.LocalCurrencySymbol
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdvancedFiltersDialog(
    currentFilter: AdvancedExpenseFilter,
    selectedCategoryFilter: Long?,
    members: List<FamilyMember>,
    categories: List<ExpenseCategory>,
    lang: Localization.Language,
    onApply: (AdvancedExpenseFilter, Long?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currencySymbol = LocalCurrencySymbol.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    var minAmountText by remember(currentFilter) {
        mutableStateOf(currentFilter.minAmount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var maxAmountText by remember(currentFilter) {
        mutableStateOf(currentFilter.maxAmount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var startDateMs by remember(currentFilter) { mutableStateOf(currentFilter.startDateMs) }
    var endDateMs by remember(currentFilter) { mutableStateOf(currentFilter.endDateMs) }
    var payerMemberId by remember(currentFilter) { mutableStateOf(currentFilter.payerMemberId) }
    var categoryId by remember(selectedCategoryFilter) { mutableStateOf(selectedCategoryFilter) }
    var payerMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val selectedPayer = remember(members, payerMemberId) { members.find { it.id == payerMemberId } }
    val selectedCategory = remember(categories, categoryId) { categories.find { it.id == categoryId } }

    val activeFilterCount = remember(minAmountText, maxAmountText, startDateMs, endDateMs, payerMemberId, categoryId) {
        var count = 0
        if (minAmountText.isNotBlank() || maxAmountText.isNotBlank()) count++
        if (startDateMs != null || endDateMs != null) count++
        if (payerMemberId != null) count++
        if (categoryId != null) count++
        count
    }

    fun applyQuickDatePreset(presetKey: String) {
        val nowEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            when (presetKey) {
                "7d" -> add(Calendar.DAY_OF_YEAR, -6)
                "30d" -> add(Calendar.DAY_OF_YEAR, -29)
                "year" -> {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }
        val targetStart = startCal.timeInMillis
        if (startDateMs == targetStart && endDateMs != null) {
            startDateMs = null
            endDateMs = null
        } else {
            startDateMs = targetStart
            endDateMs = nowEnd
        }
    }

    fun isDatePresetSelected(presetKey: String): Boolean {
        val start = startDateMs ?: return false
        if (endDateMs == null) return false
        val expected = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            when (presetKey) {
                "7d" -> add(Calendar.DAY_OF_YEAR, -6)
                "30d" -> add(Calendar.DAY_OF_YEAR, -29)
                "year" -> {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }.timeInMillis
        return kotlin.math.abs(start - expected) < 60_000L
    }

    fun openDatePicker(initialMs: Long?, isEndOfDay: Boolean, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply {
            if (initialMs != null) timeInMillis = initialMs
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    if (isEndOfDay) {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    } else {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                }
                onPicked(picked.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .widthIn(max = 460.dp)
                .testTag("advanced_filters_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald700.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = Localization.tr(lang, "Advanced Filters", "Προηγμένα Φίλτρα"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (activeFilterCount > 0) {
                                    Localization.trFormat(
                                        lang,
                                        "%1\$d active",
                                        "%1\$d ενεργά",
                                        activeFilterCount
                                    )
                                } else {
                                    Localization.tr(lang, "Filter by amount, date, payer or category", "Φιλτράρισμα ποσού, ημερομηνίας, πληρωτή, κατηγορίας")
                                },
                                fontSize = 11.5.sp,
                                fontWeight = if (activeFilterCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (activeFilterCount > 0) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("adv_filter_close_btn")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = Localization.close(lang),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Amount Range Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = Localization.tr(lang, "Amount Range", "Εύρος Ποσού"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = minAmountText,
                                    onValueChange = { minAmountText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                                    label = { Text("${Localization.tr(lang, "Min", "Ελάχ.")} ($currencySymbol)", fontSize = 12.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Emerald700,
                                        focusedLabelColor = Emerald700,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("adv_filter_min_amount")
                                )
                                OutlinedTextField(
                                    value = maxAmountText,
                                    onValueChange = { maxAmountText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                                    label = { Text("${Localization.tr(lang, "Max", "Μέγ.")} ($currencySymbol)", fontSize = 12.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Emerald700,
                                        focusedLabelColor = Emerald700,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("adv_filter_max_amount")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(20, 50, 100).forEach { preset ->
                                    val isPresetSelected = minAmountText.replace(",", ".").toDoubleOrNull() == preset.toDouble() &&
                                        maxAmountText.isBlank()
                                    FilterPresetPill(
                                        label = ">$preset$currencySymbol",
                                        selected = isPresetSelected,
                                        onClick = {
                                            if (isPresetSelected) {
                                                minAmountText = ""
                                            } else {
                                                minAmountText = preset.toString()
                                                maxAmountText = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("adv_filter_preset_$preset")
                                    )
                                }
                            }
                        }
                    }

                    // 2. Date Range Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = Localization.tr(lang, "Date Range", "Εύρος Ημερομηνιών"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DateRangePickerPill(
                                    label = startDateMs?.let { dateFormat.format(Date(it)) }
                                        ?: Localization.tr(lang, "From", "Από"),
                                    hasValue = startDateMs != null,
                                    onClick = { openDatePicker(startDateMs, isEndOfDay = false) { startDateMs = it } },
                                    onClear = { startDateMs = null },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("adv_filter_date_from")
                                )
                                DateRangePickerPill(
                                    label = endDateMs?.let { dateFormat.format(Date(it)) }
                                        ?: Localization.tr(lang, "To", "Έως"),
                                    hasValue = endDateMs != null,
                                    onClick = { openDatePicker(endDateMs, isEndOfDay = true) { endDateMs = it } },
                                    onClear = { endDateMs = null },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("adv_filter_date_to")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterPresetPill(
                                    label = Localization.tr(lang, "7 days", "7 ημέρες"),
                                    selected = isDatePresetSelected("7d"),
                                    onClick = { applyQuickDatePreset("7d") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("adv_filter_date_preset_7d")
                                )
                                FilterPresetPill(
                                    label = Localization.tr(lang, "30 days", "30 ημέρες"),
                                    selected = isDatePresetSelected("30d"),
                                    onClick = { applyQuickDatePreset("30d") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("adv_filter_date_preset_30d")
                                )
                                FilterPresetPill(
                                    label = Localization.tr(lang, "This Year", "Φέτος"),
                                    selected = isDatePresetSelected("year"),
                                    onClick = { applyQuickDatePreset("year") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("adv_filter_date_preset_year")
                                )
                            }
                        }
                    }

                    // 3. Payer & Category Dropdown Menus
                    if (members.isNotEmpty() || categories.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Payer Dropdown
                                if (members.isNotEmpty()) {
                                    ExposedDropdownMenuBox(
                                        expanded = payerMenuExpanded,
                                        onExpandedChange = { payerMenuExpanded = !payerMenuExpanded },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = selectedPayer?.name ?: Localization.allFilter(lang),
                                            onValueChange = {},
                                            readOnly = true,
                                            singleLine = true,
                                            label = {
                                                Text(
                                                    text = Localization.tr(lang, "Payer", "Πληρωτής"),
                                                    fontSize = 12.sp
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = if (selectedPayer != null) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerMenuExpanded)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Emerald700,
                                                focusedLabelColor = Emerald700,
                                                unfocusedBorderColor = if (selectedPayer != null) Emerald700.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedContainerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                                .testTag("adv_filter_payer_dropdown")
                                        )
                                        ExposedDropdownMenu(
                                            expanded = payerMenuExpanded,
                                            onDismissRequest = { payerMenuExpanded = false },
                                            modifier = Modifier.heightIn(max = 260.dp)
                                        ) {
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = Localization.allFilter(lang),
                                                            fontWeight = if (payerMemberId == null) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (payerMemberId == null) Emerald700 else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (payerMemberId == null) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = Emerald700,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    payerMemberId = null
                                                    payerMenuExpanded = false
                                                }
                                            )
                                            members.forEach { member ->
                                                val isSelected = payerMemberId == member.id
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = member.name,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected) Emerald700 else MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            if (isSelected) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = Emerald700,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        payerMemberId = member.id
                                                        payerMenuExpanded = false
                                                    },
                                                    modifier = Modifier.testTag("adv_filter_payer_${member.id}")
                                                )
                                            }
                                        }
                                    }
                                }

                                // Category Dropdown
                                if (categories.isNotEmpty()) {
                                    val selectedCatColor = selectedCategory?.let {
                                        if (it.colorHex != 0L) Color(it.colorHex) else Emerald700
                                    } ?: MaterialTheme.colorScheme.onSurfaceVariant
                                    ExposedDropdownMenuBox(
                                        expanded = categoryMenuExpanded,
                                        onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = selectedCategory?.let { Localization.translateCategory(it.name, lang) }
                                                ?: Localization.allCategoriesFilter(lang),
                                            onValueChange = {},
                                            readOnly = true,
                                            singleLine = true,
                                            label = {
                                                Text(
                                                    text = Localization.tr(lang, "Category", "Κατηγορία"),
                                                    fontSize = 12.sp
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = selectedCategory?.let { CategoryIconHelper.getIcon(it.iconKey) }
                                                        ?: Icons.Default.Category,
                                                    contentDescription = null,
                                                    tint = selectedCatColor,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Emerald700,
                                                focusedLabelColor = Emerald700,
                                                unfocusedBorderColor = if (selectedCategory != null) Emerald700.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedContainerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                                .testTag("adv_filter_category_dropdown")
                                        )
                                        ExposedDropdownMenu(
                                            expanded = categoryMenuExpanded,
                                            onDismissRequest = { categoryMenuExpanded = false },
                                            modifier = Modifier.heightIn(max = 280.dp)
                                        ) {
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = Emerald700.copy(alpha = 0.12f),
                                                                modifier = Modifier.size(26.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Category,
                                                                        contentDescription = null,
                                                                        tint = Emerald700,
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                }
                                                            }
                                                            Text(
                                                                text = Localization.allCategoriesFilter(lang),
                                                                fontWeight = if (categoryId == null) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (categoryId == null) Emerald700 else MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                        if (categoryId == null) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = Emerald700,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    categoryId = null
                                                    categoryMenuExpanded = false
                                                }
                                            )
                                            categories.forEach { cat ->
                                                val isSelected = categoryId == cat.id
                                                val catColor = if (cat.colorHex != 0L) Color(cat.colorHex) else Emerald700
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Surface(
                                                                    shape = CircleShape,
                                                                    color = catColor.copy(alpha = 0.16f),
                                                                    modifier = Modifier.size(26.dp)
                                                                ) {
                                                                    Box(contentAlignment = Alignment.Center) {
                                                                        Icon(
                                                                            imageVector = CategoryIconHelper.getIcon(cat.iconKey),
                                                                            contentDescription = null,
                                                                            tint = catColor,
                                                                            modifier = Modifier.size(14.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Text(
                                                                    text = Localization.translateCategory(cat.name, lang),
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (isSelected) Emerald700 else MaterialTheme.colorScheme.onSurface,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                            if (isSelected) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = Emerald700,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        categoryId = cat.id
                                                        categoryMenuExpanded = false
                                                    },
                                                    modifier = Modifier.testTag("adv_filter_category_${cat.id}")
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Single-row balanced footer bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onReset()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.40f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 46.dp)
                            .testTag("adv_filter_reset_btn")
                    ) {
                        Text(
                            text = Localization.tr(lang, "Clear", "Καθαρισμός"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Button(
                        onClick = {
                            val minVal = minAmountText.replace(",", ".").toDoubleOrNull()?.takeIf { it >= 0.0 }
                            val maxVal = maxAmountText.replace(",", ".").toDoubleOrNull()?.takeIf { it >= 0.0 }
                            val normalizedStart = if (startDateMs != null && endDateMs != null && startDateMs!! > endDateMs!!) endDateMs else startDateMs
                            val normalizedEnd = if (startDateMs != null && endDateMs != null && startDateMs!! > endDateMs!!) startDateMs else endDateMs
                            onApply(
                                AdvancedExpenseFilter(
                                    minAmount = minVal,
                                    maxAmount = maxVal,
                                    startDateMs = normalizedStart,
                                    endDateMs = normalizedEnd,
                                    payerMemberId = payerMemberId
                                ),
                                categoryId
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 46.dp)
                            .testTag("adv_filter_apply_btn")
                    ) {
                        Text(
                            text = Localization.tr(lang, "Apply Filters", "Εφαρμογή"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPresetPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Emerald700.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.2.dp else 1.dp,
            color = if (selected) Emerald700.copy(alpha = 0.60f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DateRangePickerPill(
    label: String,
    hasValue: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (hasValue) Emerald700.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (hasValue) 1.2.dp else 1.dp,
            color = if (hasValue) Emerald700.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (hasValue) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    fontSize = 12.5.sp,
                    fontWeight = if (hasValue) FontWeight.Bold else FontWeight.Medium,
                    color = if (hasValue) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasValue) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear date",
                    tint = Emerald700,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onClear() }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActiveAdvancedFiltersSummaryRow(
    filter: AdvancedExpenseFilter,
    members: List<FamilyMember>,
    lang: Localization.Language,
    onEditFilters: () -> Unit,
    onClearFilters: () -> Unit
) {
    if (!filter.isActive) return
    val currencySymbol = LocalCurrencySymbol.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

    val chips = buildList {
        if (filter.minAmount != null && filter.maxAmount != null) {
            add("${filter.minAmount.toInt()}–${filter.maxAmount.toInt()} $currencySymbol")
        } else if (filter.minAmount != null) {
            add(">${filter.minAmount.toInt()} $currencySymbol")
        } else if (filter.maxAmount != null) {
            add("<${filter.maxAmount.toInt()} $currencySymbol")
        }
        if (filter.startDateMs != null || filter.endDateMs != null) {
            val fromStr = filter.startDateMs?.let { dateFormat.format(Date(it)) } ?: "…"
            val toStr = filter.endDateMs?.let { dateFormat.format(Date(it)) } ?: "…"
            add("$fromStr – $toStr")
        }
        filter.payerMemberId?.let { pid ->
            val payerName = members.find { it.id == pid }?.name
            if (!payerName.isNullOrBlank()) {
                add("${Localization.tr(lang, "Payer", "Πληρωτής")}: $payerName")
            }
        }
    }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        chips.forEach { label ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Emerald700.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.32f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onEditFilters() }
            ) {
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald700,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFEF4444).copy(alpha = 0.10f),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.30f)),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onClearFilters() }
                .testTag("clear_advanced_filters_chip")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = Localization.tr(lang, "Clear", "Καθαρισμός"),
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = Localization.tr(lang, "Clear", "Καθαρισμός"),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
internal fun BulkSelectionActionBar(
    selectedCount: Int,
    canChangeCategory: Boolean,
    allSelected: Boolean,
    lang: Localization.Language,
    onToggleSelectAll: () -> Unit,
    onChangeCategoryClick: () -> Unit,
    onExportSelectedClick: () -> Unit,
    onCloseSelection: () -> Unit
) {
    Surface(
        color = Color(0xFF064E3B),
        contentColor = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bulk_selection_action_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onCloseSelection,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("bulk_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = Localization.close(lang),
                        tint = Color.White
                    )
                }
                Text(
                    text = Localization.trFormat(
                        lang,
                        "%1\$d selected",
                        "%1\$d επιλεγμένα",
                        selectedCount
                    ),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onToggleSelectAll,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("bulk_select_all_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = if (allSelected) {
                            Localization.tr(lang, "Deselect all", "Αποεπιλογή όλων")
                        } else {
                            Localization.tr(lang, "Select all", "Επιλογή όλων")
                        },
                        tint = if (allSelected) Color(0xFF6EE7B7) else Color.White
                    )
                }

                if (canChangeCategory) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onChangeCategoryClick() }
                            .testTag("bulk_change_category_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = Localization.tr(lang, "Category", "Κατηγορία"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onExportSelectedClick() }
                        .testTag("bulk_export_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = Localization.tr(lang, "Export", "Εξαγωγή"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BulkCategoryPickerDialog(
    selectedCount: Int,
    categories: List<ExpenseCategory>,
    lang: Localization.Language,
    onSelectCategory: (ExpenseCategory) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = Localization.trFormat(
                    lang,
                    "Change Category (%1\$d)",
                    "Αλλαγή Κατηγορίας (%1\$d)",
                    selectedCount
                ),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories, key = { it.id }) { cat ->
                    val catColor = if (cat.colorHex != 0L) Color(cat.colorHex) else Emerald700
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectCategory(cat) }
                            .testTag("bulk_pick_category_${cat.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = catColor.copy(alpha = 0.16f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(cat.iconKey),
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = Localization.translateCategory(cat.name, lang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Localization.cancel(lang))
            }
        }
    )
}

@Composable
internal fun BulkExportFormatDialog(
    selectedCount: Int,
    lang: Localization.Language,
    onExportCsv: () -> Unit,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = Localization.trFormat(
                    lang,
                    "Export Selected (%1\$d)",
                    "Εξαγωγή Επιλεγμένων (%1\$d)",
                    selectedCount
                ),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BulkExportOptionButton(
                    icon = Icons.Default.TableChart,
                    title = "Excel (.xls)",
                    subtitle = Localization.tr(lang, "Formatted tables & totals", "Μορφοποιημένοι πίνακες & σύνολα"),
                    accent = Emerald700,
                    onClick = {
                        onExportExcel()
                        onDismiss()
                    },
                    testTag = "bulk_export_excel_btn"
                )
                BulkExportOptionButton(
                    icon = Icons.Default.PictureAsPdf,
                    title = "PDF (.pdf)",
                    subtitle = Localization.tr(lang, "Ready to print or share", "Έτοιμο για εκτύπωση ή κοινοποίηση"),
                    accent = Color(0xFFDC2626),
                    onClick = {
                        onExportPdf()
                        onDismiss()
                    },
                    testTag = "bulk_export_pdf_btn"
                )
                BulkExportOptionButton(
                    icon = Icons.Default.Description,
                    title = "CSV (.csv)",
                    subtitle = Localization.tr(lang, "Spreadsheet & accounting data", "Δεδομένα για φύλλα εργασίας"),
                    accent = Color(0xFF0284C7),
                    onClick = {
                        onExportCsv()
                        onDismiss()
                    },
                    testTag = "bulk_export_csv_btn"
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Localization.cancel(lang))
            }
        }
    )
}

@Composable
private fun BulkExportOptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun OlderHistoryFooterCard(
    isLoading: Boolean,
    lang: Localization.Language,
    onLoadOlderHistory: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        OutlinedButton(
            onClick = onLoadOlderHistory,
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.45f)),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            modifier = Modifier
                .defaultMinSize(minHeight = 44.dp)
                .testTag("load_older_history_btn")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Emerald700
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Localization.tr(lang, "Loading older history...", "Φόρτωση παλαιότερου ιστορικού..."),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald700
                )
            } else {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Localization.tr(lang, "Load older history", "Φόρτωση παλαιότερου ιστορικού"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald700
                )
            }
        }
    }
}
