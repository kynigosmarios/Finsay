package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.FamilyMember
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ManualExpensePayerAndSplitSection(
    lang: Localization.Language,
    members: List<FamilyMember>,
    selectedPayerIds: SnapshotStateList<Long>,
    isCustomPayerSplit: Boolean,
    onCustomPayerSplitChange: (Boolean) -> Unit,
    customPayerAmounts: SnapshotStateMap<Long, String>,
    currentAmount: Double,
    isAllFamily: Boolean,
    onIsAllFamilyChange: (Boolean) -> Unit,
    selectedMemberIds: SnapshotStateList<Long>,
    splitPerPerson: Double,
    payerError: String? = null,
    beneficiaryError: String? = null,
    onPayerSelectionChanged: () -> Unit = {},
    onBeneficiarySelectionChanged: () -> Unit = {}
) {
    val currencySymbol = com.example.util.LocalCurrencySymbol.current
    // 1. Payer Section (Who paid)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.CreditCard,
            contentDescription = null,
            tint = if (payerError != null) MaterialTheme.colorScheme.error else Emerald700,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = Localization.payersSectionTitle(lang),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (payerError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
    Spacer(modifier = Modifier.height(6.dp))

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        members.forEach { member ->
            val isPayer = selectedPayerIds.contains(member.id)
            val avatarColor = Color(member.avatarColorHex)

            FilterChip(
                selected = isPayer,
                onClick = {
                    if (isPayer) {
                        selectedPayerIds.remove(member.id)
                        customPayerAmounts.remove(member.id)
                    } else {
                        selectedPayerIds.add(member.id)
                        if (isCustomPayerSplit) {
                            val currentAllocated = selectedPayerIds.sumOf { customPayerAmounts[it]?.toDoubleOrNull() ?: 0.0 }
                            val remaining = (currentAmount - currentAllocated).coerceAtLeast(0.0)
                            customPayerAmounts[member.id] = if (remaining > 0) String.format(Locale.US, "%.2f", remaining) else "0.00"
                        }
                    }
                    onPayerSelectionChanged()
                },
                label = { Text(member.name, fontSize = 12.sp) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(avatarColor)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Emerald700.copy(alpha = 0.2f),
                    selectedLabelColor = Emerald700
                )
            )
        }
    }

    if (payerError != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = payerError,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp
        )
    }

    // Multi-payer allocation details card
    AnimatedVisibility(
        visible = selectedPayerIds.size > 1,
        enter = FinsayMotion.ExpandEnter,
        exit = FinsayMotion.ShrinkExit
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                val equalTabBg by animateColorAsState(
                    targetValue = if (!isCustomPayerSplit) Emerald700 else Color.Transparent,
                    animationSpec = tween(200),
                    label = "equalTabBg"
                )
                val customTabBg by animateColorAsState(
                    targetValue = if (isCustomPayerSplit) Emerald700 else Color.Transparent,
                    animationSpec = tween(200),
                    label = "customTabBg"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(equalTabBg)
                            .clickable { onCustomPayerSplitChange(false) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Localization.payerSplitModeEqual(lang),
                            fontSize = 12.5.sp,
                            fontWeight = if (!isCustomPayerSplit) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isCustomPayerSplit) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(customTabBg)
                            .clickable {
                                onCustomPayerSplitChange(true)
                                val share = if (currentAmount > 0) currentAmount / selectedPayerIds.size else 0.0
                                selectedPayerIds.forEach { id ->
                                    if (customPayerAmounts[id].isNullOrBlank()) {
                                        customPayerAmounts[id] = String.format(Locale.US, "%.2f", share)
                                    }
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Localization.payerSplitModeCustom(lang),
                            fontSize = 12.5.sp,
                            fontWeight = if (isCustomPayerSplit) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCustomPayerSplit) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isCustomPayerSplit) {
                    val equalShare = if (currentAmount > 0) currentAmount / selectedPayerIds.size else 0.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Emerald700.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Share per person" else "Μερίδιο ανά άτομο",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", equalShare)} $currencySymbol",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald700
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedPayerIds.forEach { pid ->
                            val m = members.find { it.id == pid } ?: return@forEach
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(m.avatarColorHex))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = m.name,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = " (${Localization.translateRole(m.role, lang)})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", equalShare)} $currencySymbol",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Emerald700
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedPayerIds.forEach { pid ->
                            val m = members.find { it.id == pid } ?: return@forEach
                            val currentVal = customPayerAmounts[pid] ?: ""

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(m.avatarColorHex))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = m.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = " (${Localization.translateRole(m.role, lang)})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                OutlinedTextField(
                                    value = currentVal,
                                    onValueChange = { input ->
                                        customPayerAmounts[pid] = input.replace(',', '.')
                                    },
                                    modifier = Modifier.width(115.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    trailingIcon = {
                                        Text(currencySymbol, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Emerald700,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }

                        val totalAllocated = selectedPayerIds.sumOf { customPayerAmounts[it]?.toDoubleOrNull() ?: 0.0 }
                        val diff = currentAmount - totalAllocated

                        Spacer(modifier = Modifier.height(8.dp))

                        if (currentAmount > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    Math.abs(diff) < 0.01 -> Emerald700.copy(alpha = 0.08f)
                                    diff > 0.01 -> Amber700.copy(alpha = 0.08f)
                                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (Math.abs(diff) < 0.01) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Emerald700, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = Localization.payerFullyCovered(lang),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald700
                                            )
                                        }
                                    } else if (diff > 0.01) {
                                        Text(
                                            text = Localization.payerRemaining(lang, "${String.format(Locale.US, "%.2f", diff)} $currencySymbol"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Amber700
                                        )

                                        if (selectedPayerIds.size == 2) {
                                            val targetId = selectedPayerIds[1]
                                            val targetMember = members.find { it.id == targetId }
                                            if (targetMember != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Amber700.copy(alpha = 0.12f),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            val cur = customPayerAmounts[targetId]?.toDoubleOrNull() ?: 0.0
                                                            customPayerAmounts[targetId] = String.format(Locale.US, "%.2f", cur + diff)
                                                        }
                                                ) {
                                                    Text(
                                                        text = Localization.payerQuickBalance(lang, targetMember.name, "${String.format(Locale.US, "%.2f", diff)} $currencySymbol"),
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Amber700,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = Localization.payerExceeded(lang, "${String.format(Locale.US, "%.2f", -diff)} $currencySymbol"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", totalAllocated)} / ${String.format(Locale.US, "%.2f", currentAmount)} $currencySymbol",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
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

    Spacer(modifier = Modifier.height(16.dp))

    // 2. Beneficiary Section
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = Color(0xFF2563EB),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (lang == Localization.Language.ENGLISH) "Who is this expense for?" else "Σε ποιον αφορά το έξοδο;",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (beneficiaryError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
    Spacer(modifier = Modifier.height(6.dp))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isAllFamily) Emerald700.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onIsAllFamilyChange(!isAllFamily)
                onBeneficiarySelectionChanged()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isAllFamily,
                onCheckedChange = {
                    onIsAllFamilyChange(it)
                    onBeneficiarySelectionChanged()
                }
            )
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = Emerald700,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (lang == Localization.Language.ENGLISH) "All members" else "Όλα τα μέλη",
                fontSize = 13.sp,
                fontWeight = if (isAllFamily) FontWeight.Bold else FontWeight.Normal
            )
        }
    }

    AnimatedVisibility(
        visible = !isAllFamily,
        enter = FinsayMotion.ExpandEnter,
        exit = FinsayMotion.ShrinkExit
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                members.forEach { member ->
                    val isChecked = selectedMemberIds.contains(member.id)
                    val avatarColor = Color(member.avatarColorHex)

                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            if (isChecked) {
                                selectedMemberIds.remove(member.id)
                            } else {
                                selectedMemberIds.add(member.id)
                            }
                            onBeneficiarySelectionChanged()
                        },
                        label = { Text(member.name, fontSize = 12.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = avatarColor.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }

    if (beneficiaryError != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = beneficiaryError,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp
        )
    }

    if (currentAmount > 0) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Emerald700.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (lang == Localization.Language.ENGLISH) "Per person" else "Ανά άτομο",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format(Locale.US, "%.2f", splitPerPerson)} $currencySymbol",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald700
                )
            }
        }
    }
}
