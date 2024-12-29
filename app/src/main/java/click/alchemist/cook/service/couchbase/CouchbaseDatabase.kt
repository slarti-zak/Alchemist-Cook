package click.alchemist.cook.service.couchbase

import click.alchemist.cook.BuildConfig
import click.alchemist.cook.logDebug
import click.alchemist.cook.logError
import click.alchemist.cook.logInfo
import click.alchemist.cook.model.BlobModel
import click.alchemist.cook.model.DatabaseObject
import click.alchemist.cook.model.DatabaseSettings
import click.alchemist.cook.model.DbDuration
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
import com.couchbase.lite.MaintenanceType
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
import com.microsoft.appcenter.crashes.Crashes
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds


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
						Crashes.trackError(error)
					}
				}
			}
			replicator.addChangeListener { change: ReplicatorChange ->
				scope.launch { replicatorChanges.emit(CouchbaseState.account(change.status)) }
				logInfo(TAG, "Replicator Status ${change.status}")
				change.status.error?.let {
					logError(TAG, "Replicator Error code: ${it.code}")
				}

				if (change.status.activityLevel == ReplicatorActivityLevel.STOPPED) {
					if (active.value) {
						if (!paused && change.status.error == null) {
							replicator.start(false)
						}
					} else {
						database.close()
					}
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

	suspend fun getBlob(documentId: String, blobKey: String): BlobModel {
		return withContext(Dispatchers.IO) {
			val document = database.defaultCollection.getDocument(documentId)
			val blob = document?.getBlob(blobKey)
			if (blob == null) BlobModel.empty else BlobModel(blob)
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

	fun runMaintenance(): Boolean {
		val now = System.currentTimeMillis()
		val settings = load("database-settings", DatabaseSettings::class)
		if (settings == null || settings.lastMaintenance > now) {
			save(DatabaseSettings(now, id = "database-settings"))
			return false
		}

		if (now.milliseconds - settings.lastMaintenance.milliseconds > 30.days) {
			val result = database.performMaintenance(MaintenanceType.COMPACT)
					&& database.performMaintenance(MaintenanceType.OPTIMIZE)

			settings.lastMaintenance = now
			save(settings)
			return result
		}
		return false
	}

	companion object {
		private const val TAG: String = "Couchbase"

		fun create(executor: Executor): CouchbaseDatabase {
			val config = DatabaseConfiguration()
			val database = Database("cook_guest_database", config)

			return CouchbaseDatabase("guest", database, executor)
		}

		fun create(username: String, password: String, executor: Executor): CouchbaseDatabase {
			// Get the database (and create it if it doesn't exist).
			val config = DatabaseConfiguration()
			val database = Database(username, config)

			// Create replicators to push and pull changes to and from the cloud.
			val targetEndpoint: Endpoint = URLEndpoint(URI(BuildConfig.couchbaseSyncUrl))
			val replConfig = ReplicatorConfiguration(targetEndpoint).apply {
				addCollection(database.defaultCollection, CollectionConfiguration().apply {
					channels = listOf(username, "!")
					conflictResolver = ConflictResolver.DEFAULT
				})
				type = ReplicatorType.PUSH_AND_PULL
				authenticator = BasicAuthenticator(username, password.toCharArray())

				// Add authentication.
				isContinuous = true
				maxAttemptWaitTime = 120
			}

			val replicator = Replicator(replConfig)

			return CouchbaseDatabase(username, database, executor, replicator)
		}
	}
}