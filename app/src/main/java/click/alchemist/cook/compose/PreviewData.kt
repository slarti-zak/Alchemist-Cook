package click.alchemist.cook.compose

import click.alchemist.cook.model.*
import click.alchemist.cook.viewmodel.IngredientModel
import click.alchemist.cook.viewmodel.RecipeGraphModel
import click.alchemist.cook.viewmodel.RecipeGraphNodeModel
import click.alchemist.cook.viewmodel.TimerModel
import kotlin.time.Duration

fun previewIngredients(): List<IngredientModel> {
    return listOf(
        IngredientModel(Ingredient("Header", unitCategory = IngredientCategory.HEADER), 1),
        IngredientModel(Ingredient("Item 1"), 2),
        IngredientModel(Ingredient("Item 2"), 3),
        IngredientModel(Ingredient("Other", unitCategory = IngredientCategory.HEADER), 4),
        IngredientModel(Ingredient("Milk"), 5)
    )
}

fun previewTimers(): List<TimerModel> {
    return listOf(
        TimerModel(Timer("Timer", DbDuration(Duration.minutes(5))))
    )
}

fun previewRunningTimer(): TimerModel {
    val duration = DbDuration(Duration.minutes(5))
    return TimerModel(Timer("Timer", duration), RunningTimer(duration = duration), remaining = Duration.minutes(1), 0.2)
}

fun previewExtendedInstruction(): RecipeGraphModel {
    return RecipeGraphModel(
        listOf(
            RecipeGraphNodeModel(RecipeGraphNode("id1", "Instruction 1"), "recipe name"),
            RecipeGraphNodeModel(RecipeGraphNode("id2", "Instruction 2"), "recipe name"),
            RecipeGraphNodeModel(RecipeGraphNode("id3", "Timed Instruction", DbDuration(Duration.minutes(5))), "recipe name"),
        ), false
    )
}