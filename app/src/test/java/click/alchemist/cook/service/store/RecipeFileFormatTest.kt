package click.alchemist.cook.service.store

import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.model.RecipeGraph
import click.alchemist.cook.model.RecipeGraphNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class RecipeFileFormatTest {

	@Test
	fun `round-trips a simple recipe`() {
		val recipe = Recipe(
			name = "Pasta",
			content = "1. Boil water\n2. Add pasta",
			ingredients = listOf(Ingredient("Pasta", BigDecimal(500), IngredientCategory.WEIGHT)),
			serves = 2,
			id = "abc123"
		)

		val text = RecipeFileFormat.serialize(recipe, imageFileName = "image.jpg", updatedAt = 1700000000000)
		val parsed = RecipeFileFormat.parse(text, recipe.id)

		assertEquals(recipe.name, parsed.recipe.name)
		assertEquals(recipe.content, parsed.recipe.content)
		assertEquals(recipe.serves, parsed.recipe.serves)
		assertEquals(recipe.id, parsed.recipe.id)
		assertEquals(1, parsed.recipe.ingredients.size)
		assertEquals("Pasta", parsed.recipe.ingredients[0].name)
		assertEquals(0, BigDecimal(500).compareTo(parsed.recipe.ingredients[0].amount))
		assertEquals("image.jpg", parsed.imageFileName)
		assertEquals(1700000000000, parsed.updatedAt)
	}

	@Test
	fun `round-trips extended content and a null image`() {
		val recipe = Recipe(
			name = "Bread",
			content = "Knead the dough",
			extendedContent = RecipeGraph(listOf(RecipeGraphNode(id = "n1", text = "Rest", dependencies = emptyList()))),
			id = "bread1"
		)

		val text = RecipeFileFormat.serialize(recipe, imageFileName = null, updatedAt = 0)
		val parsed = RecipeFileFormat.parse(text, recipe.id)

		assertNull(parsed.imageFileName)
		assertEquals(1, parsed.recipe.extendedContent?.nodes?.size)
		assertEquals("n1", parsed.recipe.extendedContent?.nodes?.get(0)?.id)
	}

	@Test
	fun `body preserves markdown content verbatim including delimiter-like lines`() {
		val recipe = Recipe(name = "Edge case", content = "Some content\n\n---\n\nMore content after a horizontal rule")
		val text = RecipeFileFormat.serialize(recipe, imageFileName = null, updatedAt = 0)
		val parsed = RecipeFileFormat.parse(text, recipe.id)

		assertEquals(recipe.content, parsed.recipe.content)
	}

	@Test
	fun `serialized file starts and ends front matter with delimiters`() {
		val recipe = Recipe(name = "Test")
		val text = RecipeFileFormat.serialize(recipe, imageFileName = null, updatedAt = 0)
		val lines = text.lines()

		assertEquals("---", lines[0].trim())
		assertTrue(lines.drop(1).any { it.trim() == "---" })
	}
}
