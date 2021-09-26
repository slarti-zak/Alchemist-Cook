package click.alchemist.cook.ui.settings

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import click.alchemist.cook.BuildConfig
import click.alchemist.cook.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel


class SettingsActivity : AppCompatActivity(R.layout.activity_settings) {

	@ExperimentalCoroutinesApi
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
		toolbar.setNavigationOnClickListener {
			up()
		}

		ViewCompat.setOnApplyWindowInsetsListener(container) { _, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			toolbarSpacer.updateLayoutParams<LinearLayout.LayoutParams> { height = insets.top }
			settingsContainer.updatePadding(bottom = insets.bottom)

			WindowInsetsCompat.CONSUMED
		}
	}

	private fun up() {
		if (isTaskRoot) {
			startActivity(supportParentActivityIntent)
		}
		finish()
	}

	override fun onBackPressed() {
		up()
	}

	@ExperimentalCoroutinesApi
	class SettingsFragment : PreferenceFragmentCompat() {

		private val viewModel: SettingsViewModel by viewModel()

		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			setPreferencesFromResource(R.xml.root_preferences, rootKey)
		}

		override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
			super.onViewCreated(view, savedInstanceState)

			val info = preferenceManager.findPreference<PreferenceCategory>("key_info")
			info?.summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

			val passwordPref = preferenceManager.findPreference<EditTextPreference?>(getString(R.string.settings_account_password_key))
			passwordPref?.setOnBindEditTextListener { editText ->
				editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
				editText.setSelectAllOnFocus(true)
			}

			val syncView = preferenceManager.findPreference<SyncStatusPreference?>(getString(R.string.settings_account_sync_key))
			syncView?.apply {
				update(viewModel.syncState, lifecycleScope)
			}
		}
	}
}