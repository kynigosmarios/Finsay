package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Expense
import com.example.data.entity.FamilyMember
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald700
import com.example.util.AnalyticsExpenseFilterHelper
import com.example.util.AnalyticsSpecialFilter
import com.example.util.HomeDrillDownRequest
import com.example.util.Localization
import com.example.util.SettleUpCalculator
import com.example.util.SettlementTransfer
import java.util.Locale

@Composable
internal fun AnalyticsSettleUpCard(
    periodExpenses: List<Expense>,
    members: List<FamilyMember>,
    activeDrillDownPeriodKey: String,
    settlePeriodTimestamp: Long,
    lang: Localization.Language,
    isGreek: Boolean,
    onDrillDownToHome: (HomeDrillDownRequest) -> Unit,
    onSettleTransfer: (FamilyMember, FamilyMember, Double, Long) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val settleUpResult = remember(periodExpenses, members) {
        SettleUpCalculator.calculate(periodExpenses, members)
    }
    var pendingSettlement by remember { mutableStateOf<SettlementTransfer?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settle_up_analytics_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            AnalyticsSectionHeader(
                icon = Icons.Default.SwapHoriz,
                iconTint = MaterialTheme.colorScheme.primary,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                title = if (isGreek) "Εκκαθάριση Εξόδων" else "Expense Settle-Up",
                badge = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Emerald700.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (settleUpResult.isBalanced) (if (isGreek) "ΙΣΟΣΚΕΛΙΣΜΕΝΟ" else "BALANCED") else "${settleUpResult.transfers.size} ${if (isGreek) "μεταφορές" else "transfers"}",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (settleUpResult.isBalanced) Emerald700 else Amber700,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            if (settleUpResult.transfers.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald700.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isGreek) "✅ Όλα τα μερίδια είναι τακτοποιημένα!" else "✅ All member shares are balanced!",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Emerald700,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    settleUpResult.transfers.forEach { t ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Amber700.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Amber700.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val hasDirectPairExpenses = periodExpenses.any { exp ->
                                        AnalyticsExpenseFilterHelper.matchesPayer(exp, t.toMember.id, members) &&
                                            AnalyticsExpenseFilterHelper.resolveSettleUpBeneficiaries(exp, members)
                                                .any { it.id == t.fromMember.id }
                                    }
                                    onDrillDownToHome(
                                        HomeDrillDownRequest(
                                            periodKey = activeDrillDownPeriodKey,
                                            typeFilter = "EXPENSE",
                                            specialFilter = if (hasDirectPairExpenses) {
                                                AnalyticsSpecialFilter(
                                                    badgeLabel = "${t.toMember.name} ➔ ${t.fromMember.name}",
                                                    payerMemberId = t.toMember.id,
                                                    debtorMemberId = t.fromMember.id
                                                )
                                            } else {
                                                AnalyticsSpecialFilter(
                                                    badgeLabel = "${if (isGreek) "Πλήρωσε" else "Paid by"}: ${t.toMember.name}",
                                                    payerMemberId = t.toMember.id
                                                )
                                            }
                                        )
                                    )
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "👉 ${t.fromMember.name} ➔ ${t.toMember.name}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    Text(
                                        text = String.format(Locale.GERMANY, "%.2f €", t.amount),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Amber700
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    FilledTonalButton(
                                        onClick = { pendingSettlement = t },
                                        shape = RoundedCornerShape(9.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Emerald700.copy(alpha = 0.14f),
                                            contentColor = Emerald700
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 34.dp)
                                            .testTag("settle_now_${t.fromMember.id}_${t.toMember.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = Localization.settleNowAction(lang),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isGreek) "Ανάλυση ανά Μέλος:" else "Per-Member Breakdown:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            settleUpResult.memberSummaries.forEach { s ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onDrillDownToHome(
                                HomeDrillDownRequest(
                                    periodKey = activeDrillDownPeriodKey,
                                    typeFilter = "EXPENSE",
                                    specialFilter = AnalyticsSpecialFilter(
                                        badgeLabel = "${if (isGreek) "Πλήρωσε" else "Paid by"}: ${s.member.name}",
                                        payerMemberId = s.member.id
                                    )
                                )
                            )
                        }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(s.member.avatarColorHex),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = s.member.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${if (isGreek) "Πλ." else "Paid"} ${String.format(Locale.GERMANY, "%.0f€", s.totalPaid)} • ${if (isGreek) "Μερ." else "Share"} ${String.format(Locale.GERMANY, "%.0f€", s.totalShare)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val isPositive = s.netBalance > 0.01
                        val isNegative = s.netBalance < -0.01
                        Text(
                            text = when {
                                isPositive -> "+${String.format(Locale.GERMANY, "%.2f €", s.netBalance)}"
                                isNegative -> "-${String.format(Locale.GERMANY, "%.2f €", kotlin.math.abs(s.netBalance))}"
                                else -> "0,00 €"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isPositive -> Emerald700
                                isNegative -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    val text = SettleUpCalculator.formatSummaryText(settleUpResult, if (isGreek) "Εξοδολόγιο" else "Household", isGreek)
                    clipboardManager.setText(AnnotatedString(text))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("copy_settle_up_analytics_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isGreek) "Αντιγραφή" else "Copy",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    pendingSettlement?.let { targetTransfer ->
        val formattedAmt = String.format(Locale.US, "%.2f €", targetTransfer.amount)
        AlertDialog(
            onDismissRequest = { pendingSettlement = null },
            title = {
                Text(
                    text = Localization.settleConfirmTitle(lang),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = Localization.settleConfirmMessage(
                        lang = lang,
                        from = targetTransfer.fromMember.name,
                        to = targetTransfer.toMember.name,
                        formattedAmount = formattedAmt
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSettleTransfer(
                            targetTransfer.fromMember,
                            targetTransfer.toMember,
                            targetTransfer.amount,
                            settlePeriodTimestamp
                        )
                        pendingSettlement = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                    modifier = Modifier.testTag("confirm_settle_transfer_button")
                ) {
                    Text(Localization.settleNowAction(lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSettlement = null }) {
                    Text(Localization.cancel(lang))
                }
            }
        )
    }
}
