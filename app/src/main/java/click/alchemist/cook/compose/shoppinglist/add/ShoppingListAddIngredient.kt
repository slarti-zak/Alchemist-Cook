package click.alchemist.cook.compose.shoppinglist.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.ingredient.IngredientUnitPicker
import click.alchemist.cook.compose.previewIngredients
import click.alchemist.cook.compose.shoppinglist.ShoppingListItemName
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.model.IngredientUnit

@Composable
fun ShoppingListAddIngredient(
	entries: List<String>,
	ingredient: String,
	modifier: Modifier = Modifier,
	ingredientChanged: ((String) -> Unit)? = null,
	addIngredient: ((String, String, IngredientUnit) -> Unit)? = null
) {
	var amountString by remember { mutableStateOf("1") }
	var unit by remember { mutableStateOf(IngredientUnit.TIMES) }

	Column(modifier) {
		Row(verticalAlignment = Alignment.CenterVertically) {

			TextField(
				value = amountString,
				placeholder = { Text(stringResource(id = R.string.ingredient_amount_hint)) },
				onValueChange = { amountString = it },
				maxLines = 1,
				modifier = Modifier
					.requiredWidthIn(min = 75.dp, max = 100.dp),
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
			)

			IngredientUnitPicker(
				Modifier,
				unit
			) { unit = it }

			TextField(
				modifier = Modifier.weight(1f),
				value = ingredient,
				onValueChange = { ingredientChanged?.invoke(it) },
				label = { Text(stringResource(R.string.ingredient_name_hint)) },
				maxLines = 1,
				keyboardOptions = KeyboardOptions(
					imeAction = ImeAction.Go,
					capitalization = KeyboardCapitalization.Sentences),
				keyboardActions = KeyboardActions(onGo = {
					addIngredient?.invoke(ingredient, amountString, unit)
					amountString = "1"
				})
			)
		}

		LazyColumn(modifier = Modifier.fillMaxSize()) {
			items(items = entries, key = { it }, itemContent = { entry ->
				ShoppingListItemName(entry, Modifier.animateItem()) {
					addIngredient?.invoke(entry, amountString, unit)
					amountString = "1"
				}
			})
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
	AppTheme {
		ShoppingListAddIngredient(
			previewIngredients().filter { it.unitCategory != IngredientCategory.HEADER }.map { it.name },
			"Entry")
	}
}

@Preview(widthDp = 600, heightDp = 300, name = "Landscape", showBackground = true)
@Composable
private fun PreviewWide() {
	AppTheme {
		ShoppingListAddIngredient(
			previewIngredients().filter { it.unitCategory != IngredientCategory.HEADER }.map { it.name },
			"Entry")
	}
}