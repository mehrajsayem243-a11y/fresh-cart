package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.NavTab

@Composable
fun ExpressiveFloatingNavBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // XL Floating Pill Capsule in Deep Black
        Surface(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.50f),
                    ambientColor = Color.Black.copy(alpha = 0.35f)
                ),
            shape = CircleShape,
            color = Color(0xFF0B0B0E), // Deep Black Filled Container
            tonalElevation = 8.dp,
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFF27272A)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tab 1: Groceries
                FloatingNavTabItem(
                    selected = currentTab == NavTab.GROCERIES,
                    onClick = { onTabSelected(NavTab.GROCERIES) },
                    icon = Icons.Rounded.ShoppingCart,
                    label = "Groceries",
                    testTag = "nav_floating_groceries"
                )

                // Tab 2: Pantry
                FloatingNavTabItem(
                    selected = currentTab == NavTab.PANTRY,
                    onClick = { onTabSelected(NavTab.PANTRY) },
                    icon = Icons.Rounded.Kitchen,
                    label = "Pantry",
                    testTag = "nav_floating_pantry"
                )

                // Tab 3: Collaborators / Share
                FloatingNavTabItem(
                    selected = currentTab == NavTab.COLLABORATORS,
                    onClick = { onTabSelected(NavTab.COLLABORATORS) },
                    icon = Icons.Rounded.Group,
                    label = "Share",
                    testTag = "nav_floating_collaborators"
                )
            }
        }
    }
}

@Composable
private fun FloatingNavTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "navTabContainerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            Color(0xFF9E9EA7)
        },
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "navTabContentColor"
    )

    // XL Tab Item (height 54dp, icon 26dp)
    Surface(
        modifier = modifier
            .height(54.dp)
            .pressScale(interactionSource)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .testTag(testTag),
        shape = CircleShape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )

            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        expandHorizontally(spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                        shrinkHorizontally(spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
