package click.alchemist.cook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import click.alchemist.cook.coil.CoilBlobFetcher
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.service.background.BackgroundService
import click.alchemist.cook.service.couchbase.CouchbaseState
import click.alchemist.cook.ui.MainViewModel
import click.alchemist.cook.ui.cooking.list.CookingList
import click.alchemist.cook.ui.recipe.RecipeNavigation
import click.alchemist.cook.ui.recipe.RecipeScreen
import click.alchemist.cook.ui.recipe.edit.RecipeEditViewModel
import click.alchemist.cook.ui.shoppinglist.ShoppingListNavigation
import click.alchemist.cook.ui.shoppinglist.ShoppingScreen
import coil.ImageLoader
import coil.compose.LocalImageLoader
import com.couchbase.lite.AbstractReplicator
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.pager.ExperimentalPagerApi
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.AbstractCrashesListener
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.crashes.model.ErrorReport
import com.microsoft.appcenter.distribute.Distribute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


@ExperimentalCoroutinesApi
@FlowPreview
class MainComposeActivity : ComponentActivity() {
	companion object {
		// TODO ugly! Fix reusing view models in composables
		var editViewModel: RecipeEditViewModel? = null
	}

	private var initialized: Boolean = false
	private val viewModel: MainViewModel by viewModel()
	private val backgroundService: BackgroundService by inject()

	@ExperimentalComposeUiApi
	@ExperimentalPagerApi
	@ExperimentalAnimationApi
	@ExperimentalFoundationApi
	@ExperimentalMaterialApi
	override fun onCreate(savedInstanceState: Bundle?) {
		Crashes.setListener(object : AbstractCrashesListener() {
			override fun shouldAwaitUserConfirmation(): Boolean = false
			override fun shouldProcess(report: ErrorReport?): Boolean = true
		})

		AppCenter.start(
			application, BuildConfig.appCenterApiKey,
			Analytics::class.java, Crashes::class.java, Distribute::class.java
		)

		super.onCreate(savedInstanceState)

		WindowCompat.setDecorFitsSystemWindows(window, false)

		viewModel.databaseChanged
			.onEach { onDatabaseChanged() }
			.launchIn(lifecycleScope)

		val imageLoader = ImageLoader.Builder(this)
			.componentRegistry {
				add(CoilBlobFetcher())
			}
			.build()

		setContent {
			AppTheme {
				ProvideWindowInsets {
					CompositionLocalProvider(LocalImageLoader provides imageLoader) {
						val couchbaseState by viewModel.databaseState.collectAsState(CouchbaseState.guest())
						val cookingBadge by viewModel.cookingCount.collectAsState(0L)

						MainComposeActivityContent(couchbaseState, cookingBadge)
					}
				}
			}
		}
	}

	private fun onDatabaseChanged() {
		if (initialized) {
			finish()
		} else {
			initialized = true
			backgroundService.startSyncWorker()
		}
	}
}

@ExperimentalComposeUiApi
@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalPagerApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
private fun MainComposeActivityContent(couchbaseState: CouchbaseState, cookingBadge: Long) {

	val syncError = when (couchbaseState) {
		is CouchbaseState.AccountState -> couchbaseState.status.error != null
		else -> false
	}
	val syncActive = when (couchbaseState) {
		is CouchbaseState.AccountState -> couchbaseState.status.activityLevel != AbstractReplicator.ActivityLevel.IDLE
		else -> false
	}

	MainContent(syncError, syncActive, cookingBadge) { contentPadding, navHostController ->
		BoxWithConstraints(Modifier.padding(contentPadding)) {
			NavHost(navHostController, startDestination = Screen.Recipe.baseRoute) {
				this.RecipeNavigation(navHostController)

				composable(Screen.Cooking.baseRoute) {
					CookingList()
				}

				this.ShoppingListNavigation(navHostController, maxWidth)
			}
		}
	}
}

@ExperimentalMaterialApi
@Composable
private fun MainContent(
	syncError: Boolean,
	syncActive: Boolean,
	cookingBadge: Long,
	content: @Composable (PaddingValues, NavHostController) -> Unit
) {
	val navController = rememberNavController()

	val items = listOf(
		Screen.Recipe,
		Screen.Cooking,
		Screen.Shopping,
	)

	com.google.accompanist.insets.ui.Scaffold(
		bottomBar = {
			Box(contentAlignment = Alignment.CenterStart) {
				val bottomContentPadding = rememberInsetsPaddingValues(insets = LocalWindowInsets.current.navigationBars)
				com.google.accompanist.insets.ui.BottomNavigation(
					contentPadding = bottomContentPadding
				) {
					val navBackStackEntry by navController.currentBackStackEntryAsState()
					val currentRoute = navBackStackEntry?.destination?.route
					items.forEach { screen ->
						BottomNavigationItem(
							icon = {
								if (screen == Screen.Cooking && cookingBadge > 0) {
									BadgedBox(
										badge = {
											Badge {
												Text(
													cookingBadge.toString(),
													modifier = Modifier.semantics {
														this.contentDescription = "$cookingBadge notifications"
													}
												)
											}
										}
									) {
										Icon(painterResource(screen.iconId), "Navigation")
									}
								} else {
									Icon(painterResource(screen.iconId), "Navigation")
								}
							},
							label = { Text(stringResource(screen.resourceId)) },
							selected = currentRoute?.startsWith(screen.baseRoute) ?: false,
							onClick = {
								if (currentRoute == screen.baseRoute) {
									return@BottomNavigationItem
								}
								navController.navigate(screen.startingRoute) {
									// Pop up to the start destination of the graph to
									// avoid building up a large stack of destinations
									// on the back stack as users select items
									popUpTo(navController.graph.startDestinationId) {
										saveState = true
									}

									// Avoid multiple copies of the same destination when
									// reselecting the same item
									launchSingleTop = true

									// Restore state when reselecting a previously selected item
									restoreState = true
								}
							}
						)
					}
				}

				val bottomPadding = bottomContentPadding.calculateBottomPadding()
				if (syncError) {
					Icon(
						painter = painterResource(id = R.drawable.ic_alert_circle_back),
						contentDescription = "Sync Error",
						tint = Color.White,
						modifier = Modifier.padding(start = 8.dp, bottom = bottomPadding)
					)
					Icon(
						painter = painterResource(id = R.drawable.ic_alert_circle),
						contentDescription = "Sync Error",
						tint = MaterialTheme.colors.error,
						modifier = Modifier.padding(start = 8.dp, bottom = bottomPadding)
					)
				} else if (syncActive) {
					CircularProgressIndicator(
						Modifier
							.size(20.dp)
							.padding(start = 8.dp, bottom = bottomPadding),
						color = MaterialTheme.colors.secondaryVariant,
						strokeWidth = ProgressIndicatorDefaults.StrokeWidth / 2
					)
				}
			}
		},
		content = { contentPadding -> content(contentPadding, navController) }
	)
}

sealed class Screen(val baseRoute: String, val startingRoute: String, @StringRes val resourceId: Int, @DrawableRes val iconId: Int) {
	object Recipe : Screen(RecipeScreen.List.route, RecipeScreen.List.route, R.string.title_recipe, R.drawable.ic_format_list_text)
	object Cooking : Screen("cooking", "cooking", R.string.title_cooking, R.drawable.ic_chef_hat)
	object Shopping : Screen(ShoppingScreen.Overview.route, ShoppingScreen.Overview.route, R.string.title_shopping, R.drawable.ic_cart)
}

@ExperimentalPagerApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
@Preview("Syncing")
private fun PreviewSyncing() {
	AppTheme {
		MainContent(false, true, 1) { _, _ -> }
	}
}

@ExperimentalPagerApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
@Preview("Error")
private fun PreviewError() {
	AppTheme {
		MainContent(true, false, 1) { _, _ -> }
	}
}
