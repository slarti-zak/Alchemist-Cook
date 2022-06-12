package click.alchemist.cook.compose.recipe.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.model.BlobModel
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.ui.recipe.list.RecipeListItem

@Composable
fun RecipeListItem(
	item: RecipeListItem,
	imageLoader: suspend (Recipe) -> BlobModel,
	onClick: ((RecipeListItem) -> Unit)? = {}
) {
	Card(
		Modifier
			.padding(4.dp)
			.aspectRatio(3f, false),
		backgroundColor = MaterialTheme.colors.primary,
		elevation = 4.dp
	) {
		Box(Modifier.clickable { onClick?.invoke(item) }) {
			RecipeImage(item.recipe, imageLoader, Modifier.fillMaxSize())
			Text(
				item.recipe.name.ifBlank { stringResource(R.string.list_item_empty) },
				Modifier
					.align(Alignment.BottomCenter)
					.fillMaxWidth()
					.background(Color(0, 0, 0, 50))
					.padding(8.dp, 4.dp),
				style = MaterialTheme.typography.h6,
				maxLines = 2
			)
		}
	}
}

@Preview
@Composable
private fun Preview() {
	AppTheme {
		RecipeListItem(
			RecipeListItem(click.alchemist.cook.model.firestore.Recipe("Recipe")),
			{ BlobModel.empty },
		)
	}
}