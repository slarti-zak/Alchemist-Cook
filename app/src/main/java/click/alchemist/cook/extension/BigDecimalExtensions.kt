package click.alchemist.cook.extension

import java.math.BigDecimal

private val unicodeFractions = mapOf(
	"2" to "⅕",
	"25" to "¼",
	"33" to "⅓",
	"333" to "⅓",
	"4" to "⅖",
	"5" to "½",
	"6" to "⅗",
	"67" to "⅔",
	"667" to "⅔",
	"75" to "¾",
	"8" to "⅘"
)

fun BigDecimal.humanReadable(): String {
	val string = stripTrailingZeros().toPlainString()

	val commaIndex = string.indexOf('.')
	if (commaIndex < 0)
		return string

	val remainder = string.substring(commaIndex + 1)
	val unicodeRemainder = unicodeFractions[remainder] ?: return string

	val intValue = string.substring(0, commaIndex)
	return if (intValue == "0") unicodeRemainder
	else intValue + unicodeRemainder
}

fun BigDecimal.isZero(): Boolean {
	return this.signum() == 0
}

fun tryParse(value: String?, fallback: BigDecimal): BigDecimal {
	return try {
		BigDecimal(value ?: return BigDecimal.ZERO)
	} catch (e: Throwable) {
		fallback
	}
}