package click.alchemist.cook.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class ShoppingList(
	var name: String = "",
	@JsonIgnore override var id: String = "",
	override var owner: String = ""
) : DatabaseObject