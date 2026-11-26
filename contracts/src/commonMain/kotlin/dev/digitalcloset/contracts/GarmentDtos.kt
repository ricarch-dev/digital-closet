package dev.digitalcloset.contracts

import kotlinx.serialization.Serializable

@Serializable
data class CreateGarmentRequest(
    val uploadId: String,
    val category: String,
    val color: String,
    val tags: List<String>,
    val season: String,
    val notes: String,
)

@Serializable
data class UpdateGarmentRequest(
    val category: String? = null,
    val color: String? = null,
    val tags: List<String>? = null,
    val season: String? = null,
    val notes: String? = null,
    val status: String? = null,
    val available: Boolean? = null,
)

@Serializable
data class GarmentResponse(
    val id: String,
    val photoId: String,
    val category: String,
    val color: String,
    val tags: List<String>,
    val season: String,
    val notes: String,
    val status: String,
    val available: Boolean,
)
