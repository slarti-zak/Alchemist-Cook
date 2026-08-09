package click.alchemist.cook.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.BackButton
import click.alchemist.cook.compose.ListDropdownMenu

/** The app's root settings screen — see [SettingsNavigation]. */
@Composable
fun SettingsHome(
	versionInfo: String,
	language: String,
	onLanguageChange: (String) -> Unit,
	onBack: () -> Unit,
	onStorageClick: () -> Unit,
	onSharedLibrariesClick: () -> Unit,
	onSyncNowClick: () -> Unit
) {
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.title_activity_settings)) },
				navigationIcon = { BackButton(onBack) }
			)
		}
	) { paddingValues ->
		Column(Modifier.padding(paddingValues)) {
			SettingsSectionHeader("App", versionInfo)
			ListItem(
				headlineContent = { Text(stringResource(R.string.settings_language_name)) },
				trailingContent = { LanguagePicker(language, onLanguageChange) }
			)

			HorizontalDivider()

			SettingsSectionHeader(stringResource(R.string.settings_webdav_header))
			ListItem(
				headlineContent = { Text(stringResource(R.string.settings_storage)) },
				modifier = Modifier.clickable(onClick = onStorageClick)
			)
			ListItem(
				headlineContent = { Text(stringResource(R.string.settings_shared_libraries)) },
				modifier = Modifier.clickable(onClick = onSharedLibrariesClick)
			)
			ListItem(
				headlineContent = { Text(stringResource(R.string.settings_sync_now)) },
				modifier = Modifier.clickable(onClick = onSyncNowClick)
			)
		}
	}
}

@Composable
private fun SettingsSectionHeader(title: String, subtitle: String? = null) {
	Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)) {
		Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
		if (subtitle != null) {
			Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
		}
	}
}

@Composable
private fun LanguagePicker(language: String, onLanguageChange: (String) -> Unit) {
	val labels = stringArrayResource(R.array.languages)
	val values = stringArrayResource(R.array.languages_aliases)
	val options = labels.zip(values)
	val selected = options.firstOrNull { it.second == language } ?: options.first()

	ListDropdownMenu(selected, options, onPicked = { onLanguageChange(it.second) }) {
		Text(it.first)
	}
}

@Preview("Settings")
@Composable
private fun SettingsHomePreview() {
	AppTheme {
		SettingsHome("1.0 (1)", "", {}, {}, {}, {}, {})
	}
}

@Preview("Settings Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SettingsHomeDarkPreview() {
	AppTheme {
		SettingsHome("1.0 (1)", "en", {}, {}, {}, {}, {})
	}
}
