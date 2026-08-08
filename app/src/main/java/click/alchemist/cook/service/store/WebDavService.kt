package click.alchemist.cook.service.store

import click.alchemist.cook.model.ActiveRecipes
import click.alchemist.cook.model.PlannedRecipe
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.model.RunningTimer
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.model.ShoppingListItem
import click.alchemist.cook.service.store.index.AppDatabase
import click.alchemist.cook.service.store.index.PendingFolderDeletionEntity
import click.alchemist.cook.service.store.index.RecipeEntity
import click.alchemist.cook.service.store.index.RunningTimerEntity
import click.alchemist.cook.service.store.index.ShoppingListEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

/**
 * Facade repositories depend on, replacing `CouchbaseService`. Writes go straight to the local
 * mirror + Room index (so reads are instant and offline-first), then kick off an async sync of
 * the affected library so the write reaches the WebDAV server without the caller waiting on it.
 */
class WebDavService(
	private val libraryManager: LibraryManager,
	private val privateMirror: LocalMirror,
	private val safMirror: LocalMirror,
	private val database: AppDatabase,
	private val indexer: FileIndexer,
	private val syncEngine: SyncEngine
) {
	private val scope = CoroutineScope(Dispatchers.IO)

	val syncStatus: StateFlow<SyncStatus> = syncEngine.status

	@Volatile
	private var lastSyncAllRequestedAt = 0L

	/** Syncs every configured library right now, regardless of when it last ran (e.g. a manual "Sync now"). */
	fun syncNow() {
		lastSyncAllRequestedAt = System.currentTimeMillis()
		scope.launch { syncEngine.syncAll(libraryManager.current()) }
	}

	/** Like [syncNow], but skipped if a full sync already ran within [minInterval] — for frequent, low-signal triggers like app resume. */
	fun syncIfStale(minInterval: Duration = 1.minutes) {
		if (System.currentTimeMillis() - lastSyncAllRequestedAt < minInterval.inWholeMilliseconds) return
		syncNow()
	}

	private fun libraryIds(): Flow<List<String>> = libraryManager.libraries.map { it.map(LibraryConfig::id) }

	private fun defaultLibraryId(): String = libraryManager.personalLibrary()?.id ?: PERSONAL_LIBRARY_ID

	private fun connectionOf(libraryId: String): LibraryConnection? =
		libraryManager.current().firstOrNull { it.id == libraryId }?.connection

	/** [safMirror] *is* the canonical storage for a [LibraryConnection.LocalFolder] library; every other kind stages through [privateMirror]. */
	private fun mirrorFor(libraryId: String): LocalMirror =
		if (connectionOf(libraryId) is LibraryConnection.LocalFolder) safMirror else privateMirror

	/** A local-folder library has nothing to reconcile against — writes already land in its canonical storage, see [mirrorFor]. */
	private fun requestSync(libraryId: String) {
		if (connectionOf(libraryId) is LibraryConnection.LocalFolder) return
		scope.launch {
			libraryManager.current().firstOrNull { it.id == libraryId }?.let { syncEngine.sync(it) }
		}
	}

	private suspend fun write(libraryId: String, path: String, bytes: ByteArray) {
		mirrorFor(libraryId).write(libraryId, path, bytes)
		indexer.onFileChanged(libraryId, path, bytes)
	}

	private suspend fun remove(libraryId: String, path: String) {
		mirrorFor(libraryId).delete(libraryId, path)
		indexer.onFileRemoved(libraryId, path)
	}

	/**
	 * Deletes an entire local folder (e.g. a recipe's or shopping list's) in one go. Room cleanup for
	 * files nested inside it is the caller's job — [FileIndexer.onFileRemoved] only matches single
	 * files, not folders. For a WebDAV/Nextcloud library the folder is also queued for a remote
	 * `DELETE`: [SyncEngine] only diffs individual files, so without this the now-empty collection
	 * would be left behind on the server forever instead of being cleaned up on the next sync. A
	 * local-folder library has nothing to queue against — the delete above already is the only copy.
	 */
	private suspend fun removeFolder(libraryId: String, folderPath: String) {
		mirrorFor(libraryId).delete(libraryId, folderPath)
		if (connectionOf(libraryId) !is LibraryConnection.LocalFolder) {
			database.pendingFolderDeletionDao().upsert(PendingFolderDeletionEntity(libraryId, folderPath))
		}
	}

	// ---------------------------------------------------------------- Recipes

	/**
	 * The folder is always recomputed from the current name, so renaming a recipe re-slugs its folder
	 * too — [renamed], below, then carries the image over and sweeps up the stale old folder so the
	 * recipe doesn't end up living at two paths at once.
	 */
	suspend fun saveRecipe(
		recipe: Recipe,
		libraryId: String = defaultLibraryId(),
		image: ByteArray? = null,
		sync: Boolean = true
	): Recipe {
		val id = recipe.id.ifBlank { EntityPaths.newId() }
		val existing = database.recipeDao().load(id)
		val oldFolder = existing?.recipeFolder()
		val folder = EntityPaths.slugFolder(recipe.name, id)
		val renamed = oldFolder != null && oldFolder != folder
		val imageFileName = if (image != null) "image.jpg" else existing?.imageFileName

		val saved = recipe.copy(id = id)
		write(
			libraryId,
			EntityPaths.recipeMarkdownPath(folder),
			RecipeFileFormat.serialize(saved, imageFileName, System.currentTimeMillis()).toByteArray(Charsets.UTF_8)
		)

		// No new image bytes, but a rename means the old image, if any, needs to move over too.
		val imageBytes = image ?: imageFileName?.takeIf { renamed }?.let { mirrorFor(libraryId).read(libraryId, EntityPaths.recipeFilePath(oldFolder!!, it)) }
		if (imageBytes != null && imageFileName != null) {
			write(libraryId, EntityPaths.recipeFilePath(folder, imageFileName), imageBytes)
		}

		if (renamed) {
			removeFolder(libraryId, "${EntityPaths.RECIPES_DIR}/$oldFolder")
		}

		if (sync) requestSync(libraryId)
		return saved
	}

	fun liveRecipes(): Flow<List<Recipe>> =
		libraryIds().flatMapLatest { database.recipeDao().live(it) }.map { rows -> rows.map { it.toDomain() } }

	fun liveRecipe(id: String): Flow<Recipe?> = database.recipeDao().live(id).map { it?.toDomain() }

	fun liveRecipes(ids: List<String>): Flow<List<Recipe>> =
		database.recipeDao().liveByIds(ids).map { rows -> rows.map { it.toDomain() } }

	suspend fun loadRecipe(id: String): Recipe? = database.recipeDao().load(id)?.toDomain()

	suspend fun loadRecipeImage(id: String): File? {
		val entity = database.recipeDao().load(id) ?: return null
		val imageFileName = entity.imageFileName ?: return null
		val file = mirrorFor(entity.libraryId).file(entity.libraryId, EntityPaths.recipeFilePath(entity.recipeFolder(), imageFileName))
		return file?.takeIf { it.isFile }
	}

	fun liveIngredientNames(): Flow<Set<String>> =
		database.recipeDao().liveIngredientNames().map { it.toSortedSet(String.CASE_INSENSITIVE_ORDER) }

	suspend fun deleteRecipe(id: String) {
		val entity = database.recipeDao().load(id) ?: return
		removeFolder(entity.libraryId, "${EntityPaths.RECIPES_DIR}/${entity.recipeFolder()}")
		database.recipeDao().delete(id)
		database.recipeDao().deleteIngredientNames(id)
		requestSync(entity.libraryId)
	}

	/** Moves a recipe (and its image, if any) into a different library — e.g. sharing a personal recipe. */
	suspend fun moveRecipeToLibrary(id: String, targetLibraryId: String) {
		val entity = database.recipeDao().load(id) ?: return
		if (entity.libraryId == targetLibraryId) return

		val sourceMirror = mirrorFor(entity.libraryId)
		val markdownBytes = sourceMirror.read(entity.libraryId, entity.path) ?: return
		val imagePath = entity.imageFileName?.let { EntityPaths.recipeFilePath(entity.recipeFolder(), it) }
		val imageBytes = imagePath?.let { sourceMirror.read(entity.libraryId, it) }

		write(targetLibraryId, entity.path, markdownBytes)
		if (imagePath != null && imageBytes != null) {
			write(targetLibraryId, imagePath, imageBytes)
		}

		// Plain file deletes here, not `remove()`: the writes above already re-indexed this recipe
		// under `targetLibraryId` (same id, same path), so routing this through the indexer would
		// look the row up by that now-shared path and delete it out from under the target library.
		val sourceLibraryId = entity.libraryId
		sourceMirror.delete(sourceLibraryId, entity.path)
		imagePath?.let { sourceMirror.delete(sourceLibraryId, it) }

		requestSync(sourceLibraryId)
		requestSync(targetLibraryId)
	}

	private fun RecipeEntity.recipeFolder() =
		path.removePrefix("${EntityPaths.RECIPES_DIR}/").removeSuffix("/recipe.md")

	// ---------------------------------------------------------------- Planned recipes

	fun livePlannedRecipes(): Flow<List<PlannedRecipe>> =
		libraryIds().flatMapLatest { database.plannedRecipeDao().live(it) }.map { rows -> rows.map { it.toDomain() } }

	fun livePlannedRecipes(recipeId: String): Flow<List<PlannedRecipe>> =
		libraryIds().flatMapLatest { database.plannedRecipeDao().live(it, recipeId) }
			.map { rows -> rows.map { it.toDomain() } }

	suspend fun savePlannedRecipe(planned: PlannedRecipe, libraryId: String = defaultLibraryId()): PlannedRecipe {
		val id = planned.id.ifBlank { EntityPaths.newId() }
		val saved = planned.copy(id = id)
		write(libraryId, EntityPaths.plannedRecipePath(id), StateFileFormat.serialize(saved).toByteArray(Charsets.UTF_8))
		requestSync(libraryId)
		return saved
	}

	suspend fun deletePlannedRecipesForRecipe(recipeId: String) {
		database.plannedRecipeDao().loadForRecipe(recipeId).forEach {
			remove(it.libraryId, it.path)
			requestSync(it.libraryId)
		}
	}

	suspend fun deletePlannedRecipe(id: String) {
		val entity = database.plannedRecipeDao().load(id) ?: return
		remove(entity.libraryId, entity.path)
		requestSync(entity.libraryId)
	}

	// ---------------------------------------------------------------- Shopping lists

	fun liveShoppingLists(): Flow<List<ShoppingList>> =
		libraryIds().flatMapLatest { database.shoppingListDao().liveLists(it) }.map { rows -> rows.map { it.toDomain() } }

	fun liveShoppingList(id: String): Flow<ShoppingList?> = database.shoppingListDao().liveList(id).map { it?.toDomain() }

	fun liveShoppingListItems(): Flow<List<ShoppingListItem>> =
		libraryIds().flatMapLatest { database.shoppingListDao().liveItems(it) }.map { rows -> rows.map { it.toDomain() } }

	fun liveShoppingListItems(shoppingListId: String): Flow<List<ShoppingListItem>> =
		database.shoppingListDao().liveItems(shoppingListId).map { rows -> rows.map { it.toDomain() } }

	/** The folder is always recomputed from the current name, so renaming a list re-slugs its folder too. */
	suspend fun saveShoppingList(list: ShoppingList, libraryId: String = defaultLibraryId(), sync: Boolean = true): ShoppingList {
		val id = list.id.ifBlank { EntityPaths.newId() }
		val existing = database.shoppingListDao().loadList(id)
		val oldFolder = existing?.folder()
		val folder = EntityPaths.slugFolder(list.name, id)

		val saved = list.copy(id = id)
		write(libraryId, EntityPaths.shoppingListPath(folder), ShoppingListFileFormat.serialize(saved).toByteArray(Charsets.UTF_8))

		if (oldFolder != null && oldFolder != folder) {
			// Items live nested inside their list's folder, so a rename has to carry them over too,
			// not just list.yaml — otherwise they'd vanish along with the stale folder below.
			moveShoppingListItems(libraryId, oldFolder, folder)
			removeFolder(libraryId, "${EntityPaths.SHOPPING_LISTS_DIR}/$oldFolder")
		}

		if (sync) requestSync(libraryId)
		return saved
	}

	private suspend fun moveShoppingListItems(libraryId: String, oldFolder: String, newFolder: String) {
		val oldItemsPrefix = "${EntityPaths.SHOPPING_LISTS_DIR}/$oldFolder/items/"
		val mirror = mirrorFor(libraryId)
		mirror.listFiles(libraryId)
			.filter { it.startsWith(oldItemsPrefix) }
			.forEach { oldPath ->
				val bytes = mirror.read(libraryId, oldPath) ?: return@forEach
				write(libraryId, "${EntityPaths.SHOPPING_LISTS_DIR}/$newFolder/items/${oldPath.removePrefix(oldItemsPrefix)}", bytes)
			}
	}

	/** The library and folder are always the parent list's — an item can't live in a different library than its list. */
	suspend fun saveShoppingListItem(item: ShoppingListItem, sync: Boolean = true): ShoppingListItem {
		val list = database.shoppingListDao().loadList(item.shoppingListId)
			?: error("Cannot save shopping list item: shopping list ${item.shoppingListId} not found")

		val id = item.id.ifBlank { EntityPaths.newId() }
		val saved = item.copy(id = id)
		write(
			list.libraryId,
			EntityPaths.shoppingListItemPath(list.folder(), id),
			StateFileFormat.serialize(saved).toByteArray(Charsets.UTF_8)
		)
		if (sync) requestSync(list.libraryId)
		return saved
	}

	suspend fun deleteShoppingList(id: String) {
		val entity = database.shoppingListDao().loadList(id) ?: return
		removeFolder(entity.libraryId, "${EntityPaths.SHOPPING_LISTS_DIR}/${entity.folder()}")
		database.shoppingListDao().deleteListWithItems(id)
		requestSync(entity.libraryId)
	}

	suspend fun deleteShoppingListItem(id: String) {
		val entity = database.shoppingListDao().loadItem(id) ?: return
		remove(entity.libraryId, entity.path)
		requestSync(entity.libraryId)
	}

	private fun ShoppingListEntity.folder() =
		path.removePrefix("${EntityPaths.SHOPPING_LISTS_DIR}/").removeSuffix("/list.yaml")

	// ---------------------------------------------------------------- Active recipes
	//
	// In-progress cooking-graph state is per-device, in-the-moment state (like timers below) —
	// SyncEngine never syncs it (see EntityPaths.isSynced), so writes/deletes here don't request a sync.

	fun liveActiveRecipes(): Flow<ActiveRecipes?> =
		libraryIds().flatMapLatest { database.activeRecipeDao().live(it) }.map { it?.toDomain() }

	suspend fun saveActiveRecipes(active: ActiveRecipes, libraryId: String = defaultLibraryId()): ActiveRecipes {
		val id = active.id.ifBlank { EntityPaths.newId() }
		val saved = active.copy(id = id)
		write(libraryId, EntityPaths.activeRecipesPath(id), StateFileFormat.serialize(saved).toByteArray(Charsets.UTF_8))
		return saved
	}

	suspend fun deleteActiveRecipes(id: String) {
		val entity = database.activeRecipeDao().load(id) ?: return
		remove(entity.libraryId, entity.path)
	}

	// ---------------------------------------------------------------- Running timers
	//
	// Timers never touch the file tree at all — they're the most transient state the app has, so
	// they're written straight to the Room index and never synced, mirrored, or pulled from WebDAV.

	fun liveTimers(): Flow<List<RunningTimer>> =
		libraryIds().flatMapLatest { database.runningTimerDao().live(it) }.map { rows -> rows.map { it.toDomain() } }

	suspend fun loadTimer(id: String): RunningTimer? = database.runningTimerDao().load(id)?.toDomain()

	suspend fun loadTimer(recipeId: String, title: String): List<RunningTimer> =
		database.runningTimerDao().load(recipeId, title).map { it.toDomain() }

	suspend fun loadTimersFromNodes(graphNodeIds: List<String>): List<RunningTimer> =
		database.runningTimerDao().loadFromNodes(graphNodeIds).map { it.toDomain() }

	suspend fun saveTimer(timer: RunningTimer, libraryId: String = defaultLibraryId()): RunningTimer {
		val id = timer.id.ifBlank { EntityPaths.newId() }
		val saved = timer.copy(id = id)
		database.runningTimerDao().upsert(
			RunningTimerEntity(
				id = id,
				libraryId = libraryId,
				recipeId = saved.recipeId,
				graphNodeId = saved.graphNodeId,
				title = saved.title,
				content = saved.content,
				durationMillis = saved.duration.dbDuration.toDouble(DurationUnit.MILLISECONDS),
				startedAt = saved.startedAt
			)
		)
		return saved
	}

	suspend fun deleteTimer(id: String) {
		database.runningTimerDao().delete(id)
	}
}
