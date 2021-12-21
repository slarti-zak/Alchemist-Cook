package click.alchemist.cook.viewmodel

import click.alchemist.cook.model.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

data class RecipeGraphModel(
	val nodes: List<RecipeGraphNodeModel> = emptyList(),
	val isPreview: Boolean,
	val endAt: Long = 0
) {
	fun startCooking(): ActiveRecipeGraph {
		val nodes = nodes.map { ActiveRecipeGraphNode(it.recipeName, DbDuration(it.plannedStartTimePoint), it.node) }
		return ActiveRecipeGraph(nodes)
	}

	companion object {
		fun fromRecipe(recipes: List<Recipe>, now: Long = 0): RecipeGraphModel {
			val isSingleRecipe =
				recipes.count() < 2 || recipes
					.filter { it.extendedContent?.nodes?.isNotEmpty() ?: false }
					.map { it.id }
					.toSet().count() < 2
			val nodes = recipes.flatMap { recipe ->
				recipe.extendedContent?.nodes?.map {
					RecipeGraphNodeModel(
						it,
						recipeName = recipe.name,
						isSingleRecipe = isSingleRecipe
					)
				} ?: emptyList()
			}
			return fromNodes(nodes, now)
		}

		fun fromNodes(recipeName: String, nodes: List<RecipeGraphNode>?, now: Long = 0): RecipeGraphModel {
			val graphNodes = nodes?.map { RecipeGraphNodeModel(it, recipeName) } ?: emptyList()
			return fromNodes(graphNodes, now)
		}

		private fun fromNodes(nodes: List<RecipeGraphNodeModel>, now: Long = 0): RecipeGraphModel {
			val sortedNodes = sort(nodes)
			val endTime = (sortedNodes.map {
				now + (it.plannedStartTimePoint + it.node.duration.dbDuration).inWholeMilliseconds
			}.maxOrNull() ?: now)
			return RecipeGraphModel(sortedNodes, true, endAt = endTime)
		}

		fun fromActiveGraph(
			activeGraph: ActiveRecipes,
			timers: Map<String, RunningTimer>,
			now: Long
		): RecipeGraphModel {
			val totalElapsedLong = now - activeGraph.startedAt
			val totalElapsed = totalElapsedLong.milliseconds
			val nodes =
				activeGraph.graph.nodes.map {
					val timer = timers[it.node.id]
					val runningTimer = if (timer == null) null else TimerModel.fromRunningTimer(timer, now)
					RecipeGraphNodeModel(
						it.node,
						it.recipeName,
						activeGraph.isSingleRecipe(),
						finishedAt = it.finishedAtPoint.dbDuration,
						userTime = Duration.ZERO,
						graphStartTime = activeGraph.startedAt,
						plannedStartTimePoint = it.plannedStartPoint.dbDuration,
//						timeTaken = activeGraph.getElapsed(it, now),
						dependenciesSatisfied = activeGraph.dependenciesSatisfied(it),
						timer = runningTimer
					)
				}

			val sortedNodes = updateTimes(nodes, totalElapsed)
			val endTime = (sortedNodes.map {
				activeGraph.startedAt +
						if (it.isFinished) it.finishedAt.inWholeMilliseconds
						else (it.plannedStartTimePoint + it.node.duration.dbDuration.coerceAtLeast(
							it.timeTaken ?: Duration.ZERO
						)).inWholeMilliseconds
			}.maxOrNull() ?: totalElapsedLong)

			return RecipeGraphModel(sortedNodes, false, endAt = endTime)
		}

		private fun sort(nodes: List<RecipeGraphNodeModel>): List<RecipeGraphNodeModel> {
			if (nodes.isEmpty()) return nodes

			val nodeMap = nodes.associateBy { it.node.id }
			val visitedEdges = mutableSetOf<Pair<String, String>>()
			nodes.forEach {
				visitedEdges.clear()
				applySortOrder(it, Duration.ZERO, nodeMap, visitedEdges)
			}

			val maxDuration = nodes.maxByOrNull { it.plannedStartTimePoint }!!.plannedStartTimePoint
			nodes.forEach { it.plannedStartTimePoint = maxDuration - it.plannedStartTimePoint }
			return nodes.sortedBy { it.plannedStartTimePoint }
		}

		private fun applySortOrder(
			node: RecipeGraphNodeModel,
			timeToNode: Duration,
			nodeMap: Map<String, RecipeGraphNodeModel>,
			visitedEdges: MutableSet<Pair<String, String>>
		) {
			val durationUntilNode = timeToNode + node.node.duration.dbDuration.coerceAtLeast(1.nanoseconds)
			if (durationUntilNode <= node.plannedStartTimePoint) return

			node.plannedStartTimePoint = durationUntilNode
			node.node.dependencies.forEach {
				val edge = Pair(node.node.id, it)
				if (visitedEdges.add(edge)) {
					val dependencyNode = nodeMap.getValue(it)
					applySortOrder(dependencyNode, durationUntilNode, nodeMap, visitedEdges)
				}
			}
		}

		private fun updateTimes(
			nodes: List<RecipeGraphNodeModel>,
			totalElapsed: Duration
		): List<RecipeGraphNodeModel> {
			val nodeMap = nodes.associateBy { it.node.id }
			nodes.forEach {
				updateTime(it, nodeMap, totalElapsed)
				if (it.canBeProcessed) {
					it.timeTaken = (totalElapsed - it.plannedStartTimePoint)
				}
			}

			return nodes
		}

		private fun updateTime(
			node: RecipeGraphNodeModel,
			nodeMap: Map<String, RecipeGraphNodeModel>,
			totalElapsed: Duration
		) {
			val dependencies = node.node.dependencies.map { nodeMap.getValue(it) }
			val earliestStart = (dependencies.map { estimatedEnd(it, totalElapsed) }.maxOrNull()
				?: node.plannedStartTimePoint)

			node.plannedStartTimePoint = earliestStart
		}

		private fun estimatedEnd(node: RecipeGraphNodeModel, totalElapsed: Duration): Duration {
			if (node.isFinished) return node.finishedAt

			val durationToAccountFor =
				node.node.duration.dbDuration.coerceAtLeast(totalElapsed - node.plannedStartTimePoint)

			return node.plannedStartTimePoint + durationToAccountFor
		}
	}
}