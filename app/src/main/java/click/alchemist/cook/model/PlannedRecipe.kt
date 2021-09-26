package click.alchemist.cook.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class PlannedRecipe(
	val recipeId: String = "",
	val servings: Int = 0,
	@JsonIgnore override var id: String = "",
	override var owner: String = ""
) : DatabaseObject