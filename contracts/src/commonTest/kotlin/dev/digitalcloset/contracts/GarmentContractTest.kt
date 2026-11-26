package dev.digitalcloset.contracts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class GarmentContractTest {
    @Test
    fun privatePhotoAndGarmentContractsRoundTripWithoutAnOwnerInput() {
        val upload = RequestUploadUrl(contentType = "image/jpeg", byteSize = 128)
        assertEquals(upload, Json.decodeFromString<RequestUploadUrl>(Json.encodeToString(upload)))

        val garment = CreateGarmentRequest(
            uploadId = "upload-id",
            category = "top",
            color = "navy",
            tags = listOf("minimal"),
            season = "all",
            notes = "",
        )
        assertEquals(garment, Json.decodeFromString<CreateGarmentRequest>(Json.encodeToString(garment)))
    }
}
