package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald700
import com.example.ui.theme.ModernFontFamily
import com.example.util.Localization

/**
 * Clean, lightweight welcome dialog shown on first entry into Finsay.
 * Warmly greets the user and highlights the core key features at a glance.
 */
@Composable
fun WelcomeOnboardingCarouselDialog(
    appLanguage: String = "EL",
    onDismiss: () -> Unit,
    onOpenProPaywall: (() -> Unit)? = null,
    onOpenLegalDocument: ((String) -> Unit)? = null
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val brandAccent = if (isDark) Emerald500 else Emerald700

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SmoothDialogContent {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .widthIn(max = 420.dp)
                    .testTag("welcome_onboarding_carousel_dialog"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.45f else 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to brandAccent.copy(alpha = if (isDark) 0.18f else 0.10f),
                                    0.38f to Color.Transparent,
                                    1.0f to Color.Transparent
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Crisp branded emblem on clean surface badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                brandAccent.copy(alpha = if (isDark) 0.30f else 0.18f)
                            ),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                OikoNomosLogo(size = 38.dp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = Localization.tr(lang, "Welcome to Finsay!", "Καλώς ήρθατε στο Finsay!"),
                            fontFamily = ModernFontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.4).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = Localization.tr(
                                lang,
                                "Everything you need to manage your finances effortlessly.",
                                "Όλα τα οικονομικά σας, απλά και οργανωμένα."
                            ),
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 4 Concise, scannable key features
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(15.dp)
                        ) {
                            WelcomeFeatureItem(
                                icon = Icons.Rounded.Mic,
                                accentColor = brandAccent,
                                title = Localization.tr(lang, "Log in seconds", "Άμεση καταχώρηση"),
                                subtitle = Localization.tr(
                                    lang,
                                    "By voice, receipt scan, or quick entry.",
                                    "Με φωνή, σάρωση απόδειξης ή ένα πάτημα."
                                )
                            )
                            WelcomeFeatureItem(
                                icon = Icons.Rounded.Groups,
                                accentColor = brandAccent,
                                title = Localization.tr(lang, "Shared expense books", "Κοινά εξοδολόγια"),
                                subtitle = Localization.tr(
                                    lang,
                                    "Track & split expenses with family or friends.",
                                    "Μοιραστείτε έξοδα με οικογένεια ή παρέα."
                                )
                            )
                            WelcomeFeatureItem(
                                icon = Icons.Rounded.PieChart,
                                accentColor = brandAccent,
                                title = Localization.tr(lang, "Charts & budgets", "Γραφήματα & όρια"),
                                subtitle = Localization.tr(
                                    lang,
                                    "Clear spending insights and monthly limits.",
                                    "Καθαρή εικόνα εξόδων και μηνιαία budget."
                                )
                            )
                            WelcomeFeatureItem(
                                icon = Icons.Rounded.NotificationsActive,
                                accentColor = brandAccent,
                                title = Localization.tr(lang, "Reminders & goals", "Υπενθυμίσεις & στόχοι"),
                                subtitle = Localization.tr(
                                    lang,
                                    "Stay on top of bills and savings goals.",
                                    "Πάγιοι λογαριασμοί και στόχοι αποταμίευσης."
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald700,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("welcome_get_started_btn")
                        ) {
                            Text(
                                text = Localization.tr(lang, "Get Started", "Ξεκινήστε"),
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeFeatureItem(
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
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
