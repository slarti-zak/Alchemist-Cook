package click.alchemist.cook.ui.shoppinglist.overview

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.SwipeDeleteBackground
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.viewmodel.ShoppingListModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel


@Composable
fun ShoppingListOverview(
	modifier: Modifier = Modifier,
	onShoppingListClick: (ShoppingListModel) -> Unit,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope
) {
	val viewModel = koinViewModel<ShoppingListOverviewViewModel>()

	var editedShoppingList: ShoppingListModel? by remember { mutableStateOf(null) }
	var addShoppingList by remember { mutableStateOf(false) }
	val shoppingLists by viewModel.shoppingLists.collectAsState(initial = emptyList())

	ShoppingListOverviewContent(
		modifier = modifier,
		shoppingLists = shoppingLists,
		onClick = onShoppingListClick,
		onLongClick = { editedShoppingList = it },
		deleteEntry = { list -> viewModel.delete(list) },
		undoDeleteEntry = { viewModel.saveShoppingList(it.shoppingList.copy(id = "")) },
		onFloatingButtonClick = { addShoppingList = true },
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope
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
	modifier: Modifier = Modifier,
	shoppingLists: List<ShoppingListModel>,
	onClick: ((ShoppingListModel) -> Unit) = { },
	onLongClick: ((ShoppingListModel) -> Unit) = { },
	deleteEntry: ((ShoppingListModel) -> Unit) = { },
	undoDeleteEntry: ((ShoppingListModel) -> Unit) = { },
	onFloatingButtonClick: (() -> Unit) = { },
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope
) {
	val snackbarHostState = remember { SnackbarHostState() }
	val snackbarCoroutineScope = rememberCoroutineScope()
	val snackbarTitle = stringResource(R.string.shopping_list_deleted_toast)
	val snackbarAction = stringResource(R.string.general_undo)

	with(sharedTransitionScope) {
		Scaffold(modifier,
			containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
			snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
			floatingActionButton = {
				FloatingActionButton(
					onClick = onFloatingButtonClick,
					modifier = Modifier
						.sharedElement(
							rememberSharedContentState(key = "shoppinglist-fab"),
							animatedVisibilityScope = animatedVisibilityScope
						)
				) {
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
					val dismissState = rememberSwipeToDismissBoxState(
						confirmValueChange = {
							val dismissed = it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd
							if (dismissed) {
								deleteEntry(entry)
								snackbarCoroutineScope.launch {
									val result = snackbarHostState.showSnackbar(snackbarTitle, snackbarAction, duration = SnackbarDuration.Long)
									if (result == SnackbarResult.ActionPerformed) {
										undoDeleteEntry(entry)
									}
								}
							}
							dismissed
						}
					)

					SwipeToDismissBox(
						modifier = Modifier
							.sharedBounds(
								rememberSharedContentState(key = "shoppinglist-${entry.shoppingList.id}"),
								animatedVisibilityScope = animatedVisibilityScope,
								enter = fadeIn() + scaleIn(),
								exit = fadeOut() + scaleOut(),
								resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
							)
							.skipToLookaheadSize(),
						state = dismissState,
						backgroundContent = { SwipeDeleteBackground(dismissState) }) {
						ShoppingListItem(
							entry = entry,
							onClick = onClick,
							onLongClick = onLongClick,
							sharedTransitionScope = sharedTransitionScope,
							animatedVisibilityScope = animatedVisibilityScope
						)
					}
				})
			}
		}
	}
}


@Composable
private fun ShoppingListItem(
	entry: ShoppingListModel,
	onClick: (ShoppingListModel) -> Unit,
	onLongClick: (ShoppingListModel) -> Unit,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope
) {
	with(sharedTransitionScope) {
		Card(
			modifier = Modifier
				.fillMaxWidth()
				.combinedClickable(onClick = { onClick(entry) }, onLongClick = { onLongClick(entry) }),
			elevation = CardDefaults.cardElevation(8.dp),
			colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
		) {
			Column(Modifier.padding(8.dp)) {
				Text(
					text = entry.shoppingList.name.ifBlank { stringResource(R.string.list_item_empty) },
					style = MaterialTheme.typography.headlineMedium,
					modifier = Modifier
						.fillMaxWidth()
						.sharedElement(
							rememberSharedContentState(key = "shoppinglist-text-${entry.shoppingList.id}"),
							animatedVisibilityScope = animatedVisibilityScope
						)
				)
				Text(
					text = stringResource(
						R.string.shopping_list_item_subtitle,
						entry.completedCount,
						entry.ingredients.count()
					),
					style = MaterialTheme.typography.bodyLarge
				)
			}
		}
	}
}


@Preview
@Composable
private fun Preview() {
	AppTheme {
		SharedTransitionLayout {
			AnimatedVisibility(visible = true) {
				ShoppingListOverviewContent(
					shoppingLists = listOf(
						ShoppingListModel(ShoppingList(id = "1", name = "List 1 List 1 List 1 List 1 List 1 List 1 List 1 List 1")),
						ShoppingListModel(ShoppingList(id = "2", name = "List 2"))
					),
					sharedTransitionScope = this@SharedTransitionLayout,
					animatedVisibilityScope = this@AnimatedVisibility
				)
			}
		}
	}
}