package com.scholarsync.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.ui.theme.*

@Composable
fun HelpGettingStartedScreen(onBack: () -> Unit) {
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
                text = "Getting Started",
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
            // App intro
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "About ScholarSync",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "ScholarSync helps you discover, save, and organize academic papers with AI-powered summaries, personalized feeds, and research alerts so you can focus on what matters.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "STEP BY STEP",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Gray400,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            listOf(
                "Sign up or log in to create your ScholarSync account.",
                "Complete profile setup: add your name, research interests, and topics.",
                "Explore Discover to find papers or get a personalized Feed based on your profile.",
                "Save papers to your Library and organize them in folders.",
                "Enable Alerts for overlap detection and new-paper notifications.",
                "Open any paper for AI summaries, citations, and full details."
            ).forEachIndexed { index, step ->
                StepRow(number = index + 1, text = step)
                if (index < 5) Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "KEY FEATURES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Gray400,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            KeyFeatureItem(
                icon = Icons.Default.RocketLaunch,
                title = "Personalized Feed",
                description = "Papers ranked by relevance to your interests and research profile."
            )
            KeyFeatureItem(
                icon = Icons.Default.CheckCircle,
                title = "AI Summaries",
                description = "Clear summaries and key points generated for each paper."
            )
            KeyFeatureItem(
                icon = Icons.Default.CheckCircle,
                title = "Library & Folders",
                description = "Save papers and organize them into custom folders."
            )
            KeyFeatureItem(
                icon = Icons.Default.CheckCircle,
                title = "Alerts & Overlap",
                description = "Get notified when new relevant papers appear or overlap with your projects."
            )
            KeyFeatureItem(
                icon = Icons.Default.CheckCircle,
                title = "Citations & Trends",
                description = "Discover must-cite papers and see topic trends over time."
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Primary
        ) {
            Text(
                text = "$number",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KeyFeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AccentTeal,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}
