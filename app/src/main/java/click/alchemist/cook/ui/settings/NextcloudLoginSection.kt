package click.alchemist.cook.ui.settings

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import click.alchemist.cook.service.nextcloud.NextcloudCredentials
import click.alchemist.cook.service.nextcloud.NextcloudLoginFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

private val LOGIN_TIMEOUT = 5.minutes
private const val POLL_INTERVAL_MS = 1500L

/**
 * "Server address" field + "Log in with Nextcloud" button implementing Login Flow v2: opens the
 * server's own login page in a Custom Tab (so 2FA/SSO just work, and no password is ever typed into
 * this app), then polls until the server hands back a scoped app-password or the user cancels.
 * [onLoggedIn] fires once, with the resulting credentials, on success.
 */
@Composable
fun NextcloudLoginSection(
	serverUrl: String,
	onServerUrlChange: (String) -> Unit,
	onLoggedIn: (NextcloudCredentials) -> Unit,
	loginFlow: NextcloudLoginFlow = remember { NextcloudLoginFlow() }
) {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	var waiting by remember { mutableStateOf(false) }
	var error by remember { mutableStateOf<String?>(null) }
	var loginJob by remember { mutableStateOf<Job?>(null) }

	fun startLogin() {
		error = null
		waiting = true
		loginJob = scope.launch {
			try {
				val init = loginFlow.start(serverUrl)
				CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(init.loginUrl))

				val deadline = System.currentTimeMillis() + LOGIN_TIMEOUT.inWholeMilliseconds
				var credentials: NextcloudCredentials? = null
				while (credentials == null && System.currentTimeMillis() < deadline) {
					delay(POLL_INTERVAL_MS)
					credentials = loginFlow.poll(init)
				}

				waiting = false
				if (credentials != null) onLoggedIn(credentials) else error = "Login timed out, please try again"
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				waiting = false
				error = e.message ?: "Could not log in to Nextcloud"
			}
		}
	}

	Column {
		OutlinedTextField(
			value = serverUrl,
			onValueChange = onServerUrlChange,
			modifier = Modifier.fillMaxWidth(),
			label = { Text("Server address") },
			enabled = !waiting
		)

		Spacer(Modifier.height(8.dp))

		if (waiting) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				CircularProgressIndicator(Modifier.size(20.dp))
				Spacer(Modifier.width(8.dp))
				Text("Waiting for you to log in…", Modifier.weight(1f, fill = false))
				Spacer(Modifier.width(8.dp))
				TextButton(onClick = {
					loginJob?.cancel()
					waiting = false
				}) { Text("Cancel") }
			}
		} else {
			Button(onClick = ::startLogin, enabled = serverUrl.isNotBlank()) {
				Text("Log in with Nextcloud")
			}
		}

		error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
	}
}
