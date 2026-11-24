package dev.digitalcloset.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DemoOutfitTest {
    @Test
    fun suggestsTopBottomAndOptionalShoesFromAvailableGarments() {
        val garments = listOf(
            Garment("1", "Blue shirt", Category.TOP),
            Garment("2", "Black jeans", Category.BOTTOM),
            Garment("3", "Sneakers", Category.SHOES),
        )

        assertEquals(listOf("1", "2", "3"), suggestOutfit(garments)?.map { it.id })
    }

    @Test
    fun explainsWhenNoCompleteOutfitIsAvailable() {
        assertNull(suggestOutfit(listOf(Garment("1", "Blue shirt", Category.TOP))))
    }
}
