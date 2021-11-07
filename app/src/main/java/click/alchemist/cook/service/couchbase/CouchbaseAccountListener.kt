package click.alchemist.cook.service.couchbase

import android.content.Context
import click.alchemist.cook.R
import click.alchemist.cook.service.settings.AndroidSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class CouchbaseAccountListener(context: Context, androidSettings: AndroidSettings) {
    val databaseFlow: SharedFlow<CouchbaseDatabase>

    var database: CouchbaseDatabase? = null
        private set

    init {
        val user = androidSettings.register(context.getString(R.string.settings_account_name_key), "")
        val password = androidSettings.register(context.getString(R.string.settings_account_password_key), "")

        databaseFlow = user.combine(password) { u, p -> Pair(u, p) }
            .flatMapLatest { pair ->
                channelFlow {
                    val u = pair.first
                    val p = pair.second
                    val newDatabase = if (u.isNotBlank() && p.isNotBlank()) {
                        CouchbaseDatabase.create(u, p)
                    } else {
                        CouchbaseDatabase.create()
                    }
                    send(newDatabase)
                    awaitClose {
                        newDatabase.stop()
                    }
                }
            }.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)

        CoroutineScope(Dispatchers.IO).launch {
            databaseFlow.collect {
                database = it
            }
        }
    }
}