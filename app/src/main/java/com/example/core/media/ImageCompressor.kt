package com.example.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressor {

    private const val MAX_DIMENSION = 1600
    private const val MAX_BYTES = 4 * 1024 * 1024

    suspend fun compressUriToDataUrl(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri)?.lowercase().orEmpty()
                if (!mime.startsWith("image/")) {
                    return@withContext Result.failure(IllegalArgumentException("Only image files are supported."))
                }

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    return@withContext Result.failure(IllegalArgumentException("Unable to read image dimensions."))
                }

                var sampleSize = 1
                val longest = max(bounds.outWidth, bounds.outHeight)
                while (longest / sampleSize > MAX_DIMENSION * 2) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                } ?: return@withContext Result.failure(IllegalStateException("Failed to decode image."))

                val scaled = scaleDownIfNeeded(decoded)
                var quality = 82
                var bytes: ByteArray
                do {
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    bytes = out.toByteArray()
                    quality -= 12
                } while (bytes.size > MAX_BYTES && quality >= 34)

                if (scaled !== decoded) decoded.recycle()
                scaled.recycle()

                if (bytes.size > MAX_BYTES) {
                    return@withContext Result.failure(IllegalArgumentException("Image exceeds maximum allowed size after compression."))
                }

                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                Result.success("data:image/jpeg;base64,$encoded")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun scaleDownIfNeeded(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longest = max(w, h)
        if (longest <= MAX_DIMENSION) return bitmap
        val ratio = MAX_DIMENSION.toFloat() / longest.toFloat()
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
