package com.biprangshu.xetiabondhu.authentication

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import com.biprangshu.xetiabondhu.R
import com.biprangshu.xetiabondhu.datamodel.SignInResult
import com.biprangshu.xetiabondhu.datamodel.UserData
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class GoogleAuthClient(
    private val context: Context,
    private val oneTapClient: SignInClient
) {
    private val auth = Firebase.auth

    suspend fun signIn():IntentSender? {
        val result = try{
            oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()
        }catch (e:Exception){
            Log.d("error",e.message.toString())
            if (e is CancellationException) throw e
            null
        }
        return result?.pendingIntent?.intentSender
    }

    suspend fun getSignInResultFromIntent(intent: Intent): SignInResult {
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(intent)
            val googleIdToken = credential.googleIdToken
            val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken,null)
            val authResult = auth.signInWithCredential(googleCredentials).await()
            val user = authResult.user

            if (authResult.additionalUserInfo?.isNewUser == true){
            }

            return SignInResult(
                data = user?.run {
                    UserData(
                        userId = uid,
                        userName = displayName,
                        userEmail = email
                    )
                },
                errorMessage = null
            )
        }catch (e:Exception){
            Log.d("error",e.message.toString())
            if (e is CancellationException) throw e
            return SignInResult(
                data = null,
                errorMessage = e.message
            )
        }
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
}