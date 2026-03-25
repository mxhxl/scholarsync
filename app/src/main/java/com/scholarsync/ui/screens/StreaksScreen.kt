package com.scholarsync.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.scholarsync.api.StreakResponse
import com.scholarsync.api.StreaksApi
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val FireOrange = Color(0xFFFF9500)
private val FireRed = Color(0xFFFF6B35)
private val XpPurple = Color(0xFF8B5CF6)
private val GoalGreen = Color(0xFF22C55E)

@Composable
fun StreaksScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val streaksApi = remember { StreaksApi() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var streak by remember { mutableStateOf<StreakResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch streak data — extracted so it can be called on refresh
    fun fetchStreakData() {
        isLoading = true
        errorMessage = null
        val baseUrl = sessionManager.getApiBaseUrl()
        streaksApi.getStreak(baseUrl) { result ->
            Handler(Looper.getMainLooper()).post {
                result.onSuccess {
                    streak = it
                    isLoading = false
                }.onFailure { error ->
                    errorMessage = error.message
                    isLoading = false
                }
            }
        }
    }

    // Fetch on first load
    LaunchedEffect(Unit) {
        fetchStreakData()
    }

    // Re-fetch every time this screen becomes visible (lifecycle ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fetchStreakData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .drawBehind {
                        drawLine(Gray100, Offset(0f, size.height), Offset(size.width, size.height), 1f)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Reading Streaks", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (errorMessage != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Gray400,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Couldn't load streaks",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        errorMessage ?: "Unknown error",
                        fontSize = 13.sp,
                        color = Gray500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { fetchStreakData() },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Retry")
                    }
                }
            }
        } else {
            val s = streak ?: StreakResponse()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Hero streak card ────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (s.currentStreak > 0) listOf(FireOrange, FireRed)
                                else listOf(Gray300, Gray400)
                            )
                        )
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${s.currentStreak}",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = if (s.currentStreak == 1) "day streak" else "day streak",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        if (s.currentStreak == 0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Read a paper today to start your streak!",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // ── Stats row ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StreakStatCard(
                        icon = Icons.Default.EmojiEvents,
                        label = "Best Streak",
                        value = "${s.longestStreak} days",
                        iconColor = AccentGold,
                        modifier = Modifier.weight(1f)
                    )
                    StreakStatCard(
                        icon = Icons.Default.Star,
                        label = "Total XP",
                        value = "${s.totalXp}",
                        iconColor = XpPurple,
                        modifier = Modifier.weight(1f)
                    )
                    StreakStatCard(
                        icon = Icons.Default.MenuBook,
                        label = "Papers Read",
                        value = "${s.totalPapersRead}",
                        iconColor = AccentTeal,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── Weekly goal ─────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Flag,
                                    null,
                                    tint = GoalGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Weekly Goal",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }
                            Text(
                                "${s.weeklyGoalProgress}/${s.weeklyGoal}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (s.weeklyGoalProgress >= s.weeklyGoal) GoalGreen else Gray500
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        @Suppress("DEPRECATION")
                        LinearProgressIndicator(
                            progress = (s.weeklyGoalProgress.toFloat() / s.weeklyGoal.coerceAtLeast(1)).coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = GoalGreen,
                            trackColor = Gray100,
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = if (s.weeklyGoalProgress >= s.weeklyGoal) "Goal reached! Keep going!"
                            else "${s.weeklyGoal - s.weeklyGoalProgress} more papers to hit your goal",
                            fontSize = 13.sp,
                            color = Gray500
                        )
                    }
                }

                // ── Today's progress ────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Today", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Primary)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (s.todayPapers > 0) GoalGreen.copy(alpha = 0.1f)
                                        else Gray100
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (s.todayPapers > 0) Icons.Default.CheckCircle
                                    else Icons.Default.RadioButtonUnchecked,
                                    null,
                                    tint = if (s.todayPapers > 0) GoalGreen else Gray400,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    if (s.todayPapers > 0) "${s.todayPapers} paper${if (s.todayPapers > 1) "s" else ""} read today"
                                    else "No papers read yet today",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (s.todayPapers > 0) Primary else Gray600
                                )
                                Text(
                                    if (s.todayPapers > 0) "Great job! Keep it up!"
                                    else "Read a paper to maintain your streak",
                                    fontSize = 13.sp,
                                    color = Gray500
                                )
                            }
                        }
                    }
                }

                // ── Week calendar ───────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("This Week", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Primary)
                        Spacer(Modifier.height(16.dp))

                        val today = LocalDate.now()
                        val weekStart = today.with(DayOfWeek.MONDAY)
                        val readDateSet = s.readDates.toSet()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (i in 0..6) {
                                val day = weekStart.plusDays(i.toLong())
                                val dayLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                val isRead = readDateSet.contains(day.toString())
                                val isToday = day == today
                                val isFuture = day.isAfter(today)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        dayLabel.take(2),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isToday) Primary else Gray500
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isRead -> FireOrange
                                                    isToday -> Primary.copy(alpha = 0.1f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .then(
                                                if (isToday && !isRead) Modifier.border(2.dp, Primary, CircleShape)
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            isRead -> Icon(
                                                Icons.Default.LocalFireDepartment,
                                                null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            isFuture -> Text(
                                                "${day.dayOfMonth}",
                                                fontSize = 12.sp,
                                                color = Gray300
                                            )
                                            else -> Text(
                                                "${day.dayOfMonth}",
                                                fontSize = 12.sp,
                                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isToday) Primary else Gray500
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Achievements ────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Achievements", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Primary)
                        Spacer(Modifier.height(16.dp))

                        AchievementRow("First Paper", "Read your first paper", s.totalPapersRead >= 1, Icons.Default.AutoStories)
                        AchievementRow("3-Day Streak", "Read papers 3 days in a row", s.longestStreak >= 3, Icons.Default.LocalFireDepartment)
                        AchievementRow("Week Warrior", "7-day reading streak", s.longestStreak >= 7, Icons.Default.Shield)
                        AchievementRow("Scholar", "Read 25 papers total", s.totalPapersRead >= 25, Icons.Default.School)
                        AchievementRow("XP Master", "Earn 500 XP", s.totalXp >= 500, Icons.Default.Star)
                        AchievementRow("Month Strong", "30-day reading streak", s.longestStreak >= 30, Icons.Default.EmojiEvents)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StreakStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary)
            Text(label, fontSize = 11.sp, color = Gray500)
        }
    }
}

@Composable
private fun AchievementRow(
    title: String,
    description: String,
    unlocked: Boolean,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (unlocked) AccentGold.copy(alpha = 0.15f) else Gray100),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint = if (unlocked) AccentGold else Gray300,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (unlocked) Primary else Gray400
            )
            Text(description, fontSize = 12.sp, color = Gray500)
        }
        if (unlocked) {
            Icon(Icons.Default.CheckCircle, null, tint = GoalGreen, modifier = Modifier.size(20.dp))
        } else {
            Icon(Icons.Default.Lock, null, tint = Gray300, modifier = Modifier.size(20.dp))
        }
    }
}
