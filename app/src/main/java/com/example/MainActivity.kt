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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.game.engine.BlockType
import com.example.game.engine.GameRenderer
import com.example.game.engine.Player
import com.example.game.engine.VoxelWorld
import com.example.game.engine.WorldSaveData
import com.example.game.engine.WorldSaveManager
import com.example.game.ui.CreateWorldScreen
import com.example.game.ui.CreativeInventoryDialog
import com.example.game.ui.GameControlsOverlay
import com.example.game.ui.GameSettingsDialog
import com.example.game.ui.MainMenuScreen
import com.example.game.ui.WorldsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class AppScreen {
    MAIN_MENU,
    WORLDS,
    CREATE_WORLD,
    IN_GAME
}

/**
 * Main Activity for NovaCraft.
 * Features:
 * - Persistent Worlds System with Auto-Save
 * - Bedrock Create World Screen (Flat vs Infinite, seed input)
 * - Category-tabbed Creative Inventory Dialog
 * - Continuous OpenGL ES 2.0 Voxel Renderer Loop
 */
class MainActivity : ComponentActivity() {

    private lateinit var world: VoxelWorld
    private lateinit var player: Player
    private lateinit var renderer: GameRenderer
    private var glSurfaceView: GLSurfaceView? = null
    private var onActivityPauseSave: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        // 1. Initialize Engine with default world
        world = VoxelWorld()

        // 2. Initialize Player
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
                NovaCraftApp(
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
                    },
                    onRegisterSaveHook = { saveCallback ->
                        onActivityPauseSave = saveCallback
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
        onActivityPauseSave?.invoke()
        glSurfaceView?.onPause()
    }
}

@Composable
fun NovaCraftApp(
    world: VoxelWorld,
    player: Player,
    renderer: GameRenderer,
    onVibrate: (Long) -> Unit,
    onSurfaceCreated: (GLSurfaceView) -> Unit,
    onRegisterSaveHook: (() -> Unit) -> Unit
) {
    val context = LocalContext.current

    // Screen navigation
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN_MENU) }

    // Active world state
    var activeWorldData by remember { mutableStateOf<WorldSaveData?>(null) }

    // Hotbar state (9 slots)
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

    // Helper to persist current world data
    fun saveCurrentWorld() {
        val current = activeWorldData ?: return
        val updated = current.copy(
            playerX = player.posX,
            playerY = player.posY,
            playerZ = player.posZ,
            playerYaw = player.yaw,
            playerPitch = player.pitch,
            hotbar = hotbarSlots,
            blockDeltas = HashMap(world.blockDeltas)
        )
        WorldSaveManager.saveWorldData(context, updated)
        activeWorldData = updated
    }

    // Register activity lifecycle save hook
    remember {
        onRegisterSaveHook {
            if (currentScreen == AppScreen.IN_GAME) {
                saveCurrentWorld()
            }
        }
    }

    // Background auto-save every 25 seconds while playing
    LaunchedEffect(currentScreen, activeWorldData) {
        if (currentScreen == AppScreen.IN_GAME) {
            while (isActive) {
                delay(25_000L)
                saveCurrentWorld()
            }
        }
    }

    // Handle back button
    androidx.activity.compose.BackHandler(enabled = (currentScreen == AppScreen.IN_GAME)) {
        if (showInventoryDialog) {
            showInventoryDialog = false
        } else if (showSettingsDialog) {
            showSettingsDialog = false
        } else {
            showSettingsDialog = true
        }
    }

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
    LaunchedEffect(selectedSlotIndex, hotbarSlots) {
        renderer.currentHeldBlock = hotbarSlots.getOrElse(selectedSlotIndex) { BlockType.AIR }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ---------------- 1. 3D OPENGL SURFACE VIEW (Always active underneath) ----------------
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
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

        // ---------------- 2. TOP-LEVEL SCREEN NAVIGATION ----------------
        when (currentScreen) {
            AppScreen.MAIN_MENU -> {
                MainMenuScreen(
                    onPlayGame = {
                        currentScreen = AppScreen.WORLDS
                    },
                    onOpenSettings = {
                        showSettingsDialog = true
                    }
                )
            }

            AppScreen.WORLDS -> {
                WorldsScreen(
                    onPlayWorld = { meta ->
                        val data = WorldSaveManager.loadWorldData(context, meta.id)
                            ?: WorldSaveManager.createNewWorld(context, meta.name, meta.type, meta.seed)
                        activeWorldData = data

                        // Reset world and apply saved blocks
                        world.resetWith(data.metadata.seed, data.metadata.type, data.blockDeltas)

                        // Restore player state
                        player.posX = data.playerX
                        player.posY = data.playerY
                        player.posZ = data.playerZ
                        player.yaw = data.playerYaw
                        player.pitch = data.playerPitch
                        player.velX = 0f
                        player.velY = 0f
                        player.velZ = 0f

                        // Restore hotbar
                        hotbarSlots = data.hotbar
                        selectedSlotIndex = 0
                        renderer.currentHeldBlock = hotbarSlots.getOrElse(0) { BlockType.AIR }

                        currentScreen = AppScreen.IN_GAME
                    },
                    onCreateNewWorld = {
                        currentScreen = AppScreen.CREATE_WORLD
                    },
                    onBackToMainMenu = {
                        currentScreen = AppScreen.MAIN_MENU
                    }
                )
            }

            AppScreen.CREATE_WORLD -> {
                CreateWorldScreen(
                    onWorldCreated = { newWorldData ->
                        activeWorldData = newWorldData

                        // Generate terrain
                        world.resetWith(newWorldData.metadata.seed, newWorldData.metadata.type, newWorldData.blockDeltas)

                        // Spawn player on surface
                        val topY = world.getTopSolidBlockY(0, 0).toFloat()
                        player.posX = 0f
                        player.posY = topY + 1.2f
                        player.posZ = 0f
                        player.yaw = 0f
                        player.pitch = 0f
                        player.velX = 0f
                        player.velY = 0f
                        player.velZ = 0f

                        hotbarSlots = newWorldData.hotbar
                        selectedSlotIndex = 0
                        renderer.currentHeldBlock = BlockType.AIR

                        currentScreen = AppScreen.IN_GAME
                    },
                    onCancel = {
                        currentScreen = AppScreen.WORLDS
                    }
                )
            }

            AppScreen.IN_GAME -> {
                // In-game touch HUD controls
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

                // Overhauled Bedrock Creative Inventory Dialog
                if (showInventoryDialog) {
                    CreativeInventoryDialog(
                        hotbarSlots = hotbarSlots,
                        selectedHotbarIndex = selectedSlotIndex,
                        onSelectHotbarIndex = { slotIdx ->
                            selectedSlotIndex = slotIdx
                            renderer.currentHeldBlock = hotbarSlots.getOrElse(slotIdx) { BlockType.AIR }
                        },
                        onAssignBlockToSlot = { slotIdx, block ->
                            val updated = hotbarSlots.toMutableList()
                            if (slotIdx in updated.indices) {
                                updated[slotIdx] = block
                                hotbarSlots = updated
                                if (slotIdx == selectedSlotIndex) {
                                    renderer.currentHeldBlock = block
                                }
                            }
                        },
                        onDismiss = { showInventoryDialog = false }
                    )
                }
            }
        }

        // ---------------- 3. SETTINGS MODAL ----------------
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
                onQuitToTitle = if (currentScreen == AppScreen.IN_GAME) {
                    {
                        saveCurrentWorld()
                        showSettingsDialog = false
                        currentScreen = AppScreen.MAIN_MENU
                    }
                } else null,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}
