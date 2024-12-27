package click.alchemist.cook.ui.shoppinglist.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.BackButton
import click.alchemist.cook.compose.CookIconButton
import click.alchemist.cook.compose.VerticalDivider
import click.alchemist.cook.compose.previewShoppingItems
import click.alchemist.cook.compose.shoppinglist.detail.ShoppingListDetail
import click.alchemist.cook.model.IngredientUnit
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.model.ShoppingListItem
import click.alchemist.cook.ui.shoppinglist.add.ShoppingListAddIngredient
import click.alchemist.cook.viewmodel.ShoppingListModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun ShoppingListDetail(
	shoppingListId: String,
	navigateToAddItem: ((shoppingListId: String) -> Unit)? = null,
	backNavigation: () -> Unit
) {
	val viewModel = koinViewModel<ShoppingListDetailViewModel>(parameters = { parametersOf(shoppingListId) })
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

	val snackbarHostState = remember { SnackbarHostState() }

	Scaffold(
		snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
		topBar = {
			TopAppBar(
				title = { Text(text = shoppingList?.shoppingList?.name ?: "") },
				navigationIcon = { BackButton(backNavigation) },
				actions = {
					CookIconButton(
						onClick = clearItem,
						iconResource = R.drawable.ic_notification_clear_all,
						contentDescription = "Clear",
						tint = Color.White
					)
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
					ShoppingListAddIngredient(shoppingListId, snackbarHostState)
				}
			}
		} else {
			ShoppingListDetail(Modifier.padding(paddingValues), items, onItemClick, onLongClick = { dialogOpenFor = it })
		}

		dialogOpenFor?.let { dialogOpenForValue ->
			ShoppingListReduceDialog(
				ingredient = dialogOpenForValue,
				dismiss = { dialogOpenFor = null },
				apply = { amount, unit -> onReduceIngredient(dialogOpenForValue, amount, unit) })
		}
	}
}


@Preview(name = "Portrait")
@Composable
private fun Preview() {
	AppTheme {
		val list = ShoppingListModel(ShoppingList("Preview List"))
		val items = previewShoppingItems()
		ShoppingListDetailContent("shoppingListId", list, items, floatingButton = {})
	}
}


@Preview(widthDp = 600, heightDp = 300, name = "Landscape")
@Composable
private fun PreviewWide() {
	AppTheme {
		val list = ShoppingListModel(ShoppingList("Preview List"))
		val items = previewShoppingItems()
		ShoppingListDetailContent("shoppingListId", list, items)
	}
}