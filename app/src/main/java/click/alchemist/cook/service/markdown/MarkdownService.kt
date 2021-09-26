package click.alchemist.cook.service.markdown

import android.widget.TextView
import io.noties.markwon.editor.MarkwonEditor
import java.util.concurrent.ExecutorService

interface MarkdownService {
	fun getExecutor(): ExecutorService
	fun render(markdown: String, textView: TextView)
	fun createEditor(): MarkwonEditor
}