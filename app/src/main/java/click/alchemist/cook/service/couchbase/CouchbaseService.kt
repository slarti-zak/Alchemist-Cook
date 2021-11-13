package click.alchemist.cook.service.couchbase

import click.alchemist.cook.model.BlobModel
import click.alchemist.cook.model.DatabaseObject
import com.couchbase.lite.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlin.reflect.KClass


class CouchbaseService(
	private val database: CouchbaseAccountListener
) {
	fun save(dataOwner: DatabaseObject, documentFunction: ((MutableDocument) -> Unit)? = null) {
		database.database!!.save(dataOwner, documentFunction)
	}

	fun <T : DatabaseObject> load(id: String, clazz: Class<T>): T? {
		return database.database!!.load(id, clazz)
	}

	fun <T : DatabaseObject> load(id: String, clazz: KClass<T>) = load(id, clazz.java)

	fun <T> parse(dict: Dictionary, clazz: Class<T>): T {
		return database.database!!.parse(dict.toMap(), clazz)
	}

	fun <T> parse(dict: Map<*, *>, clazz: Class<T>): T {
		return database.database!!.parse(dict.toMap(), clazz)
	}

	fun <T : DatabaseObject> parse(row: Result, clazz: Class<T>): T {
		return database.database!!.parse(row, clazz)
	}

	fun <T : DatabaseObject> parse(
		resultRows: ResultSet?,
		clazz: Class<T>,
		deleteIfCannotBeParsed: Boolean = false
	): List<T> {
		return database.database!!.parse(resultRows, clazz, deleteIfCannotBeParsed)
	}

	fun <T : DatabaseObject> parse(resultRows: ResultSet?, clazz: KClass<T>) = parse(resultRows, clazz.java)

	fun delete(id: String) {
		database.database!!.delete(id)
	}

	suspend fun query(builder: (Database) -> Query): ResultSet {
		return database.database!!.query(builder)
	}

	fun observe(builder: (Database) -> Query): Flow<QueryChange> {
		return database.databaseFlow.flatMapLatest { db -> db.observe(builder) }
	}

	suspend fun getBlob(documentId: String, blobKey: String): BlobModel {
		return database.database!!.getBlob(documentId, blobKey)
	}

	fun batch(function: () -> Unit) {
		database.database!!.batch(function)
	}

	fun getDocument(id: String): Document? {
		return database.database!!.getDocument(id)
	}

	fun saveDocument(document: MutableDocument) {
		database.database!!.saveDocument(document)
	}
}