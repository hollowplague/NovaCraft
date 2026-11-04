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
 * and high-speed Amanatides-Woo 3D DDA voxel raycasting.
 */
class VoxelWorld(val seed: Long = 424242L) {

    private val chunks = ConcurrentHashMap<Long, Chunk>()
    private val perlin = PerlinNoise(seed)

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

        // Mark neighbor chunks dirty if modified on chunk boundaries
        if (lx == 0) getChunk(cx - 1, cz)?.isDirty = true
        if (lx == 15) getChunk(cx + 1, cz)?.isDirty = true
        if (lz == 0) getChunk(cx, cz - 1)?.isDirty = true
        if (lz == 15) getChunk(cx, cz + 1)?.isDirty = true
    }

    /**
     * Procedurally generates undulating terrain with hills, water lakes,
     * sand beaches, and leafy oak trees.
     */
    fun generateWorld() {
        chunks.clear()

        // 1. Generate terrain height & blocks
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

        // 2. Generate Oak Trees on top of grass
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
                            // Tree probability check (pseudo-random with spatial separation)
                            if (treeRng.nextFloat() < 0.035f) {
                                placeTree(wx, topY + 1, wz)
                            }
                        }
                    }
                }
            }
        }

        // 3. Mark all chunks dirty for progressive background meshing on the GL rendering thread.
        // No mesh building is performed here so onCreate and initial window layout completes in <10ms!
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
        // Top crown leaf cross
        setBlock(rootX, leafBaseY + 3, rootZ, BlockType.OAK_LEAVES)
        setBlock(rootX + 1, leafBaseY + 3, rootZ, BlockType.OAK_LEAVES)
        setBlock(rootX - 1, leafBaseY + 3, rootZ, BlockType.OAK_LEAVES)
        setBlock(rootX, leafBaseY + 3, rootZ + 1, BlockType.OAK_LEAVES)
        setBlock(rootX, leafBaseY + 3, rootZ - 1, BlockType.OAK_LEAVES)
    }

    fun getTopSolidBlockY(x: Int, z: Int): Int {
        for (y in Chunk.HEIGHT - 1 downTo 0) {
            val b = getBlock(x, y, z)
            if (b.isSolid) return y
        }
        return 0
    }

    /**
     * Fast Amanatides-Woo 3D Digital Differential Analyzer (DDA) raycast.
     * Accurately finds targeted voxel and face normal without stepping gaps.
     */
    fun raycast(
        originX: Float, originY: Float, originZ: Float,
        dirX: Float, dirY: Float, dirZ: Float,
        maxDistance: Float = 7.0f
    ): RaycastHit? {
        var x = floor(originX).toInt()
        var y = floor(originY).toInt()
        var z = floor(originZ).toInt()

        val stepX = sign(dirX).toInt()
        val stepY = sign(dirY).toInt()
        val stepZ = sign(dirZ).toInt()

        val tDeltaX = if (dirX != 0f) abs(1.0f / dirX) else Float.MAX_VALUE
        val tDeltaY = if (dirY != 0f) abs(1.0f / dirY) else Float.MAX_VALUE
        val tDeltaZ = if (dirZ != 0f) abs(1.0f / dirZ) else Float.MAX_VALUE

        var tMaxX = if (stepX > 0) (x + 1.0f - originX) * tDeltaX else (originX - x) * tDeltaX
        var tMaxY = if (stepY > 0) (y + 1.0f - originY) * tDeltaY else (originY - y) * tDeltaY
        var tMaxZ = if (stepZ > 0) (z + 1.0f - originZ) * tDeltaZ else (originZ - z) * tDeltaZ

        var normalX = 0
        var normalY = 0
        var normalZ = 0
        var dist = 0.0f

        while (dist < maxDistance) {
            val block = getBlock(x, y, z)
            if (block.isSolid || (block.isFluid && block != BlockType.AIR)) {
                return RaycastHit(x, y, z, normalX, normalY, normalZ, block)
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX
                    dist = tMaxX
                    tMaxX += tDeltaX
                    normalX = -stepX
                    normalY = 0
                    normalZ = 0
                } else {
                    z += stepZ
                    dist = tMaxZ
                    tMaxZ += tDeltaZ
                    normalX = 0
                    normalY = 0
                    normalZ = -stepZ
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY
                    dist = tMaxY
                    tMaxY += tDeltaY
                    normalX = 0
                    normalY = -stepY
                    normalZ = 0
                } else {
                    z += stepZ
                    dist = tMaxZ
                    tMaxZ += tDeltaZ
                    normalX = 0
                    normalY = 0
                    normalZ = -stepZ
                }
            }
        }
        return null
    }
}
