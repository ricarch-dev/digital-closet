package dev.digitalcloset.contracts

import kotlinx.serialization.Serializable

@Serializable
data class RequestUploadUrl(val contentType: String, val byteSize: Long)

@Serializable
data class UploadUrlResponse(
    val uploadId: String,
    val url: String,
    val headers: Map<String, String>,
    val expiresAt: Long,
)

@Serializable
data class ConfirmUploadResponse(val uploadId: String)

@Serializable
data class ReadPhotoUrlResponse(val url: String, val expiresAt: Long)
