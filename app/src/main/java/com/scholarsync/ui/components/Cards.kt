package com.scholarsync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarsync.ui.theme.*

@Composable
fun PaperCard(
    title: String,
    description: String,
    source: String,
    sourceColor: Color = AccentTeal,
    onClick: () -> Unit,
    isSavedToLibrary: Boolean = false,
    onSaveToLibraryClick: (() -> Unit)? = null,
    authors: List<String> = emptyList(),
    citationCount: Int = 0,
    isRead: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRead) Color.White.copy(alpha = 0.85f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRead) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(sourceColor.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = source,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = sourceColor
                        )
                    }
                    if (isRead) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF22C55E).copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "READ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22C55E)
                            )
                        }
                    }
                }
                if (isSavedToLibrary) {
                    Icon(
                        imageVector = Icons.Filled.LibraryAddCheck,
                        contentDescription = "Saved to library",
                        tint = AccentTeal,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (onSaveToLibraryClick != null) {
                    IconButton(
                        onClick = onSaveToLibraryClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LibraryAdd,
                            contentDescription = "Save to library",
                            tint = Gray400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                color = Gray500,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Author initials + names
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val displayAuthors = authors.take(3)
                    // Overlapping initial circles
                    if (displayAuthors.isNotEmpty()) {
                        Box(modifier = Modifier.width((displayAuthors.size * 20 + 8).dp).height(28.dp)) {
                            displayAuthors.forEachIndexed { index, author ->
                                val initials = author.split(" ")
                                    .filter { it.isNotBlank() }
                                    .take(2)
                                    .joinToString("") { it.first().uppercase() }
                                    .ifEmpty { "?" }
                                val bgColors = listOf(Primary, AccentTeal, AccentGold)
                                Box(
                                    modifier = Modifier
                                        .offset(x = (index * 20).dp)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(bgColors[index % bgColors.size]),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials.take(2),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        val authorLabel = when {
                            authors.size == 1 -> authors[0].split(" ").lastOrNull() ?: ""
                            authors.size == 2 -> "${authors[0].split(" ").lastOrNull() ?: ""} & ${authors[1].split(" ").lastOrNull() ?: ""}"
                            authors.size > 2 -> "${authors[0].split(" ").lastOrNull() ?: ""} +${authors.size - 1}"
                            else -> ""
                        }
                        if (authorLabel.isNotBlank()) {
                            Text(
                                text = authorLabel,
                                fontSize = 12.sp,
                                color = Gray500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Summary button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary.copy(alpha = 0.05f))
                        .clickable { onClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Summary",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            }
        }
    }
}

@Composable
fun FolderCard(
    name: String,
    paperCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$paperCount Papers",
                fontSize = 10.sp,
                color = Gray500
            )
        }
    }
}

@Composable
fun AlertCard(
    title: String,
    description: String,
    time: String,
    isNew: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNew) Color.White else Color.White.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNew) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isNew) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Primary)
                )
            }
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = time,
                        fontSize = 10.sp,
                        color = Gray400
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Gray500,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
