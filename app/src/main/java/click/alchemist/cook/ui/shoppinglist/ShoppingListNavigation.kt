package click.alchemist.cook.ui.shoppinglist

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import click.alchemist.cook.ui.shoppinglist.add.ShoppingListAddIngredient
import click.alchemist.cook.ui.shoppinglist.detail.ShoppingListDetail
import click.alchemist.cook.ui.shoppinglist.overview.ShoppingListOverview


fun NavGraphBuilder.ShoppingListNavigation(
	navController: NavController,
	maxWidth: Dp,
	sharedTransitionScope: SharedTransitionScope
) {
	val isWide = maxWidth >= 600.dp
	composable(ShoppingScreen.Overview.route) {
		ShoppingListOverview(
			onShoppingListClick = {
				navController.navigate("shoppinglist/${it.shoppingList.id}")
			},
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = this@composable
		)
	}

	composable(ShoppingScreen.Detail.route) { backStackEntry ->
		val id = backStackEntry.arguments?.getString("id") ?: ""
		ShoppingListDetail(
			id,
			backNavigation = navController::navigateUp,
			navigateToAddItem = getNavigate(isWide, navController),
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = this@composable
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
	data object Overview : ShoppingScreen("shoppinglist")
	data object Detail : ShoppingScreen("shoppinglist/{id}")
	data object DetailAdd : ShoppingScreen("shoppinglist/{id}/add")
}