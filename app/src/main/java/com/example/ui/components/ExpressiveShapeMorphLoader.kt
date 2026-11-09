package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ExpressiveShapeMorphLoader(
    isRefreshing: Boolean,
    pullFraction: Float = 1f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shapeMorphTransition")

    // Continuous morph phase from 0 to 3
    val morphPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "morphPhase"
    )

    // Center icon continuous spin
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "iconRotation"
    )

    // Dynamic interpolation between Circle (0), Squircle (1), Pill (2), back to Circle (3)
    val (targetWidth, targetHeight, targetRadius, targetColor, targetRotation) = when {
        !isRefreshing -> {
            // Dragging down state: morphs smoothly based on pullFraction
            val w = 38.dp + (8.dp * pullFraction.coerceIn(0f, 1f))
            val h = 38.dp + (8.dp * pullFraction.coerceIn(0f, 1f))
            val r = (24.dp - (8.dp * pullFraction.coerceIn(0f, 1f)))
            val c = MaterialTheme.colorScheme.primaryContainer
            val rot = pullFraction * 180f
            Tuple5(w, h, r, c, rot)
        }
        morphPhase < 1f -> {
            // Circle -> Squircle
            val fraction = morphPhase
            val w = 44.dp + ((48.dp - 44.dp) * fraction)
            val h = 44.dp + ((48.dp - 44.dp) * fraction)
            val r = 22.dp + ((14.dp - 22.dp) * fraction)
            val c = MaterialTheme.colorScheme.primaryContainer
            val rot = fraction * 45f
            Tuple5(w, h, r, c, rot)
        }
        morphPhase < 2f -> {
            // Squircle -> Pill Capsule
            val fraction = morphPhase - 1f
            val w = 48.dp + ((60.dp - 48.dp) * fraction)
            val h = 48.dp + ((38.dp - 48.dp) * fraction)
            val r = 14.dp + ((19.dp - 14.dp) * fraction)
            val c = MaterialTheme.colorScheme.secondaryContainer
            val rot = 45f + (fraction * 45f)
            Tuple5(w, h, r, c, rot)
        }
        else -> {
            // Pill Capsule -> Circle
            val fraction = morphPhase - 2f
            val w = 60.dp + ((44.dp - 60.dp) * fraction)
            val h = 38.dp + ((44.dp - 38.dp) * fraction)
            val r = 19.dp + ((22.dp - 19.dp) * fraction)
            val c = MaterialTheme.colorScheme.tertiaryContainer
            val rot = 90f + (fraction * 90f)
            Tuple5(w, h, r, c, rot)
        }
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "animWidth"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "animHeight"
    )
    val animatedRadius by animateDpAsState(
        targetValue = targetRadius,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "animRadius"
    )
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "animColor"
    )

    Surface(
        modifier = modifier
            .size(width = animatedWidth, height = animatedHeight)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(animatedRadius),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )
            .rotate(if (isRefreshing) targetRotation else targetRotation),
        shape = RoundedCornerShape(animatedRadius),
        color = animatedColor,
        border = BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isRefreshing) Icons.Rounded.Refresh else Icons.Rounded.AutoAwesome,
                contentDescription = "Loading",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (isRefreshing) iconRotation else 0f)
            )
        }
    }
}

/**
 * Material 3 Expressive Pull-To-Refresh container that reveals the shape-morphing loader.
 */
@Composable
fun ExpressivePullRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshTriggerDistance: Dp = 80.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val triggerPx = with(density) { refreshTriggerDistance.toPx() }

    var pullDistance by remember { mutableFloatStateOf(0f) }

    val animatedDistance by animateFloatAsState(
        targetValue = if (isRefreshing) triggerPx else pullDistance,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pullOffset"
    )

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // When scrolling up and we have pulled distance, consume the scroll
                if (source == NestedScrollSource.UserInput && available.y < 0 && pullDistance > 0) {
                    val consumed = available.y
                    pullDistance = (pullDistance + consumed).coerceAtLeast(0f)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // When pulling down past top of list
                if (source == NestedScrollSource.UserInput && available.y > 0 && !isRefreshing) {
                    val delta = available.y * 0.45f // Damping factor
                    pullDistance = (pullDistance + delta).coerceAtMost(triggerPx * 1.5f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullDistance >= triggerPx && !isRefreshing) {
                    onRefresh()
                }
                pullDistance = 0f
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullDistance = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Main Screen Content
        content()

        // Expressive Floating Morphing Indicator
        if (animatedDistance > 10f || isRefreshing) {
            val pullFraction = (animatedDistance / triggerPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, (animatedDistance - with(density) { 30.dp.toPx() }).roundToInt().coerceAtLeast(16)) }
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                ExpressiveShapeMorphLoader(
                    isRefreshing = isRefreshing,
                    pullFraction = pullFraction,
                    modifier = Modifier.scale(0.8f + (0.2f * pullFraction))
                )
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E
)
