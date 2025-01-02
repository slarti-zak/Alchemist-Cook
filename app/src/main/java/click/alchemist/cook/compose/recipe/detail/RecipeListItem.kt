package click.alchemist.cook.compose.recipe.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
	modifier: Modifier = Modifier,
	item: RecipeListItem,
	imageLoader: suspend (Recipe) -> BlobModel,
	onClick: ((RecipeListItem) -> Unit)? = {},
	sharedTransitionScope: SharedTransitionScope,
	animatedContentScope: AnimatedVisibilityScope
) {
	with(sharedTransitionScope) {
		Card(
			modifier = modifier
				.padding(4.dp)
				.aspectRatio(3f, false),
			elevation = CardDefaults.cardElevation(4.dp)
		) {
			Box(Modifier.clickable { onClick?.invoke(item) }) {
				RecipeImage(
					item = item.recipe,
					imageLoader = imageLoader,
					modifier = Modifier
						.fillMaxSize()
						.sharedElement(
							rememberSharedContentState(key = "recipeImage-${item.recipe.id}"),
							animatedVisibilityScope = animatedContentScope
						)
						.clip(CardDefaults.shape)
				)
				Text(
					text = item.recipe.name.ifBlank { stringResource(R.string.list_item_empty) },
					modifier = Modifier
						.align(Alignment.BottomCenter)
						.fillMaxWidth()
						.background(Color(0, 0, 0, 50))
						.padding(8.dp, 4.dp)
						.sharedElement(
							rememberSharedContentState(key = "recipeText-${item.recipe.id}"),
							animatedVisibilityScope = animatedContentScope,
							zIndexInOverlay = 100f
						),
					style = MaterialTheme.typography.titleLarge,
					color = Color.White,
					maxLines = 2
				)
			}
		}
	}
}

@Preview
@Composable
private fun Preview() {
	AppTheme {
		SharedTransitionLayout {
			AnimatedContent(targetState = true) { it ->
				if (it) {
					RecipeListItem(
						Modifier,
						RecipeListItem(Recipe("Recipe")),
						{ BlobModel.empty },
						sharedTransitionScope = this@SharedTransitionLayout,
						animatedContentScope = this@AnimatedContent
					)
				}
			}
		}
	}
}