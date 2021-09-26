package click.alchemist.cook.viewmodel

import click.alchemist.cook.model.*
import java.math.BigDecimal
import java.math.RoundingMode

data class Amount(val amount: BigDecimal, val unit: IngredientUnit) {
	constructor(amount: BigDecimal, category: IngredientCategory) : this(amount, category.base())

	fun asMaximumDisplay(): Amount {
		val originalAmount = amount * unit.factor
		val category = unit.category

		for (unit in category.units().reversed()) {
			val amount = originalAmount.divide(unit.factor, 3, RoundingMode.HALF_UP)
			if (BigDecimal.ONE <= amount)
				return Amount(amount, unit)
		}

		return this
	}
}

fun Ingredient.toAmount(): Amount {
	val category = unitCategory
	return Amount(amount, category.base())
}