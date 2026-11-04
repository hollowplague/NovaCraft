package com.example.game.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLUtils
import java.util.Random

/**
 * Procedurally generates an authentic, high-fidelity 16x16 pixel-art Minecraft texture atlas
 * with exact classic color palettes, jagged grass fringe, cobblestone cracks, oak bark, and brick courses.
 */
object TextureAtlas {

    const val ATLAS_GRID_SIZE = 8 // 8x8 tiles = 64 tiles
    const val TILE_PIXELS = 16
    const val ATLAS_SIZE = ATLAS_GRID_SIZE * TILE_PIXELS // 128x128 pixels

    const val TILE_STEVE_SLEEVE = 16
    const val TILE_STEVE_SKIN = 17

    private var cachedBitmap: Bitmap? = null

    /**
     * Initializes and uploads the texture atlas into OpenGL ES.
     * Returns the GL texture handle.
     */
    fun loadTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val newTextureId = textures[0]

        val bitmap = cachedBitmap ?: createAtlasBitmap().also { cachedBitmap = it }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, newTextureId)

        // GL_NEAREST preserves crisp Minecraft pixel art without blurry bilinear filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        return newTextureId
    }

    /**
     * Calculates UV coordinates for a given tile index (0..15).
     * Incorporates sub-texel insets to completely prevent adjacent tile bleeding on GPUs.
     * Returns float array [uMin, vMin, uMax, vMax].
     */
    fun getTileUV(tileIndex: Int): FloatArray {
        val col = tileIndex % ATLAS_GRID_SIZE
        val row = tileIndex / ATLAS_GRID_SIZE
        val step = 1.0f / ATLAS_GRID_SIZE
        val subTexel = 0.5f / ATLAS_SIZE // Sub-texel inset to prevent border bleeding

        val uMin = col * step + subTexel
        val vMin = row * step + subTexel
        val uMax = (col + 1) * step - subTexel
        val vMax = (row + 1) * step - subTexel

        return floatArrayOf(uMin, vMin, uMax, vMax)
    }

    private fun createAtlasBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(ATLAS_SIZE, ATLAS_SIZE, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(ATLAS_SIZE * ATLAS_SIZE)

        for (tileIndex in 0 until 18) {
            val tilePixels = generateTilePixels(tileIndex)
            val tileCol = tileIndex % ATLAS_GRID_SIZE
            val tileRow = tileIndex / ATLAS_GRID_SIZE
            val startX = tileCol * TILE_PIXELS
            val startY = tileRow * TILE_PIXELS

            for (y in 0 until TILE_PIXELS) {
                for (x in 0 until TILE_PIXELS) {
                    val atlasX = startX + x
                    val atlasY = startY + y
                    pixels[atlasY * ATLAS_SIZE + atlasX] = tilePixels[y * TILE_PIXELS + x]
                }
            }
        }

        bmp.setPixels(pixels, 0, ATLAS_SIZE, 0, 0, ATLAS_SIZE, ATLAS_SIZE)
        return bmp
    }

    private fun generateTilePixels(tileIndex: Int): IntArray {
        val p = IntArray(TILE_PIXELS * TILE_PIXELS)
        val rng = Random((tileIndex * 19937 + 777).toLong())

        when (tileIndex) {
            0 -> { // 0: Grass Top (Authentic classic Minecraft dithered lush green)
                val greens = intArrayOf(
                    Color.rgb(89, 147, 43),   // #59932B base
                    Color.rgb(76, 124, 35),   // #4C7C23 dark
                    Color.rgb(102, 161, 48),  // #66A130 highlight
                    Color.rgb(62, 102, 27),   // #3E661B shadow
                    Color.rgb(115, 179, 54)   // #73B336 light
                )
                for (i in p.indices) {
                    p[i] = greens[rng.nextInt(greens.size)]
                }
            }
            1 -> { // 1: Grass Side (Iconic stepped grass fringe dripping over rich brown dirt)
                // Authentic Minecraft stepped fringe depths per column (x: 0..15)
                val fringeDepths = intArrayOf(2, 3, 4, 3, 2, 4, 5, 4, 3, 2, 3, 4, 5, 4, 3, 2)
                val dirtColors = intArrayOf(
                    Color.rgb(134, 96, 67),   // #866043 base dirt
                    Color.rgb(115, 81, 56),   // #735138 medium dirt
                    Color.rgb(90, 61, 38),    // #5A3D26 pebble
                    Color.rgb(150, 109, 76)   // #966D4C light pebble
                )
                val grassColors = intArrayOf(
                    Color.rgb(89, 147, 43),
                    Color.rgb(102, 161, 48),
                    Color.rgb(76, 124, 35)
                )
                val borderGreen = Color.rgb(62, 102, 27)

                for (y in 0 until TILE_PIXELS) {
                    for (x in 0 until TILE_PIXELS) {
                        val maxFringe = fringeDepths[x]
                        if (y < maxFringe - 1) {
                            p[y * 16 + x] = grassColors[rng.nextInt(grassColors.size)]
                        } else if (y == maxFringe - 1) {
                            p[y * 16 + x] = borderGreen // Dark grass border before dirt
                        } else {
                            p[y * 16 + x] = dirtColors[rng.nextInt(dirtColors.size)]
                        }
                    }
                }
            }
            2 -> { // 2: Dirt (Classic earthy pebbled dirt)
                val dirtColors = intArrayOf(
                    Color.rgb(134, 96, 67),   // #866043
                    Color.rgb(115, 81, 56),   // #735138
                    Color.rgb(90, 61, 38),    // #5A3D26
                    Color.rgb(153, 112, 78)   // #99704E
                )
                for (i in p.indices) {
                    p[i] = dirtColors[rng.nextInt(dirtColors.size)]
                }
            }
            3 -> { // 3: Stone (Mottled neutral gray with mineral flecks)
                val stoneColors = intArrayOf(
                    Color.rgb(115, 115, 115), // #737373
                    Color.rgb(90, 90, 90),     // #5A5A5A
                    Color.rgb(140, 140, 140), // #8C8C8C
                    Color.rgb(104, 104, 104)  // #686868
                )
                for (i in p.indices) {
                    p[i] = stoneColors[rng.nextInt(stoneColors.size)]
                }
            }
            4 -> { // 4: Cobblestone (Interlocking stone boulders with dark cracks)
                val crackColor = Color.rgb(60, 60, 60)
                val darkStone = Color.rgb(85, 85, 85)
                val midStone = Color.rgb(118, 118, 118)
                val lightStone = Color.rgb(148, 148, 148)

                for (y in 0 until TILE_PIXELS) {
                    for (x in 0 until TILE_PIXELS) {
                        // Irregular boulder mortar lines
                        val isMortar = (y % 4 == 0 && (x + y * 2) % 3 != 0) ||
                                (x % 5 == 0 && (y + x) % 4 == 0) ||
                                (x == 8 && y in 1..4) ||
                                (x == 3 && y in 5..8) ||
                                (x == 12 && y in 9..13)
                        if (isMortar) {
                            p[y * 16 + x] = crackColor
                        } else {
                            val isHighlight = ((x + 1) % 5 == 0) || ((y + 1) % 4 == 0 && rng.nextBoolean())
                            val isShadow = (x % 5 == 1) || (y % 4 == 1)
                            p[y * 16 + x] = when {
                                isHighlight -> lightStone
                                isShadow -> darkStone
                                else -> midStone
                            }
                        }
                    }
                }
            }
            5 -> { // 5: Oak Wood Log Side (Vertical tree bark ridges)
                val barkGroove = Color.rgb(66, 48, 26)   // #42301A
                val barkDark = Color.rgb(85, 62, 34)     // #553E22
                val barkMid = Color.rgb(104, 75, 41)     // #684B29
                val barkLight = Color.rgb(125, 91, 50)   // #7D5B32

                for (y in 0 until TILE_PIXELS) {
                    for (x in 0 until TILE_PIXELS) {
                        // Longitudinal bark channels running strictly vertically
                        val isGroove = (x == 0 || x == 4 || x == 9 || x == 13)
                        val isAdjacent = (x == 1 || x == 5 || x == 10 || x == 14)
                        p[y * 16 + x] = when {
                            isGroove -> if ((y + x) % 3 == 0) barkGroove else barkDark
                            isAdjacent -> barkMid
                            else -> if ((y * 3 + x) % 5 == 0) barkLight else barkMid
                        }
                    }
                }
            }
            6 -> { // 6: Oak Wood Log Top/Bottom (Tree cross-section annual rings)
                val barkRing = Color.rgb(66, 48, 26)
                val ringLine1 = Color.rgb(133, 102, 55)
                val ringLine2 = Color.rgb(145, 112, 62)
                val woodBase = Color.rgb(166, 133, 80)
                val woodLight = Color.rgb(184, 151, 95)
                val heartwood = Color.rgb(125, 91, 50)

                for (y in 0 until TILE_PIXELS) {
                    for (x in 0 until TILE_PIXELS) {
                        val dx = x - 7.5
                        val dy = y - 7.5
                        val r = Math.sqrt(dx * dx + dy * dy)
                        p[y * 16 + x] = when {
                            r > 6.2 -> barkRing
                            r in 4.8..5.6 -> ringLine1
                            r in 2.8..3.4 -> ringLine2
                            r < 1.6 -> heartwood
                            r in 3.4..4.8 -> woodLight
                            else -> woodBase
                        }
                    }
                }
            }
            7 -> { // 7: Oak Leaves (Foliage with transparent gaps)
                val leafColors = intArrayOf(
                    Color.argb(255, 56, 102, 24),   // #386618
                    Color.argb(255, 74, 128, 34),   // #4A8022
                    Color.argb(255, 89, 150, 42),   // #59962A
                    Color.argb(255, 43, 80, 17)     // #2B5011
                )
                for (y in 0 until TILE_PIXELS) {
                    for (x in 0 until TILE_PIXELS) {
                        // Patterned cutout holes (~25% of pixels)
                        val isHole = (x % 4 == 1 && y % 4 == 2) || (x % 4 == 3 && y % 4 == 0)
                        if (isHole) {
                            p[y * 16 + x] = Color.argb(0, 0, 0, 0)
                        } else {
                            p[y * 16 + x] = leafColors[rng.nextInt(leafColors.size)]
                        }
                    }
                }
            }
            8 -> { // 8: Wood Planks (4 horizontal wooden boards with nail details)
                val seamColor = Color.rgb(76, 55, 29)    // #4C371D
                val plankDark = Color.rgb(142, 107, 64)  // #8E6B40
                val plankMid = Color.rgb(161, 124, 77)   // #A17C4D
                val plankLight = Color.rgb(181, 142, 89) // #B58E59

                for (y in 0 until TILE_PIXELS) {
                    val isHorizontalSeam = (y == 3 || y == 7 || y == 11 || y == 15)
                    for (x in 0 until TILE_PIXELS) {
                        // Staggered vertical dividing seams between boards
                        val isVerticalSeam = (y < 4 && x == 8) ||
                                (y in 4..7 && (x == 3 || x == 13)) ||
                                (y in 8..11 && x == 9) ||
                                (y in 12..15 && (x == 4 || x == 14))
                        if (isHorizontalSeam || isVerticalSeam) {
                            p[y * 16 + x] = seamColor
                        } else {
                            val v = rng.nextInt(3)
                            p[y * 16 + x] = when (v) {
                                0 -> plankLight
                                1 -> plankDark
                                else -> plankMid
                            }
                        }
                    }
                }
            }
            9 -> { // 9: Bricks (4 rows of offset clay bricks with mortar lines)
                val mortar = Color.rgb(216, 207, 188)   // #D8CFBC
                val brickRed = Color.rgb(156, 67, 54)    // #9C4336
                val brickDark = Color.rgb(138, 56, 44)   // #8A382C
                val brickLight = Color.rgb(176, 81, 68)  // #B05144

                for (y in 0 until TILE_PIXELS) {
                    val row = y / 4
                    val isHorizontalMortar = (y % 4 == 3)
                    val offset = if (row % 2 == 0) 0 else 4
                    for (x in 0 until TILE_PIXELS) {
                        val isVerticalMortar = ((x + offset) % 8 == 7)
                        if (isHorizontalMortar || isVerticalMortar) {
                            p[y * 16 + x] = mortar
                        } else {
                            val v = rng.nextInt(3)
                            p[y * 16 + x] = when (v) {
                                0 -> brickLight
                                1 -> brickDark
                                else -> brickRed
                            }
                        }
                    }
                }
            }
            10 -> { // 10: Sand (Warm golden sand grains)
                val sandColors = intArrayOf(
                    Color.rgb(219, 211, 160), // #DBD3A0
                    Color.rgb(206, 191, 139), // #CEBF8B
                    Color.rgb(229, 221, 175), // #E5DDAF
                    Color.rgb(191, 176, 120)  // #BFB078
                )
                for (i in p.indices) {
                    p[i] = sandColors[rng.nextInt(sandColors.size)]
                }
            }
            11 -> { // 11: Glass (Clear beveled frame with glint streaks)
                val frameColor = Color.argb(220, 181, 214, 235)
                val glintColor = Color.argb(190, 255, 255, 255)
                val paneColor = Color.argb(25, 200, 228, 248)

                for (y in 0 until TILE_PIXELS) {
                    for (x in 0 until TILE_PIXELS) {
                        val isBorder = (x == 0 || x == 15 || y == 0 || y == 15)
                        val isGlint = (x == y + 3 && x in 4..7) || (x == y + 8 && x in 9..13)
                        p[y * 16 + x] = when {
                            isBorder -> frameColor
                            isGlint -> glintColor
                            else -> paneColor
                        }
                    }
                }
            }
            12 -> { // 12: Water (Classic ocean wave bands)
                val waterBands = intArrayOf(
                    Color.argb(195, 46, 92, 230),  // #2E5CE6
                    Color.argb(195, 59, 107, 240), // #3B6BF0
                    Color.argb(195, 34, 72, 202)   // #2248CA
                )
                for (y in 0 until TILE_PIXELS) {
                    for (x in 0 until TILE_PIXELS) {
                        val wave = (x + y * 2) % waterBands.size
                        p[y * 16 + x] = waterBands[wave]
                    }
                }
            }
            13 -> { // 13: Glowstone (Amber crystal clusters with white-hot centers)
                val glowBase = Color.rgb(184, 138, 56)   // #B88A38
                val glowGold = Color.rgb(245, 202, 74)   // #F5CA4A
                val glowCenter = Color.rgb(255, 246, 184) // #FFF6B8

                for (y in 0 until TILE_PIXELS) {
                    for (x in 0 until TILE_PIXELS) {
                        val isCenter = (x in 3..5 && y in 3..5) || (x in 10..12 && y in 9..11)
                        val isGold = (x in 2..6 && y in 2..6) || (x in 9..13 && y in 8..12)
                        p[y * 16 + x] = when {
                            isCenter -> glowCenter
                            isGold -> if (rng.nextBoolean()) glowGold else glowBase
                            else -> if (rng.nextInt(4) == 0) glowGold else glowBase
                        }
                    }
                }
            }
            14 -> { // 14: Bedrock (Dark obsidian / slate matrix)
                val bedrockColors = intArrayOf(
                    Color.rgb(24, 24, 24),
                    Color.rgb(48, 48, 48),
                    Color.rgb(72, 72, 72),
                    Color.rgb(32, 32, 32)
                )
                for (i in p.indices) {
                    p[i] = bedrockColors[rng.nextInt(bedrockColors.size)]
                }
            }
            15 -> { // 15: Bookshelf (Oak frame with red, blue, green book spines)
                val oakBorder = Color.rgb(142, 107, 64)
                val shelfSeam = Color.rgb(76, 55, 29)
                val redBook = Color.rgb(168, 45, 45)
                val blueBook = Color.rgb(45, 95, 168)
                val greenBook = Color.rgb(45, 145, 65)
                val goldBook = Color.rgb(185, 145, 45)

                for (y in 0 until TILE_PIXELS) {
                    for (x in 0 until TILE_PIXELS) {
                        val isBorder = (y in 0..1 || y in 7..8 || y in 14..15 || x == 0 || x == 15)
                        if (isBorder) {
                            p[y * 16 + x] = if (y == 7 || y == 14) shelfSeam else oakBorder
                        } else {
                            val bookSlot = (x / 2) % 4
                            p[y * 16 + x] = when (bookSlot) {
                                0 -> redBook
                                1 -> blueBook
                                2 -> greenBook
                                else -> goldBook
                            }
                        }
                    }
                }
            }
            16 -> { // 16: Steve classic cyan sleeve (#00A8A8 fabric with #008080 shading and cuff)
                val baseCyan = Color.rgb(0, 168, 168)
                val darkCyan = Color.rgb(0, 128, 128)
                val lightCyan = Color.rgb(18, 185, 185)
                val cuffCyan = Color.rgb(0, 105, 105)
                for (y in 0 until TILE_PIXELS) {
                    for (x in 0 until TILE_PIXELS) {
                        p[y * 16 + x] = when {
                            y >= 14 -> cuffCyan // Sleeve seam / cuff at wrist boundary
                            (x + y) % 3 == 0 -> lightCyan
                            (x * 3 + y * 7) % 5 == 0 -> darkCyan
                            else -> baseCyan
                        }
                    }
                }
            }
            17 -> { // 17: Steve classic skin hand (#C88E68 peach skin with #AF744E knuckle/finger detailing)
                val baseSkin = Color.rgb(200, 142, 104)
                val darkSkin = Color.rgb(175, 116, 78)
                val lightSkin = Color.rgb(215, 158, 120)
                val shadowSkin = Color.rgb(150, 96, 62)
                for (y in 0 until TILE_PIXELS) {
                    for (x in 0 until TILE_PIXELS) {
                        p[y * 16 + x] = when {
                            y in 6..7 && x % 4 == 0 -> shadowSkin // Knuckle / finger creases
                            y in 12..14 && x in 4..11 -> darkSkin // Palm / hand shading
                            (x + y) % 4 == 0 -> lightSkin
                            else -> baseSkin
                        }
                    }
                }
            }
        }
        return p
    }
}
