package click.alchemist.cook.compose

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

@Composable
fun CookIconButton(
	onClick: () -> Unit,
	@DrawableRes iconResource: Int,
	contentDescription: String,
	modifier: Modifier = Modifier,
	tint: Color = LocalContentColor.current
) {
	IconButton(onClick = onClick, modifier = modifier) {
		Icon(painterResource(iconResource), contentDescription, tint = tint)
	}
}