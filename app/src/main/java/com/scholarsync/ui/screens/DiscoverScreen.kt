package com.scholarsync.ui.screens

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.api.DiscoverApi
import com.scholarsync.api.DiscoverPaperResult
import com.scholarsync.data.SessionManager
import com.scholarsync.navigation.NavRoutes
import com.scholarsync.ui.theme.*

private val quickTopics = listOf(
    "Machine Learning",
    "Natural Language Processing",
    "Computer Vision",
    "Quantum Computing",
    "Neuroscience",
    "Genomics",
    "Robotics",
    "Cybersecurity"
)

private val sourceFilters = listOf("All" to "all", "arXiv" to "arxiv", "PubMed" to "pubmed")

@Composable
fun DiscoverScreen(
    onNavigate: (String) -> Unit,
    onPaperClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val sessionManager = remember(context) { SessionManager(context) }
    val discoverApi = remember { DiscoverApi() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("all") }
    var results by remember { mutableStateOf<List<DiscoverPaperResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var resultCount by remember { mutableStateOf(0) }

    fun performSearch(query: String) {
        if (query.isBlank()) return
        focusManager.clearFocus()
        isLoading = true
        errorMessage = null
        hasSearched = true

        val baseUrl = sessionManager.getApiBaseUrl()
        val token = sessionManager.getAccessToken() ?: run {
            isLoading = false
            errorMessage = "Please log in to search papers"
            return
        }

        discoverApi.search(baseUrl, token, query, source = selectedSource) { result ->
            Handler(Looper.getMainLooper()).post {
                isLoading = false
                result.fold(
                    onSuccess = { response ->
                        results = response.results
                        resultCount = response.total
                    },
                    onFailure = { e ->
                        errorMessage = e.message ?: "Search failed"
                    }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .drawBehind {
                        drawLine(
                            color = Gray200,
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
                        .padding(horizontal = 24.dp)
                        .padding(top = 12.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = "Discover",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Search papers by topic, author, or keyword",
                        fontSize = 14.sp,
                        color = Gray500
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search bar — full width, search on Enter
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("e.g. transformer attention, Yann LeCun, CRISPR…", fontSize = 14.sp, color = Gray400)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Gray400, modifier = Modifier.size(22.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        results = emptyList()
                                        hasSearched = false
                                        errorMessage = null
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Gray400, modifier = Modifier.size(18.dp))
                                    }
                                    // Search button inside the field
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (!isLoading) Primary else Gray300)
                                            .clickable(enabled = !isLoading) { performSearch(searchQuery) }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Search",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Gray800
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Gray800,
                            unfocusedTextColor = Gray800,
                            cursorColor = Primary,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Gray200,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Gray50
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { performSearch(searchQuery) }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Source filter — inline, no scroll needed for 3 items
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sourceFilters.forEach { (label, value) ->
                            val isSelected = selectedSource == value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Primary else Color.White)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Primary else Gray200,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedSource = value }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Gray600
                                )
                            }
                        }
                    }
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 0.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Quick topics
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "QUICK TOPICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gray400,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 24.dp, bottom = 10.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(modifier = Modifier.width(24.dp))
                    quickTopics.forEach { topic ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .border(1.dp, AccentTeal.copy(alpha = 0.25f), RoundedCornerShape(50))
                                .clickable {
                                    searchQuery = topic
                                    performSearch(topic)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = topic,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentTeal
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Results area
                when {
                    isLoading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = Primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Searching papers…",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Gray500
                            )
                        }
                    }
                    errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Error.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Error, modifier = Modifier.size(30.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(errorMessage!!, fontSize = 14.sp, color = Gray600, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { performSearch(searchQuery) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    hasSearched && results.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Gray100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SearchOff, contentDescription = null, tint = Gray400, modifier = Modifier.size(30.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("No papers found", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Try different keywords or another source", fontSize = 13.sp, color = Gray400)
                        }
                    }
                    hasSearched && results.isNotEmpty() -> {
                        // Result header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Article, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("$resultCount papers found", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Gray700)
                            }
                            // Source badge
                            val sourceLabel = sourceFilters.firstOrNull { it.second == selectedSource }?.first ?: "All"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentTeal.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(sourceLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentTeal)
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                        ) {
                            results.forEach { paper ->
                                DiscoverResultCard(
                                    paper = paper,
                                    onClick = {
                                        val baseUrl = sessionManager.getApiBaseUrl()
                                        val token = sessionManager.getAccessToken()
                                        if (token != null) {
                                            discoverApi.savePaper(baseUrl, token, paper) { saveResult ->
                                                Handler(Looper.getMainLooper()).post {
                                                    saveResult.fold(
                                                        onSuccess = { saved -> onPaperClick(saved.id) },
                                                        onFailure = {
                                                            Toast.makeText(context, "Could not open paper", Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    else -> {
                        // Initial empty state
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Explore, contentDescription = null, tint = Primary, modifier = Modifier.size(38.dp))
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Explore Research", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Search millions of papers from arXiv and PubMed. Try a topic, author name, or keyword above.",
                                fontSize = 14.sp,
                                color = Gray400,
                                lineHeight = 21.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverResultCard(
    paper: DiscoverPaperResult,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Source badge + date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (paper.source == "arxiv") AccentTeal.copy(alpha = 0.1f)
                            else Primary.copy(alpha = 0.07f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = paper.source.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (paper.source == "arxiv") AccentTeal else Primary,
                        letterSpacing = 0.5.sp
                    )
                }
                paper.publishedDate?.let { dateStr ->
                    Text(dateStr, fontSize = 11.sp, color = Gray400)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = paper.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Gray800,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 21.sp
            )

            // Authors
            if (paper.authors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = paper.authors.take(3).joinToString(", ") +
                        if (paper.authors.size > 3) " +${paper.authors.size - 3} more" else "",
                    fontSize = 12.sp,
                    color = Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Abstract snippet
            if (!paper.abstract.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = paper.abstract,
                    fontSize = 13.sp,
                    color = Gray500,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )
            }

            // Bottom row: venue + PDF
            if (paper.venue != null || paper.pdfUrl != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    paper.venue?.let { venue ->
                        Text(
                            text = venue,
                            fontSize = 11.sp,
                            color = Gray400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (paper.pdfUrl != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AccentTeal)
                        }
                    }
                }
            }
        }
    }
}
