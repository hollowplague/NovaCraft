package com.example.game.engine

/**
 * Defines all voxel block types, their texture atlas indices, transparency,
 * and display metadata for the Minecraft Kotlin Edition.
 */
enum class BlockType(
    val id: Byte,
    val displayName: String,
    val isSolid: Boolean,
    val isTransparent: Boolean,
    val isFluid: Boolean,
    val lightLevel: Int,
    // Atlas coordinates: index (0..15) in a 4x4 atlas grid
    val topTex: Int,
    val bottomTex: Int,
    val sideTex: Int,
    val uiColor: Long
) {
    AIR(
        id = 0,
        displayName = "Air",
        isSolid = false,
        isTransparent = true,
        isFluid = false,
        lightLevel = 0,
        topTex = 0, bottomTex = 0, sideTex = 0,
        uiColor = 0x00000000
    ),
    GRASS(
        id = 1,
        displayName = "Grass Block",
        isSolid = true,
        isTransparent = false,
        isFluid = false,
        lightLevel = 0,
        topTex = 0, bottomTex = 2, sideTex = 1,
        uiColor = 0xFF5B8C33
    ),
    DIRT(
        id = 2,
        displayName = "Dirt",
        isSolid = true,
        isTransparent = false,
        isFluid = false,
        lightLevel = 0,
        topTex = 2, bottomTex = 2, sideTex = 2,
        uiColor = 0xFF866043
    ),
    STONE(
        id = 3,
        displayName = "Stone",
        isSolid = true,
        isTransparent = false,
        isFluid = false,
        lightLevel = 0,
        topTex = 3, bottomTex = 3, sideTex = 3,
        uiColor = 0xFF7A7A7A
    ),
    COBBLESTONE(
        id = 4,
        displayName = "Cobblestone",
        isSolid = true,
        isTransparent = false,
        isFluid = false,
        lightLevel = 0,
        topTex = 4, bottomTex = 4, sideTex = 4,
        uiColor = 0xFF656565
    ),
    OAK_WOOD(
        id = 5,
        displayName = "Oak Wood",
        isSolid = true,
        isTransparent = false,
        isFluid = false,
        lightLevel = 0,
        topTex = 6, bottomTex = 6, sideTex = 5,
        uiColor = 0xFF6C5331
    ),
    OAK_LEAVES(
        id = 6,
        displayName = "Oak Leaves",
        isSolid = true,
        isTransparent = true,
        isFluid = false,
        lightLevel = 0,
        topTex = 7, bottomTex = 7, sideTex = 7,
        uiColor = 0xFF355E1B
    ),
    WOOD_PLANKS(
        id = 7,
        displayName = "Wood Planks",
        isSolid = true,
        isTransparent = false,
        isFluid = false,
        lightLevel = 0,
        topTex = 8, bottomTex = 8, sideTex = 8,
        uiColor = 0xFFA17C4D
    ),
    BRICK(
        id = 8,
        displayName = "Bricks",
        isSolid = true,
        isTransparent = false,
        isFluid = false,
        lightLevel = 0,
        topTex = 9, bottomTex = 9, sideTex = 9,
        uiColor = 0xFF994E3E
    ),
    SAND(
        id = 9,
        displayName = "Sand",
        isSolid = true,
        isTransparent = false,
        isFluid = false,
        lightLevel = 0,
        topTex = 10, bottomTex = 10, sideTex = 10,
        uiColor = 0xFFD8CF9B
    ),
    GLASS(
        id = 10,
        displayName = "Glass",
        isSolid = true,
        isTransparent = true,
        isFluid = false,
        lightLevel = 0,
        topTex = 11, bottomTex = 11, sideTex = 11,
        uiColor = 0x88C4E8FF
    ),
    WATER(
        id = 11,
        displayName = "Water",
        isSolid = false,
        isTransparent = true,
        isFluid = true,
        lightLevel = 0,
        topTex = 12, bottomTex = 12, sideTex = 12,
        uiColor = 0xAA2B65EC
    ),
    GLOWSTONE(
        id = 12,
        displayName = "Glowstone",
        isSolid = true,
        isTransparent = false,
        isFluid = false,
        lightLevel = 15,
        topTex = 13, bottomTex = 13, sideTex = 13,
        uiColor = 0xFFFFDA68
    ),
    BEDROCK(
        id = 13,
        displayName = "Bedrock",
        isSolid = true,
        isTransparent = false,
        isFluid = false,
        lightLevel = 0,
        topTex = 14, bottomTex = 14, sideTex = 14,
        uiColor = 0xFF333333
    ),
    BOOKSHELF(
        id = 14,
        displayName = "Bookshelf",
        isSolid = true,
        isTransparent = false,
        isFluid = false,
        lightLevel = 0,
        topTex = 8, bottomTex = 8, sideTex = 15,
        uiColor = 0xFF8A5A36
    );

    companion object {
        private val BY_ID = Array(16) { id -> values().firstOrNull { it.id == id.toByte() } ?: AIR }
        fun fromId(id: Byte): BlockType {
            val intId = id.toInt()
            return if (intId in 0..15) BY_ID[intId] else AIR
        }

        /** All blocks selectable in creative mode (excludes Air and Bedrock) */
        val CREATIVE_INVENTORY = listOf(
            GRASS, DIRT, STONE, COBBLESTONE,
            OAK_WOOD, OAK_LEAVES, WOOD_PLANKS, BRICK,
            SAND, GLASS, WATER, GLOWSTONE, BOOKSHELF
        )

        /** Default 9 quick-access slots in the mobile hotbar (starts empty) */
        val DEFAULT_HOTBAR = List(9) { AIR }
    }
}

/** Authentic MCPE block hardness destruction time in seconds */
val BlockType.breakTimeSec: Float
    get() = when (this) {
        BlockType.AIR -> 0.0f
        BlockType.OAK_LEAVES -> 0.35f
        BlockType.GLASS -> 0.40f
        BlockType.GLOWSTONE -> 0.50f
        BlockType.SAND -> 0.65f
        BlockType.GRASS, BlockType.DIRT -> 0.75f
        BlockType.BOOKSHELF -> 1.4f
        BlockType.WOOD_PLANKS, BlockType.OAK_WOOD -> 1.8f
        BlockType.BRICK -> 2.0f
        BlockType.STONE, BlockType.COBBLESTONE -> 2.2f
        BlockType.BEDROCK, BlockType.WATER -> 9999f
    }
