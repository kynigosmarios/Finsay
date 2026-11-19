package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.ui.components.FinsayMotion
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.util.AnalyticsForecastHelper
import com.example.util.ExpenseReportExporter
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun AnalyticsMonthForecastCard(
    allExpenses: List<Expense>,
    allIncomes: List<Income>,
    recurringExpenses: List<RecurringExpense>,
    selectedYear: Int,
    selectedMonthZeroIndexed: Int,
    lang: Localization.Language
) {
    val result = remember(
        allExpenses,
        allIncomes,
        recurringExpenses,
        selectedYear,
        selectedMonthZeroIndexed
    ) {
        AnalyticsForecastHelper.calculate(
            allExpenses = allExpenses,
            allIncomes = allIncomes,
            recurringExpenses = recurringExpenses,
            selectedYear = selectedYear,
            selectedMonthZeroIndexed = selectedMonthZeroIndexed
        )
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.28f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_month_forecast_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            AnalyticsSectionHeader(
                icon = Icons.Default.Timeline,
                iconTint = Emerald700,
                iconContainerColor = Emerald100,
                title = Localization.monthlyComparisonTitle(lang),
                trailing = if (result.isCurrentCalendarMonth) {
                    {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald700.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${result.elapsedDaysInMonth}/${result.totalDaysInMonth}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700
                                )
                            }
                        }
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Expense MoM Delta Tile
                val expIncreased = result.expenseDeltaAmount > 0.01
                val expDecreased = result.expenseDeltaAmount < -0.01
                val expAccent = when {
                    expIncreased -> Color(0xFFDC2626)
                    expDecreased -> Color(0xFF059669)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = expAccent.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, expAccent.copy(alpha = 0.22f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (expIncreased) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = expAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = Localization.expenses(lang),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val sign = if (result.expenseDeltaAmount >= 0) "+" else ""
                        Text(
                            text = String.format(Locale.US, "%s%.2f €", sign, result.expenseDeltaAmount),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = expAccent,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val pctText = result.expenseDeltaPercent?.let { pct ->
                            val pctSign = if (pct >= 0) "+" else ""
                            "$pctSign$pct% "
                        } ?: ""
                        Text(
                            text = "$pctText${if (lang == Localization.Language.ENGLISH) "vs prev." else "vs προηγ."}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Income MoM Delta Tile
                val incIncreased = result.incomeDeltaAmount > 0.01
                val incDecreased = result.incomeDeltaAmount < -0.01
                val incAccent = when {
                    incIncreased -> Color(0xFF059669)
                    incDecreased -> Color(0xFFDC2626)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = incAccent.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, incAccent.copy(alpha = 0.22f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (incIncreased) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = incAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = Localization.incomes(lang),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val sign = if (result.incomeDeltaAmount >= 0) "+" else ""
                        Text(
                            text = String.format(Locale.US, "%s%.2f €", sign, result.incomeDeltaAmount),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = incAccent,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val pctText = result.incomeDeltaPercent?.let { pct ->
                            val pctSign = if (pct >= 0) "+" else ""
                            "$pctSign$pct% "
                        } ?: ""
                        Text(
                            text = "$pctText${if (lang == Localization.Language.ENGLISH) "vs prev." else "vs προηγ."}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (result.isCurrentCalendarMonth) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = Localization.forecastEndOfMonthTitle(lang),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald700
                )
                Spacer(modifier = Modifier.height(6.dp))

                val monthProgressTarget = (result.elapsedDaysInMonth.toFloat() / result.totalDaysInMonth.toFloat()).coerceIn(0f, 1f)
                val monthProgress by animateFloatAsState(
                    targetValue = monthProgressTarget,
                    animationSpec = FinsayMotion.ProgressSpec,
                    label = "month_forecast_progress"
                )
                LinearProgressIndicator(
                    progress = { monthProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Emerald700,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Localization.projectedTotalLabel(lang),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f €", result.projectedEndOfMonthExpenses),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Localization.remainingScheduledLabel(lang),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f €", result.remainingScheduledRecurringTotal),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val isSurplus = result.projectedEndOfMonthNet >= 0.0
                val netColor = if (isSurplus) Color(0xFF059669) else Color(0xFFDC2626)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = netColor.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, netColor.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSurplus) Localization.projectedSurplusLabel(lang) else Localization.projectedOverBudgetLabel(lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = netColor
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%s%.2f €",
                                if (isSurplus) "+" else "",
                                result.projectedEndOfMonthNet
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = netColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AnalyticsDataExportCard(
    expenses: List<Expense>,
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
    isPro: Boolean,
    isGreek: Boolean,
    lang: Localization.Language,
    context: Context,
    onShowPremiumPaywall: (String) -> Unit
) {
    var exportPeriodIndex by remember { mutableIntStateOf(0) }
    var exportRangeStart by remember {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -30)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        mutableLongStateOf(cal.timeInMillis)
    }
    var exportRangeEnd by remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        mutableLongStateOf(cal.timeInMillis)
    }
    var selectedExportCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedExportMemberId by remember { mutableStateOf<Long?>(null) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showMemberDropdown by remember { mutableStateOf(false) }

    val dateSdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val filteredExportExpenses = remember(
        expenses,
        exportPeriodIndex,
        exportRangeStart,
        exportRangeEnd,
        selectedExportCategoryId,
        selectedExportMemberId
    ) {
        val nowCal = Calendar.getInstance()
        val curYear = nowCal.get(Calendar.YEAR)
        val curMonth = nowCal.get(Calendar.MONTH)
        val prevCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val prevYear = prevCal.get(Calendar.YEAR)
        val prevMonth = prevCal.get(Calendar.MONTH)
        val reusableCal = Calendar.getInstance()

        expenses.filter { exp ->
            val matchesPeriod = when (exportPeriodIndex) {
                0 -> {
                    reusableCal.timeInMillis = exp.timestamp
                    reusableCal.get(Calendar.YEAR) == curYear && reusableCal.get(Calendar.MONTH) == curMonth
                }
                1 -> {
                    reusableCal.timeInMillis = exp.timestamp
                    reusableCal.get(Calendar.YEAR) == prevYear && reusableCal.get(Calendar.MONTH) == prevMonth
                }
                2 -> exp.timestamp in exportRangeStart..exportRangeEnd
                else -> true
            }
            val matchesCategory = selectedExportCategoryId == null || exp.categoryId == selectedExportCategoryId
            val matchesMember = selectedExportMemberId == null ||
                exp.payerMemberId == selectedExportMemberId ||
                exp.memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }.contains(selectedExportMemberId)

            matchesPeriod && matchesCategory && matchesMember
        }
    }

    val exportPeriodTitle = when (exportPeriodIndex) {
        0 -> if (isGreek) "Τρέχων Μήνας" else "Current Month"
        1 -> if (isGreek) "Προηγούμενος Μήνας" else "Previous Month"
        2 -> "${dateSdf.format(exportRangeStart)} - ${dateSdf.format(exportRangeEnd)}"
        else -> if (isGreek) "Όλα τα Έξοδα" else "All Expenses"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_export_data_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            AnalyticsSectionHeader(
                icon = Icons.Default.TableChart,
                iconTint = Emerald700,
                iconContainerColor = Emerald100,
                title = if (isGreek) "Εξαγωγή δεδομένων" else "Data Export",
                badge = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Pro",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                trailing = {
                    Text(
                        text = if (isGreek) "${filteredExportExpenses.size} έξοδα" else "${filteredExportExpenses.size} items",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    if (isGreek) "Τρέχων" else "Current",
                    if (isGreek) "Προηγ." else "Prev",
                    if (isGreek) "Διάστημα" else "Range",
                    if (isGreek) "Όλα" else "All"
                ).forEachIndexed { idx, label ->
                    val isSel = exportPeriodIndex == idx
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSel) Emerald700 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, if (isSel) Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { exportPeriodIndex = idx }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = exportPeriodIndex == 2,
                enter = FinsayMotion.ExpandEnter,
                exit = FinsayMotion.ShrinkExit
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = exportRangeStart }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(y, m, d, 0, 0, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            exportRangeStart = newCal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        ) {
                            Text(
                                text = (if (isGreek) "Από: " else "From: ") + dateSdf.format(exportRangeStart),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Emerald700,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = exportRangeEnd }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(y, m, d, 23, 59, 59)
                                                set(Calendar.MILLISECOND, 999)
                                            }
                                            exportRangeEnd = newCal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        ) {
                            Text(
                                text = (if (isGreek) "Έως: " else "To: ") + dateSdf.format(exportRangeEnd),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Emerald700,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    val catName = categories.find { it.id == selectedExportCategoryId }?.let {
                        Localization.translateCategory(it.name, lang)
                    } ?: if (isGreek) "Όλες οι κατηγορίες" else "All categories"

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = catName,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isGreek) "Όλες οι κατηγορίες" else "All categories", fontSize = 12.sp) },
                            onClick = {
                                selectedExportCategoryId = null
                                showCategoryDropdown = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(Localization.translateCategory(cat.name, lang), fontSize = 12.sp) },
                                onClick = {
                                    selectedExportCategoryId = cat.id
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    val memName = members.find { it.id == selectedExportMemberId }?.name
                        ?: if (isGreek) "Όλα τα μέλη" else "All members"

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMemberDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = memName,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMemberDropdown,
                        onDismissRequest = { showMemberDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isGreek) "Όλα τα μέλη" else "All members", fontSize = 12.sp) },
                            onClick = {
                                selectedExportMemberId = null
                                showMemberDropdown = false
                            }
                        )
                        members.forEach { mem ->
                            DropdownMenuItem(
                                text = { Text(mem.name, fontSize = 12.sp) },
                                onClick = {
                                    selectedExportMemberId = mem.id
                                    showMemberDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (!isPro) {
                            onShowPremiumPaywall("export_report")
                        } else {
                            val targetList = if (filteredExportExpenses.isNotEmpty()) filteredExportExpenses else expenses
                            val targetTitle = if (filteredExportExpenses.isNotEmpty()) exportPeriodTitle else (if (isGreek) "Όλα τα Έξοδα" else "All Expenses")
                            if (targetList.isEmpty()) {
                                Toast.makeText(context, if (isGreek) "Δεν υπάρχουν καταχωρημένα έξοδα προς εξαγωγή" else "No expenses to export", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, if (isGreek) "Δημιουργία και κοινοποίηση PDF..." else "Generating and sharing PDF...", Toast.LENGTH_SHORT).show()
                                ExpenseReportExporter.exportPdf(context, targetList, targetTitle)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("export_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ".pdf",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White.copy(alpha = 0.85f))
                }

                Button(
                    onClick = {
                        if (!isPro) {
                            onShowPremiumPaywall("export_report")
                        } else {
                            val targetList = if (filteredExportExpenses.isNotEmpty()) filteredExportExpenses else expenses
                            val targetTitle = if (filteredExportExpenses.isNotEmpty()) exportPeriodTitle else (if (isGreek) "Όλα τα Έξοδα" else "All Expenses")
                            if (targetList.isEmpty()) {
                                Toast.makeText(context, if (isGreek) "Δεν υπάρχουν καταχωρημένα έξοδα προς εξαγωγή" else "No expenses to export", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, if (isGreek) "Δημιουργία και κοινοποίηση CSV..." else "Generating and sharing CSV...", Toast.LENGTH_SHORT).show()
                                ExpenseReportExporter.exportCsv(context, targetList, targetTitle)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("export_csv_button")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ".csv",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White.copy(alpha = 0.85f))
                }
            }
        }
    }
}
