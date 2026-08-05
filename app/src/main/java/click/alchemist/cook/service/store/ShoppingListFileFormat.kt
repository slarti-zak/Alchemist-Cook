package click.alchemist.cook.service.store

import click.alchemist.cook.model.ShoppingList

/** The fields serialized for a shopping list's `list.yaml`; no `id` — the list's folder name already carries it. */
private data class ShoppingListRecord(val name: String = "")

/**
 * Serializes/parses a shopping list's `list.yaml` file. Shopping lists live in their own top-level,
 * human-browsable `<slug>-<id>` folder (like recipes); the id is recovered from that folder name
 * (see [EntityPaths.shoppingListIdFromPath]) rather than duplicated in the file content.
 */
object ShoppingListFileFormat {
	fun serialize(list: ShoppingList): String =
		YamlMapper.instance.writeValueAsString(ShoppingListRecord(list.name))

	/** [id] is recovered from the list's folder name by the caller (see [EntityPaths.shoppingListIdFromPath]). */
	fun parse(text: String, id: String): ShoppingList {
		val record = YamlMapper.instance.readValue(text, ShoppingListRecord::class.java)
		return ShoppingList(name = record.name, id = id)
	}
}
