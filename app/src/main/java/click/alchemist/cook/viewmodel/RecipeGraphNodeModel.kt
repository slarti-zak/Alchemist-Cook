package click.alchemist.cook.viewmodel

import click.alchemist.cook.model.RecipeGraphNode
import click.alchemist.cook.model.RunningTimer
import kotlin.time.Duration

data class RecipeGraphNodeModel(
	val node: RecipeGraphNode,
	val recipeName: String,
	val isSingleRecipe: Boolean = true,
	/**
	 * When this node should be started since the beginning of the cooking process.
	 */
	var plannedStartTimePoint: Duration = Duration.ZERO,
	/**
	 * Time when this node was finished.
	 */
	val finishedAt: Duration = Duration.INFINITE,
	/**
	 * The time a user typically takes for this task.
	 */
	val userTime: Duration = Duration.ZERO,
	/**
	 * Graph is in preview mode and not currently active. That is, it is not interactive and no timers are running.
	 */
	val graphStartTime: Long? = null,
	val dependenciesSatisfied: Boolean = false,
	/**
	 * The timer (if any) that is running for this node.
	 */
	val timer: TimerModel? = null,

	var timeTaken: Duration? = null
) {
	val isPreview: Boolean
		get() = graphStartTime == null
	val isFinished: Boolean
		get() = finishedAt != Duration.INFINITE

	/**
	 * A Node is processable when all previous nodes are finished.
	 */
	val canBeProcessed: Boolean
		get() = !isPreview && !isFinished && dependenciesSatisfied


	val timeHasCome: Boolean
		get() {
			val time = timeTaken
			return time != null && time >= Duration.ZERO
		}
}