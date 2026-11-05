package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.BlockType
import com.example.game.engine.WorldMetadata
import com.example.game.engine.WorldSaveManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Minecraft Bedrock Worlds Screen:
 * - Lists all locally saved worlds
 * - Prominent "New" button in the top-right
 * - Play & Delete controls for each world
 */
@Composable
fun WorldsScreen(
    onPlayWorld: (WorldMetadata) -> Unit,
    onCreateNewWorld: () -> Unit,
    onBackToMainMenu: () -> Unit
) {
    val context = LocalContext.current
    var worlds by remember { mutableStateOf(WorldSaveManager.listWorlds(context)) }
    var worldToDelete by remember { mutableStateOf<WorldMetadata?>(null) }

    BackHandler {
        onBackToMainMenu()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF26384E),
                        Color(0xFF162332),
                        Color(0xFF0F1722)
                    )
                )
            )
            .testTag("worlds_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // ---------------- TOP BAR: BACK, TITLE, NEW BUTTON ----------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onBackToMainMenu)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Title",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Centered Title
                Text(
                    text = "Worlds",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif
                )

                // Top Right "New" Button
                BedrockHeaderButton(
                    text = "+ New",
                    isPrimary = true,
                    onClick = onCreateNewWorld,
                    testTag = "create_new_world_button"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---------------- WORLDS LIST ----------------
            if (worlds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No worlds found!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap '+ New' in the top right to create a new world.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(worlds, key = { it.id }) { meta ->
                        WorldCardItem(
                            metadata = meta,
                            onPlay = { onPlayWorld(meta) },
                            onDelete = { worldToDelete = meta }
                        )
                    }
                }
            }
        }

        // ---------------- DELETE CONFIRMATION MODAL ----------------
        worldToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { worldToDelete = null },
                title = {
                    Text(text = "Delete World?", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete '${target.name}'? This cannot be undone.",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            WorldSaveManager.deleteWorld(context, target.id)
                            worlds = WorldSaveManager.listWorlds(context)
                            worldToDelete = null
                        }
                    ) {
                        Text("Delete", color = Color(0xFFFF5555), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { worldToDelete = null }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF262626),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
private fun WorldCardItem(
    metadata: WorldMetadata,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val isFlat = metadata.type.equals("flat", ignoreCase = true)
    val displayDate = remember(metadata.lastPlayed) {
        val sdf = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
        sdf.format(Date(metadata.lastPlayed))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1F2936))
            .border(1.5.dp, Color(0xFF384659), RoundedCornerShape(6.dp))
            .clickable(onClick = onPlay)
            .padding(14.dp)
            .testTag("world_item_${metadata.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Isometric block icon preview
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF141C25)),
                contentAlignment = Alignment.Center
            ) {
                IsometricBlockIcon(
                    block = if (isFlat) BlockType.BEDROCK else BlockType.GRASS,
                    size = 38.dp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = metadata.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Type Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isFlat) Color(0xFF7A4822) else Color(0xFF2E6325))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isFlat) "Flat" else "Infinite",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last played: $displayDate • Seed: ${metadata.seed}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF3C6A2A))
                    .clickable(onClick = onPlay)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete World",
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BedrockHeaderButton(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val highlight = if (isPrimary) Color(0xFF5A8C46) else Color(0xFF8B8B8B)
    val shadow = if (isPrimary) Color(0xFF224416) else Color(0xFF373737)
    val base = if (isPrimary) Color(0xFF3C6A2A) else Color(0xFF5C5C5C)

    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(shadow)
            .padding(top = 1.dp, start = 1.dp, end = 2.dp, bottom = 2.dp)
            .background(highlight, RoundedCornerShape(3.dp))
            .padding(top = 1.5.dp, start = 1.5.dp, end = 1.dp, bottom = 1.dp)
            .background(base, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = FontFamily.SansSerif
        )
    }
}
