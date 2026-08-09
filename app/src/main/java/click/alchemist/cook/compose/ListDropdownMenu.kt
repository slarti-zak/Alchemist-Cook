package click.alchemist.cook.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R

@Composable
fun <T> ListDropdownMenu(
	selected: T,
	items: List<T>,
	modifier: Modifier = Modifier,
	onPicked: (T) -> Unit = {},
	itemFunction: @Composable (T) -> Unit
) {
	var open by remember { mutableStateOf(false) }

	Row(
		modifier = modifier
			.then(
				Modifier
					.then(if (items.count() <= 1) Modifier else Modifier.clickable { open = true })
					.height(IntrinsicSize.Min)
					.wrapContentSize(Alignment.Center)
			),
		verticalAlignment = Alignment.CenterVertically
	) {
		itemFunction(selected)
		Icon(
			painter = painterResource(R.drawable.ic_menu_down), contentDescription = stringResource(R.string.content_description_dropdown_indicator),
			Modifier
				.heightIn(max = 24.dp)
				.fillMaxHeight()
				.aspectRatio(1f)
		)
		DropdownMenu(
			expanded = open,
			onDismissRequest = { open = false }) {
			items.forEach {
				DropdownMenuItem(
					text = { itemFunction(it) },
					onClick = {
						open = false
						onPicked(it)
					}
				)
			}
		}
	}
}

@Preview
@Composable
private fun Preview() {
	AppTheme {
		ListDropdownMenu("selected", listOf("selected", "1", "2")) {
			Text(it)
		}
	}
}