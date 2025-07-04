package com.biprangshu.xetiabondhu

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.biprangshu.xetiabondhu.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AppViewmodel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    application: Application
): AndroidViewModel(application) {


    fun uploadImageToStorage(uri: Uri){
        viewModelScope.launch {
            firebaseRepository.uploadImageToStorage(uri)
        }
    }

    fun getRequestId(id: UUID?){
        firebaseRepository.updateRequestId(id)
    }

}