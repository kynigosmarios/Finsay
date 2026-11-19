package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.entity.SavingsGoal
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.FinsayMotion
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.util.AnalyticsSpecialFilter
import com.example.util.HomeDrillDownRequest
import com.example.util.Localization
import com.example.util.SavingsGoalExpenseLinkHelper
import java.util.Locale

@Composable
internal fun AnalyticsMembersBreakdownCard(
    displayedMembers: List<FamilyMember>,
    memberSpendMap: Map<FamilyMember, Double>,
    individualSpendMap: Map<FamilyMember, Double>,
    payerSpendMap: Map<FamilyMember, Double>,
    memberIncomeMap: Map<FamilyMember, Double>,
    hasFamilyWithKids: Boolean,
    splitMode: Int,
    chartTypeMembers: Int,
    activeDrillDownPeriodKey: String,
    appLanguage: String,
    isGreek: Boolean,
    lang: Localization.Language,
    onSplitModeChange: (Int) -> Unit,
    onChartTypeMembersChange: (Int) -> Unit,
    onDrillDownToHome: (HomeDrillDownRequest) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            AnalyticsSectionHeader(
                icon = Icons.Default.Groups,
                iconTint = Emerald700,
                iconContainerColor = Emerald100,
                title = Localization.breakdownByMember(lang),
                trailing = {
                    ChartTypeToggle(
                        selectedType = chartTypeMembers,
                        onSelectType = onChartTypeMembersChange,
                        activeColor = Emerald700,
                        appLanguage = appLanguage
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            val availableModes = if (hasFamilyWithKids) {
                listOf(
                    0 to Localization.individualMode(lang),
                    1 to Localization.parentsMode(lang),
                    2 to Localization.kidsMode(lang),
                    3 to (if (lang == Localization.Language.ENGLISH) "Who Paid" else "Ποιος Πλήρωσε")
                )
            } else {
                listOf(
                    0 to Localization.individualMode(lang),
                    3 to (if (lang == Localization.Language.ENGLISH) "Who Paid" else "Ποιος Πλήρωσε")
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    availableModes.forEach { (modeIndex, title) ->
                        val isSelected = splitMode == modeIndex
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) Emerald700 else Color.Transparent,
                            animationSpec = tween(220),
                            label = "split_tab_bg_$modeIndex"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(220),
                            label = "split_tab_text_$modeIndex"
                        )
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = bgColor,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSplitModeChange(modeIndex) }
                                .testTag("split_mode_tab_$modeIndex")
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val triggerMemberBreakdownDrillDown: (FamilyMember) -> Unit = { member ->
                val req = when (splitMode) {
                    3 -> HomeDrillDownRequest(
                        periodKey = activeDrillDownPeriodKey,
                        typeFilter = "EXPENSE",
                        specialFilter = AnalyticsSpecialFilter(
                            badgeLabel = "${if (isGreek) "Πλήρωσε" else "Paid by"}: ${member.name}",
                            payerMemberId = member.id
                        )
                    )
                    2 -> HomeDrillDownRequest(
                        periodKey = activeDrillDownPeriodKey,
                        typeFilter = "EXPENSE",
                        memberId = member.id,
                        specialFilter = AnalyticsSpecialFilter(
                            badgeLabel = member.name,
                            exactMemberId = member.id
                        )
                    )
                    else -> HomeDrillDownRequest(
                        periodKey = activeDrillDownPeriodKey,
                        typeFilter = "EXPENSE",
                        memberId = member.id
                    )
                }
                onDrillDownToHome(req)
            }

            if (displayedMembers.isEmpty()) {
                Text(
                    text = if (splitMode == 2) Localization.noKidsRegisteredHint(lang) else Localization.noMembersInCategoryHint(lang),
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                AnimatedContent(
                    targetState = chartTypeMembers,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "members_chart_type"
                ) { activeChart ->
                if (activeChart == 1) {
                    val memberSlices = displayedMembers.map { m ->
                        PieChartSlice(
                            label = m.name,
                            value = memberSpendMap[m] ?: 0.0,
                            color = Color(m.avatarColorHex),
                            onClick = { triggerMemberBreakdownDrillDown(m) }
                        )
                    }

                    val displayedTotal = memberSpendMap.values.sum()
                    PieChartView(
                        slices = memberSlices,
                        totalAmount = displayedTotal,
                        appLanguage = appLanguage
                    )
                } else {
                    val safeMax = memberSpendMap.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

                    Column {
                    displayedMembers.forEach { member ->
                        val spend = memberSpendMap[member] ?: 0.0
                        val ind = individualSpendMap[member] ?: 0.0
                        val com = (spend - ind).coerceAtLeast(0.0)
                        val progress by animateFloatAsState(
                            targetValue = (spend / safeMax).toFloat().coerceIn(0f, 1f),
                            animationSpec = FinsayMotion.ProgressSpec,
                            label = "member_spend_progress_${member.id}"
                        )
                        val avatarColor = Color(member.avatarColorHex)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { triggerMemberBreakdownDrillDown(member) }
                                .padding(vertical = 7.dp, horizontal = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(avatarColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = member.name.take(1),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = member.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val mPaid = payerSpendMap[member] ?: 0.0
                                            val mInc = memberIncomeMap[member] ?: 0.0
                                            if (mPaid > mInc && (mPaid > 0.0 || mInc > 0.0)) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFEF4444).copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = Localization.expensesExceedIncomeTag(lang),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFDC2626),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                    )
                                                }
                                            }
                                        }
                                        val subtitle = when (splitMode) {
                                            0 -> if (com > 0.01) {
                                                if (lang == Localization.Language.ENGLISH) "Direct: ${String.format(Locale.US, "%.1f€", ind)} • Shared: ${String.format(Locale.US, "%.1f€", com)}"
                                                else "Ατομικά: ${String.format(Locale.US, "%.1f€", ind)} • Κοινό: ${String.format(Locale.US, "%.1f€", com)}"
                                            } else {
                                                Localization.translateRole(member.role, lang)
                                            }
                                            else -> Localization.translateRole(member.role, lang)
                                        }
                                        Text(
                                            text = subtitle,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = String.format(Locale.US, "%.2f €", spend),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = avatarColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
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
internal fun AnalyticsCategoryBudgetsCard(
    categories: List<ExpenseCategory>,
    categorySpendMap: Map<ExpenseCategory, Double>,
    budgetMultiplier: Int,
    filterScope: String,
    activeDrillDownPeriodKey: String,
    lang: Localization.Language,
    onDrillDownToHome: (HomeDrillDownRequest) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            val scopeSubtitle = if (budgetMultiplier > 1) {
                if (filterScope == "YEAR") {
                    if (lang == Localization.Language.ENGLISH) "Yearly (x12 months)" else "Ετήσιο (x12 μήνες)"
                } else {
                    if (lang == Localization.Language.ENGLISH) "Total (x$budgetMultiplier months)" else "Σύνολο (x$budgetMultiplier μήνες)"
                }
            } else null

            AnalyticsSectionHeader(
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = Emerald700,
                iconContainerColor = Emerald100,
                title = Localization.budgetMonitoring(lang),
                subtitle = scopeSubtitle,
                subtitleColor = Emerald700,
                trailing = {
                    Text(
                        text = Localization.categoriesCount(categories.size, lang),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            val activeCategories = categories
                .filter { (categorySpendMap[it] ?: 0.0) > 0.01 || it.monthlyBudget > 0.01 }
                .sortedWith(
                    compareByDescending<ExpenseCategory> { cat ->
                        val spent = categorySpendMap[cat] ?: 0.0
                        val budget = cat.monthlyBudget * budgetMultiplier
                        if (budget > 0.0) spent / budget else 0.0
                    }.thenByDescending { categorySpendMap[it] ?: 0.0 }
                )
            val displayedCats = if (activeCategories.isNotEmpty()) {
                activeCategories
            } else {
                categories.sortedWith(
                    compareByDescending<ExpenseCategory> { cat ->
                        val spent = categorySpendMap[cat] ?: 0.0
                        val budget = cat.monthlyBudget * budgetMultiplier
                        if (budget > 0.0) spent / budget else 0.0
                    }.thenByDescending { categorySpendMap[it] ?: 0.0 }
                ).take(6)
            }

            if (displayedCats.isEmpty()) {
                Text(
                    text = if (lang == Localization.Language.ENGLISH) "No expenses recorded for this period." else "Δεν υπάρχουν καταγεγραμμένα έξοδα για αυτή την περίοδο.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                displayedCats.forEach { cat ->
                    val spent = categorySpendMap[cat] ?: 0.0
                    val budget = cat.monthlyBudget * budgetMultiplier
                    val ratio = if (budget > 0) spent / budget else 0.0
                    val progress by animateFloatAsState(
                        targetValue = ratio.toFloat().coerceIn(0f, 1f),
                        animationSpec = FinsayMotion.ProgressSpec,
                        label = "cat_budget_progress_${cat.id}"
                    )

                    val isExact = budget > 0 && (kotlin.math.abs(spent - budget) < 0.05 || (ratio >= 0.999 && ratio <= 1.001))
                    val isExceeded = budget > 0 && spent > budget + 0.05
                    val isWarning = budget > 0 && !isExact && !isExceeded && ratio >= 0.75

                    val barColor = when {
                        isExceeded -> Color(0xFFEF4444)
                        isWarning -> Color(0xFFF59E0B)
                        else -> Emerald700
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onDrillDownToHome(
                                    HomeDrillDownRequest(
                                        periodKey = activeDrillDownPeriodKey,
                                        typeFilter = "EXPENSE",
                                        categoryId = cat.id
                                    )
                                )
                            }
                            .padding(vertical = 5.dp, horizontal = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color(cat.colorHex).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(cat.iconKey),
                                        contentDescription = null,
                                        tint = Color(cat.colorHex),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Localization.translateCategory(cat.name, lang),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (budget > 0) {
                                    val budgetFormatted = if (budget % 1.0 == 0.0) String.format(Locale.US, "%.0f€", budget) else String.format(Locale.US, "%.2f€", budget)
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f€", spent)} / $budgetFormatted",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = barColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "${(ratio * 100).toInt()}%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = barColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = String.format(Locale.US, "%.2f €", spent),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald700
                                    )
                                }
                            }
                        }

                        if (budget > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AnalyticsSavingsGoalsCard(
    savingsGoals: List<SavingsGoal>,
    periodExpenses: List<Expense>,
    categories: List<ExpenseCategory>,
    activeDrillDownPeriodKey: String,
    lang: Localization.Language,
    onOpenSavingsGoals: () -> Unit,
    onDrillDownToHome: (HomeDrillDownRequest) -> Unit
) {
    val isGreek = lang != Localization.Language.ENGLISH
    val savingsCategory = SavingsGoalExpenseLinkHelper.findSavingsCategory(categories)
    val periodGoalDeposits = periodExpenses.filter { exp ->
        SavingsGoalExpenseLinkHelper.isGoalDepositExpense(exp) ||
            (savingsCategory != null && exp.categoryId == savingsCategory.id)
    }
    val periodDepositedTotal = periodGoalDeposits.sumOf { it.amount }
    val totalSavedAcrossGoals = savingsGoals.sumOf { it.currentAmount.coerceAtLeast(0.0) }
    val totalTargetAcrossGoals = savingsGoals.sumOf { it.targetAmount.coerceAtLeast(0.0) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_savings_goals_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            AnalyticsSectionHeader(
                icon = Icons.Default.Savings,
                iconTint = Emerald700,
                iconContainerColor = Emerald100,
                title = if (isGreek) "Στόχοι Αποταμίευσης" else "Savings Goals",
                subtitle = if (totalTargetAcrossGoals > 0.0) {
                    "${String.format(Locale.US, "%.0f€", totalSavedAcrossGoals)} / ${String.format(Locale.US, "%.0f€", totalTargetAcrossGoals)}"
                } else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenSavingsGoals() }
                    .padding(vertical = 2.dp),
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Period deposits pill (tappable -> drills down to Home expenses)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Emerald700.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.22f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = periodDepositedTotal > 0.0) {
                        onDrillDownToHome(
                            HomeDrillDownRequest(
                                periodKey = activeDrillDownPeriodKey,
                                typeFilter = "EXPENSE",
                                categoryId = savingsCategory?.id
                            )
                        )
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGreek) "Καταθέσεις σε στόχους (περίοδος)" else "Deposits to goals (period)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (periodGoalDeposits.isNotEmpty()) {
                                if (isGreek) "${periodGoalDeposits.size} καταθέσεις"
                                else "${periodGoalDeposits.size} deposits"
                            } else {
                                if (isGreek) "Καμία κατάθεση"
                                else "No deposits"
                            },
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.US, "%.2f€", periodDepositedTotal),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald700
                    )
                }
            }

            if (savingsGoals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savingsGoals.forEach { goal ->
                        val target = goal.targetAmount.coerceAtLeast(0.0)
                        val saved = goal.currentAmount.coerceAtLeast(0.0)
                        val ratio = if (target > 0.0) (saved / target).toFloat().coerceIn(0f, 1f) else 0f
                        val animatedRatio by animateFloatAsState(
                            targetValue = ratio,
                            animationSpec = FinsayMotion.ProgressSpec,
                            label = "goal_progress_${goal.id}"
                        )
                        val pct = (ratio * 100f).toInt().coerceIn(0, 100)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenSavingsGoals() }
                                .padding(vertical = 3.dp, horizontal = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = goal.title,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%.0f€", saved)} / ${String.format(Locale.US, "%.0f€", target)} ($pct%)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { animatedRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape),
                                color = Emerald700,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
