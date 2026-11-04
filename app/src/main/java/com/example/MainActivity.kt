package com.example

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.game.engine.BlockType
import com.example.game.engine.GameRenderer
import com.example.game.engine.Player
import com.example.game.engine.VoxelWorld
import com.example.game.ui.CreativeInventoryDialog
import com.example.game.ui.GameControlsOverlay
import com.example.game.ui.GameSettingsDialog
import com.example.ui.theme.MyApplicationTheme

/**
 * Main Activity for Minecraft Kotlin Edition.
 * Hosts the continuous OpenGL ES 2.0 voxel render loop and Jetpack Compose mobile touch HUD.
 */
class MainActivity : ComponentActivity() {

    private lateinit var world: VoxelWorld
    private lateinit var player: Player
    private lateinit var renderer: GameRenderer
    private var glSurfaceView: GLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        // 1. Initialize Engine & World
        world = VoxelWorld()

        // 2. Spawn player on top of surface terrain ready for walking
        val spawnX = 0f
        val spawnZ = 0f
        val topY = world.getTopSolidBlockY(0, 0).toFloat()
        player = Player(posX = spawnX, posY = topY + 1.0f, posZ = spawnZ).apply {
            isFlying = false
            isOnGround = true
        }

        // 3. Initialize OpenGL Renderer
        renderer = GameRenderer(world, player)

        // 4. Setup Haptic Feedback
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        setContent {
            MyApplicationTheme {
                MinecraftGameApp(
                    world = world,
                    player = player,
                    renderer = renderer,
                    onVibrate = { durationMs ->
                        vibrator?.let { v ->
                            if (v.hasVibrator()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    v.vibrate(
                                        VibrationEffect.createOneShot(
                                            durationMs,
                                            VibrationEffect.DEFAULT_AMPLITUDE
                                        )
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    v.vibrate(durationMs)
                                }
                            }
                        }
                    },
                    onSurfaceCreated = { surface ->
                        glSurfaceView = surface
                    }
                )
            }
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView?.onResume()
        hideSystemBars()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView?.onPause()
    }
}

@Composable
fun MinecraftGameApp(
    world: VoxelWorld,
    player: Player,
    renderer: GameRenderer,
    onVibrate: (Long) -> Unit,
    onSurfaceCreated: (GLSurfaceView) -> Unit
) {
    // Hotbar state (9 slots, starts empty in 0.2.5)
    var hotbarSlots by remember { mutableStateOf(List(9) { BlockType.AIR }) }
    var selectedSlotIndex by remember { mutableIntStateOf(0) }

    // Dialogs
    var showInventoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Game preferences
    var fov by remember { mutableFloatStateOf(70f) }
    var sensitivity by remember { mutableFloatStateOf(1.2f) }
    var fogDistance by remember { mutableFloatStateOf(55f) }
    var hapticsEnabled by remember { mutableStateOf(true) }

    // Hook renderer haptics
    remember {
        renderer.onBlockMined = {
            if (hapticsEnabled) onVibrate(40L)
        }
        renderer.onBlockPlaced = {
            if (hapticsEnabled) onVibrate(25L)
        }
    }

    // Sync held item with renderer
    androidx.compose.runtime.LaunchedEffect(selectedSlotIndex, hotbarSlots) {
        renderer.currentHeldBlock = hotbarSlots.getOrElse(selectedSlotIndex) { BlockType.AIR }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ---------------- 1. 3D OPENGL SURFACE VIEW ----------------
        AndroidView(
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(2)
                    preserveEGLContextOnPause = true
                    setZOrderMediaOverlay(false)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    onSurfaceCreated(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ---------------- 2. JETPACK COMPOSE TOUCH HUD ----------------
        GameControlsOverlay(
            player = player,
            renderer = renderer,
            sensitivity = sensitivity,
            hotbarSlots = hotbarSlots,
            selectedSlotIndex = selectedSlotIndex,
            onSelectSlot = { index ->
                selectedSlotIndex = index
                renderer.currentHeldBlock = hotbarSlots.getOrElse(index) { BlockType.AIR }
            },
            onOpenInventory = { showInventoryDialog = true },
            onOpenSettings = { showSettingsDialog = true }
        )

        // ---------------- 3. CREATIVE INVENTORY PALETTE MODAL ----------------
        if (showInventoryDialog) {
            CreativeInventoryDialog(
                selectedHotbarIndex = selectedSlotIndex,
                onSelectBlock = { pickedBlock ->
                    val updated = hotbarSlots.toMutableList()
                    if (selectedSlotIndex in updated.indices) {
                        updated[selectedSlotIndex] = pickedBlock
                        hotbarSlots = updated
                        renderer.currentHeldBlock = pickedBlock
                    }
                },
                onDismiss = { showInventoryDialog = false }
            )
        }

        // ---------------- 4. SETTINGS MODAL ----------------
        if (showSettingsDialog) {
            GameSettingsDialog(
                fov = fov,
                onFovChange = { newFov ->
                    fov = newFov
                    renderer.fieldOfView = newFov
                },
                sensitivity = sensitivity,
                onSensitivityChange = { sensitivity = it },
                fogDistance = fogDistance,
                onFogDistanceChange = { newFog ->
                    fogDistance = newFog
                    renderer.renderDistanceFog = newFog
                },
                hapticsEnabled = hapticsEnabled,
                onHapticsToggle = { hapticsEnabled = it },
                onRegenerateWorld = {
                    world.generateWorld()
                    val newTopY = world.getTopSolidBlockY(0, 0).toFloat()
                    player.posX = 0f
                    player.posY = newTopY + 1.0f
                    player.posZ = 0f
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}
