package click.alchemist.cook.service.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import click.alchemist.cook.R
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
	private val languageKey = context.getString(R.string.settings_language_key)
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
		preferenceManager.edit {
			putString(key, value)
		}
	}

	fun getStringSet(key: String, default: MutableSet<String>?): MutableSet<String>? {
		return preferenceManager.getStringSet(key, default)
	}

	fun putStringSet(key: String, value: MutableSet<String>) {
		preferenceManager.edit {
			putStringSet(key, value)
		}
	}

	/** The user's language override — an ISO code, or "" to follow the system. See [click.alchemist.cook.LocaleHelper]. */
	fun language(): Flow<String> = register(languageKey, "")

	fun setLanguage(language: String) = putString(languageKey, language)

	class SettingUpdate(val sharedPreferences: SharedPreferences, val key: String)
}

