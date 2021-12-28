package click.alchemist.cook.ui.recipe.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import click.alchemist.cook.R
import click.alchemist.cook.compose.AppTheme
import click.alchemist.cook.compose.SimpleTextField
import click.alchemist.cook.compose.SwipeDeleteBackground
import click.alchemist.cook.compose.ingredient.IngredientUnitPicker
import click.alchemist.cook.model.IngredientUnit
import click.alchemist.cook.viewmodel.IngredientEditModel
import kotlinx.coroutines.launch


@Composable
fun RecipeEditIngredientList(
	servings: Int,
	ingredients: List<IngredientEditModel>,
	onServingChanged: (Int) -> Unit = {},
	onIngredientDeleted: (IngredientEditModel) -> Unit = {},
	onNameChanged: (IngredientEditModel, String) -> Unit = { _, _ -> }
) {
	val state = rememberLazyListState()
	val scope = rememberCoroutineScope()

	LazyColumn(
		Modifier.fillMaxSize(),
		contentPadding = PaddingValues(8.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
		state = state,
		content = {
			item {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text(
						stringResource(R.string.ingredient_portions_hint),
						Modifier
							.padding(end = 8.dp), textAlign = TextAlign.End
					)
					SimpleTextField(
						servings.toString(),
						onValueChange = { onServingChanged(it.toIntOrNull() ?: return@SimpleTextField) },
						Modifier
							.widthIn(min = 60.dp)
							.padding(vertical = 8.dp)
					)
				}
			}

			items(ingredients, key = { it.id }) { ingredient ->
				val dismissState = rememberDismissState(
					confirmStateChange = {
						val dismissed = it == DismissValue.DismissedToEnd || it == DismissValue.DismissedToStart
						if (dismissed) onIngredientDeleted(ingredient)
						dismissed
					}
				)

				SwipeToDismiss(
					state = dismissState,
					background = { SwipeDeleteBackground(dismissState, clipShape = MaterialTheme.shapes.small) }) {
					EditableIngredient(ingredient, onNameChanged = { onNameChanged(ingredient, it) })
				}
			}
		})
	if (ingredients.isNotEmpty()) {
		LaunchedEffect(ingredients) {
			scope.launch {
				state.animateScrollToItem(ingredients.lastIndex)
			}
		}
	}
}

@Composable
fun EditableIngredient(
	ingredient: IngredientEditModel,
	onNameChanged: (String) -> Unit
) {
	val amount by ingredient.amount.collectAsState()
	val unit by ingredient.unit.collectAsState()
	val name by ingredient.name.collectAsState()
	val focusManager = LocalFocusManager.current

	Surface(shape = MaterialTheme.shapes.small) {
		Row(
			Modifier
				.fillMaxWidth()
				.height(IntrinsicSize.Min)
		) {
			OutlinedTextField(
				value = amount, { ingredient.amount.value = it },
				singleLine = true,
				modifier = Modifier
					.weight(0.3f),
				keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Number),
				keyboardActions = KeyboardActions { focusManager.moveFocus(FocusDirection.Right) }
			)
			IngredientUnitPicker(
				unit = unit,
				units = IngredientUnit.values().toList(),
				onPicked = { ingredient.unit.value = it },
				modifier = Modifier.fillMaxHeight()
			)
			OutlinedTextField(
				value = name, {
					ingredient.name.value = it
					onNameChanged(it)
				}, singleLine = true,
				modifier = Modifier
					.weight(0.7f),
				keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
				keyboardActions = KeyboardActions { focusManager.moveFocus(FocusDirection.Down) }
			)
		}
	}
}


@Preview
@Composable
private fun Preview() {
	AppTheme {
		RecipeEditIngredientList(4, listOf(IngredientEditModel(1).apply {
			name.value = "Ingredient"
			amount.value = "2"
		}, IngredientEditModel(2)))
	}
}