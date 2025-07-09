package com.hooked.catches.presentation

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
    // For iOS, we'll temporarily use a placeholder implementation
    // This would need to be implemented with UIImagePickerController or PHPickerViewController
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
                TextButton(onClick = { /* TODO: iOS implementation */ }) {
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
                OutlinedButton(onClick = { /* TODO: iOS implementation */ }) {
                    Text("Choose from Gallery")
                }
            }
        }
    }
}