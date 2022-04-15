package click.alchemist.cook.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable

@Composable
fun rememberToolbarPadding() = WindowInsets.statusBars.asPaddingValues()