package com.biprangshu.xetiabondhu.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.biprangshu.xetiabondhu.appui.HistoryItem
import com.biprangshu.xetiabondhu.datamodel.AnalysisResult
import com.biprangshu.xetiabondhu.datamodel.MessageModel
import com.biprangshu.xetiabondhu.datamodel.UserData
import com.biprangshu.xetiabondhu.utils.apiKey
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject


class FirebaseRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val firebaseStorage: FirebaseStorage,
    private val context: Context
){

    private var realtimeDatabase  = FirebaseDatabase.getInstance()

//    val generativeModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).generativeModel(
//        modelName = "gemini-2.5-flash",
////        generationConfig = generationConfig {
////            temperature = 0.7f
////            topK = 40
////            topP = 0.95f
////            maxOutputTokens = 1024
////        }
//        //todo: create a generation config for your model
//    )

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey,
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
    You are an agricultural expert specializing in tea, paddy, litchi, guava, mustard, vegetables, dragon fruit, and crops common in Tezpur, Assam, India. Analyze plant leaf images and respond in this exact format:

    DISEASE: [Disease/pest name or "Healthy" if no issues]
    DESCRIPTION: [2-3 sentences on condition/symptoms/health]
    SOLUTION: [Practical recommendations for Assam farmers. If healthy, give maintenance tips]

    Guidelines:
    - Use ONLY these keywords as headers: DISEASE, DESCRIPTION, SOLUTION
    - No asterisks, markdown, or numbers
    - Address farmers as "you"
    - Simple language for local farmers
    - If image unclear: DISEASE = "Image Unclear"
    - If plant outside expertise: mention plant name in DESCRIPTION, note your expertise in SOLUTION with advice
    - Each section on new line after header
""".trimIndent()

            val processedBitmap = processImageForAnalysis(uri)

            //creating context for ai
            val inputContext = content {
                text(prompt)
                image(processedBitmap)
            }

            Log.d("Firebase Repository", "Sending request to Gemini API")

            //generate result using AI
            val response = generativeModel.generateContent(inputContext)
            //response
            val responseText = response.text ?: throw Exception("Empty response from AI")

            Log.d("Firebase Repository", "Received response from AI: $responseText")

            //saving image to firebase storage
            val downloadUrl = uploadImageToStorage(uri)

            //parsing to get structured data
            val analysisResult = parseAiResponse(responseText, downloadUrl)

            //save history in firestore
            saveAnalysisHistory(user.uid, analysisResult)

            return analysisResult

        }catch (e: Exception) {
            Log.e("Firebase Repository", "Error in AI analysis", e)
            //more detailed error message
            val errorMessage = when {
                e.message?.contains("SerializationException") == true ->
                    "Image processing failed. Please try a different image."
                e.message?.contains("network") == true ->
                    "Network error. Please check your connection and try again."
                e.message?.contains("quota") == true ->
                    "Service temporarily unavailable. Please try again later."
                else -> "Analysis failed: ${e.message}"
            }

            throw Exception(errorMessage)
        }
    }

    suspend fun uploadImageToStorage(uri: Uri): String?{
        val requestId = UUID.randomUUID()
        val user = auth.currentUser
        var downloadUrl: String? = null
        user?.let {
            val path = "uploads/${user.uid}/$requestId.jpg"

            //uploading image to storage
            val uploadTask = firebaseStorage.reference.child(path).putFile(uri).await()
            Log.d("Firebase Repository", "Image Uploaded sucessfully")

            //retrieving download link
            if(uploadTask.task.isSuccessful){
                downloadUrl = firebaseStorage.reference.child(path).downloadUrl.await()?.toString()
                Log.d("Firebase Repository", "Download URL: $downloadUrl")
            }else{
                //task failed
                Log.e("Firebase Repository", "Image upload failed: ${uploadTask.task.exception}")
            }
        }

        return downloadUrl
    }

    private fun parseAiResponse(responseText: String, downloadUrl: String?): AnalysisResult {
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
                },
                imageDownlaodUrl = downloadUrl
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
                "downloadLink" to result.imageDownlaodUrl,
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
    suspend fun getAnalysisHistory(userId: String): List<HistoryItem>{
        return try {
            Log.d("Firebase Repository", "Fetching analysis history for user: $userId")

            val querySnapshot = db.collection("analysis_history")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            Log.d("Firebase Repository", "Found ${querySnapshot.documents.size} history documents")


            val historyItems = querySnapshot.documents.mapNotNull { doc ->
                try {
                    Log.d("Firebase Repository", "Processing document: ${doc.id}")
                    Log.d("Firebase Repository", "Document data: ${doc.data}")

                    val analysisResult = AnalysisResult(
                        diseaseName = doc.getString("diseaseName") ?: "Unknown Disease",
                        description = doc.getString("description") ?: "No description available",
                        solution = doc.getString("solution") ?: "No solution available",
                        imageDownlaodUrl = doc.getString("downloadLink") ?: ""
                    )

                    val timestamp = doc.getTimestamp("timestamp")
                    val timestampString = if (timestamp != null) {
                        try {
                            val date = timestamp.toDate()
                            val format =
                                SimpleDateFormat("d MMMM yyyy 'at' HH:mm:ss z", Locale.ENGLISH)
                            format.format(date)
                        } catch (e: Exception) {
                            Log.e("Firebase Repository", "Error formatting timestamp", e)
                            "Unknown date"
                        }
                    } else {
                        "Unknown date"
                    }

                    Log.d("Firebase Repository", "Created HistoryItem: disease=${analysisResult.diseaseName}, timestamp=$timestampString")

                    HistoryItem(
                        analysisResult = analysisResult,
                        timestamp = timestampString
                    ) to (timestamp?.toDate()?.time ?: 0L)
                } catch (e: Exception) {
                    Log.e("Firebase Repository", "Error parsing history item from document ${doc.id}", e)
                    null
                }
            }.sortedByDescending { it.second }
                .take(50)
                .map { it.first }

            historyItems
        } catch (e: Exception) {
            Log.e("Firebase Repository", "Error fetching analysis history", e)
            emptyList()
        }
    }

    //functions for aichatbot

    //function to create message to AI
    suspend fun sendMessageToAi(userMessage: String){
        val currentUser = auth.currentUser ?: throw IllegalStateException("Not Signed In")

        Log.d("Firebase Repository", "Sending user message to realtime db")
        //send user message to realtime db
        sendAiMessage(message = userMessage, senderId = currentUser.uid){
            success->
            //handle on success
        }

        //write prompt to ai to generate response
        val prompt = """
    You are an agricultural expert named Xetia Bondhu AI specializing in tea, paddy, litchi, guava, mustard, vegetables, dragon fruit, and crops common in Tezpur, Assam, India. Answer the question asked by the farmer:
    $userMessage
    
    Guidelines:
    - Be friendly to the farmer.
    - No asterisks, markdown
    - Address farmers as "you"
    - Simple language for local farmers
    - Each section on new line after header
""".trimIndent()

        Log.d("Firebase Repository", "Sending prompt to AI")

        val inputContext = content {
            text(prompt)
        }

        val response  = generativeModel.generateContent(inputContext)

        val responseText = response.text ?: throw Exception("Empty response from AI")

        Log.d("Firebase Repository", "Received response from AI: $responseText, sending it to realtime db")

        //sending Ai message to realtime db
        sendAiMessage(message = responseText, senderId = "AI Response"){
            success->
            //handle on success
        }
    }

    //message sending function to realtime database
    fun sendAiMessage(message: String, senderId: String, onComplete: (Boolean)-> Unit){
        val currentUser  = auth.currentUser?: return
        val chatId = currentUser.uid
        val messageRef = realtimeDatabase.getReference("chats/$chatId/messages")

        val messageId = messageRef.push().key ?: return

        val messageModel = MessageModel(
            message = message,
            messageId = messageId,
            timestamp = System.currentTimeMillis(),
            senderId = senderId
        )

        messageRef.child(messageId).setValue(messageModel)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
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

    //helper function to process image
    private suspend fun processImageForAnalysis(uri: Uri): Bitmap{
        //converting image to bitmap
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        //image size declaration
        val maxWidth = 1024
        val maxHeight = 1024
        val maxFileSize = 4 * 1024 * 1024 //accounts upto 4MB

        //checking if image needs resising
        return if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
            Log.d("Firebase Repository", "Resizing image from ${bitmap.width}x${bitmap.height}")
            //resize required, algorithm to resise it
            val scaleFactor = minOf(
                maxWidth.toFloat() / bitmap.width,
                maxHeight.toFloat() / bitmap.height
            )

            val newWidth = (bitmap.width * scaleFactor).toInt()
            val newHeight = (bitmap.height * scaleFactor).toInt()

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            // Recycle original bitmap to free memory
            if (bitmap != resizedBitmap) {
                bitmap.recycle()
            }

            Log.d("Firebase Repository", "Image resized to ${newWidth}x${newHeight}")
            resizedBitmap
        } else {
            //resize not required, original image will do
            bitmap
        }
    }


}