package click.alchemist.cook.service.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn


class AndroidSettings(context: Context) {
	private val preferenceManager: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
	private val settingUpdatesFlow: Flow<SettingUpdate>

	init {
		settingUpdatesFlow = callbackFlow {
			val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
				if (sharedPreferences != null && key != null) {
					trySendBlocking(SettingUpdate(sharedPreferences, key))
				}
			}
			preferenceManager.registerOnSharedPreferenceChangeListener(listener)

			awaitClose {
				preferenceManager.registerOnSharedPreferenceChangeListener(listener)
			}
		}.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.WhileSubscribed(), 1)
	}

	fun register(key: String, default: String): Flow<String> {
		return settingUpdatesFlow
			.filter { it.key == key }
			.map { it.sharedPreferences.getString(key, default) ?: default }
			.onStart { emit(preferenceManager.getString(key, default) ?: default) }
	}

	fun getString(key: String, default: String?): String? {
		return preferenceManager.getString(key, default)
	}

	fun putString(key: String, value: String) {
		with(preferenceManager.edit()) {
			putString(key, value)
			apply()
		}
	}

	fun getStringSet(key: String, default: MutableSet<String>?): MutableSet<String>? {
		return preferenceManager.getStringSet(key, default)
	}

	fun putStringSet(key: String, value: MutableSet<String>) {
		with(preferenceManager.edit()) {
			putStringSet(key, value)
			apply()
		}
	}

	class SettingUpdate(val sharedPreferences: SharedPreferences, val key: String)
}

