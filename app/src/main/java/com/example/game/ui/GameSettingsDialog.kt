package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Authentic Minecraft Bedrock Game Menu & Settings Dialog (Update 0.2.5).
 * Styled with classic stone-gray panels, beveled stone buttons, and removed Invert Y/Fly toggles.
 */
@Composable
fun GameSettingsDialog(
    fov: Float,
    onFovChange: (Float) -> Unit,
    sensitivity: Float,
    onSensitivityChange: (Float) -> Unit,
    fogDistance: Float,
    onFogDistanceChange: (Float) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsToggle: (Boolean) -> Unit,
    onRegenerateWorld: () -> Unit,
    onQuitToTitle: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(420.dp)
                .padding(16.dp)
                .testTag("settings_dialog"),
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFC6C6C6), // Classic Bedrock stone gray GUI
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF373737))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Game Menu",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF282828)
                    )

                    // Bedrock Close button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF373737))
                            .padding(1.dp)
                            .background(Color(0xFF8B8B8B))
                            .clickable(onClick = onDismiss)
                            .testTag("close_settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Settings",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Resume Button
                BedrockMenuButton(
                    text = "Resume Game",
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("resume_game_button")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Settings Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF8B8B8B))
                        .padding(2.dp)
                        .background(Color(0xFF9E9E9E), RoundedCornerShape(3.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Look Sensitivity Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Touch Sensitivity",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF222222)
                                )
                                Text(
                                    text = "${"%.1f".format(sensitivity)}x",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF222222)
                                )
                            }
                            Slider(
                                value = sensitivity,
                                onValueChange = onSensitivityChange,
                                valueRange = 0.5f..2.5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF4A6B3D),
                                    activeTrackColor = Color(0xFF4A6B3D),
                                    inactiveTrackColor = Color(0xFF666666)
                                ),
                                modifier = Modifier.testTag("sensitivity_slider")
                            )
                        }

                        // FOV Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Field of View (FOV)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF222222)
                                )
                                Text(
                                    text = "${fov.toInt()}°",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF222222)
                                )
                            }
                            Slider(
                                value = fov,
                                onValueChange = onFovChange,
                                valueRange = 50f..95f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF4A6B3D),
                                    activeTrackColor = Color(0xFF4A6B3D),
                                    inactiveTrackColor = Color(0xFF666666)
                                ),
                                modifier = Modifier.testTag("fov_slider")
                            )
                        }

                        // Render Distance / Fog Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Render Distance Fog",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF222222)
                                )
                                Text(
                                    text = "${fogDistance.toInt()}m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF222222)
                                )
                            }
                            Slider(
                                value = fogDistance,
                                onValueChange = onFogDistanceChange,
                                valueRange = 30f..80f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF4A6B3D),
                                    activeTrackColor = Color(0xFF4A6B3D),
                                    inactiveTrackColor = Color(0xFF666666)
                                ),
                                modifier = Modifier.testTag("fog_slider")
                            )
                        }

                        // Haptic Feedback Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Vibration Feedback",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF222222)
                                )
                                Text(
                                    text = "Haptics on mining and placing",
                                    fontSize = 11.sp,
                                    color = Color(0xFF444444)
                                )
                            }
                            Switch(
                                checked = hapticsEnabled,
                                onCheckedChange = onHapticsToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF4A6B3D),
                                    checkedTrackColor = Color(0xFF373737)
                                ),
                                modifier = Modifier.testTag("haptics_switch")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Regenerate World Button
                BedrockMenuButton(
                    text = "Regenerate World",
                    onClick = {
                        onRegenerateWorld()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("regenerate_world_button")
                )

                if (onQuitToTitle != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    BedrockMenuButton(
                        text = "Save & Quit to Title",
                        onClick = {
                            onDismiss()
                            onQuitToTitle.invoke()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("quit_to_title_button")
                    )
                }
            }
        }
    }
}

/**
 * Authentic Minecraft Bedrock Stone Menu Button
 */
@Composable
private fun BedrockMenuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF2A2A2A))
            .clickable(onClick = onClick)
            .padding(top = 1.dp, start = 1.dp, end = 2.5.dp, bottom = 2.5.dp)
            .background(Color(0xFFB8B8B8), RoundedCornerShape(2.dp))
            .padding(top = 2.dp, start = 2.dp, end = 1.dp, bottom = 1.dp)
            .background(Color(0xFF8B8B8B), RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
