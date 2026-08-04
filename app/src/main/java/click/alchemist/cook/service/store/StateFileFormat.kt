package click.alchemist.cook.service.store

/**
 * Plain YAML (de)serialization for the small, structured, non-prose entities that live under
 * a library's `.state/` subtree (shopping lists/items, planned/active recipes, timers) — unlike
 * recipes, these have no markdown body, so no front-matter delimiters are needed.
 */
object StateFileFormat {
	fun <T> serialize(value: T): String = YamlMapper.instance.writeValueAsString(value)

	fun <T> parse(text: String, clazz: Class<T>): T = YamlMapper.instance.readValue(text, clazz)
}
