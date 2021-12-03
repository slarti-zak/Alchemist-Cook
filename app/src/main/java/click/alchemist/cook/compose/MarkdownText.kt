package click.alchemist.cook.compose

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import click.alchemist.cook.service.markdown.MarkdownService


@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, setUp: ((TextView) -> Unit)? = null, markdownService: MarkdownService? = null) {
	AndroidView(
		factory = { context ->
			NoTouchFrameLayout(context).apply {
				addView(
					TextView(context).apply { setUp?.invoke(this) },
					FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
				)
			}
		},
		modifier
	) { view ->
		val child = if (view.childCount > 0) view.getChildAt(0) else null
		if (child is TextView) {
			if (markdownService == null) {
				child.text = text
			} else {
				markdownService.render(text, child)
			}
		}
	}
}

class NoTouchFrameLayout @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
	override fun onInterceptTouchEvent(ev: MotionEvent?) = true
}