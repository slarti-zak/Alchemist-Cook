package click.alchemist.cook.ui.shoppinglist.add

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.BackButton
import click.alchemist.cook.compose.previewIngredients
import click.alchemist.cook.compose.rememberToolbarPadding
import click.alchemist.cook.compose.shoppinglist.add.ShoppingListAddIngredient
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.model.IngredientUnit
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.viewmodel.ShoppingListModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun ShoppingListAddIngredient(
	shoppingListId: String,
	scaffoldState: ScaffoldState = rememberScaffoldState(),
	backNavigation: (() -> Unit)? = null
) {
	val viewModel = getViewModel<ShoppingListAddIngredientViewModel>(parameters = { parametersOf(shoppingListId) })

	val shoppingList by viewModel.shoppingList.collectAsState(initial = null)
	val ingredients by viewModel.ingredients.collectAsState(initial = emptyList())
	val typedIngredient by viewModel.typedIngredient.collectAsState()

	val scope = rememberCoroutineScope()
	val context = LocalContext.current

	ShoppingListAddIngredientContent(
		scaffoldState,
		shoppingList,
		ingredients,
		typedIngredient,
		{ scope.launch { viewModel.typedIngredient.emit(it.replace("\n", "")) } },
		{ name, amountString, unit ->
			scope.launch {
				val added = viewModel.addIngredient(name, amountString, unit)
				if (added != null) {
					val text = context.getString(R.string.toast_ingredient_added, added)
					scaffoldState.snackbarHostState.showSnackbar(text)
				}
			}
		},
		backNavigation
	)
}

@Composable
private fun ShoppingListAddIngredientContent(
	scaffoldState: ScaffoldState,
	shoppingList: ShoppingListModel?,
	ingredients: List<String>,
	typedIngredient: String,
	ingredientChanged: ((String) -> Unit)? = null,
	addIngredient: ((String, String, IngredientUnit) -> Unit)? = null,
	backNavigation: (() -> Unit)? = null
) {
	if (backNavigation == null) {
		ShoppingListAddIngredient(
			ingredients,
			typedIngredient,
			ingredientChanged = ingredientChanged,
			addIngredient = addIngredient
		)
	} else {
		Scaffold(scaffoldState = scaffoldState,
			topBar = {
				com.google.accompanist.insets.ui.TopAppBar(
					contentPadding = rememberToolbarPadding(),
					title = { Text(text = shoppingList?.shoppingList?.name ?: "") },
					navigationIcon = { BackButton(backNavigation) }
				)
			}) { padding ->
			ShoppingListAddIngredient(
				ingredients,
				typedIngredient,
				modifier = Modifier.padding(padding),
				ingredientChanged,
				addIngredient
			)
		}
	}
}

@Preview
@Composable
private fun Preview() {
	AppTheme {
		ShoppingListAddIngredientContent(
			rememberScaffoldState(),
			ShoppingListModel(ShoppingList("My List")),
			previewIngredients().filter { it.unitCategory != IngredientCategory.HEADER }.map { it.name },
			"Search",
			backNavigation = {})
	}
}

@Preview(widthDp = 600, heightDp = 300, name = "Landscape")
@Composable
private fun PreviewWide() {
	AppTheme {
		ShoppingListAddIngredientContent(
			rememberScaffoldState(),
			ShoppingListModel(ShoppingList("My List")),
			previewIngredients().filter { it.unitCategory != IngredientCategory.HEADER }.map { it.name },
			"Search",
			backNavigation = {})
	}
}