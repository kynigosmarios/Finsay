package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.Emerald700
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun AppUpdateBanner(
    appLanguage: String,
    onUpdateClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGreek = !appLanguage.equals("EN", ignoreCase = true)
    val currentOnUpdate by rememberUpdatedState(onUpdateClick)
    val currentOnDismiss by rememberUpdatedState(onDismissClick)

    val offsetX = remember { Animatable(0f) }
    var cardWidthPx by remember { mutableFloatStateOf(1f) }
    var isDismissing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val dragProgress = (abs(offsetX.value) / (cardWidthPx * 0.28f).coerceAtLeast(1f)).coerceIn(0f, 1f)
        if (abs(offsetX.value) > 6f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
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
                                currentOnDismiss()
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
                .testTag("app_update_banner"),
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
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(18.dp)
                        )
                    }

                    Text(
                        text = if (isGreek) "Διαθέσιμη Ενημέρωση" else "Update Available",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { currentOnDismiss() },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("app_update_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isGreek) "Αγνόηση" else "Ignore",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isGreek) "Υπάρχει νέα έκδοση του Finsay" else "A new version of Finsay is available",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isGreek) {
                        "Ενημερώστε από το Google Play για τις πιο πρόσφατες βελτιώσεις και διορθώσεις."
                    } else {
                        "Update from Google Play to get the latest improvements and fixes."
                    },
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { currentOnUpdate() },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("app_update_now_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGreek) "Ενημέρωση" else "Update",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = { currentOnDismiss() },
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("app_update_ignore_button"),
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
