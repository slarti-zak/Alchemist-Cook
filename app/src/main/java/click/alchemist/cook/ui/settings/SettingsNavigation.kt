package click.alchemist.cook.ui.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable


fun NavGraphBuilder.SettingsNavigation(navController: NavController) {

	composable(SettingsScreen.Overview.route) { backStackEntry ->
		Settings(backNavigation = { navController.navigateUp() })
	}
}

sealed class SettingsScreen(val route: String) {
	object Overview : SettingsScreen("settings")
}