package click.alchemist.cook.service.store

import click.alchemist.cook.model.ShoppingList
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingListFileFormatTest {

	@Test
	fun `round-trips id and name`() {
		val list = ShoppingList(name = "Weekly groceries", id = "abc123")

		val text = ShoppingListFileFormat.serialize(list)
		val parsed = ShoppingListFileFormat.parse(text)

		assertEquals(list.id, parsed.id)
		assertEquals(list.name, parsed.name)
	}
}
