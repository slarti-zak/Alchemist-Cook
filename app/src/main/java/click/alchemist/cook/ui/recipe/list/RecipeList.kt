package click.alchemist.cook.ui.recipe.list

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.CookIconButton
import click.alchemist.cook.compose.ToolbarTextField
import click.alchemist.cook.compose.recipe.detail.RecipeListItem
import click.alchemist.cook.model.BlobModel
import click.alchemist.cook.model.Recipe
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel


@Composable
fun RecipeList(onSettingsClick: () -> Unit, onRecipeClick: (RecipeListItem) -> Unit, onAddRecipe: () -> Unit) {
	val viewModel = koinViewModel<RecipeListViewModel>()
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
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

	Scaffold(
		contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
		modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
		floatingActionButton = {
			FloatingActionButton(onClick = floatingButtonClick) {
				Icon(painterResource(R.drawable.ic_plus), "Add Recipe")
			}
		},
		topBar = {
			TopAppBar(
				title = {
					Crossfade(
						targetState = searching || searchTerm.isNotEmpty(),
						modifier = Modifier.fillMaxSize(),
						label = "RecipeListSearch"
					) { visible ->
						Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
							if (visible) {
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
				},
				scrollBehavior = scrollBehavior
			)
		}) { paddingValues ->
			LazyVerticalGrid(
				columns = GridCells.Adaptive(350.dp),
				contentPadding = paddingValues,
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


@Preview
@Composable
private fun Preview() {
	AppTheme {
		RecipeListContent(
			listOf(RecipeListItem(Recipe("Recipe"))),
			"",
			{ BlobModel.empty }
		)
	}
}