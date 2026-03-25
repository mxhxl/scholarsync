package com.scholarsync.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.scholarsync.navigation.NavRoutes
import com.scholarsync.ui.theme.NavBar

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, NavRoutes.Home.route),
        BottomNavItem("Discover", Icons.Filled.Explore, Icons.Outlined.Explore, NavRoutes.Discover.route),
        BottomNavItem("Library", Icons.Filled.LibraryBooks, Icons.Outlined.LibraryBooks, NavRoutes.Library.route),
        BottomNavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, NavRoutes.Settings.route)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavBar,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Divider(color = Color.White.copy(alpha = 0.1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed = interactionSource.collectIsPressedAsState().value
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onNavigate(item.route) }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = when {
                                isPressed -> Color.White
                                else -> Color.White.copy(alpha = 0.85f)
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
