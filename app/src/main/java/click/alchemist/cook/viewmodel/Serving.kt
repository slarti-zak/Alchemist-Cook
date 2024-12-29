package click.alchemist.cook.viewmodel

import android.os.Parcelable
import click.alchemist.cook.model.Ingredient
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.RoundingMode

@Parcelize
class Serving(private val recipeServings: BigDecimal, private val servings: BigDecimal) : Parcelable {
	constructor(
		recipeServings: Int,
		servings: Int
	) : this(
		BigDecimal.valueOf(recipeServings.coerceAtLeast(1).toLong()),
		BigDecimal.valueOf(servings.coerceAtLeast(1).toLong())
	)

	private fun convert(value: BigDecimal): BigDecimal {
		return (value * servings).divide(recipeServings, 2, RoundingMode.HALF_UP)
	}

	fun toAmount(ingredient: Ingredient): Amount {
		val converted = convert(ingredient.amount)
		return Amount(converted, ingredient.unitCategory)
	}

	fun from(ingredient: Ingredient): Ingredient {
		return ingredient.copy(amount = convert(ingredient.amount))
	}
}