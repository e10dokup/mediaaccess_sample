package dev.dokup.mediastoresample.viewmodel

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.dokup.mediastoresample.entity.ImageEntity
import dev.dokup.mediastoresample.misc.ImageLoader
import dev.dokup.mediastoresample.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class ImageCropViewModel : ViewModel() {

    private val repository = MediaRepository()

    private val _uri = MutableLiveData(Uri.EMPTY)
    val uri: LiveData<Uri> = _uri

    private val _bitmap = MutableLiveData<Bitmap?>(null)
    val bitmap: LiveData<Bitmap?> = _bitmap

    private val _croppedUri = MutableLiveData(Uri.EMPTY)
    val croppedUri: LiveData<Uri> = _croppedUri

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    fun setCropSpec(
        context: Context,
        imageUri: Uri
    ) {
        _uri.value = imageUri
        _isLoading.value = true

        viewModelScope.launch {
            runCatching {
                ImageLoader.fetchBitmap(context, imageUri)
            }
                .onSuccess { bitmap ->
                    _bitmap.value = bitmap
                    _isLoading.value = false
                }
                .onFailure {
                    Log.e("setCropSpec", "Failed to load bitmap", it)
                    _isLoading.value = false
                }
        }
    }

    fun saveCroppedBitmap(context: Context, bitmap: Bitmap) {
        val originalUri = this.uri.value ?: return

        _isSaving.value = true

        viewModelScope.launch {
            runCatching {
                val mimeType = async { repository.getMimeType(context, originalUri) }
                repository.saveToDir(
                    context = context,
                    bitmap = bitmap,
                    mimeType = mimeType.await()
                )
            }
                .onSuccess { uri ->
                    _croppedUri.value = uri
                    _isSaving.value = false
                }
                .onFailure {
                    Log.e("saveCroppedBitmap", "Failed to save cropped bitmap", it)
                    _isSaving.value = false
                }
        }
    }
}
