package click.alchemist.cook.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class RunningTimer(
	val recipeId: String = "",
	val graphNodeId: String = "",
	val title: String = "",
	val content: String = "",
	val duration: DbDuration = DbDuration.ZERO,
	/**
	 * Time in epoch seconds.
	 */
	val startedAt: Long = 0,
	@JsonIgnore var id: String = ""
)
