package com.scholarsync.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.api.AuthApi
import com.scholarsync.api.AuthApiImpl
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.components.PrimaryButton
import com.scholarsync.ui.components.ScholarTextField
import com.scholarsync.ui.theme.*

@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit = {},
    authApi: AuthApi = AuthApiImpl(),
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray100)
            .windowInsetsPadding(WindowInsets.statusBars)
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
                .padding(horizontal = 24.dp)
                .padding(top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Primary)
            }
            Text(
                text = "Change Password",
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
            Text(
                text = "Enter your current password and choose a new one. The new password will be saved to your account.",
                fontSize = 14.sp,
                color = Gray500,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            ScholarTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it; errorMessage = null },
                placeholder = "Current password",
                leadingIcon = Icons.Outlined.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password
            )
            Spacer(modifier = Modifier.height(16.dp))
            ScholarTextField(
                value = newPassword,
                onValueChange = { newPassword = it; errorMessage = null },
                placeholder = "New password",
                leadingIcon = Icons.Outlined.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password
            )
            Spacer(modifier = Modifier.height(16.dp))
            ScholarTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                placeholder = "Confirm new password",
                leadingIcon = Icons.Outlined.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    fontSize = 13.sp,
                    color = Error,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            if (successMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = successMessage!!,
                    fontSize = 13.sp,
                    color = Success,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = if (isLoading) "Updating…" else "Update password",
                onClick = {
                    when {
                        currentPassword.isBlank() -> errorMessage = "Enter your current password"
                        newPassword.isBlank() -> errorMessage = "Enter a new password"
                        newPassword.length < 6 -> errorMessage = "New password must be at least 6 characters"
                        newPassword != confirmPassword -> errorMessage = "New password and confirmation do not match"
                        else -> {
                            errorMessage = null
                            successMessage = null
                            isLoading = true
                            val baseUrl = sessionManager.getApiBaseUrl()
                            val token = sessionManager.getApiKey()
                            val mainHandler = Handler(Looper.getMainLooper())
                            authApi.changePassword(
                                baseUrl = baseUrl,
                                bearerToken = token,
                                currentPassword = currentPassword,
                                newPassword = newPassword,
                                onResult = { result ->
                                    mainHandler.post {
                                        isLoading = false
                                        result.fold(
                                            onSuccess = {
                                                successMessage = "Password updated successfully."
                                                currentPassword = ""
                                                newPassword = ""
                                                confirmPassword = ""
                                            },
                                            onFailure = { e ->
                                                val msg = e.message ?: "Failed to update password"
                                                errorMessage = if (msg.contains("400") || msg.contains("invalid_password"))
                                                    "Current password is incorrect."
                                                else msg
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
        }
    }
}
