package com.hooked.auth.presentation.model

data class CreateAccountState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isFirstNameError: Boolean = false,
    val firstNameErrorMessage: String = "",
    val isLastNameError: Boolean = false,
    val lastNameErrorMessage: String = "",
    val isEmailError: Boolean = false,
    val emailErrorMessage: String = "",
    val isPasswordError: Boolean = false,
    val passwordErrorMessage: String = "",
    val isConfirmPasswordError: Boolean = false,
    val confirmPasswordErrorMessage: String = ""
)