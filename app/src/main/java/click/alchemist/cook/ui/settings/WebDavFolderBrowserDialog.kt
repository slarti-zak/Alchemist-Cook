package click.alchemist.cook.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import click.alchemist.cook.service.webdav.WebDavClient
import click.alchemist.cook.service.webdav.WebDavConfig

/** Kept fixed across loading/error/list states so the dialog doesn't visibly resize between them. */
private val CONTENT_WIDTH = 280.dp

/**
 * Lets the user navigate the WebDAV tree rooted at [rootConfig] and pick a subfolder to use as a
 * library's root instead of the whole account — e.g. a "Recipes" folder inside a Nextcloud account
 * that also holds unrelated files. [onSelect] reports the chosen folder as a path relative to
 * [rootConfig]'s own `baseUrl` ("" for the root itself, with no navigation).
 */
@Composable
fun WebDavFolderBrowserDialog(
	rootConfig: WebDavConfig,
	onDismiss: () -> Unit,
	onSelect: (relativePath: String) -> Unit
) {
	val client = remember(rootConfig) { WebDavClient(rootConfig) }
	var path by remember { mutableStateOf("") }
	var folders by remember { mutableStateOf<List<String>>(emptyList()) }
	var loading by remember { mutableStateOf(true) }
	var error by remember { mutableStateOf<String?>(null) }

	LaunchedEffect(path) {
		loading = true
		error = null
		try {
			folders = client.propfind(path, depth = 1).filter { it.isCollection }.map { it.path }.sortedBy { it.lowercase() }
		} catch (e: Exception) {
			error = e.message ?: "Could not list folders"
		}
		loading = false
	}

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(if (path.isBlank()) "/" else "/$path") },
		text = {
			// Fixed width regardless of state, so switching between a small spinner, a short error
			// message and a wide folder list doesn't visibly resize the dialog.
			Box(Modifier.width(CONTENT_WIDTH)) {
				when {
					loading -> CircularProgressIndicator(Modifier.padding(vertical = 24.dp).align(Alignment.Center))
					error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
					folders.isEmpty() -> Text("No subfolders here")
					else -> LazyColumn(Modifier.heightIn(max = 320.dp)) {
						items(folders, key = { it }) { folder ->
							Text(
								text = folder.substringAfterLast('/'),
								modifier = Modifier
									.fillMaxWidth()
									.clickable { path = folder }
									.padding(vertical = 12.dp)
							)
						}
					}
				}
			}
		},
		confirmButton = {
			TextButton(onClick = { onSelect(path) }) { Text("Use this folder") }
		},
		dismissButton = {
			Row {
				if (path.isNotBlank()) {
					TextButton(onClick = { path = path.substringBeforeLast('/', "") }) { Text("Up") }
				}
				TextButton(onClick = onDismiss) { Text("Cancel") }
			}
		}
	)
}
