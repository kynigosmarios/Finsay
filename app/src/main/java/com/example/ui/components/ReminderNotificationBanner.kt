package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Alarm
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
import com.example.data.entity.RecurringExpense
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald700
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun ReminderNotificationBanner(
    reminders: List<RecurringExpense>,
    appLanguage: String,
    onRecordReminder: (RecurringExpense) -> Unit,
    onDismissReminder: (RecurringExpense) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reminders.isEmpty()) return
    val isGreek = !appLanguage.equals("EN", ignoreCase = true)

    var selectedIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(reminders.size) {
        if (selectedIndex >= reminders.size) {
            selectedIndex = (reminders.size - 1).coerceAtLeast(0)
        }
    }
    val currentIndex = selectedIndex.coerceIn(0, (reminders.size - 1).coerceAtLeast(0))
    val reminder = reminders[currentIndex]

    val currentOnRecord by rememberUpdatedState(onRecordReminder)
    val currentOnDismiss by rememberUpdatedState(onDismissReminder)

    val bottomStackPadding = when {
        reminders.size >= 3 -> 10.dp
        reminders.size >= 2 -> 5.dp
        else -> 0.dp
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            key(reminder.id) {
                val offsetX = remember { Animatable(0f) }
                var cardWidthPx by remember { mutableFloatStateOf(1f) }
                var isDismissing by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                val currentReminder by rememberUpdatedState(reminder)

                // Stacked card layers matched cleanly to the foreground card footprint
                if (reminders.size >= 3) {
                    Surface(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(horizontal = 16.dp)
                            .padding(top = 10.dp)
                            .zIndex(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Amber700.copy(alpha = 0.22f)),
                        shadowElevation = 1.dp
                    ) {}
                }

                if (reminders.size >= 2) {
                    Surface(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(horizontal = 8.dp)
                            .padding(top = 5.dp)
                            .padding(bottom = if (reminders.size >= 3) 5.dp else 0.dp)
                            .zIndex(2f),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, Amber700.copy(alpha = 0.35f)),
                        shadowElevation = 2.dp
                    ) {}
                }

                // Dismiss feedback background while dragging left or right
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

                // Foreground Active Reminder Card
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
                                        currentOnDismiss(currentReminder)
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
                        .testTag("reminder_notification_banner")
                        .clickable { currentOnRecord(currentReminder) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, Amber700.copy(alpha = 0.75f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        // Top Row: Alarm Icon + Title | Counter Pill (if >1) + Dismiss X
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Amber700.copy(alpha = 0.15f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = Amber700,
                                    modifier = Modifier
                                        .padding(7.dp)
                                        .size(18.dp)
                                )
                            }

                            Text(
                                text = if (isGreek) "Υπενθύμιση Εξόδου" else "Expense Reminder",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (reminders.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Amber700.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Amber700.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Previous",
                                            tint = if (currentIndex > 0) Amber700 else Amber700.copy(alpha = 0.3f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable(enabled = currentIndex > 0) {
                                                    selectedIndex = currentIndex - 1
                                                }
                                        )
                                        Text(
                                            text = "${currentIndex + 1}/${reminders.size}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Amber700,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next",
                                            tint = if (currentIndex < reminders.size - 1) Amber700 else Amber700.copy(alpha = 0.3f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable(enabled = currentIndex < reminders.size - 1) {
                                                    selectedIndex = currentIndex + 1
                                                }
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { currentOnDismiss(currentReminder) },
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

                        // Middle Row: Title + Badge/Schedule on Left | Bold Amount on Right
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
                                    text = currentReminder.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Amber700.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = if (isGreek) "Πάγιο" else "Due",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Amber700,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }

                                    val timeFormatted = String.format(
                                        Locale.US,
                                        "%02d:%02d",
                                        currentReminder.reminderHour,
                                        currentReminder.reminderMinute
                                    )
                                    val summary = com.example.util.RecurringScheduleHelper.scheduleSummary(
                                        currentReminder,
                                        if (isGreek) com.example.util.Localization.Language.GREEK else com.example.util.Localization.Language.ENGLISH
                                    )
                                    Text(
                                        text = "$summary • $timeFormatted",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Text(
                                text = String.format(Locale.GERMANY, "%.2f €", currentReminder.amount),
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
                                onClick = { currentOnRecord(currentReminder) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("record_reminder_button"),
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
                                    text = if (isGreek) "Καταγραφή" else "Record",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = { currentOnDismiss(currentReminder) },
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
