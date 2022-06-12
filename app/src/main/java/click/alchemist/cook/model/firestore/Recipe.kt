package click.alchemist.cook.model.firestore

import com.google.firebase.firestore.DocumentId

data class Recipe(
	val name: String = "",
	val content: String = "",
//	var ingredients: List<Ingredient> = listOf(),
	val serves: Int = 1,
//	var currentlyCooking: Boolean = false,
//	var extendedContent: RecipeGraph? = null,

	@DocumentId override val id: String = "",
	override var owner: String = ""
) : FirestoreObject