package com.biprangshu.xetiabondhu.appui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ResultScreen(
    disease: String,
    description: String,
    solution: String,
    onBack: ()-> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
