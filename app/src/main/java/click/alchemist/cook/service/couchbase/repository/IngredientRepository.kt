package click.alchemist.cook.service.couchbase.repository

import click.alchemist.cook.extension.equalTo
import click.alchemist.cook.extension.isNotNullOrBlank
import click.alchemist.cook.model.DatabaseObject
import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.model.Recipe
import click.alchemist.cook.service.couchbase.CouchbaseService
import com.couchbase.lite.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn


class IngredientRepository(couchbase: CouchbaseService) {
    val all = couchbase.observe { db ->
        QueryBuilder.select(SelectResult.expression(Expression.property(Recipe::ingredients.name)))
            .from(DataSource.database(db))
            .where(DatabaseObject::type equalTo Recipe::class.simpleName)
    }
        .map(this::convertIngredients)
        .shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)

    private fun convertIngredients(queryChange: QueryChange): Set<String> {
        val allResults = queryChange.results?.allResults() ?: return emptySet()
        if (allResults.isEmpty()) return emptySet()

        val allNames = sortedSetOf<String>({ o1, o2 -> o1.compareTo(o2, true) })

        for (row in allResults) {
            val value = row.getArray(0)
            for (arrayEntry in value) {
                if (arrayEntry is Dictionary) {
                    val category = arrayEntry.getString(Ingredient::unitCategory.name)
                    if (category.isNullOrBlank() || category == IngredientCategory.HEADER.name) {
                        continue
                    }

                    val name = arrayEntry.getString(Ingredient::name.name)
                    if (name.isNotNullOrBlank()) {
                        allNames.add(name.trim())
                    }
                }
            }
        }

        return allNames
    }
}