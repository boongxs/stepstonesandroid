package com.flutter.stepstonesflt.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.cos
import kotlin.math.sqrt

object PHashUtil {

    fun compute(bitmap: Bitmap): String {
        val size = 32
        val small = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val pixels = FloatArray(size * size) { i ->
            val c = small.getPixel(i % size, i / size)
            0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)
        }
        small.recycle()

        val dct = dct2d(pixels, size)

        // Top-left 8×8, skip DC at [0,0]
        val vals = FloatArray(64) { i -> dct[(i / 8) * size + (i % 8)] }
        val mean = vals.average().toFloat()

        var hash = 0L
        for (i in 0 until 64) {
            if (vals[i] > mean) hash = hash or (1L shl i)
        }
        return hash.toULong().toString(16).padStart(16, '0')
    }

    fun hammingDistance(a: String, b: String): Int {
        var dist = 0
        for (i in a.indices step 2) {
            dist += Integer.bitCount(
                a.substring(i, i + 2).toInt(16) xor b.substring(i, i + 2).toInt(16)
            )
        }
        return dist
    }

    private fun dct2d(pixels: FloatArray, n: Int): FloatArray {
        val tmp = FloatArray(n * n)
        for (row in 0 until n) dct1d(pixels, tmp, row * n, n)
        val out = FloatArray(n * n)
        val col = FloatArray(n)
        val colOut = FloatArray(n)
        for (c in 0 until n) {
            for (r in 0 until n) col[r] = tmp[r * n + c]
            dct1d(col, colOut, 0, n)
            for (r in 0 until n) out[r * n + c] = colOut[r]
        }
        return out
    }

    private fun dct1d(input: FloatArray, output: FloatArray, offset: Int, n: Int) {
        val factor = Math.PI / n
        for (k in 0 until n) {
            var sum = 0.0
            for (i in 0 until n) sum += input[offset + i] * cos(factor * (i + 0.5) * k)
            val scale = if (k == 0) sqrt(1.0 / n) else sqrt(2.0 / n)
            output[offset + k] = (sum * scale).toFloat()
        }
    }
}
