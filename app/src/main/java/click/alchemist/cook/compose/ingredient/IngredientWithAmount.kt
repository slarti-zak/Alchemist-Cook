package click.alchemist.cook.compose.ingredient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.textIngredientAmountUnitStyle
import click.alchemist.cook.compose.textIngredientStyle
import click.alchemist.cook.compose.textIngredientStyleDisabled
import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.service.IngredientFormatter
import click.alchemist.cook.viewmodel.Amount


@Composable
fun IngredientWithAmount(amount: String, name: String, finished: Boolean = false, onClick: (() -> Unit)? = null, onLongClick: (() -> Unit)? = null) {
	val rowModifier = Modifier
		.fillMaxWidth()
		.then(
			if (onLongClick == null) {
				if (onClick != null)
					Modifier.clickable(onClick = onClick)
				else {
					Modifier
				}
			} else {
				Modifier.combinedClickable(onLongClick = onLongClick, onClick = { onClick?.invoke() })
			}
		)
		.padding(8.dp)

	Row(
		modifier = rowModifier,
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = amount,
			style = textIngredientAmountUnitStyle(),
			textAlign = TextAlign.End,
			modifier = Modifier
				.alignByBaseline()
				.weight(0.3f)
				.padding(end = 8.dp)
		)

		if (finished) {
			CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium, LocalTextStyle provides textIngredientStyle()) {
				Text(
					text = name,
					style = textIngredientStyleDisabled(),
					textAlign = TextAlign.Start,
					modifier = Modifier
						.alignByBaseline()
						.weight(0.7f)
				)
			}
		} else {
			Text(
				text = name,
				style = textIngredientStyle(),
				textAlign = TextAlign.Start,
				modifier = Modifier
					.alignByBaseline()
					.weight(0.7f)
			)
		}
	}
}


@Composable
fun IngredientWithAmount(ingredient: Ingredient, finished: Boolean = false, onClick: (() -> Unit)? = null, onLongClick: (() -> Unit)? = null) {
	val amountString = IngredientFormatter.formatAmount(ingredient, LocalContext.current)
	IngredientWithAmount(amountString, ingredient.name, finished, onClick, onLongClick)
}


@Composable
fun IngredientWithAmount(
	ingredient: Ingredient,
	amount: Amount,
	finished: Boolean = false,
	onClick: (() -> Unit)? = null,
	onLongClick: (() -> Unit)? = null
) {
	val amountString = IngredientFormatter.formatAmount(amount, LocalContext.current)
	IngredientWithAmount(amountString, ingredient.name, finished, onClick, onLongClick)
}


@Preview
@Composable
private fun Preview() {
	AppTheme {
		IngredientWithAmount("1 l", "Milk")
	}
}


@Preview("Finished")
@Composable
private fun PreviewFinished() {
	AppTheme {
		IngredientWithAmount("1 l", "Milk", true)
	}
}


@Preview("Long Name")
@Composable
private fun PreviewLong() {
	AppTheme {
		IngredientWithAmount("1 l", "Milk Milk Milk Milk Milk Milk Milk Milk Milk Milk Milk Milk Milk Milk Milk Milk")
	}
}
