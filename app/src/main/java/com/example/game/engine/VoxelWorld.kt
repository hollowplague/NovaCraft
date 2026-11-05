package com.example.game.engine

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign

data class RaycastHit(
    val hitX: Int,
    val hitY: Int,
    val hitZ: Int,
    val normalX: Int,
    val normalY: Int,
    val normalZ: Int,
    val block: BlockType
)

/**
 * Manages procedural generation, chunk storage, block manipulation,
 * world delta tracking for auto-save, and high-speed Amanatides-Woo 3D DDA voxel raycasting.
 */
class VoxelWorld(
    var seed: Long = 424242L,
    var worldType: String = "infinite" // "infinite" or "flat"
) {

    private val chunks = ConcurrentHashMap<Long, Chunk>()
    private var perlin = PerlinNoise(seed)

    // Tracks placed or broken blocks for auto-save delta persistence
    val blockDeltas = ConcurrentHashMap<String, Byte>()

    val SEA_LEVEL = 20
    val WORLD_RADIUS = 2 // -2..2 in chunk coords = 5x5 = 25 chunks (80x80 blocks)

    init {
        generateWorld()
    }

    private fun chunkKey(cx: Int, cz: Int): Long {
        return (cx.toLong() and 0xFFFFFFFFL) or ((cz.toLong() and 0xFFFFFFFFL) shl 32)
    }

    fun getChunk(cx: Int, cz: Int): Chunk? = chunks[chunkKey(cx, cz)]

    fun getAllChunks(): Collection<Chunk> = chunks.values

    fun getBlock(x: Int, y: Int, z: Int): BlockType {
        if (y < 0 || y >= Chunk.HEIGHT) return BlockType.AIR
        val cx = floor(x / 16.0).toInt()
        val cz = floor(z / 16.0).toInt()
        val chunk = getChunk(cx, cz) ?: return BlockType.AIR
        val lx = (x % 16 + 16) % 16
        val lz = (z % 16 + 16) % 16
        return chunk.getBlock(lx, y, lz)
    }

    fun setBlock(x: Int, y: Int, z: Int, type: BlockType) {
        if (y < 0 || y >= Chunk.HEIGHT) return
        val cx = floor(x / 16.0).toInt()
        val cz = floor(z / 16.0).toInt()
        val chunk = getChunk(cx, cz) ?: return
        val lx = (x % 16 + 16) % 16
        val lz = (z % 16 + 16) % 16

        chunk.setBlock(lx, y, lz, type)
        // Record modification delta for auto-save persistence
        blockDeltas["$x,$y,$z"] = type.id

        // Mark neighbor chunks dirty if modified on chunk boundaries
        if (lx == 0) getChunk(cx - 1, cz)?.isDirty = true
        if (lx == 15) getChunk(cx + 1, cz)?.isDirty = true
        if (lz == 0) getChunk(cx, cz - 1)?.isDirty = true
        if (lz == 15) getChunk(cx, cz + 1)?.isDirty = true
    }

    /**
     * Resets world parameters, regenerates base terrain, and applies saved block deltas.
     */
    fun resetWith(newSeed: Long, newWorldType: String, deltas: Map<String, Byte> = emptyMap()) {
        this.seed = newSeed
        this.worldType = if (newWorldType.equals("flat", ignoreCase = true)) "flat" else "infinite"
        this.perlin = PerlinNoise(newSeed)
        this.blockDeltas.clear()

        generateWorld()

        if (deltas.isNotEmpty()) {
            applyBlockDeltas(deltas)
        }
    }

    /**
     * Applies saved block modifications on top of the generated base terrain.
     */
    fun applyBlockDeltas(deltas: Map<String, Byte>) {
        blockDeltas.putAll(deltas)
        for ((key, byteId) in deltas) {
            val parts = key.split(",")
            if (parts.size == 3) {
                val x = parts[0].toIntOrNull() ?: continue
                val y = parts[1].toIntOrNull() ?: continue
                val z = parts[2].toIntOrNull() ?: continue
                if (y in 0 until Chunk.HEIGHT) {
                    val cx = floor(x / 16.0).toInt()
                    val cz = floor(z / 16.0).toInt()
                    val chunk = getChunk(cx, cz) ?: continue
                    val lx = (x % 16 + 16) % 16
                    val lz = (z % 16 + 16) % 16
                    chunk.setBlock(lx, y, lz, BlockType.fromId(byteId))
                }
            }
        }
    }

    /**
     * Procedurally generates undulating terrain with hills, water lakes,
     * sand beaches, and leafy oak trees for Infinite worlds, or Bedrock Flat terrain for Flat worlds.
     */
    fun generateWorld() {
        chunks.clear()

        if (worldType.equals("flat", ignoreCase = true)) {
            // Authentic Minecraft Bedrock Flat: 1 Bedrock (Y=0), 2 Dirt (Y=1, 2), 1 Grass (Y=3)
            for (cz in -WORLD_RADIUS..WORLD_RADIUS) {
                for (cx in -WORLD_RADIUS..WORLD_RADIUS) {
                    val chunk = Chunk(cx, cz)
                    for (lz in 0 until Chunk.SIZE_Z) {
                        for (lx in 0 until Chunk.SIZE_X) {
                            chunk.setBlockFast(lx, 0, lz, BlockType.BEDROCK.id)
                            chunk.setBlockFast(lx, 1, lz, BlockType.DIRT.id)
                            chunk.setBlockFast(lx, 2, lz, BlockType.DIRT.id)
                            chunk.setBlockFast(lx, 3, lz, BlockType.GRASS.id)
                        }
                    }
                    chunk.isDirty = true
                    chunks[chunkKey(cx, cz)] = chunk
                }
            }
            return
        }

        // 1. Infinite World Generation
        for (cz in -WORLD_RADIUS..WORLD_RADIUS) {
            for (cx in -WORLD_RADIUS..WORLD_RADIUS) {
                val chunk = Chunk(cx, cz)
                val baseWorldX = cx * Chunk.SIZE_X
                val baseWorldZ = cz * Chunk.SIZE_Z

                for (lz in 0 until Chunk.SIZE_Z) {
                    for (lx in 0 until Chunk.SIZE_X) {
                        val wx = baseWorldX + lx
                        val wz = baseWorldZ + lz

                        // Octave noise for rolling hills and mountain ridges
                        val elevation = perlin.octaveNoise2D(wx * 0.02f, wz * 0.02f, 4, 0.45f)
                        val detail = perlin.octaveNoise2D(wx * 0.08f, wz * 0.08f, 2, 0.5f) * 3f
                        val height = (24f + elevation * 16f + detail).toInt().coerceIn(4, Chunk.HEIGHT - 10)

                        // Bedrock
                        chunk.setBlockFast(lx, 0, lz, BlockType.BEDROCK.id)

                        // Stone stratum
                        val stoneTop = (height - 4).coerceAtLeast(1)
                        for (y in 1..stoneTop) {
                            chunk.setBlockFast(lx, y, lz, BlockType.STONE.id)
                        }

                        // Dirt layer
                        for (y in (stoneTop + 1) until height) {
                            chunk.setBlockFast(lx, y, lz, BlockType.DIRT.id)
                        }

                        // Surface block
                        if (height <= SEA_LEVEL + 1) {
                            chunk.setBlockFast(lx, height, lz, BlockType.SAND.id)
                        } else {
                            chunk.setBlockFast(lx, height, lz, BlockType.GRASS.id)
                        }

                        // Water body
                        if (height < SEA_LEVEL) {
                            for (y in (height + 1)..SEA_LEVEL) {
                                chunk.setBlockFast(lx, y, lz, BlockType.WATER.id)
                            }
                        }
                    }
                }
                chunk.isDirty = true
                chunks[chunkKey(cx, cz)] = chunk
            }
        }

        // 2. Generate Oak Trees on top of grass in Infinite worlds
        val treeRng = java.util.Random(seed + 999)
        for (cz in -WORLD_RADIUS..WORLD_RADIUS) {
            for (cx in -WORLD_RADIUS..WORLD_RADIUS) {
                val baseWorldX = cx * Chunk.SIZE_X
                val baseWorldZ = cz * Chunk.SIZE_Z

                for (lz in 2 until Chunk.SIZE_Z - 2) {
                    for (lx in 2 until Chunk.SIZE_X - 2) {
                        val wx = baseWorldX + lx
                        val wz = baseWorldZ + lz

                        val topY = getTopSolidBlockY(wx, wz)
                        if (topY > SEA_LEVEL && getBlock(wx, topY, wz) == BlockType.GRASS) {
                            if (treeRng.nextFloat() < 0.035f) {
                                placeTree(wx, topY + 1, wz)
                            }
                        }
                    }
                }
            }
        }

        // 3. Mark all chunks dirty for progressive background meshing on the GL rendering thread
        for (chunk in chunks.values) {
            chunk.isDirty = true
        }
    }

    private fun placeTree(rootX: Int, rootY: Int, rootZ: Int) {
        val trunkHeight = 4 + (abs(rootX * 31 + rootZ * 17) % 2)

        // Wood trunk
        for (y in 0 until trunkHeight) {
            setBlock(rootX, rootY + y, rootZ, BlockType.OAK_WOOD)
        }

        // Leaves canopy
        val leafBaseY = rootY + trunkHeight - 2
        for (dy in 0..2) {
            val radius = if (dy == 2) 1 else 2
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    if (abs(dx) == radius && abs(dz) == radius && dy != 0) continue
                    val targetX = rootX + dx
                    val targetY = leafBaseY + dy
                    val targetZ = rootZ + dz
                    if (getBlock(targetX, targetY, targetZ) == BlockType.AIR) {
                        setBlock(targetX, targetY, targetZ, BlockType.OAK_LEAVES)
                    }
                }
            }
        }
    }

    /**
     * Raycasts from (origin) along (direction) up to maxDistance using
     * the Amanatides & Woo 3D Digital Differential Analyzer (DDA) grid traversal algorithm.
     */
    fun raycast(
        originX: Float, originY: Float, originZ: Float,
        dirX: Float, dirY: Float, dirZ: Float,
        maxDistance: Float = 6.0f
    ): RaycastHit? {
        var currentX = floor(originX.toDouble()).toInt()
        var currentY = floor(originY.toDouble()).toInt()
        var currentZ = floor(originZ.toDouble()).toInt()

        val stepX = sign(dirX).toInt()
        val stepY = sign(dirY).toInt()
        val stepZ = sign(dirZ).toInt()

        val tDeltaX = if (dirX != 0f) abs(1.0f / dirX) else Float.MAX_VALUE
        val tDeltaY = if (dirY != 0f) abs(1.0f / dirY) else Float.MAX_VALUE
        val tDeltaZ = if (dirZ != 0f) abs(1.0f / dirZ) else Float.MAX_VALUE

        val nextVoxelBoundaryX = if (stepX > 0) currentX + 1.0f else currentX.toFloat()
        val nextVoxelBoundaryY = if (stepY > 0) currentY + 1.0f else currentY.toFloat()
        val nextVoxelBoundaryZ = if (stepZ > 0) currentZ + 1.0f else currentZ.toFloat()

        var tMaxX = if (dirX != 0f) (nextVoxelBoundaryX - originX) / dirX else Float.MAX_VALUE
        var tMaxY = if (dirY != 0f) (nextVoxelBoundaryY - originY) / dirY else Float.MAX_VALUE
        var tMaxZ = if (dirZ != 0f) (nextVoxelBoundaryZ - originZ) / dirZ else Float.MAX_VALUE

        var normalX = 0
        var normalY = 0
        var normalZ = 0
        var distanceTraveled = 0f

        while (distanceTraveled <= maxDistance) {
            val block = getBlock(currentX, currentY, currentZ)
            if (block != BlockType.AIR && !block.isFluid) {
                return RaycastHit(currentX, currentY, currentZ, normalX, normalY, normalZ, block)
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    distanceTraveled = tMaxX
                    tMaxX += tDeltaX
                    currentX += stepX
                    normalX = -stepX
                    normalY = 0
                    normalZ = 0
                } else {
                    distanceTraveled = tMaxZ
                    tMaxZ += tDeltaZ
                    currentZ += stepZ
                    normalX = 0
                    normalY = 0
                    normalZ = -stepZ
                }
            } else {
                if (tMaxY < tMaxZ) {
                    distanceTraveled = tMaxY
                    tMaxY += tDeltaY
                    currentY += stepY
                    normalX = 0
                    normalY = -stepY
                    normalZ = 0
                } else {
                    distanceTraveled = tMaxZ
                    tMaxZ += tDeltaZ
                    currentZ += stepZ
                    normalX = 0
                    normalY = 0
                    normalZ = -stepZ
                }
            }
        }
        return null
    }

    /**
     * Returns the highest solid block Y coordinate at world coordinate (x, z).
     */
    fun getTopSolidBlockY(x: Int, z: Int): Int {
        if (worldType.equals("flat", ignoreCase = true)) return 3
        for (y in Chunk.HEIGHT - 1 downTo 0) {
            val b = getBlock(x, y, z)
            if (b != BlockType.AIR && !b.isFluid) return y
        }
        return SEA_LEVEL
    }
}
