package dev.dokup.mediastoresample.misc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

object ImageLoader {

    private const val MAX_IMAGE_SIZE = 4096

    suspend fun fetchBitmap(context: Context, imageUri: Uri): Bitmap {
        return withContext(Dispatchers.IO) {
            loadScaledBitmapWithRotate(
                context = context,
                uri = imageUri,
                shortMax = MAX_IMAGE_SIZE,
                longMax = MAX_IMAGE_SIZE,
                isMutable = false
            ) ?: throw IOException("Bitmap is null")
        }
    }

    @Throws(IOException::class)
    private fun loadScaledBitmapWithRotate(
        context: Context,
        uri: Uri?,
        shortMax: Int,
        longMax: Int,
        isMutable: Boolean
    ): Bitmap? {
        return loadScaledBitmap(
            context = context,
            uri = uri,
            shortMax = shortMax,
            longMax = longMax,
            isMutable = isMutable
        )
    }

    @Throws(IOException::class)
    fun loadScaledBitmap(
        context: Context,
        uri: Uri?,
        shortMax: Int,
        longMax: Int,
        isMutable: Boolean
    ): Bitmap? {
        uri ?: return null
        if (shortMax <= 0 && longMax <= 0) {
            return if (isMutable) {
                loadMutableBitmap(context, uri)
            } else loadBitmap(context, uri)
        }
        return openInputStream(context, uri).use { inputStream ->
            val bounds = decodeBitmapBounds(context, uri)
            val actualWidth = bounds.width()
            val actualHeight = bounds.height()
            val maxWidth = if (actualHeight < actualWidth) longMax else shortMax
            val maxHeight = if (actualWidth < actualHeight) longMax else shortMax
            val desiredWidth = getResizedDimension(
                maxWidth,
                maxHeight,
                actualWidth,
                actualHeight
            )
            val desiredHeight = getResizedDimension(
                maxHeight,
                maxWidth,
                actualHeight,
                actualWidth
            )
            val decodeOptions = BitmapFactory.Options()
            decodeOptions.inJustDecodeBounds = false
            decodeOptions.inSampleSize = findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight)
            val tempBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            val bitmap: Bitmap?
            if (tempBitmap != null && (tempBitmap.width > desiredWidth || tempBitmap.height > desiredHeight)) {
                bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true)
                tempBitmap.recycle()
            } else {
                bitmap = tempBitmap
            }
            bitmap
        }
    }

    private fun loadMutableBitmap(
        context: Context,
        uri: Uri
    ): Bitmap? {
        return openInputStream(context, uri).use { inputStream ->
            try {
                val decodeOptions = BitmapFactory.Options()
                decodeOptions.inMutable = true
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } catch (e: FileNotFoundException) {
                throw IOException(e)
            }
        }
    }

    @Throws(IOException::class)
    private fun loadBitmap(
        context: Context,
        uri: Uri
    ): Bitmap {
        return openInputStream(context, uri).use { inputStream ->
            try {
                BitmapFactory.decodeStream(inputStream)
            } catch (e: FileNotFoundException) {
                throw IOException(e)
            }
        }
    }

    @Throws(FileNotFoundException::class)
    private fun openInputStream(context: Context, uri: Uri): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }

    private fun decodeBitmapBounds(
        context: Context,
        uri: Uri
    ): Rect {
        return openInputStream(context, uri).use { inputStream ->
            val decodeOptions = BitmapFactory.Options()
            decodeOptions.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            Rect(0, 0, decodeOptions.outWidth, decodeOptions.outHeight)
        }
    }

    private fun getResizedDimension(
        maxPrimary: Int,
        maxSecondary: Int,
        actualPrimary: Int,
        actualSecondary: Int
    ): Int {
        // If no dominant value at all, just return the actual.
        if (maxPrimary == 0 && maxSecondary == 0) {
            return actualPrimary
        }

        // If primary is unspecified, scale primary to match secondary's scaling
        // ratio.
        if (maxPrimary == 0) {
            val ratio = maxSecondary.toDouble() / actualSecondary.toDouble()
            return (actualPrimary * ratio).toInt()
        }
        if (maxSecondary == 0) {
            return maxPrimary
        }
        val ratio = actualSecondary.toDouble() / actualPrimary.toDouble()
        var resized = maxPrimary
        if (resized * ratio > maxSecondary) {
            resized = (maxSecondary / ratio).toInt()
        }
        return resized
    }

    private fun findBestSampleSize(
        actualWidth: Int,
        actualHeight: Int,
        desiredWidth: Int,
        desiredHeight: Int
    ): Int {
        val wr = actualWidth.toDouble() / desiredWidth
        val hr = actualHeight.toDouble() / desiredHeight
        val ratio = min(wr, hr)
        var n = 1.0f
        while (n * 2 <= ratio) {
            n *= 2f
        }
        return n.toInt()
    }
}
