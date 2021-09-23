package dev.dokup.mediastoresample.entity

import android.net.Uri

data class ImageEntity(
    val uri: Uri,
    val name: String,
    val size: Int
)
