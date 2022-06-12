package click.alchemist.cook.compose.recipe.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import click.alchemist.cook.R
import click.alchemist.cook.model.BlobModel
import click.alchemist.cook.model.Recipe
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@Composable
fun RecipeImage(item: Recipe, imageLoader: suspend (Recipe) -> BlobModel, modifier: Modifier = Modifier) {
	var image by remember { mutableStateOf(BlobModel.empty) }
	LaunchedEffect(item.id) {
		launch { image = imageLoader(item) }
	}
	RecipeImage(image, modifier)
}

@Composable
fun RecipeImage(item: click.alchemist.cook.model.firestore.Recipe, imageLoader: suspend (Recipe) -> BlobModel, modifier: Modifier = Modifier) {
	var image by remember { mutableStateOf(BlobModel.empty) }
//	LaunchedEffect(item.id) {
//		launch { image = imageLoader(item) }
//	}
	RecipeImage(image, modifier)
}

@Composable
fun RecipeImage(image: BlobModel, modifier: Modifier = Modifier) {
	val imageModifier = modifier.then(Modifier.background(MaterialTheme.colors.primary))
	val contentDescription = "Recipe Image"

	if (image.isEmpty) {
		val fallback = painterResource(R.drawable.logo)
		Image(fallback, contentDescription, modifier = imageModifier)
	} else {
		AsyncImage(
			model = ImageRequest.Builder(LocalContext.current)
				.data(image.blob)
				.crossfade(true)
				.build(),
			contentDescription = contentDescription,
			modifier = imageModifier,
			contentScale = ContentScale.Crop
		)
	}
}