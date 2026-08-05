package click.alchemist.cook.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class ShoppingList(
	var name: String = "",
	@JsonIgnore var id: String = ""
)