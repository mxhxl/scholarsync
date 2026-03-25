package com.scholarsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.scholarsync.R
import com.scholarsync.ui.components.PrimaryButton
import com.scholarsync.ui.theme.*

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
    userDisplayName: String? = null
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val titleSize = if (isSmallScreen) 24.sp else 28.sp
    val nameSize = if (isSmallScreen) 20.sp else 24.sp
    val logoSize = if (isSmallScreen) 64.dp else 80.dp
    val iconSize = if (isSmallScreen) 40.dp else 48.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Gradient overlay at top (matches reference soft primary tint)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo area (responsive) — ScholarSync PNG logo
            Box(
                modifier = Modifier
                    .size(logoSize)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_scholarsync_logo),
                    contentDescription = "ScholarSync logo",
                    modifier = Modifier.size(iconSize),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!userDisplayName.isNullOrBlank()) {
                // Logged-in / signed-up: show name
                Text(
                    text = "Welcome back,",
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = userDisplayName,
                    fontSize = nameSize,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "AI RESEARCH ASSISTANT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Gray400,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "ScholarSync",
                    fontSize = if (isSmallScreen) 28.sp else 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    text = "AI RESEARCH ASSISTANT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Gray400,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Feature icon — ScholarSync PNG logo
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_scholarsync_logo),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    contentScale = ContentScale.Fit
                )
                // Sparkle badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-10).dp, y = 10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Feature list
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureItem(
                    text = buildAnnotatedString {
                        append("Monitor ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Primary)) {
                            append("4M+ papers")
                        }
                        append(" daily")
                    }.toString(),
                    highlight = "4M+ papers"
                )
                FeatureItem(
                    text = "5-minute AI summaries for instant insights",
                    highlight = "5-minute AI summaries"
                )
                FeatureItem(
                    text = "Prevent research duplication automatically",
                    highlight = "research duplication"
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Buttons
            PrimaryButton(
                text = "Get Started",
                onClick = onGetStarted
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    fontSize = 14.sp,
                    color = Gray500
                )
                TextButton(onClick = onSignIn) {
                    Text(
                        text = "Sign in",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeatureItem(
    text: String,
    highlight: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Primary)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = buildAnnotatedString {
                val startIndex = text.indexOf(highlight)
                if (startIndex >= 0) {
                    append(text.substring(0, startIndex))
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Primary)) {
                        append(highlight)
                    }
                    append(text.substring(startIndex + highlight.length))
                } else {
                    append(text)
                }
            },
            fontSize = 14.sp,
            color = Gray500
        )
    }
}
