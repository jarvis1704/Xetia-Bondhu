package com.biprangshu.xetiabondhu.repository

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.biprangshu.xetiabondhu.datamodel.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class FirebaseRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val firebaseStorage: FirebaseStorage
){

    private var _requestId by mutableStateOf<UUID?>(null)

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

//    fun updateRequestId(id: UUID?){
//        _requestId = id
//    }

//    suspend fun uploadImageToStorage(uri: Uri){
//        try {
//            val storageRef = firebaseStorage.reference
//            val user = auth.currentUser
//            user?.let {
//                //path for image
//                val path = "uploads/${user.uid}/$_requestId.jpg"
//
//                val imageRef = storageRef.child(path)
//
//                //uploading the file
//                val uploadImage = imageRef.putFile(uri).await()
//
//                Log.d("Firebase Repository", "Image Uploaded sucessfully $uploadImage")
//            }
//        }catch (e: Exception){
//            Log.e("Firebase Repostory", "Error in uploading image to storage")
//        }
//    }

    suspend fun createAnalysisAndUpload(uri: Uri): String{

        val user = auth.currentUser ?: throw IllegalStateException("Not Signed In")
        val requestId = UUID.randomUUID().toString()
        val currentRequest = mapOf(
            "userId" to user.uid,
            "status" to "processing",
            "createdAt" to FieldValue.serverTimestamp()
        )

        //write job for processing
        db.collection("analysis_requests")
            .document(requestId)
            .set(currentRequest)
            .await()

        val path = "uploads/${user.uid}/$requestId.jpg"
        //upload image to storage
        firebaseStorage.reference.child(path).putFile(uri).await()

        return requestId
    }

    //document update listener
    fun documentUpdateListener(requestId: String, onChange: (DocumentSnapshot) -> Unit){
        db.collection("analysis_requests")
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if(error!=null || snapshot==null) return@addSnapshotListener
                if (snapshot.exists()) onChange(snapshot)
            }
    }


}