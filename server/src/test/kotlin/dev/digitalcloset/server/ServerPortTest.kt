package dev.digitalcloset.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServerPortTest {
    @Test
    fun rejectsMalformedConfiguredPort() {
        listOf("", " ", "not-a-port", "8080 ", "0", "65536", "2147483648").forEach { value ->
            val error = assertFailsWith<IllegalArgumentException>(value) { serverPort(value) }
            assertEquals("PORT must be an integer between 1 and 65535", error.message)
        }
    }

    @Test
    fun preservesDefaultAndValidConfiguredPort() {
        assertEquals(8080, serverPort(null))
        assertEquals(9090, serverPort("9090"))
        assertEquals(1, serverPort("1"))
        assertEquals(65535, serverPort("65535"))
    }
}
