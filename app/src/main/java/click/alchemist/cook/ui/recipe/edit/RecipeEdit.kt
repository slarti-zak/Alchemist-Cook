package click.alchemist.cook.ui.recipe.edit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import click.alchemist.cook.App
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.BackButton
import click.alchemist.cook.compose.CookIconButton
import click.alchemist.cook.compose.ToolbarTextField
import click.alchemist.cook.compose.lightIcon
import click.alchemist.cook.compose.recipe.RecipeExtendedInstructions
import click.alchemist.cook.compose.recipe.detail.RecipeImage
import click.alchemist.cook.logError
import click.alchemist.cook.service.markdown.MarkdownService
import click.alchemist.cook.ui.recipe.detail.RECIPE_IMAGE_FULL_HEIGHT
import click.alchemist.cook.ui.recipe.detail.RecipeTab
import click.alchemist.cook.viewmodel.IngredientEditModel
import click.alchemist.cook.viewmodel.RecipeGraphModel
import click.alchemist.cook.viewmodel.RecipeGraphNodeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date


@Composable
fun RecipeEdit(
	recipeId: String?,
	viewModel: RecipeEditViewModel,
	onBackNavigation: () -> Unit,
	onSaved: (recipeId: String) -> Unit,
	onExtendedInstruction: (RecipeGraphNodeModel?) -> Unit,
	sharedTransitionScope: SharedTransitionScope,
	animatedContentScope: AnimatedContentScope
) {
	LaunchedEffect(recipeId) { viewModel.load(recipeId) }

	val markdownService = koinInject<MarkdownService>()
	val context = LocalContext.current

	var currentPhotoPath: File? = null

	val takePicture = rememberLauncherForActivityResult(TakePictureWithUriGrant()) { saved ->
		if (saved) {
			val path = currentPhotoPath ?: return@rememberLauncherForActivityResult
			viewModel.applyImage { FileInputStream(path) }
		}
	}

	val galleryPicture = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
		if (uri != null) {
			viewModel.applyImage { context.contentResolver.openInputStream(uri) }
		}
	}

	val scope = rememberCoroutineScope()

	RecipeEditContent(
		viewModel.title,
		viewModel.image,
		viewModel.content,
		viewModel.ingredients,
		viewModel.extraInstructions,
		viewModel.serves,
		onRecipeNameChanged = { scope.launch { viewModel.title.emit(it) } },
		onInstructionsChanged = { scope.launch { viewModel.content.emit(it) } },
		backNavigation = onBackNavigation,
		onSave = {
			scope.launch {
				val savedRecipeId = viewModel.save()
				onSaved(savedRecipeId)
			}
		},
		takePicture = takePicture,
		galleryPicture = galleryPicture,
		uriGetter = {
			try {
				val newPhotoPath = createImageFile(context, currentPhotoPath)
				currentPhotoPath = newPhotoPath
				getPhotoUri(context, newPhotoPath)
			} catch (ex: Exception) {
				logError("Could not get uri for file!", ex)
				null
			}
		},
		onIngredientNameChanged = { _, _ -> viewModel.ensureEmptyLastElement() },
		onIngredientDeleted = viewModel::deleteIngredientItem,
		onEditExtendedIngredient = onExtendedInstruction,
		onDeleteExtendedIngredient = viewModel::deleteExtraInstruction,
		onServingChanged = viewModel::onServingsChanged,
		onListReordered = viewModel::onListReordered,
		markdownService = markdownService,
		sharedTransitionScope = sharedTransitionScope,
		animatedContentScope = animatedContentScope
	)
}

private fun getPhotoUri(context: Context, photoPath: File): Uri {
	return FileProvider.getUriForFile(context, App.authority, photoPath)
}

/**
 * Same contract as [ActivityResultContracts.TakePicture], but sets [Intent.FLAG_GRANT_WRITE_URI_PERMISSION]
 * directly on the capture intent. The stock contract doesn't set it, and relies on the system's
 * implicit URI write grant for `ACTION_IMAGE_CAPTURE` — which Android is discontinuing, logging a
 * warning ("Implicit URI write grant... will be discontinued") until the flag is set explicitly.
 */
private class TakePictureWithUriGrant : ActivityResultContract<Uri, Boolean>() {
	override fun createIntent(context: Context, input: Uri): Intent {
		return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
			.putExtra(MediaStore.EXTRA_OUTPUT, input)
			.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
	}

	override fun parseResult(resultCode: Int, intent: Intent?): Boolean = resultCode == Activity.RESULT_OK
}

@SuppressLint("SimpleDateFormat")
private fun createImageFile(context: Context, currentPhotoPath: File?): File {
	currentPhotoPath?.delete()

	// Create an image file name
	val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
	val storageDir: File = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

	return File.createTempFile(
		"JPEG_${timeStamp}_",
		".jpg",
		storageDir
	).apply {
		mkdirs()
	}
}


@Composable
private fun RecipeEditContent(
	recipeNameData: MutableStateFlow<String>,
	recipeImageData: StateFlow<Any?>,
	instructionData: MutableStateFlow<String>,
	ingredientData: StateFlow<List<IngredientEditModel>>,
	extendedInstructionData: StateFlow<RecipeGraphModel>,
	servingsData: MutableStateFlow<Int>,
	onRecipeNameChanged: (String) -> Unit = {},
	onInstructionsChanged: (String) -> Unit = {},
	backNavigation: () -> Unit = {},
	onSave: () -> Unit = {},
	takePicture: ActivityResultLauncher<Uri>? = null,
	galleryPicture: ActivityResultLauncher<String>? = null,
	uriGetter: () -> Uri? = { null },
	onIngredientNameChanged: (IngredientEditModel, String) -> Unit = { _, _ -> },
	onIngredientDeleted: (IngredientEditModel) -> Unit = {},
	onEditExtendedIngredient: (RecipeGraphNodeModel?) -> Unit = {},
	onDeleteExtendedIngredient: (RecipeGraphNodeModel) -> Unit = {},
	onServingChanged: (Int) -> Unit = {},
	onListReordered: (Int, Int) -> Unit = { _, _ -> },
	markdownService: MarkdownService? = null,
	sharedTransitionScope: SharedTransitionScope,
	animatedContentScope: AnimatedContentScope
) {
	val scope = rememberCoroutineScope()
	var bottomSheet by remember { mutableStateOf(false) }
	var hasPositioned by remember { mutableStateOf(false) }

//	if (markdownService != null) {
//		BackHandler(bottomSheet.currentValue != SheetValue.Hidden) {
//			scope.launch { bottomSheet.hide() }
//		}
//	}

	val recipeName by recipeNameData.collectAsState()
	with(sharedTransitionScope) {
		val modifier = if (hasPositioned) {
			Modifier
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
		} else {
			Modifier.onGloballyPositioned { hasPositioned = true }
		}
		Scaffold(
			topBar = {
				TopAppBar(
					title = {
						ToolbarTextField(
							value = recipeName,
							onValueChange = onRecipeNameChanged,
							Modifier.fillMaxWidth(),
							placeholder = stringResource(R.string.recipe_edit_title_hint)
						)
					},
					navigationIcon = { BackButton(backNavigation) },
					actions = {
						CookIconButton(onClick = onSave, iconResource = R.drawable.ic_content_save, contentDescription = stringResource(R.string.general_save))
					}
				)
			},
			modifier = modifier
		) { paddingValues ->
			val recipeImage by recipeImageData.collectAsState()

			val instructions by instructionData.collectAsState()
			val ingredients by ingredientData.collectAsState()
			val extendedInstructions by extendedInstructionData.collectAsState()

			val servings by servingsData.collectAsState()

			BoxWithConstraints {
//				val isWide = maxWidth >= 600.dp
				Column(
					Modifier
						.padding(paddingValues)
						.fillMaxSize()
				) {
					RecipeEditImage(recipeImage) { scope.launch { bottomSheet = true } }

					val tabs = listOf(RecipeTab.Instructions, RecipeTab.ExtendedInstructions, RecipeTab.Ingredients)

					if (tabs.isNotEmpty()) {
						val pagerState = rememberPagerState(
							initialPage = 0,
							pageCount = { tabs.size })
						SecondaryTabRow(selectedTabIndex = pagerState.currentPage,
							indicator = {
								TabRowDefaults.SecondaryIndicator(
									modifier = Modifier.tabIndicatorOffset(pagerState.currentPage, matchContentSize = false),
									color = MaterialTheme.colorScheme.secondary
								)
							}) {

							tabs.forEachIndexed { index, recipeTab ->
								when (recipeTab) {
									RecipeTab.Instructions -> RecipeTab(
										stringResource(R.string.recipe_tab_instructions_title),
										index,
										pagerState
									)

									RecipeTab.ExtendedInstructions -> RecipeTab(
										stringResource(R.string.recipe_tab_instructions_extended_title),
										index,
										pagerState
									)

									RecipeTab.Ingredients -> RecipeTab(
										stringResource(R.string.recipe_tab_ingredients_title),
										index,
										pagerState
									)

									else -> throw IllegalArgumentException("Invalid tab type $recipeTab!")
								}
							}
						}

						HorizontalPager(state = pagerState, key = { tabs[it] }) { pageIndex ->
							val tab = if (pageIndex < tabs.size) tabs[pageIndex] else return@HorizontalPager
							when (tab) {
								RecipeTab.Instructions ->
									Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
										RecipeEditInstructions(
											instructions,
											onTextChanged = onInstructionsChanged,
											markdownService = markdownService
										)
									}

								RecipeTab.ExtendedInstructions -> RecipeEditExtendedInstructions(
									extendedInstructions,
									onEditExtendedIngredient,
									onDeleteExtendedIngredient,
									markdownService = markdownService
								)

								RecipeTab.Ingredients -> RecipeEditIngredientList(
									servings,
									ingredients,
									onNameChanged = onIngredientNameChanged,
									onIngredientDeleted = onIngredientDeleted,
									onServingChanged = onServingChanged,
									onListReordered = onListReordered
								)

								else -> throw IllegalArgumentException("Invalid tab type $tab!")
							}
						}
					}
				}
			}

			if (bottomSheet) {
				ModalBottomSheet(
					onDismissRequest = { bottomSheet = false },
				)
				{
					BottomSheetContent({
						scope.launch { bottomSheet = false }
						takePicture?.launch(uriGetter() ?: return@BottomSheetContent)
					}, {
						scope.launch { bottomSheet = false }
						galleryPicture?.launch("image/*")
					})
				}
			}
		}
	}
}

@Composable
private fun RecipeEditImage(
	recipeImage: Any?,
	onEditClick: () -> Unit
) {
	Box(contentAlignment = Alignment.Center) {
		RecipeImage(
			recipeImage,
			Modifier
				.fillMaxWidth()
				.height(RECIPE_IMAGE_FULL_HEIGHT.dp)
		)
		CompositionLocalProvider(LocalContentColor provides Color.White) {
			CookIconButton(
				onClick = onEditClick,
				iconResource = if (recipeImage == null) R.drawable.ic_plus else R.drawable.ic_pencil,
				contentDescription = stringResource(R.string.content_description_change_image),
				modifier = Modifier.background(lightIcon.copy(alpha = 0.7f), CircleShape)
			)
		}
	}
}


@Composable
private fun BottomSheetContent(
	onChangeImageFromCamera: () -> Unit,
	onChangeImageFromGallery: () -> Unit
) {
	Text(
		"Pick Image",
		style = MaterialTheme.typography.headlineMedium,
		modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
	)
	ListItem(
		leadingContent = { Icon(painterResource(R.drawable.ic_camera), stringResource(R.string.camera)) },
		headlineContent = { Text(stringResource(R.string.camera)) },
		modifier = Modifier.clickable(onClick = onChangeImageFromCamera),
		colors = ListItemDefaults.colors(containerColor = Color.Transparent)
	)
	ListItem(
		leadingContent = { Icon(painterResource(R.drawable.ic_folder_multiple_image), stringResource(R.string.gallery)) },
		headlineContent = { Text(stringResource(R.string.gallery)) },
		modifier = Modifier.clickable(onClick = onChangeImageFromGallery),
		colors = ListItemDefaults.colors(containerColor = Color.Transparent)
	)
}


@Composable
private fun RecipeEditExtendedInstructions(
	graphModel: RecipeGraphModel,
	onEdit: ((RecipeGraphNodeModel?) -> Unit),
	onDelete: ((RecipeGraphNodeModel) -> Unit),
	markdownService: MarkdownService? = null
) {
	Column(modifier = Modifier.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
		Button(onClick = { onEdit.invoke(null) }) {
			Text(text = stringResource(id = R.string.recipe_edit_extended_instruction_add))
		}
		RecipeExtendedInstructions(
			graphModel = graphModel,
			modifier = Modifier.fillMaxSize(),
			onClick = onEdit,
			onSwipeDelete = onDelete,
			markdownService = markdownService
		)
	}
}


@Preview
@Composable
private fun Preview() {
	AppTheme {
		SharedTransitionLayout {
			AnimatedContent(false) {
				RecipeEditContent(
					MutableStateFlow("Name"),
					MutableStateFlow(null),
					MutableStateFlow("Instructions"),
					MutableStateFlow(listOf()),
					MutableStateFlow(RecipeGraphModel(isPreview = true)),
					MutableStateFlow(4),
					sharedTransitionScope = this@SharedTransitionLayout,
					animatedContentScope = this@AnimatedContent,
				)
			}
		}
	}
}