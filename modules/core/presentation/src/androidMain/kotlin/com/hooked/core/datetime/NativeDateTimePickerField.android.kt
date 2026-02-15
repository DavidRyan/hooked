package com.hooked.core.datetime

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
actual fun NativeDateTimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    label: String
) {
    val context = LocalContext.current
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss") }
    val displayFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a") }

    val currentDateTime = remember(value) {
        if (value.isBlank()) {
            LocalDateTime.now()
        } else {
            runCatching { LocalDateTime.parse(value) }.getOrElse { LocalDateTime.now() }
        }
    }

    val displayValue = remember(value) {
        if (value.isBlank()) {
            "Tap to select date/time"
        } else {
            runCatching {
                LocalDateTime.parse(value).format(displayFormatter)
            }.getOrElse { value }
        }
    }

    val calendar = remember(currentDateTime) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentDateTime.year)
            set(Calendar.MONTH, currentDateTime.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, currentDateTime.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, currentDateTime.hour)
            set(Calendar.MINUTE, currentDateTime.minute)
            set(Calendar.SECOND, 0)
        }
    }

    val showDatePicker = {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)

                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        val picked = LocalDateTime.of(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.DAY_OF_MONTH),
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            0
                        )
                        onValueChange(picked.format(formatter))
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(modifier = modifier) {
        // Wrap in a Box with clickable to intercept taps
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showDatePicker() }
        ) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label) },
                readOnly = true,
                enabled = false // Disable to prevent focus, Box handles clicks
            )
        }
    }
}
