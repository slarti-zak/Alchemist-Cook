package click.alchemist.cook.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class ShoppingListItem(
	/**
	 * Not persisted: an item's file already lives at `shopping-lists/<listFolder>/items/<id>.yaml`, so
	 * its parent list is defined by folder structure alone (see [EntityPaths.shoppingListIdFromItemPath][
	 * click.alchemist.cook.service.store.EntityPaths.shoppingListIdFromItemPath]). This field only
	 * exists so callers know which list to save into; [FileIndexer][click.alchemist.cook.service.store.FileIndexer]
	 * fills it back in from the path on load.
	 */
	@JsonIgnore var shoppingListId: String = "",
	val ingredient: Ingredient = Ingredient(),
	val finished: Boolean = false,
	@JsonIgnore var id: String = ""
)