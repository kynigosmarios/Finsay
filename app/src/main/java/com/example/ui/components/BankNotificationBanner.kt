package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.Emerald700
import com.example.util.LocalCurrencySymbol
import com.example.util.PendingBankTransaction
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun BankNotificationBanner(
    transactions: List<PendingBankTransaction>,
    appLanguage: String,
    onConfirmTransaction: (PendingBankTransaction) -> Unit,
    onDismissTransaction: (PendingBankTransaction) -> Unit,
    resolveSuggestedCategory: (PendingBankTransaction) -> String? = { null },
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) return
    val isGreek = appLanguage != "EN"
    val currencySymbol = LocalCurrencySymbol.current

    var selectedIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(transactions.size) {
        if (selectedIndex >= transactions.size) {
            selectedIndex = (transactions.size - 1).coerceAtLeast(0)
        }
    }
    val currentIndex = selectedIndex.coerceIn(0, (transactions.size - 1).coerceAtLeast(0))
    val tx = transactions[currentIndex]

    val currentOnConfirm by rememberUpdatedState(onConfirmTransaction)
    val currentOnDismiss by rememberUpdatedState(onDismissTransaction)

    val bottomStackPadding = when {
        transactions.size >= 3 -> 10.dp
        transactions.size >= 2 -> 5.dp
        else -> 0.dp
    }

    AnimatedVisibility(
        visible = true,
        enter = FinsayMotion.BannerEnter,
        exit = FinsayMotion.BannerExit,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Subtle dismiss hint visible behind the card while swiping left or right
            key(tx.id) {
                val offsetX = remember { Animatable(0f) }
                var cardWidthPx by remember { mutableFloatStateOf(1f) }
                var isDismissing by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                val currentTx by rememberUpdatedState(tx)

                // Stacked deck layers matched cleanly to the foreground card footprint
                if (transactions.size >= 3) {
                    Surface(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(horizontal = 16.dp)
                            .padding(top = 10.dp)
                            .zIndex(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.22f)),
                        shadowElevation = 1.dp
                    ) {}
                }

                if (transactions.size >= 2) {
                    Surface(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(horizontal = 8.dp)
                            .padding(top = 5.dp)
                            .padding(bottom = if (transactions.size >= 3) 5.dp else 0.dp)
                            .zIndex(2f),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.35f)),
                        shadowElevation = 2.dp
                    ) {}
                }

                // Dismiss feedback background while dragging horizontally
                val dragProgress = (abs(offsetX.value) / (cardWidthPx * 0.28f).coerceAtLeast(1f)).coerceIn(0f, 1f)
                if (abs(offsetX.value) > 6f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(bottom = bottomStackPadding)
                            .zIndex(2.5f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f + 0.18f * dragProgress))
                            .padding(horizontal = 20.dp),
                        contentAlignment = if (offsetX.value >= 0f) Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isGreek) "Αγνόηση" else "Ignore",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = bottomStackPadding)
                        .zIndex(3f)
                        .onSizeChanged { cardWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                        .graphicsLayer {
                            translationX = offsetX.value
                            rotationZ = (offsetX.value / 48f).coerceIn(-5f, 5f)
                            alpha = (1f - (abs(offsetX.value) / (cardWidthPx * 1.15f)) * 0.55f).coerceIn(0.35f, 1f)
                        }
                        .draggable(
                            orientation = Orientation.Horizontal,
                            enabled = !isDismissing,
                            state = rememberDraggableState { delta ->
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + delta)
                                }
                            },
                            onDragStopped = { velocity ->
                                scope.launch {
                                    val dismissThreshold = cardWidthPx * 0.28f
                                    val matchesVelocityDirection = offsetX.value == 0f || (velocity * offsetX.value) >= 0f
                                    val shouldDismiss = abs(offsetX.value) > dismissThreshold ||
                                        (abs(velocity) > 850f && matchesVelocityDirection)

                                    if (shouldDismiss) {
                                        isDismissing = true
                                        val direction = when {
                                            abs(offsetX.value) > 12f -> if (offsetX.value > 0f) 1f else -1f
                                            velocity >= 0f -> 1f
                                            else -> -1f
                                        }
                                        offsetX.animateTo(
                                            targetValue = direction * cardWidthPx * 1.2f,
                                            animationSpec = tween(durationMillis = 180)
                                        )
                                        currentOnDismiss(currentTx)
                                    } else {
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            }
                        )
                        .testTag("bank_notification_banner"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, Emerald700.copy(alpha = 0.7f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        // Top Row: Icon + Title | Counter Pill (if >1) + Close X
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Emerald700.copy(alpha = 0.14f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier
                                        .padding(7.dp)
                                        .size(18.dp)
                                )
                            }

                            Text(
                                text = if (isGreek) "Εντοπίστηκε Συναλλαγή" else "Transaction Detected",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (transactions.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Emerald700.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.30f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Previous",
                                            tint = if (currentIndex > 0) Emerald700 else Emerald700.copy(alpha = 0.3f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable(enabled = currentIndex > 0) {
                                                    selectedIndex = currentIndex - 1
                                                }
                                        )
                                        Text(
                                            text = "${currentIndex + 1}/${transactions.size}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Emerald700,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next",
                                            tint = if (currentIndex < transactions.size - 1) Emerald700 else Emerald700.copy(alpha = 0.3f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable(enabled = currentIndex < transactions.size - 1) {
                                                    selectedIndex = currentIndex + 1
                                                }
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { currentOnDismiss(currentTx) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isGreek) "Αγνόηση" else "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Middle Row: Merchant + Non-wrapping Bank Badge on Left | Bold Amount on Right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = currentTx.merchantName.ifBlank {
                                        if (isGreek) "Πληρωμή με κάρτα" else "Card payment"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val suggestedCategory = remember(currentTx.id, currentTx.merchantName) {
                                    resolveSuggestedCategory(currentTx)?.takeIf { it.isNotBlank() }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Emerald700.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = currentTx.bankName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Emerald700,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (suggestedCategory != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                        ) {
                                            Text(
                                                text = suggestedCategory,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = String.format(Locale.GERMANY, "%.2f %s", currentTx.amount, currencySymbol),
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald700,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bottom Row: Action Buttons cleanly aligned
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { currentOnConfirm(currentTx) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("confirm_bank_transaction_button"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isGreek) "Καταχώρηση" else "Record",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = { currentOnDismiss(currentTx) },
                                modifier = Modifier.height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = if (isGreek) "Αγνόηση" else "Ignore",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
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
}
