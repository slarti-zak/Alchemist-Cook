package click.alchemist.cook.service.store.index

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local index of everything mirrored from configured WebDAV libraries — replaces Couchbase Lite's
 * local SQLite query engine, since the flat markdown/YAML file tree has no native query capability.
 * Rebuilt/kept in sync by [click.alchemist.cook.service.store.SyncEngine] and
 * [click.alchemist.cook.service.store.WebDavService] as files are pulled/pushed/written locally.
 */
@Database(
	entities = [
		RecipeEntity::class,
		RecipeIngredientNameEntity::class,
		ShoppingListEntity::class,
		ShoppingListItemEntity::class,
		PlannedRecipeEntity::class,
		ActiveRecipesEntity::class,
		RunningTimerEntity::class,
		SyncFileStateEntity::class
	],
	version = 1,
	exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
	abstract fun recipeDao(): RecipeDao
	abstract fun shoppingListDao(): ShoppingListDao
	abstract fun plannedRecipeDao(): PlannedRecipeDao
	abstract fun activeRecipeDao(): ActiveRecipeDao
	abstract fun runningTimerDao(): RunningTimerDao
	abstract fun syncFileStateDao(): SyncFileStateDao

	companion object {
		fun create(context: Context): AppDatabase =
			Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "webdav_store.db").build()
	}
}
