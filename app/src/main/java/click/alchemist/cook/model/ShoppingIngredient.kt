package click.alchemist.cook.model

import java.util.*

@Deprecated("Use ShoppingListItem")
data class ShoppingIngredient(
    val ingredient: Ingredient = Ingredient(),
    val finished: Boolean = false,
    val id: String = UUID.randomUUID().toString()
)