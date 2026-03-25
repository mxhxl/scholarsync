package com.scholarsync.ui.screens

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.scholarsync.api.FolderResponse
import com.scholarsync.api.LibraryApi
import com.scholarsync.api.SavedPaperResponse
import com.scholarsync.data.SessionManager
import com.scholarsync.navigation.NavRoutes
import com.scholarsync.ui.components.FolderCard
import com.scholarsync.ui.theme.*

@Composable
fun LibraryScreen(
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    val libraryApi = remember { LibraryApi() }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<FolderResponse?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var isCreatingFolder by remember { mutableStateOf(false) }

    var folders by remember { mutableStateOf<List<FolderResponse>>(emptyList()) }
    var savedPapers by remember { mutableStateOf<List<SavedPaperResponse>>(emptyList()) }
    var isLoadingFolders by remember { mutableStateOf(true) }
    var isLoadingPapers by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Count papers per folder
    var folderPaperCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    fun loadFolders() {
        val baseUrl = sessionManager.getApiBaseUrl()
        val token = sessionManager.getAccessToken() ?: return
        libraryApi.getFolders(baseUrl, token) { result ->
            Handler(Looper.getMainLooper()).post {
                isLoadingFolders = false
                result.onSuccess { folders = it }
                result.onFailure { errorMessage = it.message }
            }
        }
    }

    fun loadPapers(folderId: String? = null) {
        isLoadingPapers = true
        val baseUrl = sessionManager.getApiBaseUrl()
        val token = sessionManager.getAccessToken() ?: return
        libraryApi.getLibrary(baseUrl, token, folderId = folderId) { result ->
            Handler(Looper.getMainLooper()).post {
                isLoadingPapers = false
                result.onSuccess { response ->
                    savedPapers = response.items
                }
                result.onFailure { errorMessage = it.message }
            }
        }
    }

    fun loadFolderCounts() {
        val baseUrl = sessionManager.getApiBaseUrl()
        val token = sessionManager.getAccessToken() ?: return
        // Load all papers to count per folder
        libraryApi.getLibrary(baseUrl, token, limit = 100) { result ->
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { response ->
                    val counts = mutableMapOf<String, Int>()
                    response.items.forEach { paper ->
                        paper.folderId?.let { fid ->
                            counts[fid] = (counts[fid] ?: 0) + 1
                        }
                    }
                    folderPaperCounts = counts
                    // Update global saved store so other screens show indicators
                    LibrarySavedStore.savedPaperIds.clear()
                    LibrarySavedStore.savedPaperIds.addAll(response.items.map { it.paper.id })
                }
            }
        }
    }

    // Reload everything when screen becomes visible (e.g. navigating back)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadFolders()
                loadPapers(selectedFolder?.id)
                loadFolderCounts()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Reload papers when folder selection changes
    LaunchedEffect(selectedFolder) {
        loadPapers(selectedFolder?.id)
    }

    // Filter by search query
    val filteredPapers = remember(searchQuery, savedPapers) {
        if (searchQuery.isBlank()) savedPapers
        else savedPapers.filter { sp ->
            sp.paper.title.contains(searchQuery, ignoreCase = true) ||
                sp.paper.authors.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }
    val filteredFolders = remember(searchQuery, folders) {
        if (searchQuery.isBlank()) folders
        else folders.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    // New folder dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCreatingFolder) { showNewFolderDialog = false; newFolderName = "" } },
            containerColor = Color.White,
            title = { Text("New Folder", color = NavBar, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("Folder name", color = Gray400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavBar,
                        unfocusedBorderColor = Gray300,
                        focusedTextColor = NavBar,
                        unfocusedTextColor = NavBar,
                        cursorColor = NavBar
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            isCreatingFolder = true
                            val baseUrl = sessionManager.getApiBaseUrl()
                            val token = sessionManager.getAccessToken() ?: return@TextButton
                            libraryApi.createFolder(baseUrl, token, newFolderName.trim()) { result ->
                                Handler(Looper.getMainLooper()).post {
                                    isCreatingFolder = false
                                    result.onSuccess { folder ->
                                        folders = folders + folder
                                        newFolderName = ""
                                        showNewFolderDialog = false
                                    }
                                    result.onFailure { e ->
                                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                    enabled = newFolderName.isNotBlank() && !isCreatingFolder
                ) {
                    if (isCreatingFolder) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = NavBar
                        )
                    } else {
                        Text("Create", color = NavBar, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNewFolderDialog = false; newFolderName = "" },
                    enabled = !isCreatingFolder
                ) {
                    Text("Cancel", color = Gray500)
                }
            }
        )
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
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isSearchExpanded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedFolder != null) {
                                IconButton(onClick = { selectedFolder = null }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Primary)
                                }
                            }
                            Text(
                                text = selectedFolder?.name ?: "Library",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search papers, folders...", color = Gray500) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Gray200,
                                focusedTextColor = Primary,
                                cursorColor = Primary,
                                focusedLabelColor = Primary
                            )
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) searchQuery = ""
                            }
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (isSearchExpanded) "Close search" else "Search",
                                tint = Primary
                            )
                        }
                        if (!isSearchExpanded) {
                            IconButton(onClick = { onNavigate(NavRoutes.Alerts.route) }) {
                                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Primary)
                            }
                        }
                    }
                }
            }

            // Content
            when {
                isLoadingFolders && isLoadingPapers -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading your library...", fontSize = 14.sp, color = Gray500)
                        }
                    }
                }

                errorMessage != null && savedPapers.isEmpty() && folders.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(errorMessage ?: "Something went wrong", fontSize = 14.sp, color = Error)
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = {
                                errorMessage = null
                                isLoadingFolders = true
                                isLoadingPapers = true
                                loadFolders()
                                loadPapers(selectedFolder?.id)
                                loadFolderCounts()
                            }) {
                                Text("Retry", color = Primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Folders section (only when not inside a folder)
                        if (selectedFolder == null) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "FOLDERS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Primary,
                                        letterSpacing = 1.sp
                                    )
                                    TextButton(onClick = { showNewFolderDialog = true }) {
                                        Text("New Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
                                    }
                                }
                            }

                            if (filteredFolders.isEmpty() && !isLoadingFolders) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (searchQuery.isNotBlank()) "No folders match \"$searchQuery\"" else "No folders yet. Create one to organize your papers.",
                                            fontSize = 13.sp,
                                            color = Gray500,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        filteredFolders.forEach { folder ->
                                            FolderCard(
                                                name = folder.name,
                                                paperCount = folderPaperCounts[folder.id] ?: 0,
                                                onClick = { selectedFolder = folder },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        // Add spacer if odd number of folders for balanced layout
                                        if (filteredFolders.size % 2 != 0) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        // Papers section
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedFolder != null) "PAPERS" else "RECENT PAPERS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Primary,
                                    letterSpacing = 1.sp
                                )
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isLoadingPapers) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        } else if (filteredPapers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (searchQuery.isNotBlank()) {
                                            "No papers match \"$searchQuery\""
                                        } else if (selectedFolder != null) {
                                            "No papers in this folder yet.\nSave papers from your feed to add them here."
                                        } else {
                                            "Your library is empty.\nSave papers from your feed to see them here."
                                        },
                                        fontSize = 14.sp,
                                        color = Gray500,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(filteredPapers) { savedPaper ->
                                LibraryPaperItem(
                                    savedPaper = savedPaper,
                                    onClick = {
                                        onNavigate(NavRoutes.PaperDetails.createRoute(savedPaper.paper.id))
                                    }
                                )
                            }
                        }

                        if (searchQuery.isNotBlank() && filteredPapers.isEmpty() && filteredFolders.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No papers or folders match \"$searchQuery\"",
                                        fontSize = 14.sp,
                                        color = Gray500
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
private fun LibraryPaperItem(
    savedPaper: SavedPaperResponse,
    onClick: () -> Unit
) {
    val paper = savedPaper.paper
    val authorText = when {
        paper.authors.isEmpty() -> "Unknown author"
        paper.authors.size == 1 -> paper.authors[0]
        else -> "${paper.authors[0]} +${paper.authors.size - 1}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = paper.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = authorText,
                        fontSize = 12.sp,
                        color = Gray500
                    )
                }
            }
            TextButton(onClick = onClick) {
                Text("View", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
            }
        }
    }
}
