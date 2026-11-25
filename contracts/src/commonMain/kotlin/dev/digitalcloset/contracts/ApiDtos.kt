package dev.digitalcloset.contracts

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(val code: String, val message: String)

@Serializable
data class HealthResponse(val status: String)
