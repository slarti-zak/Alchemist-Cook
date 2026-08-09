package click.alchemist.cook.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import click.alchemist.cook.R

@Composable
fun BackButton(onBackNavigation: () -> Unit) {
	CookIconButton(onBackNavigation, R.drawable.ic_arrow_left, stringResource(R.string.content_description_back))
}

