package com.example.game.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * High-performance primitive float builder avoiding heap object boxing.
 */
class FastFloatList(initialCapacity: Int = 16384) {
    var data = FloatArray(initialCapacity)
        private set
    var size = 0
        private set

    fun add(f: Float) {
        if (size >= data.size) {
            data = data.copyOf(data.size * 2)
        }
        data[size++] = f
    }

    fun toDirectBuffer(): FloatBuffer {
        val buf = ByteBuffer.allocateDirect(size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buf.put(data, 0, size)
        buf.position(0)
        return buf
    }
}

/**
 * Represents a 16x16x64 column of voxels with hidden-face-culled OpenGL mesh buffers
 * and strictly aligned face UV mapping.
 */
class Chunk(val chunkX: Int, val chunkZ: Int) {

    companion object {
        const val SIZE_X = 16
        const val SIZE_Z = 16
        const val WIDTH = 16
        const val DEPTH = 16
        const val HEIGHT = 64
        const val TOTAL_BLOCKS = SIZE_X * SIZE_Z * HEIGHT
        const val FLOATS_PER_VERTEX = 9 // x, y, z, nx, ny, nz, u, v, ao
    }

    val blocks = ByteArray(TOTAL_BLOCKS)

    @Volatile
    var isDirty = true

    var opaqueBuffer: FloatBuffer? = null
    var opaqueVertexCount = 0

    var translucentBuffer: FloatBuffer? = null
    var translucentVertexCount = 0

    inline fun getIndex(x: Int, y: Int, z: Int): Int {
        return (y * SIZE_Z + z) * SIZE_X + x
    }

    fun getBlock(x: Int, y: Int, z: Int): BlockType {
        if (x !in 0 until SIZE_X || y !in 0 until HEIGHT || z !in 0 until SIZE_Z) {
            return BlockType.AIR
        }
        return BlockType.fromId(blocks[getIndex(x, y, z)])
    }

    fun setBlock(x: Int, y: Int, z: Int, type: BlockType) {
        if (x in 0 until SIZE_X && y in 0 until HEIGHT && z in 0 until SIZE_Z) {
            blocks[getIndex(x, y, z)] = type.id
            isDirty = true
        }
    }

    fun setBlockFast(x: Int, y: Int, z: Int, typeId: Byte) {
        if (x in 0 until SIZE_X && y in 0 until HEIGHT && z in 0 until SIZE_Z) {
            blocks[getIndex(x, y, z)] = typeId
        }
    }

    private fun getNeighborBlock(
        world: VoxelWorld,
        lx: Int, y: Int, lz: Int,
        dx: Int, dy: Int, dz: Int,
        worldBaseX: Int, worldBaseZ: Int
    ): BlockType {
        val ny = y + dy
        if (ny < 0 || ny >= HEIGHT) return BlockType.AIR
        val nx = lx + dx
        val nz = lz + dz
        return if (nx in 0 until SIZE_X && nz in 0 until SIZE_Z) {
            BlockType.fromId(blocks[getIndex(nx, ny, nz)])
        } else {
            world.getBlock(worldBaseX + nx, ny, worldBaseZ + nz)
        }
    }

    /**
     * Builds OpenGL vertex buffers using Hidden-Face Culling.
     * All 6 cube faces are triangulated with counter-clockwise winding
     * and upright UV coordinate alignment.
     */
    fun buildMesh(world: VoxelWorld) {
        val opaqueFloats = FastFloatList(8192)
        val transFloats = FastFloatList(2048)

        val worldBaseX = chunkX * SIZE_X
        val worldBaseZ = chunkZ * SIZE_Z

        for (y in 0 until HEIGHT) {
            for (z in 0 until SIZE_Z) {
                for (x in 0 until SIZE_X) {
                    val block = BlockType.fromId(blocks[getIndex(x, y, z)])
                    if (block == BlockType.AIR) continue

                    val wx = (worldBaseX + x).toFloat()
                    val wy = y.toFloat()
                    val wz = (worldBaseZ + z).toFloat()

                    val targetList = if (block.isFluid || block == BlockType.GLASS) transFloats else opaqueFloats

                    // TOP Face (+Y) - 1.0 daylight
                    val topBlock = getNeighborBlock(world, x, y, z, 0, 1, 0, worldBaseX, worldBaseZ)
                    if (shouldRenderFace(block, topBlock)) {
                        val uv = TextureAtlas.getTileUV(block.topTex)
                        addQuad(
                            targetList,
                            wx, wy + 1f, wz,            // TL
                            wx + 1f, wy + 1f, wz,       // TR
                            wx + 1f, wy + 1f, wz + 1f,  // BR
                            wx, wy + 1f, wz + 1f,       // BL
                            0f, 1f, 0f,
                            uv, 1.0f
                        )
                    }

                    // BOTTOM Face (-Y) - 0.5 shadow
                    val bottomBlock = getNeighborBlock(world, x, y, z, 0, -1, 0, worldBaseX, worldBaseZ)
                    if (shouldRenderFace(block, bottomBlock)) {
                        val uv = TextureAtlas.getTileUV(block.bottomTex)
                        addQuad(
                            targetList,
                            wx, wy, wz + 1f,            // TL
                            wx + 1f, wy, wz + 1f,       // TR
                            wx + 1f, wy, wz,            // BR
                            wx, wy, wz,                 // BL
                            0f, -1f, 0f,
                            uv, 0.5f
                        )
                    }

                    // NORTH Face (+Z) - 0.8 diffuse
                    val northBlock = getNeighborBlock(world, x, y, z, 0, 0, 1, worldBaseX, worldBaseZ)
                    if (shouldRenderFace(block, northBlock)) {
                        val uv = TextureAtlas.getTileUV(block.sideTex)
                        addQuad(
                            targetList,
                            wx + 1f, wy + 1f, wz + 1f,  // TL
                            wx, wy + 1f, wz + 1f,       // TR
                            wx, wy, wz + 1f,            // BR
                            wx + 1f, wy, wz + 1f,       // BL
                            0f, 0f, 1f,
                            uv, 0.8f
                        )
                    }

                    // SOUTH Face (-Z) - 0.8 diffuse
                    val southBlock = getNeighborBlock(world, x, y, z, 0, 0, -1, worldBaseX, worldBaseZ)
                    if (shouldRenderFace(block, southBlock)) {
                        val uv = TextureAtlas.getTileUV(block.sideTex)
                        addQuad(
                            targetList,
                            wx, wy + 1f, wz,            // TL
                            wx + 1f, wy + 1f, wz,       // TR
                            wx + 1f, wy, wz,            // BR
                            wx, wy, wz,                 // BL
                            0f, 0f, -1f,
                            uv, 0.8f
                        )
                    }

                    // EAST Face (+X) - 0.65 diffuse
                    val eastBlock = getNeighborBlock(world, x, y, z, 1, 0, 0, worldBaseX, worldBaseZ)
                    if (shouldRenderFace(block, eastBlock)) {
                        val uv = TextureAtlas.getTileUV(block.sideTex)
                        addQuad(
                            targetList,
                            wx + 1f, wy + 1f, wz + 1f,  // TL
                            wx + 1f, wy + 1f, wz,       // TR
                            wx + 1f, wy, wz,            // BR
                            wx + 1f, wy, wz + 1f,       // BL
                            1f, 0f, 0f,
                            uv, 0.65f
                        )
                    }

                    // WEST Face (-X) - 0.65 diffuse
                    val westBlock = getNeighborBlock(world, x, y, z, -1, 0, 0, worldBaseX, worldBaseZ)
                    if (shouldRenderFace(block, westBlock)) {
                        val uv = TextureAtlas.getTileUV(block.sideTex)
                        addQuad(
                            targetList,
                            wx, wy + 1f, wz,            // TL
                            wx, wy + 1f, wz + 1f,       // TR
                            wx, wy, wz + 1f,            // BR
                            wx, wy, wz,                 // BL
                            -1f, 0f, 0f,
                            uv, 0.65f
                        )
                    }
                }
            }
        }

        // Convert lists into native direct FloatBuffers
        opaqueVertexCount = opaqueFloats.size / FLOATS_PER_VERTEX
        opaqueBuffer = if (opaqueVertexCount > 0) opaqueFloats.toDirectBuffer() else null

        translucentVertexCount = transFloats.size / FLOATS_PER_VERTEX
        translucentBuffer = if (translucentVertexCount > 0) transFloats.toDirectBuffer() else null

        isDirty = false
    }

    private inline fun shouldRenderFace(current: BlockType, neighbor: BlockType): Boolean {
        if (neighbor == BlockType.AIR) return true
        if (current == neighbor && (current.isFluid || current == BlockType.GLASS)) return false
        if (!current.isTransparent && neighbor.isTransparent) return true
        if (current.isTransparent && !neighbor.isTransparent) return false
        return neighbor.isFluid && !current.isFluid
    }

    /**
     * Adds a quad specified by 4 corners in counter-clockwise order with upright UV mapping.
     */
    private fun addQuad(
        list: FastFloatList,
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

        // Triangle 1: TL, BL, BR
        addVertex(list, tlX, tlY, tlZ, nx, ny, nz, uMin, vMin, ao)
        addVertex(list, blX, blY, blZ, nx, ny, nz, uMin, vMax, ao)
        addVertex(list, brX, brY, brZ, nx, ny, nz, uMax, vMax, ao)

        // Triangle 2: TL, BR, TR
        addVertex(list, tlX, tlY, tlZ, nx, ny, nz, uMin, vMin, ao)
        addVertex(list, brX, brY, brZ, nx, ny, nz, uMax, vMax, ao)
        addVertex(list, trX, trY, trZ, nx, ny, nz, uMax, vMin, ao)
    }

    private inline fun addVertex(
        list: FastFloatList,
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
