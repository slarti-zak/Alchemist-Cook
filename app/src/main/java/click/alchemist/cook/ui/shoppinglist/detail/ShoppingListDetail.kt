package click.alchemist.cook.ui.shoppinglist.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import click.alchemist.cook.R
import click.alchemist.cook.compose.*
import click.alchemist.cook.compose.shoppinglist.detail.ShoppingListDetail
import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.IngredientUnit
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.model.ShoppingListItem
import click.alchemist.cook.ui.shoppinglist.add.ShoppingListAddIngredient
import click.alchemist.cook.viewmodel.ShoppingListModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf


@ExperimentalComposeUiApi
@ExperimentalCoroutinesApi
@FlowPreview
@ExperimentalFoundationApi
@Composable
fun ShoppingListDetail(
	shoppingListId: String,
	navigateToAddItem: ((shoppingListId: String) -> Unit)? = null,
	backNavigation: () -> Unit
) {
	val viewModel = getViewModel<ShoppingListDetailViewModel>(parameters = { parametersOf(shoppingListId) })
	val scope = rememberCoroutineScope()

	val shoppingList by viewModel.shoppingList.collectAsState(initial = null)
	val ingredients by viewModel.ingredients.collectAsState(emptyList())

	ShoppingListDetailContent(
		shoppingListId,
		shoppingList,
		ingredients,
		backNavigation = backNavigation,
		clearItem = { scope.launch { viewModel.clearList() } },
		floatingButton = navigateToAddItem?.let { { it(shoppingListId) } },
		onItemClick = viewModel::toggleState,
		onReduceIngredient = viewModel::remove
	)
}

@ExperimentalComposeUiApi
@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@Composable
private fun ShoppingListDetailContent(
	shoppingListId: String,
	shoppingList: ShoppingListModel?,
	items: List<ShoppingListItem>,
	backNavigation: () -> Unit = {},
	clearItem: () -> Unit = {},
	floatingButton: (() -> Unit)? = null,
	onItemClick: (ShoppingListItem) -> Unit = {},
	onReduceIngredient: (ShoppingListItem, amount: String, IngredientUnit) -> Unit = { _, _, _ -> }
) {
	var dialogOpenFor by remember { mutableStateOf<ShoppingListItem?>(null) }

	val plusIcon = painterResource(R.drawable.ic_plus)

	val scaffoldState = rememberScaffoldState()

	Scaffold(scaffoldState = scaffoldState,
		topBar = {
			com.google.accompanist.insets.ui.TopAppBar(
				contentPadding = rememberToolbarPadding(),
				title = { Text(text = shoppingList?.shoppingList?.name ?: "") },
				navigationIcon = { BackButton(backNavigation) },
				actions = {
					CookIconButton(onClick = clearItem, iconResource = R.drawable.ic_notification_clear_all, contentDescription = "Clear", tint = Color.White)
				}
			)
		},
		floatingActionButton = {
			if (floatingButton != null) {
				FloatingActionButton(onClick = floatingButton) {
					Icon(painter = plusIcon, contentDescription = "Add Ingredient")
				}
			}
		}) { paddingValues ->
		val showAddList = floatingButton == null
		if (showAddList) {
			Row(
				Modifier
					.fillMaxSize()
					.padding(paddingValues)
			) {
				Box(
					Modifier.weight(0.5f)
				) {
					ShoppingListDetail(items = items, onClick = onItemClick, onLongClick = { dialogOpenFor = it })
				}
				VerticalDivider(Modifier.fillMaxHeight())
				Box(
					Modifier.weight(0.5f)
				) {
					ShoppingListAddIngredient(shoppingListId, scaffoldState)
				}
			}
		} else {
			ShoppingListDetail(Modifier.padding(paddingValues), items, onItemClick, onLongClick = { dialogOpenFor = it })
		}

		val dialogOpenForValue = dialogOpenFor
		if (dialogOpenForValue != null) {
			ShoppingListReduceDialog(
				ingredient = dialogOpenForValue,
				dismiss = { dialogOpenFor = null },
				apply = { amount, unit -> onReduceIngredient(dialogOpenForValue, amount, unit) })
		}
	}
}

@ExperimentalComposeUiApi
@ExperimentalCoroutinesApi
@FlowPreview
@ExperimentalFoundationApi
@Preview(name = "Portrait")
@Composable
private fun Preview() {
	AppTheme {
		val list = ShoppingListModel(ShoppingList("Preview List"))
		val items = listOf(ShoppingListItem("a", id = "a"), ShoppingListItem("b", id = "b"))
		ShoppingListDetailContent("shoppingListId", list, items, floatingButton = {})
	}
}

@ExperimentalComposeUiApi
@ExperimentalCoroutinesApi
@FlowPreview
@ExperimentalFoundationApi
@Preview(widthDp = 600, heightDp = 300, name = "Landscape")
@Composable
private fun PreviewWide() {
	AppTheme {
		val list = ShoppingListModel(ShoppingList("Preview List"))
		val items = listOf(
			ShoppingListItem("a", id = "a", ingredient = Ingredient(name = "Milk")),
			ShoppingListItem("b", id = "b", ingredient = Ingredient(name = "Bread"))
		)
		ShoppingListDetailContent("shoppingListId", list, items)
	}
}