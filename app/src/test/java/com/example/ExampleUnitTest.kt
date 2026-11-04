package com.example

import com.example.game.engine.BlockType
import com.example.game.engine.VoxelWorld
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Minecraft Kotlin Edition game logic, procedural generation, and raycasting.
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun voxel_world_procedural_generation_test() {
        val world = VoxelWorld(12345L)
        val topY = world.getTopSolidBlockY(0, 0)
        assertTrue("Top Y should be above bedrock", topY > 0)
        val topBlock = world.getBlock(0, topY, 0)
        assertTrue("Surface block should be solid", topBlock.isSolid)

        // Test block modification
        world.setBlock(0, topY + 1, 0, BlockType.BRICK)
        assertEquals(BlockType.BRICK, world.getBlock(0, topY + 1, 0))

        // Test mining
        world.setBlock(0, topY + 1, 0, BlockType.AIR)
        assertEquals(BlockType.AIR, world.getBlock(0, topY + 1, 0))
    }

    @Test
    fun raycast_hits_solid_block() {
        val world = VoxelWorld(999L)
        val topY = world.getTopSolidBlockY(0, 0)

        // Cast ray downwards from above
        val hit = world.raycast(0.5f, topY + 3.5f, 0.5f, 0f, -1f, 0f, 6.0f)
        assertNotNull(hit)
        assertEquals(0, hit?.hitX)
        assertEquals(topY, hit?.hitY)
        assertEquals(0, hit?.hitZ)
        assertEquals(1, hit?.normalY) // Top face normal is +Y
    }

    @Test
    fun camera_pitch_swipe_up_looks_up() {
        val player = com.example.game.engine.Player()
        player.pitch = 0f

        // Swipe up: deltaPitch is positive
        player.rotate(0f, 30f)
        assertEquals(30f, player.pitch, 0.001f)

        val forward = player.getForwardVector()
        // When pitch is positive, forward Y must be positive (looking up towards the sky)
        assertTrue("Looking up should have positive Y forward component", forward[1] > 0f)
    }

    @Test
    fun strafe_right_orientation_test() {
        val player = com.example.game.engine.Player()
        player.yaw = 0f // Facing +Z
        val right = player.getRightVector()
        // With yaw = 0, right vector should be (-1, 0, 0)
        assertEquals(-1f, right[0], 0.001f)
        assertEquals(0f, right[2], 0.001f)
    }

    @Test
    fun jump_mechanic_test() {
        val world = com.example.game.engine.VoxelWorld(12345L)
        val player = com.example.game.engine.Player(0f, 30f, 0f)
        player.isOnGround = true
        player.inputJump = true

        player.update(world, 0.016f)
        // Jump impulse should propel velY upward
        assertTrue("Player should have positive vertical velocity after jump", player.velY > 0f)
    }

    @Test
    fun viewmodel_swing_test() {
        val vm = com.example.game.engine.ViewmodelRenderer()
        vm.triggerSwing()
        vm.update(0.05f, false)
        // Should update smoothly without exceptions
        assertTrue(true)
    }
}
