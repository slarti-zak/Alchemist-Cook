package click.alchemist.cook.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class Recipe(
	var name: String = "",
	var content: String = "",
	var ingredients: List<Ingredient> = listOf(),
	var serves: Int = 1,
//	var currentlyCooking: Boolean = false,
	var extendedContent: RecipeGraph? = null,
	@JsonIgnore var id: String = ""
)