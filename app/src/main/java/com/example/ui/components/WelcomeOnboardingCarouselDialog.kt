package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.Emerald700

/**
 * Streamlined single-screen first-launch welcome & privacy consent dialog.
 * Replaces the multi-slide carousel + separate GDPR popup so new users can
 * start logging their first expense in seconds, while permissions (mic, camera,
 * notifications, bank push) are requested strictly just-in-time.
 */
@Composable
fun WelcomeOnboardingCarouselDialog(
    appLanguage: String = "EL",
    onDismiss: () -> Unit,
    onOpenProPaywall: (() -> Unit)? = null,
    onOpenLegalDocument: ((String) -> Unit)? = null
) {
    val isEn = appLanguage.equals("EN", ignoreCase = true)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SmoothDialogContent {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 440.dp)
                    .testTag("welcome_onboarding_carousel_dialog"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Compact Branded Emerald Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF047857), Color(0xFF10B981))
                                )
                            )
                            .padding(horizontal = 22.dp, vertical = 22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.finsay_logo),
                                    contentDescription = "Finsay",
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isEn) "Welcome to Finsay" else "Καλώς ήρθατε στο Finsay",
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEn)
                                    "Track expenses in seconds — solo or with family"
                                else
                                    "Καταγράψτε έξοδα σε δευτερόλεπτα — μόνοι ή με την οικογένεια",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // 3 Concise Feature Highlights
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WelcomeFeatureRow(
                            icon = Icons.Default.Mic,
                            title = if (isEn) "Voice, Receipt Scan & Quick Entry" else "Φωνή, Σάρωση Απόδειξης & Γρήγορη Καταχώρηση",
                            subtitle = if (isEn)
                                "Tap the green + button anytime to log an expense or income."
                            else
                                "Πατήστε το πράσινο + ανά πάσα στιγμή για άμεση καταχώρηση."
                        )
                        WelcomeFeatureRow(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = if (isEn) "Shared Households & Fair Split" else "Κοινά Εξοδολόγια & Δίκαιος Επιμερισμός",
                            subtitle = if (isEn)
                                "Sync in real time with family, partners or roommates."
                            else
                                "Συγχρονισμός σε πραγματικό χρόνο με οικογένεια, σύντροφο ή συγκάτοικους."
                        )
                        WelcomeFeatureRow(
                            icon = Icons.Default.PieChart,
                            title = if (isEn) "Clear Analytics & Smart Budgets" else "Καθαρά Γραφήματα & Έξυπνοι Προϋπολογισμοί",
                            subtitle = if (isEn)
                                "Permissions (mic, camera, notifications) are only asked when you use them."
                            else
                                "Άδειες (μικρόφωνο, κάμερα, ειδοποιήσεις) ζητούνται μόνο όταν τις χρειαστείτε."
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Compact Inline GDPR / Privacy Notice
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEn)
                                            "Your data is encrypted and private (GDPR). By continuing you agree to our Privacy Policy & Terms."
                                        else
                                            "Τα δεδομένα σας προστατεύονται με κρυπτογράφηση (GDPR). Συνεχίζοντας αποδέχεστε την Πολιτική Απορρήτου & τους Όρους Χρήσης.",
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (onOpenLegalDocument != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(
                                                text = if (isEn) "Privacy Policy" else "Πολιτική Απορρήτου",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Emerald700,
                                                textDecoration = TextDecoration.Underline,
                                                modifier = Modifier.clickable { onOpenLegalDocument("privacy") }
                                            )
                                            Text(
                                                text = if (isEn) "Terms of Service" else "Όροι Χρήσης",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Emerald700,
                                                textDecoration = TextDecoration.Underline,
                                                modifier = Modifier.clickable { onOpenLegalDocument("terms") }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("welcome_get_started_btn")
                        ) {
                            Text(
                                text = if (isEn) "Get Started" else "Ξεκινήστε",
                                color = Color.White,
                                fontSize = 15.sp,
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
private fun WelcomeFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Emerald700.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Emerald700,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 16.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
