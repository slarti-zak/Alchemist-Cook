package click.alchemist.cook.service

import android.content.Context
import click.alchemist.cook.R
import click.alchemist.cook.extension.humanReadable
import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.IngredientUnit
import click.alchemist.cook.viewmodel.Amount
import click.alchemist.cook.viewmodel.toAmount

object IngredientFormatter {
	private val values = mutableListOf(
		IngredientUnit.HEADER to R.string.ingredient_unit_header,
		IngredientUnit.TIMES to R.string.ingredient_unit_times,
		IngredientUnit.GRAM to R.string.ingredient_unit_gram,
		IngredientUnit.KILOGRAM to R.string.ingredient_unit_kilogram,
		IngredientUnit.MILLILITRE to R.string.ingredient_unit_millilitre,
		IngredientUnit.LITRE to R.string.ingredient_unit_litre,
		IngredientUnit.TABLESPOON to R.string.ingredient_unit_tablespoon,
		IngredientUnit.TEASPOON to R.string.ingredient_unit_teaspoon,
		IngredientUnit.PINCH to R.string.ingredient_unit_pinch,
		IngredientUnit.PACK to R.string.ingredient_unit_pack
	)

	fun nameOf(unit: IngredientUnit): Int {
		return values.first { it.first == unit }.second
	}

	fun formatAmount(amount: Amount, context: Context): String {
		val maxAmount = amount.asMaximumDisplay()
		return context.getString(
			R.string.format_amount_unit,
			maxAmount.amount.humanReadable(),
			context.getString(nameOf(maxAmount.unit))
		)
	}

	fun formatAmount(ingredient: Ingredient, context: Context): String {
		val amount = ingredient.toAmount()
		return formatAmount(amount, context)
	}
}