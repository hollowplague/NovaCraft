package com.example.game.engine

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Handles player physics, camera orientation, smooth 1-block auto-stepping,
 * and strafe movement with correct right-vector orientation.
 */
class Player(
    var posX: Float = 0f,
    var posY: Float = 32f,
    var posZ: Float = 0f
) {
    val EYE_HEIGHT = 1.62f
    val PLAYER_WIDTH = 0.6f
    val PLAYER_HEIGHT = 1.8f
    val HALF_WIDTH = PLAYER_WIDTH / 2f

    var yaw = 0f    // horizontal angle in degrees (0 = +Z)
    var pitch = 0f  // vertical angle in degrees (-89..89), positive = looking up, negative = looking down

    var velX = 0f
    var velY = 0f
    var velZ = 0f

    var isFlying = false // Walking mode by default
    var isOnGround = true

    // Input axes (-1f..1f)
    var inputForward = 0f
    var inputRight = 0f
    var inputJump = false

    val eyeX: Float get() = posX
    val eyeY: Float get() = posY + EYE_HEIGHT
    val eyeZ: Float get() = posZ

    /**
     * Normalized forward look vector.
     */
    fun getForwardVector(): FloatArray {
        val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()

        val x = -sin(yawRad) * cos(pitchRad)
        val y = sin(pitchRad)
        val z = cos(yawRad) * cos(pitchRad)

        return floatArrayOf(x, y, z)
    }

    /**
     * Normalized right vector matching OpenGL camera view space.
     * Moving right on screen moves player towards this vector.
     */
    fun getRightVector(): FloatArray {
        val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
        return floatArrayOf(-cos(yawRad), 0f, -sin(yawRad))
    }

    fun update(world: VoxelWorld, deltaSeconds: Float) {
        val dt = deltaSeconds.coerceIn(0.001f, 0.05f)
        val yawRad = Math.toRadians(yaw.toDouble()).toFloat()

        // Forward and right vectors in horizontal XZ plane
        val fwdX = -sin(yawRad)
        val fwdZ = cos(yawRad)
        // Camera right vector: cross(forward, up) = (-cos(yaw), 0, -sin(yaw))
        val rgtX = -cos(yawRad)
        val rgtZ = -sin(yawRad)

        val speed = if (isFlying) 10.0f else 6.0f

        val desiredMoveX = (fwdX * inputForward + rgtX * inputRight) * speed
        val desiredMoveZ = (fwdZ * inputForward + rgtZ * inputRight) * speed

        if (isFlying) {
            velX += (desiredMoveX - velX) * 12f * dt
            velZ += (desiredMoveZ - velZ) * 12f * dt
            val desiredMoveY = if (inputJump) speed else 0f
            velY += (desiredMoveY - velY) * 12f * dt

            posX += velX * dt
            posY += velY * dt
            posZ += velZ * dt
        } else {
            // Walking physics
            velX += (desiredMoveX - velX) * 12f * dt
            velZ += (desiredMoveZ - velZ) * 12f * dt

            // Gravity
            velY -= 22f * dt

            // Jump
            if (inputJump && isOnGround) {
                velY = 8.2f
                isOnGround = false
                inputJump = false
            }

            // Move X with smooth auto-jump
            val newX = posX + velX * dt
            if (!hasCollision(newX, posY, posZ, world)) {
                posX = newX
            } else {
                // Auto-jump up 1 block if obstacle is 1 block high and head clearance is free
                val stepY = posY + 1.15f
                if (isOnGround && (velX * velX > 0.04f) && !hasCollision(posX, stepY, posZ, world) && !hasCollision(newX, stepY, posZ, world)) {
                    velY = 7.4f
                    isOnGround = false
                } else {
                    velX = 0f
                }
            }

            // Move Z with smooth auto-jump
            val newZ = posZ + velZ * dt
            if (!hasCollision(posX, posY, newZ, world)) {
                posZ = newZ
            } else {
                val stepY = posY + 1.15f
                if (isOnGround && (velZ * velZ > 0.04f) && !hasCollision(posX, stepY, posZ, world) && !hasCollision(posX, stepY, newZ, world)) {
                    velY = 7.4f
                    isOnGround = false
                } else {
                    velZ = 0f
                }
            }

            // Move Y with vertical collision
            val newY = posY + velY * dt
            if (!hasCollision(posX, newY, posZ, world)) {
                posY = newY
                isOnGround = false
            } else {
                if (velY < 0f) {
                    isOnGround = true
                }
                velY = 0f
            }
        }
    }

    private fun hasCollision(px: Float, py: Float, pz: Float, world: VoxelWorld): Boolean {
        val minX = floor(px - HALF_WIDTH).toInt()
        val maxX = floor(px + HALF_WIDTH).toInt()
        val minY = floor(py).toInt()
        val maxY = floor(py + PLAYER_HEIGHT).toInt()
        val minZ = floor(pz - HALF_WIDTH).toInt()
        val maxZ = floor(pz + HALF_WIDTH).toInt()

        for (y in minY..maxY) {
            for (z in minZ..maxZ) {
                for (x in minX..maxX) {
                    if (world.getBlock(x, y, z).isSolid) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Rotates camera orientation.
     */
    fun rotate(deltaYaw: Float, deltaPitch: Float) {
        yaw = (yaw + deltaYaw) % 360f
        if (yaw < 0f) yaw += 360f
        pitch = (pitch + deltaPitch).coerceIn(-89f, 89f)
    }
}
