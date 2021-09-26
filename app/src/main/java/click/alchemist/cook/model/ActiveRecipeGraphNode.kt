package click.alchemist.cook.model

import kotlin.time.Duration

data class ActiveRecipeGraphNode(
	val recipeName: String = "",
	val plannedStartPoint: DbDuration = DbDuration.INFINITE,
	val node: RecipeGraphNode = RecipeGraphNode(),
	val finishedAtPoint: DbDuration = DbDuration.INFINITE
) {
	val isFinished: Boolean
		get() = finishedAtPoint.dbDuration != Duration.INFINITE
}
