package click.alchemist.cook.ui.recipe.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import click.alchemist.cook.R
import click.alchemist.cook.compose.recipe.RecipeExtendedInstruction
import click.alchemist.cook.service.markdown.MarkdownService
import click.alchemist.cook.viewmodel.RecipeGraphNodeModel


@Composable
fun AddExtendedInstructionDependencyDialog(
	dependentNodes: List<RecipeGraphNodeModel>,
	onDismiss: () -> Unit,
	onSelected: (RecipeGraphNodeModel) -> Unit = {},
	markdownService: MarkdownService? = null
) {
	AlertDialog(onDismissRequest = onDismiss,
		confirmButton = {},
		dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.general_cancel)) } },
		title = { Text("Pick Node") },
		text = {
			LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(8.dp)) {
				items(dependentNodes, key = { it.node.id }) {
					RecipeExtendedInstruction(node = it, onClick = { onSelected(it) }, markdownService = markdownService)
				}
			}
		},
		properties = DialogProperties(usePlatformDefaultWidth = true)
	)
}