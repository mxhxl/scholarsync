package com.scholarsync.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.ui.theme.*

data class FaqEntry(val question: String, val answer: String)

@Composable
fun HelpFaqScreen(onBack: () -> Unit) {
    val faqs = remember {
        listOf(
            FaqEntry(
                "How does AI summarization work?",
                "We use large language models to read the paper's title and abstract (and full text when available) and generate a short summary plus key points. Summaries are created on our servers and cached so they load quickly when you open a paper."
            ),
            FaqEntry(
                "Can I export my library?",
                "Yes. From the Library screen you can browse your saved papers and folders. Export options (e.g. export to file or share list) can be available depending on your app version and backend support. Check the Library menu or paper actions for Export or Share."
            ),
            FaqEntry(
                "How to set up alerts?",
                "Go to Alerts from the main navigation. Create or select a project, then turn on overlap detection and/or new-paper alerts. The app will notify you when new papers match your project or overlap with your saved work. Alerts run on a schedule (e.g. daily); allow up to 24 hours for the first run."
            ),
            FaqEntry(
                "What are research interests used for?",
                "Your research interests and topics are converted into embeddings and used to rank papers in your Feed and improve Discover results. Keeping them updated in profile setup or Edit Profile helps the app recommend more relevant papers."
            ),
            FaqEntry(
                "Why isn’t my Feed updating?",
                "Pull down on the Feed to refresh. If it still doesn’t update, check your internet connection and that the backend URL in Settings is correct. The feed is built from papers in our database that match your profile; new papers are added periodically by our systems."
            ),
            FaqEntry(
                "How do citations and must-cite work?",
                "We analyze citation graphs and use PageRank-style signals to find papers that are frequently cited in your area. These appear as suggestions so you can prioritize important references for your projects."
            ),
            FaqEntry(
                "Is my data private?",
                "Your profile, saved papers, and library are stored on our servers and used only to personalize your experience and run alerts. We don’t sell your data. See Settings → Security & Privacy for more details."
            )
        )
    }

    var expandedIndex by remember { mutableStateOf<Int?>(null) }

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
                text = "Frequently Asked Questions",
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
            faqs.forEachIndexed { index, faq ->
                FaqAccordionItem(
                    question = faq.question,
                    answer = faq.answer,
                    expanded = expandedIndex == index,
                    onClick = {
                        expandedIndex = if (expandedIndex == index) null else index
                    }
                )
                if (index < faqs.lastIndex) Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FaqAccordionItem(
    question: String,
    answer: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(200)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Gray500
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = answer,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
