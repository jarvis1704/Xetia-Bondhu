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

    //history states
    private val _analysisHistory = MutableStateFlow<List<AnalysisResult>>(emptyList())
    val analysisHistory: StateFlow<List<AnalysisResult>> = _analysisHistory.asStateFlow()



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

                //calling ai for analysis
                val result = firebaseRepository.createAnalysis(uri)

                //result sucessfull
                _analysisState.value = AnalysisState.Success(result)

            }catch (e: Exception) {
                _analysisState.value = AnalysisState.Error(e.message ?: "Upload failed")
            }
        }
    }

    //TODO: Implement retriving analysis history in history screen

    fun resetAnalysisState(){
        _analysisState.value = AnalysisState.Idle
    }


}