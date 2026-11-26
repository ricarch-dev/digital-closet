package dev.digitalcloset.contracts

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val styles: List<String>,
    val colors: List<String>,
    val occasions: List<String>,
)

@Serializable
data class ProfileResponse(
    val styles: List<String>,
    val colors: List<String>,
    val occasions: List<String>,
)
