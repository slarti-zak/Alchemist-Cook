package click.alchemist.cook.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecipeGraphNode(
	val id: String = "",
	val text: String = "",
	val duration: DbDuration = DbDuration.ZERO,
	val dependencies: List<String> = emptyList()
) : Parcelable
