package click.alchemist.cook.ui.recipe.shopping

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.COLOR0_0
import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.service.IngredientFormatter


@Composable
fun RecipeShoppingItem(shoppingItem: RecipeShoppingIngredient, onClick: () -> Unit = {}) {
	Card(elevation = CardDefaults.cardElevation(4.dp)) {
		Box(
			Modifier
				.fillMaxWidth()
				.clickable(onClick = onClick)
				.padding(8.dp),
		) {
			Column(
				Modifier
					.align(alignment = Alignment.CenterStart)
					.padding(end = 32.dp)
			) {
				Text(shoppingItem.ingredient.name,
					style = MaterialTheme.typography.titleSmall)

				Row(Modifier.padding(start = 8.dp)) {
					val amountString = IngredientFormatter.formatAmount(shoppingItem.ingredient, LocalContext.current)
					Text(stringResource(R.string.ingredient_shopping_in_recipe), Modifier.weight(0.5f), style = MaterialTheme.typography.bodyMedium)
					Text(amountString, Modifier.weight(0.5f))
				}

				Row(Modifier.padding(start = 8.dp)) {
					Text(stringResource(R.string.ingredient_shopping_in_list), Modifier.weight(0.5f), style = MaterialTheme.typography.bodyMedium)
					val amountString = if (shoppingItem.shoppingIngredient != null) {
						IngredientFormatter.formatAmount(shoppingItem.shoppingIngredient, LocalContext.current)
					} else {
						"-"
					}
					Text(amountString, Modifier.weight(0.5f))
				}
			}

			this@Card.AnimatedVisibility(
				visible = shoppingItem.selected,
				modifier = Modifier
					.size(32.dp)
					.align(Alignment.CenterEnd),
				enter = fadeIn(),
				exit = fadeOut()
			) {
				Icon(
					painter = painterResource(R.drawable.ic_check),
					contentDescription = stringResource(R.string.content_description_checked),
					Modifier
						.size(32.dp)
						.align(Alignment.CenterEnd),
					tint = COLOR0_0
				)
			}
		}
	}
}


@Preview
@Composable
private fun Preview() {
	AppTheme {
		RecipeShoppingItem(RecipeShoppingIngredient(Ingredient("Ingredient 1"), Ingredient("Ingredient 1")))
	}
}