package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.ExpenseCategory
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.SmoothDialogContent
import com.example.ui.theme.Emerald700
import com.example.util.LocalCurrencySymbol
import com.example.util.Localization
import java.util.Locale

@Composable
fun CategoriesScreen(
    categories: List<ExpenseCategory>,
    incomeCategories: List<ExpenseCategory> = emptyList(),
    appLanguage: String,
    canManage: Boolean = true,
    isPro: Boolean = false,
    deletedCategories: List<ExpenseCategory> = emptyList(),
    onAddCategory: (String) -> Unit,
    onEditCategory: (ExpenseCategory) -> Unit,
    onDeleteCategory: (ExpenseCategory) -> Unit,
    onRestoreCategory: (ExpenseCategory) -> Unit = {},
    onPermanentDeleteCategory: (ExpenseCategory) -> Unit = {},
    onMoveCategoryUp: (ExpenseCategory) -> Unit,
    onMoveCategoryDown: (ExpenseCategory) -> Unit,
    onSeedStandardCategories: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val isGreek = lang != Localization.Language.ENGLISH
    val currencySymbol = LocalCurrencySymbol.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var selectedCategoryType by remember { mutableStateOf("EXPENSE") }
    var categoryToDelete by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showDeletedCategoriesDialog by remember { mutableStateOf(false) }
    var categoryToPermanentlyDelete by remember { mutableStateOf<ExpenseCategory?>(null) }

    val isIncomeTab = selectedCategoryType == "INCOME"
    val displayedCategories = if (isIncomeTab) incomeCategories else categories
    val displayedDeletedCategories = remember(deletedCategories, isIncomeTab) {
        deletedCategories.filter { it.isIncomeCategory == isIncomeTab }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Expense / Income Category Type Switcher
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (!isIncomeTab) Emerald700 else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedCategoryType = "EXPENSE" }
                        .testTag("categories_tab_expense")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = if (isGreek) "Έξοδα" else "Expenses",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isIncomeTab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isIncomeTab) Emerald700 else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedCategoryType = "INCOME" }
                        .testTag("categories_tab_income")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = if (isGreek) "Έσοδα" else "Income",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncomeTab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Categories Header Card
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
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isIncomeTab) {
                            if (isGreek) "Κατηγορίες εσόδων" else "Income Categories"
                        } else {
                            if (isGreek) "Κατηγορίες εξόδων" else "Expense Categories"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!isPro) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isGreek) {
                                "${displayedCategories.size}/5 στη δωρεάν έκδοση • Pro για απεριόριστες"
                            } else {
                                "${displayedCategories.size}/5 in Free plan • Pro for unlimited"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (canManage) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAddCategory(selectedCategoryType) },
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("add_category_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald700,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isGreek) "Προσθήκη" else "Add",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (displayedCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFD1FAE5),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "No categories found" else "Δεν υπάρχουν κατηγορίες",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (canManage) {
                        Spacer(modifier = Modifier.height(18.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onAddCategory(selectedCategoryType) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("empty_add_category_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH) "Add Category" else "Προσθήκη",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }

                            Button(
                                onClick = { onSeedStandardCategories(selectedCategoryType) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("empty_seed_categories_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH) "Load Defaults" else "Έτοιμες Κατηγορίες",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = { showDeletedCategoriesDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("empty_deleted_categories_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGreek) "Διαγραμμένες Κατηγορίες (${displayedDeletedCategories.size})" else "Deleted Categories (${displayedDeletedCategories.size})",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "top_spacer", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(2.dp))
                }

                itemsIndexed(
                    items = displayedCategories,
                    key = { _, c -> if (c.id > 0) "cat_${c.categoryType}_${c.id}" else "cat_tmp_${c.categoryType}_${c.name}" },
                    contentType = { _, _ -> "category_card" }
                ) { index, category ->
                    val color = if (category.colorHex != 0L) Color(category.colorHex) else Emerald700

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .testTag("category_card_${category.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left clickable content (Icon + Details) opens Edit Dialog
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = canManage) { onEditCategory(category) }
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Icon in tinted container
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = color.copy(alpha = 0.16f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(category.iconKey),
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Details (Title + Budget)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Localization.translateCategory(category.name, lang),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!category.isIncomeCategory && category.monthlyBudget > 0.0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val budgetColor = if (isDark) Color(0xFF34D399) else Emerald700
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = budgetColor.copy(alpha = if (isDark) 0.22f else 0.12f)
                                        ) {
                                            Text(
                                                text = "🎯 ${String.format(Locale.US, "%.2f", category.monthlyBudget)} $currencySymbol",
                                                fontSize = 11.sp,
                                                color = budgetColor,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (canManage) {
                                Spacer(modifier = Modifier.width(4.dp))

                                // Actions: Reorder, Edit, Delete (Compact & closer together)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (displayedCategories.size > 1) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (index > 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                    else Color.Transparent
                                                )
                                                .clickable(enabled = index > 0) { onMoveCategoryUp(category) }
                                                .testTag("move_up_cat_${category.id}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = "Move Up",
                                                modifier = Modifier.size(14.dp),
                                                tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (index < displayedCategories.size - 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                    else Color.Transparent
                                                )
                                                .clickable(enabled = index < displayedCategories.size - 1) { onMoveCategoryDown(category) }
                                                .testTag("move_down_cat_${category.id}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "Move Down",
                                                modifier = Modifier.size(14.dp),
                                                tint = if (index < displayedCategories.size - 1) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent
                                            )
                                        }
                                    }

                                    val editBtnColor = if (isDark) Color(0xFF34D399) else Emerald700
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(editBtnColor.copy(alpha = if (isDark) 0.22f else 0.12f))
                                            .clickable { onEditCategory(category) }
                                            .testTag("edit_cat_${category.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            modifier = Modifier.size(14.dp),
                                            tint = editBtnColor
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                            .clickable { categoryToDelete = category }
                                            .testTag("delete_cat_${category.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(14.dp),
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (canManage) {
                    item(key = "deleted_categories_btn", contentType = "footer_button") {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showDeletedCategoriesDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("deleted_categories_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGreek) "Διαγραμμένες Κατηγορίες (${displayedDeletedCategories.size})" else "Deleted Categories (${displayedDeletedCategories.size})",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                item(key = "bottom_spacer", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // 1. Soft-Delete Confirmation Dialog
    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (isGreek) "Διαγραφή Κατηγορίας;" else "Delete Category?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isGreek)
                        "«${cat.name}» θα μεταφερθεί στις Διαγραμμένες. Τα έξοδα και στατιστικά διατηρούνται."
                    else
                        "\"${cat.name}\" will move to Deleted Categories. Existing expenses and stats remain intact.",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(cat)
                        categoryToDelete = null
                    }
                ) {
                    Text(
                        text = if (isGreek) "Διαγραφή" else "Delete",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(if (isGreek) "Ακύρωση" else "Cancel")
                }
            }
        )
    }

    // 2. Recycle Bin Dialog ("Διαγραμμένες Κατηγορίες")
    if (showDeletedCategoriesDialog) {
        Dialog(onDismissRequest = { showDeletedCategoriesDialog = false }) {
            SmoothDialogContent {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .testTag("deleted_categories_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isGreek) "Διαγραμμένες Κατηγορίες" else "Deleted Categories",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (displayedDeletedCategories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isGreek) "Δεν υπάρχουν διεγραμμένες κατηγορίες." else "No deleted categories.",
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((displayedDeletedCategories.size * 65).coerceAtMost(300).dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(displayedDeletedCategories, key = { _, c -> "del_cat_${c.categoryType}_${c.id}_${c.name}" }) { _, cat ->
                                val catColor = Color(cat.colorHex)
                                val iconVector = CategoryIconHelper.getIcon(cat.iconKey)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                                                color = catColor.copy(alpha = 0.18f),
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = iconVector,
                                                        contentDescription = null,
                                                        tint = catColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = cat.name,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (!cat.isIncomeCategory && cat.monthlyBudget > 0.0) {
                                                    Text(
                                                        text = "🎯 ${String.format(Locale.US, "%.2f", cat.monthlyBudget)} $currencySymbol",
                                                        fontSize = 10.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Restore button
                                            Button(
                                                onClick = {
                                                    onRestoreCategory(cat)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Restore,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = if (isGreek) "Επαναφορά" else "Restore",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Permanent delete icon
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                                    .clickable { categoryToPermanentlyDelete = cat }
                                                    .testTag("perm_del_cat_${cat.id}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Permanent Delete",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = if (isGreek)
                                "ℹ️ Η διαγραφή κατηγορίας δεν αλλοιώνει τα παλαιότερα στατιστικά και καταχωρημένα έξοδα."
                            else
                                "ℹ️ Deleting a category preserves all historical statistics and recorded expenses.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    TextButton(
                        onClick = { showDeletedCategoriesDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = if (isGreek) "Κλείσιμο" else "Close",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            }
        }
    }

    // 3. Permanent Category Deletion Confirmation
    if (categoryToPermanentlyDelete != null) {
        val target = categoryToPermanentlyDelete!!
        AlertDialog(
            onDismissRequest = { categoryToPermanentlyDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (isGreek) "Οριστική Διαγραφή;" else "Permanently Delete?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            },
            text = {
                Text(
                    text = if (isGreek)
                        "Είστε βέβαιοι ότι θέλετε να καταργήσετε οριστικά την κατηγορία «${target.name}»; Τα ιστορικά έξοδα και τα στατιστικά δεν θα αλλοιωθούν."
                    else
                        "Are you sure you want to permanently remove category \"${target.name}\"? Historical expenses and stats will remain intact.",
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPermanentDeleteCategory(target)
                        categoryToPermanentlyDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isGreek) "Οριστική Διαγραφή" else "Permanently Delete",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToPermanentlyDelete = null }) {
                    Text(if (isGreek) "Ακύρωση" else "Cancel")
                }
            }
        )
    }
}
