package click.alchemist.cook.service.markdown

import android.content.Context
import android.widget.TextView
import click.alchemist.cook.service.recipe.RecipeTimerParser
import io.noties.markwon.Markwon
import io.noties.markwon.PrecomputedTextSetterCompat
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.simple.ext.SimpleExtPlugin
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MarkwonService(context: Context, recipeTimerParser: RecipeTimerParser) : MarkdownService {
	private val executor = Executors.newSingleThreadExecutor()

	private val markwon = Markwon.builder(context)
		.textSetter(PrecomputedTextSetterCompat.create(Executors.newCachedThreadPool()))
		.usePlugin(SimpleExtPlugin.create {
			it.addExtension(2, '(', ')') { _, _ ->
				TimerSpan(context, recipeTimerParser)
			}
		})
		.build()

	override fun getExecutor(): ExecutorService {
		return executor
	}

	override fun render(markdown: String, textView: TextView) {
		markwon.setMarkdown(textView, markdown)
	}

	override fun createEditor(): MarkwonEditor {
		return MarkwonEditor.create(markwon)
	}
}