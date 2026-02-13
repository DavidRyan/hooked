package com.hooked.core.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun NativeMapPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
)
