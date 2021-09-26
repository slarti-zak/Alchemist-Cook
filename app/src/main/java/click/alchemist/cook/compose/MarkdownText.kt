package click.alchemist.cook.compose

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import click.alchemist.cook.service.markdown.MarkdownService

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, setUp: ((TextView) -> Unit)? = null, markdownService: MarkdownService? = null) {
	AndroidView(
		factory = { context ->
			TextView(context).apply { setUp?.invoke(this) }
		},
		modifier
	) { view ->
		if (markdownService != null) {
			markdownService.render(text, view)
		} else {
			view.text = text
		}
	}
}