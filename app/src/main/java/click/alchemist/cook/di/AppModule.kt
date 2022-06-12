package click.alchemist.cook.di

import android.content.Context
import click.alchemist.cook.service.background.BackgroundService
import click.alchemist.cook.service.background.WorkManagerBackgroundService
import click.alchemist.cook.service.couchbase.CouchbaseAccountListener
import click.alchemist.cook.service.couchbase.CouchbaseService
import click.alchemist.cook.service.couchbase.repository.*
import click.alchemist.cook.service.firestore.RecipeFirestore
import click.alchemist.cook.service.firestore.UserFirestore
import click.alchemist.cook.service.markdown.MarkdownService
import click.alchemist.cook.service.markdown.MarkwonService
import click.alchemist.cook.service.recipe.AlarmManagerTimerService
import click.alchemist.cook.service.recipe.RecipeTimerParser
import click.alchemist.cook.service.recipe.RegexRecipeTimerParser
import click.alchemist.cook.service.recipe.TimerService
import click.alchemist.cook.service.settings.AndroidSettings
import click.alchemist.cook.service.time.FlowTimeService
import click.alchemist.cook.service.time.TimeService
import click.alchemist.cook.ui.MainViewModel
import click.alchemist.cook.ui.cooking.list.CookingListExtendedItemViewModel
import click.alchemist.cook.ui.cooking.list.CookingListViewModel
import click.alchemist.cook.ui.recipe.detail.RecipeDetailViewModel
import click.alchemist.cook.ui.recipe.edit.RecipeEditViewModel
import click.alchemist.cook.ui.recipe.list.RecipeListViewModel
import click.alchemist.cook.ui.recipe.shopping.RecipeShoppingViewModel
import click.alchemist.cook.ui.settings.SettingsViewModel
import click.alchemist.cook.ui.shoppinglist.add.ShoppingListAddIngredientViewModel
import click.alchemist.cook.ui.shoppinglist.detail.ShoppingListDetailViewModel
import click.alchemist.cook.ui.shoppinglist.overview.ShoppingListOverviewViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module


fun createModule(context: Context): Module {
	return module {
		// Database
		single { AndroidSettings(context) }

		single { CouchbaseAccountListener(context, get()) }
		single { CouchbaseService(get()) }
		single { RecipeRepository(get()) }
		single { ActiveRecipeRepository(get()) }
		single { ShoppingListRepository(get()) }
		single { IngredientRepository(get()) }
		single { TimerRepository(get()) }

		single { UserFirestore() }
		single { RecipeFirestore(get()) }

		// Services
		single<MarkdownService> { MarkwonService(context, get()) }
		single<RecipeTimerParser> { RegexRecipeTimerParser() }
		single<TimerService> { AlarmManagerTimerService(context, get(), get()) }
		single<BackgroundService> { WorkManagerBackgroundService(context) }
		single<TimeService> { FlowTimeService() }

		// ViewModels
		viewModel { MainViewModel(get(), get(), get(), get()) }
		viewModel { SettingsViewModel(get()) }
		viewModel { CookingListViewModel(get(), get(), get(), get(), get()) }
		viewModel { CookingListExtendedItemViewModel(get(), get(), get(), get()) }

		viewModel { RecipeListViewModel(get()) }
		viewModel { RecipeEditViewModel(get()) }
		viewModel { params -> RecipeDetailViewModel(get(), get(), params.get()) }
		viewModel { RecipeShoppingViewModel(get(), get()) }

		viewModel { ShoppingListOverviewViewModel(get()) }
		viewModel { params -> ShoppingListDetailViewModel(get(), params.get()) }
		viewModel { params -> ShoppingListAddIngredientViewModel(get(), get(), params.get()) }
	}
}