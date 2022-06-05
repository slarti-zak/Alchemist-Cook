package click.alchemist.cook.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class DatabaseSettings(
	/**
	 * Time in epoch seconds.
	 */
	var lastMaintenance: Long = 0,

	@JsonIgnore override var id: String = "",
	override var owner: String = ""
) : DatabaseObject
