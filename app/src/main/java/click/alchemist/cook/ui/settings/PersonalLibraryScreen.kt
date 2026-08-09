package click.alchemist.cook.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.BackButton
import click.alchemist.cook.compose.previewLibraries
import click.alchemist.cook.service.store.LibraryConfig
import click.alchemist.cook.service.store.LibraryConnection

/**
 * Where the personal library's storage is configured — replaces the old raw WebDAV
 * `EditTextPreference`s in `root_preferences.xml`, which couldn't cleanly show different fields per
 * connection type. Mirrors [LibraryManagementScreen]'s "add shared library" form/editor. See
 * [SettingsNavigation].
 */
@Composable
fun PersonalLibraryScreen(
	library: LibraryConfig?,
	onBack: () -> Unit,
	onSave: (label: String, connection: LibraryConnection) -> Unit
) {
	var label by remember { mutableStateOf(library?.label ?: "Personal") }
	var connection by remember { mutableStateOf<LibraryConnection?>(null) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Storage") },
				navigationIcon = { BackButton(onBack) }
			)
		}
	) { paddingValues ->
		Column(Modifier.padding(paddingValues).padding(16.dp)) {
			OutlinedTextField(label, { label = it }, Modifier.fillMaxWidth(), label = { Text("Name") })
			Spacer(Modifier.height(8.dp))
			LibraryConnectionEditor(initial = library?.connection, onConnectionChange = { connection = it })
			Spacer(Modifier.height(16.dp))
			Button(
				enabled = label.isNotBlank() && connection != null,
				onClick = { connection?.let { onSave(label, it) } }
			) { Text("Save") }
		}
	}
}

@Preview("Personal Library")
@Composable
private fun PersonalLibraryScreenPreview() {
	AppTheme {
		PersonalLibraryScreen(previewLibraries().first(), onBack = {}, onSave = { _, _ -> })
	}
}

@Preview("Personal Library Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun PersonalLibraryScreenDarkPreview() {
	AppTheme {
		PersonalLibraryScreen(previewLibraries().first(), onBack = {}, onSave = { _, _ -> })
	}
}

@Preview("Personal Library Empty")
@Composable
private fun PersonalLibraryScreenEmptyPreview() {
	AppTheme {
		PersonalLibraryScreen(null, onBack = {}, onSave = { _, _ -> })
	}
}
