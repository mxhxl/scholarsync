package com.scholarsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Handler
import android.os.Looper
import com.scholarsync.api.AuthApiImpl
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.components.PrimaryButton
import com.scholarsync.ui.components.ScholarTextField
import com.scholarsync.ui.theme.*

@Composable
fun LoginScreen(
    onSignInSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    val authApi = remember { AuthApiImpl() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sign in",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Text(
            text = "Welcome back to ScholarSync",
            fontSize = 14.sp,
            color = Gray500,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        ScholarTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            placeholder = "Email",
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        ScholarTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            placeholder = "Password",
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = PasswordVisualTransformation(),
            keyboardType = KeyboardType.Password
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                fontSize = 13.sp,
                color = Error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = if (isLoading) "Signing in…" else "Sign in",
            onClick = {
                when {
                    email.isBlank() -> errorMessage = "Enter your email"
                    password.isBlank() -> errorMessage = "Enter your password"
                    else -> {
                        errorMessage = null
                        isLoading = true
                        val baseUrl = sessionManager.getApiBaseUrl()
                        authApi.login(
                            baseUrl = baseUrl,
                            email = email.trim(),
                            password = password
                        ) { result ->
                            Handler(Looper.getMainLooper()).post {
                                result.onSuccess { tokenResponse ->
                                    sessionManager.setTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                                    sessionManager.setDisplayName(tokenResponse.user.fullName)
                                    sessionManager.setUserId(tokenResponse.user.id)
                                    sessionManager.setUserEmail(tokenResponse.user.email)
                                    sessionManager.recordLoginActivity("Android")
                                    isLoading = false
                                    onSignInSuccess()
                                }.onFailure { error ->
                                    isLoading = false
                                    errorMessage = error.message ?: "Login failed"
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Back to welcome",
                fontSize = 14.sp,
                color = Gray500
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
