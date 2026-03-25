package com.scholarsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.sp
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.theme.*

@Composable
fun ServerUrlScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    var url by remember { mutableStateOf(sessionManager.getApiBaseUrl()) }
    var saved by remember { mutableStateOf(false) }

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
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Primary)
                }
                Text(
                    text = "API Server",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary
                )
                Box(modifier = Modifier.size(48.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "Backend URL",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Gray600,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("http://192.168.x.x:8000", color = Gray400) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Gray200,
                    focusedTextColor = Primary,
                    unfocusedTextColor = Primary,
                    cursorColor = Primary,
                    focusedLabelColor = Primary,
                    unfocusedLabelColor = Gray500
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Use your computer's IP and port (e.g. http://192.168.1.100:8000). Your phone must be on the same Wi‑Fi as the computer running the backend.",
                fontSize = 12.sp,
                color = Gray500,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Button(
                onClick = {
                    val trimmed = url.trim()
                    if (trimmed.isNotBlank()) {
                        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
                        sessionManager.setApiBaseUrl(withScheme)
                        saved = true
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
            if (saved) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Saved. Go back and retry loading your feed.", fontSize = 12.sp, color = Primary)
            }
        }
    }
}
