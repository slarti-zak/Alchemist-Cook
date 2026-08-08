package click.alchemist.cook.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import click.alchemist.cook.BuildConfig
import click.alchemist.cook.LocaleHelper
import click.alchemist.cook.R
import org.koin.androidx.viewmodel.ext.android.viewModel


class SettingsActivity : AppCompatActivity(R.layout.activity_settings) {

	override fun attachBaseContext(newBase: Context?) {
		super.attachBaseContext(if (newBase == null) null else LocaleHelper.onAttach(newBase))
	}


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		WindowCompat.setDecorFitsSystemWindows(window, false)

		supportFragmentManager
			.beginTransaction()
			.replace(R.id.settings, SettingsFragment())
			.commit()

		val container = findViewById<LinearLayout>(R.id.container)
		val settingsContainer = findViewById<FrameLayout>(R.id.settings)
		val toolbar = findViewById<Toolbar>(R.id.toolbar)
		val toolbarSpacer = findViewById<View>(R.id.toolbarSpacer)

		toolbar.setNavigationOnClickListener { up() }

		ViewCompat.setOnApplyWindowInsetsListener(container) { _, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			toolbarSpacer.updateLayoutParams<LinearLayout.LayoutParams> { height = insets.top }
			settingsContainer.updatePadding(bottom = insets.bottom)

			WindowInsetsCompat.CONSUMED
		}
	}

	private fun up() {
		val intent = Intent(supportParentActivityIntent)
		intent.addFlags(
			Intent.FLAG_ACTIVITY_SINGLE_TOP
					or Intent.FLAG_ACTIVITY_NO_ANIMATION
					or Intent.FLAG_ACTIVITY_CLEAR_TASK
					or Intent.FLAG_ACTIVITY_NEW_TASK
		)
		startActivity(intent)
	}

	override fun onBackPressed() {
		up()
	}


	class SettingsFragment : PreferenceFragmentCompat() {

		private val viewModel: SettingsViewModel by viewModel()

		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			setPreferencesFromResource(R.xml.root_preferences, rootKey)
		}

		override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
			super.onViewCreated(view, savedInstanceState)

			val info = preferenceManager.findPreference<PreferenceCategory>("key_info")
			info?.summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

			setUpActionPreferences()
		}

		private fun setUpActionPreferences() {
			preferenceManager.findPreference<Preference?>(getString(R.string.settings_storage_key))
				?.setOnPreferenceClickListener {
					startActivity(PersonalLibraryActivity.intent(requireContext()))
					true
				}

			preferenceManager.findPreference<Preference?>(getString(R.string.settings_shared_libraries_key))
				?.setOnPreferenceClickListener {
					startActivity(LibraryManagementActivity.intent(requireContext()))
					true
				}

			preferenceManager.findPreference<Preference?>(getString(R.string.settings_sync_now_key))
				?.setOnPreferenceClickListener {
					viewModel.syncNow()
					Toast.makeText(requireContext(), "Sync started", Toast.LENGTH_SHORT).show()
					true
				}
		}
	}
}