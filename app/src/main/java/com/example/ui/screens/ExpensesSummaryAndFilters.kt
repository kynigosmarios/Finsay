package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.ui.theme.Emerald700
import com.example.util.AnalyticsSpecialFilter
import com.example.util.LocalCurrencySymbol
import com.example.util.Localization
import java.util.Locale

@Composable
internal fun ExpensesHeroDashboardCard(
    currentMonthDisplay: String,
    availableMonths: List<Triple<String, String, Long>>,
    effectiveMonthKey: String,
    selectedTypeFilter: String,
    netBalance: Double,
    isWithinBudget: Boolean,
    totalIncomesAmount: Double,
    totalExpensesAmount: Double,
    heroExpensesCount: Int,
    heroIncomesCount: Int,
    totalBudget: Double,
    budgetPercent: Int,
    animatedBudgetProgress: Float,
    sharedExpenses: Double,
    individualExpenses: Double,
    lang: Localization.Language,
    onMonthKeyChange: (String) -> Unit,
    onAddManualExpense: () -> Unit,
    onAddManualIncome: () -> Unit,
    onTypeFilterChange: (String) -> Unit
) {
    val currencySymbol = LocalCurrencySymbol.current
    var showMonthDropdown by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("hero_balance_card"),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF03533C),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFF10B981).copy(alpha = 0.5f),
                    Color(0xFF059669).copy(alpha = 0.3f),
                    Color(0xFF10B981).copy(alpha = 0.2f)
                )
            )
        ),
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0A5851),
                            Color(0xFF04684A),
                            Color(0xFF03533C),
                            Color(0xFF023627)
                        )
                    )
                )
        ) {
            Box(modifier = Modifier.padding(18.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0A5541).copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clickable { showMonthDropdown = true }
                                    .testTag("month_selector_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34D399))
                                    )
                                    Text(
                                        text = currentMonthDisplay,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Month",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showMonthDropdown,
                                onDismissRequest = { showMonthDropdown = false }
                            ) {
                                if (availableMonths.size > 1) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (lang == Localization.Language.ENGLISH) "All Months" else "Όλοι οι μήνες",
                                                fontWeight = if (effectiveMonthKey == "ALL") FontWeight.Bold else FontWeight.Normal,
                                                color = if (effectiveMonthKey == "ALL") Emerald700 else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            onMonthKeyChange("ALL")
                                            showMonthDropdown = false
                                        }
                                    )
                                }
                                availableMonths.forEach { month ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = month.second,
                                                fontWeight = if (month.first == effectiveMonthKey) FontWeight.Bold else FontWeight.Normal,
                                                color = if (month.first == effectiveMonthKey) Emerald700 else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            onMonthKeyChange(month.first)
                                            showMonthDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        val isIncomeSelected = selectedTypeFilter == "INCOME"
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF065F46),
                            border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clickable {
                                    if (isIncomeSelected) {
                                        onAddManualIncome()
                                    } else {
                                        onAddManualExpense()
                                    }
                                }
                                .testTag(if (isIncomeSelected) "hero_add_income_btn" else "hero_add_expense_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isIncomeSelected) {
                                        if (lang == Localization.Language.ENGLISH) "Income" else "Έσοδο"
                                    } else {
                                        if (lang == Localization.Language.ENGLISH) "Expense" else "Έξοδο"
                                    },
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "NET BALANCE" else "ΚΑΘΑΡΟ ΥΠΟΛΟΙΠΟ",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA7F3D0),
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${String.format(Locale.US, "%s%.2f", if (netBalance >= 0) "+" else "", netBalance)} $currencySymbol",
                        fontSize = 35.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    if (totalBudget > 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF065F46).copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isWithinBudget) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isWithinBudget)
                                        (if (lang == Localization.Language.ENGLISH) "Within budget" else "Εντός προϋπολογισμού")
                                    else
                                        (if (lang == Localization.Language.ENGLISH) "Over budget" else "Εκτός προϋπολογισμού"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isWithinBudget) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTypeFilterChange("INCOME") },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF065F46).copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = Localization.income(lang),
                                        fontSize = 12.sp,
                                        color = Color(0xFFA7F3D0)
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", totalIncomesAmount)} $currencySymbol",
                                        fontSize = 17.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTypeFilterChange("EXPENSE") },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF451A1A).copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF7F1D1D),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = Color(0xFFFCA5A5),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = Localization.expenses(lang),
                                        fontSize = 12.sp,
                                        color = Color(0xFFFCA5A5)
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", totalExpensesAmount)} $currencySymbol",
                                        fontSize = 17.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val expenseTabBg by androidx.compose.animation.animateColorAsState(
                        targetValue = if (selectedTypeFilter == "EXPENSE") Color(0xFF10B981) else Color.Transparent,
                        animationSpec = androidx.compose.animation.core.tween(180),
                        label = "expense_tab_bg"
                    )
                    val incomeTabBg by androidx.compose.animation.animateColorAsState(
                        targetValue = if (selectedTypeFilter == "INCOME") Color(0xFF10B981) else Color.Transparent,
                        animationSpec = androidx.compose.animation.core.tween(180),
                        label = "income_tab_bg"
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF023225),
                        border = BorderStroke(1.dp, Color(0xFF065F46).copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onTypeFilterChange("EXPENSE") },
                                shape = RoundedCornerShape(12.dp),
                                color = expenseTabBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${Localization.expenses(lang)} ($heroExpensesCount)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTypeFilter == "EXPENSE") Color.White else Color(0xFFA7F3D0)
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onTypeFilterChange("INCOME") },
                                shape = RoundedCornerShape(12.dp),
                                color = incomeTabBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${Localization.income(lang)} ($heroIncomesCount)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTypeFilter == "INCOME") Color.White else Color(0xFFA7F3D0)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", totalExpensesAmount)} $currencySymbol / ${String.format(Locale.US, "%.2f", totalBudget)} $currencySymbol",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF065F46).copy(alpha = 0.8f)
                        ) {
                            val remaining = totalBudget - totalExpensesAmount
                            Text(
                                text = if (totalBudget > 0.0) {
                                    "$budgetPercent% (${String.format(Locale.US, "%s%.2f", if (remaining >= 0) "+" else "", remaining)} $currencySymbol)"
                                } else {
                                    "0%"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalExpensesAmount <= totalBudget) Color(0xFFA7F3D0) else Color(0xFFFCA5A5),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { animatedBudgetProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF34D399),
                        trackColor = Color(0xFF065F46).copy(alpha = 0.7f),
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF065F46).copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = Color(0xFFA7F3D0),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Shared" else "Κοινά Σπιτιού",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFA7F3D0)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", sharedExpenses)} $currencySymbol",
                                    fontSize = 17.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF065F46).copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFFA7F3D0),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Personal" else "Ατομικά",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFA7F3D0)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", individualExpenses)} $currencySymbol",
                                    fontSize = 17.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExpensesSearchBar(
    searchQuery: String,
    lang: Localization.Language,
    onSearchQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = Localization.searchExpensesPlaceholder(lang),
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = Emerald700,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("expenses_search_input")
        )
    }
}

@Composable
internal fun ExpensesFilterDropdowns(
    members: List<FamilyMember>,
    categories: List<ExpenseCategory>,
    selectedMemberFilter: Long?,
    selectedCategoryFilter: Long?,
    lang: Localization.Language,
    onMemberFilterChange: (Long?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit
) {
    var showMemberDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val selectedMemberName = remember(selectedMemberFilter, members, lang) {
        if (selectedMemberFilter == null) {
            Localization.allFilter(lang)
        } else {
            members.find { it.id == selectedMemberFilter }?.name ?: Localization.allFilter(lang)
        }
    }

    val selectedCategoryName = remember(selectedCategoryFilter, categories, lang) {
        if (selectedCategoryFilter == null) {
            Localization.allCategoriesFilter(lang)
        } else {
            categories.find { it.id == selectedCategoryFilter }?.let { Localization.translateCategory(it.name, lang) }
                ?: Localization.allCategoriesFilter(lang)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMemberDropdown = true }
                    .testTag("member_filter_dropdown")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = selectedMemberName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showMemberDropdown,
                onDismissRequest = { showMemberDropdown = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = Localization.allFilter(lang),
                            fontWeight = if (selectedMemberFilter == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedMemberFilter == null) Emerald700 else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onMemberFilterChange(null)
                        showMemberDropdown = false
                    }
                )
                members.forEach { m ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = m.name,
                                fontWeight = if (selectedMemberFilter == m.id) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedMemberFilter == m.id) Emerald700 else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onMemberFilterChange(m.id)
                            showMemberDropdown = false
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCategoryDropdown = true }
                    .testTag("category_filter_dropdown")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = selectedCategoryName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = Localization.allCategoriesFilter(lang),
                            fontWeight = if (selectedCategoryFilter == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedCategoryFilter == null) Emerald700 else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onCategoryFilterChange(null)
                        showCategoryDropdown = false
                    }
                )
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = Localization.translateCategory(cat.name, lang),
                                fontWeight = if (selectedCategoryFilter == cat.id) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCategoryFilter == cat.id) Emerald700 else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onCategoryFilterChange(cat.id)
                            showCategoryDropdown = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ActiveSpecialFilterBadge(
    activeSpecialFilter: AnalyticsSpecialFilter,
    lang: Localization.Language,
    onClearSpecialFilter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Emerald700.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.35f)),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClearSpecialFilter() }
                .testTag("active_drilldown_filter_chip")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = activeSpecialFilter.badgeLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald700
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = if (lang == Localization.Language.ENGLISH) "Clear filter" else "Καθαρισμός φίλτρου",
                    tint = Emerald700,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
internal fun MonthlySummaryExpandableCard(
    currentMonthDisplay: String,
    itemsCount: Int,
    displayTotalAmount: Double,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    val currencySymbol = LocalCurrencySymbol.current
    val chevronRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "monthly_chevron"
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onToggleExpanded() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFD1FAE5),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = currentMonthDisplay,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "$itemsCount",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.2f", displayTotalAmount)} $currencySymbol",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF047857)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }
        }
    }
}
