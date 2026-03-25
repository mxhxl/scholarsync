package com.scholarsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Handler
import android.os.Looper
import com.scholarsync.api.FeedApi
import com.scholarsync.api.ProfileApi
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.components.PrimaryButton
import com.scholarsync.ui.theme.*

@Composable
fun ProfileSetupStep3Screen(
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    val profileApi = remember { ProfileApi() }
    val feedApi = remember { FeedApi() }

    var digestTime by remember { mutableStateOf("6:00 AM") }
    var alertSensitivity by remember { mutableStateOf("Medium") }
    var highPriorityEnabled by remember { mutableStateOf(true) }
    var researchOverlapEnabled by remember { mutableStateOf(true) }
    var trendingTopicsEnabled by remember { mutableStateOf(false) }
    var collaborationEnabled by remember { mutableStateOf(false) }
    var emailSummary by remember { mutableStateOf("Weekly") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Header
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(40.dp))
                Text(
                    text = "Step 3 of 3",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(40.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Gray100)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Primary)
                )
            }
        }

        // Title
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "Notification preferences",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Customize how we keep you updated.",
                fontSize = 14.sp,
                color = Gray500
            )
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Daily Digest
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daily Paper Digest", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Scheduled delivery time", fontSize = 12.sp, color = Gray500)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Primary.copy(alpha = 0.05f))
                        .border(1.dp, Primary.copy(alpha = 0.1f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(digestTime, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Alert Sensitivity
            Text("Overlap Alert Sensitivity", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Gray100)
                    .padding(4.dp)
            ) {
                listOf("Low", "Medium", "High").forEach { level ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (alertSensitivity == level) Primary else Color.Transparent)
                            .clickable { alertSensitivity = level }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = level,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (alertSensitivity == level) Color.White else Gray500
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle options
            NotificationToggle(
                title = "High priority papers",
                description = "Papers matching your core research profile",
                enabled = highPriorityEnabled,
                onToggle = { highPriorityEnabled = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            NotificationToggle(
                title = "Research overlap",
                description = "Alert when a new study overlaps yours",
                enabled = researchOverlapEnabled,
                onToggle = { researchOverlapEnabled = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            NotificationToggle(
                title = "Trending topics",
                description = "Daily breakthroughs in your wider field",
                enabled = trendingTopicsEnabled,
                onToggle = { trendingTopicsEnabled = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            NotificationToggle(
                title = "Collaboration suggestions",
                description = "Connect with authors on similar topics",
                enabled = collaborationEnabled,
                onToggle = { collaborationEnabled = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Email Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Email Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emailSummary, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Primary)
                    Icon(Icons.Default.ExpandMore, null, tint = Primary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Preview
            Text(
                text = "NOTIFICATION PREVIEW",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Gray400,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Gray50)
                    .border(1.dp, Gray100, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SCHOLARSYNC", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                        Text("now", fontSize = 10.sp, color = Gray400)
                    }
                    Text("3 high priority papers in your feed", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Gray800)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Bottom buttons - above system nav bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.98f))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    fontSize = 13.sp,
                    color = Error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            PrimaryButton(
                text = if (isLoading) "Saving..." else "Finish Setup",
                enabled = !isLoading,
                onClick = {
                    val token = sessionManager.getAccessToken()
                    if (token == null) {
                        onFinish()
                        return@PrimaryButton
                    }
                    isLoading = true
                    errorMessage = null
                    val baseUrl = sessionManager.getApiBaseUrl()

                    // Gather accumulated data from all steps
                    val role = sessionManager.getOnboardingData("role") ?: "PhD Student"
                    val interests = sessionManager.getOnboardingList("interests")
                    val keywords = sessionManager.getOnboardingList("keywords")
                    val authors = sessionManager.getOnboardingList("authors")

                    // Convert digest time display to HH:MM
                    val digestHHMM = convertDigestTimeToHHMM(digestTime)

                    // 1) Call profile setup
                    profileApi.setupProfile(
                        baseUrl = baseUrl,
                        bearerToken = token,
                        researchField = role,
                        topics = interests,
                        keywords = keywords,
                        authorsFollowing = authors
                    ) { profileResult ->
                        // 2) Call notification preferences (regardless of profile result)
                        profileApi.updateNotificationPreferences(
                            baseUrl = baseUrl,
                            bearerToken = token,
                            digestTime = digestHHMM,
                            overlapSensitivity = alertSensitivity.lowercase(),
                            enableHighPriority = highPriorityEnabled,
                            enableOverlapAlerts = researchOverlapEnabled,
                            enableEmail = emailSummary != "Never"
                        ) { prefsResult ->
                            Handler(Looper.getMainLooper()).post {
                                isLoading = false
                                if (profileResult.isFailure) {
                                    errorMessage = profileResult.exceptionOrNull()?.message ?: "Profile setup failed"
                                } else {
                                    // 3) Fire-and-forget feed refresh so papers start loading
                                    //    before the user arrives at HomeScreen
                                    feedApi.refreshFeed(
                                        baseUrl = baseUrl,
                                        bearerToken = token
                                    ) { /* best-effort: HomeScreen will retry if needed */ }
                                    sessionManager.clearOnboardingData()
                                    onFinish()
                                }
                            }
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = {
                    // Skip still saves profile with defaults if token exists
                    val token = sessionManager.getAccessToken()
                    if (token != null) {
                        val baseUrl = sessionManager.getApiBaseUrl()
                        val role = sessionManager.getOnboardingData("role") ?: "PhD Student"
                        val interests = sessionManager.getOnboardingList("interests")
                        val keywords = sessionManager.getOnboardingList("keywords")
                        val authors = sessionManager.getOnboardingList("authors")
                        profileApi.setupProfile(
                            baseUrl = baseUrl,
                            bearerToken = token,
                            researchField = role,
                            topics = interests,
                            keywords = keywords,
                            authorsFollowing = authors
                        ) { result ->
                            result.onSuccess {
                                // Fire-and-forget feed refresh so papers start loading
                                feedApi.refreshFeed(baseUrl = baseUrl, bearerToken = token) { }
                            }
                        }
                    }
                    sessionManager.clearOnboardingData()
                    onSkip()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip - Use defaults", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray400)
            }
        }
    }
}

/** Converts "6:00 AM" → "06:00", "2:30 PM" → "14:30" */
private fun convertDigestTimeToHHMM(displayTime: String): String {
    return try {
        val parts = displayTime.uppercase().replace(".", "").trim().split(" ")
        val timePart = parts[0].split(":")
        var hour = timePart[0].toInt()
        val minute = timePart.getOrElse(1) { "00" }.toInt()
        val ampm = parts.getOrElse(1) { "AM" }
        if (ampm == "PM" && hour != 12) hour += 12
        if (ampm == "AM" && hour == 12) hour = 0
        "%02d:%02d".format(hour, minute)
    } catch (_: Exception) {
        "06:00"
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(description, fontSize = 12.sp, color = Gray500)
        }
        Spacer(modifier = Modifier.width(16.dp))
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
