package com.scholarsync.ui.screens

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.api.StatsApi
import com.scholarsync.api.UserStatsResponse
import com.scholarsync.data.SessionManager
import com.scholarsync.navigation.NavRoutes
import com.scholarsync.ui.theme.*
import java.io.File

@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    val displayName = sessionManager.getDisplayName() ?: "Researcher"
    var pushNotificationsEnabled by remember { mutableStateOf(true) }

    // Profile picture
    var profileBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        sessionManager.getProfilePicturePath()?.let { path ->
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(path)?.let { bmp ->
                    profileBitmap = bmp.asImageBitmap()
                }
            }
        }
    }

    // Real stats from backend
    var stats by remember { mutableStateOf<UserStatsResponse?>(null) }
    LaunchedEffect(Unit) {
        val baseUrl = sessionManager.getApiBaseUrl()
        val mainHandler = Handler(Looper.getMainLooper())
        StatsApi().getUserStats(baseUrl) { result ->
            mainHandler.post {
                result.onSuccess { stats = it }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray100)
    ) {
            // Header: extends behind status bar; content inset below it
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .drawBehind {
                        drawLine(
                            color = Gray100,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile section
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileBitmap != null) {
                        Image(
                            bitmap = profileBitmap!!,
                            contentDescription = "Profile photo",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = displayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )

                TextButton(onClick = { onNavigate(NavRoutes.EditProfile.route) }) {
                    Text("Edit Profile", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(value = "${stats?.papersRead ?: 0}", label = "Papers Read")
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Gray200))
                    StatItem(value = "${stats?.savedPapers ?: 0}", label = "Saved")
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Gray200))
                    StatItem(value = "${stats?.summaries ?: 0}", label = "Summaries")
                }

                Divider(color = Gray100, modifier = Modifier.padding(vertical = 16.dp))

                // Account section
                SettingsSection(title = "Account") {
                    SettingsNavigationItem(
                        icon = Icons.Default.Settings,
                        title = "API Server",
                        onClick = { onNavigate(NavRoutes.ServerUrl.route) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsToggleItem(
                        icon = Icons.Default.Notifications,
                        title = "Push Notifications",
                        enabled = pushNotificationsEnabled,
                        onToggle = { pushNotificationsEnabled = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsNavigationItem(
                        icon = Icons.Default.Lock,
                        title = "Security & Privacy",
                        onClick = { onNavigate(NavRoutes.SecurityPrivacy.route) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Support section
                SettingsSection(title = "Support") {
                    SettingsNavigationItem(
                        icon = Icons.Default.Help,
                        title = "Help Center",
                        onClick = { onNavigate(NavRoutes.HelpCenter.route) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsItem(
                        icon = Icons.Default.Logout,
                        title = "Sign Out",
                        titleColor = Error,
                        onClick = onSignOut
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Gray400, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Gray400,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
        )
        content()
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Gray50)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Primary)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Gray200
            )
        )
    }
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Gray50)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Primary)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Gray400, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    titleColor: Color = Primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
    }
}
