package click.alchemist.cook.service.store

import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.model.RecipeGraph

/**
 * The fields that live in a recipe.md file's YAML front matter; `content` is the markdown body below
 * it. No `id` here — a recipe's folder name already carries it (see [EntityPaths.recipeIdFromPath]).
 */
data class RecipeFrontMatter(
	val name: String = "",
	val serves: Int = 1,
	val ingredients: List<Ingredient> = listOf(),
	val extendedContent: RecipeGraph? = null,
	val image: String? = null,
	val updatedAt: Long = 0L
)

data class ParsedRecipeFile(
	val recipe: Recipe,
	val imageFileName: String?,
	val updatedAt: Long
)

/**
 * Serializes/parses a recipe.md file: a YAML front-matter block (delimited by `---` lines)
 * followed by the recipe's markdown body, i.e. the existing [Recipe.content] string unchanged.
 */
object RecipeFileFormat {
	private const val DELIMITER = "---"

	fun serialize(recipe: Recipe, imageFileName: String?, updatedAt: Long): String {
		val frontMatter = RecipeFrontMatter(
			name = recipe.name,
			serves = recipe.serves,
			ingredients = recipe.ingredients,
			extendedContent = recipe.extendedContent,
			image = imageFileName,
			updatedAt = updatedAt
		)
		val yaml = YamlMapper.instance.writeValueAsString(frontMatter).trimEnd('\n')

		return buildString {
			appendLine(DELIMITER)
			appendLine(yaml)
			appendLine(DELIMITER)
			append(recipe.content)
		}
	}

	/** [id] is recovered from the recipe's folder name by the caller (see [EntityPaths.recipeIdFromPath]). */
	fun parse(text: String, id: String): ParsedRecipeFile {
		val lines = text.lines()
		require(lines.isNotEmpty() && lines[0].trim() == DELIMITER) {
			"Recipe file is missing its opening front matter delimiter"
		}

		val closingIndex = lines.drop(1).indexOfFirst { it.trim() == DELIMITER }
		require(closingIndex >= 0) { "Recipe file is missing its closing front matter delimiter" }

		val yaml = lines.subList(1, closingIndex + 1).joinToString("\n")
		val body = lines.drop(closingIndex + 2).joinToString("\n").trimStart('\n')

		val frontMatter = YamlMapper.instance.readValue(yaml, RecipeFrontMatter::class.java)

		val recipe = Recipe(
			name = frontMatter.name,
			content = body,
			ingredients = frontMatter.ingredients,
			serves = frontMatter.serves,
			extendedContent = frontMatter.extendedContent,
			id = id
		)

		return ParsedRecipeFile(recipe, frontMatter.image, frontMatter.updatedAt)
	}
}
