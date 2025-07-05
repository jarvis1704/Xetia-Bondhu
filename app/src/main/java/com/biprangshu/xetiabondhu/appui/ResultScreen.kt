package com.biprangshu.xetiabondhu.appui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ResultScreen(
    disease: String,
    description: String,
    solution: String,
    downloadLink: String?,
    onBack: ()-> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier= Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column {
                AsyncImage(
                    model = ImageRequest.Builder(context = LocalContext.current).data(downloadLink).crossfade(true).build(),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
                //todo: add placeholder and on error image for coil
            }
        }
        Text("Diagnosis:", style = MaterialTheme.typography.titleLarge)
        Text(disease, style = MaterialTheme.typography.bodyLarge)

        Text("Description:", style = MaterialTheme.typography.titleMedium)
        Text(description)

        Text("Solution:", style = MaterialTheme.typography.titleMedium)
        Text(solution)

        Spacer(Modifier.weight(1f))
        Button(onClick = onBack) {
            Text("Back to Home")
        }
    }
}
