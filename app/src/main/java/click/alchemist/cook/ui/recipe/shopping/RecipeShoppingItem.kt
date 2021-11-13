package click.alchemist.cook.ui.recipe.shopping

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.service.IngredientFormatter


@Composable
fun RecipeShoppingItem(shoppingItem: RecipeShoppingIngredient, onClick: () -> Unit = {}) {
	Card(elevation = 4.dp) {
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
				Text(shoppingItem.ingredient.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.subtitle2)

				Row(Modifier.padding(start = 8.dp)) {
					val amountString = IngredientFormatter.formatAmount(shoppingItem.ingredient, LocalContext.current)
					Text(stringResource(R.string.ingredient_shopping_in_recipe), Modifier.weight(0.5f), style = MaterialTheme.typography.subtitle1)
					Text(amountString, Modifier.weight(0.5f))
				}

				Row(Modifier.padding(start = 8.dp)) {
					Text(stringResource(R.string.ingredient_shopping_in_list), Modifier.weight(0.5f), style = MaterialTheme.typography.subtitle1)
					val amountString = if (shoppingItem.shoppingIngredient != null) {
						IngredientFormatter.formatAmount(shoppingItem.shoppingIngredient, LocalContext.current)
					} else {
						"-"
					}
					Text(amountString, Modifier.weight(0.5f))
				}
			}

			AnimatedVisibility(
				visible = shoppingItem.selected,
				modifier = Modifier
					.size(32.dp)
					.align(Alignment.CenterEnd),
				enter = fadeIn(),
				exit = fadeOut()
			) {
				Icon(
					painter = painterResource(R.drawable.ic_check),
					contentDescription = "Checked",
					Modifier
						.size(32.dp)
						.align(Alignment.CenterEnd),
					tint = colorResource(R.color.COLOR0_0)
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