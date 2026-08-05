package click.alchemist.cook.service.couchbase

import click.alchemist.cook.BuildConfig
import click.alchemist.cook.logDebug
import click.alchemist.cook.logError
import click.alchemist.cook.logInfo
import click.alchemist.cook.model.DatabaseObject
import click.alchemist.cook.model.DbDuration
import click.alchemist.cook.service.couchbase.CouchbaseDatabase.Companion.create
import click.alchemist.cook.service.couchbase.json.BigDecimalDeserializer
import click.alchemist.cook.service.couchbase.json.BigDecimalSerializer
import click.alchemist.cook.service.couchbase.json.DbDurationDeserializer
import click.alchemist.cook.service.couchbase.json.DbDurationSerializer
import click.alchemist.cook.service.couchbase.json.DurationDeserializer
import click.alchemist.cook.service.couchbase.json.DurationSerializer
import com.couchbase.lite.BasicAuthenticator
import com.couchbase.lite.CollectionConfiguration
import com.couchbase.lite.ConflictResolver
import com.couchbase.lite.Database
import com.couchbase.lite.DatabaseConfiguration
import com.couchbase.lite.Document
import com.couchbase.lite.Endpoint
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.Query
import com.couchbase.lite.QueryChange
import com.couchbase.lite.Replicator
import com.couchbase.lite.ReplicatorActivityLevel
import com.couchbase.lite.ReplicatorChange
import com.couchbase.lite.ReplicatorConfiguration
import com.couchbase.lite.ReplicatorType
import com.couchbase.lite.Result
import com.couchbase.lite.ResultSet
import com.couchbase.lite.URLEndpoint
import com.couchbase.lite.documentChangeFlow
import com.couchbase.lite.queryChangeFlow
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.URI
import java.util.concurrent.Executor
import kotlin.reflect.KClass


/**
 * Legacy local Couchbase access — WebDAV replaced it as the app's storage, so nothing here should
 * ever write to it again (see the read-only [create] factory below). What's left exists only to let
 * [click.alchemist.cook.service.migration.CouchbaseToWebDavMigrator] read a user's pre-migration data.
 */
class CouchbaseDatabase(
	private val username: String,
	private val database: Database,
	private val executor: Executor,
	private val replicator: Replicator? = null
) {
	private var paused: Boolean = false
	private val active = MutableStateFlow(true)

	val replicatorChanges: Flow<CouchbaseState>

	init {
		if (replicator != null) {
			val replicatorChanges = MutableStateFlow(CouchbaseState.account(replicator.status))
			val scope = CoroutineScope(Dispatchers.IO)

			// Listen to replicator change events.
			replicator.addDocumentReplicationListener { replication ->
				logDebug(TAG, "Started Replication: $replication")
				for (doc in replication.documents) {
					logDebug(TAG, "Replicated document: ${doc.id}")
					val error = doc.error
					if (error != null) {
						logError(TAG, "Could not replicate: $doc", error)
					}
				}
			}
			replicator.addChangeListener { change: ReplicatorChange ->
				scope.launch { replicatorChanges.emit(CouchbaseState.account(change.status)) }
				logInfo(TAG, "Replicator Status ${change.status}")
				change.status.error?.let {
					logError(TAG, "Replicator Error code: ${it.code}")
				}

				// This is a one-shot, pull-only top-up of the local read cache (see `create` below), not
				// a live connection to keep alive — a clean STOPPED means it finished, not that it dropped
				// and needs reconnecting, so there's nothing to restart here, only the closed-database case
				// to still handle.
				if (change.status.activityLevel == ReplicatorActivityLevel.STOPPED && !active.value) {
					database.close()
				}
			}

			logDebug(TAG, "Replication started")
			replicator.start(false)
			this.replicatorChanges = replicatorChanges
		} else {
			replicatorChanges = flowOf(CouchbaseState.guest())
		}
	}

	private val mapper = ObjectMapper().apply {
		configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
		configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)

		val module = SimpleModule().apply {
			addSerializer(BigDecimal::class.java, BigDecimalSerializer())
			addDeserializer(BigDecimal::class.java, BigDecimalDeserializer())

			addSerializer(Double::class.java, DurationSerializer())
			addDeserializer(Double::class.java, DurationDeserializer())

			addSerializer(DbDuration::class.java, DbDurationSerializer())
			addDeserializer(DbDuration::class.java, DbDurationDeserializer())
		}
		registerModule(module)
	}

	fun save(dataOwner: DatabaseObject, documentFunction: ((MutableDocument) -> Unit)? = null) {
		dataOwner.owner = username
		val mapType = HashMap::class.java

		@Suppress("UNCHECKED_CAST")
		val dict: HashMap<String, Any> = mapper.convertValue(dataOwner, mapType) as HashMap<String, Any>

		val existingId = dataOwner.id
		val mutableDoc =
			if (existingId.isBlank()) MutableDocument(dict)
			else MutableDocument(existingId, dict)
		documentFunction?.invoke(mutableDoc)
		database.defaultCollection.save(mutableDoc)
		dataOwner.id = mutableDoc.id
	}

	fun <T : DatabaseObject> load(id: String, clazz: Class<T>): T? {
		val doc = database.defaultCollection.getDocument(id) ?: return null
		return parse(doc, clazz)
	}

	fun <T : DatabaseObject> load(id: String, clazz: KClass<T>) = load(id, clazz.java)

	private fun <T : DatabaseObject> parse(doc: Document, clazz: Class<T>): T? {
		val map = doc.toMap()
		val entityType = map[DatabaseObject::type.name]
		if (entityType != clazz.simpleName) {
			return null
		}
		val entity = parse(map, clazz)
		entity.id = doc.id
		return entity
	}

	fun <T> parse(dict: com.couchbase.lite.Dictionary, clazz: Class<T>): T {
		return parse(dict.toMap(), clazz)
	}

	fun <T> parse(dict: Map<*, *>, clazz: Class<T>): T {
		return mapper.convertValue(dict, clazz)
	}

	fun <T : DatabaseObject> parse(row: Result, clazz: Class<T>): T {
		val all = row.toMap()
		val dict = all[database.defaultCollection.name] as HashMap<*, *>
		val id = all["id"] as String
		val value = mapper.convertValue(dict, clazz)
		value.id = id
		return value
	}

	fun <T : DatabaseObject> parse(
		resultRows: ResultSet?,
		clazz: Class<T>,
		deleteIfCannotBeParsed: Boolean = false
	): List<T> {
		if (resultRows == null) return emptyList()

		val entities = mutableListOf<T>()
		for (row in resultRows) {
			try {
				val element = parse(row, clazz)
				entities.add(element)
			} catch (e: Exception) {
				if (deleteIfCannotBeParsed) {
					row.getString("id")?.let {
						delete(it)
					}
				}
				logError(TAG, "Could not parse element! Auto-delete: $deleteIfCannotBeParsed", e)
			}
		}
		return entities
	}

	fun <T : DatabaseObject> parse(resultRows: ResultSet, clazz: KClass<T>) = parse(resultRows, clazz.java)

	fun delete(id: String) {
		database.defaultCollection.getDocument(id)?.let { database.defaultCollection.delete(it) }
	}

	fun stop() {
		logInfo(TAG, "Closing database")
		val scope = CoroutineScope(Dispatchers.IO)
		scope.launch {
			active.emit(false)
			if (replicator == null) {
				database.close()
			} else {
				if (replicator.status.activityLevel == ReplicatorActivityLevel.STOPPED) {
					database.close()
				} else {
					replicator.stop()
				}
			}
		}
	}

	suspend fun query(builder: (Database) -> Query): ResultSet {
		if (active.value) {
			return withContext(Dispatchers.IO) {
				builder(database).execute()
			}
		}
		throw IllegalStateException("Database closed!")
	}

	fun <T : DatabaseObject> observe(id: String, clazz: KClass<T>): Flow<T?> {
		return active.flatMapLatest { active ->
			if (active) {
				database.defaultCollection.documentChangeFlow(id, executor)
					.map {
						val document = it.collection.getDocument(it.documentID)
						if (document == null) null else parse(document, clazz.java)
					}
					.onStart {
						val document = load(id, clazz)
						emit(document)
					}
			} else emptyFlow()
		}
	}

	fun observe(builder: (Database) -> Query): Flow<QueryChange> {
		return active.flatMapLatest { active ->
			if (active) {
				val query = builder(database)
				query.queryChangeFlow(executor)
			} else emptyFlow()
		}
	}

	fun batch(function: () -> Unit) {
		database.inBatch<Exception>(function)
	}

	fun getDocument(id: String): Document? {
		return database.defaultCollection.getDocument(id)
	}

	fun saveDocument(document: MutableDocument) {
		database.defaultCollection.save(document)
	}

	fun resume() {
		if (!paused) {
			return
		}
		paused = false

		val replicator = replicator ?: return
		val activityLevel = replicator.status.activityLevel
		logInfo(TAG, "Refreshing replicator from status: $activityLevel")
		when (activityLevel) {
			ReplicatorActivityLevel.STOPPED -> replicator.start(false)
			ReplicatorActivityLevel.OFFLINE, ReplicatorActivityLevel.CONNECTING -> replicator.stop()
			else -> {}
		}
	}

	fun pause() {
		if (paused) {
			return
		}

		paused = true
		val replicator = replicator ?: return
		replicator.stop()
	}

	companion object {
		private const val TAG: String = "Couchbase"

		fun create(executor: Executor): CouchbaseDatabase {
			val config = DatabaseConfiguration()
			val database = Database("cook_guest_database", config)

			return CouchbaseDatabase("guest", database, executor)
		}

		/**
		 * WebDAV is the authoritative store now — Couchbase only sticks around as a frozen, read-only
		 * source for [click.alchemist.cook.service.migration.CouchbaseToWebDavMigrator] to read out of.
		 * Nothing in the app should ever write to it again, so this replicator only ever pulls (never
		 * pushes anything back up) and runs once (not continuously) to top up the local copy for a
		 * device that hasn't opened Couchbase before, rather than holding a live connection open.
		 */
		fun create(username: String, password: String, executor: Executor): CouchbaseDatabase {
			// Get the database (and create it if it doesn't exist).
			val config = DatabaseConfiguration()
			val database = Database(username, config)

			val targetEndpoint: Endpoint = URLEndpoint(URI(BuildConfig.couchbaseSyncUrl))
			val replConfig = ReplicatorConfiguration(targetEndpoint).apply {
				addCollection(database.defaultCollection, CollectionConfiguration().apply {
					channels = listOf(username, "!")
					conflictResolver = ConflictResolver.DEFAULT
				})
				type = ReplicatorType.PULL
				authenticator = BasicAuthenticator(username, password.toCharArray())

				isContinuous = false
				maxAttemptWaitTime = 120
			}

			val replicator = Replicator(replConfig)

			return CouchbaseDatabase(username, database, executor, replicator)
		}
	}
}