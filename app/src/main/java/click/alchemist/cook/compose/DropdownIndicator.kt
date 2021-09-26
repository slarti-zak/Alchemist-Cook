package click.alchemist.cook.compose

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R

@Composable
fun DropdownIndicator(text: String) {
	Text(
		text = text,
		modifier = Modifier
			.wrapContentHeight(Alignment.CenterVertically)
			.wrapContentWidth(Alignment.CenterHorizontally)
	)
	Icon(painter = painterResource(R.drawable.ic_menu_down), contentDescription = "Dropdown Indicator", Modifier.size(24.dp))
}

@Preview
@Composable
private fun Preview() {
	AppTheme {
		DropdownIndicator(text = "Select this")
	}
}