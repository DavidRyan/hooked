package com.hooked.auth.presentation

import com.hooked.auth.domain.entities.RegisterCredentials
import com.hooked.auth.domain.usecases.RegisterUseCase
import com.hooked.auth.presentation.model.CreateAccountEffect
import com.hooked.auth.presentation.model.CreateAccountIntent
import com.hooked.auth.presentation.model.CreateAccountState
import com.hooked.core.HookedViewModel
import com.hooked.core.domain.UseCaseResult
import com.hooked.core.logging.Logger
import kotlinx.coroutines.launch

class CreateAccountViewModel(
    private val registerUseCase: RegisterUseCase
) : HookedViewModel<CreateAccountIntent, CreateAccountState, CreateAccountEffect>() {

    companion object {
        private const val TAG = "CreateAccountViewModel"
    }

    override fun createInitialState(): CreateAccountState {
        return CreateAccountState()
    }

    override fun handleIntent(intent: CreateAccountIntent) {
        when (intent) {
            is CreateAccountIntent.UpdateFirstName -> {
                setState { 
                    copy(
                        firstName = intent.firstName,
                        isFirstNameError = false,
                        firstNameErrorMessage = ""
                    )
                }
            }
            is CreateAccountIntent.UpdateLastName -> {
                setState { 
                    copy(
                        lastName = intent.lastName,
                        isLastNameError = false,
                        lastNameErrorMessage = ""
                    )
                }
            }
            is CreateAccountIntent.UpdateEmail -> {
                setState { 
                    copy(
                        email = intent.email,
                        isEmailError = false,
                        emailErrorMessage = ""
                    )
                }
            }
            is CreateAccountIntent.UpdatePassword -> {
                setState { 
                    copy(
                        password = intent.password,
                        isPasswordError = false,
                        passwordErrorMessage = "",
                        isConfirmPasswordError = false,
                        confirmPasswordErrorMessage = ""
                    )
                }
            }
            is CreateAccountIntent.UpdateConfirmPassword -> {
                setState { 
                    copy(
                        confirmPassword = intent.confirmPassword,
                        isConfirmPasswordError = false,
                        confirmPasswordErrorMessage = ""
                    )
                }
            }
            is CreateAccountIntent.CreateAccount -> {
                performCreateAccount()
            }
            is CreateAccountIntent.ClearErrors -> {
                setState { 
                    copy(
                        isFirstNameError = false,
                        isLastNameError = false,
                        isEmailError = false,
                        isPasswordError = false,
                        isConfirmPasswordError = false,
                        firstNameErrorMessage = "",
                        lastNameErrorMessage = "",
                        emailErrorMessage = "",
                        passwordErrorMessage = "",
                        confirmPasswordErrorMessage = ""
                    )
                }
            }
        }
    }

    private fun performCreateAccount() {
        val currentState = state.value
        
        if (!validateInput(currentState)) {
            return
        }

        setState { copy(isLoading = true) }
        
        viewModelScope.launch {
            val credentials = RegisterCredentials(
                email = currentState.email,
                password = currentState.password,
                firstName = currentState.firstName,
                lastName = currentState.lastName
            )
            
            when (val result = registerUseCase(credentials)) {
                is UseCaseResult.Success -> {
                    setState { copy(isLoading = false) }
                    sendEffect { CreateAccountEffect.NavigateToHome }
                }
                is UseCaseResult.Error -> {
                    Logger.error(TAG, "Registration failed: ${result.message}")
                    setState { copy(isLoading = false) }
                    sendEffect { CreateAccountEffect.ShowError(result.message) }
                }
            }
        }
    }

    private fun validateInput(state: CreateAccountState): Boolean {
        var isValid = true
        
        if (state.firstName.isBlank()) {
            setState { 
                copy(
                    isFirstNameError = true,
                    firstNameErrorMessage = "First name is required"
                )
            }
            isValid = false
        }
        
        if (state.lastName.isBlank()) {
            setState { 
                copy(
                    isLastNameError = true,
                    lastNameErrorMessage = "Last name is required"
                )
            }
            isValid = false
        }
        
        if (state.email.isBlank()) {
            setState { 
                copy(
                    isEmailError = true,
                    emailErrorMessage = "Email is required"
                )
            }
            isValid = false
        } else if (!isValidEmail(state.email)) {
            setState { 
                copy(
                    isEmailError = true,
                    emailErrorMessage = "Invalid email format"
                )
            }
            isValid = false
        }
        
        if (state.password.isBlank()) {
            setState { 
                copy(
                    isPasswordError = true,
                    passwordErrorMessage = "Password is required"
                )
            }
            isValid = false
        } else if (state.password.length < 6) {
            setState { 
                copy(
                    isPasswordError = true,
                    passwordErrorMessage = "Password must be at least 6 characters"
                )
            }
            isValid = false
        }
        
        if (state.confirmPassword.isBlank()) {
            setState { 
                copy(
                    isConfirmPasswordError = true,
                    confirmPasswordErrorMessage = "Please confirm your password"
                )
            }
            isValid = false
        } else if (state.password != state.confirmPassword) {
            setState { 
                copy(
                    isConfirmPasswordError = true,
                    confirmPasswordErrorMessage = "Passwords do not match"
                )
            }
            isValid = false
        }
        
        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
}