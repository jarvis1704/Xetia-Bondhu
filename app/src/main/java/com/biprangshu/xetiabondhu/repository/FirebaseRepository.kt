package com.biprangshu.xetiabondhu.repository

import android.util.Log
import com.biprangshu.xetiabondhu.datamodel.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject

class FirebaseRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val firebaseStorage: FirebaseStorage
){

    suspend fun saveUserToFireStore(user: FirebaseUser){
        val userData = UserData(
            userId = user.uid,
            userName = user.displayName,
            userEmail = user.email
        )

        Log.d("Firebase Repostitory", "Saving user to firestore")

        db.collection("users").document(user.uid).set(userData, SetOptions.merge())
            .addOnSuccessListener {
            Log.d("Firebase Repostory", "Saved user to firestore")
        }.addOnFailureListener {
                e->
                Log.e("Firebase Repostory", "failed to store data in firestore", e)
            }
    }

}