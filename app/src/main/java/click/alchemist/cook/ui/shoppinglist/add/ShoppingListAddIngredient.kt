package click.alchemist.cook.ui.shoppinglist.add

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.BackButton
import click.alchemist.cook.compose.previewIngredients
import click.alchemist.cook.compose.shoppinglist.add.ShoppingListAddIngredient
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.model.IngredientUnit
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.viewmodel.ShoppingListModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun ShoppingListAddIngredient(
	shoppingListId: String,
	snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
	backNavigation: (() -> Unit)? = null,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
	val viewModel = koinViewModel<ShoppingListAddIngredientViewModel>(parameters = { parametersOf(shoppingListId) })

	val shoppingList by viewModel.shoppingList.collectAsState(initial = null)
	val ingredients by viewModel.ingredients.collectAsState(initial = emptyList())
	val typedIngredient by viewModel.typedIngredient.collectAsState()

	val scope = rememberCoroutineScope()
	val context = LocalContext.current

	ShoppingListAddIngredientContent(
		snackbarHostState,
		shoppingList,
		ingredients,
		typedIngredient,
		{ scope.launch { viewModel.typedIngredient.emit(it.replace("\n", "")) } },
		{ name, amountString, unit ->
			scope.launch {
				val added = viewModel.addIngredient(name, amountString, unit)
				if (added != null) {
					val text = context.getString(R.string.toast_ingredient_added, added)
					snackbarHostState.showSnackbar(text)
				}
			}
		},
		backNavigation,
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope
	)
}

@Composable
private fun ShoppingListAddIngredientContent(
	snackbarHostState: SnackbarHostState,
	shoppingList: ShoppingListModel?,
	ingredients: List<String>,
	typedIngredient: String,
	ingredientChanged: ((String) -> Unit)? = null,
	addIngredient: ((String, String, IngredientUnit) -> Unit)? = null,
	backNavigation: (() -> Unit)? = null,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
	val sharedAnimation = if (sharedTransitionScope == null || animatedVisibilityScope == null) Modifier else {
		with(sharedTransitionScope) {

			Modifier
				.sharedBounds(
					rememberSharedContentState(key = "shoppinglist-add-fab"),
					animatedVisibilityScope = animatedVisibilityScope,
					enter = fadeIn() + scaleIn(),
					exit = fadeOut() + scaleOut(),
					resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds()
				)
				.skipToLookaheadSize()
		}
	}

	if (backNavigation == null) {
		ShoppingListAddIngredient(
			ingredients,
			typedIngredient,
			ingredientChanged = ingredientChanged,
			addIngredient = addIngredient
		)
	} else {
		with(sharedTransitionScope) {
			Scaffold(
				modifier = sharedAnimation,
				snackbarHost = {
					SnackbarHost(hostState = snackbarHostState)
				},
				topBar = {
					TopAppBar(
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
}

@Preview
@Composable
private fun Preview() {
	AppTheme {
		ShoppingListAddIngredientContent(
			remember { SnackbarHostState() },
			ShoppingListModel(ShoppingList("My List")),
			previewIngredients().filter { it.unitCategory != IngredientCategory.HEADER }.map { it.name },
			"Search",
			backNavigation = {},
		)
	}
}

@Preview(widthDp = 600, heightDp = 300, name = "Landscape")
@Composable
private fun PreviewWide() {
	AppTheme {
		ShoppingListAddIngredientContent(
			remember { SnackbarHostState() },
			ShoppingListModel(ShoppingList("My List")),
			previewIngredients().filter { it.unitCategory != IngredientCategory.HEADER }.map { it.name },
			"Search",
			backNavigation = {},
		)
	}
}