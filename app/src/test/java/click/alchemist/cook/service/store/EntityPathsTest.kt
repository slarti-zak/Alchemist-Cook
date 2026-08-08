package click.alchemist.cook.service.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityPathsTest {

	@Test
	fun `recipes, shopping lists and planned recipes are synced`() {
		assertTrue(EntityPaths.isSynced(EntityPaths.recipeMarkdownPath("pasta-abc123")))
		assertTrue(EntityPaths.isSynced(EntityPaths.shoppingListPath("weekly-abc123")))
		assertTrue(EntityPaths.isSynced(EntityPaths.plannedRecipePath("abc123")))
	}

	@Test
	fun `active recipes are not synced`() {
		assertFalse(EntityPaths.isSynced(EntityPaths.activeRecipesPath("abc123")))
	}

	@Test
	fun `shopping list id is recovered from an item's folder, slugged or not`() {
		assertEquals("weeklyabc1", EntityPaths.shoppingListIdFromItemPath(EntityPaths.shoppingListItemPath("weekly-groceries-weeklyabc1", "item1")))
		assertEquals("weeklyabc1", EntityPaths.shoppingListIdFromItemPath(EntityPaths.shoppingListItemPath("weeklyabc1", "item1")))
	}

	@Test
	fun `recipe and shopping list ids are recovered from their own folder, slugged or not`() {
		assertEquals("pastaabc12", EntityPaths.recipeIdFromPath(EntityPaths.recipeMarkdownPath("yummy-pasta-pastaabc12")))
		assertEquals("pastaabc12", EntityPaths.recipeIdFromPath(EntityPaths.recipeMarkdownPath("pastaabc12")))

		assertEquals("weeklyabc1", EntityPaths.shoppingListIdFromPath(EntityPaths.shoppingListPath("weekly-groceries-weeklyabc1")))
		assertEquals("weeklyabc1", EntityPaths.shoppingListIdFromPath(EntityPaths.shoppingListPath("weeklyabc1")))
	}
}
