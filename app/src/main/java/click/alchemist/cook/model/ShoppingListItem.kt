package click.alchemist.cook.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class ShoppingListItem(
	var shoppingListId: String = "",
	val ingredient: Ingredient = Ingredient(),
	val finished: Boolean = false,
	@JsonIgnore override var id: String = "",
	override var owner: String = ""
) : DatabaseObject