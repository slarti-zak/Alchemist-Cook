package click.alchemist.cook.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.documentfile.provider.DocumentFile
import click.alchemist.cook.service.nextcloud.NextcloudCredentials
import click.alchemist.cook.service.store.LibraryConnection
import click.alchemist.cook.service.webdav.WebDavConfig

private enum class LibraryConnectionKind(val label: String) {
	WEBDAV("WebDAV"),
	NEXTCLOUD("Nextcloud"),
	LOCAL_FOLDER("Local")
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
 *
 * For WebDAV/Nextcloud, once there's enough to connect with (typed-in credentials, or a completed
 * Nextcloud login), a [WebDavFolderBrowserDialog] lets the user pick a subfolder of that account to
 * use as the library's root instead of the whole thing — e.g. a "Recipes" folder inside a Nextcloud
 * account that also holds unrelated files.
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
	var webDavSubPath by remember { mutableStateOf((initial as? LibraryConnection.WebDav)?.rootPath.orEmpty()) }

	var nextcloudServerUrl by remember { mutableStateOf((initial as? LibraryConnection.Nextcloud)?.serverUrl.orEmpty()) }
	var nextcloudCredentials by remember { mutableStateOf<NextcloudCredentials?>(null) }
	var nextcloudSubPath by remember { mutableStateOf((initial as? LibraryConnection.Nextcloud)?.rootPath.orEmpty()) }
	// The account's DAV root to build the final connection (and browse folders) from: freshly logged
	// in this session, or — when editing an already-connected library without re-logging in — the one
	// it was already configured with.
	val nextcloudRootConfig = nextcloudCredentials?.toWebDavConfig() ?: (initial as? LibraryConnection.Nextcloud)?.config

	var folderUri by remember { mutableStateOf((initial as? LibraryConnection.LocalFolder)?.treeUri) }
	var folderDisplayName by remember { mutableStateOf((initial as? LibraryConnection.LocalFolder)?.displayName.orEmpty()) }

	var showBrowser by remember { mutableStateOf(false) }

	val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
		if (uri == null) return@rememberLauncherForActivityResult
		context.contentResolver.takePersistableUriPermission(
			uri,
			Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
		)
		folderUri = uri.toString()
		folderDisplayName = DocumentFile.fromTreeUri(context, uri)?.name ?: uri.lastPathSegment.orEmpty()
	}

	// The plain account root, with no picked subfolder folded in — kept separate (see
	// LibraryConnection.rootPath) so it stays around as its own field when re-opening the editor,
	// instead of being invisibly baked into the URL, and so re-browsing starts from the true account
	// root rather than wherever a previous browse session left off.
	val webDavAccountConfig = if (url.isNotBlank() && username.isNotBlank()) WebDavConfig(url, username, password) else null

	val connection = when (kind) {
		LibraryConnectionKind.WEBDAV ->
			webDavAccountConfig?.let { LibraryConnection.WebDav(it, rootPath = webDavSubPath) }

		LibraryConnectionKind.NEXTCLOUD -> nextcloudRootConfig?.let { root ->
			LibraryConnection.Nextcloud(root, nextcloudServerUrl, rootPath = nextcloudSubPath)
		}

		LibraryConnectionKind.LOCAL_FOLDER ->
			folderUri?.let { LibraryConnection.LocalFolder(it, folderDisplayName) }
	}

	LaunchedEffect(connection) { onConnectionChange(connection) }

	// What to browse when "Browse"/"Choose a subfolder" is tapped.
	val browseRootConfig = when (kind) {
		LibraryConnectionKind.WEBDAV -> webDavAccountConfig
		LibraryConnectionKind.NEXTCLOUD -> nextcloudRootConfig
		LibraryConnectionKind.LOCAL_FOLDER -> null
	}

	Column(modifier) {
		SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
			LibraryConnectionKind.entries.forEachIndexed { index, entry ->
				SegmentedButton(
					selected = kind == entry,
					onClick = { kind = entry },
					shape = SegmentedButtonDefaults.itemShape(index, LibraryConnectionKind.entries.size)
				) {
					Text(entry.label, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
				}
			}
		}

		when (kind) {
			LibraryConnectionKind.WEBDAV -> {
				OutlinedTextField(
					url,
					{ url = it; webDavSubPath = "" }, // a changed root invalidates whatever subfolder was picked under the old one
					Modifier.fillMaxWidth(),
					label = { Text("Server URL") },
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
				)
				OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") })
				OutlinedTextField(
					password,
					{ password = it },
					Modifier.fillMaxWidth(),
					label = { Text("Password") },
					visualTransformation = PasswordVisualTransformation(),
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
				)

				TextButton(enabled = browseRootConfig != null, onClick = { showBrowser = true }) {
					Text("Browse for a subfolder…")
				}
				if (webDavSubPath.isNotBlank()) {
					Text("Root folder: /$webDavSubPath")
				}
			}

			LibraryConnectionKind.NEXTCLOUD -> {
				NextcloudLoginSection(
					serverUrl = nextcloudServerUrl,
					onServerUrlChange = { nextcloudServerUrl = it },
					onLoggedIn = { credentials ->
						nextcloudCredentials = credentials
						nextcloudSubPath = "" // forget any subfolder picked under a previous account
						showBrowser = true // straight into folder selection — no reason to make the user ask for it
					}
				)
				val loginName = nextcloudCredentials?.loginName ?: (initial as? LibraryConnection.Nextcloud)?.config?.username
				if (loginName != null) {
					Text("Connected as $loginName", color = MaterialTheme.colorScheme.primary)
				}
				if (nextcloudRootConfig != null) {
					TextButton(onClick = { showBrowser = true }) { Text("Choose a subfolder…") }
				}
				if (nextcloudSubPath.isNotBlank()) {
					Text("Root folder: /$nextcloudSubPath")
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

	if (showBrowser && browseRootConfig != null) {
		WebDavFolderBrowserDialog(
			rootConfig = browseRootConfig,
			onDismiss = { showBrowser = false },
			onSelect = { relativePath ->
				when (kind) {
					LibraryConnectionKind.WEBDAV -> webDavSubPath = relativePath
					LibraryConnectionKind.NEXTCLOUD -> nextcloudSubPath = relativePath
					LibraryConnectionKind.LOCAL_FOLDER -> Unit
				}
				showBrowser = false
			}
		)
	}
}
