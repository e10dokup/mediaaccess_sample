package dev.dokup.mediastoresample.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.dokup.mediastoresample.entity.ImageEntity
import dev.dokup.mediastoresample.repository.MediaRepository
import kotlinx.coroutines.launch

class ImageGridViewModel : ViewModel() {

    private val mediaRepository = MediaRepository()

    private val _imageList = MutableLiveData<List<ImageEntity>>(emptyList())
    val imageList: LiveData<List<ImageEntity>> = _imageList

    fun loadImages(context: Context) {
        viewModelScope.launch {
            runCatching {
                mediaRepository.loadImages(context)
            }.onSuccess {
                Log.d("loadImages", "success to load images")
                _imageList.value = it
            }.onFailure {
                Log.e("loadImages", "Failed to load images", it)
            }
        }
    }
}
