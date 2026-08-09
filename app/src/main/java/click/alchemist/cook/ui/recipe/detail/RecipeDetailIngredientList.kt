package click.alchemist.cook.ui.recipe.detail

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.Header
import click.alchemist.cook.compose.SimpleTextField
import click.alchemist.cook.compose.ingredient.IngredientWithAmount
import click.alchemist.cook.compose.previewIngredients
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.viewmodel.IngredientModel


@Composable
fun RecipeDetailIngredientList(
	servings: Int,
	ingredients: List<IngredientModel>,
	onServingChanged: (Int) -> Unit = {},
	onShoppingClicked: () -> Unit = {}
) {
	LazyColumn(Modifier.fillMaxSize(),
		content = { recipeDetailIngredientListContent(servings, onServingChanged, onShoppingClicked, ingredients) })
}


fun LazyListScope.recipeDetailIngredientListContent(
	servings: Int,
	onServingChanged: (Int) -> Unit,
	onShoppingClicked: () -> Unit,
	ingredients: List<IngredientModel>
) {
	item {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Spacer(Modifier.weight(0.2f))
			Text(
				stringResource(R.string.ingredient_portions_hint),
				Modifier
					.padding(end = 8.dp),
				textAlign = TextAlign.End
			)
			SimpleTextField(
				value = servings.toString(),
				textStyle = TextStyle.Default.copy(color = MaterialTheme.colorScheme.onSurface),
				onValueChange = { onServingChanged(it.toIntOrNull() ?: return@SimpleTextField) },
				modifier = Modifier
					.widthIn(min = 60.dp)
					.padding(vertical = 8.dp)
			)
			IconButton(
				onClick = onShoppingClicked,
				modifier = Modifier.padding(start = 16.dp)
			) {
				Icon(painterResource(R.drawable.ic_cart), contentDescription = stringResource(R.string.content_description_shopping_cart))
			}
			Spacer(Modifier.weight(1f))
		}
	}
	items(ingredients, { it.id }) { ingredient ->
		if (ingredient.ingredient.unitCategory == IngredientCategory.HEADER) {
			Header(name = ingredient.name)
		} else {
			IngredientWithAmount(ingredient.ingredient, ingredient.amount, false)
		}
	}
}


@Preview
@Composable
private fun Preview() {
	AppTheme {
		RecipeDetailIngredientList(
			4,
			previewIngredients()
		)
	}
}