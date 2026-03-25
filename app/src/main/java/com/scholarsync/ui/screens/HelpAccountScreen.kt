package com.scholarsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.theme.*

@Composable
fun HelpAccountScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    val displayName = sessionManager.getDisplayName() ?: "Not set"
    val apiBaseUrl = sessionManager.getApiBaseUrl()
    val hasApiKey = !sessionManager.getApiKey().isNullOrBlank()

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
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 24.dp)
                .padding(top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Primary)
            }
            Text(
                text = "Account",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary,
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.size(48.dp))
        }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Your ScholarSync account details (app only — not linked to banking or payment).",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    AccountRow(
                        icon = Icons.Default.Person,
                        label = "Account name",
                        value = displayName
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .height(1.dp)
                            .background(Primary.copy(alpha = 0.12f))
                    )
                    AccountRow(
                        label = "Backend",
                        value = apiBaseUrl
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .height(1.dp)
                            .background(Primary.copy(alpha = 0.12f))
                    )
                    AccountRow(
                        label = "API key",
                        value = if (hasApiKey) "••••••••" else "Not configured"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "You can change your display name in Settings → Edit Profile. API and backend settings are for connecting this app to the ScholarSync server.",
                fontSize = 12.sp,
                color = Gray500,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AccountRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                it,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Gray500,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 15.sp,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
