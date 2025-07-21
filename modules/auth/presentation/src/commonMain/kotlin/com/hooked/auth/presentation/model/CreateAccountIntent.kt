package com.hooked.auth.presentation.model

sealed class CreateAccountIntent {
    data class UpdateFirstName(val firstName: String) : CreateAccountIntent()
    data class UpdateLastName(val lastName: String) : CreateAccountIntent()
    data class UpdateEmail(val email: String) : CreateAccountIntent()
    data class UpdatePassword(val password: String) : CreateAccountIntent()
    data class UpdateConfirmPassword(val confirmPassword: String) : CreateAccountIntent()
    object CreateAccount : CreateAccountIntent()
    object ClearErrors : CreateAccountIntent()
}