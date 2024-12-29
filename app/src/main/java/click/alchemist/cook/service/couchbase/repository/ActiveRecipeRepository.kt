package click.alchemist.cook.service.couchbase.repository

import click.alchemist.cook.extension.equalTo
import click.alchemist.cook.model.ActiveRecipes
import click.alchemist.cook.model.DatabaseObject
import click.alchemist.cook.service.couchbase.BaseRepository
import click.alchemist.cook.service.couchbase.CouchbaseService
import com.couchbase.lite.DataSource
import com.couchbase.lite.Expression
import com.couchbase.lite.Meta
import com.couchbase.lite.Ordering
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.QueryChange
import com.couchbase.lite.ResultSet
import com.couchbase.lite.SelectResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class ActiveRecipeRepository(couchbase: CouchbaseService) : BaseRepository<ActiveRecipes>(couchbase, ActiveRecipes::class) {
	fun save(recipe: ActiveRecipes) {
		couchbase.save(recipe)
	}

	fun live(): Flow<List<ActiveRecipes>> {
		return couchbase.observe { db ->
			QueryBuilder.select(SelectResult.all(), SelectResult.expression(Meta.id))
				.from(DataSource.collection(db.defaultCollection))
				.where(DatabaseObject::type equalTo ActiveRecipes::class.simpleName)
				.orderBy(Ordering.property(ActiveRecipes::startedAt.name))
				.limit(Expression.intValue(1))
		}.map(this::parse)
	}

	private fun parse(change: QueryChange) = parse(change.results)

	private fun parse(resultSet: ResultSet?): List<ActiveRecipes> {
		return couchbase.parse(resultSet, ActiveRecipes::class.java, true)
	}
}