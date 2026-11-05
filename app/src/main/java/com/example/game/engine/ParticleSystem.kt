package com.example.game.engine

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Random

/**
 * High-performance 3D block-destruction voxel particle system.
 * When a block is broken, spawns 16 small debris cubes colored and textured with the broken block's
 * authentic palette, bursting outward with gravity, air resistance, and ground collision.
 */
class ParticleSystem {

    companion object {
        private const val MAX_PARTICLES = 128
        private const val PARTICLE_SIZE = 0.042f // Half-extent = ~0.084m mini cube
        private const val GRAVITY = 9.8f
        private const val MAX_LIFETIME = 1.0f // 1 second
    }

    private class Particle(
        var x: Float = 0f,
        var y: Float = 0f,
        var z: Float = 0f,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var vz: Float = 0f,
        var uMin: Float = 0f,
        var vMin: Float = 0f,
        var uMax: Float = 0f,
        var vMax: Float = 0f,
        var life: Float = 0f,
        var maxLife: Float = MAX_LIFETIME,
        var active: Boolean = false
    )

    private val particles = Array(MAX_PARTICLES) { Particle() }
    private val random = Random()

    // 36 vertices per cube * 9 floats per vertex = 324 floats per particle
    private val maxFloats = MAX_PARTICLES * 36 * Chunk.FLOATS_PER_VERTEX
    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(maxFloats * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private var activeVertexCount = 0

    /**
     * Spawns 16 debris cubes at the broken block location.
     */
    fun spawnBlockBreak(blockX: Float, blockY: Float, blockZ: Float, blockType: BlockType) {
        if (blockType == BlockType.AIR || blockType == BlockType.BEDROCK || blockType.isFluid) return

        // Get UV bounding box for the block texture
        val uv = TextureAtlas.getTileUV(blockType.sideTex)
        val uStep = (uv[2] - uv[0]) / 4f
        val vStep = (uv[3] - uv[1]) / 4f

        val centerX = blockX + 0.5f
        val centerY = blockY + 0.5f
        val centerZ = blockZ + 0.5f

        // Exactly 16 debris cubes in a 4x4 spatial/texture grid burst
        var spawned = 0
        for (i in 0 until 16) {
            val p = findInactiveParticle() ?: break

            // Sub-texture coordinates for this particle piece (4x4 pieces per texture tile)
            val subCol = i % 4
            val subRow = i / 4
            p.uMin = uv[0] + subCol * uStep
            p.vMin = uv[1] + subRow * vStep
            p.uMax = p.uMin + uStep
            p.vMax = p.vMin + vStep

            // Distributed starting position across the broken block volume
            val offsetX = (subCol / 3.0f - 0.5f) * 0.6f + (random.nextFloat() - 0.5f) * 0.15f
            val offsetY = (subRow / 3.0f - 0.5f) * 0.6f + (random.nextFloat() - 0.5f) * 0.15f
            val offsetZ = (random.nextFloat() - 0.5f) * 0.6f

            p.x = centerX + offsetX
            p.y = centerY + offsetY
            p.z = centerZ + offsetZ

            // Burst velocity: outward impulse + upward pop + gravity
            val angle = random.nextFloat() * 2.0 * Math.PI
            val speed = 1.8f + random.nextFloat() * 1.8f
            p.vx = (Math.cos(angle) * speed).toFloat()
            p.vy = 2.2f + random.nextFloat() * 2.0f // Upward burst
            p.vz = (Math.sin(angle) * speed).toFloat()

            p.life = 0f
            p.maxLife = 0.75f + random.nextFloat() * 0.45f
            p.active = true
            spawned++
        }
    }

    private fun findInactiveParticle(): Particle? {
        for (p in particles) {
            if (!p.active) return p
        }
        // If all active, reuse oldest
        var oldest: Particle? = null
        var maxAge = -1f
        for (p in particles) {
            if (p.life > maxAge) {
                maxAge = p.life
                oldest = p
            }
        }
        return oldest
    }

    /**
     * Updates physics: gravity, velocity, drag, and lifetime.
     */
    fun update(deltaSec: Float, world: VoxelWorld) {
        val dt = deltaSec.coerceIn(0.001f, 0.05f)

        for (p in particles) {
            if (!p.active) continue

            p.life += dt
            if (p.life >= p.maxLife) {
                p.active = false
                continue
            }

            // Apply gravity
            p.vy -= GRAVITY * dt

            // Air friction
            val friction = (1.0f - 1.2f * dt).coerceIn(0f, 1f)
            p.vx *= friction
            p.vz *= friction

            // Simple block collision: if particle hits solid ground below, bounce with damping
            val nextX = p.x + p.vx * dt
            val nextY = p.y + p.vy * dt
            val nextZ = p.z + p.vz * dt

            val blockBelow = world.getBlock(nextX.toInt(), nextY.toInt(), nextZ.toInt())
            if (blockBelow != BlockType.AIR && !blockBelow.isFluid && p.vy < 0) {
                p.vy = -p.vy * 0.3f // Slight bounce
                p.vx *= 0.6f
                p.vz *= 0.6f
                p.y = nextY.toInt() + 1.0f + PARTICLE_SIZE // Rest above block
            } else {
                p.x = nextX
                p.y = nextY
                p.z = nextZ
            }
        }

        // Rebuild dynamic mesh
        rebuildMesh()
    }

    private fun rebuildMesh() {
        vertexBuffer.clear()
        var vertexCount = 0

        for (p in particles) {
            if (!p.active) continue

            val s = PARTICLE_SIZE
            val cx = p.x
            val cy = p.y
            val cz = p.z

            val uMin = p.uMin
            val vMin = p.vMin
            val uMax = p.uMax
            val vMax = p.vMax

            // Top (+Y)
            addParticleQuad(
                cx - s, cy + s, cz - s,
                cx + s, cy + s, cz - s,
                cx + s, cy + s, cz + s,
                cx - s, cy + s, cz + s,
                0f, 1f, 0f, uMin, vMin, uMax, vMax, 1.0f
            )
            // Bottom (-Y)
            addParticleQuad(
                cx - s, cy - s, cz + s,
                cx + s, cy - s, cz + s,
                cx + s, cy - s, cz - s,
                cx - s, cy - s, cz - s,
                0f, -1f, 0f, uMin, vMin, uMax, vMax, 0.55f
            )
            // North (+Z)
            addParticleQuad(
                cx + s, cy + s, cz + s,
                cx - s, cy + s, cz + s,
                cx - s, cy - s, cz + s,
                cx + s, cy - s, cz + s,
                0f, 0f, 1f, uMin, vMin, uMax, vMax, 0.8f
            )
            // South (-Z)
            addParticleQuad(
                cx - s, cy + s, cz - s,
                cx + s, cy + s, cz - s,
                cx + s, cy - s, cz - s,
                cx - s, cy - s, cz - s,
                0f, 0f, -1f, uMin, vMin, uMax, vMax, 0.8f
            )
            // East (+X)
            addParticleQuad(
                cx + s, cy + s, cz + s,
                cx + s, cy + s, cz - s,
                cx + s, cy - s, cz - s,
                cx + s, cy - s, cz + s,
                1f, 0f, 0f, uMin, vMin, uMax, vMax, 0.7f
            )
            // West (-X)
            addParticleQuad(
                cx - s, cy + s, cz - s,
                cx - s, cy + s, cz + s,
                cx - s, cy - s, cz + s,
                cx - s, cy - s, cz - s,
                -1f, 0f, 0f, uMin, vMin, uMax, vMax, 0.7f
            )

            vertexCount += 36
        }

        vertexBuffer.flip()
        activeVertexCount = vertexCount
    }

    private fun addParticleQuad(
        tlX: Float, tlY: Float, tlZ: Float,
        trX: Float, trY: Float, trZ: Float,
        brX: Float, brY: Float, brZ: Float,
        blX: Float, blY: Float, blZ: Float,
        nx: Float, ny: Float, nz: Float,
        uMin: Float, vMin: Float, uMax: Float, vMax: Float,
        ao: Float
    ) {
        addVertex(tlX, tlY, tlZ, nx, ny, nz, uMin, vMin, ao)
        addVertex(blX, blY, blZ, nx, ny, nz, uMin, vMax, ao)
        addVertex(brX, brY, brZ, nx, ny, nz, uMax, vMax, ao)

        addVertex(tlX, tlY, tlZ, nx, ny, nz, uMin, vMin, ao)
        addVertex(brX, brY, brZ, nx, ny, nz, uMax, vMax, ao)
        addVertex(trX, trY, trZ, nx, ny, nz, uMax, vMin, ao)
    }

    private fun addVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float,
        ao: Float
    ) {
        vertexBuffer.put(x)
        vertexBuffer.put(y)
        vertexBuffer.put(z)
        vertexBuffer.put(nx)
        vertexBuffer.put(ny)
        vertexBuffer.put(nz)
        vertexBuffer.put(u)
        vertexBuffer.put(v)
        vertexBuffer.put(ao)
    }

    /**
     * Renders all active debris particles using the standard VoxelShader.
     */
    fun render(shader: VoxelShader) {
        if (activeVertexCount <= 0) return

        val stride = Chunk.FLOATS_PER_VERTEX * 4

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(shader.aPositionLoc)
        GLES20.glVertexAttribPointer(shader.aPositionLoc, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(3)
        GLES20.glEnableVertexAttribArray(shader.aNormalLoc)
        GLES20.glVertexAttribPointer(shader.aNormalLoc, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(6)
        GLES20.glEnableVertexAttribArray(shader.aTexCoordLoc)
        GLES20.glVertexAttribPointer(shader.aTexCoordLoc, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(8)
        GLES20.glEnableVertexAttribArray(shader.aAOLoc)
        GLES20.glVertexAttribPointer(shader.aAOLoc, 1, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, activeVertexCount)

        GLES20.glDisableVertexAttribArray(shader.aPositionLoc)
        GLES20.glDisableVertexAttribArray(shader.aNormalLoc)
        GLES20.glDisableVertexAttribArray(shader.aTexCoordLoc)
        GLES20.glDisableVertexAttribArray(shader.aAOLoc)
    }
}
