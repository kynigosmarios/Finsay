package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald700
import kotlinx.coroutines.launch

private data class WelcomeCarouselSlide(
    val id: String,
    val badge: String,
    val title: String,
    val subtitle: String,
    val bullets: List<String>,
    val icon: ImageVector,
    val gradientStart: Color,
    val gradientEnd: Color,
    val accentColor: Color,
    val isProSlide: Boolean = false
)

@Composable
fun WelcomeOnboardingCarouselDialog(
    appLanguage: String = "EL",
    onDismiss: () -> Unit,
    onOpenProPaywall: (() -> Unit)? = null
) {
    val isEnglish = appLanguage.equals("EN", ignoreCase = true)
    val slides = remember(isEnglish) { buildWelcomeCarouselSlides(isEnglish) }
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SmoothDialogContent {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.93f)
                    .widthIn(max = 460.dp)
                    .testTag("welcome_onboarding_carousel_dialog"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Skip Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 10.dp, top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${slides.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("welcome_carousel_skip_btn")
                        ) {
                            Text(
                                text = if (isEnglish) "Skip" else "Παράλειψη",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Swipeable Pager Slides
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val slide = slides[page]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Hero Icon Banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(slide.gradientStart, slide.gradientEnd)
                                        )
                                    )
                                    .padding(vertical = 20.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = slide.icon,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color.White.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            text = slide.badge,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = slide.title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = slide.subtitle,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                border = BorderStroke(
                                    1.dp,
                                    if (slide.isProSlide) Amber700.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    slide.bullets.forEach { bullet ->
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = slide.accentColor,
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = bullet,
                                                fontSize = 12.5.sp,
                                                lineHeight = 17.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            if (slide.isProSlide && onOpenProPaywall != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onOpenProPaywall()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Amber700.copy(alpha = 0.65f)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .testTag("welcome_carousel_pro_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Amber700,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Explore Finsay Pro" else "Δείτε το Finsay Pro",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Amber700
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Page Indicator Dots
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        slides.forEachIndexed { idx, slide ->
                            val isSelected = pagerState.currentPage == idx
                            val dotWidth by animateDpAsState(
                                targetValue = if (isSelected) 22.dp else 7.dp,
                                animationSpec = tween(220),
                                label = "welcomeDotWidth_$idx"
                            )
                            val dotColor by animateColorAsState(
                                targetValue = if (isSelected) slide.accentColor else MaterialTheme.colorScheme.outlineVariant,
                                animationSpec = tween(220),
                                label = "welcomeDotColor_$idx"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(7.dp)
                                    .width(dotWidth)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }

                    // Bottom Navigation Buttons
                    val isFirst = pagerState.currentPage == 0
                    val isLast = pagerState.currentPage == slides.lastIndex
                    val activeAccent = slides[pagerState.currentPage].accentColor
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 18.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isFirst) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("welcome_carousel_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "Back" else "Πίσω",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isLast) {
                                    onDismiss()
                                } else {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(slides.lastIndex))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeAccent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(if (isFirst) 1f else 1.4f)
                                .height(48.dp)
                                .testTag("welcome_carousel_next_btn")
                        ) {
                            Text(
                                text = when {
                                    isLast -> if (isEnglish) "Get Started" else "Ξεκινήστε"
                                    else -> if (isEnglish) "Next" else "Επόμενο"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isLast) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildWelcomeCarouselSlides(isEnglish: Boolean): List<WelcomeCarouselSlide> {
    return if (isEnglish) {
        listOf(
            WelcomeCarouselSlide(
                id = "setup",
                badge = "QUICK SETUP",
                title = "Welcome to Finsay",
                subtitle = "Ready in under a minute.",
                bullets = listOf(
                    "Add members & voice keywords",
                    "Set categories & monthly budgets",
                    "Pick your default payer(s)"
                ),
                icon = Icons.Default.AccountBalanceWallet,
                gradientStart = Color(0xFF065F46),
                gradientEnd = Color(0xFF10B981),
                accentColor = Emerald700
            ),
            WelcomeCarouselSlide(
                id = "entry",
                badge = "FAST ENTRY",
                title = "Log in Seconds",
                subtitle = "Manual, voice, or automatic.",
                bullets = listOf(
                    "Voice expense logging (15 free/month)",
                    "Split across members & multiple payers",
                    "Instant filters by month, category & member"
                ),
                icon = Icons.Default.Mic,
                gradientStart = Color(0xFF0369A1),
                gradientEnd = Color(0xFF0EA5E9),
                accentColor = Color(0xFF0284C7)
            ),
            WelcomeCarouselSlide(
                id = "insights",
                badge = "CONTROL & SYNC",
                title = "Statistics & Sharing",
                subtitle = "Clear view in real time.",
                bullets = listOf(
                    "Charts, budget alerts & Settle-Up",
                    "Bill reminders & savings goals",
                    "Shared households with roles"
                ),
                icon = Icons.Default.PieChart,
                gradientStart = Color(0xFF4338CA),
                gradientEnd = Color(0xFF6366F1),
                accentColor = Color(0xFF4F46E5)
            ),
            WelcomeCarouselSlide(
                id = "pro",
                badge = "FINSAY PRO",
                title = "Full Automation",
                subtitle = "Start free, upgrade anytime.",
                bullets = listOf(
                    "Unlimited voice & receipt scanning",
                    "Automatic Bank Push reader",
                    "Multiple households & PDF/CSV export"
                ),
                icon = Icons.Default.WorkspacePremium,
                gradientStart = Color(0xFFB45309),
                gradientEnd = Color(0xFFF59E0B),
                accentColor = Amber700,
                isProSlide = true
            )
        )
    } else {
        listOf(
            WelcomeCarouselSlide(
                id = "setup",
                badge = "ΓΡΗΓΟΡΗ ΡΥΘΜΙΣΗ",
                title = "Καλώς ήρθατε στο Finsay",
                subtitle = "Έτοιμο σε 1 λεπτό.",
                bullets = listOf(
                    "Προσθήκη μελών & φωνητικών λέξεων",
                    "Κατηγορίες & μηνιαία όρια (Budget)",
                    "Επιλογή προεπιλεγμένου πληρωτή"
                ),
                icon = Icons.Default.AccountBalanceWallet,
                gradientStart = Color(0xFF065F46),
                gradientEnd = Color(0xFF10B981),
                accentColor = Emerald700
            ),
            WelcomeCarouselSlide(
                id = "entry",
                badge = "ΑΜΕΣΗ ΚΑΤΑΧΩΡΗΣΗ",
                title = "Καταγραφή σε Δευτερόλεπτα",
                subtitle = "Με το χέρι ή τη φωνή.",
                bullets = listOf(
                    "Ελληνική φωνητική καταχώρηση (15 δωρεάν/μήνα)",
                    "Επιμερισμός μελών & πολλαπλοί πληρωτές",
                    "Γρήγορα φίλτρα μήνα, κατηγορίας & μέλους"
                ),
                icon = Icons.Default.Mic,
                gradientStart = Color(0xFF0369A1),
                gradientEnd = Color(0xFF0EA5E9),
                accentColor = Color(0xFF0284C7)
            ),
            WelcomeCarouselSlide(
                id = "insights",
                badge = "ΕΛΕΓΧΟΣ & ΣΥΓΧΡΟΝΙΣΜΟΣ",
                title = "Στατιστικά & Κοινή Χρήση",
                subtitle = "Καθαρή εικόνα σε πραγματικό χρόνο.",
                bullets = listOf(
                    "Γραφήματα, όρια & εκκαθάριση οφειλών",
                    "Πάγιες υπενθυμίσεις & στόχοι αποταμίευσης",
                    "Κοινοποίηση εξοδολογίου με ρόλους"
                ),
                icon = Icons.Default.PieChart,
                gradientStart = Color(0xFF4338CA),
                gradientEnd = Color(0xFF6366F1),
                accentColor = Color(0xFF4F46E5)
            ),
            WelcomeCarouselSlide(
                id = "pro",
                badge = "FINSAY PRO",
                title = "Πλήρης Αυτοματισμός",
                subtitle = "Ξεκινήστε δωρεάν, αναβαθμίστε όποτε θέλετε.",
                bullets = listOf(
                    "Απεριόριστη φωνή & σάρωση αποδείξεων",
                    "Αυτόματη ανάγνωση τραπεζικών Push",
                    "Πολλαπλά εξοδολόγια & εξαγωγή PDF/CSV"
                ),
                icon = Icons.Default.WorkspacePremium,
                gradientStart = Color(0xFFB45309),
                gradientEnd = Color(0xFFF59E0B),
                accentColor = Amber700,
                isProSlide = true
            )
        )
    }
}
