package com.example.ui.components

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Household
import com.example.data.model.HouseholdRole
import com.example.data.model.SharedHouseholdUser
import com.example.data.model.UserProfile
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald700
import com.example.ui.viewmodel.OikoNomosViewModel
import com.example.util.Localization

private val EditorBlue = Color(0xFF0284C7)

@Composable
fun HouseholdRoleBadge(
    role: HouseholdRole,
    lang: Localization.Language,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (role) {
        HouseholdRole.CREATOR -> Emerald700
        HouseholdRole.EDITOR -> EditorBlue
        HouseholdRole.REGISTAR -> Amber700
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = badgeColor.copy(alpha = 0.13f),
        modifier = modifier.testTag("household_role_badge_${role.key.lowercase()}")
    ) {
        Text(
            text = Localization.householdRoleLabel(role, lang),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun HouseholdShareMask(
    household: Household,
    appLanguage: String,
    lookupState: OikoNomosViewModel.VerifiedUserLookupState,
    resolvedUsersByUid: Map<String, UserProfile> = emptyMap(),
    currentUserId: String = "",
    onSearchUser: (email: String, householdId: String) -> Unit,
    onClearLookup: () -> Unit,
    onAddSharedUser: (householdId: String, targetUser: UserProfile, role: HouseholdRole) -> Unit,
    onChangeSharedUserRole: (householdId: String, targetUid: String, targetEmail: String, newRole: HouseholdRole) -> Unit,
    onRemoveSharedUser: (householdId: String, targetUid: String, targetEmail: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    var emailInput by remember(household.id) { mutableStateOf("") }
    var selectedNewRole by remember(household.id) { mutableStateOf(HouseholdRole.REGISTAR) }

    val addedUsers = remember(
        household.memberUids,
        household.memberRoles,
        household.memberEmails,
        household.memberNames,
        household.ownerId,
        resolvedUsersByUid,
        currentUserId
    ) {
        household.memberUids
            .filter { it.isNotBlank() && it != household.ownerId && it != currentUserId }
            .distinct()
            .map { uid ->
                val resolved = resolvedUsersByUid[uid]
                val email = household.memberEmails[uid]?.trim()?.takeIf { it.isNotBlank() }
                    ?: resolved?.email?.trim()?.takeIf { it.isNotBlank() }
                    ?: ""
                val displayName = household.memberNames[uid]?.trim()?.takeIf { it.isNotBlank() }
                    ?: resolved?.displayName?.trim()?.takeIf { it.isNotBlank() }
                    ?: ""
                val rawRole = household.memberRoles[uid]
                val role = HouseholdRole.fromRaw(
                    rawRole ?: email.takeIf { it.isNotBlank() }?.let {
                        household.memberRoles[it.lowercase()] ?: household.memberRoles[it]
                    },
                    isCreator = false
                )
                SharedHouseholdUser(
                    uid = uid,
                    email = email,
                    displayName = displayName,
                    role = role
                )
            }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("household_share_mask_${household.id}"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )

        // Email input + Find button (flush alignment)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = emailInput,
                onValueChange = {
                    emailInput = it
                    onClearLookup()
                },
                placeholder = {
                    Text(
                        text = Localization.userEmailPlaceholder(lang),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald700,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (emailInput.isNotBlank()) {
                            onSearchUser(emailInput, household.id)
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("share_email_input_${household.id}")
            )

            Button(
                onClick = { onSearchUser(emailInput, household.id) },
                enabled = emailInput.isNotBlank() && lookupState !is OikoNomosViewModel.VerifiedUserLookupState.Searching,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald700,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier
                    .height(52.dp)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("share_find_btn_${household.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = Localization.findAction(lang),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = Localization.findAction(lang),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Lookup result
        when (lookupState) {
            is OikoNomosViewModel.VerifiedUserLookupState.Searching -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Emerald700
                    )
                }
            }

            is OikoNomosViewModel.VerifiedUserLookupState.NotFound,
            is OikoNomosViewModel.VerifiedUserLookupState.NotVerified,
            is OikoNomosViewModel.VerifiedUserLookupState.SelfEmail -> {
                Text(
                    text = Localization.userNotFound(lang),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("share_not_found_text_${household.id}")
                )
            }

            is OikoNomosViewModel.VerifiedUserLookupState.AlreadyMember -> {
                Text(
                    text = Localization.alreadyAddedMsg(lang),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Amber700,
                    modifier = Modifier.testTag("share_already_added_text_${household.id}")
                )
            }

            is OikoNomosViewModel.VerifiedUserLookupState.Error -> {
                Text(
                    text = lookupState.message,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("share_error_text_${household.id}")
                )
            }

            is OikoNomosViewModel.VerifiedUserLookupState.Found -> {
                val foundUser = lookupState.user
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Emerald700.copy(alpha = 0.08f),
                    border = BorderStroke(0.8.dp, Emerald700.copy(alpha = 0.30f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_found_user_card_${household.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Emerald700.copy(alpha = 0.16f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val initial = (foundUser.email.ifBlank { foundUser.displayName }).firstOrNull()?.uppercaseChar()?.toString()
                                    if (initial != null) {
                                        Text(
                                            text = initial,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald700
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Emerald700,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = foundUser.email.ifBlank { foundUser.displayName },
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (foundUser.displayName.isNotBlank() && !foundUser.displayName.equals(foundUser.email, ignoreCase = true)) {
                                    Text(
                                        text = foundUser.displayName,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        RoleSegmentedSelector(
                            selectedRole = selectedNewRole,
                            lang = lang,
                            onSelectRole = { selectedNewRole = it },
                            editorTag = "share_select_editor_${household.id}",
                            registarTag = "share_select_registar_${household.id}"
                        )

                        Button(
                            onClick = {
                                onAddSharedUser(household.id, foundUser, selectedNewRole)
                                emailInput = ""
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald700,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("share_add_btn_${household.id}")
                        ) {
                            Text(
                                text = Localization.add(lang),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            is OikoNomosViewModel.VerifiedUserLookupState.Idle -> Unit
        }

        // Added users for this household
        if (addedUsers.isNotEmpty()) {
            Text(
                text = Localization.sharedUsersTitle(lang, addedUsers.size),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.2.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                addedUsers.forEach { sharedUser ->
                    val hasResolved = resolvedUsersByUid.containsKey(sharedUser.uid)
                    val primaryEmailLabel = when {
                        sharedUser.email.isNotBlank() -> sharedUser.email
                        !hasResolved -> Localization.loadingEmail(lang)
                        sharedUser.displayName.isNotBlank() -> sharedUser.displayName
                        else -> Localization.registeredUserLabel(lang)
                    }
                    val isPendingEmail = sharedUser.email.isBlank() && !hasResolved

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shared_user_row_${sharedUser.uid}")
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Emerald700.copy(alpha = 0.12f),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (sharedUser.email.isNotBlank()) {
                                            Text(
                                                text = sharedUser.email.first().uppercaseChar().toString(),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald700
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Emerald700,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = primaryEmailLabel,
                                        fontSize = 13.sp,
                                        fontWeight = if (isPendingEmail) FontWeight.Normal else FontWeight.SemiBold,
                                        color = if (isPendingEmail) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (sharedUser.email.isNotBlank() &&
                                        sharedUser.displayName.isNotBlank() &&
                                        !sharedUser.displayName.equals(sharedUser.email, ignoreCase = true)
                                    ) {
                                        Text(
                                            text = sharedUser.displayName,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        onRemoveSharedUser(household.id, sharedUser.uid, sharedUser.email)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("remove_shared_user_${sharedUser.uid}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = Localization.removeUserAction(lang),
                                        tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Box(modifier = Modifier.padding(end = 6.dp)) {
                                RoleSegmentedSelector(
                                    selectedRole = sharedUser.role,
                                    lang = lang,
                                    onSelectRole = { newRole ->
                                        if (sharedUser.role != newRole) {
                                            onChangeSharedUserRole(
                                                household.id,
                                                sharedUser.uid,
                                                sharedUser.email,
                                                newRole
                                            )
                                        }
                                    },
                                    editorTag = "shared_user_role_editor_${sharedUser.uid}",
                                    registarTag = "shared_user_role_registar_${sharedUser.uid}"
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
private fun RoleSegmentedSelector(
    selectedRole: HouseholdRole,
    lang: Localization.Language,
    onSelectRole: (HouseholdRole) -> Unit,
    editorTag: String,
    registarTag: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RoleSegmentPill(
                label = Localization.householdRoleLabel(HouseholdRole.EDITOR, lang),
                selected = selectedRole == HouseholdRole.EDITOR,
                accentColor = EditorBlue,
                onClick = { onSelectRole(HouseholdRole.EDITOR) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(editorTag)
            )
            RoleSegmentPill(
                label = Localization.householdRoleLabel(HouseholdRole.REGISTAR, lang),
                selected = selectedRole == HouseholdRole.REGISTAR,
                accentColor = Amber700,
                onClick = { onSelectRole(HouseholdRole.REGISTAR) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(registarTag)
            )
        }
    }
}

@Composable
private fun RoleSegmentPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) accentColor.copy(alpha = 0.16f) else Color.Transparent,
        border = if (selected) BorderStroke(0.8.dp, accentColor.copy(alpha = 0.45f)) else null,
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
