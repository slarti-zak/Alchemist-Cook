package click.alchemist.cook.viewmodel

import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.model.IngredientUnit
import java.math.BigDecimal

class IngredientModel(val ingredient: Ingredient, val id: Int = -1) {
	val name: String
		get() = ingredient.name

	val unitCategory: IngredientCategory
		get() = ingredient.unitCategory

	var amount = Amount(BigDecimal.ONE, IngredientUnit.TIMES)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as IngredientModel

		return id == other.id
				&& ingredient == other.ingredient
				&& ((ingredient.unitCategory == IngredientCategory.HEADER && other.ingredient.unitCategory == IngredientCategory.HEADER) || amount == other.amount)
	}

	override fun hashCode(): Int {
		return id
	}
}
