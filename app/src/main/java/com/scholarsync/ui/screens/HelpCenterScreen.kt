package com.scholarsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.ui.components.SearchField
import com.scholarsync.ui.theme.*

@Composable
fun HelpCenterScreen(
    onBack: () -> Unit,
    onNavigateToGettingStarted: () -> Unit = {},
    onNavigateToResearchTools: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToTroubleshooting: () -> Unit = {},
    onNavigateToFaq: () -> Unit = {},
    onNavigateToRaiseTicket: () -> Unit = {},
    onNavigateToEmailSupport: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

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
                text = "Help Center",
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
            SearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search for help...",
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Category grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HelpCategoryCard(
                    title = "Getting Started",
                    icon = Icons.Default.RocketLaunch,
                    onClick = onNavigateToGettingStarted,
                    modifier = Modifier.weight(1f)
                )
                HelpCategoryCard(
                    title = "Research Tools",
                    icon = Icons.Default.Analytics,
                    onClick = onNavigateToResearchTools,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HelpCategoryCard(
                    title = "Account",
                    icon = Icons.Default.Person,
                    onClick = onNavigateToAccount,
                    modifier = Modifier.weight(1f)
                )
                HelpCategoryCard(
                    title = "Troubleshooting",
                    icon = Icons.Default.Build,
                    onClick = onNavigateToTroubleshooting,
                    modifier = Modifier.weight(1f)
                )
            }

            // FAQ section
            Text(
                text = "FREQUENTLY ASKED QUESTIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Gray400,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
            )
            FAQItem("How does AI summarization work?", onClick = onNavigateToFaq)
            FAQItem("Can I export my library?", onClick = onNavigateToFaq)
            FAQItem("How to set up alerts?", onClick = onNavigateToFaq)

            // Still need help section: Raise a ticket + Email support
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Still need help?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = onNavigateToRaiseTicket,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Raise a Ticket", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onNavigateToEmailSupport,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Email Support", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpCategoryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1.1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Primary,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun FAQItem(
    question: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = question,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Primary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Gray400,
            modifier = Modifier.size(24.dp)
        )
    }
    Divider(color = Primary.copy(alpha = 0.1f))
}
