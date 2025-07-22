package com.hooked.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hooked.auth.presentation.model.CreateAccountEffect
import com.hooked.auth.presentation.model.CreateAccountIntent
import com.hooked.core.animation.AnimationConstants
import com.hooked.core.presentation.toast.ToastManager
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateAccountScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateAccountViewModel = koinViewModel(),
    toastManager: ToastManager = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    
    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateAccountEffect.NavigateToHome -> {
                    onNavigateToHome()
                }
                is CreateAccountEffect.ShowError -> {
                    toastManager.showError(effect.message)
                }
            }
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(AnimationConstants.CONTENT_PADDING_DP.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = AnimationConstants.CARD_ELEVATION_DP.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Join Hooked to track your catches",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.firstName,
                            onValueChange = { viewModel.sendIntent(CreateAccountIntent.UpdateFirstName(it)) },
                            label = { Text("First Name") },
                            placeholder = { Text("Enter first name") },
                            isError = state.isFirstNameError,
                            supportingText = if (state.isFirstNameError) {
                                { Text(state.firstNameErrorMessage) }
                            } else null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Right) }
                            ),
                            modifier = Modifier.weight(1f),
                            enabled = !state.isLoading
                        )
                        
                        OutlinedTextField(
                            value = state.lastName,
                            onValueChange = { viewModel.sendIntent(CreateAccountIntent.UpdateLastName(it)) },
                            label = { Text("Last Name") },
                            placeholder = { Text("Enter last name") },
                            isError = state.isLastNameError,
                            supportingText = if (state.isLastNameError) {
                                { Text(state.lastNameErrorMessage) }
                            } else null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.weight(1f),
                            enabled = !state.isLoading
                        )
                    }
                    
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { viewModel.sendIntent(CreateAccountIntent.UpdateEmail(it)) },
                        label = { Text("Email") },
                        placeholder = { Text("Enter your email") },
                        isError = state.isEmailError,
                        supportingText = if (state.isEmailError) {
                            { Text(state.emailErrorMessage) }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    )
                    
                    PasswordTextField(
                        value = state.password,
                        onValueChange = { viewModel.sendIntent(CreateAccountIntent.UpdatePassword(it)) },
                        label = "Password",
                        placeholder = "Enter your password",
                        isError = state.isPasswordError,
                        errorMessage = state.passwordErrorMessage,
                        enabled = !state.isLoading,
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    PasswordTextField(
                        value = state.confirmPassword,
                        onValueChange = { viewModel.sendIntent(CreateAccountIntent.UpdateConfirmPassword(it)) },
                        label = "Confirm Password",
                        placeholder = "Confirm your password",
                        isError = state.isConfirmPasswordError,
                        errorMessage = state.confirmPasswordErrorMessage,
                        enabled = !state.isLoading,
                        onDone = { viewModel.sendIntent(CreateAccountIntent.CreateAccount) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { viewModel.sendIntent(CreateAccountIntent.CreateAccount) },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp)
                            )
                        } else {
                            Text("Create Account")
                        }
                    }
                    
                    TextButton(
                        onClick = onNavigateToLogin,
                        enabled = !state.isLoading
                    ) {
                        Text("Already have an account? Sign In")
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean,
    errorMessage: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = isError,
        supportingText = if (isError) {
            { Text(errorMessage) }
        } else null,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) {
                        Icons.Default.VisibilityOff
                    } else {
                        Icons.Default.Visibility
                    },
                    contentDescription = if (passwordVisible) {
                        "Hide password"
                    } else {
                        "Show password"
                    }
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext?.invoke() },
            onDone = { onDone?.invoke() }
        ),
        modifier = modifier,
        enabled = enabled
    )
}