package click.alchemist.cook.service.firestore

import click.alchemist.cook.extension.asFlow
import click.alchemist.cook.extension.whereEqualTo
import click.alchemist.cook.model.firestore.Recipe
import com.couchbase.lite.Blob
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await


class RecipeFirestore(private val userFirestore: UserFirestore) {
	suspend fun load(id: String): Recipe? {
		val document = Firebase.firestore.collection("recipes").document(id)
		val loaded = document.get().await()
		return loaded.toObject<Recipe>()
	}

	suspend fun save(recipe: Recipe, blob: Blob?): String {
		val collection = Firebase.firestore.collection("recipes")
		val document = recipe.id?.let { collection.document(it) } ?: collection.document()
		if (recipe.owner.isBlank()) FirebaseAuth.getInstance().currentUser?.let { recipe.owner = it.uid }
		document.set(recipe).await()
		return document.id
	}

	fun observe(id: String): Flow<Recipe> {
		val document = Firebase.firestore.collection("recipes").document(id)
		return document.asFlow()
	}

	fun observe() = userFirestore.account.flatMapLatest { user ->
		val uid = user?.uid ?: return@flatMapLatest flowOf(emptyList())
		val document = Firebase.firestore.collection("recipes").whereEqualTo(Recipe::owner, uid)
		document.asFlow<Recipe>()
	}

	suspend fun delete(id: String) {
		val document = Firebase.firestore.collection("recipes").document(id)
		document.delete().await()
	}
}