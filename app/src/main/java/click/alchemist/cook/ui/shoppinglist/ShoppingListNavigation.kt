package click.alchemist.cook.ui.shoppinglist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import click.alchemist.cook.ui.shoppinglist.add.ShoppingListAddIngredient
import click.alchemist.cook.ui.shoppinglist.detail.ShoppingListDetail
import click.alchemist.cook.ui.shoppinglist.overview.ShoppingListOverview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
fun NavGraphBuilder.ShoppingListNavigation(navController: NavController, maxWidth: Dp) {
	val isWide = maxWidth >= 600.dp
	composable(ShoppingScreen.Overview.route) {
		ShoppingListOverview(Modifier) {
			navController.navigate("shoppinglist/${it.shoppingList.id}")
		}
	}

	composable(ShoppingScreen.Detail.route) { backStackEntry ->
		val id = backStackEntry.arguments?.getString("id") ?: ""
		ShoppingListDetail(
			id,
			backNavigation = navController::navigateUp,
			navigateToAddItem = getNavigate(isWide, navController)
		)
	}

	composable(ShoppingScreen.DetailAdd.route) { backStackEntry ->
		val id = backStackEntry.arguments?.getString("id") ?: ""

		if (isWide) {
			navController.navigateUp()
		} else {
			ShoppingListAddIngredient(id, backNavigation = { navController.navigateUp() })
		}
	}
}

private fun getNavigate(isWide: Boolean, navController: NavController): ((shoppingListId: String) -> Unit)? {
	if (isWide) {
		return null
	}
	return { navController.navigate("shoppinglist/${it}/add") }
}

sealed class ShoppingScreen(val route: String) {
	object Overview : ShoppingScreen("shoppinglist")
	object Detail : ShoppingScreen("shoppinglist/{id}")
	object DetailAdd : ShoppingScreen("shoppinglist/{id}/add")
}