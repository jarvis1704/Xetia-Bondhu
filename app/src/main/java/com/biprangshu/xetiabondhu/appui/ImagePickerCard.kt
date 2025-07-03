package com.biprangshu.xetiabondhu.appui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.biprangshu.xetiabondhu.utils.createImageUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerCard(
    modifier: Modifier = Modifier,
    onImageSelected: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    //camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) {
        success->
        if(success && cameraImageUri!=null){
            onImageSelected(cameraImageUri!!)
        }
    }

    //galery launcher
    val galeryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) {
        uri->
        uri?.let { onImageSelected(it) }
    }

    //camera launcher permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        isGranted->
        if(isGranted){
            //create URI and launch camera
            val uri = createImageUri(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }else{
            //permission denied
            Toast.makeText(context, "Camera permission is required. Please Grant it!", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showBottomSheet = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Image",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add Image",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    //modal bottom sheet
    if(showBottomSheet){
        ModalBottomSheet(
            onDismissRequest = {showBottomSheet = false}
        ) {
            ImagePickerBottomSheetContent(
                onCameraClick = {
                    showBottomSheet = false
                    //checking if camera permission granted
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            //granted, launch camera
                            val uri = createImageUri(context)
                            cameraImageUri= uri
                            cameraLauncher.launch(uri)
                        }
                        else -> {
                            //ask for camera permission
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }

                },
                onGalleryClick = {
                    showBottomSheet = false
                    galeryLauncher.launch("image/*")
                }
            )
        }
    }
}

@Composable
fun ImagePickerBottomSheetContent(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Image",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Camera option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCameraClick() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Camera",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Take Photo",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Gallery option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onGalleryClick() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Gallery",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Choose from Gallery",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}