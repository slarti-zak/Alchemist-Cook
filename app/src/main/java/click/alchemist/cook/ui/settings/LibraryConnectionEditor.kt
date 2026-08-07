package click.alchemist.cook.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import click.alchemist.cook.service.nextcloud.NextcloudCredentials
import click.alchemist.cook.service.store.LibraryConnection
import click.alchemist.cook.service.webdav.WebDavConfig

private enum class LibraryConnectionKind(val label: String) {
	WEBDAV("WebDAV"),
	NEXTCLOUD("Nextcloud"),
	LOCAL_FOLDER("Local folder")
}

private fun LibraryConnection?.toKind(): LibraryConnectionKind = when (this) {
	is LibraryConnection.WebDav -> LibraryConnectionKind.WEBDAV
	is LibraryConnection.Nextcloud -> LibraryConnectionKind.NEXTCLOUD
	is LibraryConnection.LocalFolder -> LibraryConnectionKind.LOCAL_FOLDER
	null -> LibraryConnectionKind.WEBDAV
}

/**
 * Type selector (WebDAV / Nextcloud / Local folder) plus the fields for whichever is selected.
 * Reports the fully-formed [LibraryConnection] via [onConnectionChange] whenever the currently
 * selected type has everything it needs, or null while it doesn't (callers use that to gate their
 * "Add"/"Save" action). [initial] seeds the editor when editing an already-configured library.
 */
@Composable
fun LibraryConnectionEditor(
	initial: LibraryConnection? = null,
	onConnectionChange: (LibraryConnection?) -> Unit,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	var kind by remember { mutableStateOf(initial.toKind()) }

	var url by remember { mutableStateOf((initial as? LibraryConnection.WebDav)?.config?.baseUrl.orEmpty()) }
	var username by remember { mutableStateOf((initial as? LibraryConnection.WebDav)?.config?.username.orEmpty()) }
	var password by remember { mutableStateOf((initial as? LibraryConnection.WebDav)?.config?.password.orEmpty()) }

	var nextcloudServerUrl by remember { mutableStateOf((initial as? LibraryConnection.Nextcloud)?.serverUrl.orEmpty()) }
	var nextcloudCredentials by remember { mutableStateOf<NextcloudCredentials?>(null) }
	val nextcloudAlreadyConnected = initial is LibraryConnection.Nextcloud

	var folderUri by remember { mutableStateOf((initial as? LibraryConnection.LocalFolder)?.treeUri) }
	var folderDisplayName by remember { mutableStateOf((initial as? LibraryConnection.LocalFolder)?.displayName.orEmpty()) }

	val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
		if (uri == null) return@rememberLauncherForActivityResult
		context.contentResolver.takePersistableUriPermission(
			uri,
			Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
		)
		folderUri = uri.toString()
		folderDisplayName = DocumentFile.fromTreeUri(context, uri)?.name ?: uri.lastPathSegment.orEmpty()
	}

	val connection = when (kind) {
		LibraryConnectionKind.WEBDAV ->
			if (url.isNotBlank() && username.isNotBlank()) LibraryConnection.WebDav(WebDavConfig(url, username, password)) else null

		LibraryConnectionKind.NEXTCLOUD -> when {
			nextcloudCredentials != null -> LibraryConnection.Nextcloud(nextcloudCredentials!!.toWebDavConfig(), nextcloudServerUrl)
			nextcloudAlreadyConnected -> initial
			else -> null
		}

		LibraryConnectionKind.LOCAL_FOLDER ->
			folderUri?.let { LibraryConnection.LocalFolder(it, folderDisplayName) }
	}

	LaunchedEffect(connection) { onConnectionChange(connection) }

	Column(modifier) {
		SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
			LibraryConnectionKind.entries.forEachIndexed { index, entry ->
				SegmentedButton(
					selected = kind == entry,
					onClick = { kind = entry },
					shape = SegmentedButtonDefaults.itemShape(index, LibraryConnectionKind.entries.size)
				) { Text(entry.label) }
			}
		}

		when (kind) {
			LibraryConnectionKind.WEBDAV -> {
				OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("Server URL") })
				OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") })
				OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") })
			}

			LibraryConnectionKind.NEXTCLOUD -> {
				NextcloudLoginSection(
					serverUrl = nextcloudServerUrl,
					onServerUrlChange = { nextcloudServerUrl = it },
					onLoggedIn = { nextcloudCredentials = it }
				)
				val loginName = nextcloudCredentials?.loginName ?: (initial as? LibraryConnection.Nextcloud)?.config?.username
				if (loginName != null) {
					Text("Connected as $loginName", color = MaterialTheme.colorScheme.primary)
				}
			}

			LibraryConnectionKind.LOCAL_FOLDER -> {
				Button(onClick = { folderPicker.launch(null) }) { Text("Choose folder") }
				if (folderDisplayName.isNotBlank()) {
					Text("Selected: $folderDisplayName")
				}
			}
		}
	}
}
