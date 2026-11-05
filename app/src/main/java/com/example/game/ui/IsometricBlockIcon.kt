package com.example.game.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.game.engine.BlockType
import com.example.game.engine.TextureAtlas
import kotlin.math.roundToInt

/**
 * Renders an authentic Minecraft 3D isometric block icon exported as a crisp 2D bitmap.
 * Recreates the exact Minecraft inventory icon projection:
 * - 32x32 pixel canvas with isometric cube orientation
 * - Real 16x16 pixel-art textures from TextureAtlas for Top, Left, and Right faces
 * - Accurate lighting: Top at 100%, Right at 80%, Left at 60%
 * - Clean pixel silhouette border
 */
@Composable
fun IsometricBlockIcon(
    block: BlockType,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    if (block == BlockType.AIR) return

    val bitmap = remember(block) {
        renderIsometricBlockBitmap(block)
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = block.displayName,
        modifier = modifier.size(size)
    )
}

private val isometricBitmapCache = HashMap<BlockType, Bitmap>()

private fun renderIsometricBlockBitmap(block: BlockType): Bitmap {
    isometricBitmapCache[block]?.let { return it }

    val iconSize = 32
    val bmp = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)

    val topPixels = TextureAtlas.getTilePixels(block.topTex)
    val sidePixels = TextureAtlas.getTilePixels(block.sideTex)
    val bottomPixels = TextureAtlas.getTilePixels(block.bottomTex)

    val pixels = IntArray(iconSize * iconSize) { 0 }

    // Isometric 3D Cube Mapping onto 32x32 bitmap:
    // 16x16 grid coordinates (u, v) from 0 to 15 mapped to isometric projection
    // Origin at cube center
    val originX = 15.5f
    val originY = 15.0f

    // 1. TOP FACE: u from 0..15, v from 0..15
    // Top face is illuminated (100% brightness, factor = 1.0)
    for (v in 0 until 16) {
        for (u in 0 until 16) {
            val color = topPixels[v * 16 + u]
            if ((color ushr 24) == 0) continue

            // Isometric projection of horizontal top plane:
            // x = originX + (u - v) * 0.82f
            // y = originY - 6.5f + (u + v) * 0.42f
            val px = (originX + (u - v) * 0.84f).roundToInt()
            val py = (originY - 7.5f + (u + v) * 0.42f).roundToInt()

            setPixelSafe(pixels, iconSize, px, py, color, 1.0f)
            setPixelSafe(pixels, iconSize, px + 1, py, color, 1.0f) // Sub-pixel fill for solid look
        }
    }

    // 2. LEFT FACE: u from 0..15, y from 0..15 (Shadowed, 60% brightness, factor = 0.60)
    for (y in 0 until 16) {
        for (u in 0 until 16) {
            val color = sidePixels[y * 16 + u]
            if ((color ushr 24) == 0) continue

            // Left side plane:
            // x goes from originX - 13.0f to originX
            // y drops down with depth
            val px = (originX - 13.0f + u * 0.84f).roundToInt()
            val py = (originY - 1.0f + u * 0.42f + y * 0.82f).roundToInt()

            setPixelSafe(pixels, iconSize, px, py, color, 0.62f)
            setPixelSafe(pixels, iconSize, px, py + 1, color, 0.62f)
        }
    }

    // 3. RIGHT FACE: u from 0..15, y from 0..15 (Diffuse, 80% brightness, factor = 0.80)
    for (y in 0 until 16) {
        for (u in 0 until 16) {
            val color = sidePixels[y * 16 + u]
            if ((color ushr 24) == 0) continue

            // Right side plane:
            // x goes from originX to originX + 13.0f
            val px = (originX + u * 0.84f).roundToInt()
            val py = (originY + 5.5f - u * 0.42f + y * 0.82f).roundToInt()

            setPixelSafe(pixels, iconSize, px, py, color, 0.82f)
            setPixelSafe(pixels, iconSize, px, py + 1, color, 0.82f)
        }
    }

    // 4. CRISP SILHOUETTE OUTLINE around the exported 3D cube
    applySilhouetteOutline(pixels, iconSize)

    bmp.setPixels(pixels, 0, iconSize, 0, 0, iconSize, iconSize)
    isometricBitmapCache[block] = bmp
    return bmp
}

private fun setPixelSafe(pixels: IntArray, size: Int, x: Int, y: Int, color: Int, lightFactor: Float) {
    if (x !in 0 until size || y !in 0 until size) return
    val a = (color ushr 24) and 0xFF
    if (a < 10) return

    val r = (((color ushr 16) and 0xFF) * lightFactor).toInt().coerceIn(0, 255)
    val g = (((color ushr 8) and 0xFF) * lightFactor).toInt().coerceIn(0, 255)
    val b = ((color and 0xFF) * lightFactor).toInt().coerceIn(0, 255)

    pixels[y * size + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
}

private fun applySilhouetteOutline(pixels: IntArray, size: Int) {
    val outlineColor = (0xD0 shl 24) or (0x10 shl 16) or (0x10 shl 8) or 0x10 // Dark translucent edge
    val toOutline = ArrayList<Int>()

    for (y in 0 until size) {
        for (x in 0 until size) {
            val idx = y * size + x
            if ((pixels[idx] ushr 24) == 0) {
                // Check if adjacent to a solid pixel
                val hasSolidNeighbor = (x > 0 && (pixels[idx - 1] ushr 24) > 100) ||
                        (x < size - 1 && (pixels[idx + 1] ushr 24) > 100) ||
                        (y > 0 && (pixels[idx - size] ushr 24) > 100) ||
                        (y < size - 1 && (pixels[idx + size] ushr 24) > 100)
                if (hasSolidNeighbor) {
                    toOutline.add(idx)
                }
            }
        }
    }

    for (idx in toOutline) {
        pixels[idx] = outlineColor
    }
}
