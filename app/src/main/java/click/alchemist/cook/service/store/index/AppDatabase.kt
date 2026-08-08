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
		SyncFileStateEntity::class,
		PendingFolderDeletionEntity::class
	],
	version = 3,
	exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
	abstract fun recipeDao(): RecipeDao
	abstract fun shoppingListDao(): ShoppingListDao
	abstract fun plannedRecipeDao(): PlannedRecipeDao
	abstract fun activeRecipeDao(): ActiveRecipeDao
	abstract fun runningTimerDao(): RunningTimerDao
	abstract fun syncFileStateDao(): SyncFileStateDao
	abstract fun pendingFolderDeletionDao(): PendingFolderDeletionDao

	companion object {
		/**
		 * This is a rebuildable local index, not a source of truth (recipes/shopping lists resync from
		 * WebDAV; the little that doesn't — running timers, active-recipe progress — is fine to lose on
		 * a schema change), so destructive fallback beats hand-writing migrations for a Room DB nothing
		 * outside the device ever needs to read back.
		 */
		fun create(context: Context): AppDatabase =
			Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "webdav_store.db")
				.fallbackToDestructiveMigration(false)
				.build()
	}
}
