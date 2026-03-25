package com.scholarsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.ui.theme.*

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
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
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Primary)
            }
            Text(
                text = "Privacy Policy",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
        }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            PolicyTitle("Last updated: March 2026")
            PolicyParagraph(
                "ScholarSync (\"we\", \"our\", or \"us\") is committed to protecting your privacy. " +
                "This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use our application and services."
            )
            PolicyHeading("1. Information We Collect")
            PolicyParagraph(
                "We collect information you provide directly (e.g., account registration, profile details, research interests), " +
                "usage data (e.g., papers viewed, saved items, search queries), and device information (e.g., device type, OS version) to deliver and improve our services."
            )
            PolicyHeading("2. How We Use Your Information")
            PolicyParagraph(
                "We use your information to personalize your feed, provide paper recommendations and summaries, manage your library and alerts, " +
                "send notifications you opt into, improve our algorithms and product, and comply with legal obligations."
            )
            PolicyHeading("3. Data Sharing and Disclosure")
            PolicyParagraph(
                "We do not sell your personal data. We may share data with service providers who assist our operations (under strict agreements), " +
                "when required by law, or to protect our rights and safety. We may share aggregated, non-identifying statistics for research or marketing."
            )
            PolicyHeading("4. Data Security")
            PolicyParagraph(
                "We use industry-standard measures to protect your data, including encryption in transit and at rest, secure authentication, " +
                "and access controls. You are responsible for keeping your password and account credentials confidential."
            )
            PolicyHeading("5. Your Rights")
            PolicyParagraph(
                "Depending on your location, you may have the right to access, correct, delete, or port your data, and to object to or restrict certain processing. " +
                "You can manage preferences in Settings and contact us for other requests."
            )
            PolicyHeading("6. Children's Privacy")
            PolicyParagraph(
                "Our services are not directed to individuals under 16. We do not knowingly collect personal information from children under 16."
            )
            PolicyHeading("7. Changes to This Policy")
            PolicyParagraph(
                "We may update this Privacy Policy from time to time. We will notify you of material changes via the app or email. " +
                "Continued use after changes constitutes acceptance of the updated policy."
            )
            PolicyParagraph(
                "If you have questions, contact us at privacy@scholarsync.app.",
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PolicyTitle(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = Gray500,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
private fun PolicyHeading(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun PolicyParagraph(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Gray700,
        lineHeight = 22.sp,
        modifier = modifier.padding(bottom = 12.dp)
    )
}
