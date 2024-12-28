package click.alchemist.cook.compose

import android.util.TypedValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import click.alchemist.cook.service.markdown.MarkdownService

@Composable
fun RecipeText(text: String, modifier: Modifier = Modifier, markdownService: MarkdownService? = null, sp: TextUnit = 12.sp) {
	val textSize = with(LocalDensity.current) { sp.toPx() }
	val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
	MarkdownText(
		text,
		modifier,
		{
			it.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
			it.setTextColor(textColor)
		},
		markdownService
	)
}