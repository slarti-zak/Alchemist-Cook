package click.alchemist.cook

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import androidx.preference.PreferenceManager
import org.koin.core.component.KoinComponent
import java.util.*

object LocaleHelper : KoinComponent {

	fun onAttach(context: Context): Context {
		val language = getLanguage(context) ?: return context

		return setLocale(context, language)
	}

	private fun getLanguage(context: Context): String? {
		val preferences = PreferenceManager.getDefaultSharedPreferences(context)
		val language = preferences.getString(getLanguageKey(context), null)
		return if (language.isNullOrBlank()) null else language
	}

	private fun setLocale(context: Context, language: String): Context {
		persist(context, language)

		return try {
			val locale = Locale(language)
			Locale.setDefault(locale)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				updateResources(context, locale)
			} else {
				updateResourcesLegacy(context, locale)
			}
		} catch (e: Exception) {
			logError("Could not set language to $language", e)
			context
		}
	}

	private fun persist(context: Context, language: String?) {
		val preferences = PreferenceManager.getDefaultSharedPreferences(context)

		val editor = preferences.edit()
		if (language == null) {
			editor.remove(getLanguageKey(context))
		} else {
			editor.putString(getLanguageKey(context), language)
		}
		editor.apply()
	}

	private fun getLanguageKey(context: Context) = context.getString(R.string.settings_language_key)

	@TargetApi(Build.VERSION_CODES.N)
	private fun updateResources(context: Context, locale: Locale): Context {
		val configuration = context.resources.configuration
		configuration.setLocale(locale)
		configuration.setLayoutDirection(locale)
		return context.createConfigurationContext(configuration)
	}

	@Suppress("DEPRECATION")
	private fun updateResourcesLegacy(context: Context, locale: Locale): Context {
		val resources = context.resources
		val configuration = resources.configuration
		configuration.locale = locale
		configuration.setLayoutDirection(locale)
		resources.updateConfiguration(configuration, resources.displayMetrics)
		return context
	}
}