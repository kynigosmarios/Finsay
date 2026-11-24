package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import com.example.data.entity.getPayerShares
import com.example.ui.components.BudgetAlertsCard
import com.example.util.AnalyticsExpenseFilterHelper
import com.example.util.AnalyticsSpecialFilter
import com.example.util.GreekVoiceParser
import com.example.util.HomeDrillDownRequest
import com.example.util.Localization
import com.example.util.SavingsGoalExpenseLinkHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class SpendCalculationResult(
    val individualSpendMap: Map<FamilyMember, Double>,
    val commonHouseholdSpend: Double,
    val directParentsSpend: Double,
    val kidsSpend: Double,
    val payerSpendMap: Map<FamilyMember, Double>,
    val memberSpendMap: Map<FamilyMember, Double>
)

@Composable
fun AnalyticsScreen(
    expenses: List<Expense>,
    incomes: List<Income> = emptyList(),
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
    recurringExpenses: List<RecurringExpense> = emptyList(),
    savingsGoals: List<SavingsGoal> = emptyList(),
    appLanguage: String = "EN",
    isPro: Boolean = false,
    canManageStructure: Boolean = true,
    onShowPremiumPaywall: (String) -> Unit = {},
    onExportReport: () -> Unit = {},
    onManageCategories: () -> Unit = {},
    onOpenSavingsGoals: () -> Unit = {},
    onDrillDownToHome: (HomeDrillDownRequest) -> Unit = {},
    onSettleTransfer: (FamilyMember, FamilyMember, Double, Long) -> Unit = { _, _, _, _ -> },
    initialFilterScope: String = "MONTH",
    initialPeriodKey: String = "CURRENT_MONTH",
    initialYearKey: String? = null,
    initialSplitMode: Int = 0,
    onAnalyticsStateChange: (filterScope: String, periodKey: String, yearKey: String, splitMode: Int) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val isGreek = lang != Localization.Language.ENGLISH
    val context = LocalContext.current
    var splitMode by remember { mutableIntStateOf(initialSplitMode) }
    var chartTypeFamily by remember { mutableIntStateOf(0) }
    var chartTypeMembers by remember { mutableIntStateOf(0) }
    var chartTypeIncome by remember { mutableIntStateOf(0) }

    var filterScope by remember { mutableStateOf(initialFilterScope) }

    val currentMonthKey = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    val currentMonthLabel = remember(lang) {
        val cal = Calendar.getInstance()
        SimpleDateFormat("LLLL yyyy", Localization.getDateLocale(lang)).format(cal.time).replaceFirstChar { it.uppercase() }
    }

    val availableMonths = remember(expenses, incomes) {
        val set = linkedSetOf<String>()
        set.add(currentMonthKey)
        val fmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        expenses.forEach { exp -> set.add(fmt.format(Date(exp.timestamp))) }
        incomes.forEach { inc -> set.add(fmt.format(Date(inc.timestamp))) }
        set.toList().sortedDescending()
    }

    val currentYearKey = remember { SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()) }
    val availableYears = remember(expenses, incomes) {
        val set = linkedSetOf<String>()
        set.add(currentYearKey)
        val fmt = SimpleDateFormat("yyyy", Locale.getDefault())
        expenses.forEach { exp -> set.add(fmt.format(Date(exp.timestamp))) }
        incomes.forEach { inc -> set.add(fmt.format(Date(inc.timestamp))) }
        set.toList().sortedDescending()
    }

    var selectedPeriodKey by remember { mutableStateOf(initialPeriodKey) }
    var selectedYearKey by remember { mutableStateOf(initialYearKey ?: currentYearKey) }

    val activeDrillDownPeriodKey = when (filterScope) {
        "ALL" -> "ALL"
        "YEAR" -> "YEAR:$selectedYearKey"
        else -> if (selectedPeriodKey == "CURRENT_MONTH") currentMonthKey else selectedPeriodKey
    }

    LaunchedEffect(filterScope, selectedPeriodKey, selectedYearKey, splitMode) {
        onAnalyticsStateChange(filterScope, selectedPeriodKey, selectedYearKey, splitMode)
    }

    fun getMonthLabel(key: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(key) ?: Date()
            SimpleDateFormat("LLLL yyyy", Localization.getDateLocale(lang)).format(date).replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            key
        }
    }

    val activePeriodDisplay = when (filterScope) {
        "ALL" -> Localization.allMonthsCombined(lang)
        "YEAR" -> if (lang == Localization.Language.ENGLISH) "Year $selectedYearKey" else "Έτος $selectedYearKey"
        else -> if (selectedPeriodKey == "CURRENT_MONTH" || selectedPeriodKey == currentMonthKey) currentMonthLabel else getMonthLabel(selectedPeriodKey)
    }

    val selectedMonthYearPair = remember(selectedPeriodKey, currentMonthKey) {
        val effKey = if (selectedPeriodKey == "CURRENT_MONTH") currentMonthKey else selectedPeriodKey
        val parts = effKey.split("-")
        val nowCal = Calendar.getInstance()
        val year = parts.getOrNull(0)?.toIntOrNull() ?: nowCal.get(Calendar.YEAR)
        val monthOneBased = parts.getOrNull(1)?.toIntOrNull() ?: (nowCal.get(Calendar.MONTH) + 1)
        Pair(year, (monthOneBased - 1).coerceIn(0, 11))
    }

    val settlePeriodTimestamp = remember(filterScope, selectedPeriodKey, selectedYearKey, currentMonthKey, currentYearKey) {
        val now = System.currentTimeMillis()
        when (filterScope) {
            "MONTH" -> {
                val effKey = if (selectedPeriodKey == "CURRENT_MONTH") currentMonthKey else selectedPeriodKey
                if (effKey == currentMonthKey) now
                else {
                    try {
                        val cal = Calendar.getInstance()
                        cal.time = SimpleDateFormat("yyyy-MM", Locale.US).parse(effKey) ?: Date(now)
                        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                        cal.set(Calendar.HOUR_OF_DAY, 12)
                        cal.set(Calendar.MINUTE, 0)
                        cal.timeInMillis
                    } catch (_: Exception) {
                        now
                    }
                }
            }
            "YEAR" -> {
                if (selectedYearKey == currentYearKey) now
                else {
                    val y = selectedYearKey.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, y)
                        set(Calendar.MONTH, Calendar.DECEMBER)
                        set(Calendar.DAY_OF_MONTH, 31)
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                    }.timeInMillis
                }
            }
            else -> now
        }
    }

    val periodExpenses = remember(expenses, filterScope, selectedPeriodKey, selectedYearKey) {
        when (filterScope) {
            "ALL" -> expenses
            "YEAR" -> {
                val yearFmt = SimpleDateFormat("yyyy", Locale.getDefault())
                expenses.filter { yearFmt.format(Date(it.timestamp)) == selectedYearKey }
            }
            else -> {
                val effKey = if (selectedPeriodKey == "CURRENT_MONTH") currentMonthKey else selectedPeriodKey
                val monthFmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                expenses.filter { monthFmt.format(Date(it.timestamp)) == effKey }
            }
        }
    }

    val periodIncomes = remember(incomes, filterScope, selectedPeriodKey, selectedYearKey) {
        when (filterScope) {
            "ALL" -> incomes
            "YEAR" -> {
                val yearFmt = SimpleDateFormat("yyyy", Locale.getDefault())
                incomes.filter { yearFmt.format(Date(it.timestamp)) == selectedYearKey }
            }
            else -> {
                val effKey = if (selectedPeriodKey == "CURRENT_MONTH") currentMonthKey else selectedPeriodKey
                val monthFmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                incomes.filter { monthFmt.format(Date(it.timestamp)) == effKey }
            }
        }
    }

    val totalSpend = remember(periodExpenses) { periodExpenses.sumOf { it.amount } }
    val totalIncome = remember(periodIncomes) { periodIncomes.sumOf { it.amount } }
    val netBalance = totalIncome - totalSpend
    val savingsRate = if (totalIncome > 0) ((netBalance / totalIncome) * 100).coerceIn(-100.0, 100.0) else 0.0

    val activeRecordedMonthsCount = remember(expenses, incomes) {
        val fmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val recordedMonths = mutableSetOf<String>()
        expenses.forEach { recordedMonths.add(fmt.format(Date(it.timestamp))) }
        incomes.forEach { recordedMonths.add(fmt.format(Date(it.timestamp))) }
        recordedMonths.size.coerceAtLeast(1)
    }

    val budgetMultiplier = remember(filterScope, activeRecordedMonthsCount) {
        when (filterScope) {
            "YEAR" -> 12
            "ALL" -> activeRecordedMonthsCount
            else -> 1
        }
    }

    val daysInActivePeriod = remember(filterScope, selectedPeriodKey, selectedYearKey, periodExpenses) {
        val cal = Calendar.getInstance()
        when (filterScope) {
            "ALL" -> {
                if (periodExpenses.isEmpty()) 1 else {
                    val minTs = periodExpenses.minOf { it.timestamp }
                    val maxTs = periodExpenses.maxOf { it.timestamp }
                    maxOf(1, ((maxTs - minTs) / (1000L * 60 * 60 * 24)).toInt() + 1)
                }
            }
            "YEAR" -> {
                val curYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(cal.time)
                if (selectedYearKey == curYear) {
                    maxOf(1, cal.get(Calendar.DAY_OF_YEAR))
                } else {
                    val y = selectedYearKey.toIntOrNull() ?: 2026
                    try {
                        if (java.time.Year.of(y).isLeap) 366 else 365
                    } catch (_: Exception) {
                        365
                    }
                }
            }
            else -> {
                if (selectedPeriodKey == "CURRENT_MONTH" || selectedPeriodKey == currentMonthKey) {
                    maxOf(1, cal.get(Calendar.DAY_OF_MONTH))
                } else {
                    try {
                        val date = SimpleDateFormat("yyyy-MM", Locale.US).parse(selectedPeriodKey) ?: Date()
                        cal.time = date
                        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    } catch (_: Exception) {
                        30
                    }
                }
            }
        }
    }
    val dailyAverageExpense = if (daysInActivePeriod > 0) totalSpend / daysInActivePeriod else 0.0

    val peakSpendingDayInfo = remember(periodExpenses, lang) {
        if (periodExpenses.isEmpty()) null else {
            val fmtKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dayMap = periodExpenses.groupBy { fmtKey.format(Date(it.timestamp)) }
            val peak = dayMap.maxByOrNull { entry -> entry.value.sumOf { it.amount } }
            peak?.let { entry ->
                val daySum = entry.value.sumOf { it.amount }
                val parsedDate = try { fmtKey.parse(entry.key) ?: Date() } catch (_: Exception) { Date() }
                val disp = SimpleDateFormat("dd MMM", Localization.getDateLocale(lang)).format(parsedDate)
                Triple(entry.key, disp, daySum)
            }
        }
    }

    val memberIncomeMap = remember(members, periodIncomes) {
        members.associateWith { member ->
            periodIncomes.filter { it.memberId == member.id }.sumOf { it.amount }
        }
    }
    val unassignedMemberIncomes = remember(members, periodIncomes) {
        periodIncomes.filter { inc -> members.none { it.id == inc.memberId } }.sumOf { it.amount }
    }

    val categoryIncomeMap = remember(periodIncomes) {
        periodIncomes.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val parentsList = remember(members) { members.filter { it.role.equals("Γονέας", ignoreCase = true) } }
    val kidsMembers = remember(members) { members.filter { it.role.equals("Παιδί", ignoreCase = true) } }
    val hasFamilyWithKids = remember(parentsList, kidsMembers) { parentsList.isNotEmpty() && kidsMembers.isNotEmpty() }

    val displayedMembers = remember(splitMode, hasFamilyWithKids, members, parentsList, kidsMembers) {
        when {
            splitMode == 3 -> members
            splitMode == 1 && hasFamilyWithKids -> parentsList
            splitMode == 2 && hasFamilyWithKids -> kidsMembers
            else -> members
        }
    }

    val (individualSpendMap, commonHouseholdSpend, directParentsSpend, kidsSpend, payerSpendMap, memberSpendMap) = remember(
        periodExpenses, members, displayedMembers, splitMode, hasFamilyWithKids, parentsList
    ) {
        val indSpendMap = members.associateWith { 0.0 }.toMutableMap()
        var commonSpend = 0.0

        periodExpenses.forEach { exp ->
            val assigned = AnalyticsExpenseFilterHelper.resolveMembersForExpense(exp, members)
            if (assigned.isEmpty()) {
                commonSpend += exp.amount
            } else {
                val perPerson = exp.amount / assigned.size
                assigned.forEach { m ->
                    indSpendMap[m] = (indSpendMap[m] ?: 0.0) + perPerson
                }
            }
        }

        var dirParents = 0.0
        var kSpend = 0.0
        members.forEach { m ->
            val spend = indSpendMap[m] ?: 0.0
            if (m.role.equals("Παιδί", ignoreCase = true)) {
                kSpend += spend
            } else {
                dirParents += spend
            }
        }

        val pSpendMap = members.associateWith { 0.0 }.toMutableMap()
        periodExpenses.forEach { exp ->
            val shares = exp.getPayerShares(members)
            shares.forEach { share ->
                val payer = members.find { it.id == share.memberId }
                    ?: members.find { it.name.equals(share.memberName, ignoreCase = true) }
                    ?: members.firstOrNull { it.role.equals("Γονέας", ignoreCase = true) }
                    ?: members.firstOrNull()
                if (payer != null) {
                    pSpendMap[payer] = (pSpendMap[payer] ?: 0.0) + share.amount
                }
            }
        }

        val mSpendMap = displayedMembers.associateWith { member ->
            if (splitMode == 3) {
                pSpendMap[member] ?: 0.0
            } else {
                val ind = indSpendMap[member] ?: 0.0
                val comShare = when {
                    splitMode == 2 -> 0.0
                    hasFamilyWithKids -> {
                        if (member.role.equals("Γονέας", ignoreCase = true) && parentsList.isNotEmpty()) {
                            commonSpend / parentsList.size
                        } else if (parentsList.isEmpty() && members.isNotEmpty()) {
                            commonSpend / members.size
                        } else {
                            0.0
                        }
                    }
                    else -> if (members.isNotEmpty()) commonSpend / members.size else 0.0
                }
                ind + comShare
            }
        }

        SpendCalculationResult(indSpendMap, commonSpend, dirParents, kSpend, pSpendMap, mSpendMap)
    }

    val parentsTotalSpend = directParentsSpend + commonHouseholdSpend

    val categorySpendMap = remember(categories, periodExpenses) {
        categories.associateWith { cat ->
            val catNorm = GreekVoiceParser.normalizeGreek(cat.name.trim().lowercase())
            periodExpenses.filter { exp ->
                exp.categoryId == cat.id ||
                    (exp.categoryName.isNotBlank() && GreekVoiceParser.normalizeGreek(exp.categoryName.trim().lowercase()) == catNorm)
            }.sumOf { it.amount }
        }
    }

    val membersWithDeficit = remember(members, payerSpendMap, memberIncomeMap) {
        members.mapNotNull { m ->
            val paid = payerSpendMap[m] ?: 0.0
            val income = memberIncomeMap[m] ?: 0.0
            val deficit = paid - income
            if (deficit > 0.01 && (paid > 0.0 || income > 0.0)) Triple(m, paid, income) else null
        }
    }

    val hasPeriodGoalDeposits = remember(periodExpenses) {
        periodExpenses.any { SavingsGoalExpenseLinkHelper.isGoalDepositExpense(it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "period_selector", contentType = "period_selector") {
            AnalyticsPeriodSelectorCard(
                filterScope = filterScope,
                selectedPeriodKey = selectedPeriodKey,
                selectedYearKey = selectedYearKey,
                currentMonthKey = currentMonthKey,
                currentMonthLabel = currentMonthLabel,
                currentYearKey = currentYearKey,
                availableMonths = availableMonths,
                availableYears = availableYears,
                periodExpensesCount = periodExpenses.size,
                periodIncomesCount = periodIncomes.size,
                lang = lang,
                getMonthLabel = ::getMonthLabel,
                onFilterScopeChange = { filterScope = it },
                onSelectedPeriodKeyChange = { selectedPeriodKey = it },
                onSelectedYearKeyChange = { selectedYearKey = it }
            )
        }

        item(key = "budget_alerts", contentType = "budget_alerts") {
            BudgetAlertsCard(
                expenses = periodExpenses,
                categories = categories,
                appLanguage = appLanguage,
                onManageBudgets = if (canManageStructure) onManageCategories else ({}),
                onCategoryClick = { cat ->
                    onDrillDownToHome(
                        HomeDrillDownRequest(
                            periodKey = currentMonthKey,
                            typeFilter = "EXPENSE",
                            categoryId = cat.id
                        )
                    )
                }
            )
        }

        item(key = "summary_kpi", contentType = "summary_kpi") {
            AnalyticsSummaryKpiCard(
                activePeriodDisplay = activePeriodDisplay,
                activeDrillDownPeriodKey = activeDrillDownPeriodKey,
                totalIncome = totalIncome,
                totalSpend = totalSpend,
                netBalance = netBalance,
                savingsRate = savingsRate,
                periodIncomesCount = periodIncomes.size,
                periodExpensesCount = periodExpenses.size,
                dailyAverageExpense = dailyAverageExpense,
                peakSpendingDayInfo = peakSpendingDayInfo,
                lang = lang,
                onDrillDownToHome = onDrillDownToHome
            )
        }

        if (filterScope == "MONTH") {
            item(key = "month_forecast", contentType = "month_forecast") {
                AnalyticsMonthForecastCard(
                    allExpenses = expenses,
                    allIncomes = incomes,
                    recurringExpenses = recurringExpenses,
                    selectedYear = selectedMonthYearPair.first,
                    selectedMonthZeroIndexed = selectedMonthYearPair.second,
                    lang = lang
                )
            }
        }

        item(key = "family_income", contentType = "family_income") {
            AnalyticsFamilyIncomeCard(
                periodIncomes = periodIncomes,
                members = members,
                memberIncomeMap = memberIncomeMap,
                unassignedMemberIncomes = unassignedMemberIncomes,
                categoryIncomeMap = categoryIncomeMap,
                totalIncome = totalIncome,
                chartTypeIncome = chartTypeIncome,
                activeDrillDownPeriodKey = activeDrillDownPeriodKey,
                appLanguage = appLanguage,
                lang = lang,
                onSelectChartTypeIncome = { chartTypeIncome = it },
                onDrillDownToHome = onDrillDownToHome
            )
        }

        if (membersWithDeficit.isNotEmpty()) {
            item(key = "member_deficit_alert", contentType = "member_deficit_alert") {
                AnalyticsMemberDeficitAlertCard(
                    membersWithDeficit = membersWithDeficit,
                    lang = lang
                )
            }
        }

        if (hasFamilyWithKids) {
            item(key = "family_vs_kids", contentType = "family_vs_kids") {
                AnalyticsFamilyVsKidsCard(
                    parentsTotalSpend = parentsTotalSpend,
                    kidsSpend = kidsSpend,
                    totalSpend = totalSpend,
                    chartTypeFamily = chartTypeFamily,
                    activeDrillDownPeriodKey = activeDrillDownPeriodKey,
                    appLanguage = appLanguage,
                    lang = lang,
                    onSelectChartTypeFamily = { chartTypeFamily = it },
                    onDrillDownToHome = onDrillDownToHome
                )
            }

            item(key = "parents_for_kids", contentType = "parents_for_kids") {
                AnalyticsParentsForKidsCard(
                    kidsMembers = kidsMembers,
                    individualSpendMap = individualSpendMap,
                    kidsSpend = kidsSpend,
                    activeDrillDownPeriodKey = activeDrillDownPeriodKey,
                    lang = lang,
                    onDrillDownToHome = onDrillDownToHome
                )
            }
        }

        item(key = "members_breakdown", contentType = "members_breakdown") {
            AnalyticsMembersBreakdownCard(
                displayedMembers = displayedMembers,
                memberSpendMap = memberSpendMap,
                individualSpendMap = individualSpendMap,
                payerSpendMap = payerSpendMap,
                memberIncomeMap = memberIncomeMap,
                hasFamilyWithKids = hasFamilyWithKids,
                splitMode = splitMode,
                chartTypeMembers = chartTypeMembers,
                activeDrillDownPeriodKey = activeDrillDownPeriodKey,
                appLanguage = appLanguage,
                isGreek = isGreek,
                lang = lang,
                onSplitModeChange = { splitMode = it },
                onChartTypeMembersChange = { chartTypeMembers = it },
                onDrillDownToHome = onDrillDownToHome
            )
        }

        item(key = "category_budgets", contentType = "category_budgets") {
            AnalyticsCategoryBudgetsCard(
                categories = categories,
                categorySpendMap = categorySpendMap,
                budgetMultiplier = budgetMultiplier,
                filterScope = filterScope,
                activeDrillDownPeriodKey = activeDrillDownPeriodKey,
                lang = lang,
                onDrillDownToHome = onDrillDownToHome
            )
        }

        if (savingsGoals.isNotEmpty() || hasPeriodGoalDeposits) {
            item(key = "savings_goals", contentType = "savings_goals") {
                AnalyticsSavingsGoalsCard(
                    savingsGoals = savingsGoals,
                    periodExpenses = periodExpenses,
                    categories = categories,
                    activeDrillDownPeriodKey = activeDrillDownPeriodKey,
                    lang = lang,
                    onOpenSavingsGoals = onOpenSavingsGoals,
                    onDrillDownToHome = onDrillDownToHome
                )
            }
        }

        if (members.size >= 2) {
            item(key = "settle_up", contentType = "settle_up") {
                AnalyticsSettleUpCard(
                    periodExpenses = periodExpenses,
                    members = members,
                    activeDrillDownPeriodKey = activeDrillDownPeriodKey,
                    settlePeriodTimestamp = settlePeriodTimestamp,
                    lang = lang,
                    isGreek = isGreek,
                    onDrillDownToHome = onDrillDownToHome,
                    onSettleTransfer = onSettleTransfer
                )
            }
        }

        item(key = "data_export", contentType = "data_export") {
            AnalyticsDataExportCard(
                expenses = expenses,
                incomes = incomes,
                categories = categories,
                members = members,
                isPro = isPro,
                isGreek = isGreek,
                lang = lang,
                context = context,
                onShowPremiumPaywall = onShowPremiumPaywall
            )
        }
    }
}
