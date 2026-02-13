package com.hooked.core.datetime

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIDatePicker
import platform.UIKit.UIDatePickerMode
import platform.UIKit.UIDatePickerStyle
import platform.UIKit.UIControlEventValueChanged
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeDateTimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    label: String
) {
    val onChangeState by rememberUpdatedState(onValueChange)
    val formatter = remember {
        NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        }
    }

    val parsedDate = remember(value) {
        formatter.dateFromString(value) ?: NSDate()
    }

    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
    val target = remember {
        DatePickerTarget { date ->
            val newValue = formatter.stringFromDate(date)
            onChangeState(newValue)
        }
    }

    UIKitView(
        modifier = Modifier.fillMaxWidth(),
        factory = {
            UIDatePicker().apply {
                datePickerMode = UIDatePickerMode.UIDatePickerModeDateAndTime
                preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleWheels
                date = parsedDate
                addTarget(
                    target = target,
                    action = NSSelectorFromString("onValueChanged:"),
                    forControlEvents = UIControlEventValueChanged
                )
            }
        },
        update = { picker ->
            if (picker.date != parsedDate) {
                picker.date = parsedDate
            }
        }
        )
        Text(
            text = "Defaults to now",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private class DatePickerTarget(
    private val onChange: (NSDate) -> Unit
) : NSObject() {
    @ObjCAction
    fun onValueChanged(sender: UIDatePicker) {
        onChange(sender.date)
    }
}
