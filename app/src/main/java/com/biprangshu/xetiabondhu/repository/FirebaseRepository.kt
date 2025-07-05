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
    Analyze this image of a plant leaf and provide a structured response.
    
    Format your response EXACTLY as follows:
    
    DISEASE: [Name of the disease/pest/condition or "Healthy" if no issues found]
    
    DESCRIPTION: [2-3 sentences describing the condition, symptoms, or health status]
    
    SOLUTION: [Practical recommendations for farmers in Assam. If healthy, provide maintenance tips]
    
    Important guidelines:
    - Use ONLY the keywords DISEASE, DESCRIPTION, and SOLUTION as section headers
    - Do not use asterisks, markdown formatting, or numbers
    - Keep language simple and practical for local farmers
    - If the image is unclear or not a plant leaf, set DISEASE to "Image Unclear"
    - Each section should be on a new line after the header
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

    private fun parseAiResponse(responseText: String): AnalysisResult {
        return try {
            var diseaseName = "Unknown"
            var description = ""
            var solution = ""

            //cleaning the reponse before formatting
            val cleanedText = responseText
                .replace("**", "")  //bold markers
                .replace("*", "")   //italic markers
                .replace("##", "")  //heading markers
                .replace("#", "")   //heading markers

            val lines = cleanedText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

            var currentSection = ""
            val diseaseLines = mutableListOf<String>()
            val descriptionLines = mutableListOf<String>()
            val solutionLines = mutableListOf<String>()

            for (line in lines) {
                when {
                    //section header to exactly match
                    line.uppercase().startsWith("DISEASE:") -> {
                        currentSection = "disease"
                        val content = line.substring(8).trim()
                        if (content.isNotEmpty()) diseaseLines.add(content)
                    }
                    line.uppercase().startsWith("DESCRIPTION:") -> {
                        currentSection = "description"
                        val content = line.substring(12).trim()
                        if (content.isNotEmpty()) descriptionLines.add(content)
                    }
                    line.uppercase().startsWith("SOLUTION:") -> {
                        currentSection = "solution"
                        val content = line.substring(9).trim()
                        if (content.isNotEmpty()) solutionLines.add(content)
                    }

                    //fallback method
                    line.contains("disease", ignoreCase = true) && line.contains(":") -> {
                        currentSection = "disease"
                        val content = line.substringAfter(":").trim()
                        if (content.isNotEmpty()) diseaseLines.add(content)
                    }
                    line.contains("description", ignoreCase = true) && line.contains(":") -> {
                        currentSection = "description"
                        val content = line.substringAfter(":").trim()
                        if (content.isNotEmpty()) descriptionLines.add(content)
                    }
                    line.contains("solution", ignoreCase = true) && line.contains(":") -> {
                        currentSection = "solution"
                        val content = line.substringAfter(":").trim()
                        if (content.isNotEmpty()) solutionLines.add(content)
                    }

                    //content lines
                    line.isNotEmpty() -> {
                        when (currentSection) {
                            "disease" -> diseaseLines.add(line)
                            "description" -> descriptionLines.add(line)
                            "solution" -> solutionLines.add(line)
                            "" -> {

                                if (diseaseLines.isEmpty() && line.length < 100) {
                                    diseaseLines.add(line)
                                    currentSection = "disease"
                                }
                            }
                        }
                    }
                }
            }


            diseaseName = diseaseLines.joinToString(" ").trim()
            description = descriptionLines.joinToString(" ").trim()
            solution = solutionLines.joinToString(" ").trim()


            if (diseaseName.isEmpty() || description.isEmpty()) {
                val fallbackResult = intelligentFallbackParse(lines)
                if (diseaseName.isEmpty()) diseaseName = fallbackResult.first
                if (description.isEmpty()) description = fallbackResult.second
                if (solution.isEmpty()) solution = fallbackResult.third
            }

            AnalysisResult(
                diseaseName = cleanText(diseaseName).ifEmpty { "Analysis Completed" },
                description = cleanText(description).ifEmpty {
                    "The image has been analyzed. Please consult with local experts for detailed guidance."
                },
                solution = cleanText(solution).ifEmpty {
                    "Apply general plant care practices and consult local agricultural extension services."
                }
            )

        } catch (e: Exception) {
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

    //helper functions to parse text
    private fun cleanText(text: String): String {
        return text.trim()
            .replace(Regex("\\s+"), " ")  //replace multiple spaces with single space
            .replace(Regex("^[\\d\\.\\-\\*\\•]+\\s*"), "")  //remove leading numbers, dots, dashes, bullets
    }

    //fallback parsing
    private fun intelligentFallbackParse(lines: List<String>): Triple<String, String, String> {
        var diseaseName = "Unknown Issue"
        var description = ""
        var solution = ""

        if (lines.isNotEmpty()) {
            val firstMeaningfulLine = lines.firstOrNull { line ->
                line.isNotEmpty() &&
                        !line.contains("analyze", ignoreCase = true) &&
                        !line.contains("image", ignoreCase = true) &&
                        line.length > 3
            }

            if (firstMeaningfulLine != null) {
                diseaseName = if (firstMeaningfulLine.length > 80) {
                    firstMeaningfulLine.substring(0, 77) + "..."
                } else {
                    firstMeaningfulLine
                }
            }

            val remainingLines = lines.filter { it != firstMeaningfulLine }
            val totalLines = remainingLines.size

            if (totalLines > 0) {
                val midPoint = totalLines / 2
                description = remainingLines.take(maxOf(1, midPoint)).joinToString(" ").trim()
                solution = remainingLines.drop(midPoint).joinToString(" ").trim()
            }
        }

        return Triple(
            cleanText(diseaseName),
            cleanText(description),
            cleanText(solution)
        )
    }


}