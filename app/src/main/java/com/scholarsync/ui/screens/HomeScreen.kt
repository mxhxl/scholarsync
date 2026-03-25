package com.scholarsync.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.scholarsync.api.FeedApi
import com.scholarsync.api.FeedItemResponse
import com.scholarsync.api.StreakResponse
import com.scholarsync.api.StreaksApi
import com.scholarsync.data.SessionManager
import com.scholarsync.navigation.NavRoutes
import com.scholarsync.ui.components.SaveToLibraryDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.scholarsync.ui.components.*
import com.scholarsync.ui.theme.*

data class PaperItem(
    val id: String,
    val title: String,
    val description: String,
    val source: String,
    val sourceColor: Color,
    val authors: List<String> = emptyList(),
    val citationCount: Int = 0,
    val isRead: Boolean = false
)

@Composable
fun HomeScreen(
    onPaperClick: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    val feedApi = remember { FeedApi() }
    val streaksApi = remember { StreaksApi() }
    val displayName = sessionManager.getDisplayName() ?: "Researcher"
    val todayDate = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var currentStreak by remember { mutableIntStateOf(0) }

    var selectedCategory by remember { mutableStateOf("All Topics") }
    val categories = listOf("All Topics", "High Priority", "Unread")

    var feedItems by remember { mutableStateOf<List<FeedItemResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasTriedRefresh by remember { mutableStateOf(false) }
    var saveToLibraryPaperId by remember { mutableStateOf<String?>(null) }
    // Track locally-marked-read papers so UI updates immediately
    var localReadIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Refresh streak count + feed data every time HomeScreen becomes visible
    var hasLoadedOnce by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val baseUrl = sessionManager.getApiBaseUrl()
                // Refresh streak counter
                streaksApi.getStreak(baseUrl) { result ->
                    Handler(Looper.getMainLooper()).post {
                        result.onSuccess { currentStreak = it.currentStreak }
                    }
                }
                // Reload feed to sync is_read status from backend (skip first load — LaunchedEffect handles it)
                if (hasLoadedOnce) {
                    val token = sessionManager.getAccessToken()
                    if (token != null) {
                        feedApi.getFeed(
                            baseUrl = baseUrl,
                            bearerToken = token,
                            filter = when (selectedCategory) {
                                "High Priority" -> "high_priority"
                                "Unread" -> "unread"
                                else -> "all"
                            }
                        ) { result ->
                            Handler(Looper.getMainLooper()).post {
                                result.onSuccess { response ->
                                    feedItems = response.items
                                    // Clear local overrides since backend is now authoritative
                                    localReadIds = emptySet()
                                }
                            }
                        }
                    }
                }
                hasLoadedOnce = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (saveToLibraryPaperId != null) {
        SaveToLibraryDialog(
            paperId = saveToLibraryPaperId!!,
            onDismiss = { saveToLibraryPaperId = null },
            onSaved = {
                LibrarySavedStore.markSaved(saveToLibraryPaperId!!)
                saveToLibraryPaperId = null
            }
        )
    }

    // Load saved paper IDs for library indicators
    LaunchedEffect(Unit) {
        val token = sessionManager.getAccessToken()
        if (token != null) {
            LibrarySavedStore.refresh(sessionManager.getApiBaseUrl(), token)
        }
    }

    // Map category to API filter
    val apiFilter = when (selectedCategory) {
        "High Priority" -> "high_priority"
        "Unread" -> "unread"
        else -> "all"
    }

    // Trigger a feed refresh: fetches papers from arXiv/PubMed on the backend
    fun doRefresh() {
        val token = sessionManager.getAccessToken() ?: return
        val baseUrl = sessionManager.getApiBaseUrl()
        isRefreshing = true
        errorMessage = null

        feedApi.refreshFeed(
            baseUrl = baseUrl,
            bearerToken = token
        ) { refreshResult ->
            Handler(Looper.getMainLooper()).post {
                refreshResult.onSuccess { refreshResponse ->
                    // Reload feed after refresh
                    feedApi.getFeed(
                        baseUrl = baseUrl,
                        bearerToken = token,
                        filter = apiFilter
                    ) { reloadResult ->
                        Handler(Looper.getMainLooper()).post {
                            isRefreshing = false
                            reloadResult.onSuccess { r -> feedItems = r.items }
                            reloadResult.onFailure { e ->
                                errorMessage = e.message
                            }
                        }
                    }
                }
                refreshResult.onFailure { e ->
                    isRefreshing = false
                    errorMessage = "Feed refresh failed: ${e.message}"
                }
            }
        }
    }

    // Load feed from backend
    fun loadFeed() {
        val token = sessionManager.getAccessToken() ?: return
        val baseUrl = sessionManager.getApiBaseUrl()
        isLoading = true
        errorMessage = null

        feedApi.getFeed(
            baseUrl = baseUrl,
            bearerToken = token,
            filter = apiFilter
        ) { result ->
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { response ->
                    feedItems = response.items
                    isLoading = false

                    // If feed is empty and we haven't tried refreshing yet, trigger a refresh
                    if (response.items.isEmpty() && !hasTriedRefresh) {
                        hasTriedRefresh = true
                        doRefresh()
                    }
                }.onFailure { error ->
                    isLoading = false
                    errorMessage = error.message
                }
            }
        }
    }

    // Load on first compose and when filter changes
    LaunchedEffect(apiFilter) {
        loadFeed()
    }

    // Convert feed items to PaperItems for display
    val papers = feedItems.map { item ->
        PaperItem(
            id = item.paper.id,
            title = item.paper.title,
            description = item.paper.abstract?.take(150)?.plus("...") ?: "No abstract available",
            source = when {
                item.paper.source == "arxiv" -> item.paper.externalId.removePrefix("arxiv:")
                item.paper.venue != null -> item.paper.venue
                else -> item.paper.source
            },
            sourceColor = when (item.paper.source) {
                "arxiv" -> AccentTeal
                "pubmed" -> AccentGold
                else -> Primary
            },
            authors = item.paper.authors,
            citationCount = item.paper.citationCount,
            isRead = item.isRead || localReadIds.contains(item.paper.id)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray50)
    ) {
            // Header
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 6.dp, bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "WELCOME BACK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Gray400,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = displayName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                        // Streak fire widget
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (currentStreak > 0) Color(0xFFFF9500).copy(alpha = 0.12f)
                                    else Gray100
                                )
                                .clickable { onNavigate(NavRoutes.Streaks.route) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = if (currentStreak > 0) Color(0xFFFF9500) else Gray400,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "$currentStreak",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentStreak > 0) Color(0xFFFF9500) else Gray500
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Papers",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Text(
                            text = todayDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Gray400
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        items(categories) { category ->
                            CategoryChip(
                                text = category,
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category }
                            )
                        }
                    }
                }
            }

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading your feed...", fontSize = 14.sp, color = Gray500)
                        }
                    }
                }

                isRefreshing -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Fetching papers based on your interests...\nThis may take a moment.",
                                fontSize = 14.sp,
                                color = Gray500,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                errorMessage ?: "Something went wrong",
                                fontSize = 14.sp,
                                color = Error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Use the same Wi‑Fi as your computer and ensure the backend is running, or set the server URL in Settings → API Server.",
                                fontSize = 12.sp,
                                color = Gray500,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            PrimaryButton(text = "Retry", onClick = { loadFeed() })
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { onNavigate(NavRoutes.ServerUrl.route) }) {
                                Text("Change server URL", fontSize = 14.sp, color = Primary)
                            }
                        }
                    }
                }

                papers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                "No papers yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Your personalized feed will populate as we find papers matching your research interests.",
                                fontSize = 14.sp,
                                color = Gray500,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            PrimaryButton(
                                text = "Refresh Feed",
                                onClick = { doRefresh() }
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                    ) {
                        items(papers) { paper ->
                            PaperCard(
                                title = paper.title,
                                description = paper.description,
                                source = paper.source,
                                sourceColor = paper.sourceColor,
                                onClick = {
                                    // Mark paper as read locally for instant UI feedback
                                    if (!paper.isRead) {
                                        localReadIds = localReadIds + paper.id
                                    }
                                    onPaperClick(paper.id)
                                },
                                isSavedToLibrary = LibrarySavedStore.isSaved(paper.id),
                                onSaveToLibraryClick = { saveToLibraryPaperId = paper.id },
                                authors = paper.authors,
                                citationCount = paper.citationCount,
                                isRead = paper.isRead
                            )
                        }
                    }
                }
            }
        }
    }

