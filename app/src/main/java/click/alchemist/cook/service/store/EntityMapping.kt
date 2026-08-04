package click.alchemist.cook.service.store

import click.alchemist.cook.model.ActiveRecipeGraph
import click.alchemist.cook.model.ActiveRecipes
import click.alchemist.cook.model.DbDuration
import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.model.PlannedRecipe
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.model.RecipeGraph
import click.alchemist.cook.model.RunningTimer
import click.alchemist.cook.model.ShoppingList
import click.alchemist.cook.model.ShoppingListItem
import click.alchemist.cook.service.store.index.ActiveRecipesEntity
import click.alchemist.cook.service.store.index.PlannedRecipeEntity
import click.alchemist.cook.service.store.index.RecipeEntity
import click.alchemist.cook.service.store.index.RunningTimerEntity
import click.alchemist.cook.service.store.index.ShoppingListEntity
import click.alchemist.cook.service.store.index.ShoppingListItemEntity
import com.fasterxml.jackson.module.kotlin.readValue
import java.math.BigDecimal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Room index rows -> the same domain models the repositories always returned. */

fun RecipeEntity.toDomain(): Recipe = Recipe(
	name = name,
	content = content,
	ingredients = YamlMapper.instance.readValue(ingredientsJson),
	serves = serves,
	extendedContent = extendedContentJson?.let { YamlMapper.instance.readValue<RecipeGraph>(it) },
	id = id
)

fun ShoppingListEntity.toDomain(): ShoppingList = ShoppingList(name = name, id = id)

fun ShoppingListItemEntity.toDomain(): ShoppingListItem = ShoppingListItem(
	shoppingListId = shoppingListId,
	ingredient = Ingredient(ingredientName, BigDecimal(ingredientAmount), IngredientCategory.valueOf(ingredientUnitCategory)),
	finished = finished,
	id = id
)

fun PlannedRecipeEntity.toDomain(): PlannedRecipe = PlannedRecipe(recipeId = recipeId, servings = servings, id = id)

fun ActiveRecipesEntity.toDomain(): ActiveRecipes = ActiveRecipes(
	graph = YamlMapper.instance.readValue<ActiveRecipeGraph>(graphJson),
	startedAt = startedAt,
	id = id
)

fun RunningTimerEntity.toDomain(): RunningTimer = RunningTimer(
	recipeId = recipeId,
	graphNodeId = graphNodeId,
	title = title,
	content = content,
	duration = DbDuration(if (durationMillis == Double.MAX_VALUE) Duration.INFINITE else durationMillis.milliseconds),
	startedAt = startedAt,
	id = id
)
