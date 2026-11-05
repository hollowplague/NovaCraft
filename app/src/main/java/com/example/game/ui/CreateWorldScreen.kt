package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.example.game.engine.WorldSaveData
import com.example.game.engine.WorldSaveManager

/**
 * Bedrock-style Create New World Screen:
 * - World Name input
 * - World Type Selector: Infinite vs Flat
 * - World Seed input with Randomize button
 * - Create & Cancel actions
 */
@Composable
fun CreateWorldScreen(
    onWorldCreated: (WorldSaveData) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var worldName by remember { mutableStateOf("New World") }
    var worldType by remember { mutableStateOf("infinite") } // "infinite" or "flat"
    var seedText by remember { mutableStateOf("${(100000..999999).random()}") }

    BackHandler {
        onCancel()
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
            .testTag("create_world_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Create New World",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ---------------- 1. WORLD NAME ----------------
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "World Name",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = worldName,
                    onValueChange = { worldName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF5A8C46),
                        unfocusedBorderColor = Color(0xFF4A5568),
                        focusedContainerColor = Color(0xFF1A202C),
                        unfocusedContainerColor = Color(0xFF1A202C)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("world_name_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------------- 2. WORLD TYPE SELECTOR ----------------
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "World Type",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WorldTypeOptionCard(
                        title = "Infinite",
                        description = "Rolling hills, oak trees, water, sand beaches & caves",
                        iconBlock = BlockType.GRASS,
                        isSelected = worldType == "infinite",
                        onSelect = { worldType = "infinite" },
                        modifier = Modifier.weight(1f)
                    )
                    WorldTypeOptionCard(
                        title = "Flat",
                        description = "Classic Bedrock flat: 1 Bedrock, 2 Dirt, 1 Grass surface",
                        iconBlock = BlockType.BEDROCK,
                        isSelected = worldType == "flat",
                        onSelect = { worldType = "flat" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------------- 3. WORLD SEED ----------------
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Seed",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = seedText,
                        onValueChange = { seedText = it },
                        singleLine = true,
                        placeholder = { Text("Enter numeric or text seed", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF5A8C46),
                            unfocusedBorderColor = Color(0xFF4A5568),
                            focusedContainerColor = Color(0xFF1A202C),
                            unfocusedContainerColor = Color(0xFF1A202C)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("world_seed_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { seedText = "${(100000..999999).random()}" },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2D3748))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Randomize Seed",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---------------- 4. ACTIONS: CREATE & CANCEL ----------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cancel Button
                BedrockActionButton(
                    text = "Cancel",
                    isPrimary = false,
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    testTag = "cancel_create_world_button"
                )

                // Create World Button
                BedrockActionButton(
                    text = "Create World",
                    isPrimary = true,
                    onClick = {
                        val parsedSeed = seedText.trim().toLongOrNull()
                            ?: seedText.hashCode().toLong()
                        val newWorldData = WorldSaveManager.createNewWorld(
                            context = context,
                            name = worldName,
                            type = worldType,
                            seed = parsedSeed
                        )
                        onWorldCreated(newWorldData)
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "confirm_create_world_button"
                )
            }
        }
    }
}

@Composable
private fun WorldTypeOptionCard(
    title: String,
    description: String,
    iconBlock: BlockType,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Color(0xFF5A8C46) else Color(0xFF384659)
    val bgColor = if (isSelected) Color(0xFF1E3A24) else Color(0xFF1A232F)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onSelect)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IsometricBlockIcon(block = iconBlock, size = 32.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF86E26E) else Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun BedrockActionButton(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val highlight = if (isPrimary) Color(0xFF5A8C46) else Color(0xFF8B8B8B)
    val shadow = if (isPrimary) Color(0xFF224416) else Color(0xFF373737)
    val base = if (isPrimary) Color(0xFF3C6A2A) else Color(0xFF5C5C5C)

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(shadow)
            .padding(top = 1.dp, start = 1.dp, end = 2.dp, bottom = 2.dp)
            .background(highlight, RoundedCornerShape(3.dp))
            .padding(top = 2.dp, start = 2.dp, end = 1.dp, bottom = 1.dp)
            .background(base, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = FontFamily.SansSerif
        )
    }
}
