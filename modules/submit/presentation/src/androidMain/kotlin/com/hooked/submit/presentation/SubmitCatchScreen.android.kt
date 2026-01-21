package com.hooked.submit.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hooked.core.components.AsyncImage
import com.hooked.theme.HookedTheme

@Composable
internal actual fun PhotoSectionContent(
    photoUri: String?,
    onPhotoSelected: (String) -> Unit
) {
    // Use OpenDocument instead of GetContent to preserve EXIF metadata (including GPS location)
    // The photo picker and GetContent strip location data for privacy, but OpenDocument
    // provides access to the original file with all metadata intact
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.toString()?.let(onPhotoSelected)
    }
    
    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (photoUri != null) {
            AsyncImage(
                imageUrl = photoUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { galleryLauncher.launch(arrayOf("image/*")) }) {
                    Text("Choose Different")
                }
            }
        } else {
            Icon(
                modifier = Modifier.size(40.dp)
                    .align(Alignment.CenterHorizontally),
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = HookedTheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                text = "Add a photo of your catch",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { galleryLauncher.launch(arrayOf("image/*")) }) {
                    Text("Choose from Gallery")
                }
            }
        }
    }
}