package click.alchemist.cook.compose

import android.os.Build
import android.text.InputType
import android.widget.EditText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.addTextChangedListener
import click.alchemist.cook.service.markdown.MarkdownService
import io.noties.markwon.editor.MarkwonEditorTextWatcher

@Composable
fun MarkdownEditText(
	text: String,
	onTextChanged: (String) -> Unit,
	modifier: Modifier = Modifier,
	markdownService: MarkdownService? = null,
	factoryModifier: ((EditText) -> Unit)? = null
) {
	val editor by remember { mutableStateOf(markdownService?.createEditor()) }
	AndroidView(
		factory = { context ->
			EditText(context).apply {
				inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
				minLines = 2

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					importantForAutofill = EditText.IMPORTANT_FOR_AUTOFILL_NO
				}

				val currentEditor = editor
				if (currentEditor != null && markdownService != null) {
					addTextChangedListener(MarkwonEditorTextWatcher.withPreRender(currentEditor, markdownService.getExecutor(), this))
				}
				addTextChangedListener { onTextChanged(it?.toString() ?: "") }

				factoryModifier?.invoke(this)
			}
		},
		modifier
	) { view ->
		if (view.text.toString() != text) {
			view.setText(text)
		}
	}
}