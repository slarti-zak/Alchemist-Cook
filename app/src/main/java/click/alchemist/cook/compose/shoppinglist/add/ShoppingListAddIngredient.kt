package click.alchemist.cook.compose.shoppinglist.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.atLeastWrapContent
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.ingredient.IngredientUnitPicker
import click.alchemist.cook.compose.shoppinglist.ShoppingListItemName
import click.alchemist.cook.model.IngredientUnit

@Composable
fun ShoppingListAddIngredient(
	entries: List<String>,
	ingredient: String,
	ingredientChanged: ((String) -> Unit)? = null,
	addIngredient: ((String, String, IngredientUnit) -> Unit)? = null
) {
	var amountString by remember { mutableStateOf("1") }
	var unit by remember { mutableStateOf(IngredientUnit.TIMES) }

	Column(Modifier.fillMaxSize()) {
		ConstraintLayout(Modifier.fillMaxWidth()) {
			val (amountId, unitId, nameId) = createRefs()

			TextField(
				value = amountString,
				placeholder = { Text(stringResource(id = R.string.ingredient_amount_hint)) },
				onValueChange = { amountString = it },
				maxLines = 1,
				modifier = Modifier
					.constrainAs(amountId) {
						height = Dimension.wrapContent
						width = Dimension.preferredValue(75.dp).atLeastWrapContent
						top.linkTo(parent.top)
						bottom.linkTo(parent.bottom)
						start.linkTo(parent.start)
					}
					.requiredWidthIn(min = 75.dp, max = 100.dp),
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
			)
			IngredientUnitPicker(
				modifier = Modifier
					.constrainAs(unitId) {
						height = Dimension.fillToConstraints
						width = Dimension.preferredValue(70.dp).atLeastWrapContent
						top.linkTo(parent.top)
						bottom.linkTo(parent.bottom)
						start.linkTo(amountId.end)
						end.linkTo(nameId.start)
					}, unit
			) { unit = it }

			TextField(
				value = ingredient,
				onValueChange = { ingredientChanged?.invoke(it) },
				label = { Text(stringResource(R.string.ingredient_name_hint)) },
				maxLines = 1,
				modifier = Modifier.constrainAs(nameId) {
					height = Dimension.wrapContent
					width = Dimension.fillToConstraints
					top.linkTo(parent.top)
					bottom.linkTo(parent.bottom)
					start.linkTo(unitId.end)
					end.linkTo(parent.end)
				},
				keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
				keyboardActions = KeyboardActions(onGo = {
					addIngredient?.invoke(ingredient, amountString, unit)
					amountString = "1"
				})
			)
		}

		LazyColumn(modifier = Modifier.fillMaxSize()) {
			items(items = entries, key = { it }, itemContent = { entry ->
				ShoppingListItemName(entry, Modifier.animateItemPlacement()) {
					addIngredient?.invoke(entry, amountString, unit)
					amountString = "1"
				}
			})
		}
	}
}

@Preview
@Composable
private fun Preview() {
	AppTheme {
		ShoppingListAddIngredient(listOf("a", "b"), "Entry")
	}
}