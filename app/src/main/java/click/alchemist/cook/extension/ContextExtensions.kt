package click.alchemist.cook.extension

import android.content.Context
import android.util.DisplayMetrics

interface DisplaySizeType {
}

@JvmInline
value class Px(
	val value: Float
) : DisplaySizeType {
	fun dp(context: Context): Dp = context.convertPixelsToDp(value)
}

@JvmInline
value class Dp(
	val value: Float
) : DisplaySizeType {
	fun px(context: Context): Px = context.convertDpToPixel(value)
}

/**
 * This method converts dp unit to equivalent pixels, depending on device density.
 *
 * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
 * @return A float value to represent px equivalent to dp depending on device density
 */
fun Context.convertDpToPixel(dp: Float): Px {
	return Px(dp * (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT))
}

/**
 * This method converts device specific pixels to density independent pixels.
 *
 * @param px A value in px (pixels) unit. Which we need to convert into db
 * @return A float value to represent dp equivalent to px value
 */
fun Context.convertPixelsToDp(px: Float): Dp {
	return Dp(px / (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT))
}

fun Context.heightToInch(pixel: Int): Float {
	val dm = resources.displayMetrics
	return pixel.toFloat() / dm.ydpi
}

fun Context.heightToCm(pixel: Int): Float {
	return this.heightToInch(pixel) * 2.54f
}

fun Context.widthToInch(pixel: Int): Float {
	val dm = resources.displayMetrics
	return pixel.toFloat() / dm.xdpi
}

fun Context.widthToCm(pixel: Int): Float {
	return this.heightToInch(pixel) * 2.54f
}


