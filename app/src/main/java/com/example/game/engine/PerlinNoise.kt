package com.example.game.engine

import kotlin.math.floor

/**
 * 2D & 3D Perlin coherent gradient noise generator for procedural Minecraft terrain.
 */
class PerlinNoise(seed: Long = 1337L) {

    private val p = IntArray(512)

    init {
        val permutation = IntArray(256) { it }
        // Shuffle using seed
        var s = seed
        for (i in 255 downTo 1) {
            s = s * 6364136223846793005L + 1442695040888963407L
            val j = ((s ushr 32) and 0x7FFFFFFFL % (i + 1)).toInt()
            val temp = permutation[i]
            permutation[i] = permutation[j]
            permutation[j] = temp
        }
        for (i in 0 until 256) {
            p[i] = permutation[i]
            p[256 + i] = permutation[i]
        }
    }

    private fun fade(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)

    private fun lerp(t: Float, a: Float, b: Float): Float = a + t * (b - a)

    private fun grad(hash: Int, x: Float, y: Float): Float {
        val h = hash and 7
        val u = if (h < 4) x else y
        val v = if (h < 4) y else x
        return (if ((h and 1) == 0) u else -u) + (if ((h and 2) == 0) v else -v)
    }

    private fun grad3D(hash: Int, x: Float, y: Float, z: Float): Float {
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = if (h < 4) y else if (h == 12 || h == 14) x else z
        return (if ((h and 1) == 0) u else -u) + (if ((h and 2) == 0) v else -v)
    }

    fun noise2D(x: Float, y: Float): Float {
        val X = floor(x).toInt() and 255
        val Y = floor(y).toInt() and 255

        val xf = x - floor(x)
        val yf = y - floor(y)

        val u = fade(xf)
        val v = fade(yf)

        val a = p[X] + Y
        val aa = p[a]
        val ab = p[a + 1]
        val b = p[X + 1] + Y
        val ba = p[b]
        val bb = p[b + 1]

        return lerp(
            v,
            lerp(u, grad(p[aa], xf, yf), grad(p[ba], xf - 1f, yf)),
            lerp(u, grad(p[ab], xf, yf - 1f), grad(p[bb], xf - 1f, yf - 1f))
        )
    }

    /**
     * Multi-octave fractal brownian motion for realistic terrain height.
     */
    fun octaveNoise2D(x: Float, y: Float, octaves: Int, persistence: Float = 0.5f, lacunarity: Float = 2.0f): Float {
        var total = 0f
        var frequency = 1f
        var amplitude = 1f
        var maxValue = 0f

        for (i in 0 until octaves) {
            total += noise2D(x * frequency, y * frequency) * amplitude
            maxValue += amplitude
            amplitude *= persistence
            frequency *= lacunarity
        }

        return total / maxValue
    }
}
