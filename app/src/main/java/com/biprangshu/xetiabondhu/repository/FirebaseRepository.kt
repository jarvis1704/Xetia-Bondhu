package com.biprangshu.xetiabondhu.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.decodeBitmap
import com.biprangshu.xetiabondhu.datamodel.AnalysisResult
import com.biprangshu.xetiabondhu.datamodel.UserData
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class FirebaseRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val firebaseStorage: FirebaseStorage,
    private val context: Context
){

    private var _requestId by mutableStateOf<UUID?>(null)

    val generativeModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).generativeModel(
        modelName = "gemini-2.5-flash",
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 1024
        }
    )



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

    suspend fun createAnalysis(uri: Uri): AnalysisResult{

        val user = auth.currentUser ?: throw IllegalStateException("Not Signed In")
        try {
            Log.d("Firebase Repository", "Starting AI analysis for image")

            //prompt for analysis
            val prompt = """
                You are an expert in agricultural science specializing in tea and paddy (rice) crops common in Assam, India.
                Analyze this image of a plant leaf.
                
                Please provide:
                1. Identify the most likely disease or pest affecting the plant
                2. Provide a brief, simple description of the issue in 2-3 sentences
                3. Suggest a practical, actionable solution or remedy for a local farmer in Assam
                
                Important: If the image is not a plant leaf, is unclear, or you cannot identify any issue, 
                set the disease name to "Healthy" or "Unknown" and provide appropriate guidance.
                
                Provide your response in a structured format with clear sections for disease name, description, and solution.
                Keep the language simple and practical for local farmers.
            """.trimIndent()

            val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            //creating context for ai
            val inputContext = content {
                text(prompt)
                image(bitmap)
            }

            Log.d("Firebase Repository", "Sending request to Gemini API")

            //generate result using AI
            val response = generativeModel.generateContent(inputContext)
            //response
            val responseText = response.text ?: throw Exception("Empty response from AI")

            Log.d("Firebase Repository", "Received response from AI: $responseText")

            //parsing to get structured data
            val analysisResult = parseAiResponse(responseText)

            //save history in firestore
            saveAnalysisHistory(user.uid, analysisResult)

            return analysisResult

        }catch (e: Exception) {
            Log.e("Firebase Repository", "Error in AI analysis", e)
            throw Exception("Analysis failed: ${e.message}")
        }
    }

    private fun parseAiResponse(responseText: String): AnalysisResult{
        return try {

            val lines = responseText.split("\n").filter { it.isNotBlank() }

            var diseaseName = "Unknown"
            var description = ""
            var solution = ""

            var currentSection = ""
            val descriptionLines = mutableListOf<String>()
            val solutionLines = mutableListOf<String>()

            for (line in lines){
                val cleanLine = line.trim()
                when{
                    cleanLine.contains("disease", ignoreCase = true) &&
                            cleanLine.contains(":", ignoreCase = true) -> {
                        currentSection = "disease"
                        diseaseName = cleanLine.substringAfter(":").trim()
                    }
                    cleanLine.contains("description", ignoreCase = true) &&
                            cleanLine.contains(":", ignoreCase = true) -> {
                        currentSection = "description"
                        val desc = cleanLine.substringAfter(":").trim()
                        if (desc.isNotEmpty()) descriptionLines.add(desc)
                    }
                    cleanLine.contains("solution", ignoreCase = true) &&
                            cleanLine.contains(":", ignoreCase = true) -> {
                        currentSection = "solution"
                        val sol = cleanLine.substringAfter(":").trim()
                        if (sol.isNotEmpty()) solutionLines.add(sol)
                    }
                    cleanLine.isNotEmpty() -> {
                        when (currentSection) {
                            "description" -> descriptionLines.add(cleanLine)
                            "solution" -> solutionLines.add(cleanLine)
                        }
                    }
                }
            }

            //if above parsing fails
            if (diseaseName == "Unknown" || description.isEmpty()) {
                // Fallback: use the first few lines for disease name
                val firstLine = lines.firstOrNull()?.trim() ?: "Unknown Issue"
                diseaseName = if (firstLine.length > 50) firstLine.substring(0, 50) + "..." else firstLine

                description = if (lines.size > 1) {
                    lines.subList(1, minOf(lines.size, 4)).joinToString(" ")
                } else {
                    "The image has been analyzed. Please consult with a local agricultural expert for detailed guidance."
                }

                solution = if (lines.size > 4) {
                    lines.subList(4, lines.size).joinToString(" ")
                } else {
                    "Apply general plant care practices and consult local agricultural extension services."
                }
            } else {
                description = descriptionLines.joinToString(" ")
                solution = solutionLines.joinToString(" ")
            }

            AnalysisResult(
                diseaseName = diseaseName,
                description = description.ifEmpty { "Analysis completed. Please consult with local experts for detailed guidance." },
                solution = solution.ifEmpty { "Apply general plant care practices and consult local agricultural extension services." }
            )
        }catch (e: Exception) {
            Log.e("Firebase Repository", "Error parsing AI response", e)
            AnalysisResult(
                diseaseName = "Analysis Completed",
                description = "The image has been analyzed. Please consult with a local agricultural expert for detailed guidance.",
                solution = "Apply general plant care practices and consult local agricultural extension services."
            )
        }
    }

    private suspend fun saveAnalysisHistory(userId: String, result: AnalysisResult){
        try {
            val historyData = hashMapOf(
                "userId" to userId,
                "diseaseName" to result.diseaseName,
                "description" to result.description,
                "solution" to result.solution,
                "timestamp" to FieldValue.serverTimestamp()
            )

            db.collection("analysis_history")
                .add(historyData)
                .await()

            Log.d("Firebase Repository", "Analysis saved to history")
        }catch (e: Exception) {
            Log.e("Firebase Repository", "Error saving to history", e)
        }
    }

    //function to get analysis history
    suspend fun getAnalysisHistory(userId: String): List<AnalysisResult>{
        return try {
            val querySnapshot = db.collection("analysis_history")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()

            querySnapshot.documents.mapNotNull {
                doc->
                try {
                    AnalysisResult(
                        diseaseName = doc.getString("diseaseName") ?: "Unknown",
                        description = doc.getString("description")?: "",
                        solution = doc.getString("solution")?: "",
                    )
                }catch (e: Exception) {
                    Log.e("Firebase Repository", "Error parsing history item", e)
                    null
                }
            }
        }catch (e: Exception) {
            Log.e("Firebase Repository", "Error fetching analysis history", e)
            emptyList()
        }
    }


}