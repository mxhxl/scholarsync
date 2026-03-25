package com.scholarsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.ui.theme.*

val TICKET_ISSUE_OPTIONS = listOf(
    "Feed or Discover not loading",
    "Summaries not showing",
    "Alerts not working",
    "Login or session issues",
    "Library or folders issue",
    "Account or profile problem",
    "App crash or freeze",
    "Other"
)

@Composable
fun RaiseTicketScreen(onBack: () -> Unit, onSubmit: () -> Unit) {
    var selectedIssue by remember { mutableStateOf<String?>(null) }
    var showDropdown by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var otherIssue by remember { mutableStateOf("") }

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
                text = "Raise a Ticket",
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
            Text(
                text = "Select the issue that best describes your problem, then add details. We’ll get back to you as soon as we can.",
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 22.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Text(
                text = "Issue type",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedIssue ?: "Choose an issue",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDropdown = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Primary.copy(alpha = 0.5f),
                        focusedTextColor = Primary,
                        unfocusedTextColor = if (selectedIssue != null) Primary else Gray500
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
                ) {
                    TICKET_ISSUE_OPTIONS.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    color = Primary,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                selectedIssue = option
                                showDropdown = false
                            }
                        )
                    }
                }
            }

            if (selectedIssue == "Other") {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = otherIssue,
                    onValueChange = { otherIssue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Describe your issue") },
                    placeholder = { Text("e.g. Sync problem with multiple devices") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Primary.copy(alpha = 0.5f),
                        focusedLabelColor = Primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Additional details (optional)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = { Text("Any extra information that might help us…") },
                minLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Primary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = {
                    // In a real app: send ticket to backend, then onSubmit()
                    onSubmit()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit Ticket", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
