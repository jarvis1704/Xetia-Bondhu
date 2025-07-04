package com.biprangshu.xetiabondhu

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.biprangshu.xetiabondhu.datamodel.AnalysisResult
import com.biprangshu.xetiabondhu.datamodel.AnalysisState
import com.biprangshu.xetiabondhu.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AppViewmodel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    application: Application
): AndroidViewModel(application) {


    //analysis states
    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()


//    fun uploadImageToStorage(uri: Uri){
//        viewModelScope.launch {
//            firebaseRepository.uploadImageToStorage(uri)
//        }
//    }
//
//    fun getRequestId(id: UUID?){
//        firebaseRepository.updateRequestId(id)
//    }

    fun analysisImage(uri: Uri){
        viewModelScope.launch {

            _analysisState.value = AnalysisState.Loading

            try {
                //generating a fresh analysis request
                val requestId = firebaseRepository.createAnalysisAndUpload(uri)

                //listening to the cloud function to update firestore
                firebaseRepository.documentUpdateListener(requestId){
                    snapshot->
                    val status = snapshot.getString("status")
                    when(status){
                        "complete" -> {
                            val map = snapshot.get("result") as Map<*, *>
                            val result = AnalysisResult(
                                diseaseName = map["diseaseName"] as String,
                                description = map["description"] as String,
                                solution = map["solution"] as String,
                            )
                            //analysis complete
                            _analysisState.value = AnalysisState.Success(result)
                        }
                        "error" -> {
                            val error = snapshot.getString("error") ?: "Unknown Error"
                            _analysisState.value = AnalysisState.Error(error)
                        }
                    }

                }
            }catch (e: Exception) {
                _analysisState.value = AnalysisState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun resetAnalysisState(){
        _analysisState.value = AnalysisState.Idle
    }


}