package click.alchemist.cook.ui.settings

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import click.alchemist.cook.LocaleHelper
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.previewLibraries
import click.alchemist.cook.service.store.LibraryConfig
import org.koin.androidx.viewmodel.ext.android.viewModel

class LibraryManagementActivity : ComponentActivity() {
	private val viewModel: SettingsViewModel by viewModel()

	override fun attachBaseContext(newBase: Context?) {
		super.attachBaseContext(if (newBase == null) null else LocaleHelper.onAttach(newBase))
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			AppTheme {
				val libraries by viewModel.sharedLibraries.collectAsState(emptyList())
				LibraryManagementScreen(
					libraries = libraries,
					onBack = { finish() },
					onAdd = { label, url, username, password -> viewModel.addSharedLibrary(label, url, username, password) },
					onRemove = { viewModel.removeSharedLibrary(it) }
				)
			}
		}
	}

	companion object {
		fun intent(context: Context) = Intent(context, LibraryManagementActivity::class.java)
	}
}

@Composable
private fun LibraryManagementScreen(
	libraries: List<LibraryConfig>,
	onBack: () -> Unit,
	onAdd: (label: String, url: String, username: String, password: String) -> Unit,
	onRemove: (id: String) -> Unit
) {
	var showAddDialog by remember { mutableStateOf(false) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.settings_shared_libraries)) },
				navigationIcon = { click.alchemist.cook.compose.BackButton(onBack) }
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
					supportingContent = { Text(library.webDav.baseUrl) },
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
			onConfirm = { label, url, username, password ->
				onAdd(label, url, username, password)
				showAddDialog = false
			}
		)
	}
}

@Composable
private fun AddLibraryDialog(
	onDismiss: () -> Unit,
	onConfirm: (label: String, url: String, username: String, password: String) -> Unit
) {
	var label by remember { mutableStateOf("") }
	var url by remember { mutableStateOf("") }
	var username by remember { mutableStateOf("") }
	var password by remember { mutableStateOf("") }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Add shared library") },
		text = {
			Column {
				OutlinedTextField(label, { label = it }, Modifier.fillMaxWidth(), label = { Text("Name") })
				OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("Server URL") })
				OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") })
				OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") })
			}
		},
		confirmButton = {
			TextButton(onClick = { onConfirm(label, url, username, password) }) { Text("Add") }
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
		LibraryManagementScreen(previewLibraries(), onBack = {}, onAdd = { _, _, _, _ -> }, onRemove = {})
	}
}

@Preview("Shared Libraries Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun LibraryManagementScreenDarkPreview() {
	AppTheme {
		LibraryManagementScreen(previewLibraries(), onBack = {}, onAdd = { _, _, _, _ -> }, onRemove = {})
	}
}

@Preview("Shared Libraries Empty")
@Composable
private fun LibraryManagementScreenEmptyPreview() {
	AppTheme {
		LibraryManagementScreen(emptyList(), onBack = {}, onAdd = { _, _, _, _ -> }, onRemove = {})
	}
}

@Preview("Add Shared Library")
@Composable
private fun AddLibraryDialogPreview() {
	AppTheme {
		AddLibraryDialog(onDismiss = {}, onConfirm = { _, _, _, _ -> })
	}
}
