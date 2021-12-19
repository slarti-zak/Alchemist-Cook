package click.alchemist.cook.ui.recipe.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import click.alchemist.cook.compose.RecipeText
import click.alchemist.cook.service.markdown.MarkdownService

@Composable
fun RecipeDetailInstruction(instructions: String, modifier: Modifier = Modifier, markdownService: MarkdownService? = null, sp: TextUnit = 12.sp) {
//	val padding = with(LocalDensity.current) { 16.dp.roundToPx() }
	Column(
		modifier = modifier.then(
			Modifier
				.verticalScroll(rememberScrollState())
		)
	) {
		RecipeText(
			"&nbsp;\n$instructions\n\n&nbsp;",
			Modifier.fillMaxWidth().padding(horizontal = 16.dp),
			markdownService,
			sp
		)
	}
}