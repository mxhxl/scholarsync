package com.scholarsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import com.scholarsync.api.FeedApi
import com.scholarsync.api.FeedPaperResponse
import com.scholarsync.api.HighlightResponse
import com.scholarsync.api.HighlightsApi
import com.scholarsync.api.LiteratureReviewResponse
import com.scholarsync.api.PaperSummaryApi
import com.scholarsync.api.PaperSummaryApiImpl
import com.scholarsync.api.ProfileApi
import com.scholarsync.api.StreaksApi
import com.scholarsync.api.SummaryResponse
import com.scholarsync.data.LocalHighlightsStore
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.components.SaveToLibraryDialog
import com.scholarsync.ui.theme.*

private val HighlightYellow = Color(0xFFFEF3C7)
private val HighlightBlue = Color(0xFFDBEAFE)
private val HighlightGreen = Color(0xFFD1FAE5)
private val HighlightPink = Color(0xFFFCE7F3)
private val HighlightOrange = Color(0xFFFFEDD5)

private fun highlightColor(name: String): Color = when (name) {
    "blue" -> HighlightBlue
    "green" -> HighlightGreen
    "pink" -> HighlightPink
    "orange" -> HighlightOrange
    else -> HighlightYellow
}

private fun highlightBorderColor(name: String): Color = when (name) {
    "blue" -> Color(0xFF3B82F6)
    "green" -> Color(0xFF22C55E)
    "pink" -> Color(0xFFEC4899)
    "orange" -> Color(0xFFF97316)
    else -> Color(0xFFEAB308)
}

@Composable
fun PaperDetailsScreen(
    paperId: String,
    onBack: () -> Unit,
    onNavigateToAlerts: () -> Unit = {},
    onOpenPdf: (pdfUrl: String, paperTitle: String) -> Unit = { _, _ -> },
    paperTitle: String? = null,
    paperAuthor: String? = null
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    val summaryApi: PaperSummaryApi = remember { PaperSummaryApiImpl() }
    val feedApi = remember { FeedApi() }
    val profileApi = remember { ProfileApi() }
    val highlightsApi = remember { HighlightsApi() }
    val localHighlightsStore = remember(context) { LocalHighlightsStore(context) }
    val streaksApi = remember { StreaksApi() }

    var paper by remember { mutableStateOf<FeedPaperResponse?>(null) }

    val displayTitle = paperTitle?.ifBlank { null } ?: paper?.title ?: "Loading..."
    val displayAuthor = paperAuthor?.ifBlank { null } ?: paper?.authors?.joinToString(", ") ?: ""
    val pdfUrl = paper?.pdfUrl

    var summary by remember { mutableStateOf<SummaryResponse?>(null) }
    var summaryLoading by remember { mutableStateOf(false) }
    var summaryError by remember { mutableStateOf<String?>(null) }

    var litReview by remember { mutableStateOf<LiteratureReviewResponse?>(null) }
    var litReviewLoading by remember { mutableStateOf(false) }
    var litReviewError by remember { mutableStateOf<String?>(null) }

    var showSaveToLibraryDialog by remember { mutableStateOf(false) }
    val isSavedToLibrary = LibrarySavedStore.isSaved(paperId)

    // Follow state
    var followedAuthors by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Highlights state
    val highlights = remember { mutableStateListOf<HighlightResponse>() }
    var showAddHighlightDialog by remember { mutableStateOf(false) }

    if (showSaveToLibraryDialog) {
        SaveToLibraryDialog(
            paperId = paperId,
            onDismiss = { showSaveToLibraryDialog = false },
            onSaved = { LibrarySavedStore.markSaved(paperId) }
        )
    }

    if (showAddHighlightDialog) {
        AddHighlightDialog(
            onDismiss = { showAddHighlightDialog = false },
            onSave = { text, note, color ->
                // Save locally first so it always works
                val localHighlight = localHighlightsStore.addHighlight(paperId, text, note.ifBlank { null }, color)
                highlights.add(localHighlight)
                showAddHighlightDialog = false

                // Also try to sync to backend
                val baseUrl = sessionManager.getApiBaseUrl()
                highlightsApi.createHighlight(baseUrl, paperId, text, note.ifBlank { null }, color) { _ -> }
            }
        )
    }

    var markedAsRead by remember { mutableStateOf(false) }

    // Fetch paper details on load
    LaunchedEffect(paperId) {
        val baseUrl = sessionManager.getApiBaseUrl()
        val token = sessionManager.getAccessToken() ?: return@LaunchedEffect
        feedApi.getPaper(baseUrl, token, paperId) { result ->
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { paper = it }
            }
        }
    }

    // Record reading event & mark as read
    LaunchedEffect(paperId) {
        val baseUrl = sessionManager.getApiBaseUrl()

        // Record the streak first (returns XP info for toast)
        // Then mark feed item as read AFTER recordRead completes to avoid race condition
        streaksApi.recordRead(baseUrl, paperId) { result ->
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { response ->
                    markedAsRead = true
                    if (response.xpEarned > 0) {
                        Toast.makeText(
                            context,
                            "Paper read! +${response.xpEarned} XP (${response.streak.currentStreak}-day streak)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        "Couldn't record reading: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            // Mark feed item as read after streak is recorded (sequential to avoid race)
            feedApi.markRead(baseUrl, paperId) { _ -> }
        }
    }

    // Fetch followed authors
    LaunchedEffect(Unit) {
        val baseUrl = sessionManager.getApiBaseUrl()
        profileApi.getProfile(baseUrl) { result ->
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { profile ->
                    followedAuthors = profile.authorsFollowing.toSet()
                }
            }
        }
    }

    // Load highlights: local first, then try backend
    LaunchedEffect(paperId) {
        val localItems = localHighlightsStore.getHighlights(paperId)
        if (localItems.isNotEmpty()) {
            highlights.clear()
            highlights.addAll(localItems)
        }

        val baseUrl = sessionManager.getApiBaseUrl()
        highlightsApi.getHighlights(baseUrl, paperId) { result ->
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { resp ->
                    if (resp.items.isNotEmpty()) {
                        highlights.clear()
                        highlights.addAll(resp.items)
                    }
                }
            }
        }
    }

    fun loadSummary() {
        summaryError = null
        summaryLoading = true
        val baseUrl = sessionManager.getApiBaseUrl()
        val token = sessionManager.getAccessToken()
        summaryApi.fetchSummary(paperId, baseUrl, token) { result ->
            Handler(Looper.getMainLooper()).post {
                summaryLoading = false
                result.fold(
                    onSuccess = { summary = it },
                    onFailure = { summaryError = it.message ?: "Failed to load summary" }
                )
            }
        }
        // Also fetch literature review in parallel
        litReviewError = null
        litReviewLoading = true
        summaryApi.fetchLiteratureReview(paperId, baseUrl) { result ->
            Handler(Looper.getMainLooper()).post {
                litReviewLoading = false
                result.fold(
                    onSuccess = { litReview = it },
                    onFailure = { litReviewError = it.message ?: "Failed to load literature review" }
                )
            }
        }
    }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Primary)
                }
                Row {
                    IconButton(
                        onClick = { showSaveToLibraryDialog = true }
                    ) {
                        Icon(
                            imageVector = if (isSavedToLibrary) Icons.Default.LibraryAdd else Icons.Default.LibraryAdd,
                            contentDescription = "Save to Library",
                            tint = if (isSavedToLibrary) AccentTeal else Primary
                        )
                    }
                    IconButton(onClick = onNavigateToAlerts) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Primary)
                    }
                }
            }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Divider(color = Gray100)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { loadSummary() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp),
                        enabled = !summaryLoading
                    ) {
                        if (summaryLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Primary
                            )
                        } else {
                            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (summaryLoading) "Loading…" else "AI Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Button(
                        onClick = {
                            if (pdfUrl != null) {
                                onOpenPdf(pdfUrl, displayTitle)
                            } else {
                                Toast.makeText(context, "PDF not available for this paper", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Source tag
                val sourceLabel = when (paper?.source) {
                    "arxiv" -> "ARXIV • ${paper?.externalId?.removePrefix("arxiv:") ?: ""}"
                    "pubmed" -> "PUBMED • ${paper?.externalId?.removePrefix("pubmed:") ?: ""}"
                    else -> paper?.source?.uppercase() ?: "PAPER"
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Primary.copy(alpha = 0.05f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = sourceLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    if (markedAsRead) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF22C55E).copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "READ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22C55E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = displayTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Authors with follow/unfollow buttons
                val authors = paper?.authors ?: emptyList()
                if (authors.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        authors.forEach { author ->
                            val isFollowed = followedAuthors.contains(author)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = author,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Gray700,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val baseUrl = sessionManager.getApiBaseUrl()
                                if (isFollowed) {
                                    OutlinedButton(
                                        onClick = {
                                            profileApi.unfollowAuthor(baseUrl, author) { result ->
                                                Handler(Looper.getMainLooper()).post {
                                                    result.onSuccess {
                                                        followedAuthors = followedAuthors - author
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(15.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentTeal)
                                    ) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Following", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            profileApi.followAuthor(baseUrl, author) { result ->
                                                Handler(Looper.getMainLooper()).post {
                                                    result.onSuccess {
                                                        followedAuthors = followedAuthors + author
                                                        Toast.makeText(context, "Following $author", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(15.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Follow", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = displayAuthor.ifBlank { "Unknown authors" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Gray700,
                        lineHeight = 20.sp
                    )
                }

                paper?.publishedDate?.let { dateStr ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateStr,
                        fontSize = 13.sp,
                        color = Gray500
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats cards
                val citationCount = paper?.citationCount ?: 0
                val citationText = when {
                    citationCount >= 1000 -> String.format("%.1fk", citationCount / 1000.0)
                    else -> citationCount.toString()
                }
                val sourceText = paper?.venue?.take(12)
                    ?: paper?.source?.uppercase()
                    ?: "—"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatInfoCard(
                        icon = Icons.Default.FormatQuote,
                        label = "Citations",
                        value = citationText,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    StatInfoCard(
                        icon = Icons.Default.QueryStats,
                        label = "Source",
                        value = sourceText,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    StatInfoCard(
                        icon = Icons.Default.Description,
                        label = "PDF",
                        value = if (pdfUrl != null) "Available" else "N/A",
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Summary section (static fallback or API result)
                SectionHeader(title = if (summary != null) "AI Summary" else "Executive Summary")

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    summaryError != null -> Text(
                        text = summaryError!!,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        lineHeight = 22.sp
                    )
                    summary != null -> {
                        val s = summary!!
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (s.purpose.isNotBlank()) {
                                Text("Purpose", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
                                Text(s.purpose, fontSize = 15.sp, color = Gray600, lineHeight = 24.sp)
                            }
                            if (s.methodology.isNotBlank()) {
                                Text("Methodology", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
                                Text(s.methodology, fontSize = 15.sp, color = Gray600, lineHeight = 24.sp)
                            }
                            if (s.keyResults.isNotBlank()) {
                                Text("Key Results", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
                                Text(s.keyResults, fontSize = 15.sp, color = Gray600, lineHeight = 24.sp)
                            }
                            if (s.limitations.isNotBlank()) {
                                Text("Limitations", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
                                Text(s.limitations, fontSize = 15.sp, color = Gray600, lineHeight = 24.sp)
                            }
                            if (s.relevance.isNotBlank()) {
                                Text("Relevance", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
                                Text(s.relevance, fontSize = 15.sp, color = Gray600, lineHeight = 24.sp)
                            }
                            if (s.researchGaps.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = Gray100)
                                Spacer(modifier = Modifier.height(8.dp))
                                SectionHeader(title = "Research Opportunities")
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AccentGold.copy(alpha = 0.08f))
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Lightbulb,
                                                contentDescription = null,
                                                tint = AccentGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Gaps & New Paper Ideas",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            s.researchGaps,
                                            fontSize = 14.sp,
                                            color = Gray700,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> Text(
                        text = "This paper proposes a novel architecture that allows Transformer models to process sequences of arbitrary length without the quadratic memory overhead. By implementing a sliding window attention mechanism coupled with dynamic key-value compression, the authors demonstrate 10x throughput improvements.",
                        fontSize = 15.sp,
                        color = Gray600,
                        lineHeight = 24.sp
                    )
                }

                // ── IEEE Literature Review Table ────────────────────────────────
                if (litReviewLoading || litReview != null || litReviewError != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = Gray100)
                    Spacer(modifier = Modifier.height(16.dp))

                    SectionHeader(title = "IEEE Literature Review")
                    Spacer(modifier = Modifier.height(12.dp))

                    when {
                        litReviewLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Primary.copy(alpha = 0.04f))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Primary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Generating literature review...",
                                        fontSize = 13.sp,
                                        color = Gray500
                                    )
                                }
                            }
                        }
                        litReviewError != null -> Text(
                            text = litReviewError!!,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            lineHeight = 22.sp
                        )
                        litReview != null && litReview!!.entries.isNotEmpty() -> {
                            val entries = litReview!!.entries
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Gray200, RoundedCornerShape(12.dp))
                            ) {
                                entries.forEachIndexed { index, entry ->
                                    val bgColor = if (index % 2 == 0) Color.White else Gray50
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(bgColor)
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Header row: ref number + title
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text(
                                                text = "[${entry.refNo}]",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Primary,
                                                modifier = Modifier.width(32.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = entry.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Primary,
                                                    lineHeight = 18.sp
                                                )
                                                Text(
                                                    text = "${entry.authors}, ${entry.year}",
                                                    fontSize = 11.sp,
                                                    color = Gray500
                                                )
                                            }
                                        }
                                        // Detail rows
                                        if (entry.methodology.isNotBlank()) {
                                            LitReviewField("Methodology", entry.methodology)
                                        }
                                        if (entry.keyFindings.isNotBlank()) {
                                            LitReviewField("Key Findings", entry.keyFindings)
                                        }
                                        if (entry.limitations.isNotBlank()) {
                                            LitReviewField("Limitations", entry.limitations)
                                        }
                                    }
                                    if (index < entries.lastIndex) {
                                        Divider(color = Gray200)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Highlights & Notes section ─────────────────────────────────
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Gray100)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "Highlights & Notes")
                    IconButton(onClick = { showAddHighlightDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add highlight",
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (highlights.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Gray50)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Highlight,
                                contentDescription = null,
                                tint = Gray300,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No highlights yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Gray400
                            )
                            Text(
                                "Tap + to save key passages from this paper",
                                fontSize = 12.sp,
                                color = Gray400
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        highlights.forEach { highlight ->
                            HighlightCard(
                                highlight = highlight,
                                onDelete = {
                                    // Remove locally first
                                    localHighlightsStore.deleteHighlight(paperId, highlight.id)
                                    highlights.remove(highlight)

                                    // Also try to delete from backend
                                    val baseUrl = sessionManager.getApiBaseUrl()
                                    highlightsApi.deleteHighlight(baseUrl, paperId, highlight.id) { _ -> }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

}

@Composable
private fun HighlightCard(
    highlight: HighlightResponse,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = highlightColor(highlight.color))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(highlightBorderColor(highlight.color))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "\"${highlight.highlightedText}\"",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Primary,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Gray400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (!highlight.note.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.StickyNote2,
                        contentDescription = null,
                        tint = Gray500,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = highlight.note,
                        fontSize = 13.sp,
                        color = Gray600,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AddHighlightDialog(
    onDismiss: () -> Unit,
    onSave: (text: String, note: String, color: String) -> Unit
) {
    var highlightText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("yellow") }
    val colors = listOf("yellow", "blue", "green", "pink", "orange")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Primary,
        textContentColor = Primary,
        title = {
            Text("Add Highlight", fontWeight = FontWeight.Bold, color = Primary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = highlightText,
                    onValueChange = { highlightText = it },
                    label = { Text("Highlighted text", color = Primary.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Primary,
                        unfocusedTextColor = Primary,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Primary.copy(alpha = 0.3f),
                        cursorColor = Primary,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = Primary.copy(alpha = 0.6f)
                    )
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (optional)", color = Primary.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Primary,
                        unfocusedTextColor = Primary,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Primary.copy(alpha = 0.3f),
                        cursorColor = Primary,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = Primary.copy(alpha = 0.6f)
                    )
                )
                Text("Color", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(highlightColor(color))
                                .then(
                                    if (selectedColor == color)
                                        Modifier.border(2.dp, highlightBorderColor(color), CircleShape)
                                    else Modifier.border(1.dp, Gray200, CircleShape)
                                )
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = highlightBorderColor(color),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(highlightText, noteText, selectedColor) },
                enabled = highlightText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Primary)
            }
        }
    )
}

@Composable
private fun StatInfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Primary.copy(alpha = 0.04f))
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Gray500,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun LitReviewField(label: String, value: String) {
    Row(modifier = Modifier.padding(start = 32.dp)) {
        Text(
            text = "$label: ",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Gray600
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = Gray600,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            letterSpacing = 0.5.sp
        )
    }
}
