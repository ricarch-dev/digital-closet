package dev.digitalcloset.contracts

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileContractTest {
    @Test
    fun profileSupportsMultipleSelectedStyles() {
        val request = UpdateProfileRequest(
            styles = listOf("casual", "formal"),
            colors = listOf("blue"),
            occasions = listOf("work"),
        )

        val json = Json.encodeToString(UpdateProfileRequest.serializer(), request)

        assertEquals(request, Json.decodeFromString(UpdateProfileRequest.serializer(), json))
    }
}
