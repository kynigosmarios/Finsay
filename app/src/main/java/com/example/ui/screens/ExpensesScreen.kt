package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.getBeneficiaryCategoryOrNamesDisplay
import com.example.data.entity.getPayerCategoryOrNamesDisplay
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.Emerald700
import com.example.util.AnalyticsSpecialFilter
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun stripParen(text: String): String {
    val idx = text.indexOf('(')
    return if (idx >= 0) text.substring(0, idx).trim() else text.trim()
}

@Immutable
sealed class ActivityItem(val timestamp: Long, val key: String) {
    @Immutable
    data class ExpenseItem(
        val expense: Expense,
        val titleText: String,
        val categoryDisplay: String,
        val categoryColor: Color,
        val categoryIconKey: String,
        val formattedAmount: String,
        val formattedDate: String,
        val payerText: String,
        val isPayerGroup: Boolean,
        val beneficiaryText: String,
        val isBeneficiaryGroup: Boolean,
        val itemKey: String
    ) : ActivityItem(expense.timestamp, itemKey)

    @Immutable
    data class IncomeItem(
        val income: Income,
        val titleText: String,
        val categoryDisplay: String,
        val formattedAmount: String,
        val formattedDate: String,
        val memberName: String,
        val itemKey: String
    ) : ActivityItem(income.timestamp, itemKey)
}

@Immutable
data class DayGroup(
    val key: String,
    val label: String,
    val dayExpenseTotal: Double,
    val dayIncomeTotal: Double,
    val items: List<ActivityItem>
)

@Composable
fun ExpensesScreen(
    expenses: List<Expense>,
    incomes: List<Income>,
    allExpenses: List<Expense> = expenses,
    allIncomes: List<Income> = incomes,
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
    selectedMemberFilter: Long?,
    selectedCategoryFilter: Long?,
    searchQuery: String,
    selectedMonthKey: String? = null,
    selectedTypeFilter: String = "EXPENSE",
    activeSpecialFilter: AnalyticsSpecialFilter? = null,
    drillDownScrollTrigger: Int = 0,
    scrollToTopTrigger: Int = 0,
    appLanguage: String,
    onMemberFilterChange: (Long?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onMonthKeyChange: (String) -> Unit = {},
    onTypeFilterChange: (String) -> Unit = {},
    onClearSpecialFilter: () -> Unit = {},
    onAddManualExpense: () -> Unit,
    onAddManualIncome: () -> Unit,
    onVoiceExpense: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onEditIncome: (Income) -> Unit,
    onDeleteIncome: (Income) -> Unit,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    onShareReport: () -> Unit,
    onAddMember: () -> Unit,
    onAddCategory: () -> Unit,
    onSeedStandardCategories: () -> Unit,
    isInitialLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val locale = Localization.getDateLocale(lang)

    val monthYearFormat = remember(locale) { SimpleDateFormat("LLLL yyyy", locale) }
    val monthYearKeyFormat = remember { SimpleDateFormat("yyyy-MM", Locale.US) }
    val yearKeyFormat = remember { SimpleDateFormat("yyyy", Locale.US) }

    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var incomeToDelete by remember { mutableStateOf<Income?>(null) }

    // Dropdown toggles
    var showMonthDropdown by remember { mutableStateOf(false) }
    var showMemberDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var isMonthlyListExpanded by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    LaunchedEffect(drillDownScrollTrigger) {
        if (drillDownScrollTrigger > 0) {
            isMonthlyListExpanded = true
            listState.animateScrollToItem(1)
        }
    }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
        }
    }

    // Extract available months from unfiltered dataset so months never vanish during filtering
    val availableMonths = remember(allExpenses, allIncomes, locale) {
        val allTimestamps = (allExpenses.map { it.timestamp } + allIncomes.map { it.timestamp })
            .filter { it > 0 }
            .distinct()

        val list = allTimestamps.map { ts ->
            val key = monthYearKeyFormat.format(Date(ts))
            val rawName = monthYearFormat.format(Date(ts))
            val display = rawName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            Triple(key, display, ts)
        }
            .distinctBy { it.first }
            .sortedByDescending { it.third }

        if (list.isEmpty()) {
            val now = System.currentTimeMillis()
            val rawNow = monthYearFormat.format(Date(now))
            listOf(Triple(monthYearKeyFormat.format(Date(now)), rawNow.replaceFirstChar { it.titlecase(locale) }, now))
        } else list
    }

    val effectiveMonthKey = remember(selectedMonthKey, availableMonths) {
        when {
            selectedMonthKey == "ALL" -> "ALL"
            selectedMonthKey != null && selectedMonthKey.startsWith("YEAR:") -> selectedMonthKey
            selectedMonthKey != null && selectedMonthKey.isNotBlank() -> selectedMonthKey
            else -> availableMonths.firstOrNull()?.first ?: ""
        }
    }

    val currentMonthDisplay = remember(effectiveMonthKey, availableMonths, lang, locale) {
        when {
            effectiveMonthKey == "ALL" -> if (lang == Localization.Language.ENGLISH) "All Months" else "Όλοι οι μήνες"
            effectiveMonthKey.startsWith("YEAR:") -> {
                val y = effectiveMonthKey.removePrefix("YEAR:")
                if (lang == Localization.Language.ENGLISH) "Year $y" else "Έτος $y"
            }
            else -> {
                availableMonths.find { it.first == effectiveMonthKey }?.second ?: try {
                    val parsed = monthYearKeyFormat.parse(effectiveMonthKey)
                    if (parsed != null) {
                        val raw = monthYearFormat.format(parsed)
                        raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                    } else effectiveMonthKey
                } catch (_: Exception) {
                    effectiveMonthKey
                }
            }
        }
    }

    // Unfiltered period datasets so the green Hero Card ALWAYS reflects total period figures
    val heroMonthExpenses = remember(allExpenses, effectiveMonthKey) {
        when {
            effectiveMonthKey.isBlank() || effectiveMonthKey == "ALL" -> allExpenses
            effectiveMonthKey.startsWith("YEAR:") -> {
                val targetYear = effectiveMonthKey.removePrefix("YEAR:")
                allExpenses.filter { yearKeyFormat.format(Date(it.timestamp)) == targetYear }
            }
            else -> allExpenses.filter { monthYearKeyFormat.format(Date(it.timestamp)) == effectiveMonthKey }
        }
    }
    val heroMonthIncomes = remember(allIncomes, effectiveMonthKey) {
        when {
            effectiveMonthKey.isBlank() || effectiveMonthKey == "ALL" -> allIncomes
            effectiveMonthKey.startsWith("YEAR:") -> {
                val targetYear = effectiveMonthKey.removePrefix("YEAR:")
                allIncomes.filter { yearKeyFormat.format(Date(it.timestamp)) == targetYear }
            }
            else -> allIncomes.filter { monthYearKeyFormat.format(Date(it.timestamp)) == effectiveMonthKey }
        }
    }

    // Month-filtered datasets for the filtered transactions list below
    val monthExpenses = remember(expenses, effectiveMonthKey) {
        when {
            effectiveMonthKey.isBlank() || effectiveMonthKey == "ALL" -> expenses
            effectiveMonthKey.startsWith("YEAR:") -> {
                val targetYear = effectiveMonthKey.removePrefix("YEAR:")
                expenses.filter { yearKeyFormat.format(Date(it.timestamp)) == targetYear }
            }
            else -> expenses.filter { monthYearKeyFormat.format(Date(it.timestamp)) == effectiveMonthKey }
        }
    }
    val monthIncomes = remember(incomes, effectiveMonthKey) {
        when {
            effectiveMonthKey.isBlank() || effectiveMonthKey == "ALL" -> incomes
            effectiveMonthKey.startsWith("YEAR:") -> {
                val targetYear = effectiveMonthKey.removePrefix("YEAR:")
                incomes.filter { yearKeyFormat.format(Date(it.timestamp)) == targetYear }
            }
            else -> incomes.filter { monthYearKeyFormat.format(Date(it.timestamp)) == effectiveMonthKey }
        }
    }

    val totalExpensesAmount = remember(heroMonthExpenses) { heroMonthExpenses.sumOf { it.amount } }
    val totalIncomesAmount = remember(heroMonthIncomes) { heroMonthIncomes.sumOf { it.amount } }
    val filteredExpensesAmount = remember(monthExpenses) { monthExpenses.sumOf { it.amount } }
    val filteredIncomesAmount = remember(monthIncomes) { monthIncomes.sumOf { it.amount } }
    val netBalance = totalIncomesAmount - totalExpensesAmount

    // Dynamic budget calculation: exact sum of monthly budgets of categories.
    // When "ALL" months or "YEAR:" are selected, scale budget by active months count so total expenses are fairly compared
    val totalBudget = remember(categories, effectiveMonthKey, availableMonths) {
        val baseMonthlyBudget = categories.sumOf { it.monthlyBudget }
        when {
            effectiveMonthKey == "ALL" -> baseMonthlyBudget * availableMonths.size.coerceAtLeast(1)
            effectiveMonthKey.startsWith("YEAR:") -> baseMonthlyBudget * 12
            else -> baseMonthlyBudget
        }
    }
    val remainingBudget = remember(totalBudget, totalExpensesAmount) {
        totalBudget - totalExpensesAmount
    }
    val budgetPercent = remember(totalBudget, totalExpensesAmount) {
        if (totalBudget > 0.0) ((totalExpensesAmount / totalBudget) * 100).toInt() else 0
    }
    val isWithinBudget = totalExpensesAmount <= totalBudget
    val animatedBudgetProgress by animateFloatAsState(
        targetValue = (budgetPercent.coerceIn(0, 100) / 100f),
        label = "budget_progress"
    )

    // Shared family ("Κοινά Σπιτιού") vs Individual ("Ατομικά")
    val sharedExpenses = remember(heroMonthExpenses) {
        heroMonthExpenses.filter { it.splitType == "ALL" || it.splitType == "ALL_FAMILY" }.sumOf { it.amount }
    }
    val individualExpenses = remember(heroMonthExpenses) {
        heroMonthExpenses.filter { it.splitType != "ALL" && it.splitType != "ALL_FAMILY" }.sumOf { it.amount }
    }

    // Filtered transaction items to display in the list below with all formatting precomputed
    val displayItems = remember(monthExpenses, monthIncomes, selectedTypeFilter, categories, members, locale, lang) {
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy • HH:mm", locale)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", locale)
        val itemCal = Calendar.getInstance()

        val catById = categories.associateBy { it.id }
        val catByName = categories.associateBy { it.name.trim().lowercase() }

        fun createExpenseItem(exp: Expense): ActivityItem.ExpenseItem {
            val matchingCat = catById[exp.categoryId] ?: catByName[exp.categoryName.trim().lowercase()]
            val iconKey = matchingCat?.iconKey ?: "cart"
            val catColor = if (matchingCat != null && matchingCat.colorHex != 0L) Color(matchingCat.colorHex) else Color(0xFF059669)
            val catDisplay = Localization.translateCategory(exp.categoryName, lang)
            val title = exp.title.ifBlank { catDisplay }

            itemCal.timeInMillis = exp.timestamp
            val hasTime = itemCal.get(Calendar.HOUR_OF_DAY) != 0 || itemCal.get(Calendar.MINUTE) != 0
            val dateStr = if (hasTime) dateTimeFormat.format(Date(exp.timestamp)) else dateFormat.format(Date(exp.timestamp))

            val rawPayer = exp.getPayerCategoryOrNamesDisplay(members, lang)
            val cleanPayer = stripParen(rawPayer)
            val isPayerGroup = cleanPayer.contains("&") || cleanPayer.contains(",") ||
                    cleanPayer.contains("Γονείς") || cleanPayer.contains("Παιδιά") ||
                    cleanPayer.contains("Συγκάτοικοι") || cleanPayer.contains("Οικογένεια") ||
                    cleanPayer.contains("Parents") || cleanPayer.contains("Children") ||
                    cleanPayer.contains("Roommates") || cleanPayer.contains("Family")

            val rawBeneficiary = exp.getBeneficiaryCategoryOrNamesDisplay(members, lang)
            val cleanBeneficiary = stripParen(rawBeneficiary)
            val isBeneficiaryGroup = cleanBeneficiary.contains("Γονείς") || cleanBeneficiary.contains("Παιδιά") ||
                    cleanBeneficiary.contains("Συγκάτοικοι") || cleanBeneficiary.contains("Οικογένεια") ||
                    cleanBeneficiary.contains("Parents") || cleanBeneficiary.contains("Children") ||
                    cleanBeneficiary.contains("Roommates") || cleanBeneficiary.contains("Family") ||
                    cleanBeneficiary.contains("&") || cleanBeneficiary.contains(",")

            val key = if (exp.id > 0) "exp_${exp.id}" else "exp_c_${if (exp.cloudId.isNotBlank()) exp.cloudId else exp.timestamp.toString()}_${exp.title.hashCode()}"

            return ActivityItem.ExpenseItem(
                expense = exp,
                titleText = title,
                categoryDisplay = catDisplay,
                categoryColor = catColor,
                categoryIconKey = iconKey,
                formattedAmount = String.format(Locale.US, "%.2f €", exp.amount),
                formattedDate = dateStr,
                payerText = cleanPayer,
                isPayerGroup = isPayerGroup,
                beneficiaryText = cleanBeneficiary,
                isBeneficiaryGroup = isBeneficiaryGroup,
                itemKey = key
            )
        }

        fun createIncomeItem(inc: Income): ActivityItem.IncomeItem {
            itemCal.timeInMillis = inc.timestamp
            val hasTime = itemCal.get(Calendar.HOUR_OF_DAY) != 0 || itemCal.get(Calendar.MINUTE) != 0
            val dateStr = if (hasTime) dateTimeFormat.format(Date(inc.timestamp)) else dateFormat.format(Date(inc.timestamp))
            val key = if (inc.id > 0) "inc_${inc.id}" else "inc_c_${if (inc.cloudId.isNotBlank()) inc.cloudId else inc.timestamp.toString()}_${inc.title.hashCode()}"

            return ActivityItem.IncomeItem(
                income = inc,
                titleText = inc.title,
                categoryDisplay = inc.category.ifBlank { Localization.income(lang) },
                formattedAmount = String.format(Locale.US, "+%.2f €", inc.amount),
                formattedDate = dateStr,
                memberName = inc.memberName,
                itemKey = key
            )
        }

        when (selectedTypeFilter) {
            "EXPENSE" -> monthExpenses.map { createExpenseItem(it) }
            "INCOME" -> monthIncomes.map { createIncomeItem(it) }
            else -> {
                val exps = monthExpenses.map { createExpenseItem(it) }
                val incs = monthIncomes.map { createIncomeItem(it) }
                (exps + incs).sortedByDescending { it.timestamp }
            }
        }
    }

    // Date grouping: Group items into "Σήμερα", "Χθες", or genuine calendar date (e.g. "Τετάρτη, 14 Οκτωβρίου 2026")
    val groupedItems = remember(displayItems, locale, lang) {
        val dayFormat = SimpleDateFormat("EEEE, d MMMM yyyy", locale)
        val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

        val calToday = Calendar.getInstance()
        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val itemCal = Calendar.getInstance()

        val rawGroups = displayItems.groupBy { item ->
            itemCal.timeInMillis = item.timestamp

            val isToday = itemCal.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
                    itemCal.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR)

            val isYesterday = itemCal.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
                    itemCal.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)

            val label = when {
                isToday -> if (lang == Localization.Language.ENGLISH) "Today" else "Σήμερα"
                isYesterday -> if (lang == Localization.Language.ENGLISH) "Yesterday" else "Χθες"
                else -> {
                    val raw = dayFormat.format(Date(item.timestamp))
                    raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                }
            }
            val key = dayKeyFormat.format(Date(item.timestamp))
            Pair(key, label)
        }

        rawGroups.entries.sortedByDescending { it.key.first }.map { entry ->
            val itemsInDay = entry.value
            val dayExpenseTotal = itemsInDay.filterIsInstance<ActivityItem.ExpenseItem>().sumOf { it.expense.amount }
            val dayIncomeTotal = itemsInDay.filterIsInstance<ActivityItem.IncomeItem>().sumOf { it.income.amount }
            DayGroup(
                key = entry.key.first,
                label = entry.key.second,
                dayExpenseTotal = dayExpenseTotal,
                dayIncomeTotal = dayIncomeTotal,
                items = itemsInDay
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. HERO FOREST GREEN CARD (Glossy glass reflection matching logo)
            item(key = "hero_dashboard_card") {
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
                            // Top Row: Month Dropdown Chip + '+ Έξοδο' Pill Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Month Selector Chip
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

                                // Dynamic 'Έξοδο' / 'Έσοδο' Pill Button (Same color for both)
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

                            // Net Balance Header
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "NET BALANCE" else "ΚΑΘΑΡΟ ΥΠΟΛΟΙΠΟ",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0),
                                letterSpacing = 0.8.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Big Amount
                            Text(
                                text = String.format(Locale.US, "%s%.2f €", if (netBalance >= 0) "+" else "", netBalance),
                                fontSize = 35.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 'Εντός προϋπολογισμού' Status Badge
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
                                        tint = Color(0xFF6EE7B7),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isWithinBudget)
                                            (if (lang == Localization.Language.ENGLISH) "Within budget" else "Εντός προϋπολογισμού")
                                        else
                                            (if (lang == Localization.Language.ENGLISH) "Over budget" else "Εκτός προϋπολογισμού"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF6EE7B7)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Middle Row: Incomes & Expenses Cards side by side
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Left Card: Έσοδα
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onTypeFilterChange("INCOME")
                                        },
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
                                                text = String.format(Locale.US, "%.2f €", totalIncomesAmount),
                                                fontSize = 17.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Right Card: Έξοδα
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onTypeFilterChange("EXPENSE")
                                        },
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
                                                text = String.format(Locale.US, "%.2f €", totalExpensesAmount),
                                                fontSize = 17.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Segmented Switcher Tab: 💸 Έξοδα (71) | 💰 Έσοδα (1)
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
                                    // Tab 1: Έξοδα
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onTypeFilterChange("EXPENSE") },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selectedTypeFilter == "EXPENSE") Color(0xFF10B981) else Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 9.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "💸 ", fontSize = 14.sp)
                                            Text(
                                                text = "${Localization.expenses(lang)} (${heroMonthExpenses.size})",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedTypeFilter == "EXPENSE") Color.White else Color(0xFFA7F3D0)
                                            )
                                        }
                                    }

                                    // Tab 2: Έσοδα
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onTypeFilterChange("INCOME") },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selectedTypeFilter == "INCOME") Color(0xFF10B981) else Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 9.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "💰 ", fontSize = 14.sp)
                                            Text(
                                                text = "${Localization.income(lang)} (${heroMonthIncomes.size})",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedTypeFilter == "INCOME") Color.White else Color(0xFFA7F3D0)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Budget Progress Section: Actual Expenses vs Category Budget Sum
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        if (lang == Localization.Language.ENGLISH) "Expenses: %.2f € / Budget: %.2f €" else "Έξοδα: %.2f € / Budget: %.2f €",
                                        totalExpensesAmount,
                                        totalBudget
                                    ),
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
                                            "$budgetPercent% (${String.format(Locale.US, "%s%.2f €", if (remaining >= 0) "+" else "", remaining)})"
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

                            // Budget Progress Bar
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

                            // Bottom Row: Κοινά Σπιτιού & Ατομικά Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Left Card: Κοινά Σπιτιού
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
                                            text = String.format(Locale.US, "%.2f €", sharedExpenses),
                                            fontSize = 17.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Right Card: Ατομικά
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
                                            text = String.format(Locale.US, "%.2f €", individualExpenses),
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

            // 2. SEARCH BAR (Matches Screenshot 1 & 2)
            item(key = "search_bar_item") {
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

            // 3. FILTER DROPDOWN BUTTONS (Matches Screenshot 1 & 2: '👤 Όλα ▾' and '🔺 Όλες οι Κατηγορίες ▾')
            item(key = "filter_dropdowns_item") {
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
                        categories.find { it.id == selectedCategoryFilter }?.let { Localization.translateCategory(it.name, lang) } ?: Localization.allCategoriesFilter(lang)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Member Filter Dropdown
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

                    // Category Filter Dropdown
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

            if (activeSpecialFilter != null && activeSpecialFilter.badgeLabel.isNotBlank()) {
                item(key = "active_special_filter_chip") {
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
            }

            // 4. MONTHLY SUMMARY EXPANDABLE CARD (Matches Screenshot 1 & 2: '📅 Οκτώβριος 2026 | 71 συναλλαγές | 2062.66 € ⌃')
            item(key = "monthly_summary_card") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { isMonthlyListExpanded = !isMonthlyListExpanded }
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
                            // Mint box with Calendar Icon
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFD1FAE5),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = currentMonthDisplay,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${displayItems.size} συναλλαγές",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2f €", if (selectedTypeFilter == "INCOME") filteredIncomesAmount else filteredExpensesAmount),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                            Icon(
                                imageVector = if (isMonthlyListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isMonthlyListExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 5. TRANSACTIONS LIST (Grouped by Day: Σήμερα, Χθες, etc.)
            if (isInitialLoading) {
                item(key = "loading_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Emerald700)
                    }
                }
            } else if (displayItems.isEmpty()) {
                val isIncomeMode = selectedTypeFilter == "INCOME"
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = Emerald700.copy(alpha = 0.12f),
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isIncomeMode) Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isIncomeMode) {
                                    if (lang == Localization.Language.ENGLISH) "No incomes found" else "Δεν βρέθηκαν έσοδα"
                                } else {
                                    Localization.noExpensesFound(lang)
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isIncomeMode) {
                                    if (lang == Localization.Language.ENGLISH) "No incomes recorded for this month" else "Δεν υπάρχουν καταχωρημένα έσοδα για αυτόν τον μήνα"
                                } else {
                                    Localization.noExpensesForPeriod(lang)
                                },
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF047857),
                                modifier = Modifier.clickable {
                                    if (isIncomeMode) onAddManualIncome() else onAddManualExpense()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isIncomeMode) {
                                            if (lang == Localization.Language.ENGLISH) "Add Income" else "Καταχώρηση Εσόδου"
                                        } else {
                                            if (lang == Localization.Language.ENGLISH) "Add Expense" else "Καταχώρηση Εξόδου"
                                        },
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (isMonthlyListExpanded) {
                groupedItems.forEach { group ->
                    // Day Header Row (Σήμερα - 28.00 €, Χθες - 559.20 €)
                    item(key = "header_${group.key}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = group.label,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )

                            val displayDayTotal = if (selectedTypeFilter == "INCOME") group.dayIncomeTotal else group.dayExpenseTotal
                            if (displayDayTotal > 0.0) {
                                Text(
                                    text = String.format(Locale.US, "%.2f €", displayDayTotal),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Individual Cards for this Day
                    items(
                        items = group.items,
                        key = { it.key }
                    ) { item ->
                        when (item) {
                            is ActivityItem.ExpenseItem -> {
                                ExpenseCardItem(
                                    item = item,
                                    onEditExpense = onEditExpense,
                                    onDeleteExpense = { expenseToDelete = it }
                                )
                            }
                            is ActivityItem.IncomeItem -> {
                                IncomeCardItem(
                                    item = item,
                                    onEditIncome = onEditIncome,
                                    onDeleteIncome = { incomeToDelete = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Expense Confirmation Dialog
    if (expenseToDelete != null) {
        val exp = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text(Localization.delete(lang)) },
            text = { Text("Είστε βέβαιοι ότι θέλετε να διαγράψετε το έξοδο «${exp.title}» (${String.format(Locale.US, "%.2f €", exp.amount)});") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExpense(exp)
                        expenseToDelete = null
                    }
                ) {
                    Text(Localization.delete(lang), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text(Localization.cancel(lang))
                }
            }
        )
    }

    // Delete Income Confirmation Dialog
    if (incomeToDelete != null) {
        val inc = incomeToDelete!!
        AlertDialog(
            onDismissRequest = { incomeToDelete = null },
            title = { Text(Localization.delete(lang)) },
            text = { Text("Είστε βέβαιοι ότι θέλετε να διαγράψετε το έσοδο «${inc.title}» (${String.format(Locale.US, "%.2f €", inc.amount)});") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteIncome(inc)
                        incomeToDelete = null
                    }
                ) {
                    Text(Localization.delete(lang), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { incomeToDelete = null }) {
                    Text(Localization.cancel(lang))
                }
            }
        )
    }
}

@Composable
private fun ExpenseCardItem(
    item: ActivityItem.ExpenseItem,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    val expense = item.expense
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clickable { onEditExpense(expense) }
            .testTag("expense_card_${expense.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Icon + Title + Category + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon in tinted rounded box matching custom category color
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = item.categoryColor.copy(alpha = 0.16f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = CategoryIconHelper.getIcon(item.categoryIconKey),
                            contentDescription = null,
                            tint = item.categoryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.titleText,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.categoryDisplay,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = item.categoryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = " • ${item.formattedDate}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = item.formattedAmount,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Row: Member Chips + Actions (Pencil & Trash)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Chip 1 (Payer): Mint background
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE6F4EA),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isPayerGroup) Icons.Default.Group else Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = item.payerText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Chip 2 (Target / Split): Soft Blue background
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isBeneficiaryGroup) Icons.Default.Group else Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = item.beneficiaryText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D4ED8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Action Buttons: Edit & Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onEditExpense(expense) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = { onDeleteExpense(expense) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeCardItem(
    item: ActivityItem.IncomeItem,
    onEditIncome: (Income) -> Unit,
    onDeleteIncome: (Income) -> Unit
) {
    val income = item.income
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clickable { onEditIncome(income) }
            .testTag("income_card_${income.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD1FAE5),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.titleText,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.categoryDisplay,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF059669),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = " • ${item.formattedDate}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = item.formattedAmount,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (item.memberName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE6F4EA),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = item.memberName,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onEditIncome(income) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = { onDeleteIncome(income) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
