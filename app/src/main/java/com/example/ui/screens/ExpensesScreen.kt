package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.getBeneficiaryCategoryOrNamesDisplay
import com.example.data.entity.getPayerCategoryOrNamesDisplay
import com.example.ui.theme.Emerald700
import com.example.util.AnalyticsSpecialFilter
import com.example.util.LocalCurrencySymbol
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
    onDuplicateExpense: (Expense) -> Unit = {},
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
    val currencySymbol = LocalCurrencySymbol.current

    val monthYearFormat = remember(locale) { SimpleDateFormat("LLLL yyyy", locale) }
    val monthYearKeyFormat = remember { SimpleDateFormat("yyyy-MM", Locale.US) }
    val yearKeyFormat = remember { SimpleDateFormat("yyyy", Locale.US) }

    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var incomeToDelete by remember { mutableStateOf<Income?>(null) }
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
            effectiveMonthKey == "ALL" -> Localization.tr(lang, "All Months", "Όλοι οι μήνες")
            effectiveMonthKey.startsWith("YEAR:") -> {
                val y = effectiveMonthKey.removePrefix("YEAR:")
                Localization.trFormat(lang, "Year %1\$s", "Έτος %1\$s", y)
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

    val heroMonthExpenses = remember(allExpenses, effectiveMonthKey) {
        when {
            effectiveMonthKey.isBlank() || effectiveMonthKey == "ALL" -> allExpenses
            effectiveMonthKey.startsWith("YEAR:") -> {
                val targetYear = effectiveMonthKey.removePrefix("YEAR:")
                allExpenses.filter { it.timestamp > 0L && yearKeyFormat.format(Date(it.timestamp)) == targetYear }
            }
            else -> allExpenses.filter { it.timestamp > 0L && monthYearKeyFormat.format(Date(it.timestamp)) == effectiveMonthKey }
        }
    }
    val heroMonthIncomes = remember(allIncomes, effectiveMonthKey) {
        when {
            effectiveMonthKey.isBlank() || effectiveMonthKey == "ALL" -> allIncomes
            effectiveMonthKey.startsWith("YEAR:") -> {
                val targetYear = effectiveMonthKey.removePrefix("YEAR:")
                allIncomes.filter { it.timestamp > 0L && yearKeyFormat.format(Date(it.timestamp)) == targetYear }
            }
            else -> allIncomes.filter { it.timestamp > 0L && monthYearKeyFormat.format(Date(it.timestamp)) == effectiveMonthKey }
        }
    }

    val monthExpenses = remember(expenses, effectiveMonthKey) {
        when {
            effectiveMonthKey.isBlank() || effectiveMonthKey == "ALL" -> expenses
            effectiveMonthKey.startsWith("YEAR:") -> {
                val targetYear = effectiveMonthKey.removePrefix("YEAR:")
                expenses.filter { it.timestamp > 0L && yearKeyFormat.format(Date(it.timestamp)) == targetYear }
            }
            else -> expenses.filter { it.timestamp > 0L && monthYearKeyFormat.format(Date(it.timestamp)) == effectiveMonthKey }
        }
    }
    val monthIncomes = remember(incomes, effectiveMonthKey) {
        when {
            effectiveMonthKey.isBlank() || effectiveMonthKey == "ALL" -> incomes
            effectiveMonthKey.startsWith("YEAR:") -> {
                val targetYear = effectiveMonthKey.removePrefix("YEAR:")
                incomes.filter { it.timestamp > 0L && yearKeyFormat.format(Date(it.timestamp)) == targetYear }
            }
            else -> incomes.filter { it.timestamp > 0L && monthYearKeyFormat.format(Date(it.timestamp)) == effectiveMonthKey }
        }
    }

    val totalExpensesAmount = remember(heroMonthExpenses) { heroMonthExpenses.sumOf { it.amount } }
    val totalIncomesAmount = remember(heroMonthIncomes) { heroMonthIncomes.sumOf { it.amount } }
    val filteredExpensesAmount = remember(monthExpenses) { monthExpenses.sumOf { it.amount } }
    val filteredIncomesAmount = remember(monthIncomes) { monthIncomes.sumOf { it.amount } }
    val netBalance = totalIncomesAmount - totalExpensesAmount

    val totalBudget = remember(categories, effectiveMonthKey, availableMonths) {
        val baseMonthlyBudget = categories.sumOf { it.monthlyBudget }
        when {
            effectiveMonthKey == "ALL" -> baseMonthlyBudget * availableMonths.size.coerceAtLeast(1)
            effectiveMonthKey.startsWith("YEAR:") -> baseMonthlyBudget * 12
            else -> baseMonthlyBudget
        }
    }
    val budgetPercent = remember(totalBudget, totalExpensesAmount) {
        if (totalBudget > 0.0) ((totalExpensesAmount / totalBudget) * 100).toInt() else 0
    }
    val isWithinBudget = totalExpensesAmount <= totalBudget
    val animatedBudgetProgress by animateFloatAsState(
        targetValue = (budgetPercent.coerceIn(0, 100) / 100f),
        animationSpec = com.example.ui.components.FinsayMotion.ProgressSpec,
        label = "budget_progress"
    )

    val sharedExpenses = remember(heroMonthExpenses) {
        heroMonthExpenses.filter { it.splitType == "ALL" || it.splitType == "ALL_FAMILY" }.sumOf { it.amount }
    }
    val individualExpenses = remember(heroMonthExpenses) {
        heroMonthExpenses.filter { it.splitType != "ALL" && it.splitType != "ALL_FAMILY" }.sumOf { it.amount }
    }

    val displayItems = remember(monthExpenses, monthIncomes, selectedTypeFilter, categories, members, locale, lang, currencySymbol) {
        val timeOnlyFormat = SimpleDateFormat("HH:mm", locale)
        val itemCal = Calendar.getInstance()

        val catById = categories.associateBy { it.id }
        val catByName = categories.associateBy { it.name.trim().lowercase() }

        val usedKeys = HashSet<String>((monthExpenses.size + monthIncomes.size) * 2 + 8)
        fun uniqueKey(base: String): String {
            if (usedKeys.add(base)) return base
            var suffix = 1
            while (!usedKeys.add("${base}_#$suffix")) {
                suffix++
            }
            return "${base}_#$suffix"
        }

        fun createExpenseItem(exp: Expense): ActivityItem.ExpenseItem {
            val matchingCat = catById[exp.categoryId] ?: catByName[exp.categoryName.trim().lowercase()]
            val iconKey = matchingCat?.iconKey ?: "cart"
            val catColor = if (matchingCat != null && matchingCat.colorHex != 0L) Color(matchingCat.colorHex) else Color(0xFF059669)
            val catDisplay = Localization.translateCategory(exp.categoryName, lang)
            val title = exp.title.ifBlank { catDisplay }

            val safeTs = if (exp.timestamp > 0L) exp.timestamp else System.currentTimeMillis()
            itemCal.timeInMillis = safeTs
            val hasTime = itemCal.get(Calendar.HOUR_OF_DAY) != 0 || itemCal.get(Calendar.MINUTE) != 0
            val dateStr = if (hasTime) timeOnlyFormat.format(Date(safeTs)) else ""

            val rawPayer = exp.getPayerCategoryOrNamesDisplay(members, lang)
            val cleanPayer = stripParen(rawPayer)
            val isPayerGroup = cleanPayer.contains("&") || cleanPayer.contains(",") ||
                Localization.isPluralRoleDisplay(cleanPayer) ||
                Localization.isAllMembersLabel(cleanPayer)

            val rawBeneficiary = exp.getBeneficiaryCategoryOrNamesDisplay(members, lang)
            val cleanBeneficiary = stripParen(rawBeneficiary)
            val isBeneficiaryGroup = Localization.isPluralRoleDisplay(cleanBeneficiary) ||
                Localization.isAllMembersLabel(cleanBeneficiary) ||
                cleanBeneficiary.contains("&") || cleanBeneficiary.contains(",")

            val baseKey = if (exp.id > 0) "exp_${exp.id}" else "exp_c_${if (exp.cloudId.isNotBlank()) exp.cloudId else safeTs.toString()}_${exp.title.hashCode()}"
            val key = uniqueKey(baseKey)

            return ActivityItem.ExpenseItem(
                expense = exp,
                titleText = title,
                categoryDisplay = catDisplay,
                categoryColor = catColor,
                categoryIconKey = iconKey,
                formattedAmount = String.format(Locale.US, "%.2f %s", exp.amount, currencySymbol),
                formattedDate = dateStr,
                payerText = cleanPayer,
                isPayerGroup = isPayerGroup,
                beneficiaryText = cleanBeneficiary,
                isBeneficiaryGroup = isBeneficiaryGroup,
                itemKey = key
            )
        }

        fun createIncomeItem(inc: Income): ActivityItem.IncomeItem {
            val safeTs = if (inc.timestamp > 0L) inc.timestamp else System.currentTimeMillis()
            itemCal.timeInMillis = safeTs
            val hasTime = itemCal.get(Calendar.HOUR_OF_DAY) != 0 || itemCal.get(Calendar.MINUTE) != 0
            val dateStr = if (hasTime) timeOnlyFormat.format(Date(safeTs)) else ""
            val baseKey = if (inc.id > 0) "inc_${inc.id}" else "inc_c_${if (inc.cloudId.isNotBlank()) inc.cloudId else safeTs.toString()}_${inc.title.hashCode()}"
            val key = uniqueKey(baseKey)
            val rawIncomeMember = inc.memberName.trim()
            val displayIncomeMember = when {
                Localization.isAllMembersLabel(rawIncomeMember) ||
                    (rawIncomeMember.isBlank() && inc.memberId == 0L) ->
                    Localization.allMembersWord(lang)
                else -> rawIncomeMember
            }

            return ActivityItem.IncomeItem(
                income = inc,
                titleText = inc.title,
                categoryDisplay = if (inc.category.isNotBlank()) Localization.translateCategory(inc.category, lang) else Localization.income(lang),
                formattedAmount = String.format(Locale.US, "+%.2f %s", inc.amount, currencySymbol),
                formattedDate = dateStr,
                memberName = displayIncomeMember,
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

    val groupedItems = remember(displayItems, locale, lang) {
        val dayFormat = SimpleDateFormat("EEEE, d MMMM yyyy", locale)
        val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

        val calToday = Calendar.getInstance()
        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val itemCal = Calendar.getInstance()

        val rawGroups = displayItems.groupBy { item ->
            val safeTs = if (item.timestamp > 0L) item.timestamp else System.currentTimeMillis()
            itemCal.timeInMillis = safeTs

            val isToday = itemCal.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
                itemCal.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR)

            val isYesterday = itemCal.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
                itemCal.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)

            val label = when {
                isToday -> Localization.tr(lang, "Today", "Σήμερα")
                isYesterday -> Localization.tr(lang, "Yesterday", "Χθες")
                else -> {
                    val raw = dayFormat.format(Date(safeTs))
                    raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                }
            }
            val key = dayKeyFormat.format(Date(safeTs))
            Pair(key, label)
        }

        val usedGroupKeys = HashSet<String>(rawGroups.size * 2 + 4)
        rawGroups.entries
            .sortedByDescending { it.key.first }
            .groupBy { it.key.first }
            .map { (dayKey, entriesForDay) ->
                val itemsInDay = entriesForDay.flatMap { it.value }
                val label = entriesForDay.first().key.second
                val dayExpenseTotal = itemsInDay.filterIsInstance<ActivityItem.ExpenseItem>().sumOf { it.expense.amount }
                val dayIncomeTotal = itemsInDay.filterIsInstance<ActivityItem.IncomeItem>().sumOf { it.income.amount }
                val uniqueGroupKey = if (usedGroupKeys.add(dayKey)) dayKey else "${dayKey}_${usedGroupKeys.size}"
                DayGroup(
                    key = uniqueGroupKey,
                    label = label,
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
            item(key = "hero_dashboard_card", contentType = "hero") {
                ExpensesHeroDashboardCard(
                    currentMonthDisplay = currentMonthDisplay,
                    availableMonths = availableMonths,
                    effectiveMonthKey = effectiveMonthKey,
                    selectedTypeFilter = selectedTypeFilter,
                    netBalance = netBalance,
                    isWithinBudget = isWithinBudget,
                    totalIncomesAmount = totalIncomesAmount,
                    totalExpensesAmount = totalExpensesAmount,
                    heroExpensesCount = heroMonthExpenses.size,
                    heroIncomesCount = heroMonthIncomes.size,
                    totalBudget = totalBudget,
                    budgetPercent = budgetPercent,
                    animatedBudgetProgress = animatedBudgetProgress,
                    sharedExpenses = sharedExpenses,
                    individualExpenses = individualExpenses,
                    lang = lang,
                    onMonthKeyChange = onMonthKeyChange,
                    onAddManualExpense = onAddManualExpense,
                    onAddManualIncome = onAddManualIncome,
                    onTypeFilterChange = onTypeFilterChange
                )
            }

            item(key = "search_bar_item", contentType = "search") {
                ExpensesSearchBar(
                    searchQuery = searchQuery,
                    lang = lang,
                    onSearchQueryChange = onSearchQueryChange
                )
            }

            item(key = "filter_dropdowns_item", contentType = "filters") {
                ExpensesFilterDropdowns(
                    members = members,
                    categories = categories,
                    selectedMemberFilter = selectedMemberFilter,
                    selectedCategoryFilter = selectedCategoryFilter,
                    lang = lang,
                    onMemberFilterChange = onMemberFilterChange,
                    onCategoryFilterChange = onCategoryFilterChange
                )
            }

            if (activeSpecialFilter != null && activeSpecialFilter.badgeLabel.isNotBlank()) {
                item(key = "active_special_filter_chip", contentType = "special_filter") {
                    ActiveSpecialFilterBadge(
                        activeSpecialFilter = activeSpecialFilter,
                        lang = lang,
                        onClearSpecialFilter = onClearSpecialFilter
                    )
                }
            }

            item(key = "monthly_summary_card", contentType = "monthly_summary") {
                MonthlySummaryExpandableCard(
                    currentMonthDisplay = currentMonthDisplay,
                    itemsCount = displayItems.size,
                    displayTotalAmount = if (selectedTypeFilter == "INCOME") filteredIncomesAmount else filteredExpensesAmount,
                    isExpanded = isMonthlyListExpanded,
                    onToggleExpanded = { isMonthlyListExpanded = !isMonthlyListExpanded }
                )
            }

            if (isInitialLoading) {
                item(key = "loading_state", contentType = "loading") {
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
                item(key = "empty_state", contentType = "empty") {
                    ExpensesEmptyStateCard(
                        isIncomeMode = selectedTypeFilter == "INCOME",
                        lang = lang,
                        onAddManualIncome = onAddManualIncome,
                        onAddManualExpense = onAddManualExpense
                    )
                }
            } else if (isMonthlyListExpanded) {
                groupedItems.forEach { group ->
                    item(key = "header_${group.key}", contentType = "day_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
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
                                    text = String.format(Locale.US, "%.2f %s", displayDayTotal, currencySymbol),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    items(
                        items = group.items,
                        key = { it.key },
                        contentType = { if (it is ActivityItem.ExpenseItem) "expense_row" else "income_row" }
                    ) { item ->
                        when (item) {
                            is ActivityItem.ExpenseItem -> {
                                ExpenseCardItem(
                                    item = item,
                                    onEditExpense = onEditExpense,
                                    onDeleteExpense = { expenseToDelete = it },
                                    onSwipeDeleteExpense = { onDeleteExpense(it) },
                                    onDuplicateExpense = onDuplicateExpense,
                                    lang = lang,
                                    modifier = Modifier.animateItem()
                                )
                            }
                            is ActivityItem.IncomeItem -> {
                                IncomeCardItem(
                                    item = item,
                                    onEditIncome = onEditIncome,
                                    onDeleteIncome = { incomeToDelete = it },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (expenseToDelete != null) {
        val exp = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text(Localization.delete(lang)) },
            text = {
                Text(
                    Localization.trFormat(
                        lang,
                        "Delete \"%1\$s\" (%2\$s)?",
                        "Διαγραφή «%1\$s» (%2\$s);",
                        exp.title,
                        String.format(Locale.US, "%.2f %s", exp.amount, currencySymbol)
                    )
                )
            },
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

    if (incomeToDelete != null) {
        val inc = incomeToDelete!!
        AlertDialog(
            onDismissRequest = { incomeToDelete = null },
            title = { Text(Localization.delete(lang)) },
            text = {
                Text(
                    Localization.trFormat(
                        lang,
                        "Delete \"%1\$s\" (%2\$s)?",
                        "Διαγραφή «%1\$s» (%2\$s);",
                        inc.title,
                        "${String.format(Locale.US, "%.2f", inc.amount)} $currencySymbol"
                    )
                )
            },
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
