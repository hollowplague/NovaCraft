package com.example.game.engine

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance OpenGL ES 2.0 Renderer for Minecraft Kotlin Edition.
 * Handles rendering passes, camera matrices, dynamic atmosphere, and raycast interactions.
 */
class GameRenderer(
    val world: VoxelWorld,
    val player: Player
) : GLSurfaceView.Renderer {

    private val shader = VoxelShader()
    private var textureId = 0

    // Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    var fieldOfView = 70.0f
    var renderDistanceFog = 55.0f

    // Time of day: 0.0 (midnight) -> 0.25 (dawn) -> 0.5 (noon) -> 0.75 (sunset) -> 1.0
    var timeOfDay = 0.45f
    var isTimeFlowing = false

    // State exposed to HUD overlay
    @Volatile var fps = 60
    @Volatile var currentRaycastHit: RaycastHit? = null

    private var lastFrameTimeNanos = 0L
    private var frameCounter = 0
    private var fpsTimer = 0L

    // Block interaction queue
    @Volatile var pendingMine = false
    @Volatile var pendingPlaceType: BlockType? = null
    @Volatile var currentHeldBlock: BlockType = BlockType.AIR

    // MCPE Touch Mining & Cooldown State
    @Volatile var isMining = false
    @Volatile var miningProgress = 0f // 0.0 to 1.0
    @Volatile var miningStage = 0 // 0 to 9
    @Volatile var currentMiningHit: RaycastHit? = null
    var onMiningProgress: ((Boolean, Float) -> Unit)? = null
    private var currentMiningTime = 0f
    private var swingCooldown = 0f

    fun startMining(hit: RaycastHit) {
        if (hit.block == BlockType.BEDROCK || hit.block == BlockType.AIR || hit.block.isFluid) return
        isMining = true
        currentMiningHit = hit
        currentMiningTime = 0f
        miningProgress = 0f
        miningStage = 0
        onMiningProgress?.invoke(true, 0f)
    }

    fun stopMining() {
        isMining = false
        currentMiningHit = null
        currentMiningTime = 0f
        miningProgress = 0f
        miningStage = 0
        onMiningProgress?.invoke(false, 0f)
    }

    fun placeBlockAt(hit: RaycastHit, block: BlockType) {
        if (block == BlockType.AIR) return
        viewmodelRenderer.triggerSwing()
        val px = hit.hitX + hit.normalX
        val py = hit.hitY + hit.normalY
        val pz = hit.hitZ + hit.normalZ

        // Check if placed block would intersect player's body AABB
        val pMinX = player.posX - player.HALF_WIDTH
        val pMaxX = player.posX + player.HALF_WIDTH
        val pMinY = player.posY
        val pMaxY = player.posY + player.PLAYER_HEIGHT
        val pMinZ = player.posZ - player.HALF_WIDTH
        val pMaxZ = player.posZ + player.HALF_WIDTH

        val bMinX = px.toFloat()
        val bMaxX = px + 1.0f
        val bMinY = py.toFloat()
        val bMaxY = py + 1.0f
        val bMinZ = pz.toFloat()
        val bMaxZ = pz + 1.0f

        val overlaps = (pMinX < bMaxX && pMaxX > bMinX) &&
                (pMinY < bMaxY && pMaxY > bMinY) &&
                (pMinZ < bMaxZ && pMaxZ > bMinZ)

        if (!overlaps && py in 0 until Chunk.HEIGHT) {
            world.setBlock(px, py, pz, block)
            onBlockPlaced?.invoke()
        }
    }

    // First-person 3D hand & held block viewmodel
    val viewmodelRenderer = ViewmodelRenderer()
    val particleSystem = ParticleSystem()
    private var currentAspect = 1.0f
    @Volatile var viewportWidth = 1
    @Volatile var viewportHeight = 1
    private val invertedMvpMatrix = FloatArray(16)
    private val matrixLock = Any()

    // Callbacks to UI for haptics and sound effects
    var onBlockMined: (() -> Unit)? = null
    var onBlockPlaced: (() -> Unit)? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        shader.initialize()
        textureId = TextureAtlas.loadTexture()

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDisable(GLES20.GL_CULL_FACE) // Chunk mesher already performs hidden-face culling

        Matrix.setIdentityM(modelMatrix, 0)
        lastFrameTimeNanos = System.nanoTime()
        fpsTimer = SystemClock.uptimeMillis()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, width, height)
        currentAspect = viewportWidth.toFloat() / viewportHeight.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, fieldOfView, currentAspect, 0.1f, 150.0f)
    }

    fun updateFov(fov: Float, width: Int, height: Int) {
        fieldOfView = fov
        if (width > 0 && height > 0) {
            viewportWidth = width
            viewportHeight = height
            currentAspect = width.toFloat() / height.toFloat()
            Matrix.perspectiveM(projectionMatrix, 0, fieldOfView, currentAspect, 0.1f, 150.0f)
        }
    }

    /**
     * Unprojects screen coordinates (x, y) into a 3D ray in world space,
     * and performs a DDA raycast into the voxel world.
     * This provides true MCPE direct block tapping and holding!
     */
    fun raycastFromScreen(screenX: Float, screenY: Float, width: Int = viewportWidth, height: Int = viewportHeight): RaycastHit? {
        val w = if (width > 0) width else viewportWidth
        val h = if (height > 0) height else viewportHeight
        if (w <= 0 || h <= 0) return null

        // Normalized Device Coordinates (-1..1)
        val ndcX = (2.0f * screenX) / w.toFloat() - 1.0f
        val ndcY = 1.0f - (2.0f * screenY) / h.toFloat()

        val invMvp = FloatArray(16)
        synchronized(matrixLock) {
            System.arraycopy(invertedMvpMatrix, 0, invMvp, 0, 16)
        }

        val nearPoint = floatArrayOf(ndcX, ndcY, -1.0f, 1.0f)
        val farPoint = floatArrayOf(ndcX, ndcY, 1.0f, 1.0f)

        val unprojectedNear = FloatArray(4)
        val unprojectedFar = FloatArray(4)

        Matrix.multiplyMV(unprojectedNear, 0, invMvp, 0, nearPoint, 0)
        Matrix.multiplyMV(unprojectedFar, 0, invMvp, 0, farPoint, 0)

        if (unprojectedNear[3] == 0f || unprojectedFar[3] == 0f) return null

        val nearX = unprojectedNear[0] / unprojectedNear[3]
        val nearY = unprojectedNear[1] / unprojectedNear[3]
        val nearZ = unprojectedNear[2] / unprojectedNear[3]

        val farX = unprojectedFar[0] / unprojectedFar[3]
        val farY = unprojectedFar[1] / unprojectedFar[3]
        val farZ = unprojectedFar[2] / unprojectedFar[3]

        var dirX = farX - nearX
        var dirY = farY - nearY
        var dirZ = farZ - nearZ

        val len = sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        if (len < 0.0001f) return null

        dirX /= len
        dirY /= len
        dirZ /= len

        return world.raycast(
            player.eyeX, player.eyeY, player.eyeZ,
            dirX, dirY, dirZ,
            maxDistance = 6.5f
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val deltaSec = (now - lastFrameTimeNanos) / 1_000_000_000.0f
        lastFrameTimeNanos = now

        // FPS calculation
        frameCounter++
        val nowMs = SystemClock.uptimeMillis()
        if (nowMs - fpsTimer >= 1000) {
            fps = frameCounter
            frameCounter = 0
            fpsTimer = nowMs
        }

        // Continuous 10-minute automatic day/night cycle (600s)
        timeOfDay = (timeOfDay + deltaSec / 600.0f) % 1.0f

        // Update player movement & particle physics
        player.update(world, deltaSec)
        particleSystem.update(deltaSec, world)

        // Process raycasting from player eye along look vector
        val forward = player.getForwardVector()
        val hit = world.raycast(
            player.eyeX, player.eyeY, player.eyeZ,
            forward[0], forward[1], forward[2],
            maxDistance = 6.5f
        )
        currentRaycastHit = hit

        // Process progressive MCPE block destruction on the targeted block
        if (isMining) {
            val targetHit = currentMiningHit
            if (targetHit != null) {
                val currentBlock = world.getBlock(targetHit.hitX, targetHit.hitY, targetHit.hitZ)
                // Check if block still exists and player is within reach
                val dx = (targetHit.hitX + 0.5f) - player.eyeX
                val dy = (targetHit.hitY + 0.5f) - player.eyeY
                val dz = (targetHit.hitZ + 0.5f) - player.eyeZ
                val distSq = dx * dx + dy * dy + dz * dz

                if (currentBlock != BlockType.AIR && distSq <= 7.0f * 7.0f) {
                    swingCooldown += deltaSec
                    if (swingCooldown >= 0.22f) {
                        swingCooldown = 0f
                        viewmodelRenderer.triggerSwing()
                    }

                    currentMiningTime += deltaSec
                    val maxDuration = targetHit.block.breakTimeSec
                    miningProgress = (currentMiningTime / maxDuration).coerceIn(0f, 1f)
                    miningStage = (miningProgress * 9.99f).toInt().coerceIn(0, 9)
                    onMiningProgress?.invoke(true, miningProgress)

                    if (miningProgress >= 1.0f) {
                        val brokenBlock = targetHit.block
                        world.setBlock(targetHit.hitX, targetHit.hitY, targetHit.hitZ, BlockType.AIR)
                        particleSystem.spawnBlockBreak(targetHit.hitX.toFloat(), targetHit.hitY.toFloat(), targetHit.hitZ.toFloat(), brokenBlock)
                        onBlockMined?.invoke()
                        stopMining()
                    }
                } else {
                    stopMining()
                }
            } else {
                stopMining()
            }
        }

        // Calculate sky & atmosphere colors based on timeOfDay
        val (skyR, skyG, skyB, sunR, sunG, sunB, ambR, ambG, ambB) = calculateAtmosphere()

        // Clear screen with atmospheric sky color
        GLES20.glClearColor(skyR, skyG, skyB, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Compute View Matrix from player orientation
        Matrix.setLookAtM(
            viewMatrix, 0,
            player.eyeX, player.eyeY, player.eyeZ,
            player.eyeX + forward[0], player.eyeY + forward[1], player.eyeZ + forward[2],
            0f, 1f, 0f
        )

        // Compute MVP matrix and invert for screen-space touch raycasting
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        synchronized(matrixLock) {
            Matrix.invertM(invertedMvpMatrix, 0, mvpMatrix, 0)
        }

        // Sunlight direction rotates with time of day
        val sunAngle = (timeOfDay * 2.0 * Math.PI).toFloat()
        val sunDirX = cos(sunAngle)
        val sunDirY = sin(sunAngle)
        val sunDirZ = 0.35f

        // Rebuild dirty chunk meshes progressively (1 chunk on initial frame for instant SurfaceView ready, then up to 2 per frame)
        val maxRebuildThisFrame = if (frameCounter <= 1) 1 else 2
        var rebuilt = 0
        val dirtyChunks = world.getAllChunks().filter { it.isDirty }
        if (dirtyChunks.isNotEmpty()) {
            val playerChunkX = (player.posX / Chunk.WIDTH).toInt()
            val playerChunkZ = (player.posZ / Chunk.DEPTH).toInt()
            val sorted = dirtyChunks.sortedBy { chunk ->
                val dx = chunk.chunkX - playerChunkX
                val dz = chunk.chunkZ - playerChunkZ
                dx * dx + dz * dz
            }
            for (chunk in sorted) {
                chunk.buildMesh(world)
                rebuilt++
                if (rebuilt >= maxRebuildThisFrame) break
            }
        }

        // ---------------- PASS 1: OPAQUE VOXELS ----------------
        GLES20.glUseProgram(shader.programId)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniform3f(shader.uCameraPosLoc, player.eyeX, player.eyeY, player.eyeZ)
        GLES20.glUniform3f(shader.uSunDirLoc, sunDirX, sunDirY, sunDirZ)
        GLES20.glUniform3f(shader.uSunColorLoc, sunR, sunG, sunB)
        GLES20.glUniform3f(shader.uAmbientColorLoc, ambR, ambG, ambB)
        GLES20.glUniform3f(shader.uFogColorLoc, skyR, skyG, skyB)
        GLES20.glUniform1f(shader.uFogStartLoc, renderDistanceFog * 0.4f)
        GLES20.glUniform1f(shader.uFogEndLoc, renderDistanceFog)
        GLES20.glUniform1f(shader.uAlphaMultiplierLoc, 1.0f)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(shader.uTextureLoc, 0)

        for (chunk in world.getAllChunks()) {
            val buf = chunk.opaqueBuffer ?: continue
            val count = chunk.opaqueVertexCount
            if (count <= 0) continue

            drawChunkBuffer(buf, count)
        }

        // ---------------- PASS 2: BREAKING CRACKS ON TOUCH-MINED BLOCK ----------------
        // Note: Crosshair selection box is removed for authentic MCPE direct touch controls.
        if (isMining) {
            currentMiningHit?.let { target ->
                Matrix.translateM(tempMatrix, 0, mvpMatrix, 0, target.hitX.toFloat(), target.hitY.toFloat(), target.hitZ.toFloat())
                shader.drawBreakingCracks(tempMatrix, textureId, miningStage)
            }
        }

        // ---------------- PASS 2.5: 3D BLOCK DEBRIS PARTICLES BURST ----------------
        GLES20.glUseProgram(shader.programId)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(shader.uAlphaMultiplierLoc, 1.0f)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(shader.uTextureLoc, 0)
        particleSystem.render(shader)

        // ---------------- PASS 3: TRANSLUCENT VOXELS (WATER, GLASS) ----------------
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glUseProgram(shader.programId)
        GLES20.glUniform1f(shader.uAlphaMultiplierLoc, 0.88f)

        for (chunk in world.getAllChunks()) {
            val buf = chunk.translucentBuffer ?: continue
            val count = chunk.translucentVertexCount
            if (count <= 0) continue

            drawChunkBuffer(buf, count)
        }

        GLES20.glDisable(GLES20.GL_BLEND)

        // ---------------- PASS 4: FIRST-PERSON 3D HAND & HELD BLOCK ----------------
        val isMoving = (player.velX * player.velX + player.velZ * player.velZ) > 0.05f
        viewmodelRenderer.update(deltaSec, isMoving)
        viewmodelRenderer.render(
            shader,
            textureId,
            currentHeldBlock,
            currentAspect,
            floatArrayOf(sunDirX, sunDirY, sunDirZ),
            floatArrayOf(sunR, sunG, sunB),
            floatArrayOf(ambR, ambG, ambB)
        )
    }

    private fun drawChunkBuffer(buffer: java.nio.FloatBuffer, count: Int) {
        val stride = Chunk.FLOATS_PER_VERTEX * 4 // 9 floats * 4 bytes = 36 bytes

        buffer.position(0)
        GLES20.glEnableVertexAttribArray(shader.aPositionLoc)
        GLES20.glVertexAttribPointer(shader.aPositionLoc, 3, GLES20.GL_FLOAT, false, stride, buffer)

        buffer.position(3)
        GLES20.glEnableVertexAttribArray(shader.aNormalLoc)
        GLES20.glVertexAttribPointer(shader.aNormalLoc, 3, GLES20.GL_FLOAT, false, stride, buffer)

        buffer.position(6)
        GLES20.glEnableVertexAttribArray(shader.aTexCoordLoc)
        GLES20.glVertexAttribPointer(shader.aTexCoordLoc, 2, GLES20.GL_FLOAT, false, stride, buffer)

        buffer.position(8)
        GLES20.glEnableVertexAttribArray(shader.aAOLoc)
        GLES20.glVertexAttribPointer(shader.aAOLoc, 1, GLES20.GL_FLOAT, false, stride, buffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)

        GLES20.glDisableVertexAttribArray(shader.aPositionLoc)
        GLES20.glDisableVertexAttribArray(shader.aNormalLoc)
        GLES20.glDisableVertexAttribArray(shader.aTexCoordLoc)
        GLES20.glDisableVertexAttribArray(shader.aAOLoc)
    }

    private data class AtmosphereValues(
        val skyR: Float, val skyG: Float, val skyB: Float,
        val sunR: Float, val sunG: Float, val sunB: Float,
        val ambR: Float, val ambG: Float, val ambB: Float
    )

    private fun calculateAtmosphere(): AtmosphereValues {
        // Vibrant daylight cycle
        // Noon: timeOfDay ~ 0.5 -> Sky: Azure Cyan, Sun: Warm White
        // Sunset: timeOfDay ~ 0.75 -> Sky: Amber Orange, Sun: Fiery Red
        // Night: timeOfDay ~ 0.0 -> Sky: Deep Navy, Sun: Moonlight Blue
        return when {
            timeOfDay in 0.20f..0.30f -> { // Dawn
                AtmosphereValues(
                    0.75f, 0.55f, 0.65f,
                    0.95f, 0.70f, 0.40f,
                    0.40f, 0.35f, 0.45f
                )
            }
            timeOfDay in 0.30f..0.70f -> { // Vibrant Day
                AtmosphereValues(
                    0.45f, 0.68f, 0.95f, // Azure sky
                    0.95f, 0.92f, 0.85f, // Bright warm sun
                    0.45f, 0.48f, 0.52f  // Bright daylight ambient
                )
            }
            timeOfDay in 0.70f..0.80f -> { // Golden Hour / Sunset
                AtmosphereValues(
                    0.85f, 0.45f, 0.30f, // Crimson amber sky
                    0.98f, 0.50f, 0.25f, // Golden sun
                    0.35f, 0.30f, 0.35f
                )
            }
            else -> { // Night
                AtmosphereValues(
                    0.05f, 0.08f, 0.16f, // Deep indigo night sky
                    0.20f, 0.25f, 0.45f, // Cool moonlight
                    0.15f, 0.18f, 0.28f  // Soft ambient
                )
            }
        }
    }
}
