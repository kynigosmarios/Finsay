package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Household
import com.example.data.model.SyncStatusState
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald700
import com.example.util.Localization

@Composable
fun HouseholdActiveBar(
    currentHousehold: Household?,
    households: List<Household>,
    appLanguage: String,
    isDarkTheme: Boolean,
    onSelectHousehold: (Household) -> Unit,
    onManageHouseholdsClick: () -> Unit,
    onCreateNewHouseholdClick: () -> Unit,
    onSwitchClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPro: Boolean = false,
    currentUserId: String = "",
    syncStatus: SyncStatusState = SyncStatusState.SYNCED,
    activityCount: Int = 0,
    onOpenActivity: (() -> Unit)? = null,
    onRefreshSync: (() -> Unit)? = null
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val isEn = lang == Localization.Language.ENGLISH
    val displayName = currentHousehold?.name?.ifBlank { if (isEn) "My Household Book" else "Το Εξοδολόγιό μου" } ?: if (isEn) "My Household Book" else "Το Εξοδολόγιό μου"

    val syncColor = when (syncStatus) {
        SyncStatusState.SYNCING -> Amber700
        SyncStatusState.SYNCED -> Emerald700
        SyncStatusState.OFFLINE -> MaterialTheme.colorScheme.error
    }
    val syncIcon = when (syncStatus) {
        SyncStatusState.OFFLINE -> Icons.Default.CloudOff
        SyncStatusState.SYNCING, SyncStatusState.SYNCED -> Icons.Default.CloudSync
    }
    val syncLabel = when (syncStatus) {
        SyncStatusState.SYNCING -> Localization.syncingStatus(lang)
        SyncStatusState.SYNCED -> Localization.syncedStatus(lang)
        SyncStatusState.OFFLINE -> Localization.offlineStatus(lang)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onManageHouseholdsClick() }
            .testTag("household_active_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = if (isEn) "Active Household" else "Ενεργό Εξοδολόγιο",
                    tint = Emerald700,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isPro) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Emerald700.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "PRO",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald700,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = syncColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, syncColor.copy(alpha = 0.30f)),
                    modifier = Modifier
                        .size(30.dp)
                        .then(
                            if (onRefreshSync != null) {
                                Modifier.clickable { onRefreshSync() }
                            } else {
                                Modifier
                            }
                        )
                        .testTag("household_sync_status_pill")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = syncIcon,
                            contentDescription = syncLabel,
                            tint = syncColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (onOpenActivity != null) {
                    Surface(
                        shape = CircleShape,
                        color = Emerald700.copy(alpha = 0.10f),
                        modifier = Modifier
                            .size(30.dp)
                            .clickable { onOpenActivity() }
                            .testTag("household_activity_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = Localization.activityHistoryTitle(lang),
                                tint = Emerald700,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onSwitchClick,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("household_switch_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = if (isEn) "Switch Household" else "Αλλαγή Εξοδολογίου",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
