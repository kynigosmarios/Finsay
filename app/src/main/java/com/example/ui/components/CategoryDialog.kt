package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ExpenseCategory
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDialog(
    categoryToEdit: ExpenseCategory?,
    isIncomeCategory: Boolean = categoryToEdit?.isIncomeCategory == true,
    appLanguage: String = "EL",
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        iconKey: String,
        monthlyBudget: Double,
        colorHex: Long,
        voiceKeywords: String,
        fontColorHex: Long
    ) -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val effectiveIsIncome = categoryToEdit?.isIncomeCategory ?: isIncomeCategory

    var name by remember(categoryToEdit) { mutableStateOf(categoryToEdit?.name ?: "") }
    var budgetText by remember(categoryToEdit) {
        mutableStateOf(
            categoryToEdit?.let { String.format(Locale.US, "%.0f", it.monthlyBudget) } ?: "0"
        )
    }
    var selectedIconKey by remember(categoryToEdit, effectiveIsIncome) {
        mutableStateOf(categoryToEdit?.iconKey ?: if (effectiveIsIncome) "work" else "cart")
    }
    var selectedColorHex by remember(categoryToEdit) {
        mutableLongStateOf(categoryToEdit?.colorHex ?: 0xFF059669L)
    }
    var voiceKeywords by remember(categoryToEdit) { mutableStateOf(categoryToEdit?.voiceKeywords ?: "") }
    var errorText by remember(categoryToEdit) { mutableStateOf<String?>(null) }
    var isIconPickerExpanded by remember { mutableStateOf(false) }
    var showVoiceKeywordsHint by remember { mutableStateOf(false) }
    var hasShownVoiceKeywordsHint by remember { mutableStateOf(false) }

    if (showVoiceKeywordsHint) {
        AlertDialog(
            onDismissRequest = { showVoiceKeywordsHint = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(26.dp)
                )
            },
            title = {
                Text(
                    text = Localization.tr(lang, "Voice Keywords", "Φωνητικές Λέξεις"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = Localization.tr(
                        lang,
                        "Enter words separated by commas (e.g. market, lidl, groceries). When you speak an expense, Finsay automatically matches this category whenever it hears any of these words.",
                        "Γράψτε λέξεις-κλειδιά χωρισμένες με κόμμα (π.χ. σούπερ μάρκετ, lidl, ψώνια). Όταν καταχωρείτε έξοδο φωνητικά, το Finsay αναγνωρίζει αυτόματα αυτή την κατηγορία μόλις ακούσει κάποια από αυτές."
                    ),
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showVoiceKeywordsHint = false },
                    modifier = Modifier.testTag("category_voice_hint_ok_btn")
                ) {
                    Text(
                        text = Localization.tr(lang, "Got it", "Κατάλαβα"),
                        fontWeight = FontWeight.Bold,
                        color = Emerald700
                    )
                }
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
                    .widthIn(max = 520.dp)
                    .fillMaxHeight(0.92f)
                    .padding(vertical = 12.dp)
                    .testTag("category_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 1. Fixed Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val headerTitle = if (effectiveIsIncome) {
                        if (categoryToEdit == null) {
                            Localization.tr(lang, "New Income Category", "Νέα Κατηγορία Εσόδου")
                        } else {
                            Localization.tr(lang, "Edit Income Category", "Επεξεργασία Κατηγορίας Εσόδου")
                        }
                    } else {
                        if (categoryToEdit == null) Localization.addCategory(lang) else Localization.editCategoryTitle(lang)
                    }
                    Text(
                        text = headerTitle,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(selectedColorHex)
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, contentDescription = Localization.close(lang), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Middle Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // LIVE PREVIEW CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.5.dp, Color(selectedColorHex).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = Localization.tr(lang, "Preview", "Προεπισκόπηση"),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Simulated expense item inside ledger
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val previewFontColor = MaterialTheme.colorScheme.onSurface
                                val previewCatColor = Color(selectedColorHex)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(previewCatColor.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(selectedIconKey),
                                            contentDescription = null,
                                            tint = previewCatColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name.ifBlank {
                                                if (effectiveIsIncome) {
                                                    Localization.tr(lang, "Sample Income", "Παράδειγμα Εσόδου")
                                                } else {
                                                    Localization.tr(lang, "Sample Expense", "Παράδειγμα Εξόδου")
                                                }
                                            },
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = previewFontColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = name.ifBlank { Localization.translateCategory(selectedIconKey, lang) },
                                            fontSize = 11.5.sp,
                                            color = previewCatColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    val currencySymbol = com.example.util.LocalCurrencySymbol.current
                                    Text(
                                        text = if (effectiveIsIncome) "+350.00 $currencySymbol" else "35.50 $currencySymbol",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (effectiveIsIncome) Emerald700 else previewFontColor
                                    )
                                }
                            }
                        }
                    }

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorText = null
                        },
                        label = { Text(Localization.categoryNameLabel(lang)) },
                        placeholder = { Text(Localization.categoryNamePlaceholder(lang)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (!effectiveIsIncome) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Monthly Budget (Expense Categories only)
                        OutlinedTextField(
                            value = budgetText,
                            onValueChange = {
                                budgetText = it.replace(',', '.')
                                errorText = null
                            },
                            label = { Text(Localization.monthlyBudgetLimitLabel(lang)) },
                            leadingIcon = { Icon(Icons.Default.Euro, contentDescription = null, tint = Emerald700) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. CATEGORY COLOR SELECTION
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = Color(selectedColorHex),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.tr(lang, "Category & Icon Color", "Χρώμα Κατηγορίας & Εικονιδίου"),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIconHelper.availableCategoryColors.forEach { (colorLong, _) ->
                            val color = Color(colorLong)
                            val isSelected = selectedColorHex == colorLong
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = colorLong },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. ICON SELECTION (Collapsible)
                    val selectedIconDisplay = remember(selectedIconKey, lang) {
                        val rawLabel = CategoryIconHelper.availableIcons.find { it.first == selectedIconKey }?.second ?: selectedIconKey
                        Localization.translateCategory(rawLabel, lang)
                    }
                    val iconChevronRotation by animateFloatAsState(
                        targetValue = if (isIconPickerExpanded) 180f else 0f,
                        animationSpec = tween(220),
                        label = "category_icon_picker_chevron"
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            if (isIconPickerExpanded) Color(selectedColorHex).copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { isIconPickerExpanded = !isIconPickerExpanded }
                            .testTag("category_icon_picker_toggle")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(selectedColorHex).copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(selectedIconKey),
                                    contentDescription = null,
                                    tint = Color(selectedColorHex),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Localization.categoryIconLabel(lang),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = selectedIconDisplay,
                                    fontSize = 11.5.sp,
                                    color = Color(selectedColorHex),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(iconChevronRotation)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isIconPickerExpanded,
                        enter = FinsayMotion.ExpandEnter,
                        exit = FinsayMotion.ShrinkExit
                    ) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CategoryIconHelper.availableIcons.forEach { (key, label) ->
                                val isSelected = selectedIconKey == key
                                val displayLabel = Localization.translateCategory(label, lang)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedIconKey = key },
                                    label = { Text(displayLabel, fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(key),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isSelected) Color(selectedColorHex) else Color.Gray
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Voice Keywords (with instruction popup on first focus + help button)
                    OutlinedTextField(
                        value = voiceKeywords,
                        onValueChange = { voiceKeywords = it },
                        label = {
                            Text(
                                text = Localization.categoryKeywordsLabel(lang),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        placeholder = { Text(Localization.categoryKeywordsPlaceholder(lang)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { showVoiceKeywordsHint = true },
                                modifier = Modifier.testTag("category_voice_keywords_info_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = Localization.tr(lang, "Voice keywords help", "Οδηγίες φωνητικών λέξεων"),
                                    tint = Emerald700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && !hasShownVoiceKeywordsHint) {
                                    hasShownVoiceKeywordsHint = true
                                    showVoiceKeywordsHint = true
                                }
                            }
                            .testTag("category_voice_keywords_input"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    if (errorText != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = errorText ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // 3. Fixed Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = Localization.cancel(lang),
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            val parsedBudget = if (effectiveIsIncome) 0.0 else budgetText.toDoubleOrNull()
                            if (name.isBlank()) {
                                errorText = Localization.tr(lang, "Please enter category name", "Συμπληρώστε όνομα κατηγορίας")
                                return@Button
                            }
                            if (parsedBudget == null || parsedBudget < 0) {
                                errorText = Localization.tr(lang, "Please enter a valid budget", "Συμπληρώστε έγκυρο budget")
                                return@Button
                            }

                            onSave(
                                categoryToEdit?.id ?: 0L,
                                name.trim(),
                                selectedIconKey,
                                parsedBudget,
                                selectedColorHex,
                                voiceKeywords.trim(),
                                0L
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(selectedColorHex), contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_category_button"),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Localization.save(lang),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
        }
    }
}
