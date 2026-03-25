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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Science
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
fun HelpResearchToolsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray100)
    ) {
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
                text = "Research Tools",
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "How we research",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "ScholarSync combines semantic search, embeddings, and AI to surface relevant papers and explain them in plain language. Below are the main technical components we use.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "TOOLS WE USE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Gray400,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ToolCard(
                icon = Icons.Default.Science,
                title = "SciBERT & embeddings",
                description = "We use SciBERT-based models to turn paper titles and abstracts into vector embeddings. Your profile interests are embedded the same way so we can match you to the most relevant papers."
            )
            ToolCard(
                icon = Icons.Default.AutoAwesome,
                title = "AI summarization (Local LLM)",
                description = "Paper summaries and key takeaways are generated using a local large language model via Ollama. This gives you a quick overview without reading the full PDF first."
            )
            ToolCard(
                icon = Icons.Default.Science,
                title = "Vector search (pgvector)",
                description = "Relevance ranking is done with similarity search over embeddings in our database, so your feed is ordered by how well each paper matches your interests."
            )
            ToolCard(
                icon = Icons.Default.Science,
                title = "Topic modeling (BERTopic)",
                description = "Trends and topic analysis use BERTopic to group and track research themes over time in the Insights section."
            )
            ToolCard(
                icon = Icons.Default.Science,
                title = "Citation analysis",
                description = "We use citation graphs and PageRank-style signals to highlight papers that are often cited and worth reading for your projects."
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AccentTeal,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
