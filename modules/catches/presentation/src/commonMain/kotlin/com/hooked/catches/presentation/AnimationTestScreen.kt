package com.hooked.catches.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hooked.theme.HookedTheme

@Composable
fun AnimationTestScreen() {
    var step by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HookedTheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Animation Test Screen",
            style = MaterialTheme.typography.headlineMedium,
            color = HookedTheme.onBackground
        )
        
        Text(
            text = "Step: $step",
            style = MaterialTheme.typography.bodyLarge,
            color = HookedTheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        ProgressIndicator(step = step)
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { if (step > 0) step-- },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HookedTheme.primary
                )
            ) {
                Text("Previous")
            }
            
            Button(
                onClick = { step = 0 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HookedTheme.secondary
                )
            ) {
                Text("Reset")
            }
            
            Button(
                onClick = { if (step < 4) step++ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HookedTheme.primary
                )
            ) {
                Text("Next")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Single Bobber Test:",
            style = MaterialTheme.typography.titleMedium,
            color = HookedTheme.onBackground
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Bobber(shouldSink = false)
        }
    }
}
