package click.alchemist.cook.viewmodel

import click.alchemist.cook.model.IngredientUnit
import kotlinx.coroutines.flow.MutableStateFlow

class IngredientEditModel(val id: Int = -1) {
    val name = MutableStateFlow("")
    var amount = MutableStateFlow("1")
    var unit = MutableStateFlow(IngredientUnit.TIMES)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IngredientEditModel

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }
}
