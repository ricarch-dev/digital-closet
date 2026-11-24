package dev.digitalcloset.shared

enum class Category(val label: String) {
    TOP("Tops"),
    BOTTOM("Bottoms"),
    ONE_PIECE("One-pieces"),
    SHOES("Shoes"),
}

data class Garment(
    val id: String,
    val name: String,
    val category: Category,
)

val sampleGarments = listOf(
    Garment("sample-1", "Blue linen shirt", Category.TOP),
    Garment("sample-2", "Black straight jeans", Category.BOTTOM),
    Garment("sample-3", "White sneakers", Category.SHOES),
    Garment("sample-4", "Green summer dress", Category.ONE_PIECE),
)

/**
 * A deliberately small prototype rule, not the production recommendation engine.
 */
fun suggestOutfit(garments: List<Garment>): List<Garment>? {
    val base = garments.firstOrNull { it.category == Category.ONE_PIECE }?.let(::listOf)
        ?: run {
            val top = garments.firstOrNull { it.category == Category.TOP }
            val bottom = garments.firstOrNull { it.category == Category.BOTTOM }
            if (top == null || bottom == null) return null
            listOf(top, bottom)
        }
    return base + listOfNotNull(garments.firstOrNull { it.category == Category.SHOES })
}
