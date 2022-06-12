package click.alchemist.cook.service.firestore

import click.alchemist.cook.extension.whereEqualTo
import click.alchemist.cook.model.firestore.Recipe
import com.couchbase.lite.Blob
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class RecipeFirestore(private val userFirestore: UserFirestore) {
	suspend fun load(id: String): Recipe? {
		return suspendCancellableCoroutine { cont ->
			val document = Firebase.firestore.collection("recipes").document(id)
			val task = document.get()
			task.addOnCompleteListener {
				if (it.isSuccessful) {
					val value = it.result
					if (value != null && value.exists()) {
						cont.resume(value.toObject<Recipe>())
					}
				} else {
					cont.resumeWithException(it.exception!!)
				}
			}
		}
	}

	suspend fun save(recipe: Recipe, blob: Blob?) = suspendCancellableCoroutine<String> { cont ->
		val collection = Firebase.firestore.collection("recipes")
		val document = recipe.id?.let { collection.document(it) } ?: collection.document()
		if (recipe.owner.isBlank()) FirebaseAuth.getInstance().currentUser?.let { recipe.owner = it.uid }
		try {
			val task = document.set(recipe)
			task.addOnCompleteListener {
				if (it.isSuccessful) {
					cont.resume(document.id)
				} else {
					cont.resumeWithException(it.exception!!)
				}
			}
		} catch (e: Exception) {
			cont.resumeWithException(e)
		}
	}

	fun observe(): Flow<List<Recipe>> = userFirestore.account.flatMapLatest {
		val uid = it?.uid ?: return@flatMapLatest flowOf(emptyList<Recipe>())
		callbackFlow {

			val document = Firebase.firestore.collection("recipes").whereEqualTo(Recipe::owner, uid)
			val token = document.addSnapshotListener { value, error ->
				if (error == null) {
					val recipes = value?.toObjects<Recipe>() ?: emptyList()
					trySend(recipes)
				}
			}
			awaitClose {
				token.remove()
			}
		}
	}
}