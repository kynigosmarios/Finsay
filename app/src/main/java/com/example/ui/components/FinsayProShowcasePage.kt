package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.BillingManager
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald700
import com.example.util.Localization

private data class ProBenefitItem(
    val icon: ImageVector,
    val accent: Color,
    val title: String,
    val description: String,
    val freeLimitBadge: String? = null
)

@Composable
fun FinsayProShowcasePage(
    isPro: Boolean,
    appLanguage: String = "EN",
    onLaunchBilling: ((String) -> Boolean)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lang = Localization.Language.fromCode(appLanguage)
    val isEnglish = lang == Localization.Language.ENGLISH
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var selectedPlan by remember { mutableStateOf(BillingManager.SUB_YEARLY) }

    BackHandler(onBack = onDismiss)

    val benefits = remember(isEnglish) {
        if (isEnglish) {
            listOf(
                ProBenefitItem(
                    icon = Icons.Default.Category,
                    accent = Color(0xFF059669),
                    title = "Unlimited Categories",
                    description = "Create as many expense and income categories as your lifestyle needs.",
                    freeLimitBadge = "Free: 5 each"
                ),
                ProBenefitItem(
                    icon = Icons.Default.Mic,
                    accent = Color(0xFF7C3AED),
                    title = "Unlimited Voice Logging",
                    description = "Dictate expenses naturally anytime without monthly voice quotas.",
                    freeLimitBadge = "Free: 15/mo"
                ),
                ProBenefitItem(
                    icon = Icons.Default.CameraAlt,
                    accent = Color(0xFF0284C7),
                    title = "Receipt OCR & AADE QR Scanner",
                    description = "Snap paper receipts or scan QR codes to autofill merchant, amount & date."
                ),
                ProBenefitItem(
                    icon = Icons.Default.AccountBalance,
                    accent = Color(0xFF0D9488),
                    title = "Automatic Bank & Wallet Catcher",
                    description = "Detect payments and deposits from banks, Revolut, Google Pay & PayPal automatically."
                ),
                ProBenefitItem(
                    icon = Icons.Default.Groups,
                    accent = Color(0xFFD97706),
                    title = "Multiple Household Books & Members",
                    description = "Manage separate books for home, travel, or business with unlimited members.",
                    freeLimitBadge = "Free: 1 book • 4 members"
                ),
                ProBenefitItem(
                    icon = Icons.Default.Savings,
                    accent = Color(0xFF10B981),
                    title = "Unlimited Goals & Bill Reminders",
                    description = "Track every savings goal and automate recurring bills without caps.",
                    freeLimitBadge = "Free: 1 goal • 2 bills"
                ),
                ProBenefitItem(
                    icon = Icons.Default.Description,
                    accent = Color(0xFF2563EB),
                    title = "PDF, Excel & CSV Reports",
                    description = "Export polished financial summaries and member settle-up reports in one tap."
                )
            )
        } else {
            listOf(
                ProBenefitItem(
                    icon = Icons.Default.Category,
                    accent = Color(0xFF059669),
                    title = "Απεριόριστες Κατηγορίες",
                    description = "Δημιουργήστε όσες κατηγορίες εξόδων και εσόδων χρειάζεστε χωρίς περιορισμούς.",
                    freeLimitBadge = "Δωρεάν: έως 5"
                ),
                ProBenefitItem(
                    icon = Icons.Default.Mic,
                    accent = Color(0xFF7C3AED),
                    title = "Απεριόριστες Φωνητικές Καταχωρήσεις",
                    description = "Καταγράψτε έξοδα με τη φωνή σας όσες φορές θέλετε κάθε μήνα.",
                    freeLimitBadge = "Δωρεάν: 15/μήνα"
                ),
                ProBenefitItem(
                    icon = Icons.Default.CameraAlt,
                    accent = Color(0xFF0284C7),
                    title = "Σάρωση Απόδειξης (OCR & QR ΑΑΔΕ)",
                    description = "Φωτογραφίστε αποδείξεις ή σαρώστε QR ΑΑΔΕ για άμεση συμπλήρωση στοιχείων."
                ),
                ProBenefitItem(
                    icon = Icons.Default.AccountBalance,
                    accent = Color(0xFF0D9488),
                    title = "Αυτόματος Εντοπισμός Τραπεζών",
                    description = "Αναγνωρίζει πληρωμές και καταθέσεις από τράπεζες, Revolut, Google Pay & PayPal."
                ),
                ProBenefitItem(
                    icon = Icons.Default.Groups,
                    accent = Color(0xFFD97706),
                    title = "Πολλαπλά Εξοδολόγια & Μέλη",
                    description = "Οργανώστε ξεχωριστά εξοδολόγια για σπίτι, ταξίδια ή επιχείρηση με απεριόριστα μέλη.",
                    freeLimitBadge = "Δωρεάν: 1 βιβλίο • 4 μέλη"
                ),
                ProBenefitItem(
                    icon = Icons.Default.Savings,
                    accent = Color(0xFF10B981),
                    title = "Απεριόριστοι Στόχοι & Πάγιες Εντολές",
                    description = "Παρακολουθήστε όλους τους στόχους αποταμίευσης και τις υπενθυμίσεις λογαριασμών.",
                    freeLimitBadge = "Δωρεάν: 1 στόχος • 2 πάγιες"
                ),
                ProBenefitItem(
                    icon = Icons.Default.Description,
                    accent = Color(0xFF2563EB),
                    title = "Εξαγωγή Αναφορών PDF, Excel & CSV",
                    description = "Κατεβάστε και μοιραστείτε καθαρές οικονομικές αναφορές και εκκαθαρίσεις με 1 πάτημα."
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("finsay_pro_showcase_page"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("finsay_pro_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isEnglish) "Back" else "Πίσω",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Finsay Pro",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isPro) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Amber700.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Amber700.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Amber700,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isEnglish) "ACTIVE" else "ΕΝΕΡΓΟ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Amber700
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hero Banner Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF064E3B),
                                    Color(0xFF047857),
                                    Color(0xFF0F766E)
                                )
                            )
                        )
                        .padding(horizontal = 22.dp, vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFBBF24).copy(alpha = 0.22f),
                            border = BorderStroke(1.5.dp, Color(0xFFFBBF24).copy(alpha = 0.7f)),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Finsay Pro",
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isPro) {
                                        if (isEnglish) "YOU ARE A PRO MEMBER" else "ΕΙΣΤΕ ΜΕΛΟΣ FINSAY PRO"
                                    } else {
                                        if (isEnglish) "COMPLETE FINANCIAL FREEDOM" else "ΠΛΗΡΗΣ ΟΙΚΟΝΟΜΙΚΟΣ ΕΛΕΓΧΟΣ"
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = Color(0xFFFDE68A)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isPro) {
                                if (isEnglish) "All Finsay Pro features are unlocked"
                                else "Όλες οι δυνατότητες του Finsay Pro είναι ενεργές"
                            } else {
                                if (isEnglish) "Unlock the full power of Finsay"
                                else "Ξεκλειδώστε όλη τη δύναμη του Finsay"
                            },
                            fontSize = 22.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isPro) {
                                if (isEnglish) "Thank you for supporting Finsay! Enjoy unlimited automation, categories, books, and exports."
                                else "Ευχαριστούμε που στηρίζετε το Finsay! Απολαύστε απεριόριστους αυτοματισμούς, κατηγορίες, βιβλία και αναφορές."
                            } else {
                                if (isEnglish) "Automate every expense, remove all free limits, and manage your finances effortlessly."
                                else "Αυτοματοποιήστε κάθε συναλλαγή, καταργήστε όλα τα όρια και διαχειριστείτε τα οικονομικά σας χωρίς κόπο."
                            },
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            color = Color.White.copy(alpha = 0.88f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pricing Section (or Manage Subscription if already Pro)
            if (!isPro) {
                Text(
                    text = if (isEnglish) "Choose Your Plan" else "Επιλέξτε το Πλάνο σας",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProPlanOptionCard(
                        title = if (isEnglish) "Yearly Plan" else "Ετήσιο Πλάνο",
                        price = if (isEnglish) "29€ / year" else "29€ / έτος",
                        subtitle = if (isEnglish) "Just 2.41€/month • Best value" else "Μόλις 2,41€/μήνα • Καλύτερη επιλογή",
                        badge = if (isEnglish) "POPULAR • -19%" else "ΔΗΜΟΦΙΛΕΣ • -19%",
                        badgeColor = Emerald700,
                        isSelected = selectedPlan == BillingManager.SUB_YEARLY,
                        isDark = isDark,
                        testTag = "finsay_pro_plan_yearly",
                        onClick = { selectedPlan = BillingManager.SUB_YEARLY }
                    )

                    ProPlanOptionCard(
                        title = if (isEnglish) "Monthly Plan" else "Μηνιαίο Πλάνο",
                        price = if (isEnglish) "2.99€ / month" else "2,99€ / μήνα",
                        subtitle = if (isEnglish) "Flexible billing • Cancel anytime" else "Ευέλικτη χρέωση • Ακύρωση όποτε θέλετε",
                        badge = null,
                        badgeColor = Emerald700,
                        isSelected = selectedPlan == BillingManager.SUB_MONTHLY,
                        isDark = isDark,
                        testTag = "finsay_pro_plan_monthly",
                        onClick = { selectedPlan = BillingManager.SUB_MONTHLY }
                    )

                    ProPlanOptionCard(
                        title = if (isEnglish) "Lifetime Access" else "Εφάπαξ (Lifetime)",
                        price = "49€",
                        subtitle = if (isEnglish) "Pay once, own Finsay Pro forever" else "Πληρώνετε μία φορά, το έχετε για πάντα",
                        badge = if (isEnglish) "ONE-TIME" else "ΓΙΑ ΠΑΝΤΑ",
                        badgeColor = Amber700,
                        isSelected = selectedPlan == BillingManager.INAPP_LIFETIME,
                        isDark = isDark,
                        testTag = "finsay_pro_plan_lifetime",
                        onClick = { selectedPlan = BillingManager.INAPP_LIFETIME }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val launched = onLaunchBilling?.invoke(selectedPlan) == true
                        if (!launched) {
                            try {
                                val pkg = context.packageName
                                val url = "https://play.google.com/store/account/subscriptions?package=$pkg"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) {}
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald700,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("finsay_pro_subscribe_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFDE68A),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (selectedPlan) {
                            BillingManager.SUB_MONTHLY -> if (isEnglish) "Upgrade to Pro • 2.99€/mo" else "Αναβάθμιση σε Pro • 2,99€/μήνα"
                            BillingManager.INAPP_LIFETIME -> if (isEnglish) "Unlock Lifetime Pro • 49€" else "Απόκτηση Lifetime Pro • 49€"
                            else -> if (isEnglish) "Upgrade to Pro • 29€/year" else "Αναβάθμιση σε Pro • 29€/έτος"
                        },
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isEnglish) {
                        "Secure checkout via Google Play • Cancel subscriptions anytime"
                    } else {
                        "Ασφαλής πληρωμή μέσω Google Play • Ακύρωση συνδρομής ανά πάσα στιγμή"
                    },
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedButton(
                    onClick = {
                        try {
                            val pkg = context.packageName
                            val url = "https://play.google.com/store/account/subscriptions?package=$pkg"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("finsay_pro_manage_sub_btn")
                ) {
                    Text(
                        text = if (isEnglish) "Manage Subscription in Google Play" else "Διαχείριση Συνδρομής στο Google Play",
                        fontWeight = FontWeight.Bold,
                        color = Emerald700
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Features Showcase Section
            Text(
                text = if (isEnglish) "Everything Included in Finsay Pro" else "Όλα όσα περιλαμβάνει το Finsay Pro",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                benefits.forEach { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = item.accent.copy(alpha = if (isDark) 0.22f else 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (item.freeLimitBadge != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = item.freeLimitBadge,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = item.description,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProPlanOptionCard(
    title: String,
    price: String,
    subtitle: String,
    badge: String?,
    badgeColor: Color,
    isSelected: Boolean,
    isDark: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val bgColor = if (isSelected) {
        if (isDark) Emerald700.copy(alpha = 0.20f) else Emerald700.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Emerald700 else Color.Transparent)
                    .then(
                        if (!isSelected) Modifier.background(Color.Transparent) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) Emerald700 else Color.Transparent,
                    border = BorderStroke(2.dp, if (isSelected) Emerald700 else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isSelected) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor
                        ) {
                            Text(
                                text = badge,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = price,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) Emerald700 else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
