package click.alchemist.cook.compose.shoppinglist.detail

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.Header
import click.alchemist.cook.compose.ingredient.IngredientWithAmount
import click.alchemist.cook.model.Ingredient
import click.alchemist.cook.model.IngredientCategory
import click.alchemist.cook.model.ShoppingListItem
import java.math.BigDecimal


@Composable
fun ShoppingListDetail(
	modifier: Modifier = Modifier,
	items: List<ShoppingListItem>,
	onClick: (ShoppingListItem) -> Unit = { },
	onLongClick: (ShoppingListItem) -> Unit = { }
) {
	LazyColumn(modifier = modifier) {
		items(
			items = items,
			key = { if (it.ingredient.unitCategory == IngredientCategory.HEADER) it.finished else it.id },
			itemContent = { item ->
				if (item.ingredient.unitCategory == IngredientCategory.HEADER) {
					val title = stringResource(if (item.finished) R.string.shopping_list_header_finished else R.string.shopping_list_header_todo)
					Header(title)
				} else {
					IngredientWithAmount(
						item.ingredient,
						item.finished,
						onClick = { onClick(item) },
						onLongClick = { onLongClick(item) })
				}
			})
	}
}


@Preview
@Composable
private fun Preview() {
	AppTheme {
		ShoppingListDetail(
			items = listOf(
				ShoppingListItem("a", Ingredient("Milk", BigDecimal.ONE, IngredientCategory.VOLUME)),
				ShoppingListItem(
					"b", Ingredient("Meat", BigDecimal.TEN, IngredientCategory.WEIGHT)
				)
			)
		)
	}
}