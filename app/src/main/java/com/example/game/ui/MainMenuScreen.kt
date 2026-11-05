package com.example.game.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.BlockType

/**
 * Classic Bedrock Title Screen Main Menu:
 * - "NovaCraft" pixelated stone title logo
 * - "Play Game" and "Settings" Bedrock stone buttons
 * - Version tag and world info
 */
@Composable
fun MainMenuScreen(
    onPlayGame: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2C4A75), // Deep sky blue
                        Color(0xFF1B2C46),
                        Color(0xFF101C2B)
                    )
                )
            )
            .testTag("main_menu_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // NovaCraft Logo Banner
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E1E1E))
                    .border(3.dp, Color(0xFF5A5A5A), RoundedCornerShape(6.dp))
                    .padding(horizontal = 28.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IsometricBlockIcon(block = BlockType.GRASS, size = 38.dp)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NOVACRAFT",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE0E0E0),
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "POCKET VOXEL EDITION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Main Menu Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(260.dp)
            ) {
                BedrockMenuButton(
                    text = "Play Game",
                    isPrimary = true,
                    onClick = onPlayGame,
                    testTag = "play_game_button"
                )

                BedrockMenuButton(
                    text = "Settings",
                    isPrimary = false,
                    onClick = onOpenSettings,
                    testTag = "menu_settings_button"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Version info footer
            Text(
                text = "NovaCraft v1.1.5 • Infinite & Flat Worlds",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun BedrockMenuButton(
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
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(shadow)
            .padding(top = 1.dp, start = 1.dp, end = 2.5.dp, bottom = 2.5.dp)
            .background(highlight, RoundedCornerShape(3.dp))
            .padding(top = 2.dp, start = 2.dp, end = 1.5.dp, bottom = 1.5.dp)
            .background(base, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = FontFamily.SansSerif
        )
    }
}
