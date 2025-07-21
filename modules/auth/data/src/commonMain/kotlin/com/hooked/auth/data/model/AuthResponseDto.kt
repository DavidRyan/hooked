package com.hooked.auth.data.model

import com.hooked.auth.domain.entities.UserEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto(
    val data: AuthDataDto
)

@Serializable
data class AuthDataDto(
    val user: UserDto,
    val token: String
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("inserted_at") val insertedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class UserResponseDto(
    val data: UserDto
)

@Serializable
data class AuthErrorDto(
    val error: String,
    val message: String? = null,
    val details: Map<String, List<String>>? = null,
    val code: String? = null
)

fun AuthResponseDto.toUserEntity(): UserEntity {
    return UserEntity(
        id = data.user.id,
        email = data.user.email,
        username = "${data.user.firstName ?: ""} ${data.user.lastName ?: ""}".trim().ifEmpty { data.user.email },
        token = data.token
    )
}

fun UserResponseDto.toUserEntity(): UserEntity {
    return UserEntity(
        id = data.id,
        email = data.email,
        username = "${data.firstName ?: ""} ${data.lastName ?: ""}".trim().ifEmpty { data.email },
        token = null
    )
}