package com.scholarsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LoginActivityScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    var activityList by remember { mutableStateOf(sessionManager.getLoginActivityList()) }

    LaunchedEffect(Unit) {
        sessionManager.recordLoginActivity("This device")
        activityList = sessionManager.getLoginActivityList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray100)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color.White)
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
                .background(androidx.compose.ui.graphics.Color.White)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 24.dp)
                .padding(top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Primary)
            }
            Text(
                text = "Login Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
        }
        }

        Text(
            text = "Recent sign-ins. Updated when you open this screen.",
            fontSize = 14.sp,
            color = Gray500,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        if (activityList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No login activity yet",
                    fontSize = 14.sp,
                    color = Gray500
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(activityList, key = { index, _ -> index }) { _, (timestampMs, device) ->
                    LoginActivityItem(
                        timestampMs = timestampMs,
                        device = device
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginActivityItem(
    timestampMs: Long,
    device: String
) {
    val dateStr = remember(timestampMs) {
        val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        sdf.format(Date(timestampMs))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Primary
            )
            Text(
                text = dateStr,
                fontSize = 12.sp,
                color = Gray500
            )
        }
    }
}
