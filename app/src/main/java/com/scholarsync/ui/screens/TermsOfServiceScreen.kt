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
fun TermsOfServiceScreen(
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
                text = "Terms of Service",
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
                "Welcome to ScholarSync. By accessing or using our application and services, you agree to be bound by these Terms of Service. " +
                "If you do not agree, do not use our services."
            )
            PolicyHeading("1. Acceptance of Terms")
            PolicyParagraph(
                "By creating an account or using ScholarSync, you confirm that you are at least 16 years old (or the age of majority in your jurisdiction) " +
                "and that you accept these terms and our Privacy Policy."
            )
            PolicyHeading("2. Description of Service")
            PolicyParagraph(
                "ScholarSync provides a research discovery and organization platform: personalized paper feeds, AI-generated summaries, " +
                "library and folder management, alerts, and related features. We may add, change, or discontinue features with notice where appropriate."
            )
            PolicyHeading("3. Account and Security")
            PolicyParagraph(
                "You must provide accurate registration information and keep your account credentials secure. " +
                "You are responsible for all activity under your account. Notify us immediately of any unauthorized use."
            )
            PolicyHeading("4. Acceptable Use")
            PolicyParagraph(
                "You agree not to use ScholarSync for any illegal purpose, to violate others' rights, or to abuse the service (e.g., scraping, reverse engineering, " +
                "or overloading our systems). We may suspend or terminate accounts that violate these terms."
            )
            PolicyHeading("5. Intellectual Property")
            PolicyParagraph(
                "ScholarSync and its content, features, and branding are owned by us or our licensors. " +
                "You may not copy, modify, or distribute our materials without permission. Paper metadata and summaries are provided for personal research use."
            )
            PolicyHeading("6. Disclaimers")
            PolicyParagraph(
                "The service is provided \"as is.\" We do not guarantee accuracy of recommendations or AI-generated content. " +
                "You use the service at your own risk. We are not liable for decisions you make based on content in the app."
            )
            PolicyHeading("7. Limitation of Liability")
            PolicyParagraph(
                "To the maximum extent permitted by law, we are not liable for any indirect, incidental, special, or consequential damages, " +
                "or for any loss of data or profits arising from your use of ScholarSync."
            )
            PolicyHeading("8. Changes")
            PolicyParagraph(
                "We may update these Terms from time to time. We will notify you of material changes. Continued use after changes constitutes acceptance."
            )
            PolicyParagraph(
                "For questions, contact us at support@scholarsync.app.",
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
