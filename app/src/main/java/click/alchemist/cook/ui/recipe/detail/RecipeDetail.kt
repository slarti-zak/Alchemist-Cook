package click.alchemist.cook.ui.recipe.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import click.alchemist.cook.R
import click.alchemist.cook.compose.*
import click.alchemist.cook.compose.recipe.FloatingCookingButton
import click.alchemist.cook.compose.recipe.detail.RecipeImage
import click.alchemist.cook.extension.isNotNullOrBlank
import click.alchemist.cook.model.BlobModel
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.model.RecipeGraphNode
import click.alchemist.cook.service.markdown.MarkdownService
import click.alchemist.cook.viewmodel.*
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun RecipeDetail(
	recipeId: String,
	onBackNavigation: () -> Unit,
	onEdit: () -> Unit,
	navigateShopping: (recipeId: String, servings: Serving) -> Unit
) {
	val markdownService = get<MarkdownService>()
	val viewModel = getViewModel<RecipeDetailViewModel>(parameters = { parametersOf(recipeId) })

	val recipe by viewModel.recipe.collectAsState(null)
	val servings by viewModel.servings.collectAsState(1)

	val ingredients by viewModel.ingredients.collectAsState(emptyList())
	val timers by viewModel.timers.collectAsState(emptyList())

//	private val args: RecipeDetailFragmentArgs by navArgs()
//	private var openTimers = false

//	override fun onCreate(savedInstanceState: Bundle?) {
//		super.onCreate(savedInstanceState)
//		val transitionInflater = TransitionInflater.from(requireContext())
//		sharedElementEnterTransition = transitionInflater.inflateTransition(android.R.transition.move)
//		sharedElementReturnTransition = transitionInflater.inflateTransition(android.R.transition.move)
//		viewModel.load(args.recipeId)
//		openTimers = args.openTimers
//	}

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
			createServing(recipe, servings)?.let { serving ->
				navigateShopping(recipeId, serving)
			}
		},
		floatingButtonClick = { scope.launch { viewModel.toggleCooking() } },
		markdownService = markdownService
	)
}

//		postponeEnterTransition()
//		viewModel.image.observe(viewLifecycleOwner) {
//			Glide.with(binding.image)
//				.load(it.blob)
//				.placeholder(R.drawable.logo)
//				.fallback(R.drawable.logo)
//				.centerCrop()
//				.addListener(object : RequestListener<Drawable> {
//					override fun onResourceReady(
//						resource: Drawable?,
//						model: Any?,
//						target: Target<Drawable>?,
//						dataSource: DataSource?,
//						isFirstResource: Boolean
//					): Boolean {
//						startPostponedEnterTransition()
//						return false
//					}
//
//					override fun onLoadFailed(
//						e: GlideException?,
//						model: Any?,
//						target: Target<Drawable>?,
//						isFirstResource: Boolean
//					): Boolean {
//						startPostponedEnterTransition()
//						return false
//					}
//				})
//				.into(binding.image)
//		}
//
//		return binding.root


private fun createServing(recipe: Recipe?, userServings: Int): Serving? {
	if (recipe != null) {
		return Serving(recipe.serves.coerceAtLeast(1), userServings)
	}
	return null
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
	markdownService: MarkdownService? = null
) {
	var deleteDialog by rememberSaveable { mutableStateOf(false) }

	val isPlaning by isPlaningData.collectAsState(false)
	val recipeImage by recipeImageData.collectAsState(BlobModel.empty)

	Scaffold(floatingActionButton = { FloatingCookingButton(isPlaning, floatingButtonClick) })
	{ paddingValues ->
		Column {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.windowInsetsTopHeight(WindowInsets.statusBars)
					.background(MaterialTheme.colors.primary)
					.zIndex(1f)
			)
			CollapsingToolbarScaffold(Modifier.padding(paddingValues),
				state = rememberCollapsingToolbarScaffoldState(),
				scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed,
				toolbar = {
					RecipeImage(
						recipeImage,
						Modifier
							.padding(top = 56.dp)
							.fillMaxWidth()
							.height(150.dp)
							.parallax(0.5f)
					)
					com.google.accompanist.insets.ui.TopAppBar(
						title = { Text((recipe?.name ?: "").ifBlank { stringResource(R.string.list_item_empty) }) },
						navigationIcon = { BackButton(onBackNavigation) },
						actions = {
							CookIconButton(
								onClick = onEdit,
								iconResource = R.drawable.ic_pencil,
								contentDescription = "Edit",
								tint = Color.White
							)
							CookIconButton(
								onClick = { deleteDialog = true },
								iconResource = R.drawable.ic_delete,
								contentDescription = "Delete",
								tint = Color.White
							)
						}
					)
				}) {
				if (recipe == null) {
					Box(
						Modifier
							.fillMaxSize()
							.padding(bottom = 150.dp)) {
						CircularProgressIndicator(Modifier.align(Alignment.Center))
					}
					return@CollapsingToolbarScaffold
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
			val pagerState = rememberPagerState()
			TabRow(selectedTabIndex = pagerState.currentPage,
				indicator = { tabPositions ->
					TabRowDefaults.Indicator(Modifier.pagerTabIndicatorOffset(pagerState, tabPositions), color = MaterialTheme.colors.secondary)
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
				count = tabs.size,
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
				if (isWide) 18.sp else MaterialTheme.typography.body1.fontSize
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
		RecipeDetailContent(
			recipe = Recipe("My Recipe", "My Instructions"),
			recipeImageData = MutableStateFlow(BlobModel.empty),
			servings = 1,
			ingredients = previewIngredients(),
			timers = previewTimers(),
			extendedData = MutableStateFlow(RecipeGraphModel(nodes = listOf(RecipeGraphNodeModel(RecipeGraphNode("id1", "Instruction"), "")), true)),
			isPlaningData = MutableStateFlow(false)
		)
	}
}


@Preview(name = "Wide", widthDp = 1000, heightDp = 600)
@Composable
private fun PreviewWide() {
	AppTheme {
		RecipeDetailContent(
			recipe = Recipe("My Recipe Wide", "My Instructions"),
			recipeImageData = MutableStateFlow(BlobModel.empty),
			servings = 1,
			ingredients = previewIngredients(),
			timers = previewTimers(),
			extendedData = MutableStateFlow(RecipeGraphModel(nodes = listOf(RecipeGraphNodeModel(RecipeGraphNode("id1", "Instruction"), "")), true)),
			isPlaningData = MutableStateFlow(false)
		)
	}
}


@Preview(name = "Wide no extended", widthDp = 1000, heightDp = 600)
@Composable
private fun PreviewWideNoExtended() {
	AppTheme {
		RecipeDetailContent(
			recipe = Recipe("My Recipe Wide", "My Instructions"),
			recipeImageData = MutableStateFlow(BlobModel.empty),
			servings = 1,
			ingredients = previewIngredients(),
			timers = previewTimers(),
			extendedData = MutableStateFlow(RecipeGraphModel(emptyList(), true)),
			isPlaningData = MutableStateFlow(false)
		)
	}
}

