package click.alchemist.cook.service.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFolderDiffTest {

	@Test
	fun `nothing changes when every path's mtime matches what was last recorded`() {
		val changes = LocalFolderDiff.diff(
			currentMtimes = mapOf("recipes/pasta/recipe.md" to 100L),
			knownMtimes = mapOf("recipes/pasta/recipe.md" to 100L)
		)

		assertTrue(changes.isEmpty())
	}

	@Test
	fun `a path with no recorded state yet is reindexed`() {
		val changes = LocalFolderDiff.diff(
			currentMtimes = mapOf("recipes/pasta/recipe.md" to 100L),
			knownMtimes = emptyMap()
		)

		assertEquals(listOf(LocalFolderChange.Reindex("recipes/pasta/recipe.md", 100L)), changes)
	}

	@Test
	fun `a path whose mtime moved since the last rescan is reindexed`() {
		val changes = LocalFolderDiff.diff(
			currentMtimes = mapOf("recipes/pasta/recipe.md" to 200L),
			knownMtimes = mapOf("recipes/pasta/recipe.md" to 100L)
		)

		assertEquals(listOf(LocalFolderChange.Reindex("recipes/pasta/recipe.md", 200L)), changes)
	}

	@Test
	fun `a path that's known but no longer on disk is removed`() {
		val changes = LocalFolderDiff.diff(
			currentMtimes = emptyMap(),
			knownMtimes = mapOf("recipes/pasta/recipe.md" to 100L)
		)

		assertEquals(listOf(LocalFolderChange.Remove("recipes/pasta/recipe.md")), changes)
	}

	@Test
	fun `unrelated paths are each diffed independently`() {
		val changes = LocalFolderDiff.diff(
			currentMtimes = mapOf("a" to 1L, "b" to 2L, "c" to 3L),
			knownMtimes = mapOf("a" to 1L, "b" to 99L)
		)

		assertEquals(setOf(LocalFolderChange.Reindex("b", 2L), LocalFolderChange.Reindex("c", 3L)), changes.toSet())
	}
}
