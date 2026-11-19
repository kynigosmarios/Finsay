package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald700

@Composable
fun OnboardingCarouselDialog(
    onDismiss: () -> Unit,
    onAddMember: (() -> Unit)? = null,
    onAddCategory: (() -> Unit)? = null,
    onSeedCategories: (() -> Unit)? = null,
    appLanguage: String = "EN",
    modifier: Modifier = Modifier
) {
    val isEnglish = appLanguage.equals("EN", ignoreCase = true)
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Quick Start, 1 = Per Tab, 2 = Features

    val quickStartSteps = remember(isEnglish) { GuideContentData.buildQuickStartSteps(isEnglish) }
    val tabSections = remember(isEnglish) { GuideContentData.buildTabGuideSections(isEnglish) }
    val smartFeatures = remember(isEnglish) { GuideContentData.buildSmartFeatureItems(isEnglish) }

    // All items start collapsed so the Guide page stays calm and scannable
    val expandedSteps = remember { mutableStateMapOf<Int, Boolean>() }
    val expandedTabs = remember { mutableStateMapOf<String, Boolean>() }
    val expandedFeatures = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("onboarding_carousel_dialog")
    ) {
        // Consistent In-App Page Header Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("onboarding_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isEnglish) "Back" else "Επιστροφή",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Emerald700.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isEnglish) "App Guide" else "Οδηγίες",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3-Segment Section Switcher (Quick Start | By Tab | Features)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GuideSegmentPill(
                    label = if (isEnglish) "Start" else "1α Βήματα",
                    icon = Icons.Default.PlayArrow,
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    testTag = "guide_tab_quickstart",
                    modifier = Modifier.weight(1f)
                )
                GuideSegmentPill(
                    label = if (isEnglish) "By Tab" else "Καρτέλες",
                    icon = Icons.Default.Dashboard,
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    testTag = "guide_tab_screens",
                    modifier = Modifier.weight(1f)
                )
                GuideSegmentPill(
                    label = if (isEnglish) "Features" else "Λειτουργίες",
                    icon = Icons.Default.AutoAwesome,
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    testTag = "guide_tab_features",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Scrollable Section Content with smooth directional transitions
        AnimatedContent(
            targetState = selectedSection,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(FinsayMotion.TAB_DURATION_MS)) { if (forward) it / 8 else -it / 8 } +
                    fadeIn(tween(FinsayMotion.TAB_DURATION_MS))) togetherWith
                    (slideOutHorizontally(tween(FinsayMotion.TAB_DURATION_MS - 30)) { if (forward) -it / 10 else it / 10 } +
                        fadeOut(tween(FinsayMotion.TAB_DURATION_MS - 40)))
            },
            label = "guideSectionTransition",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { sectionIndex ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (sectionIndex) {
                    0 -> {
                        item(key = "qs_hero", contentType = "hero") {
                            GuideWelcomeHeroCard(isEnglish = isEnglish)
                        }
                        item(key = "qs_toggle_all", contentType = "intro") {
                            val allStepsExpanded = quickStartSteps.isNotEmpty() && quickStartSteps.all { expandedSteps[it.number] == true }
                            GuideExpandCollapseAllRow(
                                allExpanded = allStepsExpanded,
                                isEnglish = isEnglish,
                                onToggleAll = {
                                    val nextState = !allStepsExpanded
                                    quickStartSteps.forEach { expandedSteps[it.number] = nextState }
                                }
                            )
                        }
                        items(quickStartSteps, key = { "qs_${it.number}" }, contentType = { "qs_step" }) { step ->
                            QuickStartStepCard(
                                step = step,
                                isExpanded = expandedSteps[step.number] == true,
                                onToggle = { expandedSteps[step.number] = !(expandedSteps[step.number] == true) },
                                isEnglish = isEnglish,
                                onAddMember = onAddMember,
                                onAddCategory = onAddCategory,
                                onSeedCategories = onSeedCategories
                            )
                        }
                        item(key = "qs_footer", contentType = "footer") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { selectedSection = 1 },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("guide_explore_tabs_button")
                                ) {
                                    Text(
                                        text = if (isEnglish) "Explore what each tab does" else "Δείτε τι κάνει κάθε καρτέλα",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = Emerald700
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Button(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("onboarding_next_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "Get Started in Finsay" else "Ξεκινήστε στην εφαρμογή",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        item(key = "tabs_intro", contentType = "intro") {
                            val allExpanded = tabSections.isNotEmpty() && tabSections.all { expandedTabs[it.id] == true }
                            GuideExpandCollapseAllRow(
                                allExpanded = allExpanded,
                                isEnglish = isEnglish,
                                onToggleAll = {
                                    val nextState = !allExpanded
                                    tabSections.forEach { expandedTabs[it.id] = nextState }
                                }
                            )
                        }

                        items(tabSections, key = { "tab_${it.id}" }, contentType = { "tab_accordion" }) { section ->
                            TabGuideAccordionCard(
                                section = section,
                                isExpanded = expandedTabs[section.id] == true,
                                onToggle = { expandedTabs[section.id] = !(expandedTabs[section.id] == true) }
                            )
                        }
                    }

                    else -> {
                        item(key = "features_toggle_all", contentType = "intro") {
                            val allFeaturesExpanded = smartFeatures.isNotEmpty() && smartFeatures.all { expandedFeatures[it.id] == true }
                            GuideExpandCollapseAllRow(
                                allExpanded = allFeaturesExpanded,
                                isEnglish = isEnglish,
                                onToggleAll = {
                                    val nextState = !allFeaturesExpanded
                                    smartFeatures.forEach { expandedFeatures[it.id] = nextState }
                                }
                            )
                        }
                        items(smartFeatures, key = { "feat_${it.id}" }, contentType = { "smart_feature" }) { feature ->
                            SmartFeatureGuideCard(
                                feature = feature,
                                isExpanded = expandedFeatures[feature.id] == true,
                                onToggle = { expandedFeatures[feature.id] = !(expandedFeatures[feature.id] == true) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSegmentPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) Emerald700 else Color.Transparent,
        animationSpec = tween(200),
        label = "guidePillBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "guidePillContent"
    )
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = bgColor,
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GuideWelcomeHeroCard(isEnglish: Boolean) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF065F46), Color(0xFF059669), Color(0xFF10B981))
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isEnglish) "Welcome to Finsay!" else "Καλώς ήρθατε στο Finsay!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = if (isEnglish) {
                        "Follow these 5 simple steps as soon as you sign in so your household budget, voice entry, reminders, and statistics work effortlessly from day one."
                    } else {
                        "Ακολουθήστε αυτά τα 5 απλά βήματα με το που μπείτε στην εφαρμογή, ώστε οι καταχωρήσεις, η φωνητική αναγνώριση, οι στόχοι και τα στατιστικά να δουλεύουν άψογα από την πρώτη μέρα."
                    },
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
        }
    }
}

@Composable
private fun GuideExpandCollapseAllRow(
    allExpanded: Boolean,
    isEnglish: Boolean,
    onToggleAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Emerald700.copy(alpha = 0.12f),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onToggleAll() }
        ) {
            Text(
                text = if (allExpanded) {
                    if (isEnglish) "Collapse all" else "Σύμπτυξη όλων"
                } else {
                    if (isEnglish) "Expand all" else "Ανάπτυξη όλων"
                },
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Emerald700,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickStartStepCard(
    step: QuickStartStep,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isEnglish: Boolean,
    onAddMember: (() -> Unit)?,
    onAddCategory: (() -> Unit)?,
    onSeedCategories: (() -> Unit)?
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(220),
        label = "quickStartChevron_${step.number}"
    )
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isExpanded) step.accentColor.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("guide_step_header_${step.number}")
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(step.accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${step.number}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = step.title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = step.whereLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = step.accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = step.accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = step.accentColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = FinsayMotion.ExpandEnter,
                exit = FinsayMotion.ShrinkExit
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Text(
                        text = step.description,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Optional direct action buttons for Steps 1 & 2 when permitted
                    when (step.actionType) {
                        "ADD_MEMBER" -> if (onAddMember != null) {
                            OutlinedButton(
                                onClick = onAddMember,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, step.accentColor.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "+ Add Member Now" else "+ Προσθήκη Μέλους τώρα",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = step.accentColor
                                )
                            }
                        }
                        "SEED_CATEGORIES" -> if (onSeedCategories != null || onAddCategory != null) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (onSeedCategories != null) {
                                    Button(
                                        onClick = onSeedCategories,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = step.accentColor),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Add Standard Categories" else "Προσθήκη Βασικών Κατηγοριών",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                if (onAddCategory != null) {
                                    OutlinedButton(
                                        onClick = onAddCategory,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, step.accentColor.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "+ New Category" else "+ Νέα Κατηγορία",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = step.accentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabGuideAccordionCard(
    section: TabGuideSection,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isExpanded) section.accentColor.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = section.accentColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = null,
                            tint = section.accentColor,
                            modifier = Modifier.size(21.dp)
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
                            text = section.title,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = "${section.locationBadge} • ${section.subtitle}",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                val chevronRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    animationSpec = tween(220),
                    label = "guideChevron"
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = FinsayMotion.ExpandEnter,
                exit = FinsayMotion.ShrinkExit
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    section.capabilities.forEach { bullet ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = section.accentColor,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = bullet,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartFeatureGuideCard(
    feature: SmartFeatureItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(220),
        label = "smartFeatureChevron_${feature.id}"
    )
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isExpanded) feature.accentColor.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("guide_feature_header_${feature.id}")
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = feature.accentColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = feature.icon,
                            contentDescription = null,
                            tint = feature.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = feature.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = FinsayMotion.ExpandEnter,
                exit = FinsayMotion.ShrinkExit
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Text(
                        text = feature.summary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        feature.highlights.forEach { hl ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = feature.accentColor,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = hl,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (!feature.tipText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = feature.accentColor.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, feature.accentColor.copy(alpha = 0.22f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = feature.accentColor,
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(15.dp)
                                )
                                Text(
                                    text = feature.tipText,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
