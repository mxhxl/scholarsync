package com.scholarsync.ui.components

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.FolderOpen
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
import com.scholarsync.api.FolderResponse
import com.scholarsync.api.LibraryApi
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.theme.*

@Composable
fun SaveToLibraryDialog(
    paperId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    val libraryApi = remember { LibraryApi() }

    var folders by remember { mutableStateOf<List<FolderResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var showNewFolderField by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var isCreatingFolder by remember { mutableStateOf(false) }

    // Load folders on open
    LaunchedEffect(Unit) {
        val baseUrl = sessionManager.getApiBaseUrl()
        val token = sessionManager.getAccessToken() ?: return@LaunchedEffect
        libraryApi.getFolders(baseUrl, token) { result ->
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { folders = it }
                isLoading = false
            }
        }
    }

    fun createFolderAndSelect() {
        if (newFolderName.isBlank()) return
        isCreatingFolder = true
        val baseUrl = sessionManager.getApiBaseUrl()
        val token = sessionManager.getAccessToken() ?: return
        libraryApi.createFolder(baseUrl, token, newFolderName.trim()) { result ->
            Handler(Looper.getMainLooper()).post {
                isCreatingFolder = false
                result.onSuccess { folder ->
                    folders = folders + folder
                    selectedFolderId = folder.id
                    newFolderName = ""
                    showNewFolderField = false
                }
                result.onFailure { e ->
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun savePaper() {
        isSaving = true
        val baseUrl = sessionManager.getApiBaseUrl()
        val token = sessionManager.getAccessToken() ?: return
        libraryApi.savePaper(baseUrl, token, paperId, selectedFolderId) { result ->
            Handler(Looper.getMainLooper()).post {
                isSaving = false
                result.onSuccess {
                    Toast.makeText(context, "Saved to library", Toast.LENGTH_SHORT).show()
                    onSaved()
                    onDismiss()
                }
                result.onFailure { e ->
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                "Save to Library",
                color = Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Choose a folder (optional)",
                    fontSize = 13.sp,
                    color = Gray500,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(28.dp))
                    }
                } else {
                    // No folder option
                    FolderOptionItem(
                        name = "No folder",
                        isSelected = selectedFolderId == null,
                        onClick = { selectedFolderId = null },
                        isNoFolder = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Folder list
                    if (folders.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(folders) { folder ->
                                FolderOptionItem(
                                    name = folder.name,
                                    isSelected = selectedFolderId == folder.id,
                                    onClick = { selectedFolderId = folder.id }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Create new folder
                    if (showNewFolderField) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newFolderName,
                                onValueChange = { newFolderName = it },
                                placeholder = { Text("Folder name", color = Gray400, fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Gray300,
                                    focusedTextColor = Primary,
                                    unfocusedTextColor = Primary,
                                    cursorColor = Primary
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isCreatingFolder) {
                                CircularProgressIndicator(
                                    color = Primary,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                TextButton(
                                    onClick = { createFolderAndSelect() },
                                    enabled = newFolderName.isNotBlank()
                                ) {
                                    Text("Add", color = if (newFolderName.isNotBlank()) Primary else Gray400, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        TextButton(
                            onClick = { showNewFolderField = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Create New Folder",
                                color = AccentTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { savePaper() },
                enabled = !isSaving && !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (isSaving) "Saving..." else "Save",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel", color = Gray500)
            }
        }
    )
}

@Composable
private fun FolderOptionItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isNoFolder: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isNoFolder) Icons.Outlined.FolderOpen else Icons.Default.Folder,
            contentDescription = null,
            tint = if (isSelected) Primary else Gray400,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Primary else Gray700,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
