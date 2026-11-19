package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald700
import com.example.ui.viewmodel.PendingUndoDeletion
import com.example.util.Localization
import java.util.Locale

@Composable
fun UndoDeletionBanner(
    pendingUndo: PendingUndoDeletion?,
    currencySymbol: String,
    appLanguage: String,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)

    AnimatedVisibility(
        visible = pendingUndo != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        val item = pendingUndo ?: return@AnimatedVisibility
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            contentColor = Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 88.dp, bottom = 16.dp)
                .testTag("undo_deletion_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Emerald700.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.6f)),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${item.remainingSeconds}s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald400,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Localization.deletedPrefix(lang),
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.title} (${String.format(Locale.GERMANY, "%.2f %s", item.amount, currencySymbol)})",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TextButton(
                    onClick = onUndo,
                    colors = ButtonDefaults.textButtonColors(contentColor = Emerald400),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("undo_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = Localization.undoAction(lang),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Localization.undoAction(lang),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("dismiss_undo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = Localization.close(lang),
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
