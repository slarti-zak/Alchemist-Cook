package click.alchemist.cook.compose

import androidx.compose.runtime.Composable
import click.alchemist.cook.R

@Composable
fun BackButton(onBackNavigation: () -> Unit) {
	CookIconButton(onBackNavigation, R.drawable.ic_arrow_left, "Back")
}

