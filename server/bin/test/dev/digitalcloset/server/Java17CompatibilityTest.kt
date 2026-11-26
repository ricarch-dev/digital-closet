package dev.digitalcloset.server

import dev.digitalcloset.contracts.HealthResponse
import java.io.DataInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Java17CompatibilityTest {
    @Test
    fun healthContractBytecodeSupportsJava17() {
        val resource = "/dev/digitalcloset/contracts/HealthResponse.class"
        val stream = requireNotNull(HealthResponse::class.java.getResourceAsStream(resource))
        DataInputStream(stream).use {
            assertEquals(0xCAFEBABE.toInt(), it.readInt())
            it.readUnsignedShort()
            val major = it.readUnsignedShort()
            assertTrue(major <= 61, "Expected Java 17-compatible bytecode; got $major")
        }
    }
}
