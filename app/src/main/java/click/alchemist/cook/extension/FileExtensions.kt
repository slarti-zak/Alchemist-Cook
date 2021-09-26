package click.alchemist.cook.extension

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import kotlin.math.roundToInt


fun File.scaledBitmap(reqWidth: Int, reqHeight: Int): Bitmap {
    return decodeFile(this, reqWidth, reqHeight)
}

fun decodeFile(
    file: File,
    reqWidth: Int,
    reqHeight: Int
): Bitmap { // First decode with inJustDecodeBounds=true to check dimensions
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(file.path, options)

    // Calculate inSampleSize
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false
    val image = BitmapFactory.decodeFile(file.path, options)
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

fun calculateInSampleSize(
    options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
): Int {
    // Raw height and width of image
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) { // Calculate ratios of height and width to requested height and width
        val heightRatio = (height.toFloat() / reqHeight.toFloat()).roundToInt()
        val widthRatio = (width.toFloat() / reqWidth.toFloat()).roundToInt()
        // Choose the smallest ratio as inSampleSize value, this will guarantee
        // a final image with both dimensions larger than or equal to the
        // requested height and width.
        inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
    }
    return inSampleSize
}