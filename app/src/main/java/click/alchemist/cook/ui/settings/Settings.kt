package click.alchemist.cook.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.BackButton
import click.alchemist.cook.compose.rememberToolbarPadding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.accompanist.insets.ui.TopAppBar
import com.google.firebase.auth.FirebaseUser
import org.koin.androidx.compose.getViewModel

@Composable
fun Settings(backNavigation: () -> Unit = {}) {
	val viewModel: SettingsViewModel = getViewModel()
	val account = viewModel.account.collectAsState(initial = null)

	Settings(
		backNavigation,
		account
	)
}

@Composable
fun Settings(backNavigation: () -> Unit = {}, account: State<FirebaseUser?>) {
	val login = rememberLauncherForActivityResult(contract = FirebaseAuthUIActivityResultContract(), onResult = {})
	val context = LocalContext.current
	Scaffold(topBar = {
		TopAppBar(
			contentPadding = rememberToolbarPadding(),
			title = {
				Text("Settings")
			},
			navigationIcon = { BackButton(backNavigation) }
		)
	}) { paddingValues ->
		LazyColumn(Modifier.padding(paddingValues)) {
			item {
				AccountSettings(account.value, login = {
					// Choose authentication providers
					val providers = arrayListOf(
						AuthUI.IdpConfig.EmailBuilder().build(),
					)

					// Create and launch sign-in intent
					val signInIntent = AuthUI.getInstance()
						.createSignInIntentBuilder()
						.setAvailableProviders(providers)
						.setLogo(R.drawable.logo)
						.build()
					login.launch(signInIntent)
				}, logout = {
					AuthUI.getInstance().signOut(context)
				})
			}
		}
	}
}

@Composable
fun AccountSettings(value: FirebaseUser?, login: () -> Unit = {}, logout: () -> Unit = {}) {
	val paddingStart = 32.dp
	val paddingEnd = 32.dp

	Column(Modifier.padding(vertical = 8.dp)) {
		Row() {
			Column(Modifier.fillMaxWidth().padding(start = paddingStart, end = paddingEnd)) {
				Text(text = "Account")
				Text(text = value?.displayName ?: "Not logged in")
			}
		}
		Crossfade(
			modifier = Modifier.padding(start = paddingStart, end = paddingEnd),
			targetState = value
		) {
			if (it == null) {
				Button(onClick = login) {
					Text(text = "Login")
				}
			} else {
				Button(onClick = login) {
					Text(text = "Logout")
				}
			}
		}
	}
}


@Preview
@Composable
private fun Preview() {

	AppTheme {
		Settings(account = mutableStateOf(null))
	}
}
