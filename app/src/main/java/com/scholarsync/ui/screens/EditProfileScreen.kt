package com.scholarsync.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.theme.*
import java.io.File

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSave: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    var fullName by remember { mutableStateOf(sessionManager.getDisplayName() ?: "") }
    var email by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var researchInterests by remember { mutableStateOf("") }

    // Profile picture state
    var profileBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Load existing profile picture on first composition
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

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val destFile = File(context.filesDir, "profile_picture.jpg")
                inputStream?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                sessionManager.setProfilePicturePath(destFile.absolutePath)
                BitmapFactory.decodeFile(destFile.absolutePath)?.let { bmp ->
                    profileBitmap = bmp.asImageBitmap()
                }
            } catch (_: Exception) { }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray100)
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
                .padding(horizontal = 24.dp)
                .padding(top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Primary)
            }
            Text(
                text = "Edit Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            // Profile picture
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap!!,
                                contentDescription = "Profile photo",
                                modifier = Modifier
                                    .size(128.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Primary, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Change photo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text("Change Profile Photo", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Primary)
                }
            }

            // Form fields
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ProfileField(
                    label = "Full Name",
                    value = fullName,
                    onValueChange = { fullName = it },
                    singleLine = true
                )
                ProfileField(
                    label = "Email Address",
                    value = email,
                    onValueChange = { email = it },
                    singleLine = true
                )
                ProfileField(
                    label = "Institution",
                    value = institution,
                    onValueChange = { institution = it },
                    singleLine = true
                )
                ProfileField(
                    label = "Bio",
                    value = bio,
                    onValueChange = { bio = it },
                    singleLine = false,
                    minLines = 4
                )
                Column {
                    ProfileField(
                        label = "Research Interests",
                        value = researchInterests,
                        onValueChange = { researchInterests = it },
                        singleLine = true
                    )
                    Text(
                        text = "Separate interests with commas",
                        fontSize = 12.sp,
                        color = Gray500,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Sticky save button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Gray100)
                    )
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        sessionManager.setDisplayName(fullName.trim())
                    }
                    onSave()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    minLines: Int = 1
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Primary
        )
        if (singleLine) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Gray300,
                    focusedTextColor = Primary,
                    unfocusedTextColor = Primary,
                    cursorColor = Primary,
                    focusedLabelColor = Primary,
                    unfocusedLabelColor = Gray500
                )
            )
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                minLines = minLines,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Gray300,
                    focusedTextColor = Primary,
                    unfocusedTextColor = Primary,
                    cursorColor = Primary
                )
            )
        }
    }
}
