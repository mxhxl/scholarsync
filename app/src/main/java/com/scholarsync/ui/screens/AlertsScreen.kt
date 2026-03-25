package com.scholarsync.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.api.AlertsApi
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.components.CategoryChip
import com.scholarsync.ui.theme.*

data class Alert(
    val id: String,
    val title: String,
    val description: String,
    val time: String,
    val isNew: Boolean,
    val category: String,
    val topic: String = "all"
)

private val filterTopics = listOf(
    "All Topics",
    "Machine Learning",
    "Quantum AI",
    "NLP",
    "Computer Vision",
    "App Updates"
)

@Composable
fun AlertsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val alertsApi = remember { AlertsApi() }

    var selectedFilter by remember { mutableStateOf("All Topics") }
    var allAlerts by remember { mutableStateOf<List<Alert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch alerts from backend
    LaunchedEffect(Unit) {
        val baseUrl = sessionManager.getApiBaseUrl()
        if (sessionManager.isLoggedIn()) {
            alertsApi.getAlerts(baseUrl = baseUrl, limit = 50) { result ->
                Handler(Looper.getMainLooper()).post {
                    val fetched = result.getOrNull()
                    if (fetched != null && fetched.items.isNotEmpty()) {
                        allAlerts = fetched.items.map { apiAlert ->
                            val topic = extractTopicFromAlert(apiAlert.title, apiAlert.description)
                            val category = when (apiAlert.type) {
                                "new_paper", "followed_author" -> "new_papers"
                                else -> "updates"
                            }
                            Alert(
                                id = apiAlert.id,
                                title = apiAlert.paper?.title ?: apiAlert.title,
                                description = apiAlert.description,
                                time = formatRelativeTime(apiAlert.createdAt),
                                isNew = !apiAlert.isRead,
                                category = category,
                                topic = topic
                            )
                        }
                    } else {
                        // Fallback to mock data when backend is unavailable
                        allAlerts = mockAlerts
                    }
                    isLoading = false
                }
            }
        } else {
            allAlerts = mockAlerts
            isLoading = false
        }
    }

    val filteredAlerts = if (selectedFilter == "All Topics") {
        allAlerts
    } else {
        allAlerts.filter { it.topic == selectedFilter }
    }

    val newPaperAlerts = filteredAlerts.filter { it.category == "new_papers" }
    val updateAlerts = filteredAlerts.filter { it.category == "updates" }
    val unreadCount = filteredAlerts.count { it.isNew }

    Scaffold(
        topBar = {
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
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 24.dp)
                    .padding(top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Notifications",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
        ) {
            // Unread count bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.5f))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .drawBehind {
                        drawLine(
                            color = Gray200,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$unreadCount Unread",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Gray500
                )
                TextButton(onClick = {
                    // Mark all as read via API
                    val baseUrl = sessionManager.getApiBaseUrl()
                    allAlerts.filter { it.isNew }.forEach { alert ->
                        alertsApi.markAlertRead(baseUrl, alert.id) { }
                    }
                    allAlerts = allAlerts.map { it.copy(isNew = false) }
                }) {
                    Text(
                        "Mark all read",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                }
            }

            // Filter chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filterTopics.forEach { topic ->
                    CategoryChip(
                        text = topic,
                        selected = selectedFilter == topic,
                        onClick = { selectedFilter = topic }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (newPaperAlerts.isNotEmpty()) {
                        item {
                            AlertSectionHeader(
                                icon = Icons.Default.AutoStories,
                                title = "New Papers",
                                iconTint = Primary
                            )
                        }
                        items(newPaperAlerts) { alert ->
                            AlertItem(alert = alert)
                        }
                    }

                    if (updateAlerts.isNotEmpty()) {
                        item {
                            AlertSectionHeader(
                                icon = Icons.Default.Update,
                                title = "App Updates",
                                iconTint = Gray400
                            )
                        }
                        items(updateAlerts) { alert ->
                            AlertItem(alert = alert)
                        }
                    }

                    if (filteredAlerts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No notifications for \"$selectedFilter\"",
                                    fontSize = 14.sp,
                                    color = Gray400
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extracts a human-readable topic from backend alert title/description.
 */
private fun extractTopicFromAlert(title: String, description: String): String {
    // Backend format: "New paper on {topic}"
    val titleMatch = Regex("New paper on (.+)", RegexOption.IGNORE_CASE).find(title)
    if (titleMatch != null) return titleMatch.groupValues[1].trim()

    // Backend format: "New paper by {author}" for followed-author alerts
    val authorMatch = Regex("New paper by (.+)", RegexOption.IGNORE_CASE).find(title)
    if (authorMatch != null) return "all"

    // Backend format: matching your "{topic}" interest
    val descMatch = Regex("matching your [\"'](.+?)[\"']", RegexOption.IGNORE_CASE).find(description)
    if (descMatch != null) return descMatch.groupValues[1].trim()

    return "all"
}

/**
 * Converts an ISO 8601 timestamp to a relative time string like "2h ago".
 */
private fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val instant = java.time.Instant.parse(isoTimestamp)
        val now = java.time.Instant.now()
        val diff = java.time.Duration.between(instant, now)
        when {
            diff.toMinutes() < 1 -> "Just now"
            diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
            diff.toHours() < 24 -> "${diff.toHours()}h ago"
            diff.toDays() < 2 -> "Yesterday"
            diff.toDays() < 7 -> "${diff.toDays()}d ago"
            else -> "${diff.toDays() / 7}w ago"
        }
    } catch (_: Exception) {
        try {
            val dt = java.time.LocalDateTime.parse(isoTimestamp.replace(" ", "T"))
            val now = java.time.LocalDateTime.now()
            val diff = java.time.Duration.between(dt, now)
            when {
                diff.toMinutes() < 1 -> "Just now"
                diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
                diff.toHours() < 24 -> "${diff.toHours()}h ago"
                diff.toDays() < 2 -> "Yesterday"
                else -> "${diff.toDays()}d ago"
            }
        } catch (_: Exception) {
            isoTimestamp
        }
    }
}

/** Mock data — shown when backend is unreachable or user is not logged in. */
private val mockAlerts = listOf(
    Alert(
        id = "1",
        title = "Large Language Models in Quantum Physics",
        description = "A new breakthrough paper matching your \"Quantum AI\" watch-list has been published.",
        time = "2m ago",
        isNew = true,
        category = "new_papers",
        topic = "Quantum AI"
    ),
    Alert(
        id = "2",
        title = "Neural Architecture Search Efficiency",
        description = "New findings on reducing compute costs for NAS by up to 40%.",
        time = "1h ago",
        isNew = true,
        category = "new_papers",
        topic = "Machine Learning"
    ),
    Alert(
        id = "4",
        title = "Transformer Attention Mechanisms Revisited",
        description = "New paper explores sparse attention in long-sequence NLP tasks with 3× throughput gains.",
        time = "3h ago",
        isNew = true,
        category = "new_papers",
        topic = "NLP"
    ),
    Alert(
        id = "5",
        title = "Diffusion Models for Medical Imaging",
        description = "State-of-the-art diffusion-based segmentation now surpasses CNNs on CT scan benchmarks.",
        time = "5h ago",
        isNew = false,
        category = "new_papers",
        topic = "Computer Vision"
    ),
    Alert(
        id = "3",
        title = "New AI Model Integrated",
        description = "ScholarSync now uses updated models for even more accurate research summaries.",
        time = "Yesterday",
        isNew = false,
        category = "updates",
        topic = "App Updates"
    )
)

@Composable
private fun AlertSectionHeader(
    icon: ImageVector,
    title: String,
    iconTint: Color
) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Gray400,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun AlertItem(alert: Alert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isNew) Color.White else Color.White.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (alert.isNew) 2.dp else 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (alert.isNew) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(80.dp)
                        .background(Primary)
                )
            }
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = alert.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alert.isNew) Primary else Gray700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.time,
                        fontSize = 10.sp,
                        color = Gray400
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.description,
                    fontSize = 14.sp,
                    color = Gray600,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
