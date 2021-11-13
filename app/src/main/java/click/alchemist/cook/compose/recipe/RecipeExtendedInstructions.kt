package click.alchemist.cook.compose.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.previewExtendedInstruction
import click.alchemist.cook.service.markdown.MarkdownService
import click.alchemist.cook.viewmodel.RecipeGraphModel
import click.alchemist.cook.viewmodel.RecipeGraphNodeModel


@Composable
fun RecipeExtendedInstructions(
    graphModel: RecipeGraphModel,
    modifier: Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    onClick: ((RecipeGraphNodeModel) -> Unit)? = null,
    onSwipeDelete: ((RecipeGraphNodeModel) -> Unit)? = null,
    onFinished: ((RecipeGraphNodeModel) -> Unit) = {},
    onTimerToggle: ((RecipeGraphNodeModel) -> Unit) = {},
    onAddMinute: ((RecipeGraphNodeModel) -> Unit) = {},
    markdownService: MarkdownService? = null
) {
    LazyColumn(
        modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            items(graphModel.nodes, key = { it.node.id }) {
                RecipeExtendedInstruction(
                    it,
                    onClick = onClick,
                    onSwipeDelete = onSwipeDelete,
                    onFinished = onFinished,
                    onTimerToggle = onTimerToggle,
                    onAddMinute = onAddMinute,
                    markdownService = markdownService
                )
            }
        })
}


@Preview
@Composable
private fun Preview() {
    AppTheme {
        RecipeExtendedInstructions(
            previewExtendedInstruction(),
            Modifier.fillMaxSize()
        )
    }
}