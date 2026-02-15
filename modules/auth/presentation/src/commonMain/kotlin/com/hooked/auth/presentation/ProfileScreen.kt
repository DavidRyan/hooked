package com.hooked.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hooked.auth.presentation.model.ProfileEffect
import com.hooked.auth.presentation.model.ProfileIntent
import com.hooked.core.presentation.toast.ToastManager
import com.hooked.theme.HookedTheme
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
    toastManager: ToastManager = koinInject()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sendIntent(ProfileIntent.LoadProfile)
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ProfileEffect.NavigateToLogin -> onNavigateToLogin()
                is ProfileEffect.ShowError -> toastManager.showError(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HookedTheme.primary,
                    titleContentColor = HookedTheme.onPrimary,
                    navigationIconContentColor = HookedTheme.onPrimary
                )
            )
        },
        containerColor = HookedTheme.background
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = HookedTheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Initials avatar
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(HookedTheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.initials,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = HookedTheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Username
                Text(
                    text = state.username,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HookedTheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Email
                Text(
                    text = state.email,
                    fontSize = 14.sp,
                    color = HookedTheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Account section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = HookedTheme.surface
                    )
                ) {
                    Column {
                        // Section header
                        Text(
                            text = "Account",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HookedTheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                            letterSpacing = 1.sp
                        )

                        HorizontalDivider(
                            color = HookedTheme.onSurface.copy(alpha = 0.1f)
                        )

                        // Logout row
                        TextButton(
                            onClick = { viewModel.sendIntent(ProfileIntent.Logout) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoggingOut
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    tint = HookedTheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                if (state.isLoggingOut) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = HookedTheme.error
                                    )
                                } else {
                                    Text(
                                        text = "Log Out",
                                        color = HookedTheme.error,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
