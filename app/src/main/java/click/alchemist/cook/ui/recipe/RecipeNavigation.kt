package click.alchemist.cook.ui.recipe

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import click.alchemist.cook.ui.recipe.detail.RecipeDetail
import click.alchemist.cook.ui.recipe.edit.RecipeEdit
import click.alchemist.cook.ui.recipe.edit.RecipeEditAddExtendedInstructionEntryDialog
import click.alchemist.cook.ui.recipe.edit.RecipeEditViewModel
import click.alchemist.cook.ui.recipe.list.RecipeList
import click.alchemist.cook.ui.recipe.shopping.RecipeShopping
import click.alchemist.cook.ui.settings.SettingsScreen
import click.alchemist.cook.viewmodel.Serving
import org.koin.androidx.compose.koinViewModel


fun NavGraphBuilder.RecipeNavigation(
	navController: NavController,
	sharedTransitionScope: SharedTransitionScope
) {
	composable(RecipeScreen.List.route) {
		RecipeList(
			onSettingsClick = { navController.navigate(SettingsScreen.Home.route) },
			onRecipeClick = { navController.navigate("recipe/view/${it.recipe.id}") },
			onAddRecipe = { navController.navigate("recipe/edit") },
			sharedTransitionScope = sharedTransitionScope,
			animatedContentScope = this@composable
		)
	}

	composable(RecipeScreen.Detail.route) { backStackEntry ->
		val id = backStackEntry.arguments?.getString("id") ?: return@composable
		RecipeDetail(
			id,
			onBackNavigation = navController::navigateUp,
			onEdit = { navController.navigate("recipe/edit?id=$id") },
			navigateShopping = { recipeId, recipeServings, servings ->
				navController.navigate("recipe/shopping/$recipeId/$recipeServings/$servings")
			},
			sharedTransitionScope = sharedTransitionScope,
			animatedContentScope = this@composable
		)
	}

	composable(
		RecipeScreen.Shopping.route
	) { backStackEntry ->
		val id = backStackEntry.arguments?.getString("id") ?: return@composable
		val recipeServings = backStackEntry.arguments?.getInt("recipeServings") ?: return@composable
		val servings = backStackEntry.arguments?.getInt("servings") ?: return@composable
		RecipeShopping(
			id,
			Serving(recipeServings, servings),
			onBackNavigation = navController::navigateUp
		)
	}

	navigation(startDestination = RecipeScreen.Edit.route, route = RecipeScreen.EditFlow.route) {
		composable(
			RecipeScreen.Edit.route,
			arguments = listOf(navArgument("id") { nullable = true })
		) { backStackEntry ->
			val id = backStackEntry.arguments?.getString("id")
			val editFlowEntry = remember(backStackEntry) {
				navController.getBackStackEntry(RecipeScreen.EditFlow.route)
			}
			val viewModel = koinViewModel<RecipeEditViewModel>(viewModelStoreOwner = editFlowEntry)
			RecipeEdit(
				id,
				viewModel,
				onBackNavigation = navController::navigateUp,
				onSaved = { savedRecipeId ->
					if (id == null) {
						navController.navigate("recipe/view/$savedRecipeId") {
							popUpTo(RecipeScreen.List.route) { inclusive = false }
						}
					} else {
						navController.navigateUp()
					}
				},
				onExtendedInstruction = {
					navController.navigate("recipe/editextended?id=${it?.node?.id}")
				},
				sharedTransitionScope = sharedTransitionScope,
				animatedContentScope = this@composable
			)
		}

		composable(
			RecipeScreen.EditExtended.route,
			arguments = listOf(navArgument("id") {
				type = NavType.StringType
				nullable = true
			})
		) { backStackEntry ->
			val id = backStackEntry.arguments?.getString("id")
			val editFlowEntry = remember(backStackEntry) {
				navController.getBackStackEntry(RecipeScreen.EditFlow.route)
			}
			val viewModel = koinViewModel<RecipeEditViewModel>(viewModelStoreOwner = editFlowEntry)
			RecipeEditAddExtendedInstructionEntryDialog(
				id,
				viewModel,
				onBackNavigation = navController::navigateUp
			)
		}
	}
}

sealed class RecipeScreen(val route: String) {
	data object List : RecipeScreen("recipe")
	data object Detail : RecipeScreen("recipe/view/{id}")
	data object Shopping : RecipeScreen("recipe/shopping/{id}/{recipeServings}/{servings}")
	data object Edit : RecipeScreen("recipe/edit?id={id}")
	data object EditExtended : RecipeScreen("recipe/editextended?id={id}")
	data object EditFlow : RecipeScreen("recipe/edit_flow")
}