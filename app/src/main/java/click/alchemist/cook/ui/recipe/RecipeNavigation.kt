package click.alchemist.cook.ui.recipe

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import click.alchemist.cook.ui.recipe.detail.RecipeDetail
import click.alchemist.cook.ui.recipe.edit.RecipeEdit
import click.alchemist.cook.ui.recipe.edit.RecipeEditAddExtendedInstructionEntryDialog
import click.alchemist.cook.ui.recipe.list.RecipeList
import click.alchemist.cook.ui.recipe.shopping.RecipeShopping
import click.alchemist.cook.ui.settings.SettingsActivity
import click.alchemist.cook.viewmodel.Serving


fun NavGraphBuilder.RecipeNavigation(navController: NavController) {
	composable(RecipeScreen.List.route) {
		val context = LocalContext.current
		RecipeList(
			onSettingsClick = {
				context.startActivity(Intent(context, SettingsActivity::class.java))
			},
			onRecipeClick = { navController.navigate("recipe/view/${it.recipe.id}") },
			onAddRecipe = { navController.navigate("recipe/edit") },
		)
	}

	composable(RecipeScreen.Detail.route) { backStackEntry ->
		val id = backStackEntry.arguments?.getString("id") ?: return@composable
		RecipeDetail(
			id,
			onBackNavigation = navController::navigateUp,
			onEdit = { navController.navigate("recipe/edit?id=$id") },
			navigateShopping = { recipeId, serving ->
				backStackEntry.arguments?.putParcelable("serving", serving)
				navController.navigate("recipe/shopping?id=$recipeId")
			}
		)
	}

	composable(RecipeScreen.Shopping.route) { backStackEntry ->
		val id = backStackEntry.arguments?.getString("id") ?: return@composable
		val servings = navController.previousBackStackEntry?.arguments?.getParcelable<Serving>("serving") ?: return@composable
		RecipeShopping(
			id,
			servings,
			onBackNavigation = navController::navigateUp
		)
	}

	composable(
		RecipeScreen.Edit.route,
		arguments = listOf(navArgument("id") { nullable = true })
	) { backStackEntry ->
		val id = backStackEntry.arguments?.getString("id")
		RecipeEdit(
			id,
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
			}
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
		RecipeEditAddExtendedInstructionEntryDialog(
			id,
			onBackNavigation = navController::navigateUp
		)
	}
}

sealed class RecipeScreen(val route: String) {
	data object List : RecipeScreen("recipe")
	data object Detail : RecipeScreen("recipe/view/{id}")
	data object Shopping : RecipeScreen("recipe/shopping?id={id}")
	data object Edit : RecipeScreen("recipe/edit?id={id}")
	data object EditExtended : RecipeScreen("recipe/editextended?id={id}")
}