package dev.digitalcloset.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class PrototypeBoundaryTest {
    @Test
    fun everyCategorySubsetProducesOnlyACompleteAvailableOutfit() {
        for (mask in 0 until 16) {
            val input = Category.entries
                .filter { mask and (1 shl it.ordinal) != 0 }
                .map { Garment(it.name, it.name, it) }
                .toMutableList()
            val before = input.toList()
            val expectedBase = when {
                mask and 4 != 0 -> listOf(Category.ONE_PIECE)
                mask and 3 == 3 -> listOf(Category.TOP, Category.BOTTOM)
                else -> null
            }
            val expected = expectedBase?.let {
                it + if (mask and 8 != 0) listOf(Category.SHOES)
                else emptyList()
            }
            assertEquals(expected, suggestOutfit(input)?.map { it.category }, "mask=$mask")
            assertEquals(before, input, "Input mutated for mask=$mask")
        }
    }

    @Test
    fun sampleOutfitUsesExistingDressAndShoes() {
        assertEquals(
            listOf("sample-4", "sample-3"),
            suggestOutfit(sampleGarments)?.map { it.id },
        )
    }
}
