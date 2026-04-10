package com.scholarsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import android.os.Handler
import android.os.Looper
import com.scholarsync.api.AuthApiImpl
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.components.*
import com.scholarsync.ui.theme.*

/** Returns error message or null if email is valid. */
private fun validateEmail(email: String): String? {
    val trimmed = email.trim()
    if (trimmed.isEmpty()) return "Email is required."
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) return "Please enter a valid email address."
    return null
}

/** Returns error message or null if password is strong enough. Matches backend rules. */
private fun validatePasswordStrength(password: String): String? {
    if (password.length < 8) return "Password must be at least 8 characters long."
    if (!password.any { it.isUpperCase() }) return "Password must contain at least one uppercase letter."
    if (!password.any { it.isLowerCase() }) return "Password must contain at least one lowercase letter."
    if (!password.any { it.isDigit() }) return "Password must contain at least one digit."
    return null
}

@Composable
fun ProfileSetupStep1Screen(
    onContinue: (displayName: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    val authApi = remember { AuthApiImpl() }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var registerError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("PhD Student") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf(listOf("Machine Learning", "Computer Vision")) }
    var university by remember { mutableStateOf("") }

    val roles = listOf("PhD Student", "Professor", "R&D Specialist", "Other")
    val allInterests = listOf("NLP", "Robotics", "Ethics in AI", "Deep Learning", "Data Science")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Header
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = Primary
                    )
                }
                Text(
                    text = "Step 1 of 3",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(40.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Gray100)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.33f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Primary)
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Build your profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tell us your name, interests, and university to personalize your experience.",
                fontSize = 14.sp,
                color = Gray500
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Name
            Text(
                text = "Full name",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            ScholarTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "e.g. Alex Rivera",
                leadingIcon = Icons.Outlined.Person
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Email
            Text(
                text = "Email",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            ScholarTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                placeholder = "e.g. alex@university.edu",
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email
            )
            if (emailError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = emailError!!,
                    fontSize = 12.sp,
                    color = Error,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Password
            Text(
                text = "Password",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            ScholarTextField(
                value = password,
                onValueChange = { password = it; passwordError = null },
                placeholder = "Min 8 chars, upper, lower, digit",
                leadingIcon = Icons.Outlined.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password
            )
            if (passwordError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = passwordError!!,
                    fontSize = 12.sp,
                    color = Error,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirm password
            Text(
                text = "Confirm password",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            ScholarTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; confirmPasswordError = null },
                placeholder = "Re-enter your password",
                leadingIcon = Icons.Outlined.Lock,
                visualTransformation = PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password
            )
            if (confirmPasswordError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = confirmPasswordError!!,
                    fontSize = 12.sp,
                    color = Error,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Role selection
            Text(
                text = "I am a...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    roles.take(2).forEach { role ->
                        SelectableChip(
                            text = role,
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    roles.drop(2).forEach { role ->
                        SelectableChip(
                            text = role,
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Research Interests
            Text(
                text = "Research Interests",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            SearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search fields..."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Selected and available tags
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedInterests.forEach { interest ->
                    TagChip(
                        text = interest,
                        selected = true,
                        onRemove = {
                            selectedInterests = selectedInterests - interest
                        }
                    )
                }
                allInterests.filter { it !in selectedInterests }.forEach { interest ->
                    TagChip(
                        text = interest,
                        selected = false,
                        onClick = {
                            selectedInterests = selectedInterests + interest
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // University
            Text(
                text = "University",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScholarTextField(
                value = university,
                onValueChange = { university = it },
                placeholder = "e.g. Stanford University"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom button - above system nav bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            if (registerError != null) {
                Text(
                    text = registerError!!,
                    fontSize = 13.sp,
                    color = Error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            PrimaryButton(
                text = if (isLoading) "Creating account..." else "Continue",
                onClick = {
                    emailError = validateEmail(email)
                    passwordError = validatePasswordStrength(password)
                    confirmPasswordError = when {
                        confirmPassword != password -> "Passwords do not match."
                        else -> null
                    }
                    if (emailError == null && passwordError == null && confirmPasswordError == null) {
                        registerError = null
                        isLoading = true
                        val baseUrl = sessionManager.getApiBaseUrl()
                        authApi.register(
                            baseUrl = baseUrl,
                            email = email.trim(),
                            password = password,
                            fullName = name.trim()
                        ) { result ->
                            Handler(Looper.getMainLooper()).post {
                                result.onSuccess { tokenResponse ->
                                    sessionManager.setTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                                    sessionManager.setDisplayName(tokenResponse.user.fullName)
                                    sessionManager.setUserId(tokenResponse.user.id)
                                    sessionManager.setUserEmail(tokenResponse.user.email)
                                    sessionManager.recordLoginActivity("Android")
                                    // Save Step1 profile data for later API call
                                    sessionManager.setOnboardingData("role", selectedRole)
                                    sessionManager.setOnboardingList("interests", selectedInterests)
                                    sessionManager.setOnboardingData("university", university.trim())
                                    isLoading = false
                                    onContinue(name.trim())
                                }.onFailure { error ->
                                    isLoading = false
                                    registerError = error.message ?: "Registration failed"
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading && name.isNotBlank() && email.isNotBlank() &&
                    password.isNotBlank() && confirmPassword.isNotBlank()
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    content: @Composable () -> Unit
) {
    // Simple implementation - in production use accompanist FlowRow
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
