package com.biprangshu.xetiabondhu

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.biprangshu.xetiabondhu.appui.HistoryItem
import com.biprangshu.xetiabondhu.datamodel.AnalysisResult
import com.biprangshu.xetiabondhu.datamodel.AnalysisState
import com.biprangshu.xetiabondhu.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
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
    private val auth: FirebaseAuth,
    application: Application
): AndroidViewModel(application) {


    //analysis states
    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    //history states
    private val _analysisHistory = MutableStateFlow<List<HistoryItem>>(emptyList())
    val analysisHistory: StateFlow<List<HistoryItem>> = _analysisHistory.asStateFlow()

    //history loading animation
    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()



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

                loadAnalysisHistory()

            }catch (e: Exception) {
                _analysisState.value = AnalysisState.Error(e.message ?: "Upload failed")
            }
        }
    }


    fun loadAnalysisHistory(){

        viewModelScope.launch {
            _isHistoryLoading.value = true
            try {
                val user = auth.currentUser
                user?.let {
                    val historyData = firebaseRepository.getAnalysisHistory(it.uid)
                    Log.d("AppViewModel", "Loaded ${historyData.size} history items")
                    _analysisHistory.value = historyData
                }
            }catch (e: Exception){
                _analysisHistory.value = emptyList()
            }finally {
                _isHistoryLoading.value=false
            }
        }
    }

    fun resetAnalysisState(){
        _analysisState.value = AnalysisState.Idle
    }


}