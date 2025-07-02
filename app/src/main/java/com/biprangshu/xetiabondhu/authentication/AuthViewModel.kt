package com.biprangshu.xetiabondhu.authentication

import android.app.Application
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.biprangshu.xetiabondhu.datamodel.AuthState
import com.biprangshu.xetiabondhu.datamodel.UpdateState
import com.biprangshu.xetiabondhu.repository.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val googleAuthClient: GoogleAuthClient,
    private val userPreferencesRepository: UserPreferencesRepository,
    application: Application
): AndroidViewModel(application) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Initial)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        checkAuthenticationStatus()
    }


    private fun checkAuthenticationStatus(){
        viewModelScope.launch {
            try {

                _authState.value = AuthState.Loading

                val isCurrentUserLoggedIn = userPreferencesRepository.isUserLoggedIn.first()
                Log.d("AuthViewModel", "isuserlogged in = $isCurrentUserLoggedIn")
                val currentUser = auth.currentUser

                if(isCurrentUserLoggedIn){
                    _authState.value = AuthState.SignedIn(currentUser)
                    Log.d("AuthViewModel", "User is logged in")
                }else{
                    userPreferencesRepository.setUserLoggedIn(false)
                    _authState.value = AuthState.SignedOut
                }
            }catch (e: Exception) {
                Log.e("AuthViewModel", "Error checking auth status", e)
                _authState.value = AuthState.Error("Failed to check authentication status")
            }
        }
    }

    suspend fun signInWithGoogle(): IntentSender? {
        return try {
            _authState.value = AuthState.Loading
            Log.d("AuthViewModel", "Started Google Login process")
            val intentSender = googleAuthClient.signIn()

            if(intentSender == null){
                _authState.value = AuthState.Error("Failed to Log in, could not initiate google sign in")
            }

            intentSender
        } catch (e: Exception){
            Log.e("AuthViewModel", "Google sign-in error", e)
            _authState.value = AuthState.Error(e.message ?: "Unknown error occurred")
            null
        }
    }

    fun handleGoogleSignInResult(intent: Intent){
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val result = googleAuthClient.getSignInResultFromIntent(intent)

                if (result.data != null){
                    val currentUser = auth.currentUser
                    _authState.value = AuthState.SignedIn(currentUser)

                    //saving data in datastore
                    userPreferencesRepository.setUserLoggedIn(true)
                    userPreferencesRepository.saveUser(
                        userId = result.data.userId,
                        userName = result.data.userName ?: "",
                        userEmail = currentUser?.email ?: ""
                    )

                    //todo save user to firestore
                }else{
                    _authState.value = AuthState.Error(result.errorMessage ?: "Sign in failed")
                }
            }catch (e: Exception){
                Log.e("AuthViewModel", "Error handling Google sign-in result", e)
                _authState.value = AuthState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }


    fun resetAuthState(){
        _authState.value = AuthState.Initial
    }
}