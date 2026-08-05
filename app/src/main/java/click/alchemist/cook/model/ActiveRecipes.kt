package click.alchemist.cook.model

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * The graph of all currently active (that is cooking has started and timer is counting) recipes.
 */
data class ActiveRecipes(
	val graph: ActiveRecipeGraph = ActiveRecipeGraph(),
	val startedAt: Long = 0L,
	@JsonIgnore var id: String = ""
) {
	fun dependenciesSatisfied(node: ActiveRecipeGraphNode): Boolean {
		return node.node.dependencies.all { id -> graph.nodes.first { it.node.id == id }.isFinished }
	}

//	fun getElapsed(node: ActiveRecipeGraphNode, now: Long): Duration? {
//		val dependencies = graph.nodes.filter { node.node.dependencies.contains(it.node.id) }
//		val max =
//			dependencies.maxBy { if (it.finishedAt == 0L) Long.MAX_VALUE else it.finishedAt }?.finishedAt ?: startedAt
//		if (max == 0L || max == Long.MAX_VALUE) {
//			return null
//		}
//
//		val time = if (node.finishedAt > 0) node.finishedAt else now
//		return (time - max).milliseconds
//	}

	fun isSingleRecipe(): Boolean {
		val count = graph.nodes.count()
		if (count > 1) {
			val name = graph.nodes[0].recipeName
			return graph.nodes.all { it.recipeName == name }
		}
		return true
	}
}