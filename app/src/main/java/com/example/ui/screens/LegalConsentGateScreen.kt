package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.LegalDocumentsDialog
import com.example.ui.components.OikoNomosLogo
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald700
import com.example.ui.theme.LogoGreen
import com.example.ui.theme.ModernFontFamily
import com.example.ui.viewmodel.OikoNomosViewModel
import com.example.util.Localization

@Composable
fun LegalConsentGateScreen(
    viewModel: OikoNomosViewModel,
    modifier: Modifier = Modifier
) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val lang = Localization.Language.fromCode(appLanguage)
    val isEn = lang == Localization.Language.ENGLISH

    var isChecked by remember { mutableStateOf(false) }
    var showCheckboxError by remember { mutableStateOf(false) }
    var legalDialogTab by remember { mutableStateOf<String?>(null) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val brandAccent = if (isDark) Emerald500 else Emerald700

    BackHandler(enabled = legalDialogTab != null) {
        legalDialogTab = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to brandAccent.copy(alpha = if (isDark) 0.14f else 0.08f),
                                0.34f to Color.Transparent,
                                1.0f to Color.Transparent
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                            .weight(1f, fill = false),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top bar: compact language switcher on the right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactLanguageToggle(
                                currentLanguage = appLanguage,
                                accentColor = brandAccent,
                                onSelectLanguage = { viewModel.setAppLanguage(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Brand Emblem Lockup
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                brandAccent.copy(alpha = if (isDark) 0.28f else 0.16f)
                            ),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                OikoNomosLogo(size = 44.dp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Finsay",
                            fontFamily = ModernFontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.4).sp,
                            color = if (isDark) Emerald500 else LogoGreen
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        // Warm, non-repetitive editorial headline
                        Text(
                            text = if (isEn) "Before we begin" else "Πριν ξεκινήσουμε",
                            fontFamily = ModernFontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isEn)
                                "Your privacy and security always come first."
                            else
                                "Η ασφάλεια και το απόρρητό σας είναι πάντα προτεραιότητα.",
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Three calm, scannable privacy assurances
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.45f else 0.55f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                PrivacyPromiseRow(
                                    icon = Icons.Outlined.Lock,
                                    accentColor = brandAccent,
                                    title = if (isEn) "Encrypted & GDPR compliant" else "Κρυπτογραφημένα δεδομένα",
                                    subtitle = if (isEn)
                                        "Protected storage built to European privacy standards."
                                    else
                                        "Ασφαλής αποθήκευση σύμφωνα με τα πρότυπα GDPR."
                                )
                                PrivacyPromiseRow(
                                    icon = Icons.Outlined.VerifiedUser,
                                    accentColor = brandAccent,
                                    title = if (isEn) "Strictly private" else "Αυστηρά ιδιωτικά",
                                    subtitle = if (isEn)
                                        "We never sell or share your personal finances."
                                    else
                                        "Δεν κοινοποιούμε ούτε πουλάμε τα δεδομένα σας."
                                )
                                PrivacyPromiseRow(
                                    icon = Icons.Outlined.Tune,
                                    accentColor = brandAccent,
                                    title = if (isEn) "Always in your control" else "Πάντα στον έλεγχό σας",
                                    subtitle = if (isEn)
                                        "Export or delete your account whenever you want."
                                    else
                                        "Εξαγωγή ή διαγραφή λογαριασμού ανά πάσα στιγμή."
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Single, clean consent checkbox with inline clickable links (zero repetition)
                        val isErrorBorder = showCheckboxError && !isChecked
                        val consentContainerBg by animateColorAsState(
                            targetValue = when {
                                isErrorBorder -> MaterialTheme.colorScheme.error.copy(alpha = 0.07f)
                                isChecked -> brandAccent.copy(alpha = if (isDark) 0.14f else 0.08f)
                                else -> MaterialTheme.colorScheme.surface
                            },
                            label = "consent_bg"
                        )
                        val consentBorderColor by animateColorAsState(
                            targetValue = when {
                                isErrorBorder -> MaterialTheme.colorScheme.error
                                isChecked -> brandAccent
                                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                            },
                            label = "consent_border"
                        )

                        val linkStyle = TextLinkStyles(
                            style = SpanStyle(
                                color = brandAccent,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                        val consentAnnotated = buildAnnotatedString {
                            if (isEn) {
                                append("I agree to the ")
                                withLink(
                                    LinkAnnotation.Clickable(
                                        tag = "TERMS",
                                        styles = linkStyle,
                                        linkInteractionListener = { legalDialogTab = "TERMS" }
                                    )
                                ) {
                                    append("Terms & Conditions")
                                }
                                append(" and ")
                                withLink(
                                    LinkAnnotation.Clickable(
                                        tag = "PRIVACY",
                                        styles = linkStyle,
                                        linkInteractionListener = { legalDialogTab = "PRIVACY" }
                                    )
                                ) {
                                    append("Privacy Policy")
                                }
                            } else {
                                append("Αποδέχομαι τους ")
                                withLink(
                                    LinkAnnotation.Clickable(
                                        tag = "TERMS",
                                        styles = linkStyle,
                                        linkInteractionListener = { legalDialogTab = "TERMS" }
                                    )
                                ) {
                                    append("Όρους & Προϋποθέσεις")
                                }
                                append(" και την ")
                                withLink(
                                    LinkAnnotation.Clickable(
                                        tag = "PRIVACY",
                                        styles = linkStyle,
                                        linkInteractionListener = { legalDialogTab = "PRIVACY" }
                                    )
                                ) {
                                    append("Πολιτική Απορρήτου")
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = consentContainerBg,
                            border = BorderStroke(
                                width = if (isErrorBorder) 1.5.dp else 1.dp,
                                color = consentBorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        isChecked = it
                                        if (it) showCheckboxError = false
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Emerald700,
                                        checkmarkColor = Color.White,
                                        uncheckedColor = if (isErrorBorder) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    ),
                                    modifier = Modifier.testTag("legal_gate_checkbox")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = consentAnnotated,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isErrorBorder,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = if (isEn)
                                    "Please check the box above to continue."
                                else
                                    "Παρακαλούμε επιλέξτε το παραπάνω πεδίο για να συνεχίσετε.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, start = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Primary CTA Button
                        Button(
                            onClick = {
                                if (!isChecked) {
                                    showCheckboxError = true
                                } else {
                                    showCheckboxError = false
                                    viewModel.acceptCurrentLegalTerms()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald700,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("legal_gate_accept_button")
                        ) {
                            Text(
                                text = if (isEn) "Enter Finsay" else "Είσοδος στο Finsay",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Discreet return to Sign In
                        TextButton(
                            onClick = { viewModel.signOutUser() },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("legal_gate_back_to_auth_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "Back to Sign In" else "Επιστροφή στην αρχική οθόνη",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        // Full-screen in-window overlay for Legal Documents (avoids Android Dialog scroll/height clipping)
        if (legalDialogTab != null) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background
            ) { overlayPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(overlayPadding)
                ) {
                    LegalDocumentsDialog(
                        initialTab = legalDialogTab ?: "TERMS",
                        appLanguage = appLanguage,
                        onDismiss = { legalDialogTab = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyPromiseRow(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.11f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                lineHeight = 17.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactLanguageToggle(
    currentLanguage: String,
    accentColor: Color,
    onSelectLanguage: (String) -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguageChip(
                label = "ΕΛ",
                selected = currentLanguage == "EL",
                accentColor = accentColor,
                onClick = { onSelectLanguage("EL") }
            )
            LanguageChip(
                label = "EN",
                selected = currentLanguage == "EN",
                accentColor = accentColor,
                onClick = { onSelectLanguage("EN") }
            )
        }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) accentColor else Color.Transparent,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
