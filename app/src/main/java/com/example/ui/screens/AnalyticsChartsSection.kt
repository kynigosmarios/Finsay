package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.util.AnalyticsSpecialFilter
import com.example.util.HomeDrillDownRequest
import com.example.util.LocalCurrencySymbol
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PieChartSlice(
    val label: String,
    val value: Double,
    val color: Color,
    val onClick: (() -> Unit)? = null
)

@Composable
internal fun AnalyticsSectionHeader(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconContainerColor: Color = iconTint.copy(alpha = 0.13f),
    badge: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .padding(end = if (trailing != null) 8.dp else 0.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = iconContainerColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (badge != null) {
                        badge()
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PieChartView(
    slices: List<PieChartSlice>,
    totalAmount: Double,
    appLanguage: String = "EN",
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val currencySymbol = LocalCurrencySymbol.current
    val total = totalAmount.coerceAtLeast(1.0)
    val validSlices = slices.filter { it.value > 0.001 }

    if (validSlices.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Localization.noExpensesRegistered(lang),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                val strokeWidth = 20.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                val rect = androidx.compose.ui.geometry.Rect(
                    centerOffset.x - radius,
                    centerOffset.y - radius,
                    centerOffset.x + radius,
                    centerOffset.y + radius
                )

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                var startAngle = -90f
                validSlices.forEach { slice ->
                    val sweepAngle = ((slice.value / total) * 360f).toFloat()
                    val gap = if (validSlices.size > 1) 4f else 0f
                    val effectiveSweep = (sweepAngle - gap).coerceAtLeast(1f)
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle + (gap / 2),
                        sweepAngle = effectiveSweep,
                        useCenter = false,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = Localization.total(lang).uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${String.format(Locale.US, "%.2f", totalAmount)} $currencySymbol",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            validSlices.forEach { slice ->
                val pct = ((slice.value / total) * 100).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .then(
                            if (slice.onClick != null) {
                                Modifier.clickable { slice.onClick.invoke() }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${slice.label} ($pct%): ${String.format(Locale.US, "%.2f", slice.value)}$currencySymbol",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
internal fun ChartTypeToggle(
    selectedType: Int,
    onSelectType: (Int) -> Unit,
    activeColor: Color = Emerald700,
    appLanguage: String = "EN",
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val barBg by animateColorAsState(
                targetValue = if (selectedType == 0) activeColor else Color.Transparent,
                animationSpec = tween(200),
                label = "chart_toggle_bar_bg"
            )
            val barTint by animateColorAsState(
                targetValue = if (selectedType == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "chart_toggle_bar_tint"
            )
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = barBg,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .clickable { onSelectType(0) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = Localization.barChartView(lang),
                        tint = barTint,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            val pieBg by animateColorAsState(
                targetValue = if (selectedType == 1) activeColor else Color.Transparent,
                animationSpec = tween(200),
                label = "chart_toggle_pie_bg"
            )
            val pieTint by animateColorAsState(
                targetValue = if (selectedType == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "chart_toggle_pie_tint"
            )
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = pieBg,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .clickable { onSelectType(1) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = Localization.pieChartView(lang),
                        tint = pieTint,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun AnalyticsPeriodSelectorCard(
    filterScope: String,
    selectedPeriodKey: String,
    selectedYearKey: String,
    currentMonthKey: String,
    currentMonthLabel: String,
    currentYearKey: String,
    availableMonths: List<String>,
    availableYears: List<String>,
    periodExpensesCount: Int,
    periodIncomesCount: Int,
    lang: Localization.Language,
    getMonthLabel: (String) -> String,
    onFilterScopeChange: (String) -> Unit,
    onSelectedPeriodKeyChange: (String) -> Unit,
    onSelectedYearKeyChange: (String) -> Unit
) {
    var showPeriodDropdown by remember { mutableStateOf(false) }
    var showYearDropdown by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_month_selector_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val scopes = listOf(
                    Triple("MONTH", Localization.tr(lang, "Month", "Μήνας"), Icons.Default.CalendarMonth),
                    Triple("YEAR", Localization.tr(lang, "Year", "Χρονιά"), Icons.Default.DateRange),
                    Triple("ALL", Localization.tr(lang, "Total", "Σύνολο"), Icons.Default.AllInclusive)
                )

                scopes.forEach { (scopeKey, label, icon) ->
                    val isSelected = filterScope == scopeKey
                    val pillBg by animateColorAsState(
                        targetValue = if (isSelected) Emerald700 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        animationSpec = tween(200),
                        label = "scope_bg_$scopeKey"
                    )
                    val pillContent by animateColorAsState(
                        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "scope_fg_$scopeKey"
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = pillBg,
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onFilterScopeChange(scopeKey) }
                            .testTag("analytics_filter_${scopeKey.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = pillContent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = pillContent,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (filterScope) {
                "MONTH" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val effKey = if (selectedPeriodKey == "CURRENT_MONTH") currentMonthKey else selectedPeriodKey
                                try {
                                    val cal = Calendar.getInstance().apply {
                                        time = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(effKey) ?: Date()
                                        add(Calendar.MONTH, -1)
                                    }
                                    val prevKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
                                    onSelectedPeriodKeyChange(if (prevKey == currentMonthKey) "CURRENT_MONTH" else prevKey)
                                } catch (_: Exception) {
                                    onSelectedPeriodKeyChange("CURRENT_MONTH")
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Month",
                                tint = Emerald700
                            )
                        }

                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Emerald700.copy(alpha = 0.09f),
                                border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showPeriodDropdown = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedPeriodKey == "CURRENT_MONTH" || selectedPeriodKey == currentMonthKey) currentMonthLabel else getMonthLabel(selectedPeriodKey),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = Localization.selectMonthYear(lang),
                                        tint = Emerald700,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showPeriodDropdown,
                                onDismissRequest = { showPeriodDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                                    },
                                    text = {
                                        Text(
                                            text = "${Localization.currentMonth(lang)} ($currentMonthLabel)",
                                            fontWeight = if (selectedPeriodKey == "CURRENT_MONTH" || selectedPeriodKey == currentMonthKey) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onSelectedPeriodKeyChange("CURRENT_MONTH")
                                        showPeriodDropdown = false
                                    }
                                )
                                availableMonths.filter { it != currentMonthKey }.forEach { monthKey ->
                                    val label = getMonthLabel(monthKey)
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                                        },
                                        text = {
                                            Text(
                                                text = label,
                                                fontWeight = if (selectedPeriodKey == monthKey) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            onSelectedPeriodKeyChange(monthKey)
                                            showPeriodDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                val effKey = if (selectedPeriodKey == "CURRENT_MONTH") currentMonthKey else selectedPeriodKey
                                try {
                                    val cal = Calendar.getInstance().apply {
                                        time = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(effKey) ?: Date()
                                        add(Calendar.MONTH, 1)
                                    }
                                    val nextKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
                                    onSelectedPeriodKeyChange(if (nextKey == currentMonthKey) "CURRENT_MONTH" else nextKey)
                                } catch (_: Exception) {
                                    onSelectedPeriodKeyChange("CURRENT_MONTH")
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Month",
                                tint = Emerald700
                            )
                        }
                    }
                }
                "YEAR" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val curY = selectedYearKey.toIntOrNull() ?: currentYearKey.toInt()
                                onSelectedYearKeyChange((curY - 1).toString())
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Year",
                                tint = Emerald700
                            )
                        }

                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Emerald700.copy(alpha = 0.09f),
                                border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showYearDropdown = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Localization.trFormat(lang, "Year %1\$s", "Έτος %1\$s", selectedYearKey),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showYearDropdown,
                                onDismissRequest = { showYearDropdown = false }
                            ) {
                                availableYears.forEach { year ->
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.DateRange, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                                        },
                                        text = {
                                            Text(
                                                text = Localization.trFormat(lang, "Year %1\$s", "Έτος %1\$s", year),
                                                fontWeight = if (selectedYearKey == year) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            onSelectedYearKeyChange(year)
                                            showYearDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                val curY = selectedYearKey.toIntOrNull() ?: currentYearKey.toInt()
                                onSelectedYearKeyChange((curY + 1).toString())
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Year",
                                tint = Emerald700
                            )
                        }
                    }
                }
                "ALL" -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Emerald700.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AllInclusive,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.trFormat(lang, "%1\$d expenses • %2\$d incomes", "%1\$d έξοδα • %2\$d έσοδα", periodExpensesCount, periodIncomesCount),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Emerald700
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AnalyticsSummaryKpiCard(
    activePeriodDisplay: String,
    activeDrillDownPeriodKey: String,
    totalIncome: Double,
    totalSpend: Double,
    netBalance: Double,
    savingsRate: Double,
    periodIncomesCount: Int,
    periodExpensesCount: Int,
    dailyAverageExpense: Double,
    peakSpendingDayInfo: Triple<String, String, Double>?,
    lang: Localization.Language,
    onDrillDownToHome: (HomeDrillDownRequest) -> Unit
) {
    val currencySymbol = LocalCurrencySymbol.current
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            AnalyticsSectionHeader(
                icon = Icons.Default.PieChart,
                iconTint = Emerald700,
                iconContainerColor = Emerald100,
                title = Localization.statisticalAnalysis(lang)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onDrillDownToHome(
                                HomeDrillDownRequest(
                                    periodKey = activeDrillDownPeriodKey,
                                    typeFilter = "INCOME"
                                )
                            )
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.28f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp)) {
                        Text(Localization.incomes(lang), fontSize = 11.sp, color = Color(0xFF047857), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "+${String.format(Locale.US, "%.2f", totalIncome)} $currencySymbol",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF059669)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Localization.entriesCount(periodIncomesCount, lang),
                            fontSize = 9.sp,
                            maxLines = 1,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onDrillDownToHome(
                                HomeDrillDownRequest(
                                    periodKey = activeDrillDownPeriodKey,
                                    typeFilter = "EXPENSE"
                                )
                            )
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.22f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp)) {
                        Text(Localization.expenses(lang), fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.2f", totalSpend)} $currencySymbol",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Localization.transactionsCount(periodExpensesCount, lang),
                            fontSize = 9.sp,
                            maxLines = 1,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = (if (netBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, (if (netBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.28f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp)) {
                        Text(Localization.netBalanceTitle(lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${String.format(Locale.US, "%s%.2f", if (netBalance >= 0) "+" else "", netBalance)} $currencySymbol",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (netBalance >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (totalIncome > 0) Localization.savingsRateLabel(savingsRate.toInt(), lang) else Localization.balanceLabel(lang),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            color = if (netBalance >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald700.copy(alpha = 0.12f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Localization.dailyAverageTitle(lang),
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            minLines = 2,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.2f", dailyAverageExpense)} $currencySymbol",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                val savingsColor = if (netBalance >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = savingsColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, savingsColor.copy(alpha = 0.22f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = savingsColor.copy(alpha = 0.12f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = savingsColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Localization.savingsRatioTitle(lang),
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            minLines = 2,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (totalIncome > 0) String.format(Locale.US, "%s%.1f%%", if (savingsRate >= 0) "+" else "", savingsRate) else "0.0%",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = savingsColor,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (peakSpendingDayInfo != null) {
                                Modifier.clickable {
                                    onDrillDownToHome(
                                        HomeDrillDownRequest(
                                            periodKey = activeDrillDownPeriodKey,
                                            typeFilter = "EXPENSE",
                                            specialFilter = AnalyticsSpecialFilter(
                                                badgeLabel = "${Localization.peakDayTitle(lang)}: ${peakSpendingDayInfo.second}",
                                                exactDayKey = peakSpendingDayInfo.first
                                            )
                                        )
                                    )
                                }
                            } else {
                                Modifier
                            }
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEA580C).copy(alpha = 0.12f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Localization.peakDayTitle(lang),
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            minLines = 2,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = peakSpendingDayInfo?.let { "${String.format(Locale.US, "%.0f", it.third)} $currencySymbol" } ?: "-",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = peakSpendingDayInfo?.second ?: Localization.noPeakDay(lang),
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
