package click.alchemist.cook.ui.recipe.list

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.CookIconButton
import click.alchemist.cook.compose.ToolbarTextField
import click.alchemist.cook.compose.recipe.detail.RecipeListItem
import click.alchemist.cook.compose.rememberToolbarPadding
import click.alchemist.cook.model.BlobModel
import click.alchemist.cook.model.Recipe
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import org.koin.androidx.compose.getViewModel


@Composable
fun RecipeList(onSettingsClick: () -> Unit, onRecipeClick: (RecipeListItem) -> Unit, onAddRecipe: () -> Unit) {
	val viewModel = getViewModel<RecipeListViewModel>()
	val recipes by viewModel.recipes.collectAsState(initial = emptyList())
	val searchTerm by viewModel.search.collectAsState()

	val scope = rememberCoroutineScope()

	RecipeListContent(
		recipes,
		searchTerm,
		viewModel::loadImage,
		floatingButtonClick = onAddRecipe,
		onItemClick = onRecipeClick,
		onSettingsClick = onSettingsClick,
		onSearched = { scope.launch { viewModel.search.emit(it) } }
	)
}


@Composable
fun RecipeListContent(
	recipes: List<RecipeListItem>,
	searchTerm: String,
	imageLoader: suspend (Recipe) -> BlobModel,
	floatingButtonClick: (() -> Unit) = {},
	onItemClick: ((RecipeListItem) -> Unit) = {},
	onSettingsClick: (() -> Unit) = {},
	onSearched: ((term: String) -> Unit) = {},
) {
	var searching by remember { mutableStateOf(false) }

	Scaffold(
		floatingActionButton = {
			FloatingActionButton(onClick = floatingButtonClick) {
				Icon(painterResource(R.drawable.ic_plus), "Add Recipe")
			}
		}) { paddingValues ->
		CollapsingToolbarScaffold(Modifier.padding(paddingValues),
			state = rememberCollapsingToolbarScaffoldState(),
			scrollStrategy = ScrollStrategy.EnterAlways,
			toolbar = {
				com.google.accompanist.insets.ui.TopAppBar(
					contentPadding = rememberToolbarPadding(),
					title = {
						Crossfade(searching, Modifier.fillMaxSize()) {
							Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
								if (it) {
									ToolbarTextField(
										value = searchTerm,
										onValueChange = onSearched,
										Modifier.weight(1f),
										placeholder = "Search"
									)
									CookIconButton(
										onClick = {
											searching = false
											onSearched("")
										},
										iconResource = R.drawable.ic_close,
										contentDescription = "Clear Search"
									)
								} else {
									Text(stringResource(R.string.title_recipe), Modifier.weight(1f))
									CookIconButton(
										onClick = { searching = true },
										iconResource = R.drawable.ic_magnify,
										contentDescription = "Search Recipes"
									)
								}
							}
						}
					},
					navigationIcon = {
						CookIconButton(
							onClick = onSettingsClick,
							iconResource = R.drawable.ic_settings_outline,
							contentDescription = "Settings"
						)
					}
				)
			}) {
			LazyVerticalGrid(
				columns = GridCells.Adaptive(350.dp),
				contentPadding = PaddingValues(4.dp, 4.dp, 4.dp, (4 + 56).dp),
				content = {
					items(recipes) { item ->
						key(item.recipe.id) {
							// TODO Animate recipe image between views
							RecipeListItem(item, imageLoader, onClick = onItemClick)
						}
					}
				})
		}
	}
}


@Preview
@Composable
private fun Preview() {
	AppTheme {
		RecipeListContent(
			listOf(RecipeListItem(click.alchemist.cook.model.firestore.Recipe("Recipe"))),
			"",
			{ BlobModel.empty }
		)
	}
}