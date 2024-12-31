package click.alchemist.cook.ui.recipe.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.CookIconButton
import click.alchemist.cook.compose.recipe.detail.RecipeListItem
import click.alchemist.cook.model.BlobModel
import click.alchemist.cook.model.Recipe
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel


@Composable
fun RecipeList(
	onSettingsClick: () -> Unit,
	onRecipeClick: (RecipeListItem) -> Unit,
	onAddRecipe: () -> Unit,
	sharedTransitionScope: SharedTransitionScope,
	animatedContentScope: AnimatedVisibilityScope,
) {
	val viewModel = koinViewModel<RecipeListViewModel>()
	val recipes by viewModel.recipes.collectAsState(initial = emptyList())
	val searchTerm by viewModel.search.collectAsState()

	val scope = rememberCoroutineScope()

	RecipeListContent(
		recipes = recipes,
		searchTerm = searchTerm,
		imageLoader = viewModel::loadImage,
		floatingButtonClick = onAddRecipe,
		onItemClick = onRecipeClick,
		onSettingsClick = onSettingsClick,
		onSearched = { scope.launch { viewModel.search.emit(it) } },
		sharedTransitionScope = sharedTransitionScope,
		animatedContentScope = animatedContentScope
	)
}


@Composable
fun RecipeListContent(
	recipes: List<RecipeListItem>,
	searchTerm: String,
	imageLoader: suspend (Recipe) -> BlobModel,
	floatingButtonClick: () -> Unit = {},
	onItemClick: (RecipeListItem) -> Unit = {},
	onSettingsClick: () -> Unit = {},
	onSearched: (term: String) -> Unit = {},
	sharedTransitionScope: SharedTransitionScope,
	animatedContentScope: AnimatedVisibilityScope,
) {
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

	Scaffold(
		contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
		//modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
		floatingActionButton = {
			FloatingActionButton(onClick = floatingButtonClick) {
				Icon(painterResource(R.drawable.ic_plus), "Add Recipe")
			}
		},
		topBar = {
			Box(modifier = Modifier.fillMaxWidth()) {
				SearchBar(
					inputField = {
						SearchBarDefaults.InputField(
							query = searchTerm,
							onQueryChange = onSearched,
							onSearch = onSearched,
							expanded = false,
							onExpandedChange = {},
							placeholder = { Text("Search") },
							leadingIcon = {
								CookIconButton(
									onClick = onSettingsClick,
									iconResource = R.drawable.ic_settings_outline,
									contentDescription = "Settings"
								)
							},
							trailingIcon = {
								CookIconButton(
									onClick = {
										onSearched("")
									},
									iconResource = R.drawable.ic_close,
									contentDescription = "Clear Search"
								)
							})
					},
					expanded = false,
					onExpandedChange = {},
					modifier = Modifier.align(Alignment.Center)
				) {
				}
			}
		}) { paddingValues ->
		LazyVerticalGrid(
			columns = GridCells.Adaptive(350.dp),
			contentPadding = paddingValues,
			content = {
				items(recipes) { item ->
					key(item.recipe.id) {
						RecipeListItem(
							item = item,
							imageLoader = imageLoader,
							onClick = onItemClick,
							sharedTransitionScope = sharedTransitionScope,
							animatedContentScope = animatedContentScope,
							modifier = Modifier.animateItem()
						)
					}
				}
			})
	}
}


@Preview
@Composable
private fun Preview() {
	AppTheme {
		SharedTransitionLayout {
			AnimatedContent(targetState = true) {
				RecipeListContent(
					listOf(RecipeListItem(Recipe("Recipe"))),
					"",
					{ BlobModel.empty },
					sharedTransitionScope = this@SharedTransitionLayout,
					animatedContentScope = this@AnimatedContent
				)
			}
		}
	}
}