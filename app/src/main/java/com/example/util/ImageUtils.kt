package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {

    /**
     * Converts an image Uri to a compressed Base64 String suitable for Firestore storage.
     * Automatically handles EXIF rotation and downsizes the image to [maxDimension] to keep
     * payload lightweight (< 60 KB).
     */
    fun uriToBase64(context: Context, uri: Uri, maxDimension: Int = 300, quality: Int = 80): String? {
        return try {
            val contentResolver = context.contentResolver
            
            // 1. Check EXIF orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    }
                }
            } catch (e: Throwable) {
                Log.w("ImageUtils", "Could not read EXIF orientation: ${e.message}")
            }

            // 2. Decode bitmap with downsampling
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val originalBitmap: Bitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 3. Apply EXIF rotation if necessary
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            val rotatedBitmap = if (!matrix.isIdentity) {
                Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
            } else {
                originalBitmap
            }

            // 4. Exact scale to maxDimension if needed
            val width = rotatedBitmap.width
            val height = rotatedBitmap.height
            val scale = if (width > height) {
                maxDimension.toFloat() / width
            } else {
                maxDimension.toFloat() / height
            }

            val finalBitmap = if (scale < 1.0f) {
                val targetW = (width * scale).toInt().coerceAtLeast(1)
                val targetH = (height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(rotatedBitmap, targetW, targetH, true)
            } else {
                rotatedBitmap
            }

            // 5. Compress to JPEG and encode to Base64
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            outputStream.close()

            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Throwable) {
            Log.e("ImageUtils", "Failed to convert Uri to Base64: ${e.message}", e)
            null
        }
    }

    /**
     * Decodes a Base64 string (with or without data URI prefix) into a Bitmap.
     */
    fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            if (base64String.isBlank()) return null
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Throwable) {
            Log.w("ImageUtils", "Failed to decode base64 bitmap: ${e.message}")
            null
        }
    }
}
