package click.alchemist.cook.ui.recipe.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.MainComposeActivity
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.BackButton
import click.alchemist.cook.compose.CookIconButton
import click.alchemist.cook.compose.DurationPickerDialog
import click.alchemist.cook.compose.MarkdownEditText
import click.alchemist.cook.compose.SimpleTextField
import click.alchemist.cook.compose.SwipeDeleteBackground
import click.alchemist.cook.compose.recipe.RecipeExtendedInstruction
import click.alchemist.cook.compose.rememberToolbarPadding
import click.alchemist.cook.extension.humanReadable
import click.alchemist.cook.model.DbDuration
import click.alchemist.cook.model.RecipeGraphNode
import click.alchemist.cook.service.markdown.MarkdownService
import click.alchemist.cook.viewmodel.RecipeGraphModel
import click.alchemist.cook.viewmodel.RecipeGraphNodeModel
import org.koin.androidx.compose.get
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


@Composable
fun RecipeEditAddExtendedInstructionEntryDialog(nodeId: String?, onBackNavigation: () -> Unit) {
	val markdownService = get<MarkdownService>()

//	val viewModel = getViewModel<RecipeEditViewModel>(qualifier = named("Edit"))
	val viewModel = MainComposeActivity.editViewModel!!
	val allInstructions = viewModel.extraInstructions.value

	val editedNode = getInitialNode(nodeId, allInstructions)
	val all = allInstructions.nodes.map { it }

	val dependentNodes = remember {
		mutableStateListOf(*editedNode.dependencies.map { nodeId -> all.first { it.node.id == nodeId } }.toTypedArray())
	}

	RecipeEditAddExtendedInstructionEntryDialogContent(
		editedNode,
		dependentNodes,
		availableDependentNodes = { showAddDialog(viewModel, editedNode, dependentNodes) },
		addDependentNode = { dependentNodes.add(it) },
		deleteDependentNode = { dependentNodes.remove(it) },
		onBackNavigation = onBackNavigation,
		onSave = { text, duration ->
			val node = editedNode.copy(text = text, duration = DbDuration(duration), dependencies = dependentNodes.map { it.node.id })
			viewModel.addExtendedEntry(node)
			onBackNavigation()
		},
		markdownService = markdownService
	)
}


private fun getInitialNode(
	nodeId: String?,
	allInstructions: RecipeGraphModel
): RecipeGraphNode {
	if (nodeId != null) {
		val existingNode = allInstructions.nodes
			.find { it.node.id == nodeId }
		if (existingNode != null) {
			return existingNode.node
		}
	}

	val lastEntry = allInstructions.nodes.lastOrNull()
	val newId = UUID.randomUUID().toString()
	return if (lastEntry != null) {
		RecipeGraphNode(id = newId, dependencies = listOf(lastEntry.node.id))
	} else {
		RecipeGraphNode(id = newId)
	}
}


private fun showAddDialog(
	viewModel: RecipeEditViewModel,
	editedNode: RecipeGraphNode,
	dependentNodes: List<RecipeGraphNodeModel>
): List<RecipeGraphNodeModel> {
	val instructions = viewModel.extraInstructions.value.nodes
	return instructions
		.filter { !dependentNodes.contains(it) && it.node.id != editedNode.id }
}


@Composable
private fun RecipeEditAddExtendedInstructionEntryDialogContent(
	node: RecipeGraphNode,
	dependentNodes: List<RecipeGraphNodeModel>,
	onBackNavigation: () -> Unit = {},
	onSave: (text: String, duration: Duration) -> Unit = { _, _ -> },
	availableDependentNodes: () -> List<RecipeGraphNodeModel> = { emptyList() },
	addDependentNode: (RecipeGraphNodeModel) -> Unit = { },
	deleteDependentNode: (RecipeGraphNodeModel) -> Unit = { },
	markdownService: MarkdownService? = null
) {
	var duration by remember { mutableStateOf(node.duration.dbDuration) }
	var durationDialog by remember { mutableStateOf(false) }
	var dependencyDialog by remember { mutableStateOf(emptyList<RecipeGraphNodeModel>()) }
	var text by remember { mutableStateOf(node.text) }
	val availableDependencies = availableDependentNodes()

	Scaffold(topBar = {
		TopAppBar(
			modifier = Modifier.padding(rememberToolbarPadding()),
			title = { Text("Extended Node") },
			navigationIcon = { BackButton(onBackNavigation) },
			actions = { CookIconButton(onClick = { onSave(text, duration) }, iconResource = R.drawable.ic_content_save, contentDescription = "Save") }
		)
	}) { paddingValues ->
		Column(
			Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.padding(vertical = 8.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			MarkdownEditText(
				text, { text = it },
				Modifier
					.fillMaxWidth()
					.padding(horizontal = 8.dp),
				markdownService
			)

			Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				Text("Duration")
				SimpleTextField(
					value = duration.humanReadable(),
					onValueChange = {},
					Modifier
						.fillMaxWidth()
						.clickable { durationDialog = true },
					enabled = false,
					singleLine = true
				)
			}

			Column {
				Text("Dependent on", Modifier.padding(horizontal = 8.dp))
				if (dependentNodes.isEmpty()) {
					Text(
						"None",
						Modifier
							.padding(8.dp)
							.weight(1f)
					)
				} else {
					LazyColumn(
						Modifier
							.fillMaxWidth()
							.weight(1f),
						verticalArrangement = Arrangement.spacedBy(8.dp),
						contentPadding = PaddingValues(8.dp)
					) {
						items(dependentNodes, key = { it.node.id }) { nodeModel ->
							val dismissState = rememberSwipeToDismissBoxState(
								confirmValueChange = {
									val dismissed = it == SwipeToDismissBoxValue.StartToEnd || it == SwipeToDismissBoxValue.EndToStart
									if (dismissed) deleteDependentNode(nodeModel)
									dismissed
								}
							)
							SwipeToDismissBox(
								state = dismissState,
								backgroundContent = { SwipeDeleteBackground(dismissState) }) {
								RecipeExtendedInstruction(nodeModel)
							}
						}
					}
				}

				AnimatedVisibility(availableDependencies.isNotEmpty()) {
					Button(
						onClick = { dependencyDialog = availableDependencies }, modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 8.dp)
							.wrapContentWidth()
					) {
						Text(text = "Add Dependency")
					}
				}
			}
		}
	}

	if (durationDialog) {
		DurationPickerDialog(duration,
			{
				duration = it
				durationDialog = false
			}, { durationDialog = false })
	}

	val addDependentNodes = dependencyDialog
	if (addDependentNodes.isNotEmpty()) {
		AddExtendedInstructionDependencyDialog(addDependentNodes,
			onSelected = {
				addDependentNode(it)
				dependencyDialog = emptyList()
			}, onDismiss = { dependencyDialog = emptyList() })
	}
}


@Preview
@Composable
private fun Preview() {
	AppTheme {
		RecipeEditAddExtendedInstructionEntryDialogContent(
			RecipeGraphNode(duration = DbDuration(5.minutes)),
			listOf(RecipeGraphNodeModel(RecipeGraphNode(text = "Text"), ""))
		)
	}
}