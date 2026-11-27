package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Expense
import com.example.data.entity.Income
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ExpensesEmptyStateCard(
    isIncomeMode: Boolean,
    lang: Localization.Language,
    onAddManualIncome: () -> Unit,
    onAddManualExpense: () -> Unit
) {
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
                    Localization.tr(lang, "No incomes found", "Δεν βρέθηκαν έσοδα")
                } else {
                    Localization.noExpensesFound(lang)
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
                            Localization.tr(lang, "Add Income", "Καταχώρηση Εσόδου")
                        } else {
                            Localization.tr(lang, "Add Expense", "Καταχώρηση Εξόδου")
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

internal enum class ExpenseSwipeAction { NONE, DELETE, DUPLICATE }

internal object ExpenseSwipePolicy {
    // Must drag at least 72% across the card width (nearly edge-to-edge); velocity flicks NEVER trigger.
    const val TRIGGER_WIDTH_FRACTION = 0.72f
    const val HORIZONTAL_LOCK_RATIO = 2.5f
    const val VERTICAL_CANCEL_DP = 12f
    const val HORIZONTAL_START_SLOP_DP = 28f

    fun evaluateAction(
        offsetPx: Float,
        cardWidthPx: Float,
        canDuplicate: Boolean,
        wasCancelled: Boolean
    ): ExpenseSwipeAction {
        if (wasCancelled || cardWidthPx <= 0f) return ExpenseSwipeAction.NONE
        val requiredPx = cardWidthPx * TRIGGER_WIDTH_FRACTION
        return when {
            offsetPx <= -requiredPx -> ExpenseSwipeAction.DELETE
            canDuplicate && offsetPx >= requiredPx -> ExpenseSwipeAction.DUPLICATE
            else -> ExpenseSwipeAction.NONE
        }
    }
}

@Composable
internal fun ExpenseCardItem(
    item: ActivityItem.ExpenseItem,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onSwipeDeleteExpense: ((Expense) -> Unit)? = null,
    onDuplicateExpense: ((Expense) -> Unit)? = null,
    lang: Localization.Language = Localization.Language.GREEK,
    modifier: Modifier = Modifier
) {
    val expense = item.expense
    val currentExpense by rememberUpdatedState(expense)
    val currentOnSwipeDelete by rememberUpdatedState(onSwipeDeleteExpense ?: onDeleteExpense)
    val currentOnDuplicate by rememberUpdatedState(onDuplicateExpense)
    val canDuplicate by rememberUpdatedState(onDuplicateExpense != null)

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val horizontalStartSlopPx = with(density) { ExpenseSwipePolicy.HORIZONTAL_START_SLOP_DP.dp.toPx() }
    val verticalCancelSlopPx = with(density) { ExpenseSwipePolicy.VERTICAL_CANCEL_DP.dp.toPx() }

    var cardWidthPx by remember { mutableFloatStateOf(0f) }
    var rawOffsetPx by remember(expense.id) { mutableFloatStateOf(0f) }
    var isHorizontalDragging by remember(expense.id) { mutableStateOf(false) }

    val animatedOffsetPx by animateFloatAsState(
        targetValue = rawOffsetPx,
        animationSpec = if (isHorizontalDragging) snap() else spring(stiffness = Spring.StiffnessMediumLow),
        label = "expense_swipe_offset"
    )

    val triggerThresholdPx = (cardWidthPx * ExpenseSwipePolicy.TRIGGER_WIDTH_FRACTION).coerceAtLeast(1f)
    val swipeProgress = (abs(animatedOffsetPx) / triggerThresholdPx).coerceIn(0f, 1f)
    val isRepeat = animatedOffsetPx > 1f
    val isDelete = animatedOffsetPx < -1f
    val thresholdReached = cardWidthPx > 0f && abs(animatedOffsetPx) >= triggerThresholdPx

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .onSizeChanged { size -> cardWidthPx = size.width.toFloat() }
            .pointerInput(expense.id, horizontalStartSlopPx, verticalCancelSlopPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalDx = 0f
                    var totalDy = 0f
                    var lockedHorizontal = false
                    var hapticFired = false
                    var cancelled = false
                    var finalOffset = 0f

                    try {
                        while (true) {
                            val pass = if (lockedHorizontal) PointerEventPass.Initial else PointerEventPass.Main
                            val event = awaitPointerEvent(pass = pass)
                            if (event.changes.count { it.pressed } > 1) {
                                cancelled = true
                                break
                            }

                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                                ?: break

                            if (!change.pressed) {
                                if (change.isConsumed) {
                                    cancelled = true
                                } else if (lockedHorizontal) {
                                    // Prevent child clickable / IconButton from firing on swipe release
                                    change.consume()
                                }
                                break
                            }

                            val dx = change.position.x - change.previousPosition.x
                            val dy = change.position.y - change.previousPosition.y

                            if (!lockedHorizontal) {
                                if (change.isConsumed) {
                                    cancelled = true
                                    break
                                }
                                totalDx += dx
                                totalDy += dy

                                // Yield immediately to vertical LazyColumn scrolling
                                if (abs(totalDy) > verticalCancelSlopPx) {
                                    cancelled = true
                                    break
                                }

                                if (abs(totalDx) >= horizontalStartSlopPx) {
                                    val isStrictlyHorizontal =
                                        abs(totalDx) >= abs(totalDy) * ExpenseSwipePolicy.HORIZONTAL_LOCK_RATIO
                                    val directionAllowed = totalDx < 0f || canDuplicate
                                    if (isStrictlyHorizontal && directionAllowed) {
                                        lockedHorizontal = true
                                        isHorizontalDragging = true
                                        change.consume()
                                        val minOffset = -cardWidthPx
                                        val maxOffset = if (canDuplicate) cardWidthPx else 0f
                                        // Start smoothly from 0 so the card never jumps
                                        rawOffsetPx = dx.coerceIn(minOffset, maxOffset)
                                    } else {
                                        cancelled = true
                                        break
                                    }
                                }
                            } else {
                                if (dx != 0f || dy != 0f) {
                                    change.consume()
                                }
                                val minOffset = -cardWidthPx
                                val maxOffset = if (canDuplicate) cardWidthPx else 0f
                                val nextOffset = (rawOffsetPx + dx).coerceIn(minOffset, maxOffset)
                                rawOffsetPx = nextOffset

                                val reachedNow = cardWidthPx > 0f &&
                                    abs(nextOffset) >= cardWidthPx * ExpenseSwipePolicy.TRIGGER_WIDTH_FRACTION
                                if (reachedNow && !hapticFired) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    hapticFired = true
                                } else if (!reachedNow) {
                                    hapticFired = false
                                }
                            }
                        }
                        finalOffset = rawOffsetPx
                    } finally {
                        isHorizontalDragging = false
                        rawOffsetPx = 0f
                    }

                    if (lockedHorizontal && !cancelled) {
                        when (ExpenseSwipePolicy.evaluateAction(finalOffset, cardWidthPx, canDuplicate, wasCancelled = false)) {
                            ExpenseSwipeAction.DELETE -> currentOnSwipeDelete(currentExpense)
                            ExpenseSwipeAction.DUPLICATE -> currentOnDuplicate?.invoke(currentExpense)
                            ExpenseSwipeAction.NONE -> Unit
                        }
                    }
                }
            }
    ) {
        // Reveal background strictly while actively swiped horizontally
        if (isRepeat || isDelete) {
            val baseAlpha = if (thresholdReached) 0.24f else (0.08f + 0.12f * swipeProgress)
            val bgColor = when {
                isRepeat -> Emerald700.copy(alpha = baseAlpha)
                else -> Color(0xFFEF4444).copy(alpha = baseAlpha)
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (isRepeat) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (isRepeat) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = Localization.tr(lang, "Repeat today", "Επανάληψη σήμερα"),
                            tint = Emerald700.copy(alpha = if (thresholdReached) 1f else 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = Localization.tr(lang, "Repeat today", "Επανάληψη σήμερα"),
                            color = Emerald700.copy(alpha = if (thresholdReached) 1f else 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = Localization.delete(lang),
                            color = Color(0xFFEF4444).copy(alpha = if (thresholdReached) 1f else 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = Localization.delete(lang),
                            tint = Color(0xFFEF4444).copy(alpha = if (thresholdReached) 1f else 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(animatedOffsetPx.roundToInt(), 0) }
            .clickable { onEditExpense(expense) }
            .testTag("expense_card_${expense.id}")
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
                        if (item.formattedDate.isNotBlank()) {
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
}

@Composable
internal fun IncomeCardItem(
    item: ActivityItem.IncomeItem,
    onEditIncome: (Income) -> Unit,
    onDeleteIncome: (Income) -> Unit,
    modifier: Modifier = Modifier
) {
    val income = item.income
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
        tonalElevation = 1.dp,
        modifier = modifier
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
                        if (item.formattedDate.isNotBlank()) {
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
