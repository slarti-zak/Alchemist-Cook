package click.alchemist.cook.compose.ingredient

import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.ListDropdownMenu
import click.alchemist.cook.model.IngredientUnit
import click.alchemist.cook.service.IngredientFormatter

@Composable
fun IngredientUnitPicker(
	modifier: Modifier = Modifier,
	unit: IngredientUnit = IngredientUnit.TIMES,
	units: List<IngredientUnit> = IngredientUnit.values().filter { it != IngredientUnit.HEADER },
	onPicked: ((IngredientUnit) -> Unit) = { }
) {
	ListDropdownMenu(
		unit, units, modifier.then(
			Modifier
				.requiredWidthIn(min = 70.dp)
		), onPicked
	) {
		Text(stringResource(IngredientFormatter.nameOf(it)))
	}
}

@Preview
@Composable
private fun Preview() {
	AppTheme {
		IngredientUnitPicker()
	}
}