package click.alchemist.cook.ui.cooking.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import click.alchemist.cook.R
import click.alchemist.cook.compose.*
import click.alchemist.cook.compose.ingredient.IngredientWithAmount
import click.alchemist.cook.compose.recipe.FloatingCookingButton
import click.alchemist.cook.compose.recipe.RecipeExtendedInstructions
import click.alchemist.cook.compose.recipe.detail.RecipeImage
import click.alchemist.cook.compose.timer.TimerItem
import click.alchemist.cook.model.BlobModel
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.service.markdown.MarkdownService
import click.alchemist.cook.viewmodel.TimerModel
import com.google.accompanist.insets.statusBarsHeight
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import java.text.DateFormat
import java.util.*


@Composable
fun CookingList() {
	val markdownService = get<MarkdownService>()
	val viewModel = getViewModel<CookingListViewModel>()

	val hasExtendedGraph by viewModel.hasExtendedGraph.collectAsState(false)
	val recipes by viewModel.recipes.collectAsState(emptyList())

	val scope = rememberCoroutineScope()

	CookingListContent(
		hasExtendedGraph,
		recipes,
		viewModel::subscribeRecipe,
		viewModel::loadImage,
		onFinishRecipeClick = { scope.launch { viewModel.onFinishRecipeClick(it) } },
		onTimerClick = { item, timer -> scope.launch { viewModel.onTimerClick(item, timer) } },
		onAddMinute = viewModel::addMinute,
		markdownService = markdownService
	)
}


@Composable
private fun CookingListContent(
	hasExtendedGraph: Boolean,
	recipes: List<String>,
	recipeGetter: (String) -> Flow<CookingRecipeListItem>,
	imageLoader: suspend (Recipe) -> BlobModel,
	onFinishRecipeClick: (CookingRecipeListItem) -> Unit,
	onTimerClick: (CookingRecipeListItem, TimerModel) -> Unit,
	onAddMinute: (TimerModel) -> Unit,
	markdownService: MarkdownService? = null
) {

	val extendedGraphOffset = if (hasExtendedGraph) 1 else 0
	val pageCount = recipes.size + extendedGraphOffset
	val pagerState = rememberPagerState()

	Box {
		Column(
			Modifier
				.fillMaxSize()
		) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.statusBarsHeight()
					.background(MaterialTheme.colors.primary)
					.zIndex(1f)
			)

			CollapsingToolbarScaffold(
				modifier = Modifier,
				state = rememberCollapsingToolbarScaffoldState(),
				scrollStrategy = ScrollStrategy.EnterAlways,
				toolbar = {
					com.google.accompanist.insets.ui.TopAppBar(
						title = { Text(stringResource(R.string.title_cooking)) },
					)
				}) {
				if (recipes.isEmpty()) {
					NothingCooking(Modifier)
				} else {
					Box(
						Modifier
							.fillMaxSize()
					) {
						HorizontalPager(
							state = pagerState,
							count = pageCount,
							key = { page ->
								if (page == 0 && hasExtendedGraph) {
									"Extended"
								} else {
									val recipeIndex = page - extendedGraphOffset
									if (recipeIndex >= 0 && recipeIndex <= recipes.lastIndex) {
										recipes[recipeIndex]
									}
									page
								}
							}) { page ->
							if (page == 0 && hasExtendedGraph) {
								ExtendedItem()
							} else {
								val recipeIndex = page - extendedGraphOffset
								if (recipeIndex >= 0 && recipeIndex <= recipes.lastIndex) {
									val recipeId = recipes[recipeIndex]
									RecipeItem(
										recipeId,
										recipeGetter,
										imageLoader,
										onFinishRecipeClick,
										onTimerClick = onTimerClick,
										onAddMinute = onAddMinute,
										markdownService = markdownService
									)
								}
							}
						}
					}
				}
			}
		}

		if (recipes.isNotEmpty()) {
			HorizontalPagerIndicator(
				pagerState = pagerState,
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.padding(16.dp),
			)
		}
	}
//	Scaffold(topBar = {
//		com.google.accompanist.insets.ui.TopAppBar(
//			contentPadding = rememberToolbarPadding(),
//			title = { Text(stringResource(R.string.title_cooking)) },
//		)
//	}) { paddingValues ->
//		if (recipes.isEmpty()) {
//			NothingCooking(Modifier.padding(paddingValues))
//		} else {
//			Box(
//				Modifier
//					.fillMaxSize()
//					.padding(paddingValues)
//			) {
//				val extendedGraphOffset = if (hasExtendedGraph) 1 else 0
//				val pageCount = recipes.size + extendedGraphOffset
//				val pagerState = rememberPagerState(pageCount = pageCount)
//				HorizontalPager(state = pagerState) { page ->
//					if (page == 0 && hasExtendedGraph) {
//						ExtendedItem()
//					} else {
//						val recipeIndex = page - extendedGraphOffset
//						if (recipeIndex >= 0 && recipeIndex <= recipes.lastIndex) {
//							val recipeId = recipes[recipeIndex]
//							RecipeItem(
//								recipeId,
//								recipeGetter,
//								imageLoader,
//								onFinishRecipeClick,
//								onTimerClick = onTimerClick,
//								onAddMinute = onAddMinute,
//								markdownService = markdownService
//							)
//						}
//					}
//				}
//
//				HorizontalPagerIndicator(
//					pagerState = pagerState,
//					modifier = Modifier
//						.align(Alignment.BottomCenter)
//						.padding(16.dp),
//				)
//			}
//		}
//	}
}


@Composable
private fun ExtendedItem() {
	val viewModel = getViewModel<CookingListExtendedItemViewModel>()
	val item by viewModel.extendedGraph.collectAsState(initial = null)
	val extendedItem = item ?: return

	val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
	val scope = rememberCoroutineScope()

	Column(
		modifier = Modifier
			.fillMaxSize()
	) {
		Row(
			Modifier
				.fillMaxWidth()
				.padding(8.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Column {
				if (extendedItem.startedAt > 0) {
					Row {
						Column {
							Text("Start:")
							Text("End:")
						}
						Column(Modifier.padding(start = 8.dp)) {
							Text(timeFormat.format(Date(extendedItem.startedAt)))
							Text(timeFormat.format(Date(extendedItem.graph.endAt)))
						}
					}
				} else {
					Text("Not Started")
					Text(buildAnnotatedString {
						append("End: ")
						append(timeFormat.format(Date(extendedItem.graph.endAt)))
						append(" (projected)")
					})
				}
			}

			Button(onClick = { scope.launch { viewModel.onStartCooking(extendedItem) } }) {
				Text(stringResource(if (extendedItem.graph.isPreview) R.string.start_cooking else R.string.stop_cooking))
			}
		}

		Divider()

		RecipeExtendedInstructions(
			graphModel = extendedItem.graph,
			modifier = Modifier
				.weight(1f),
			onFinished = { scope.launch { viewModel.onCookingItemFinished(it) } },
			onTimerToggle = { scope.launch { viewModel.onCookingItemTimer(it) } },
			onAddMinute = viewModel::onAddMinute,
			contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 30.dp)
		)
	}
}


@Composable
private fun RecipeItem(
	recipeId: String,
	recipeGetter: (String) -> Flow<CookingRecipeListItem>,
	imageLoader: suspend (Recipe) -> BlobModel = { BlobModel.empty },
	onFinishRecipeClick: (CookingRecipeListItem) -> Unit = {},
	onTimerClick: (CookingRecipeListItem, TimerModel) -> Unit = { _, _ -> },
	onAddMinute: (TimerModel) -> Unit = {},
	markdownService: MarkdownService? = null
) {
//    val recipeFlow = remember(recipeId) { recipeGetter(recipeId) }
	val recipeItem by recipeGetter(recipeId).collectAsState(null)
	val recipe = recipeItem
	if (recipe == null) {
		Box(Modifier.fillMaxSize())
	} else {
		RecipeItem(recipe, imageLoader, onFinishRecipeClick, onTimerClick, onAddMinute, markdownService)
	}
}


@Composable
private fun RecipeItem(
	recipeItem: CookingRecipeListItem,
	imageLoader: suspend (Recipe) -> BlobModel = { BlobModel.empty },
	onFinishRecipeClick: (CookingRecipeListItem) -> Unit = {},
	onTimerClick: (CookingRecipeListItem, TimerModel) -> Unit = { _, _ -> },
	onAddMinute: (TimerModel) -> Unit = {},
	markdownService: MarkdownService? = null
) {
	val recipe = recipeItem.recipe
	val scrollState = rememberScrollState()

	BoxWithConstraints(
		Modifier
			.fillMaxSize()
			.verticalScroll(scrollState)
			.padding(top = 8.dp, bottom = 30.dp, start = 8.dp, end = 8.dp)
	) {
		Card {
			val isWide = maxWidth > 400.dp

			Column {
				Box(contentAlignment = Alignment.BottomEnd) {
					RecipeImage(recipe, imageLoader, Modifier.aspectRatio(3f))
					FloatingCookingButton(isPlaning = true, { onFinishRecipeClick(recipeItem) }, Modifier.padding(8.dp))
				}

				if (isWide) {
					Row(Modifier.padding(8.dp)) {
						Column(Modifier.weight(0.7f)) {
							Text(recipe.name.ifBlank { stringResource(R.string.list_item_empty) })
							RecipeText(
								recipe.content,
								Modifier
									.fillMaxWidth()
									.padding(8.dp),
								markdownService = markdownService,
								if (isWide) 18.sp else 12.sp
							)
						}
						Column(Modifier.weight(0.3f), horizontalAlignment = Alignment.End) {
							RecipeItemIngredients(recipeItem)
							RecipeItemTimers(recipeItem, onClick = { onTimerClick(recipeItem, it) }, onAddMinute)
						}
					}
				} else {
					Column(Modifier.padding(8.dp)) {

						Text(recipe.name.ifBlank { stringResource(R.string.list_item_empty) })
						RecipeText(
							recipe.content, Modifier.fillMaxWidth(), markdownService = markdownService, if (isWide) 18.sp else 12.sp
						)

						RecipeItemIngredients(recipeItem)
						RecipeItemTimers(recipeItem, onClick = { onTimerClick(recipeItem, it) }, onAddMinute)
					}
				}
			}
		}
	}
}


@Composable
private fun RecipeItemIngredients(recipeItem: CookingRecipeListItem) {
	if (recipeItem.ingredients.isNotEmpty()) {
		HeaderFilled(stringResource(R.string.cooking_ingredients_title, recipeItem.servings), Modifier.padding(vertical = 8.dp))
		for (ingredient in recipeItem.ingredients) {
			if (ingredient.unitCategory == IngredientCategory.HEADER) {
				Header(ingredient.name)
			} else {
				IngredientWithAmount(ingredient.ingredient, ingredient.amount)
			}
		}
	}
}


@Composable
private fun RecipeItemTimers(recipeItem: CookingRecipeListItem, onClick: (TimerModel) -> Unit, onAddMinute: (TimerModel) -> Unit) {
	if (recipeItem.timers.isNotEmpty()) {
		HeaderFilled(stringResource(R.string.recipe_tab_timer_title), Modifier.padding(vertical = 8.dp))
		for (timer in recipeItem.timers) {
			TimerItem(Modifier.padding(vertical = 4.dp), timer, onClick = onClick, onAddMinute = onAddMinute)
		}
	}
}

@Composable
private fun NothingCooking(modifier: Modifier = Modifier) {
	Column(
		modifier.then(
			Modifier
				.fillMaxSize()
				.padding(8.dp)
		),
		verticalArrangement = Arrangement.spacedBy(32.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Spacer(Modifier.weight(0.7f))
		Text(stringResource(R.string.list_item_empty_cooking), style = MaterialTheme.typography.h4, textAlign = TextAlign.Center)
		Image(
			painterResource(R.drawable.logo), contentDescription = "Logo",
			Modifier
				.fillMaxWidth()
				.weight(0.8f), contentScale = ContentScale.Fit
		)
		Spacer(Modifier.weight(1.1f))
	}
}


@Preview("Empty")
@Composable
private fun PreviewEmpty() {
	AppTheme {
		Surface(Modifier.fillMaxSize()) {
			NothingCooking()
		}
	}
}


@Preview("RecipeItem", group = "Recipe")
@Composable
private fun PreviewRecipeItem() {
	AppTheme {
		Surface(Modifier.fillMaxSize()) {
			RecipeItem(
				CookingRecipeListItem(
					Recipe("Recipe"),
					4,
					previewIngredients(),
					previewTimers()
				),
			)
		}
	}
}


@Preview("RecipeItemWide", widthDp = 800, group = "Recipe")
@Composable
private fun PreviewRecipeItemWide() {
	AppTheme {
		Surface(Modifier.fillMaxSize()) {
			RecipeItem(
				CookingRecipeListItem(
					Recipe("Recipe"),
					4,
					previewIngredients(),
					previewTimers()
				),
			)
		}
	}
}


@Preview("Extended")
@Composable
private fun PreviewExtended() {
	AppTheme {
		Surface(Modifier.fillMaxSize()) {
//            ExtendedItem(ExtendedCookingListItem(previewExtendedInstruction()))
		}
	}
}