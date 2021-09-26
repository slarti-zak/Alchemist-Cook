package click.alchemist.cook.extension

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream


fun (() -> InputStream?).scaledBitmap(reqWidth: Int, reqHeight: Int): Bitmap? {
	return decodeFile(this, reqWidth, reqHeight)
}

fun decodeFile(
	stream: () -> InputStream?,
	reqWidth: Int,
	reqHeight: Int
): Bitmap? {
	// First decode with inJustDecodeBounds=true to check dimensions
	val options = BitmapFactory.Options()
	options.inJustDecodeBounds = true
	stream().use {
		BitmapFactory.decodeStream(it, null, options)
	}

	// Calculate inSampleSize
	options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

	// Decode bitmap with inSampleSize set
	options.inJustDecodeBounds = false
	val image = stream().use {
		BitmapFactory.decodeStream(it, null, options)
	} ?: return null
	val imageWidth = image.width
	val imageHeight = image.height

	if (imageHeight > reqHeight || imageWidth > reqWidth) {
		val scaleWidth = imageWidth.toFloat() / reqWidth.toFloat()
		val scaleHeight = imageHeight.toFloat() / reqHeight.toFloat()
		val scale = if (scaleWidth < scaleHeight) scaleHeight else scaleWidth
		return Bitmap.createScaledBitmap(
			image,
			(imageWidth / scale).toInt(),
			(imageHeight / scale).toInt(),
			true
		).also {
			image.recycle()
		}
	}

	return image
}