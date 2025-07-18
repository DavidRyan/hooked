package com.hooked.auth.presentation

import com.hooked.auth.domain.entities.LoginCredentials
import com.hooked.auth.domain.usecases.LoginUseCase
import com.hooked.auth.presentation.model.LoginEffect
import com.hooked.auth.presentation.model.LoginIntent
import com.hooked.auth.presentation.model.LoginState
import com.hooked.core.HookedViewModel
import com.hooked.core.domain.UseCaseResult
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : HookedViewModel<LoginIntent, LoginState, LoginEffect>() {

    override fun createInitialState(): LoginState {
        return LoginState()
    }

    override fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateEmail -> {
                setState { 
                    copy(
                        email = intent.email,
                        isEmailError = false,
                        emailErrorMessage = ""
                    )
                }
            }
            is LoginIntent.UpdatePassword -> {
                setState { 
                    copy(
                        password = intent.password,
                        isPasswordError = false,
                        passwordErrorMessage = ""
                    )
                }
            }
            is LoginIntent.Login -> {
                performLogin()
            }
            is LoginIntent.ClearErrors -> {
                setState { 
                    copy(
                        isEmailError = false,
                        isPasswordError = false,
                        emailErrorMessage = "",
                        passwordErrorMessage = ""
                    )
                }
            }
        }
    }

    private fun performLogin() {
        val currentState = state.value
        
        if (!validateInput(currentState)) {
            return
        }

        setState { copy(isLoading = true) }
        
        viewModelScope.launch {
            val credentials = LoginCredentials(
                email = currentState.email,
                password = currentState.password
            )
            
            when (val result = loginUseCase(credentials)) {
                is UseCaseResult.Success -> {
                    setState { copy(isLoading = false) }
                    sendEffect { LoginEffect.NavigateToHome }
                }
                is UseCaseResult.Error -> {
                    setState { copy(isLoading = false) }
                    sendEffect { LoginEffect.ShowError(result.message) }
                }
            }
        }
    }

    private fun validateInput(state: LoginState): Boolean {
        var isValid = true
        
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
        
        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
}