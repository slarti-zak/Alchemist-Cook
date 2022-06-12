package click.alchemist.cook.model.firestore

import click.alchemist.cook.model.Ingredient
import com.google.firebase.firestore.DocumentId

data class Recipe(
	var name: String = "",
	var content: String = "",
	var ingredients: List<Ingredient> = listOf(),
	var serves: Int = 1,
//	var currentlyCooking: Boolean = false,
//	var extendedContent: RecipeGraph? = null,

	@DocumentId var id: String? = null,
	override var owner: String = ""
) : FirestoreObject