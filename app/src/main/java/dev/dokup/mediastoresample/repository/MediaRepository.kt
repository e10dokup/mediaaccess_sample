package dev.dokup.mediastoresample.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import dev.dokup.mediastoresample.entity.ImageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.*

class MediaRepository{

    companion object {
        private const val MIME_PNG = "image/png"
        private const val MIME_JPEG = "image/jpeg"
    }

    suspend fun loadImages(context: Context): List<ImageEntity> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID, // ContentProvider上におけるID
            MediaStore.Images.Media.DISPLAY_NAME, // ファイル名
            MediaStore.Images.Media.SIZE // ファイルサイズ
        )

        val contentResolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val sortOrder = "${MediaStore.Images.Media._ID} DESC"

        return withContext(Dispatchers.IO) {
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            ).use {
                if (it == null || it.count == 0) {
                    return@withContext emptyList<ImageEntity>()
                }

                val images = mutableListOf<ImageEntity>()

                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val displayNameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val displayName = it.getString(displayNameColumn)
                    val size = it.getInt(sizeColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    images += ImageEntity(
                        uri = contentUri,
                        name = displayName,
                        size = size
                    )
                }

                return@withContext images.toList()
            }
        }
    }

    suspend fun getMimeType(
        context: Context,
        uri: Uri,
    ): String {
        val contentResolver = context.contentResolver

        val projection = arrayOf(MediaStore.Images.Media.MIME_TYPE)

        return withContext(Dispatchers.IO) {
            contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            ).use {
                if (it == null || it.count == 0) {
                    throw IllegalStateException("Failed to find target media file")
                }
                it.moveToFirst()
                val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                return@withContext it.getString(mimeTypeColumn)
            }
        }
    }

    suspend fun saveToDir(
        context: Context,
        bitmap: Bitmap,
        mimeType: String
    ): Uri {
        val format = when (mimeType) {
            MIME_JPEG -> ImageFileFormat(
                "${createRandomFileName()}.jpg",
                Bitmap.CompressFormat.JPEG
            )
            MIME_PNG -> ImageFileFormat(
                "${createRandomFileName()}.png",
                Bitmap.CompressFormat.PNG
            )
            else -> throw IllegalArgumentException("MimeType must be image/jpeg or image/png")
        }

        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveFile(context, bitmap, format, mimeType)
            } else {
                saveFileLegacy(context, bitmap, format)
            }
        }
    }

    private fun saveFileLegacy(
        context: Context,
        bitmap: Bitmap,
        format: ImageFileFormat
    ): Uri {
        val externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(externalFilesDir, format.fileName)
        val uri = Uri.fromFile(imageFile)

        val result = compressBitmap(
            context = context,
            bitmap = bitmap,
            uri = uri,
            format = format.compressFormat
        )

        if (result) {
            val path = arrayOf(uri.toString())
            MediaScannerConnection.scanFile(
                context,
                path,
                null
            ) { resultPath, resultUri ->
                Log.d("cropscan", "Success to scan: $resultPath as $resultUri")
            }
            return Uri.fromFile(imageFile)
        } else {
            throw IOException("Failed file saving: $uri")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveFile(
        context: Context,
        bitmap: Bitmap,
        format: ImageFileFormat,
        mimeType: String
    ): Uri {
        val contentResolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, format.fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = contentResolver.insert(collection, values)!!

        compressBitmap(
            context = context,
            bitmap = bitmap,
            uri = uri,
            format = format.compressFormat
        )

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        contentResolver.update(uri, values, null, null)
        return uri
    }



//    private fun insertToMediaStore(context: Context, uri: Uri): Uri {
//        val contentResolver = context.contentResolver
//        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//        } else {
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//        }
//
//        val newImageDetails = ContentValues().apply {
//            put(MediaStore.Images.Media.DISPLAY_NAME, "My Song.mp3")
//        }
//    }

    private fun createRandomFileName(): String {
        val date = SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(Date())
        return "IMG_$date"
    }

    private fun compressBitmap(
        context: Context,
        bitmap: Bitmap?,
        uri: Uri?,
        format: Bitmap.CompressFormat?
    ): Boolean {
        bitmap ?: return false
        uri ?: return false
        val bufferSize = 8192
        return context.contentResolver.openOutputStream(uri).use { outputStream ->
            BufferedOutputStream(outputStream, bufferSize).use { os ->
                try {
                    bitmap.compress(format, 100, os)
                    true
                } catch (e: FileNotFoundException) {
                    Log.e("saveToFile", "Not found target file", e)
                    false
                }
            }
        }
    }

    internal class ImageFileFormat(val fileName: String, val compressFormat: Bitmap.CompressFormat)
}
