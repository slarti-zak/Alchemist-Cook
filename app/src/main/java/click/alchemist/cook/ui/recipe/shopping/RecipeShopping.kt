package click.alchemist.cook.ui.recipe.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.BackButton
import click.alchemist.cook.compose.CookIconButton
import click.alchemist.cook.compose.Header
import click.alchemist.cook.compose.ListDropdownMenu
import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.viewmodel.Serving
import click.alchemist.cook.viewmodel.ShoppingListModel
import org.koin.androidx.compose.getViewModel


@Composable

fun RecipeShopping(recipeId: String, serving: Serving, onBackNavigation: () -> Unit) {
	val viewModel = getViewModel<RecipeShoppingViewModel>()
	LaunchedEffect(recipeId) {
		viewModel.load(recipeId, serving)
	}

	val ingredients by viewModel.ingredients.collectAsState()
	val selectedShoppingList by viewModel.selectedShoppingList.collectAsState()
	val shoppingLists by viewModel.shoppingLists.collectAsState()

	RecipeShoppingContent(
		ingredients,
		selectedShoppingList,
		shoppingLists,
		onBackClick = onBackNavigation,
		floatingButtonClick = {
			viewModel.addToShoppingList()
			onBackNavigation()
		},
		onShoppingListSelected = viewModel::setSelectedShoppingList,
		onItemClick = viewModel::toggleIngredient
	)
}


@Composable
fun RecipeShoppingContent(
	ingredients: List<RecipeShoppingIngredient>?,
	selectedShoppingList: ShoppingListModel?,
	shoppingLists: List<ShoppingListModel>,
	floatingButtonClick: () -> Unit = {},
	onBackClick: () -> Unit = {},
	onShoppingListSelected: (ShoppingListModel?) -> Unit = {},
	onItemClick: (RecipeShoppingIngredient) -> Unit = {},
) {
	Scaffold(topBar = {
		TopAppBar(
			title = {
				ListDropdownMenu(
					selected = selectedShoppingList, items = shoppingLists,
					Modifier.fillMaxSize(),
					onShoppingListSelected
				) {
					Text(it?.shoppingList?.name ?: "none")
				}
			},
			navigationIcon = { BackButton(onBackClick) },
			actions = {
				CookIconButton(onClick = floatingButtonClick, iconResource = R.drawable.ic_check, contentDescription = "Accept")
			}
		)
	}) { paddingValues ->
		if (ingredients == null) return@Scaffold
		LazyColumn(
			Modifier.padding(paddingValues),
			contentPadding = PaddingValues(8.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
			content = {
				items(ingredients, key = { it.id }) {
					if (it.ingredient.unitCategory == IngredientCategory.HEADER) {
						Header(it.ingredient.name)
					} else {
						RecipeShoppingItem(it, onClick = { onItemClick(it) })
					}
				}
			})
	}
}


@Preview
@Composable
private fun Preview() {
	AppTheme {
		val shoppingListModel = ShoppingListModel(ShoppingList("Shopping"))
		RecipeShoppingContent(
			listOf(
				RecipeShoppingIngredient(Ingredient("Ingredient 1"), Ingredient("Ingredient 1")),
				RecipeShoppingIngredient(Ingredient("Ingredient 2"), null),
			),
			shoppingListModel,
			listOf(shoppingListModel),
		)
	}
}
