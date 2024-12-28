package click.alchemist.cook.service.markdown

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.style.ReplacementSpan
import androidx.annotation.IntRange
import androidx.annotation.NonNull
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import click.alchemist.cook.R
import click.alchemist.cook.compose.OnBackground
import click.alchemist.cook.compose.OnBackgroundDark
import click.alchemist.cook.extension.convertDpToPixel
import click.alchemist.cook.extension.humanReadable
import click.alchemist.cook.service.recipe.RecipeTimerParser
import java.lang.ref.WeakReference
import kotlin.math.roundToInt


class TimerSpan(
	val context: Context,
	private val recipeTimerParser: RecipeTimerParser
) : ReplacementSpan() {

	private var mDrawableRef: WeakReference<Drawable?>? = null

	private fun getDrawable(): Drawable =
		ContextCompat.getDrawable(context, R.drawable.ic_timer_sand_empty)!!.mutate().apply {
			val size = context.convertDpToPixel(20f)
			val sizePx = size.value.toInt()
			setBounds(0, 0, sizePx, sizePx)

			val isDark = when (context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
				Configuration.UI_MODE_NIGHT_YES -> {true}
				else -> {false}
			}

			setTint(if(isDark) OnBackgroundDark.toArgb() else OnBackground.toArgb())
		}

	private fun getDrawableBounds(): Rect = getDrawable().bounds

	private fun timerText(text: CharSequence?): String {
		if (text == null) return ""

		val timer = recipeTimerParser.parseSingle(text, false)
		return timer.duration.humanReadable()
	}

	override fun getSize(
		paint: Paint,
		text: CharSequence?,
		start: Int,
		end: Int,
		fm: FontMetricsInt?
	): Int {
		val drawableBounds = getDrawableBounds()
		val heightToDraw = -paint.fontMetrics.ascent + paint.fontMetrics.descent
		val height = drawableBounds.height()
		val scale = heightToDraw / height

		val textWidth = paint.measureText(timerText(text?.substring(start, end)))

		return (drawableBounds.width() * scale + textWidth + 0.5f).roundToInt()
	}

	override fun draw(
		@NonNull canvas: Canvas, text: CharSequence?,
		@IntRange(from = 0) start: Int, @IntRange(from = 0) end: Int,
		x: Float,
		top: Int,
		y: Int,
		bottom: Int, @NonNull paint: Paint
	) {
		val drawable = getCachedDrawable()
		val drawableBounds = getDrawableBounds()

		canvas.save()
		canvas.translate(x, y.toFloat())

		val heightToDraw = -paint.fontMetrics.ascent + paint.fontMetrics.descent
		val height = drawableBounds.height()
		val scale = heightToDraw / height
		canvas.save()
		canvas.translate(0f, paint.fontMetrics.ascent)
		canvas.scale(scale, scale)
		drawable.draw(canvas)
		canvas.restore()

		canvas.translate(drawable.bounds.width() * scale, 0f)
		canvas.drawText(
			timerText(text?.substring(start, end)),
			0f,
			0f,
			paint
		)

		canvas.restore()
	}

	private fun getCachedDrawable(): Drawable {
		val wr = mDrawableRef
		var d: Drawable? = null
		if (wr != null) {
			d = wr.get()
		}
		if (d == null) {
			d = getDrawable()
			mDrawableRef = WeakReference(d)
		}
		return d
	}
}
