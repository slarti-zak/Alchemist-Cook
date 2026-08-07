package click.alchemist.cook.compose

import click.alchemist.cook.model.DbDuration
import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.model.RecipeGraphNode
import click.alchemist.cook.model.RunningTimer
import click.alchemist.cook.model.ShoppingListItem
import click.alchemist.cook.model.Timer
import click.alchemist.cook.service.store.LibraryConfig
import click.alchemist.cook.service.store.LibraryConnection
import click.alchemist.cook.service.store.LibraryRole
import click.alchemist.cook.service.webdav.WebDavConfig
import click.alchemist.cook.viewmodel.IngredientModel
import click.alchemist.cook.viewmodel.RecipeGraphModel
import click.alchemist.cook.viewmodel.RecipeGraphNodeModel
import click.alchemist.cook.viewmodel.TimerModel
import java.math.BigDecimal
import kotlin.time.Duration.Companion.minutes

fun previewIngredients(): List<IngredientModel> {
	return listOf(
		IngredientModel(Ingredient("Header", unitCategory = IngredientCategory.HEADER), 1),
		IngredientModel(Ingredient("Item 1"), 2),
		IngredientModel(Ingredient("Item 2"), 3),
		IngredientModel(Ingredient("Other", unitCategory = IngredientCategory.HEADER), 4),
		IngredientModel(Ingredient("Milk"), 5)
	)
}

fun previewShoppingItems(): List<ShoppingListItem> {
	return listOf(
		ShoppingListItem("a", Ingredient("", BigDecimal.ONE, IngredientCategory.HEADER), finished = false),
		ShoppingListItem("a", Ingredient("Milk", BigDecimal.ONE, IngredientCategory.VOLUME), id = "a"),
		ShoppingListItem("a", Ingredient("Meat", BigDecimal.TEN, IngredientCategory.WEIGHT), id = "b"),
		ShoppingListItem("a", Ingredient("", BigDecimal.ONE, IngredientCategory.HEADER), finished = true),
		ShoppingListItem("a", Ingredient("Bread", BigDecimal.TEN, IngredientCategory.WEIGHT), id = "c")
	)
}

fun previewTimers(): List<TimerModel> {
	return listOf(
		TimerModel(Timer("Timer", DbDuration(5.minutes)))
	)
}

fun previewRunningTimer(): TimerModel {
	val duration = DbDuration(5.minutes)
	return TimerModel(Timer("Timer", duration), RunningTimer(duration = duration), remaining = 1.minutes, 0.2)
}

fun previewLibraries(): List<LibraryConfig> {
	return listOf(
		LibraryConfig(
			id = "family",
			label = "Family Recipes",
			role = LibraryRole.SHARED,
			connection = LibraryConnection.WebDav(WebDavConfig("https://cloud.example.com/remote.php/dav/files/family", "family", ""))
		),
		LibraryConfig(
			id = "friends",
			label = "Friends Potluck",
			role = LibraryRole.SHARED,
			connection = LibraryConnection.Nextcloud(
				WebDavConfig("https://nextcloud.example.org/remote.php/dav/files/friends", "friends", ""),
				serverUrl = "https://nextcloud.example.org"
			)
		),
		LibraryConfig(
			id = "camping",
			label = "Camping Trip",
			role = LibraryRole.SHARED,
			connection = LibraryConnection.LocalFolder(treeUri = "content://example/tree/camping", displayName = "Camping")
		)
	)
}

fun previewExtendedInstruction(): RecipeGraphModel {
	return RecipeGraphModel(
		listOf(
			RecipeGraphNodeModel(RecipeGraphNode("id1", "Instruction 1"), "recipe name"),
			RecipeGraphNodeModel(RecipeGraphNode("id2", "Instruction 2"), "recipe name"),
			RecipeGraphNodeModel(RecipeGraphNode("id3", "Timed Instruction", DbDuration(5.minutes)), "recipe name"),
		), false
	)
}