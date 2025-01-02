package click.alchemist.cook

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.couchbase.lite.ReplicatorActivityLevel
import com.microsoft.appcenter.crashes.AbstractCrashesListener
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.crashes.model.ErrorReport
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainComposeActivity : ComponentActivity() {
	companion object {
		// TODO ugly! Fix reusing view models in composables
		var editViewModel: RecipeEditViewModel? = null
	}

	private var initialized: Boolean = false
	private val viewModel: MainViewModel by viewModel()
	private val backgroundService: BackgroundService by inject()

	override fun attachBaseContext(newBase: Context?) {
		super.attachBaseContext(if (newBase == null) null else LocaleHelper.onAttach(newBase))
	}


	override fun onCreate(savedInstanceState: Bundle?) {
		Crashes.setListener(object : AbstractCrashesListener() {
			override fun shouldAwaitUserConfirmation(): Boolean = false
			override fun shouldProcess(report: ErrorReport?): Boolean = true
		})

		//AppCenter.start(
		//	application, BuildConfig.appCenterApiKey,
		//	Analytics::class.java, Crashes::class.java, Distribute::class.java
		//)

		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.light(
				android.graphics.Color.TRANSPARENT,
				android.graphics.Color.TRANSPARENT
			)
		)

		super.onCreate(savedInstanceState)

		//WindowCompat.setDecorFitsSystemWindows(window, false)

		viewModel.databaseChanged
			.onEach { onDatabaseChanged() }
			.launchIn(lifecycleScope)

		setContent {
			AppTheme {
				Box(Modifier.safeDrawingPadding()) {
					val couchbaseState by viewModel.databaseState.collectAsState(CouchbaseState.guest())
					val cookingBadge by viewModel.cookingCount.collectAsState(0L)

					MainComposeActivityContent(couchbaseState, cookingBadge)
				}
			}
		}
	}

	private fun onDatabaseChanged() {
		if (!initialized) {
			initialized = true
			backgroundService.startSyncWorker()
		}
	}
}


@Composable
private fun MainComposeActivityContent(couchbaseState: CouchbaseState, cookingBadge: Long) {

	val syncError = when (couchbaseState) {
		is CouchbaseState.AccountState -> couchbaseState.status.error != null
		else -> false
	}
	val syncActive = when (couchbaseState) {
		is CouchbaseState.AccountState -> couchbaseState.status.activityLevel != ReplicatorActivityLevel.IDLE
		else -> false
	}

	MainContent(syncError, syncActive, cookingBadge) { contentPadding, navHostController ->
		BoxWithConstraints(Modifier.padding(contentPadding)) {
			SharedTransitionLayout {
				NavHost(navHostController, startDestination = Screen.Recipe.baseRoute) {
					this.RecipeNavigation(navHostController, this@SharedTransitionLayout)

					composable(Screen.Cooking.baseRoute) {
						CookingList()
					}

					this.ShoppingListNavigation(navHostController, maxWidth, this@SharedTransitionLayout)
				}
			}
		}
	}
}


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

	Scaffold(
		bottomBar = {
			val bottomContentPadding = WindowInsets.navigationBars.asPaddingValues()
			NavigationBar(
				Modifier.navigationBarsPadding()
			) {
				val navBackStackEntry by navController.currentBackStackEntryAsState()
				val currentRoute = navBackStackEntry?.destination?.route
				items.forEach { screen ->
					NavigationBarItem(
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
								return@NavigationBarItem
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
					tint = MaterialTheme.colorScheme.error,
					modifier = Modifier.padding(start = 8.dp, bottom = bottomPadding)
				)
			} else if (syncActive) {
				CircularProgressIndicator(
					Modifier
						.size(20.dp)
						.padding(start = 8.dp, bottom = bottomPadding),
					color = MaterialTheme.colorScheme.onTertiary,
					strokeWidth = androidx.compose.material3.ProgressIndicatorDefaults.CircularStrokeWidth * 0.5f
				)
			}
		},
		content = { contentPadding -> content(contentPadding, navController) }
	)
}

sealed class Screen(val baseRoute: String, val startingRoute: String, @StringRes val resourceId: Int, @DrawableRes val iconId: Int) {
	data object Recipe : Screen(RecipeScreen.List.route, RecipeScreen.List.route, R.string.title_recipe, R.drawable.ic_format_list_text)
	data object Cooking : Screen("cooking", "cooking", R.string.title_cooking, R.drawable.ic_chef_hat)
	data object Shopping : Screen(ShoppingScreen.Overview.route, ShoppingScreen.Overview.route, R.string.title_shopping, R.drawable.ic_cart)
}


@Composable
@Preview("Syncing")
private fun PreviewSyncing() {
	AppTheme {
		MainContent(false, true, 1) { _, _ -> }
	}
}


@Composable
@Preview("Error")
private fun PreviewError() {
	AppTheme {
		MainContent(true, false, 1) { _, _ -> }
	}
}
