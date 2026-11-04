package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.game.engine.BlockType

/**
 * Renders an authentic Minecraft-style 3D isometric voxel block icon.
 * Features illuminated Top face (100% brightness), shaded Left face (60% brightness),
 * and diffuse Right face (80% brightness) with characteristic block textures and bevels.
 */
@Composable
fun IsometricBlockIcon(
    block: BlockType,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()

        // Isometric 3D cube geometry centered in canvas
        val centerX = w * 0.5f
        val centerY = h * 0.48f
        val radiusX = w * 0.38f
        val radiusY = h * 0.22f
        val cubeHeight = h * 0.40f

        // 7 key isometric vertices:
        // C: Center top vertex (closest to viewer in top face)
        val pCenter = Offset(centerX, centerY)
        // N: North vertex (far top)
        val pNorth = Offset(centerX, centerY - radiusY)
        // W: West vertex (far left top)
        val pWest = Offset(centerX - radiusX, centerY - radiusY * 0.5f)
        // E: East vertex (far right top)
        val pEast = Offset(centerX + radiusX, centerY - radiusY * 0.5f)
        // SW: South-West vertex (bottom left)
        val pBottomLeft = Offset(centerX - radiusX, centerY - radiusY * 0.5f + cubeHeight)
        // S: South vertex (bottom center)
        val pBottomCenter = Offset(centerX, centerY + cubeHeight)
        // SE: South-East vertex (bottom right)
        val pBottomRight = Offset(centerX + radiusX, centerY - radiusY * 0.5f + cubeHeight)

        val (topColor, leftColor, rightColor) = getBlockIsometricColors(block)

        // 1. LEFT FACE (Shadowed, ~60% brightness)
        val leftPath = Path().apply {
            moveTo(pCenter.x, pCenter.y)
            lineTo(pWest.x, pWest.y)
            lineTo(pBottomLeft.x, pBottomLeft.y)
            lineTo(pBottomCenter.x, pBottomCenter.y)
            close()
        }
        drawPath(leftPath, color = leftColor)
        drawFaceDetails(block, isSide = true, isLeft = true, pCenter, pWest, pBottomLeft, pBottomCenter)

        // 2. RIGHT FACE (Diffuse, ~80% brightness)
        val rightPath = Path().apply {
            moveTo(pCenter.x, pCenter.y)
            lineTo(pEast.x, pEast.y)
            lineTo(pBottomRight.x, pBottomRight.y)
            lineTo(pBottomCenter.x, pBottomCenter.y)
            close()
        }
        drawPath(rightPath, color = rightColor)
        drawFaceDetails(block, isSide = true, isLeft = false, pCenter, pEast, pBottomRight, pBottomCenter)

        // 3. TOP FACE (Illuminated, 100% brightness)
        val topPath = Path().apply {
            moveTo(pNorth.x, pNorth.y)
            lineTo(pEast.x, pEast.y)
            lineTo(pCenter.x, pCenter.y)
            lineTo(pWest.x, pWest.y)
            close()
        }
        drawPath(topPath, color = topColor)
        drawTopFaceDetails(block, pNorth, pEast, pCenter, pWest)

        // 4. CRISP SILHOUETTE OUTLINE
        val outlineColor = Color(0xFF181818).copy(alpha = 0.85f)
        val strokeW = 1.5f

        // Outer contour
        drawLine(outlineColor, pNorth, pEast, strokeW)
        drawLine(outlineColor, pEast, pBottomRight, strokeW)
        drawLine(outlineColor, pBottomRight, pBottomCenter, strokeW)
        drawLine(outlineColor, pBottomCenter, pBottomLeft, strokeW)
        drawLine(outlineColor, pBottomLeft, pWest, strokeW)
        drawLine(outlineColor, pWest, pNorth, strokeW)

        // Inner 3 central edges meeting at pCenter
        val innerEdgeColor = Color(0xFF000000).copy(alpha = 0.35f)
        drawLine(innerEdgeColor, pCenter, pNorth, strokeW)
        drawLine(innerEdgeColor, pCenter, pBottomCenter, strokeW)
        drawLine(innerEdgeColor, pCenter, pWest, strokeW)
        drawLine(innerEdgeColor, pCenter, pEast, strokeW)
    }
}

/**
 * Face detail decorations for authentic block identification
 */
private fun DrawScope.drawFaceDetails(
    block: BlockType,
    isSide: Boolean,
    isLeft: Boolean,
    pTopCenter: Offset,
    pTopOuter: Offset,
    pBottomOuter: Offset,
    pBottomCenter: Offset
) {
    val hVec = Offset(pBottomCenter.x - pTopCenter.x, pBottomCenter.y - pTopCenter.y)
    val wVec = Offset(pTopOuter.x - pTopCenter.x, pTopOuter.y - pTopCenter.y)

    when (block) {
        BlockType.GRASS -> {
            // Serrated natural grass fringe dripping onto dirt
            val grassSideColor = if (isLeft) Color(0xFF4C7C23) else Color(0xFF5B8C33)
            val darkGrassColor = if (isLeft) Color(0xFF385E19) else Color(0xFF477025)

            val p1 = Offset(pTopCenter.x + wVec.x * 0.25f, pTopCenter.y + wVec.y * 0.25f + hVec.y * 0.40f)
            val p2 = Offset(pTopCenter.x + wVec.x * 0.50f, pTopCenter.y + wVec.y * 0.50f + hVec.y * 0.25f)
            val p3 = Offset(pTopCenter.x + wVec.x * 0.75f, pTopCenter.y + wVec.y * 0.75f + hVec.y * 0.45f)

            val grassPath = Path().apply {
                moveTo(pTopCenter.x, pTopCenter.y)
                lineTo(pTopOuter.x, pTopOuter.y)
                lineTo(pTopOuter.x, pTopOuter.y + hVec.y * 0.30f)
                lineTo(p3.x, p3.y)
                lineTo(p2.x, p2.y)
                lineTo(p1.x, p1.y)
                lineTo(pTopCenter.x, pTopCenter.y + hVec.y * 0.32f)
                close()
            }
            drawPath(grassPath, grassSideColor)

            // Drip tooth
            drawCircle(darkGrassColor, radius = 1.2f, center = p3)
        }
        BlockType.COBBLESTONE -> {
            // Irregular cobblestone cracks and mortar lines
            val crackColor = Color(0xFF2A2A2A).copy(alpha = 0.55f)
            val highlightColor = Color(0xFFAAAAAA).copy(alpha = 0.45f)

            val m1 = Offset(pTopCenter.x + wVec.x * 0.5f, pTopCenter.y + wVec.y * 0.5f)
            val m2 = Offset(pBottomCenter.x + wVec.x * 0.5f, pBottomCenter.y + wVec.y * 0.5f)
            val hMid1 = Offset(pTopCenter.x + hVec.x * 0.45f, pTopCenter.y + hVec.y * 0.45f)
            val hMid2 = Offset(pTopOuter.x + hVec.x * 0.55f, pTopOuter.y + hVec.y * 0.55f)

            drawLine(crackColor, m1, m2, 1.2f)
            drawLine(crackColor, hMid1, hMid2, 1.2f)
            drawCircle(highlightColor, radius = 1.5f, center = Offset(pTopCenter.x + wVec.x * 0.3f + hVec.x * 0.3f, pTopCenter.y + wVec.y * 0.3f + hVec.y * 0.3f))
        }
        BlockType.OAK_WOOD -> {
            // Vertical tree bark ridges
            val barkDark = if (isLeft) Color(0xFF332010) else Color(0xFF452C16)
            val barkLight = if (isLeft) Color(0xFF553D25) else Color(0xFF704E2D)

            for (frac in listOf(0.33f, 0.67f)) {
                val topPt = Offset(pTopCenter.x + wVec.x * frac, pTopCenter.y + wVec.y * frac)
                val botPt = Offset(pBottomCenter.x + wVec.x * frac, pBottomCenter.y + wVec.y * frac)
                drawLine(barkDark, topPt, botPt, 1.5f)
                drawLine(barkLight, Offset(topPt.x + 1f, topPt.y), Offset(botPt.x + 1f, botPt.y), 1.0f)
            }
        }
        BlockType.WOOD_PLANKS -> {
            // Horizontal wooden planks with nail/seam lines
            val seamColor = Color(0xFF3D2714).copy(alpha = 0.65f)
            val lightSeam = Color(0xFFC79E69).copy(alpha = 0.35f)

            for (frac in listOf(0.33f, 0.66f)) {
                val start = Offset(pTopCenter.x + hVec.x * frac, pTopCenter.y + hVec.y * frac)
                val end = Offset(pTopOuter.x + hVec.x * frac, pTopOuter.y + hVec.y * frac)
                drawLine(seamColor, start, end, 1.5f)
                drawLine(lightSeam, Offset(start.x, start.y + 1f), Offset(end.x, end.y + 1f), 1.0f)
            }
        }
        BlockType.BRICK -> {
            // Running-bond brick pattern
            val mortarColor = Color(0xFFDED6C8).copy(alpha = 0.85f)
            for (vFrac in listOf(0.33f, 0.66f)) {
                val start = Offset(pTopCenter.x + hVec.x * vFrac, pTopCenter.y + hVec.y * vFrac)
                val end = Offset(pTopOuter.x + hVec.x * vFrac, pTopOuter.y + hVec.y * vFrac)
                drawLine(mortarColor, start, end, 1.2f)
            }
            // Vertical header seams
            val s1 = Offset(pTopCenter.x + wVec.x * 0.5f, pTopCenter.y + wVec.y * 0.5f)
            val e1 = Offset(s1.x + hVec.x * 0.33f, s1.y + hVec.y * 0.33f)
            drawLine(mortarColor, s1, e1, 1.2f)
        }
        BlockType.GLASS -> {
            // Translucent glass with bright diagonal reflection
            val glintColor = Color.White.copy(alpha = 0.8f)
            val g1 = Offset(pTopCenter.x + wVec.x * 0.2f + hVec.x * 0.2f, pTopCenter.y + wVec.y * 0.2f + hVec.y * 0.2f)
            val g2 = Offset(pTopCenter.x + wVec.x * 0.6f + hVec.x * 0.6f, pTopCenter.y + wVec.y * 0.6f + hVec.y * 0.6f)
            drawLine(glintColor, g1, g2, 1.8f)
        }
        BlockType.GLOWSTONE -> {
            // Bright amber glowstone crystal cluster
            val goldSpeck = Color(0xFFFFFAAA)
            val center = Offset(pTopCenter.x + wVec.x * 0.5f + hVec.x * 0.5f, pTopCenter.y + wVec.y * 0.5f + hVec.y * 0.5f)
            drawCircle(goldSpeck, radius = 2.0f, center = center)
        }
        BlockType.BOOKSHELF -> {
            // Shelf horizontal divider
            val shelfDark = Color(0xFF4A3015)
            val midStart = Offset(pTopCenter.x + hVec.x * 0.5f, pTopCenter.y + hVec.y * 0.5f)
            val midEnd = Offset(pTopOuter.x + hVec.x * 0.5f, pTopOuter.y + hVec.y * 0.5f)
            drawLine(shelfDark, midStart, midEnd, 2f)

            // Book spines
            val bookColors = listOf(Color(0xFFB33030), Color(0xFF285AB8), Color(0xFF2E8540), Color(0xFFD49B28))
            for (i in 0 until 4) {
                val frac = 0.15f + i * 0.22f
                val spineTop = Offset(pTopCenter.x + wVec.x * frac + hVec.x * 0.08f, pTopCenter.y + wVec.y * frac + hVec.y * 0.08f)
                val spineBot = Offset(pTopCenter.x + wVec.x * frac + hVec.x * 0.44f, pTopCenter.y + wVec.y * frac + hVec.y * 0.44f)
                drawLine(bookColors[i % bookColors.size], spineTop, spineBot, 2.0f)
            }
        }
        else -> {}
    }
}

private fun DrawScope.drawTopFaceDetails(
    block: BlockType,
    pNorth: Offset, pEast: Offset, pCenter: Offset, pWest: Offset
) {
    val midX = (pNorth.x + pEast.x + pCenter.x + pWest.x) * 0.25f
    val midY = (pNorth.y + pEast.y + pCenter.y + pWest.y) * 0.25f
    val center = Offset(midX, midY)

    when (block) {
        BlockType.OAK_WOOD -> {
            // Concentric tree growth rings on log top
            drawCircle(Color(0xFF805E32), radius = 4.2f, center = center)
            drawCircle(Color(0xFF9E7743), radius = 3.0f, center = center)
            drawCircle(Color(0xFF6B4823), radius = 1.4f, center = center)
        }
        BlockType.GRASS -> {
            // Dappled vibrant top foliage specks
            drawCircle(Color(0xFF74B83B), radius = 1.4f, center = Offset(midX - 3f, midY - 2f))
            drawCircle(Color(0xFF528726), radius = 1.2f, center = Offset(midX + 3f, midY + 1f))
        }
        BlockType.GLOWSTONE -> {
            // Hot crystal center
            drawCircle(Color(0xFFFFFFD0), radius = 2.5f, center = center)
        }
        BlockType.BRICK -> {
            // Top mortar line
            drawLine(Color(0xFFDED6C8).copy(alpha = 0.75f), Offset((pNorth.x + pWest.x) * 0.5f, (pNorth.y + pWest.y) * 0.5f), Offset((pEast.x + pCenter.x) * 0.5f, (pEast.y + pCenter.y) * 0.5f), 1.2f)
        }
        else -> {}
    }
}

private data class IsometricColors(val top: Color, val left: Color, val right: Color)

private fun getBlockIsometricColors(block: BlockType): IsometricColors {
    return when (block) {
        BlockType.GRASS -> IsometricColors(
            top = Color(0xFF62A431),   // Lush green top
            left = Color(0xFF6B4B31),  // Dirt left
            right = Color(0xFF866043)  // Dirt right
        )
        BlockType.DIRT -> IsometricColors(
            top = Color(0xFF966D4C),
            left = Color(0xFF6B4B31),
            right = Color(0xFF866043)
        )
        BlockType.STONE -> IsometricColors(
            top = Color(0xFF8C8C8C),
            left = Color(0xFF5A5A5A),
            right = Color(0xFF737373)
        )
        BlockType.COBBLESTONE -> IsometricColors(
            top = Color(0xFF888888),
            left = Color(0xFF525252),
            right = Color(0xFF6E6E6E)
        )
        BlockType.OAK_WOOD -> IsometricColors(
            top = Color(0xFFA68550),   // Tree rings top
            left = Color(0xFF4A341C),  // Dark bark
            right = Color(0xFF684B29)  // Medium bark
        )
        BlockType.OAK_LEAVES -> IsometricColors(
            top = Color(0xFF4A8022),
            left = Color(0xFF2C5012),
            right = Color(0xFF386618)
        )
        BlockType.WOOD_PLANKS -> IsometricColors(
            top = Color(0xFFB58E59),
            left = Color(0xFF7D5B32),
            right = Color(0xFFA17C4D)
        )
        BlockType.BRICK -> IsometricColors(
            top = Color(0xFFB05144),
            left = Color(0xFF782E24),
            right = Color(0xFF9C4336)
        )
        BlockType.SAND -> IsometricColors(
            top = Color(0xFFE5DDAF),
            left = Color(0xFFBFB078),
            right = Color(0xFFDBD3A0)
        )
        BlockType.GLASS -> IsometricColors(
            top = Color(0x66D8EEFF),
            left = Color(0x77A8D4F8),
            right = Color(0x66B8DEFB)
        )
        BlockType.WATER -> IsometricColors(
            top = Color(0xB03C6FF0),
            left = Color(0xC01C44C2),
            right = Color(0xB52E5CE6)
        )
        BlockType.GLOWSTONE -> IsometricColors(
            top = Color(0xFFFFF099),
            left = Color(0xFFD4A030),
            right = Color(0xFFF0BC40)
        )
        BlockType.BEDROCK -> IsometricColors(
            top = Color(0xFF484848),
            left = Color(0xFF202020),
            right = Color(0xFF303030)
        )
        BlockType.BOOKSHELF -> IsometricColors(
            top = Color(0xFFB58E59),   // Planks top
            left = Color(0xFF7A4A28),  // Shelves left
            right = Color(0xFF8A5A36)  // Shelves right
        )
        else -> IsometricColors(Color.LightGray, Color.Gray, Color.DarkGray)
    }
}
