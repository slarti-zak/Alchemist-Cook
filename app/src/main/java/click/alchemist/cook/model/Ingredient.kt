package click.alchemist.cook.model

import java.math.BigDecimal

data class Ingredient(
	var name: String = "",
	var amount: BigDecimal = BigDecimal.ONE,
	var unitCategory: IngredientCategory = IngredientCategory.TIMES
) {
	constructor(name: String, amount: BigDecimal, unit: IngredientUnit) : this(name, unit.toBase(amount), unit.category)
}
