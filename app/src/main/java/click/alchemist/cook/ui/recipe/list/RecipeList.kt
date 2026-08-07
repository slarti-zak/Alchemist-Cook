package click.alchemist.cook.ui.recipe.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.CookIconButton
import click.alchemist.cook.compose.recipe.detail.RecipeListItem
import click.alchemist.cook.model.Recipe
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File


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
	imageLoader: suspend (Recipe) -> File?,
	floatingButtonClick: () -> Unit = {},
	onItemClick: (RecipeListItem) -> Unit = {},
	onSettingsClick: () -> Unit = {},
	onSearched: (term: String) -> Unit = {},
	sharedTransitionScope: SharedTransitionScope,
	animatedContentScope: AnimatedVisibilityScope,
) {
	var focussed by remember { mutableStateOf(false) }
	val searchBarContainerColor = SearchBarDefaults.colors().containerColor
	val searchBarColor by animateColorAsState(
		if (focussed) searchBarContainerColor else searchBarContainerColor.copy(alpha = 0.6f)
	)

	Scaffold(
		contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
		floatingActionButton = {
			with(sharedTransitionScope) {
				FloatingActionButton(
					onClick = floatingButtonClick, modifier = Modifier
						.sharedBounds(
							rememberSharedContentState(key = "create-recipe"),
							animatedVisibilityScope = animatedContentScope,
							enter = fadeIn() + slideInVertically {
								it
							},
							exit = fadeOut() + slideOutVertically {
								it
							},
							resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
						)
						.skipToLookaheadSize()
				) {
					Icon(painterResource(R.drawable.ic_plus), "Add Recipe")
				}
			}
		},
		topBar = {
			Box(modifier = Modifier.fillMaxWidth()) {
				SearchBar(
					inputField = {
						SearchBarDefaults.InputField(
							modifier = Modifier.onFocusChanged { focussed = it.hasFocus },
							query = searchTerm,
							onQueryChange = onSearched,
							onSearch = { recipes.firstOrNull()?.apply(onItemClick) },
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
					modifier = Modifier.align(Alignment.Center),
					colors = SearchBarDefaults.colors(containerColor = searchBarColor)
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
					{ null },
					sharedTransitionScope = this@SharedTransitionLayout,
					animatedContentScope = this@AnimatedContent
				)
			}
		}
	}
}