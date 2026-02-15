package com.hooked.core.location

import androidx.compose.runtime.Composable

/**
 * A composable that handles requesting location permissions on each platform.
 *
 * @param onPermissionResult Called with `true` if permission was granted, `false` otherwise.
 * @param content A composable that receives a `requestPermission` lambda to trigger the request.
 */
@Composable
expect fun LocationPermissionRequester(
    onPermissionResult: (granted: Boolean) -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
)
