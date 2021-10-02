package click.alchemist.cook.ui.recipe.detail

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import click.alchemist.cook.compose.recipe.RecipeExtendedInstructions
import click.alchemist.cook.service.markdown.MarkdownService
import click.alchemist.cook.viewmodel.RecipeGraphModel

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun RecipeDetailExtendedInstruction(graph: RecipeGraphModel, markdownService: MarkdownService? = null) {
	RecipeExtendedInstructions(
		graph,
		Modifier.fillMaxSize(),
		contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 100.dp),
		markdownService = markdownService
	)
}