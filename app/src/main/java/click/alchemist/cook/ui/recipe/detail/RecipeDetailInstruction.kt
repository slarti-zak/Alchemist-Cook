package click.alchemist.cook.ui.recipe.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import click.alchemist.cook.compose.RecipeText
import click.alchemist.cook.service.markdown.MarkdownService

@Composable
fun RecipeDetailInstruction(instructions: String, markdownService: MarkdownService? = null, sp: TextUnit = 12.sp) {
//	val padding = with(LocalDensity.current) { 16.dp.roundToPx() }
	RecipeText(
		"&nbsp;\n\n$instructions\n\n&nbsp;",
		Modifier.fillMaxWidth(),
		markdownService,
		sp
	)
}