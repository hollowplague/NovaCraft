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
 * First-person held voxel block renderer for NovaCraft 1.1.0:
 * - Empty POV hand deleted (clean, unobstructed view when empty-handed)
 * - Compact 3D held block neatly tucked in the lower-right corner so it never covers the screen
 * - Smooth walking bobbing and crisp placement/punch swing animation
 */
class ViewmodelRenderer {

    private val viewmodelProj = FloatArray(16)
    private val viewmodelMVP = FloatArray(16)
    private val viewmodelModel = FloatArray(16)

    // Animation state
    private var walkBobTimer = 0f
    private var bobIntensity = 0f
    private var swingProgress = 1.0f // 1.0 = idle, 0.0..1.0 = swinging
    private val SWING_DURATION = 0.22f // seconds

    // Vertex buffer for compact held block
    private var blockBuffer: FloatBuffer? = null
    private var lastHeldBlock: BlockType? = null

    fun triggerSwing() {
        swingProgress = 0.0f
    }

    fun update(deltaSec: Float, isMoving: Boolean) {
        // Smoothly interpolate bobbing intensity: 0 when idle, 1 when walking
        val targetIntensity = if (isMoving) 1.0f else 0.0f
        bobIntensity += (targetIntensity - bobIntensity) * (deltaSec * 9.0f).coerceIn(0f, 1f)

        if (bobIntensity > 0.005f) {
            walkBobTimer += deltaSec * 8.5f
        }

        // Update swing animation
        if (swingProgress < 1.0f) {
            swingProgress = (swingProgress + deltaSec / SWING_DURATION).coerceAtMost(1.0f)
        }
    }

    /**
     * Renders the 3D held block in a dedicated first-person pass.
     * If empty-handed (AIR), does not draw anything (POV hand deleted).
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
        // POV hand deleted: if empty-handed, keep screen completely clean and unobstructed
        if (heldBlock == BlockType.AIR) {
            return
        }

        // 1. Clear depth buffer so held block never clips into nearby terrain/walls
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)

        // 2. Setup dedicated first-person camera projection (fixed 52 deg FOV)
        Matrix.perspectiveM(viewmodelProj, 0, 52.0f, aspect, 0.05f, 20.0f)

        // 3. Compute Viewmodel Model-View Matrix
        Matrix.setIdentityM(viewmodelModel, 0)

        // Realistic walking bobbing: subtle sinusoidal figure-8 horizontal and vertical dip
        val bobX = sin(walkBobTimer) * 0.012f * bobIntensity
        val bobY = -abs(kotlin.math.cos(walkBobTimer)) * 0.010f * bobIntensity
        val bobRoll = sin(walkBobTimer) * 1.8f * bobIntensity

        // Swing animation calculations
        val isSwinging = swingProgress < 1.0f
        val swingSin = if (isSwinging) sin(swingProgress * PI.toFloat()) else 0.0f
        val swingRotX = swingSin * 30.0f
        val swingRotY = -swingSin * 35.0f
        val swingRotZ = swingSin * 12.0f
        val swingTransX = -swingSin * 0.04f
        val swingTransY = -swingSin * 0.10f
        val swingTransZ = swingSin * 0.06f

        // Compact held 3D block: positioned neatly in the lower-right corner
        val posX = 0.30f + bobX + swingTransX
        val posY = -0.22f + bobY + swingTransY
        val posZ = -0.52f + swingTransZ

        Matrix.translateM(viewmodelModel, 0, posX, posY, posZ)
        // Authentic Minecraft item rotation: 42 deg yaw (showing front & side faces), 22 deg pitch upright (showing top)
        Matrix.rotateM(viewmodelModel, 0, 22f + swingRotX, 1f, 0f, 0f)
        Matrix.rotateM(viewmodelModel, 0, -42f + swingRotY, 0f, 1f, 0f)
        Matrix.rotateM(viewmodelModel, 0, -4f + bobRoll + swingRotZ, 0f, 0f, 1f)

        // Compute MVP
        Matrix.multiplyMM(viewmodelMVP, 0, viewmodelProj, 0, viewmodelModel, 0)

        // 4. Build/Update held block buffer
        if (heldBlock != lastHeldBlock || blockBuffer == null) {
            lastHeldBlock = heldBlock
            blockBuffer = buildHeldBlockMesh(heldBlock)
        }

        // 5. Draw with VoxelShader using exact environmental lighting from sky
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glUseProgram(shader.programId)
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixLoc, 1, false, viewmodelMVP, 0)
        GLES20.glUniform3f(shader.uCameraPosLoc, 0f, 0f, 0f)
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

        blockBuffer?.let { buf ->
            drawBuffer(shader, buf, 36) // 6 faces * 2 tris * 3 verts = 36 verts
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
     * Builds a neat, compact 3D voxel cube representing the held item.
     * Full width = ~0.16m (half-extent 0.08m) so it sits comfortably in the corner
     * and never obstructs the gameplay screen.
     */
    private fun buildHeldBlockMesh(block: BlockType): FloatBuffer {
        val list = ArrayList<Float>(36 * Chunk.FLOATS_PER_VERTEX)
        val s = 0.08f // Compact 0.16m cube size
        val cx = 0.0f
        val cy = 0.0f
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
            0f, -1f, 0f, bottomUV, 0.50f
        )
        // North (+Z)
        addQuad(list,
            cx + s, cy + s, cz + s,
            cx - s, cy + s, cz + s,
            cx - s, cy - s, cz + s,
            cx + s, cy - s, cz + s,
            0f, 0f, 1f, sideUV, 0.82f
        )
        // South (-Z)
        addQuad(list,
            cx - s, cy + s, cz - s,
            cx + s, cy + s, cz - s,
            cx + s, cy - s, cz - s,
            cx - s, cy - s, cz - s,
            0f, 0f, -1f, sideUV, 0.82f
        )
        // East (+X)
        addQuad(list,
            cx + s, cy + s, cz + s,
            cx + s, cy + s, cz - s,
            cx + s, cy - s, cz - s,
            cx + s, cy - s, cz + s,
            1f, 0f, 0f, sideUV, 0.68f
        )
        // West (-X)
        addQuad(list,
            cx - s, cy + s, cz - s,
            cx - s, cy + s, cz + s,
            cx - s, cy - s, cz + s,
            cx - s, cy - s, cz - s,
            -1f, 0f, 0f, sideUV, 0.68f
        )

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
