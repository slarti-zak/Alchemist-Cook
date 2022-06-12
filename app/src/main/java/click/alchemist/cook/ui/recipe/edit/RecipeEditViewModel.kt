package click.alchemist.cook.ui.recipe.edit

import android.graphics.Bitmap
import click.alchemist.cook.extension.MimeType
import click.alchemist.cook.extension.isNotNullOrBlank
import click.alchemist.cook.extension.scaledBitmap
import click.alchemist.cook.extension.tryParse
import click.alchemist.cook.model.*
import click.alchemist.cook.model.firestore.Recipe
import click.alchemist.cook.service.firestore.RecipeFirestore
import click.alchemist.cook.ui.BaseViewModel
import click.alchemist.cook.viewmodel.IngredientEditModel
import click.alchemist.cook.viewmodel.RecipeGraphModel
import click.alchemist.cook.viewmodel.RecipeGraphNodeModel
import com.couchbase.lite.Blob
import kotlinx.coroutines.flow.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.math.BigDecimal


class RecipeEditViewModel(private val recipeStore: RecipeFirestore) : BaseViewModel() {
	private var indexCounter = 0
		get() = field++

	private var originalRecipe: Recipe? = null

	private val _title = MutableStateFlow("")
	val title: MutableStateFlow<String> = _title

	private val _content = MutableStateFlow("")
	val content: MutableStateFlow<String> = _content

	private val _serves = MutableStateFlow(4)
	val serves: MutableStateFlow<Int> = _serves

	private val _ingredients = MutableStateFlow<List<IngredientEditModel>>(emptyList())
	val ingredients: StateFlow<List<IngredientEditModel>> = _ingredients

	private val _extraInstructions = MutableStateFlow(RecipeGraphModel(emptyList(), true))
	val extraInstructions: StateFlow<RecipeGraphModel> = _extraInstructions

	private val _image = MutableStateFlow(BlobModel.empty)
	val image: StateFlow<BlobModel> = _image

//	private val _scrollToBottom = MutableLiveData<Unit>()
//	val scrollToBottom: LiveData<Unit> = _scrollToBottom

	var isNewRecipe: Boolean = false

	suspend fun load(recipeId: String?) {
		if (originalRecipe != null) return

		isNewRecipe = recipeId == null

		val storedRecipe = loadRecipe(recipeId).firstOrNull()
		if (storedRecipe != null) {
			_title.value = storedRecipe.name
			_content.value = storedRecipe.content
			_serves.value = storedRecipe.serves.let { if (it > 0) it else 4 }
//			_extraInstructions.value = RecipeGraphModel.fromNodes("", storedRecipe.extendedContent?.nodes)
//			_image.emit(recipeStore.loadImage(storedRecipe))
		}
//		_ingredients.value = getIngredientsToLoad(storedRecipe?.ingredients)
		originalRecipe = storedRecipe
	}

	private fun loadRecipe(recipeId: String?): Flow<Recipe?> = if (recipeId == null) emptyFlow() else recipeStore.load(recipeId)

	suspend fun save(): Result<String> {
		val recipe = Recipe(
			_title.value,
			_content.value,
//			getIngredientsToSave(),
			serves.value,
//			extendedContent = getGraphToSave(),
			id = originalRecipe?.id ?: ""
		)

		val image = _image.value
		return kotlin.runCatching { recipeStore.save(recipe, if (image.isEmpty) null else image.blob) }
	}

	fun applyImage(image: () -> InputStream?) {
		val imageBlob = getImageToSave(image) ?: return
		_image.value = BlobModel(imageBlob)
	}

	private fun getImageToSave(image: () -> InputStream?): Blob? {
		val scaledImage = image.scaledBitmap(1024, 1024) ?: return null

		val stream = ByteArrayOutputStream()
		scaledImage.compress(Bitmap.CompressFormat.JPEG, 75, stream)
		return Blob(MimeType.Jpg, stream.toByteArray()).also {
			scaledImage.recycle()
		}
	}

	fun moveIngredientItem(from: Int, to: Int) {
		val newList = _ingredients.value.toMutableList()
		val fromItem = newList.removeAt(from)
		newList.add(to, fromItem)
		_ingredients.value = newList
	}

	fun deleteIngredientItem(item: IngredientEditModel) {
		val list = _ingredients.value

		val newList = list.toMutableList()
		if (!newList.remove(item)) return
		if (newList.isEmpty() || newList.last().name.value.isNotNullOrBlank()) {
			newList.add(createEmptyIngredient())
		}

		_ingredients.value = newList
	}

	fun ensureEmptyLastElement() {
		val list = _ingredients.value
		if (list.isEmpty()) return

		if (list.last().name.value.isNotNullOrBlank()) {
			_ingredients.value = list.plus(createEmptyIngredient())
		}
	}

	fun ensureLastEntryIsCreationEntry() {
		val list = _ingredients.value
		if (list.lastOrNull()?.name?.value.isNotNullOrBlank()) {
//			_scrollToBottom.value = Unit
			_ingredients.value = list.plus(createEmptyIngredient())
		}
	}

	private fun getIngredientsToLoad(ingredients: List<Ingredient>?): List<IngredientEditModel> {
		if (ingredients == null) return listOf(createEmptyIngredient())

		val ingredientList = ingredients.map {
			createEmptyIngredient().apply {
				name.value = it.name
				amount.value = it.amount.toString()
				unit.value = it.unitCategory.base()
			}
		}

		return ingredientList.plus(createEmptyIngredient())
	}

	private fun createEmptyIngredient() = IngredientEditModel(indexCounter)

	private fun getIngredientsToSave(): List<Ingredient> {
		return _ingredients.value.asSequence()
			.filter { it.name.value.isNotNullOrBlank() }
			.map {
				val amount = tryParse(it.amount.value, BigDecimal.ONE)
				Ingredient(it.name.value, amount, it.unit.value)
			}
			.toList()
	}

	private fun getGraphToSave(): RecipeGraph? {
		val nodes = _extraInstructions.value.nodes.map { it.node }
		if (nodes.isEmpty()) return null

		return RecipeGraph(nodes)
	}

	fun addExtendedEntry(node: RecipeGraphNode) {
		val newList = _extraInstructions.value.nodes.map { it.node }.toMutableList()
		val existingId = newList.indexOfFirst { it.id == node.id }
		if (existingId < 0) {
			newList.add(node)
		} else {
			newList[existingId] = node
		}
		_extraInstructions.value = RecipeGraphModel.fromNodes("", newList)
	}

	fun deleteExtraInstruction(toRemove: RecipeGraphNodeModel) {
		val list = _extraInstructions.value.nodes

		val mutable = list.toMutableList()
		if (!mutable.removeAll { node -> toRemove.node.id == node.node.id }) {
			return
		}
		val result = mutable.map {
			it.node.copy(dependencies = it.node.dependencies.filter { dependencyId -> dependencyId != toRemove.node.id })
		}
		_extraInstructions.value = RecipeGraphModel.fromNodes("", result)
	}

	fun onServingsChanged(newServings: Int) {
		_serves.tryEmit(newServings)
	}
}
