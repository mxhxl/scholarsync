package com.scholarsync.ui.screens

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.scholarsync.ui.theme.*

@Composable
fun PdfViewerScreen(
    pdfUrl: String,
    paperTitle: String,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loadProgress by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .drawBehind {
                        drawLine(
                            color = Gray200,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBackIosNew,
                                contentDescription = "Back",
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // Title area
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "PDF Viewer",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentTeal,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = paperTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Reload
                        IconButton(onClick = {
                            isLoading = true
                            hasError = false
                            loadProgress = 0
                            webViewRef?.reload()
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Progress bar
                    AnimatedVisibility(
                        visible = isLoading && !hasError,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        @Suppress("DEPRECATION")
                        LinearProgressIndicator(
                            progress = loadProgress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = AccentTeal,
                            trackColor = Gray100,
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            // WebView
            val googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=${pdfUrl}"
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }
                            @Suppress("DEPRECATION")
                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                isLoading = false
                                hasError = true
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportZoom(true)
                        settings.domStorageEnabled = true
                        settings.defaultTextEncodingName = "utf-8"
                        setInitialScale(100)
                        setBackgroundColor(android.graphics.Color.parseColor("#F8FAFC"))
                        loadUrl(googleDocsUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Floating zoom controls (bottom-right)
            AnimatedVisibility(
                visible = !isLoading && !hasError,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Primary)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Zoom in
                    Surface(
                        onClick = { webViewRef?.zoomIn() },
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ZoomIn,
                                contentDescription = "Zoom in",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    // Divider
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                    // Zoom out
                    Surface(
                        onClick = { webViewRef?.zoomOut() },
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ZoomOut,
                                contentDescription = "Zoom out",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Loading state
            AnimatedVisibility(
                visible = isLoading && !hasError,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Loading PDF…",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This may take a moment",
                        fontSize = 13.sp,
                        color = Gray400
                    )
                }
            }

            // Error state
            AnimatedVisibility(
                visible = hasError && !isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Error.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Failed to load PDF",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Check your connection and try again",
                        fontSize = 13.sp,
                        color = Gray500
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            hasError = false
                            loadProgress = 0
                            webViewRef?.reload()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
