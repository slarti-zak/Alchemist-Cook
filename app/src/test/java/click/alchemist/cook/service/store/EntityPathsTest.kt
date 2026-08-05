package click.alchemist.cook.service.store

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
	fun `active recipes and timers are not synced`() {
		assertFalse(EntityPaths.isSynced(EntityPaths.activeRecipesPath("abc123")))
		assertFalse(EntityPaths.isSynced(EntityPaths.timerPath("abc123")))
	}
}
