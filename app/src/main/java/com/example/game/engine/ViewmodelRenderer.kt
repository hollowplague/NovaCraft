package com.example.game.engine

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Renders the authentic first-person 3D player arm and held voxel block
 * in the lower-right viewport with walking bobbing and punch/place swing animations.
 */
class ViewmodelRenderer {

    private val viewmodelProj = FloatArray(16)
    private val viewmodelMVP = FloatArray(16)
    private val viewmodelModel = FloatArray(16)
    private val tempMat = FloatArray(16)

    // Animation state
    private var walkBobTimer = 0f
    private var bobIntensity = 0f
    private var swingProgress = 1.0f // 1.0 = idle, 0.0..1.0 = swinging
    private val SWING_DURATION = 0.22f // seconds

    // Vertex buffers
    private var blockBuffer: FloatBuffer? = null
    private var armBuffer: FloatBuffer? = null
    private var emptyHandArmBuffer: FloatBuffer? = null
    private var lastHeldBlock: BlockType? = null

    fun triggerSwing() {
        swingProgress = 0.0f
    }

    fun update(deltaSec: Float, isMoving: Boolean) {
        // Smoothly interpolate bobbing intensity: 0 when idle, 1 when walking
        val targetIntensity = if (isMoving) 1.0f else 0.0f
        bobIntensity += (targetIntensity - bobIntensity) * (deltaSec * 10f).coerceIn(0f, 1f)

        if (bobIntensity > 0.01f) {
            walkBobTimer += deltaSec * 8.0f
        }

        // Update swing animation
        if (swingProgress < 1.0f) {
            swingProgress = (swingProgress + deltaSec / SWING_DURATION).coerceAtMost(1.0f)
        }
    }

    /**
     * Renders the 3D player arm and held block in a dedicated viewmodel pass.
     */
    fun render(
        shader: VoxelShader,
        textureId: Int,
        heldBlock: BlockType,
        aspect: Float,
        sunDir: FloatArray,
        sunColor: FloatArray,
        ambColor: FloatArray
    ) {
        // 1. Clear depth buffer so held item never clips into nearby terrain/walls
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)

        // 2. Setup dedicated first-person camera projection (fixed 52 deg FOV)
        Matrix.perspectiveM(viewmodelProj, 0, 52.0f, aspect, 0.05f, 20.0f)

        // 3. Compute Viewmodel Model-View Matrix
        Matrix.setIdentityM(viewmodelModel, 0)

        // Natural walking bobbing: sinusoidal figure-8 horizontal and vertical dip
        val bobX = sin(walkBobTimer) * 0.016f * bobIntensity
        val bobY = -abs(kotlin.math.cos(walkBobTimer)) * 0.014f * bobIntensity
        val bobRoll = sin(walkBobTimer) * 2.2f * bobIntensity

        // Swing animation calculations
        val isSwinging = swingProgress < 1.0f
        val swingSin = if (isSwinging) sin(swingProgress * PI.toFloat()) else 0.0f
        val swingRotX = swingSin * 40.0f
        val swingRotY = -swingSin * 45.0f
        val swingTransY = -swingSin * 0.14f
        val swingTransZ = swingSin * 0.10f

        val isAir = (heldBlock == BlockType.AIR)

        // Minecraft authentic first-person placement in lower-right viewport
        // Empty fist is positioned so the player's forearm and hand clearly enter diagonally from the bottom-right corner.
        val posX = (if (isAir) 0.22f else 0.26f) + bobX
        val posY = (if (isAir) -0.15f else -0.17f) + bobY + swingTransY
        val posZ = (if (isAir) -0.36f else -0.40f) + swingTransZ

        Matrix.translateM(viewmodelModel, 0, posX, posY, posZ)

        // Authentic Minecraft handheld item rotation:
        if (isAir) {
            // Steve bare punch arm: angled up-left towards crosshair
            Matrix.rotateM(viewmodelModel, 0, 18f + swingRotX, 1f, 0f, 0f)
            Matrix.rotateM(viewmodelModel, 0, -32f + swingRotY, 0f, 1f, 0f)
            Matrix.rotateM(viewmodelModel, 0, 8f + bobRoll, 0f, 0f, 1f)
        } else {
            // Block held in right hand: ~42 deg yaw so top, front and side are all visible, 22 deg pitch upright
            Matrix.rotateM(viewmodelModel, 0, 22f + swingRotX, 1f, 0f, 0f)
            Matrix.rotateM(viewmodelModel, 0, -42f + swingRotY, 0f, 1f, 0f)
            Matrix.rotateM(viewmodelModel, 0, 4f + bobRoll, 0f, 0f, 1f)
        }

        // Compute MVP
        Matrix.multiplyMM(viewmodelMVP, 0, viewmodelProj, 0, viewmodelModel, 0)

        // 4. Build/Update held block buffer if holding a real block
        if (!isAir && (heldBlock != lastHeldBlock || blockBuffer == null)) {
            lastHeldBlock = heldBlock
            blockBuffer = buildHeldBlockMesh(heldBlock)
        }

        // 5. Build Steve arm buffers
        if (armBuffer == null) {
            armBuffer = buildSteveArmMesh(isEmptyHand = false)
        }
        if (emptyHandArmBuffer == null) {
            emptyHandArmBuffer = buildSteveArmMesh(isEmptyHand = true)
        }

        // 6. Draw with VoxelShader using exact environmental lighting from sky
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glUseProgram(shader.programId)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, viewmodelMVP, 0)
        GLES20.glUniform3f(shader.uCameraPosLoc, 0f, 0f, 0f)
        // Pass real environmental lighting
        GLES20.glUniform3f(shader.uSunDirLoc, sunDir[0], sunDir[1], sunDir[2])
        GLES20.glUniform3f(shader.uSunColorLoc, sunColor[0], sunColor[1], sunColor[2])
        GLES20.glUniform3f(shader.uAmbientColorLoc, ambColor[0], ambColor[1], ambColor[2])
        GLES20.glUniform3f(shader.uFogColorLoc, 0f, 0f, 0f)
        GLES20.glUniform1f(shader.uFogStartLoc, 100f) // No fog on viewmodel
        GLES20.glUniform1f(shader.uFogEndLoc, 200f)
        GLES20.glUniform1f(shader.uAlphaMultiplierLoc, 1.0f)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(shader.uTextureLoc, 0)

        // Draw held block (if not AIR)
        if (!isAir) {
            blockBuffer?.let { buf ->
                drawBuffer(shader, buf, 36) // 6 faces * 2 tris * 3 verts = 36 verts
            }
            // Draw arm supporting the block
            armBuffer?.let { buf ->
                drawBuffer(shader, buf, 72) // 2 segments (sleeve + hand) = 72 verts
            }
        } else {
            // Draw empty fist punch arm
            emptyHandArmBuffer?.let { buf ->
                drawBuffer(shader, buf, 72)
            }
        }
    }

    private fun drawBuffer(shader: VoxelShader, buffer: FloatBuffer, count: Int) {
        val stride = Chunk.FLOATS_PER_VERTEX * 4

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

    /**
     * Builds a 3D miniature cube mesh representing the held item.
     */
    private fun buildHeldBlockMesh(block: BlockType): FloatBuffer {
        val list = ArrayList<Float>(36 * Chunk.FLOATS_PER_VERTEX)
        val s = 0.12f // Half-extent = 0.24 cube size
        val cx = 0.0f
        val cy = 0.05f
        val cz = 0.0f

        val topUV = TextureAtlas.getTileUV(block.topTex)
        val bottomUV = TextureAtlas.getTileUV(block.bottomTex)
        val sideUV = TextureAtlas.getTileUV(block.sideTex)

        // Top (+Y)
        addQuad(list,
            cx - s, cy + s, cz - s,
            cx + s, cy + s, cz - s,
            cx + s, cy + s, cz + s,
            cx - s, cy + s, cz + s,
            0f, 1f, 0f, topUV, 1.0f
        )
        // Bottom (-Y)
        addQuad(list,
            cx - s, cy - s, cz + s,
            cx + s, cy - s, cz + s,
            cx + s, cy - s, cz - s,
            cx - s, cy - s, cz - s,
            0f, -1f, 0f, bottomUV, 0.5f
        )
        // North (+Z)
        addQuad(list,
            cx + s, cy + s, cz + s,
            cx - s, cy + s, cz + s,
            cx - s, cy - s, cz + s,
            cx + s, cy - s, cz + s,
            0f, 0f, 1f, sideUV, 0.8f
        )
        // South (-Z)
        addQuad(list,
            cx - s, cy + s, cz - s,
            cx + s, cy + s, cz - s,
            cx + s, cy - s, cz - s,
            cx - s, cy - s, cz - s,
            0f, 0f, -1f, sideUV, 0.8f
        )
        // East (+X)
        addQuad(list,
            cx + s, cy + s, cz + s,
            cx + s, cy + s, cz - s,
            cx + s, cy - s, cz - s,
            cx + s, cy - s, cz + s,
            1f, 0f, 0f, sideUV, 0.65f
        )
        // West (-X)
        addQuad(list,
            cx - s, cy + s, cz - s,
            cx - s, cy + s, cz + s,
            cx - s, cy - s, cz + s,
            cx - s, cy - s, cz - s,
            -1f, 0f, 0f, sideUV, 0.65f
        )

        val buf = ByteBuffer.allocateDirect(list.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        for (f in list) buf.put(f)
        buf.position(0)
        return buf
    }

    /**
     * Builds Steve's classic player forearm with cyan shirt sleeve and peach skin hand.
     */
    private fun buildSteveArmMesh(isEmptyHand: Boolean): FloatBuffer {
        val list = ArrayList<Float>(72 * Chunk.FLOATS_PER_VERTEX)
        val w = if (isEmptyHand) 0.10f else 0.085f
        val h = if (isEmptyHand) 0.10f else 0.085f
        val len = if (isEmptyHand) 0.45f else 0.42f

        val x0 = if (isEmptyHand) -0.05f else 0.03f
        val x1 = x0 + w
        val y0 = if (isEmptyHand) -0.05f else -0.10f
        val y1 = y0 + h
        val z0 = if (isEmptyHand) 0.18f else 0.22f
        val z1 = z0 - len
        val zSplit = z0 - len * 0.48f // Boundary between sleeve and bare forearm/hand

        val sleeveUV = TextureAtlas.getTileUV(TextureAtlas.TILE_STEVE_SLEEVE)
        val skinUV = TextureAtlas.getTileUV(TextureAtlas.TILE_STEVE_SKIN)

        // 1. REAR CUBOID: STEVE'S CYAN SHIRT SLEEVE
        // Top
        addQuad(list, x0, y1, zSplit, x1, y1, zSplit, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f, sleeveUV, 0.95f)
        // Bottom
        addQuad(list, x0, y0, z0, x1, y0, z0, x1, y0, zSplit, x0, y0, zSplit, 0f, -1f, 0f, sleeveUV, 0.5f)
        // Front (+Z towards camera)
        addQuad(list, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0f, 0f, 1f, sleeveUV, 0.8f)
        // Back (-Z towards hand)
        addQuad(list, x0, y1, zSplit, x1, y1, zSplit, x1, y0, zSplit, x0, y0, zSplit, 0f, 0f, -1f, sleeveUV, 0.8f)
        // Right (+X)
        addQuad(list, x1, y1, z0, x1, y1, zSplit, x1, y0, zSplit, x1, y0, z0, 1f, 0f, 0f, sleeveUV, 0.7f)
        // Left (-X)
        addQuad(list, x0, y1, zSplit, x0, y1, z0, x0, y0, z0, x0, y0, zSplit, -1f, 0f, 0f, sleeveUV, 0.7f)

        // 2. FRONT CUBOID: STEVE'S PEACH SKIN HAND / WRIST
        // Top
        addQuad(list, x0, y1, z1, x1, y1, z1, x1, y1, zSplit, x0, y1, zSplit, 0f, 1f, 0f, skinUV, 0.95f)
        // Bottom
        addQuad(list, x0, y0, zSplit, x1, y0, zSplit, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f, skinUV, 0.5f)
        // Front (+Z seam)
        addQuad(list, x1, y1, zSplit, x0, y1, zSplit, x0, y0, zSplit, x1, y0, zSplit, 0f, 0f, 1f, skinUV, 0.8f)
        // Back (-Z knuckles/fingers)
        addQuad(list, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0f, 0f, -1f, skinUV, 0.85f)
        // Right (+X)
        addQuad(list, x1, y1, zSplit, x1, y1, z1, x1, y0, z1, x1, y0, zSplit, 1f, 0f, 0f, skinUV, 0.7f)
        // Left (-X)
        addQuad(list, x0, y1, z1, x0, y1, zSplit, x0, y0, zSplit, x0, y0, z1, -1f, 0f, 0f, skinUV, 0.7f)

        val buf = ByteBuffer.allocateDirect(list.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        for (f in list) buf.put(f)
        buf.position(0)
        return buf
    }

    private fun addQuad(
        list: ArrayList<Float>,
        tlX: Float, tlY: Float, tlZ: Float,
        trX: Float, trY: Float, trZ: Float,
        brX: Float, brY: Float, brZ: Float,
        blX: Float, blY: Float, blZ: Float,
        nx: Float, ny: Float, nz: Float,
        uv: FloatArray,
        ao: Float
    ) {
        val uMin = uv[0]
        val vMin = uv[1]
        val uMax = uv[2]
        val vMax = uv[3]

        addVertex(list, tlX, tlY, tlZ, nx, ny, nz, uMin, vMin, ao)
        addVertex(list, blX, blY, blZ, nx, ny, nz, uMin, vMax, ao)
        addVertex(list, brX, brY, brZ, nx, ny, nz, uMax, vMax, ao)

        addVertex(list, tlX, tlY, tlZ, nx, ny, nz, uMin, vMin, ao)
        addVertex(list, brX, brY, brZ, nx, ny, nz, uMax, vMax, ao)
        addVertex(list, trX, trY, trZ, nx, ny, nz, uMax, vMin, ao)
    }

    private inline fun addVertex(
        list: ArrayList<Float>,
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float,
        ao: Float
    ) {
        list.add(x)
        list.add(y)
        list.add(z)
        list.add(nx)
        list.add(ny)
        list.add(nz)
        list.add(u)
        list.add(v)
        list.add(ao)
    }
}
