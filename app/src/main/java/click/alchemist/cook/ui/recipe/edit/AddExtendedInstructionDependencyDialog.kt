package click.alchemist.cook.ui.recipe.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import click.alchemist.cook.R
import click.alchemist.cook.compose.recipe.RecipeExtendedInstruction
import click.alchemist.cook.model.RecipeGraphNode
import click.alchemist.cook.service.markdown.MarkdownService
import click.alchemist.cook.viewmodel.RecipeGraphNodeModel


@Composable
fun AddExtendedInstructionDependencyDialog(
	dependentNodes: List<RecipeGraphNodeModel>,
	onDismiss: () -> Unit,
	onSelected: (RecipeGraphNodeModel) -> Unit = {},
	markdownService: MarkdownService? = null
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = {},
		dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.general_cancel)) } },
		title = { /* Don't put title here as it gets cut off when the lazy column grows in height  */ },
		text = {
			Column {
				CompositionLocalProvider(
					LocalTextStyle provides MaterialTheme.typography.bodySmall
				) {
					Text(stringResource(R.string.recipe_edit_pick_node))
				}
				LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(8.dp)) {
					items(dependentNodes, key = { it.node.id }) { node ->
						RecipeExtendedInstruction(node = node, onClick = { onSelected(it) }, markdownService = markdownService)
					}
				}
			}
		},
		properties = DialogProperties(usePlatformDefaultWidth = true)
	)
}

@Preview
@Composable
private fun Preview() {
	AddExtendedInstructionDependencyDialog(listOf(RecipeGraphNodeModel(RecipeGraphNode(text = "T1"), "")), {})
}