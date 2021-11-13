package click.alchemist.cook.ui.shoppinglist.overview

import androidx.annotation.StringRes
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.SwipeDeleteBackground
import click.alchemist.cook.compose.rememberToolbarPadding
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.viewmodel.ShoppingListModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel


@Composable
fun ShoppingListOverview(modifier: Modifier, onShoppingListClick: (ShoppingListModel) -> Unit) {
	val viewModel = getViewModel<ShoppingListOverviewViewModel>()

	var editedShoppingList: ShoppingListModel? by remember { mutableStateOf(null) }
	var addShoppingList by remember { mutableStateOf(false) }
	val shoppingLists by viewModel.shoppingLists.collectAsState(initial = emptyList())

	ShoppingListOverviewContent(
		modifier,
		shoppingLists,
		onClick = onShoppingListClick,
		onLongClick = { editedShoppingList = it },
		deleteEntry = { list -> viewModel.delete(list) },
		undoDeleteEntry = { viewModel.saveShoppingList(it.shoppingList.copy(id = "")) },
		onFloatingButtonClick = { addShoppingList = true }
	)

	val edited = editedShoppingList
	if (edited != null) {
		EditDialog(
			edited.shoppingList.name,
			R.string.shopping_list_rename_dialog_title,
			{ newName -> viewModel.editShoppingList(edited, newName) },
			{ editedShoppingList = null }
		)
	}

	if (addShoppingList) {
		EditDialog("",
			R.string.shopping_list_add_dialog_title,
			{ newName -> viewModel.saveShoppingList(newName) },
			{ addShoppingList = false })
	}
}


@Composable
private fun EditDialog(initialText: String, @StringRes title: Int, applyFunction: (name: String) -> Unit, onDismissRequest: () -> Unit) {
	var text by remember { mutableStateOf(initialText) }

	AlertDialog(
		onDismissRequest = onDismissRequest,
		confirmButton = {
			TextButton(onClick = {
				applyFunction(text)
				onDismissRequest()
			}) { Text(stringResource(R.string.general_ok)) }
		},
		dismissButton = {
			TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.general_cancel)) }
		},
		title = { Text(stringResource(title)) },
		text = {
			TextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
		},
		properties = DialogProperties(usePlatformDefaultWidth = true)
	)
}


@Composable
private fun ShoppingListOverviewContent(
	modifier: Modifier,
	shoppingLists: List<ShoppingListModel>,
	onClick: ((ShoppingListModel) -> Unit) = { },
	onLongClick: ((ShoppingListModel) -> Unit) = { },
	deleteEntry: ((ShoppingListModel) -> Unit) = { },
	undoDeleteEntry: ((ShoppingListModel) -> Unit) = { },
	onFloatingButtonClick: (() -> Unit) = { }
) {
	val scaffoldState = rememberScaffoldState()
	val snackbarCoroutineScope = rememberCoroutineScope()
	val snackbarTitle = stringResource(R.string.shopping_list_deleted_toast)
	val snackbarAction = stringResource(R.string.general_undo)

	Scaffold(modifier,
		scaffoldState = scaffoldState,
		topBar = {
			com.google.accompanist.insets.ui.TopAppBar(
				contentPadding = rememberToolbarPadding(),
				title = { Text(text = stringResource(R.string.title_shopping)) })
		},
		floatingActionButton = {
			FloatingActionButton(onClick = onFloatingButtonClick) {
				Icon(painterResource(R.drawable.ic_plus), contentDescription = "Add List")
			}
		}
	)
	{ paddingValues ->
		LazyColumn(
			Modifier
				.fillMaxSize()
				.padding(paddingValues),
			contentPadding = PaddingValues(8.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			items(items = shoppingLists, key = { it.shoppingList.id }, itemContent = { entry ->
				val dismissState = rememberDismissState(
					confirmStateChange = {
						val dismissed = it == DismissValue.DismissedToEnd || it == DismissValue.DismissedToStart
						if (dismissed) {
							deleteEntry(entry)
							snackbarCoroutineScope.launch {
								val result = scaffoldState.snackbarHostState.showSnackbar(snackbarTitle, snackbarAction, SnackbarDuration.Long)
								if (result == SnackbarResult.ActionPerformed) {
									undoDeleteEntry(entry)
								}
							}
						}
						dismissed
					}
				)

				SwipeToDismiss(state = dismissState, background = { SwipeDeleteBackground(dismissState) }) {
					ShoppingListItem(entry, onClick, onLongClick)
				}
			})
		}
	}
}


@Composable
private fun ShoppingListItem(
	entry: ShoppingListModel,
	onClick: (ShoppingListModel) -> Unit,
	onLongClick: (ShoppingListModel) -> Unit
) {
	Card(
		Modifier.fillMaxWidth(),
		elevation = 8.dp
	) {
		Column(
			Modifier
				.combinedClickable(onClick = { onClick(entry) }, onLongClick = { onLongClick(entry) })
				.padding(8.dp)
		) {
			Text(
				if (entry.shoppingList.name.isBlank()) stringResource(R.string.list_item_empty) else entry.shoppingList.name,
				style = MaterialTheme.typography.h6
			)
			Text(
				stringResource(
					R.string.shopping_list_item_subtitle,
					entry.completedCount,
					entry.ingredients.count()
				),
				style = MaterialTheme.typography.subtitle2
			)
		}
	}
}


@Preview
@Composable
private fun Preview() {
	AppTheme {
		ShoppingListOverviewContent(
			Modifier,
			listOf(
				ShoppingListModel(ShoppingList(id = "1", name = "List 1 List 1 List 1 List 1 List 1 List 1 List 1 List 1")),
				ShoppingListModel(ShoppingList(id = "2", name = "List 2"))
			)
		)
	}
}