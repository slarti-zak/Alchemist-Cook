package click.alchemist.cook.ui.recipe.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.BackButton
import click.alchemist.cook.compose.CookIconButton
import click.alchemist.cook.compose.HeaderFilled
import click.alchemist.cook.compose.previewIngredients
import click.alchemist.cook.compose.previewTimers
import click.alchemist.cook.compose.recipe.FloatingCookingButton
import click.alchemist.cook.compose.recipe.detail.RecipeImage
import click.alchemist.cook.extension.isNotNullOrBlank
import click.alchemist.cook.model.BlobModel
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.model.RecipeGraphNode
import click.alchemist.cook.service.markdown.MarkdownService
import click.alchemist.cook.viewmodel.IngredientModel
import click.alchemist.cook.viewmodel.RecipeGraphModel
import click.alchemist.cook.viewmodel.RecipeGraphNodeModel
import click.alchemist.cook.viewmodel.TimerModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf


@Composable
fun RecipeDetail(
	recipeId: String,
	onBackNavigation: () -> Unit,
	onEdit: () -> Unit,
	navigateShopping: (recipeId: String, recipeServings: Int, servings: Int) -> Unit,
	sharedTransitionScope: SharedTransitionScope,
	animatedContentScope: AnimatedVisibilityScope
) {
	val markdownService = koinInject<MarkdownService>()
	val viewModel = koinViewModel<RecipeDetailViewModel>(parameters = { parametersOf(recipeId) })

	val recipe by viewModel.recipe.collectAsState(null)
	val servings by viewModel.servings.collectAsState(1)

	val ingredients by viewModel.ingredients.collectAsState(emptyList())
	val timers by viewModel.timers.collectAsState(emptyList())

	LaunchedEffect(viewModel.closeEvent) {
		viewModel.closeEvent.first()
		onBackNavigation()
	}

	val scope = rememberCoroutineScope()
	RecipeDetailContent(
		recipe,
		viewModel.image,
		servings,
		ingredients,
		timers,
		viewModel.extraInstructions,
		viewModel.isPlanning,
		onBackNavigation = onBackNavigation,
		onEdit = onEdit,
		onDelete = { scope.launch { viewModel.delete() } },
		onServingChanged = { scope.launch { viewModel.userServings.emit(it) } },
		onTimerClick = { scope.launch { viewModel.toggleTimer(it) } },
		onTimerAddMinute = viewModel::addTimerMinute,
		onShoppingClick = {
			val localRecipe = recipe
			if (localRecipe != null) {
				navigateShopping(
					recipeId,
					localRecipe.serves.coerceAtLeast(1),
					servings
				)
			}
		},
		floatingButtonClick = { scope.launch { viewModel.toggleCooking() } },
		markdownService = markdownService,
		sharedTransitionScope = sharedTransitionScope,
		animatedContentScope = animatedContentScope
	)
}

@Composable
private fun RecipeDetailContent(
	recipe: Recipe?,
	recipeImageData: Flow<BlobModel>,
	servings: Int,
	ingredients: List<IngredientModel>,
	timers: List<TimerModel>,
	extendedData: Flow<RecipeGraphModel>,
	isPlaningData: Flow<Boolean>,
	onBackNavigation: () -> Unit = { },
	floatingButtonClick: () -> Unit = {},
	onEdit: () -> Unit = { },
	onDelete: () -> Unit = { },
	onServingChanged: (Int) -> Unit = {},
	onShoppingClick: () -> Unit = {},
	onTimerClick: (TimerModel) -> Unit = {},
	onTimerAddMinute: (TimerModel) -> Unit = {},
	markdownService: MarkdownService? = null,
	sharedTransitionScope: SharedTransitionScope,
	animatedContentScope: AnimatedVisibilityScope
) {
	var deleteDialog by rememberSaveable { mutableStateOf(false) }

	val isPlaning by isPlaningData.collectAsState(false)
	val recipeImage by recipeImageData.collectAsState(BlobModel.empty)

	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
	val isCollapsed = remember { derivedStateOf { scrollBehavior.state.collapsedFraction > 0.45 } }

	Scaffold(
		topBar = {
			LargeTopAppBar(
				title = {
					Box {
//						if (!isCollapsed.value) {
							with(sharedTransitionScope) {
								RecipeImage(
									recipeImage,
									Modifier
										//.padding(top = 56.dp)
										.fillMaxSize()
										.sharedElement(
											rememberSharedContentState(key = "recipeImage-${recipe?.id}"),
											animatedVisibilityScope = animatedContentScope,
											zIndexInOverlay = 50f
										)
										.zIndex(50f)
									//.height(150.dp)
								)
							}

							Text(
								text = (recipe?.name ?: "").ifBlank { stringResource(R.string.list_item_empty) },
								textAlign = TextAlign.Center,
								modifier = Modifier
									.align(Alignment.BottomCenter)
									.fillMaxWidth()
									.background(Color(0, 0, 0, 50))
									.padding(8.dp, 4.dp)
									.zIndex(100f),
								color = Color(255, 255, 255, 255),
								style = MaterialTheme.typography.titleLarge,
								maxLines = 2
							)
//						} else {
//							Text(text = (recipe?.name ?: "").ifBlank { stringResource(R.string.list_item_empty) })
//						}
					}
				},
				expandedHeight = 200.dp,
				navigationIcon = { BackButton(onBackNavigation) },
				scrollBehavior = scrollBehavior,
				actions = {
					CookIconButton(
						onClick = onEdit,
						iconResource = R.drawable.ic_pencil,
						contentDescription = "Edit"
					)
					CookIconButton(
						onClick = { deleteDialog = true },
						iconResource = R.drawable.ic_delete,
						contentDescription = "Delete"
					)
				}
			)
		},
		floatingActionButton = { FloatingCookingButton(isPlaning, floatingButtonClick) },
		modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
	)
	{ paddingValues ->
		Column(Modifier.padding(paddingValues)) {
			/*Box(
				modifier = Modifier
					.fillMaxWidth()
					.windowInsetsTopHeight(WindowInsets.statusBars)
					.background(MaterialTheme.colorScheme.primary)
					.zIndex(1f)
			)*/

			if (recipe == null) {
				Box(
					Modifier
						.fillMaxSize()
						.padding(bottom = 150.dp)
				) {
					CircularProgressIndicator(Modifier.align(Alignment.Center))
				}
				return@Column
			}
			val extendedInstructions by extendedData.collectAsState(null)

			val hasInstructions = recipe.content.isNotNullOrBlank()
			val hasIngredients = ingredients.isNotEmpty()
			val hasTimers = timers.isNotEmpty()
			val hasExtendedInstructions = (extendedInstructions?.nodes?.size ?: 0) > 0

			BoxWithConstraints {
				val isWide = maxWidth >= 600.dp
				Column {
					val tabs = mutableListOf<RecipeTab>()
						.apply {
							if (isWide) {
								if (hasInstructions) {
									add(RecipeTab.Instructions)
								}
								if (hasExtendedInstructions) {
									add(RecipeTab.ExtendedInstructions)
								}
							} else {
								if (hasInstructions) {
									add(RecipeTab.Instructions)
								}
								if (hasExtendedInstructions) {
									add(RecipeTab.ExtendedInstructions)
								}
								if (hasIngredients) {
									add(RecipeTab.Ingredients)
								}
								if (hasTimers) {
									add(RecipeTab.Timer)
								}
							}
						}

					Row {
						RecipeContentTabs(
							Modifier.weight(2f),
							isWide,
							tabs = tabs,
							recipe = recipe,
							markdownService = markdownService,
							extendedInstructions = extendedInstructions,
							servings = servings,
							ingredients = ingredients,
							onServingChanged = onServingChanged,
							onShoppingClick = onShoppingClick,
							timers = timers,
							onTimerClick = onTimerClick,
							onTimerAddMinute = onTimerAddMinute
						)
						if (isWide && (hasTimers || hasIngredients)) {
							LazyColumn(
								Modifier
									.weight(1f)
									.widthIn(max = 300.dp),
								contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 100.dp),
								verticalArrangement = Arrangement.spacedBy(8.dp),
								content = {
									if (hasIngredients) {
										item {
											HeaderFilled(
												stringResource(R.string.cooking_ingredients_title, servings),
												Modifier.padding(vertical = 8.dp)
											)
										}
										recipeDetailIngredientListContent(servings, onServingChanged, onShoppingClick, ingredients)
									}

									if (hasTimers) {
										item {
											HeaderFilled(stringResource(R.string.recipe_tab_timer_title), Modifier.padding(vertical = 8.dp))
										}
										recipeDetailTimerListContent(timers, onTimerClick, onTimerAddMinute)
									}
								})
						}
					}
				}
			}
		}
	}

	if (deleteDialog) {
		DeleteDialog({ deleteDialog = false }, onDelete)
	}
}


@Composable
private fun RecipeContentTabs(
	modifier: Modifier,
	isWide: Boolean,
	tabs: List<RecipeTab>,
	recipe: Recipe?,
	markdownService: MarkdownService?,
	extendedInstructions: RecipeGraphModel?,
	servings: Int,
	ingredients: List<IngredientModel>,
	onServingChanged: (Int) -> Unit,
	onShoppingClick: () -> Unit,
	timers: List<TimerModel>,
	onTimerClick: (TimerModel) -> Unit,
	onTimerAddMinute: (TimerModel) -> Unit
) {
	if (tabs.isEmpty() || recipe == null) {
		return
	}

	Column(modifier) {
		if (tabs.size == 1) {
			SelectRecipeContentTab(
				tabs[0],
				isWide,
				recipe,
				markdownService,
				extendedInstructions,
				servings,
				ingredients,
				onServingChanged,
				onShoppingClick,
				timers,
				onTimerClick,
				onTimerAddMinute
			)
		} else {
			val pagerState = rememberPagerState(
				initialPage = 0,
				pageCount = { tabs.size })
			TabRow(selectedTabIndex = pagerState.currentPage,
				indicator = { tabPositions ->
					TabRowDefaults.SecondaryIndicator(
						modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
						color = MaterialTheme.colorScheme.secondary
					)
				}) {
				tabs.forEachIndexed { index, recipeTab ->
					when (recipeTab) {
						RecipeTab.Instructions -> RecipeTab(stringResource(R.string.recipe_tab_instructions_title), index, pagerState)
						RecipeTab.ExtendedInstructions -> RecipeTab(
							stringResource(R.string.recipe_tab_instructions_extended_title),
							index,
							pagerState
						)

						RecipeTab.Ingredients -> RecipeTab(stringResource(R.string.recipe_tab_ingredients_title), index, pagerState)
						RecipeTab.Timer -> RecipeTab(stringResource(R.string.recipe_tab_timer_title), index, pagerState)
					}
				}
			}

			HorizontalPager(
				state = pagerState,
				verticalAlignment = Alignment.Top,
				key = { pageIndex ->
					if (pageIndex < tabs.size) tabs[pageIndex] else pageIndex
				}) { pageIndex ->
				val tab = if (pageIndex < tabs.size) tabs[pageIndex] else return@HorizontalPager
				SelectRecipeContentTab(
					tab,
					isWide,
					recipe,
					markdownService,
					extendedInstructions,
					servings,
					ingredients,
					onServingChanged,
					onShoppingClick,
					timers,
					onTimerClick,
					onTimerAddMinute
				)
			}
		}
	}
}


@Composable
private fun SelectRecipeContentTab(
	tab: RecipeTab,
	isWide: Boolean,
	recipe: Recipe,
	markdownService: MarkdownService?,
	extendedInstructions: RecipeGraphModel?,
	servings: Int,
	ingredients: List<IngredientModel>,
	onServingChanged: (Int) -> Unit,
	onShoppingClick: () -> Unit,
	timers: List<TimerModel>,
	onTimerClick: (TimerModel) -> Unit,
	onTimerAddMinute: (TimerModel) -> Unit
) {
	when (tab) {
		RecipeTab.Instructions ->
			RecipeDetailInstruction(
				recipe.content,
				Modifier
					.fillMaxSize(),
				markdownService,
				if (isWide) 18.sp else MaterialTheme.typography.bodyLarge.fontSize
			)

		RecipeTab.ExtendedInstructions -> RecipeDetailExtendedInstruction(extendedInstructions!!, markdownService)
		RecipeTab.Ingredients -> RecipeDetailIngredientList(servings, ingredients, onServingChanged, onShoppingClick)
		RecipeTab.Timer -> RecipeDetailTimerList(timers, onTimerClick, onTimerAddMinute)
	}
}


@Composable
fun RecipeTab(title: String, tabIndex: Int, pagerState: PagerState) {
	val scope = rememberCoroutineScope()
	Tab(selected = (pagerState.targetPage) == tabIndex,
		onClick = { scope.launch { pagerState.animateScrollToPage(tabIndex) } }) {
		Text(title, Modifier.padding(vertical = 8.dp), textAlign = TextAlign.Center)
	}
}


@Composable
private fun DeleteDialog(
	onDismiss: () -> Unit,
	onYes: () -> Unit = {}
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = { TextButton(onClick = onYes) { Text(stringResource(R.string.general_accept)) } },
		dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.general_cancel)) } },
		title = { Text("Delete Recipe?") },
		properties = DialogProperties(usePlatformDefaultWidth = true)
	)
}


@Preview(name = "Normal")
@Composable
private fun Preview() {
	AppTheme {
		SharedTransitionLayout {
			AnimatedContent(targetState = true) {
				RecipeDetailContent(
					recipe = Recipe("My Recipe", "My Instructions"),
					recipeImageData = MutableStateFlow(BlobModel.empty),
					servings = 1,
					ingredients = previewIngredients(),
					timers = previewTimers(),
					extendedData = MutableStateFlow(
						RecipeGraphModel(
							nodes = listOf(RecipeGraphNodeModel(RecipeGraphNode("id1", "Instruction"), "")),
							true
						)
					),
					isPlaningData = MutableStateFlow(false),
					sharedTransitionScope = this@SharedTransitionLayout,
					animatedContentScope = this@AnimatedContent
				)
			}
		}
	}
}


@Preview(name = "Wide", widthDp = 1000, heightDp = 600)
@Composable
private fun PreviewWide() {
	AppTheme {
		SharedTransitionLayout {
			AnimatedContent(targetState = false) {
				RecipeDetailContent(
					recipe = Recipe("My Recipe Wide", "My Instructions"),
					recipeImageData = MutableStateFlow(BlobModel.empty),
					servings = 1,
					ingredients = previewIngredients(),
					timers = previewTimers(),
					extendedData = MutableStateFlow(
						RecipeGraphModel(
							nodes = listOf(RecipeGraphNodeModel(RecipeGraphNode("id1", "Instruction"), "")),
							true
						)
					),
					isPlaningData = MutableStateFlow(false),
					sharedTransitionScope = this@SharedTransitionLayout,
					animatedContentScope = this@AnimatedContent
				)
			}
		}
	}
}


@Preview(name = "Wide no extended", widthDp = 1000, heightDp = 600)
@Composable
private fun PreviewWideNoExtended() {
	AppTheme {
		SharedTransitionLayout {
			AnimatedContent(targetState = false) {
				RecipeDetailContent(
					recipe = Recipe("My Recipe Wide", "My Instructions"),
					recipeImageData = MutableStateFlow(BlobModel.empty),
					servings = 1,
					ingredients = previewIngredients(),
					timers = previewTimers(),
					extendedData = MutableStateFlow(RecipeGraphModel(emptyList(), true)),
					isPlaningData = MutableStateFlow(false),
					sharedTransitionScope = this@SharedTransitionLayout,
					animatedContentScope = this@AnimatedContent
				)
			}
		}
	}
}


