package click.alchemist.cook.ui.shoppinglist.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.SimpleTextField
import click.alchemist.cook.compose.ingredient.IngredientUnitPicker
import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.IngredientUnit
import click.alchemist.cook.model.IngredientUnitConversion
import click.alchemist.cook.model.ShoppingListItem
import click.alchemist.cook.service.IngredientFormatter
import click.alchemist.cook.viewmodel.toAmount
import java.math.BigDecimal


@Composable
fun ShoppingListReduceDialog(
	ingredient: ShoppingListItem,
	dismiss: () -> Unit,
	apply: ((amount: String, IngredientUnit) -> Unit)? = null
) {
	val text = buildAnnotatedString {
		append(stringResource(R.string.ingredient_remove_desription_start))

		withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
			append(IngredientFormatter.formatAmount(ingredient.ingredient, LocalContext.current))
		}

		append(stringResource(R.string.ingredient_remove_desription_center))

		withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
			append(ingredient.ingredient.name)
		}

		append(stringResource(R.string.ingredient_remove_desription_end))
	}

	val amount = ingredient.ingredient.toAmount().asMaximumDisplay()
	val units = IngredientUnitConversion.conversions[amount.unit.category] ?: listOf(amount.unit)

	var amountString by remember { mutableStateOf(amount.amount.stripTrailingZeros().toPlainString()) }
	var unit by remember { mutableStateOf(amount.unit) }

	AlertDialog(
		onDismissRequest = dismiss,
		properties = DialogProperties(usePlatformDefaultWidth = true),
		confirmButton = {
			TextButton(onClick = {
				apply?.invoke(amountString, unit)
				dismiss()
			}) { Text(stringResource(R.string.general_apply)) }
		},
		dismissButton = {
			TextButton(onClick = dismiss) { Text(stringResource(R.string.general_cancel)) }
		},
		title = { Text(stringResource(R.string.ingredient_remove_message)) },
		text = {
			Column {
				CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
					Text(text)
				}

				Row(
					modifier = Modifier
						.height(IntrinsicSize.Min)
						.fillMaxWidth()
						.wrapContentWidth()
						.padding(top = 16.dp)
				) {
					SimpleTextField(
						value = amountString,
						onValueChange = { amountString = it },
						modifier = Modifier.widthIn(min = 70.dp, max = 100.dp),
						singleLine = true
					)
					IngredientUnitPicker(
						Modifier.fillMaxHeight(),
						unit,
						units = units
					) { unit = it }
				}
			}
		}
	)
}


@Preview
@Composable
private fun Preview() {
	val ingredient = ShoppingListItem(ingredient = Ingredient("Milk", BigDecimal.ONE))
	AppTheme {
		Box(Modifier.fillMaxSize()) {
			ShoppingListReduceDialog(ingredient, {})
		}
	}
}