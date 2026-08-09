package click.alchemist.cook.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.BackButton
import click.alchemist.cook.compose.previewLibraries
import click.alchemist.cook.service.store.LibraryConfig
import click.alchemist.cook.service.store.LibraryConnection

/** See [SettingsNavigation]. */
@Composable
fun LibraryManagementScreen(
	libraries: List<LibraryConfig>,
	onBack: () -> Unit,
	onAdd: (label: String, connection: LibraryConnection) -> Unit,
	onRemove: (id: String) -> Unit
) {
	var showAddDialog by remember { mutableStateOf(false) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.settings_shared_libraries)) },
				navigationIcon = { BackButton(onBack) }
			)
		},
		floatingActionButton = {
			FloatingActionButton(onClick = { showAddDialog = true }) {
				Icon(painterResource(R.drawable.ic_plus), "Add shared library")
			}
		}
	) { paddingValues ->
		LazyColumn(Modifier.padding(paddingValues)) {
			items(libraries, key = { it.id }) { library ->
				ListItem(
					headlineContent = { Text(library.label) },
					supportingContent = { Text(library.connection.summary()) },
					trailingContent = {
						TextButton(onClick = { onRemove(library.id) }) { Text("Remove") }
					}
				)
			}
		}
	}

	if (showAddDialog) {
		AddLibraryDialog(
			onDismiss = { showAddDialog = false },
			onConfirm = { label, connection ->
				onAdd(label, connection)
				showAddDialog = false
			}
		)
	}
}

private fun LibraryConnection.summary(): String = when (this) {
	is LibraryConnection.WebDav -> config.baseUrl
	is LibraryConnection.Nextcloud -> serverUrl
	is LibraryConnection.LocalFolder -> "Local folder: $displayName"
}

@Composable
private fun AddLibraryDialog(
	onDismiss: () -> Unit,
	onConfirm: (label: String, connection: LibraryConnection) -> Unit
) {
	var label by remember { mutableStateOf("") }
	var connection by remember { mutableStateOf<LibraryConnection?>(null) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Add shared library") },
		text = {
			Column {
				OutlinedTextField(label, { label = it }, Modifier.fillMaxWidth(), label = { Text("Name") })
				LibraryConnectionEditor(onConnectionChange = { connection = it })
			}
		},
		confirmButton = {
			TextButton(
				enabled = label.isNotBlank() && connection != null,
				onClick = { connection?.let { onConfirm(label, it) } }
			) { Text("Add") }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text("Cancel") }
		}
	)
}

@Preview("Shared Libraries")
@Composable
private fun LibraryManagementScreenPreview() {
	AppTheme {
		LibraryManagementScreen(previewLibraries(), onBack = {}, onAdd = { _, _ -> }, onRemove = {})
	}
}

@Preview("Shared Libraries Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun LibraryManagementScreenDarkPreview() {
	AppTheme {
		LibraryManagementScreen(previewLibraries(), onBack = {}, onAdd = { _, _ -> }, onRemove = {})
	}
}

@Preview("Shared Libraries Empty")
@Composable
private fun LibraryManagementScreenEmptyPreview() {
	AppTheme {
		LibraryManagementScreen(emptyList(), onBack = {}, onAdd = { _, _ -> }, onRemove = {})
	}
}

@Preview("Add Shared Library")
@Composable
private fun AddLibraryDialogPreview() {
	AppTheme {
		AddLibraryDialog(onDismiss = {}, onConfirm = { _, _ -> })
	}
}
