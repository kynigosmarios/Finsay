package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.FamilyMember
import com.example.data.model.ConnectedHouseholdUser
import com.example.ui.theme.Emerald700
import com.example.util.Localization

val MemberPalette = listOf(
    0xFF0284C7, // Sky Blue
    0xFF10B981, // Emerald
    0xFFF59E0B, // Amber
    0xFFEF4444, // Coral / Red
    0xFF8B5CF6, // Purple
    0xFFD946EF, // Fuchsia
    0xFFEC4899, // Pink
    0xFF06B6D4, // Cyan
    0xFF84CC16, // Lime
    0xFF6366F1  // Indigo
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberDialog(
    memberToEdit: FamilyMember?,
    appLanguage: String = "EL",
    connectedUsers: List<ConnectedHouseholdUser> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, role: String, avatarColorHex: Long, voiceKeywords: String, linkedUserUid: String) -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)

    var name by remember(memberToEdit) { mutableStateOf(memberToEdit?.name ?: "") }
    var role by remember(memberToEdit) { mutableStateOf(memberToEdit?.role ?: "Μονοπρόσωπο") }
    var selectedColor by remember(memberToEdit) {
        mutableLongStateOf(memberToEdit?.avatarColorHex ?: 0xFF0284C7)
    }
    var voiceKeywords by remember(memberToEdit) {
        mutableStateOf(memberToEdit?.voiceKeywords ?: "")
    }
    var linkedUserUid by remember(memberToEdit) {
        mutableStateOf(memberToEdit?.linkedUserUid ?: "")
    }
    var errorText by remember(memberToEdit) { mutableStateOf<String?>(null) }
    var showVoiceKeywordsHint by remember { mutableStateOf(false) }
    var hasShownVoiceKeywordsHint by remember { mutableStateOf(false) }

    if (showVoiceKeywordsHint) {
        val isEnglish = lang == Localization.Language.ENGLISH
        AlertDialog(
            onDismissRequest = { showVoiceKeywordsHint = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(26.dp)
                )
            },
            title = {
                Text(
                    text = if (isEnglish) "Voice Keywords" else "Φωνητικές Λέξεις",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isEnglish) {
                        "Enter nicknames or alternative names separated by commas (e.g. mom, Helen, wife). When using voice entry, Finsay automatically recognizes this member whenever it hears any of these words."
                    } else {
                        "Γράψτε υποκοριστικά ή λέξεις χωρισμένες με κόμμα (π.χ. μαμά, Ελένη, γυναίκα). Στη φωνητική καταχώρηση, το Finsay αναγνωρίζει αυτόματα αυτό το μέλος μόλις ακούσει κάποια από αυτές."
                    },
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showVoiceKeywordsHint = false },
                    modifier = Modifier.testTag("member_voice_hint_ok_btn")
                ) {
                    Text(
                        text = if (isEnglish) "Got it" else "Κατάλαβα",
                        fontWeight = FontWeight.Bold,
                        color = Emerald700
                    )
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SmoothDialogContent {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .widthIn(max = 500.dp)
                    .fillMaxHeight(0.88f)
                    .padding(vertical = 12.dp)
                    .testTag("member_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 1. Fixed Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (memberToEdit == null) Localization.addMember(lang) else Localization.editMemberTitle(lang),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald700
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, contentDescription = Localization.close(lang), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Middle Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorText = null
                        },
                        label = { Text(Localization.memberNameLabel(lang)) },
                        placeholder = { Text(Localization.memberNamePlaceholder(lang)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Role selection
                    Text(
                        text = Localization.memberRoleLabel(lang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Γονέας", "Παιδί", "Σύντροφος", "Συγκάτοικος", "Συνεργάτης", "Φίλος", "Μονοπρόσωπο", "Άλλο").forEach { r ->
                            val translatedRole = Localization.translateRole(r, lang)
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(translatedRole, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Color picker
                    var showColorPalette by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showColorPalette = !showColorPalette }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(selectedColor))
                                .border(1.5.dp, Emerald700, CircleShape)
                        )
                        Text(
                            text = Localization.selectColor(lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (showColorPalette) (if (lang == Localization.Language.ENGLISH) "Hide" else "Απόκρυψη") else Localization.changeAction(lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald700
                        )
                    }

                    AnimatedVisibility(
                        visible = showColorPalette,
                        enter = FinsayMotion.ExpandEnter,
                        exit = FinsayMotion.ShrinkExit
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MemberPalette.forEach { colorVal ->
                                    val isSelected = selectedColor == colorVal
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorVal))
                                            .clickable {
                                                selectedColor = colorVal
                                                showColorPalette = false
                                            }
                                            .then(
                                                if (isSelected) Modifier.border(2.dp, Emerald700, CircleShape)
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Voice keywords (with instruction popup on first focus + help button)
                    OutlinedTextField(
                        value = voiceKeywords,
                        onValueChange = { voiceKeywords = it },
                        label = {
                            Text(
                                text = Localization.voiceKeywordsLabel(lang),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        placeholder = { Text(Localization.memberKeywordsPlaceholder(lang)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { showVoiceKeywordsHint = true },
                                modifier = Modifier.testTag("member_voice_keywords_info_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = if (lang == Localization.Language.ENGLISH) "Voice keywords help" else "Οδηγίες φωνητικών λέξεων",
                                    tint = Emerald700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && !hasShownVoiceKeywordsHint) {
                                    hasShownVoiceKeywordsHint = true
                                    showVoiceKeywordsHint = true
                                }
                            }
                            .testTag("member_voice_keywords_input"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2
                    )

                    if (connectedUsers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = Localization.linkedAppUserLabel(lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = linkedUserUid.isBlank(),
                                onClick = { linkedUserUid = "" },
                                label = {
                                    Text(
                                        text = Localization.noLinkedUserLabel(lang),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                modifier = Modifier.testTag("member_linked_user_chip_none")
                            )
                            connectedUsers.forEach { user ->
                                val label = user.email.ifBlank { user.displayName }.ifBlank { user.uid.take(8) }
                                FilterChip(
                                    selected = linkedUserUid == user.uid,
                                    onClick = { linkedUserUid = user.uid },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("member_linked_user_chip_${user.uid}")
                                )
                            }
                        }
                    }

                    if (errorText != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = errorText ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // 3. Fixed Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = Localization.cancel(lang),
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorText = if (lang == Localization.Language.ENGLISH) "Please enter member name" else "Συμπληρώστε όνομα μέλους"
                                return@Button
                            }
                            onSave(
                                memberToEdit?.id ?: 0L,
                                name.trim(),
                                role,
                                selectedColor,
                                voiceKeywords.trim(),
                                linkedUserUid
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_member_button"),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Localization.save(lang),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
        }
    }
}
