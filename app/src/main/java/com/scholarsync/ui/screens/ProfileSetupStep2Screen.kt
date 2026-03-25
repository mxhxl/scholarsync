package com.scholarsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.data.SessionManager
import com.scholarsync.ui.components.*
import com.scholarsync.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupStep2Screen(
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }

    var keywords by remember { mutableStateOf(listOf("federated learning", "differential privacy")) }
    var keywordInput by remember { mutableStateOf("") }
    var authorSearch by remember { mutableStateOf("") }
    var selectedAuthors by remember { mutableStateOf(listOf("Yann LeCun")) }
    var institution by remember { mutableStateOf("") }
    var showAuthorDropdown by remember { mutableStateOf(false) }

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
                    text = "Step 2 of 3",
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
                        .fillMaxWidth(0.66f)
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
                text = "Refine your interests",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add specific keywords and authors to personalize your feed.",
                fontSize = 14.sp,
                color = Gray500
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Keywords section
            Text(
                text = "Keywords",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
            Text(
                text = "Press enter after each keyword",
                fontSize = 12.sp,
                color = Gray500
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScholarTextField(
                value = keywordInput,
                onValueChange = { keywordInput = it },
                placeholder = "Add keywords..."
            )

            Spacer(modifier = Modifier.height(12.dp))

            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                keywords.forEach { keyword ->
                    TagChip(
                        text = keyword,
                        selected = true,
                        onRemove = { keywords = keywords - keyword }
                    )
                }
                TagChip(
                    text = "NLP",
                    selected = false,
                    onClick = { keywords = keywords + "NLP" }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Authors section
            Text(
                text = "Authors to Follow",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box {
                SearchField(
                    value = authorSearch,
                    onValueChange = {
                        authorSearch = it
                        showAuthorDropdown = it.isNotEmpty()
                    },
                    placeholder = "Search authors..."
                )

                // Dropdown
                if (showAuthorDropdown) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column {
                            AuthorSearchResult(
                                name = "Yoshua Bengio",
                                institution = "Mila, Université de Montréal",
                                initials = "YB",
                                onClick = {
                                    selectedAuthors = selectedAuthors + "Yoshua Bengio"
                                    authorSearch = ""
                                    showAuthorDropdown = false
                                }
                            )
                            Divider(color = Gray100)
                            AuthorSearchResult(
                                name = "Yoshua Baker",
                                institution = "Stanford University",
                                initials = null,
                                onClick = {
                                    selectedAuthors = selectedAuthors + "Yoshua Baker"
                                    authorSearch = ""
                                    showAuthorDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedAuthors.forEach { author ->
                    TagChip(
                        text = author,
                        selected = true,
                        onRemove = { selectedAuthors = selectedAuthors - author }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Institution
            Text(
                text = "Current Institution ",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
            Text(
                text = "(Optional)",
                fontSize = 14.sp,
                color = Gray400
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScholarTextField(
                value = institution,
                onValueChange = { institution = it },
                placeholder = "e.g. MIT, Mila, Google Research"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pro tip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Gray100)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Pro Tip: Adding specific authors helps our AI model recommend papers that align with their research methodologies and current focus areas.",
                    fontSize = 12.sp,
                    color = Gray600,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Bottom buttons - above system nav bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.98f))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SecondaryButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.weight(0.35f)
            )
            PrimaryButton(
                text = "Continue",
                onClick = {
                    sessionManager.setOnboardingList("keywords", keywords)
                    sessionManager.setOnboardingList("authors", selectedAuthors)
                    if (institution.isNotBlank()) {
                        sessionManager.setOnboardingData("institution", institution.trim())
                    }
                    onContinue()
                },
                modifier = Modifier.weight(0.65f)
            )
        }
    }
}

@Composable
private fun AuthorSearchResult(
    name: String,
    institution: String,
    initials: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (initials != null) Primary.copy(alpha = 0.1f) else Gray100),
            contentAlignment = Alignment.Center
        ) {
            if (initials != null) {
                Text(
                    text = initials,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Gray400
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = institution,
                fontSize = 12.sp,
                color = Gray500
            )
        }
    }
}
