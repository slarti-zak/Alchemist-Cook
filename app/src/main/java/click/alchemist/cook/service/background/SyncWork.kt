package click.alchemist.cook.service.background

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import click.alchemist.cook.BuildConfig
import click.alchemist.cook.R
import click.alchemist.cook.logError
import click.alchemist.cook.logInfo
import click.alchemist.cook.service.couchbase.ReplicatorTypeHelper
import com.couchbase.lite.*
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import java.net.URI

class SyncWork(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val preferenceManager: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(appContext)


    override suspend fun doWork(): Result {
        val user = preferenceManager.getString(applicationContext.getString(R.string.settings_account_name_key), "")
        val password = preferenceManager.getString(applicationContext.getString(R.string.settings_account_password_key), "")

        if (user.isNullOrBlank() || password.isNullOrBlank()) {
            logInfo("RecipeSync", "Not syncing, not user or password")
            return Result.success()
        }

        val config = DatabaseConfiguration()
        val database = try {
            Database(user, config)
        } catch (e: Exception) {
            logError("RecipeSync", "Database error", e)
            return Result.retry()
        }

        val repl = create(user, password, database)
        var token: ListenerToken? = null
        var exception: Exception? = null

        val syncedDocuments = try {
            logInfo("RecipeSync", "Syncing for <$user>")
            suspendCancellableCoroutine { cont ->
                token = repl.addChangeListener {
                    val activityLevel = it.status.activityLevel
                    logInfo("RecipeSync", "Sync change to $activityLevel")
                    if (activityLevel == AbstractReplicator.ActivityLevel.STOPPED) {
                        cont.resume(it.status.progress.total, null)
                    }
                }

                logInfo("RecipeSync", "Starting sync")
                repl.start()
            }
        } catch (e: Exception) {
            logError("RecipeSync", "Sync error", e)
            exception = e
            0L
        }

        token?.let { repl.removeChangeListener(it) }
        database.close()

        return if (exception == null) {
            logInfo("RecipeSync", "Sync done. $syncedDocuments documents synced.")
            val result = Data.Builder().putLong("count", syncedDocuments).build()
            Result.success(result)
        } else {
            Result.retry()
        }
    }

    private fun create(username: String, password: String, database: Database): Replicator {
        // Create replicators to push and pull changes to and from the cloud.
        val targetEndpoint: Endpoint = URLEndpoint(URI(BuildConfig.couchbaseSyncUrl))
        val replConfig = ReplicatorConfiguration(database, targetEndpoint).apply {

            @Suppress("INACCESSIBLE_TYPE", "UsePropertyAccessSyntax")
            setReplicatorType(ReplicatorTypeHelper.getReplicatorTypeFor(true, true))
            setAuthenticator(BasicAuthenticator(username, password.toCharArray()))

            // Add authentication.
            isContinuous = false
            channels = listOf(username, "!")
            conflictResolver = ConflictResolver.DEFAULT
        }

        return Replicator(replConfig)
    }
}
