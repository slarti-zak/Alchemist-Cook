package click.alchemist.cook.ui.settings

import android.app.Activity
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import click.alchemist.cook.BuildConfig
import org.koin.androidx.compose.koinViewModel


fun NavGraphBuilder.SettingsNavigation(navController: NavController) {
	composable(SettingsScreen.Home.route) {
		val context = LocalContext.current
		val viewModel = koinViewModel<SettingsViewModel>()
		val language by viewModel.language.collectAsState("")

		SettingsHome(
			versionInfo = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
			language = language,
			onLanguageChange = {
				viewModel.setLanguage(it)
				// Only takes effect via attachBaseContext on the next Activity creation.
				(context as? Activity)?.recreate()
			},
			onBack = navController::navigateUp,
			onStorageClick = { navController.navigate(SettingsScreen.Storage.route) },
			onSharedLibrariesClick = { navController.navigate(SettingsScreen.SharedLibraries.route) },
			onSyncNowClick = {
				viewModel.syncNow()
				Toast.makeText(context, "Sync started", Toast.LENGTH_SHORT).show()
			}
		)
	}

	composable(SettingsScreen.Storage.route) {
		val viewModel = koinViewModel<SettingsViewModel>()
		// Seeded synchronously so the form doesn't briefly render empty before the Flow catches up.
		val library by viewModel.personalLibrary.collectAsState(initial = viewModel.personalLibrary())
		PersonalLibraryScreen(
			library = library,
			onBack = navController::navigateUp,
			onSave = { label, connection ->
				viewModel.setPersonalLibrary(label, connection)
				navController.navigateUp()
			}
		)
	}

	composable(SettingsScreen.SharedLibraries.route) {
		val viewModel = koinViewModel<SettingsViewModel>()
		val libraries by viewModel.sharedLibraries.collectAsState(emptyList())
		LibraryManagementScreen(
			libraries = libraries,
			onBack = navController::navigateUp,
			onAdd = { label, connection -> viewModel.addSharedLibrary(label, connection) },
			onRemove = { viewModel.removeSharedLibrary(it) }
		)
	}
}

sealed class SettingsScreen(val route: String) {
	data object Home : SettingsScreen("settings")
	data object Storage : SettingsScreen("settings/storage")
	data object SharedLibraries : SettingsScreen("settings/shared_libraries")
}
