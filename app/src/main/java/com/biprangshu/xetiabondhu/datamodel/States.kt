package com.biprangshu.xetiabondhu.datamodel

import com.google.firebase.auth.FirebaseUser

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object SignedOut : AuthState()
    data class SignedIn(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class UpdateState {
    object Initial : UpdateState()
    object Loading : UpdateState()
    object Success : UpdateState()
    data class Error(val message: String) : UpdateState()
}