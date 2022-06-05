package click.alchemist.cook.service.couchbase

import click.alchemist.cook.model.DatabaseObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlin.reflect.KClass

abstract class BaseRepository<T : DatabaseObject>(
	protected val couchbase: CouchbaseService,
	protected val clazz: KClass<T>
) {
	fun live(id: String): Flow<T> {
		return couchbase.observe(id, clazz)
			.filterNotNull()
	}

	fun delete(document: T) {
		delete(document.id)
	}

	fun delete(documentId: String) {
		couchbase.delete(documentId)
	}
}