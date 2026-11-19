package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize

/**
 * Shared motion tokens and reusable animation wrappers for fluid, flagship-grade
 * transitions across screens, expandable sections, and modal dialogs.
 */
object FinsayMotion {
    const val TAB_DURATION_MS = 210
    const val SUBPAGE_ENTER_MS = 250
    const val SUBPAGE_EXIT_MS = 200
    const val PROGRESS_DURATION_MS = 450

    fun <T> contentSizeSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val ExpandEnter: EnterTransition =
        expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            expandFrom = Alignment.Top
        ) + fadeIn(animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing))

    val ShrinkExit: ExitTransition =
        shrinkVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing))

    val BannerEnter: EnterTransition =
        expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            expandFrom = Alignment.Top
        ) + fadeIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))

    val BannerExit: ExitTransition =
        shrinkVertically(
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(durationMillis = 150))

    val ProgressSpec = tween<Float>(
        durationMillis = PROGRESS_DURATION_MS,
        easing = FastOutSlowInEasing
    )

    val SizeSpringIntSize = spring<IntSize>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

/**
 * Smooth entrance wrapper for custom Compose [androidx.compose.ui.window.Dialog] content.
 * Provides a soft scale + gentle vertical lift + fade so modals open cleanly without snapping.
 */
@Composable
fun SmoothDialogContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing)
        ) + scaleIn(
            initialScale = 0.94f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + slideInVertically(
            initialOffsetY = { (it * 0.04f).toInt().coerceAtMost(36) },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 130)
        ) + scaleOut(
            targetScale = 0.96f,
            animationSpec = tween(durationMillis = 130)
        ) + slideOutVertically(
            targetOffsetY = { (it * 0.03f).toInt().coerceAtMost(24) },
            animationSpec = tween(durationMillis = 130)
        )
    ) {
        content()
    }
}
