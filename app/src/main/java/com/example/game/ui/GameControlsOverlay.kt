package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.BlockType
import com.example.game.engine.GameRenderer
import com.example.game.engine.Player
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Authentic Minecraft Pocket Edition (MCPE) Touch Interface (Update 0.2.5):
 * - NO Crosshair.
 * - NO separate Break and Place buttons.
 * - Interactive MCPE Hollow Circle that fills from the center during block mining.
 * - Direct block placement on quick tap.
 * - Redesigned square stone Jump button on bottom right.
 * - Bedrock Cross D-Pad on bottom left (center button jump disabled).
 * - Immersive clean top bar (coordinates removed, Bedrock pause button in top right).
 */
@Composable
fun GameControlsOverlay(
    player: Player,
    renderer: GameRenderer,
    sensitivity: Float,
    hotbarSlots: List<BlockType>,
    selectedSlotIndex: Int,
    onSelectSlot: (Int) -> Unit,
    onOpenInventory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    // Touch circle position in screen coordinates
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var isHoldingDown by remember { mutableStateOf(false) }
    var touchStartTime by remember { mutableLongStateOf(0L) }
    var lastLookPos by remember { mutableStateOf<Offset?>(null) }

    val selectedBlock = hotbarSlots.getOrElse(selectedSlotIndex) { BlockType.AIR }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("game_controls_overlay")
    ) {
        // ---------------- 1. MCPE SCREEN TOUCH CONTROLLER (LOOK / TAP / MINE) ----------------
        // Covers right side and middle, clear of D-Pad and bottom hotbar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 180.dp, bottom = 70.dp) // Leave D-Pad and hotbar zones free
                .pointerInput(sensitivity, selectedBlock) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            touchStartTime = System.currentTimeMillis()
                            touchPos = offset
                            lastLookPos = offset
                            isHoldingDown = true

                            // If targeting a block, begin MCPE mining
                            renderer.currentRaycastHit?.let { hit ->
                                renderer.startMining(hit)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val currentPos = change.position
                            touchPos = currentPos

                            val startP = lastLookPos ?: currentPos
                            val deltaX = currentPos.x - startP.x
                            val deltaY = currentPos.y - startP.y
                            val distSq = deltaX * deltaX + deltaY * deltaY

                            // If dragging significantly, it's a camera look gesture
                            if (distSq > 36f) {
                                renderer.stopMining()
                                val rotX = dragAmount.x * 0.12f * sensitivity
                                val rotY = -dragAmount.y * 0.12f * sensitivity
                                player.rotate(rotX, rotY)
                            }
                            lastLookPos = currentPos
                        },
                        onDragEnd = {
                            val elapsed = System.currentTimeMillis() - touchStartTime
                            // Quick tap (< 220ms) with minimal drag -> Place Block
                            if (elapsed < 220L) {
                                renderer.currentRaycastHit?.let { hit ->
                                    renderer.placeBlockAt(hit, selectedBlock)
                                } ?: run {
                                    // Empty hand punch swing
                                    renderer.viewmodelRenderer.triggerSwing()
                                }
                            }
                            renderer.stopMining()
                            touchPos = null
                            isHoldingDown = false
                            lastLookPos = null
                        },
                        onDragCancel = {
                            renderer.stopMining()
                            touchPos = null
                            isHoldingDown = false
                            lastLookPos = null
                        }
                    )
                }
                .pointerInput(selectedBlock) {
                    detectTapGestures(
                        onTap = {
                            // Tap placing
                            renderer.currentRaycastHit?.let { hit ->
                                renderer.placeBlockAt(hit, selectedBlock)
                            } ?: run {
                                renderer.viewmodelRenderer.triggerSwing()
                            }
                        }
                    )
                }
        )

        // ---------------- 2. MCPE HOLLOW BREAKING CIRCLE (CENTER FILL) ----------------
        // Appears at touch position when mining a block
        if (renderer.isMining && touchPos != null) {
            val pos = touchPos!!
            val progress = renderer.miningProgress
            val circleRadiusPx = 32.dp.value * 2.5f

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (pos.x - circleRadiusPx + 180.dp.roundToPx()).roundToInt(),
                            (pos.y - circleRadiusPx).roundToInt()
                        )
                    }
                    .size(64.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.width / 2f - 4f

                    // Outer Hollow Circle Border
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.45f),
                        radius = maxRadius,
                        center = center,
                        style = Stroke(width = 4.5f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.95f),
                        radius = maxRadius,
                        center = center,
                        style = Stroke(width = 3.0f)
                    )

                    // Inner radial progress filling outward from the center
                    val fillRadius = maxRadius * progress.coerceIn(0f, 1f)
                    if (fillRadius > 1f) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = fillRadius,
                            center = center
                        )
                    }

                    // Center focus dot
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f,
                        center = center
                    )
                }
            }
        }

        // ---------------- 3. CLEAN TOP BAR (PAUSE BUTTON ONLY) ----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            // Authentic Minecraft Bedrock Pause Button
            BedrockPauseButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("open_settings_button")
            )
        }

        // ---------------- 4. BEDROCK SQUARE CROSS D-PAD (CENTER JUMP REMOVED) ----------------
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            BedrockCrossDPad(
                onMove = { forward, right ->
                    player.inputForward = forward
                    player.inputRight = right
                }
            )
        }

        // ---------------- 5. REDESIGNED BEDROCK JUMP BUTTON (BOTTOM RIGHT) ----------------
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        ) {
            BedrockJumpButton(
                onClick = { player.inputJump = true },
                modifier = Modifier.testTag("action_jump")
            )
        }

        // ---------------- 6. BOTTOM HOTBAR ----------------
        HotbarRow(
            slots = hotbarSlots,
            selectedIndex = selectedSlotIndex,
            onSelectSlot = onSelectSlot,
            onOpenInventory = onOpenInventory,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

/**
 * Redesigned Bedrock Jump button:
 * Square stone button with iconic jump chevron arrow.
 */
@Composable
private fun BedrockJumpButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(62.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF2A2A2A)) // Outer shadow
            .clickable(onClick = onClick)
            .padding(top = 1.dp, start = 1.dp, end = 3.dp, bottom = 3.dp)
            .background(Color(0xFFB8B8B8), RoundedCornerShape(3.dp)) // Highlight
            .padding(top = 2.dp, start = 2.dp, end = 1.5.dp, bottom = 1.5.dp)
            .background(Color(0xFF8B8B8B), RoundedCornerShape(2.dp)), // Stone surface
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "▲",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Authentic Minecraft Bedrock Pause button:
 * Stone square button with 2 white vertical bars.
 */
@Composable
private fun BedrockPauseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF373737))
            .padding(top = 1.dp, start = 1.dp, end = 2.dp, bottom = 2.dp)
            .background(Color(0xFFB8B8B8), RoundedCornerShape(2.dp))
            .padding(top = 2.dp, start = 2.dp, end = 1.dp, bottom = 1.dp)
            .background(Color(0xFF8B8B8B), RoundedCornerShape(2.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 3.5.dp, height = 14.dp)
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .size(width = 3.5.dp, height = 14.dp)
                    .background(Color.White)
            )
        }
    }
}

/**
 * Authentic Bedrock Cross D-Pad:
 * 5 square stone-beveled buttons.
 * Center button jump is REMOVED per user request (passive center anchor).
 */
@Composable
private fun BedrockCrossDPad(
    onMove: (forward: Float, right: Float) -> Unit
) {
    val buttonSize = 42.dp
    var activeDirection by remember { mutableStateOf<String?>(null) }

    fun processPosition(offset: Offset, sizePx: Float) {
        val center = sizePx / 2f
        val dx = offset.x - center
        val dy = offset.y - center
        val dist = sqrt(dx * dx + dy * dy)

        // Center button: passive anchor (jump removed)
        if (dist < 26f) {
            activeDirection = "CENTER"
            onMove(0f, 0f)
            return
        }

        // Calculate 8-way directional movement
        val angleDeg = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val normalizedAngle = (angleDeg + 360f) % 360f

        when (normalizedAngle) {
            in 337.5f..360f, in 0f..22.5f -> { // Right
                activeDirection = "RIGHT"
                onMove(0f, 1f)
            }
            in 22.5f..67.5f -> { // Down-Right
                activeDirection = "DOWN_RIGHT"
                onMove(-0.75f, 0.75f)
            }
            in 67.5f..112.5f -> { // Down
                activeDirection = "DOWN"
                onMove(-1f, 0f)
            }
            in 112.5f..157.5f -> { // Down-Left
                activeDirection = "DOWN_LEFT"
                onMove(-0.75f, -0.75f)
            }
            in 157.5f..202.5f -> { // Left
                activeDirection = "LEFT"
                onMove(0f, -1f)
            }
            in 202.5f..247.5f -> { // Up-Left
                activeDirection = "UP_LEFT"
                onMove(0.75f, -0.75f)
            }
            in 247.5f..292.5f -> { // Up
                activeDirection = "UP"
                onMove(1f, 0f)
            }
            in 292.5f..337.5f -> { // Up-Right
                activeDirection = "UP_RIGHT"
                onMove(0.75f, 0.75f)
            }
        }
    }

    Box(
        modifier = Modifier
            .size(buttonSize * 3)
            .testTag("virtual_joystick")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        processPosition(offset, size.width.toFloat())
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        processPosition(change.position, size.width.toFloat())
                    },
                    onDragEnd = {
                        activeDirection = null
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        activeDirection = null
                        onMove(0f, 0f)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        processPosition(offset, size.width.toFloat())
                        tryAwaitRelease()
                        activeDirection = null
                        onMove(0f, 0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Row 1: UP button
            BedrockDPadSquare(
                symbol = "▲",
                size = buttonSize,
                isPressed = activeDirection in listOf("UP", "UP_LEFT", "UP_RIGHT")
            )

            // Row 2: LEFT, CENTER (PASSIVE), RIGHT
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                BedrockDPadSquare(
                    symbol = "◄",
                    size = buttonSize,
                    isPressed = activeDirection in listOf("LEFT", "UP_LEFT", "DOWN_LEFT")
                )
                BedrockDPadSquare(
                    symbol = "•",
                    size = buttonSize,
                    isPressed = activeDirection == "CENTER"
                )
                BedrockDPadSquare(
                    symbol = "►",
                    size = buttonSize,
                    isPressed = activeDirection in listOf("RIGHT", "UP_RIGHT", "DOWN_RIGHT")
                )
            }

            // Row 3: DOWN button
            BedrockDPadSquare(
                symbol = "▼",
                size = buttonSize,
                isPressed = activeDirection in listOf("DOWN", "DOWN_LEFT", "DOWN_RIGHT")
            )
        }
    }
}

@Composable
private fun BedrockDPadSquare(
    symbol: String,
    size: androidx.compose.ui.unit.Dp,
    isPressed: Boolean
) {
    val highlightColor = if (isPressed) Color(0xFF373737) else Color(0xFFB8B8B8)
    val shadowColor = if (isPressed) Color(0xFFB8B8B8) else Color(0xFF2A2A2A)
    val baseColor = if (isPressed) Color(0xFF6E6E6E) else Color(0xFF8B8B8B)

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(2.dp))
            .background(shadowColor)
            .padding(top = 1.dp, start = 1.dp, end = 2.dp, bottom = 2.dp)
            .background(highlightColor, RoundedCornerShape(1.dp))
            .padding(top = 1.5.dp, start = 1.5.dp, end = 1.dp, bottom = 1.dp)
            .background(baseColor, RoundedCornerShape(1.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = if (symbol == "•") 18.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Authentic Minecraft Bedrock Mobile Hotbar:
 * Supports empty slots with subtle centered inset dot.
 */
@Composable
private fun HotbarRow(
    slots: List<BlockType>,
    selectedIndex: Int,
    onSelectSlot: (Int) -> Unit,
    onOpenInventory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("hotbar_container"),
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF8B8B8B),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF373737))
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            slots.forEachIndexed { index, block ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isSelected) Color(0xFFFFFFFF) else Color(0xFF373737))
                        .padding(if (isSelected) 2.5.dp else 1.5.dp)
                        .background(Color(0xFF8F8F8F))
                        .clickable { onSelectSlot(index) }
                        .testTag("hotbar_slot_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    if (block != BlockType.AIR) {
                        IsometricBlockIcon(block = block, size = 26.dp)
                    } else {
                        // Subtle empty slot marker
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color(0xFF707070), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            // Inventory drawer toggle button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF373737))
                    .padding(1.5.dp)
                    .background(Color(0xFF6B7280))
                    .clickable(onClick = onOpenInventory)
                    .testTag("inventory_drawer_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Open Inventory",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
