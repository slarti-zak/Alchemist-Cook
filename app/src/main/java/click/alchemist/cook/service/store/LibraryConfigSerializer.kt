package click.alchemist.cook.service.store

import click.alchemist.cook.logError
import click.alchemist.cook.service.store.LibraryConfigSerializer.EMPTY
import click.alchemist.cook.service.webdav.WebDavConfig
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Pure (de)serialization for the persisted list of [LibraryConfig]s, kept separate from
 * [LibraryManager] (and its [click.alchemist.cook.service.settings.AndroidSettings]/`Context`
 * dependency) purely so this can be unit-tested directly.
 */
internal object LibraryConfigSerializer {
	const val EMPTY = "[]"
	private const val TAG = "LibraryConfigSerializer"

	fun serialize(libraries: List<LibraryConfig>): String = YamlMapper.instance.writeValueAsString(libraries)

	fun deserialize(raw: String): List<LibraryConfig> {
		if (raw.isBlank() || raw == EMPTY) return emptyList()
		return try {
			YamlMapper.instance.readValue(raw)
		} catch (e: Exception) {
			deserializeLegacy(raw)
		}
	}

	/**
	 * [LibraryConfig] used to embed a [WebDavConfig] directly — there was only one connection kind.
	 * Existing installs have that flat shape persisted, so a straight parse against the current
	 * ([LibraryConnection]-based) shape fails for them. Falling back straight to [EMPTY] on any parse
	 * failure, as before this migration was added, would silently wipe every configured library
	 * (including the personal one) the first time such an install loads this after an update — so the
	 * old shape is tried explicitly before giving up.
	 */
	private data class LegacyLibraryConfig(val id: String, val label: String, val role: LibraryRole, val webDav: WebDavConfig)

	private fun deserializeLegacy(raw: String): List<LibraryConfig> {
		return try {
			YamlMapper.instance.readValue<List<LegacyLibraryConfig>>(raw).map {
				LibraryConfig(it.id, it.label, it.role, LibraryConnection.WebDav(it.webDav))
			}
		} catch (e: Exception) {
			logError(TAG, "Could not parse configured libraries, resetting to none", e)
			emptyList()
		}
	}
}
