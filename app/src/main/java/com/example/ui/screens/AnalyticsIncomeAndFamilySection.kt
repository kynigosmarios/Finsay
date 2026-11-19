package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.ui.theme.Emerald700
import com.example.util.AnalyticsSpecialFilter
import com.example.util.HomeDrillDownRequest
import com.example.util.Localization
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AnalyticsFamilyIncomeCard(
    periodIncomes: List<Income>,
    members: List<FamilyMember>,
    memberIncomeMap: Map<FamilyMember, Double>,
    unassignedMemberIncomes: Double,
    categoryIncomeMap: List<Pair<String, Double>>,
    totalIncome: Double,
    chartTypeIncome: Int,
    activeDrillDownPeriodKey: String,
    appLanguage: String,
    lang: Localization.Language,
    onSelectChartTypeIncome: (Int) -> Unit,
    onDrillDownToHome: (HomeDrillDownRequest) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Localization.familyIncome(lang),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                ChartTypeToggle(
                    selectedType = chartTypeIncome,
                    onSelectType = onSelectChartTypeIncome,
                    activeColor = Color(0xFF059669),
                    appLanguage = appLanguage
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (periodIncomes.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Localization.noIncomesYet(lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Localization.registerIncomeHint(lang),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val safeTotalIncome = totalIncome.coerceAtLeast(1.0)

                if (chartTypeIncome == 1) {
                    val incomeSlices = members.mapNotNull { m ->
                        val inc = memberIncomeMap[m] ?: 0.0
                        if (inc > 0.001) {
                            PieChartSlice(
                                label = m.name,
                                value = inc,
                                color = Color(m.avatarColorHex),
                                onClick = {
                                    onDrillDownToHome(
                                        HomeDrillDownRequest(
                                            periodKey = activeDrillDownPeriodKey,
                                            typeFilter = "INCOME",
                                            memberId = m.id,
                                            specialFilter = AnalyticsSpecialFilter(
                                                badgeLabel = "${Localization.income(lang)}: ${m.name}",
                                                exactMemberId = m.id
                                            )
                                        )
                                    )
                                }
                            )
                        } else null
                    } + if (unassignedMemberIncomes > 0.001) {
                        listOf(
                            PieChartSlice(
                                label = Localization.commonOther(lang),
                                value = unassignedMemberIncomes,
                                color = Color(0xFF10B981),
                                onClick = {
                                    onDrillDownToHome(
                                        HomeDrillDownRequest(
                                            periodKey = activeDrillDownPeriodKey,
                                            typeFilter = "INCOME",
                                            memberId = -1L,
                                            specialFilter = AnalyticsSpecialFilter(
                                                badgeLabel = Localization.commonOtherIncome(lang),
                                                exactMemberId = -1L
                                            )
                                        )
                                    )
                                }
                            )
                        )
                    } else emptyList()

                    PieChartView(
                        slices = incomeSlices,
                        totalAmount = totalIncome,
                        appLanguage = appLanguage
                    )
                } else {
                    val maxIncome = (memberIncomeMap.values + unassignedMemberIncomes).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

                    members.forEach { member ->
                        val inc = memberIncomeMap[member] ?: 0.0
                        val percent = ((inc / safeTotalIncome) * 100).toInt()
                        val progress = (inc / maxIncome).toFloat().coerceIn(0f, 1f)
                        val avatarColor = Color(member.avatarColorHex)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onDrillDownToHome(
                                        HomeDrillDownRequest(
                                            periodKey = activeDrillDownPeriodKey,
                                            typeFilter = "INCOME",
                                            memberId = member.id,
                                            specialFilter = AnalyticsSpecialFilter(
                                                badgeLabel = "${Localization.income(lang)}: ${member.name}",
                                                exactMemberId = member.id
                                            )
                                        )
                                    )
                                }
                                .padding(vertical = 6.dp, horizontal = 2.dp)
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
                                        Text(
                                            text = member.name,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${Localization.translateRole(member.role, lang)} • ${Localization.percentOfTotalIncome(percent, lang)}",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = String.format(Locale.US, "+%.2f €", inc),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (inc > 0) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (inc > 0) avatarColor else Color.Transparent,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    if (unassignedMemberIncomes > 0.01) {
                        val unassignedPct = ((unassignedMemberIncomes / safeTotalIncome) * 100).toInt()
                        val unassignedProgress = (unassignedMemberIncomes / maxIncome).toFloat().coerceIn(0f, 1f)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onDrillDownToHome(
                                        HomeDrillDownRequest(
                                            periodKey = activeDrillDownPeriodKey,
                                            typeFilter = "INCOME",
                                            memberId = -1L,
                                            specialFilter = AnalyticsSpecialFilter(
                                                badgeLabel = Localization.commonOtherIncome(lang),
                                                exactMemberId = -1L
                                            )
                                        )
                                    )
                                }
                                .padding(vertical = 6.dp, horizontal = 2.dp)
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
                                            .background(Color(0xFF10B981)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = Localization.commonOtherIncome(lang),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = Localization.unassignedMemberIncome(unassignedPct, lang),
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = String.format(Locale.US, "+%.2f €", unassignedMemberIncomes),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF059669)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { unassignedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = Color(0xFF10B981),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                if (categoryIncomeMap.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = Localization.incomeSourcesByCategory(lang),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryIncomeMap.forEach { (catName, amount) ->
                            val catPct = ((amount / safeTotalIncome) * 100).toInt()
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF059669).copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onDrillDownToHome(
                                            HomeDrillDownRequest(
                                                periodKey = activeDrillDownPeriodKey,
                                                typeFilter = "INCOME",
                                                specialFilter = AnalyticsSpecialFilter(
                                                    badgeLabel = "${Localization.income(lang)}: ${Localization.translateCategory(catName, lang)}",
                                                    incomeCategory = catName
                                                )
                                            )
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = Localization.translateCategory(catName, lang),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = String.format(Locale.US, "+%.2f€ (%d%%)", amount, catPct),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF059669)
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
internal fun AnalyticsMemberDeficitAlertCard(
    membersWithDeficit: List<Triple<FamilyMember, Double, Double>>,
    lang: Localization.Language
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("members_deficit_alert_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = Localization.memberIncomeDeficitTitle(lang),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                membersWithDeficit.forEach { (m, paid, income) ->
                    val deficit = paid - income
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(m.avatarColorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = m.name.take(1),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = m.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = "Έσοδα: ${String.format(Locale.US, "%.1f€", income)} • Έξοδα: ${String.format(Locale.US, "%.1f€", paid)}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDC2626)
                            ) {
                                Text(
                                    text = "-${String.format(Locale.US, "%.2f €", deficit)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
internal fun AnalyticsFamilyVsKidsCard(
    parentsTotalSpend: Double,
    kidsSpend: Double,
    totalSpend: Double,
    chartTypeFamily: Int,
    activeDrillDownPeriodKey: String,
    appLanguage: String,
    lang: Localization.Language,
    onSelectChartTypeFamily: (Int) -> Unit,
    onDrillDownToHome: (HomeDrillDownRequest) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.12f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Localization.expenseDistribution(lang),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                ChartTypeToggle(
                    selectedType = chartTypeFamily,
                    onSelectType = onSelectChartTypeFamily,
                    activeColor = Emerald700,
                    appLanguage = appLanguage
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val safeTotal = totalSpend.coerceAtLeast(1.0)
            val parentsPercent = ((parentsTotalSpend / safeTotal) * 100).toInt()
            val kidsPercent = ((kidsSpend / safeTotal) * 100).toInt()

            if (chartTypeFamily == 1) {
                PieChartView(
                    slices = listOf(
                        PieChartSlice(
                            label = Localization.parents(lang),
                            value = parentsTotalSpend,
                            color = Color(0xFF0284C7),
                            onClick = {
                                onDrillDownToHome(
                                    HomeDrillDownRequest(
                                        periodKey = activeDrillDownPeriodKey,
                                        typeFilter = "EXPENSE",
                                        specialFilter = AnalyticsSpecialFilter(
                                            badgeLabel = Localization.parents(lang),
                                            roleGroupFilter = "PARENTS"
                                        )
                                    )
                                )
                            }
                        ),
                        PieChartSlice(
                            label = Localization.children(lang),
                            value = kidsSpend,
                            color = Color(0xFF10B981),
                            onClick = {
                                onDrillDownToHome(
                                    HomeDrillDownRequest(
                                        periodKey = activeDrillDownPeriodKey,
                                        typeFilter = "EXPENSE",
                                        specialFilter = AnalyticsSpecialFilter(
                                            badgeLabel = Localization.children(lang),
                                            roleGroupFilter = "KIDS"
                                        )
                                    )
                                )
                            }
                        )
                    ),
                    totalAmount = totalSpend,
                    appLanguage = appLanguage
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CircleShape)
                ) {
                    if (parentsTotalSpend > 0) {
                        Box(
                            modifier = Modifier
                                .weight(parentsTotalSpend.toFloat().coerceAtLeast(0.001f))
                                .fillMaxSize()
                                .background(Color(0xFF0284C7))
                                .clickable {
                                    onDrillDownToHome(
                                        HomeDrillDownRequest(
                                            periodKey = activeDrillDownPeriodKey,
                                            typeFilter = "EXPENSE",
                                            specialFilter = AnalyticsSpecialFilter(
                                                badgeLabel = Localization.parents(lang),
                                                roleGroupFilter = "PARENTS"
                                            )
                                        )
                                    )
                                }
                        )
                    }
                    if (kidsSpend > 0) {
                        Box(
                            modifier = Modifier
                                .weight(kidsSpend.toFloat().coerceAtLeast(0.001f))
                                .fillMaxSize()
                                .background(Color(0xFF10B981))
                                .clickable {
                                    onDrillDownToHome(
                                        HomeDrillDownRequest(
                                            periodKey = activeDrillDownPeriodKey,
                                            typeFilter = "EXPENSE",
                                            specialFilter = AnalyticsSpecialFilter(
                                                badgeLabel = Localization.children(lang),
                                                roleGroupFilter = "KIDS"
                                            )
                                        )
                                    )
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onDrillDownToHome(
                                    HomeDrillDownRequest(
                                        periodKey = activeDrillDownPeriodKey,
                                        typeFilter = "EXPENSE",
                                        specialFilter = AnalyticsSpecialFilter(
                                            badgeLabel = Localization.parents(lang),
                                            roleGroupFilter = "PARENTS"
                                        )
                                    )
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0284C7)))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("${Localization.parents(lang)} ($parentsPercent%)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.2f €", parentsTotalSpend),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onDrillDownToHome(
                                    HomeDrillDownRequest(
                                        periodKey = activeDrillDownPeriodKey,
                                        typeFilter = "EXPENSE",
                                        specialFilter = AnalyticsSpecialFilter(
                                            badgeLabel = Localization.children(lang),
                                            roleGroupFilter = "KIDS"
                                        )
                                    )
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("${Localization.children(lang)} ($kidsPercent%)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.2f €", kidsSpend),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AnalyticsParentsForKidsCard(
    kidsMembers: List<FamilyMember>,
    individualSpendMap: Map<FamilyMember, Double>,
    kidsSpend: Double,
    activeDrillDownPeriodKey: String,
    lang: Localization.Language,
    onDrillDownToHome: (HomeDrillDownRequest) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth().testTag("kids_spending_highlight_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Parents' Expenses for Children" else "Έξοδα Γονέων για τα Παιδιά",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF4F46E5).copy(alpha = 0.10f)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.2f €", kidsSpend),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4338CA),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (kidsMembers.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = Localization.noKidsRegisteredHint(lang),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    kidsMembers.forEach { kid ->
                        val kidSpend = individualSpendMap[kid] ?: 0.0
                        val kidPercent = if (kidsSpend > 0) (kidSpend / kidsSpend).toFloat().coerceIn(0f, 1f) else 0f
                        val kidColor = Color(kid.avatarColorHex)

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onDrillDownToHome(
                                        HomeDrillDownRequest(
                                            periodKey = activeDrillDownPeriodKey,
                                            typeFilter = "EXPENSE",
                                            memberId = kid.id,
                                            specialFilter = AnalyticsSpecialFilter(
                                                badgeLabel = kid.name,
                                                exactMemberId = kid.id
                                            )
                                        )
                                    )
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(kidColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = kid.name.take(1),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = kid.name,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (lang == Localization.Language.ENGLISH) "100% Covered by parents" else "🛡️ 100% Κάλυψη από γονείς",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Emerald700
                                            )
                                        }
                                    }

                                    Text(
                                        text = String.format(Locale.US, "%.2f €", kidSpend),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (kidsSpend > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { kidPercent },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(CircleShape),
                                        color = kidColor,
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
