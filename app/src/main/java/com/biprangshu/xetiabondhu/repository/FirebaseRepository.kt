package com.biprangshu.xetiabondhu.repository

import android.util.Log
import com.biprangshu.xetiabondhu.datamodel.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

class FirebaseRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
){

    suspend fun saveUserToFireStore(user: FirebaseUser){
        val userData = UserData(
            userId = user.uid,
            userName = user.displayName,
            userEmail = user.email
        )

        Log.d("Firebase Repostitory", "Saving user to firestore")

        db.collection("users").document(user.uid).set(userData)
            .addOnSuccessListener {
            Log.d("Firebase Repostory", "Saved user to firestore")
        }.addOnFailureListener {
                e->
                Log.e("Firebase Repostory", "failed to store data in firestore", e)
            }
    }

}