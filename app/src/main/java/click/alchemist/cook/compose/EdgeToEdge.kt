package click.alchemist.cook.compose

import androidx.compose.runtime.Composable
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues

@Composable
fun rememberToolbarPadding() = rememberInsetsPaddingValues(
	LocalWindowInsets.current.statusBars,
	applyBottom = false,
)