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

	@Test
	fun `stable id is deterministic, valid, and fixed-length regardless of the source`() {
		val couchbaseDocId = "8B2E1F3A-9C4D-4E11-BF2A-6D1C0F5A7E90"

		val id = EntityPaths.stableId(couchbaseDocId)

		assertEquals(id, EntityPaths.stableId(couchbaseDocId))
		assertEquals(10, id.length)
		assertTrue(id.all { it in '0'..'9' || it in 'a'..'z' })
	}

	@Test
	fun `stable id folder still round-trips through idFromFolder`() {
		val id = EntityPaths.stableId("8B2E1F3A-9C4D-4E11-BF2A-6D1C0F5A7E90")
		val folder = EntityPaths.slugFolder("Grandma's Pasta", id)

		assertEquals(id, EntityPaths.idFromFolder(folder))
	}
}
