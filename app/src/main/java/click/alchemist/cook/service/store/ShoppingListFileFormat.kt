package click.alchemist.cook.service.store

import click.alchemist.cook.model.ShoppingList

/** The fields serialized for a shopping list's `list.yaml`; unlike [ShoppingList.id], this carries the id explicitly. */
private data class ShoppingListRecord(val id: String = "", val name: String = "")

/**
 * Serializes/parses a shopping list's `list.yaml` file. Shopping lists live in their own top-level,
 * human-browsable `<slug>-<id>` folder (like recipes), so — same reasoning as [RecipeFileFormat] —
 * the id can't just be the filename and is carried in the YAML body instead.
 */
object ShoppingListFileFormat {
	fun serialize(list: ShoppingList): String =
		YamlMapper.instance.writeValueAsString(ShoppingListRecord(list.id, list.name))

	fun parse(text: String): ShoppingList {
		val record = YamlMapper.instance.readValue(text, ShoppingListRecord::class.java)
		return ShoppingList(name = record.name, id = record.id)
	}
}
