package click.alchemist.cook.model

import java.math.BigDecimal
import java.util.*

enum class IngredientUnit(
	val category: IngredientCategory,
	val factor: BigDecimal = BigDecimal.ONE
) {
	GRAM(IngredientCategory.WEIGHT),
	KILOGRAM(IngredientCategory.WEIGHT, BigDecimal.valueOf(1000L)),
	MILLILITRE(IngredientCategory.VOLUME),
	LITRE(IngredientCategory.VOLUME, BigDecimal.valueOf(1000L)),
	TIMES(IngredientCategory.TIMES),
	HEADER(IngredientCategory.HEADER),
	TABLESPOON(IngredientCategory.TABLESPOON),
	TEASPOON(IngredientCategory.TEASPOON),
	PINCH(IngredientCategory.PINCH),
	PACK(IngredientCategory.PACK);

	fun toBase(amount: BigDecimal): BigDecimal {
		return amount * factor
	}
}

enum class IngredientCategory {
	WEIGHT,
	VOLUME,
	TIMES,
	HEADER,
	TABLESPOON,
	TEASPOON,
	PINCH,
	PACK
}

fun IngredientCategory.base(): IngredientUnit {
	return IngredientUnitConversion.conversions[this]!!.first()
}

fun IngredientCategory.units(): List<IngredientUnit> {
	return IngredientUnitConversion.conversions[this]!!
}

class IngredientUnitConversion {
	companion object {
		val conversions =
			EnumMap<IngredientCategory, List<IngredientUnit>>(IngredientCategory::class.java)

		init {
			conversions[IngredientCategory.WEIGHT] = listOf(IngredientUnit.GRAM, IngredientUnit.KILOGRAM)

			conversions[IngredientCategory.VOLUME] = listOf(IngredientUnit.MILLILITRE, IngredientUnit.LITRE)

			conversions[IngredientCategory.TIMES] = listOf(IngredientUnit.TIMES)
			conversions[IngredientCategory.HEADER] = listOf(IngredientUnit.HEADER)
			conversions[IngredientCategory.TABLESPOON] = listOf(IngredientUnit.TABLESPOON)
			conversions[IngredientCategory.TEASPOON] = listOf(IngredientUnit.TEASPOON)
			conversions[IngredientCategory.PINCH] = listOf(IngredientUnit.PINCH)
			conversions[IngredientCategory.PACK] = listOf(IngredientUnit.PACK)
		}
	}
}