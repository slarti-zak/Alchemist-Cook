package click.alchemist.cook.compose.recipe.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import click.alchemist.cook.R
import click.alchemist.cook.model.BlobModel
import click.alchemist.cook.model.Recipe
import coil.compose.rememberImagePainter
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
fun RecipeImage(image: BlobModel, modifier: Modifier = Modifier) {
	val imageModifier = modifier.then(Modifier.background(colorResource(R.color.colorPrimary)))
	val fallback = painterResource(R.drawable.logo)
	val contentDescription = "Recipe Image"

	if (image.isEmpty) {
		Image(fallback, contentDescription, modifier = imageModifier)
	} else {
		val painter = rememberImagePainter(
			data = image.blob,
			onExecute = { _, _ -> true },
			builder = {
				crossfade(true)
			})

		Image(
			painter = painter,
			contentDescription = contentDescription,
			modifier = imageModifier,
			contentScale = ContentScale.Crop
		)
	}
}