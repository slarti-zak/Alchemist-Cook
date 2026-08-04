package click.alchemist.cook.compose.recipe.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import click.alchemist.cook.R
import click.alchemist.cook.compose.COLOR0_1
import click.alchemist.cook.model.Recipe
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RecipeImage(item: Recipe, imageLoader: suspend (Recipe) -> File?, modifier: Modifier = Modifier) {
	var image by remember { mutableStateOf<Any?>(null) }
	LaunchedEffect(item.id) {
		launch { image = imageLoader(item) }
	}
	RecipeImage(image, modifier)
}

/** [image] is a [File] (persisted recipe image) or a [android.graphics.Bitmap] (in-memory preview); null means no image. */
@Composable
fun RecipeImage(image: Any?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
	val imageModifier = modifier.then(Modifier.background(COLOR0_1))
	val contentDescription = "Recipe Image"

	if (image == null) {
		val fallback = painterResource(R.drawable.logo)
		Image(
			painter = fallback,
			contentDescription = contentDescription,
			modifier = imageModifier
		)
	} else {
		AsyncImage(
			model = ImageRequest.Builder(LocalContext.current)
				.data(image)
				.crossfade(true)
				.build(),
			contentDescription = contentDescription,
			modifier = imageModifier,
			contentScale = contentScale,
		)
	}
}