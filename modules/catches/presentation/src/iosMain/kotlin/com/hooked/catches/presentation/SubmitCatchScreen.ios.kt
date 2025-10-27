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
    onPhotoSelected: (String) -> Unit,
    onRemovePhoto: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (photoUri != null) {
            AsyncImage(
                imageUrl = photoUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: iOS image picker implementation */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Change Photo")
                }
                
                OutlinedButton(
                    onClick = onRemovePhoto,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
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
                OutlinedButton(onClick = { /* TODO: iOS image picker implementation */ }) {
                    Text("Choose from Gallery")
                }
            }
        }
    }
}