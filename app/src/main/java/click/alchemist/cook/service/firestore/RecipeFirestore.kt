package click.alchemist.cook.service.firestore

import click.alchemist.cook.model.firestore.Recipe
import com.couchbase.lite.Blob
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class RecipeFirestore {
	fun load(id: String) =
		callbackFlow<Recipe?> {
			val document = Firebase.firestore.collection("recipes").document(id)
			val task = document.get()
			task.addOnCompleteListener {
				if (it.isSuccessful) {
					val value = it.result
					if (value != null && value.exists()) {
						trySend(value.toObject<Recipe>())
					}
				}
				close(it.exception)
			}
		}

	suspend fun save(recipe: Recipe, blob: Blob?) = suspendCancellableCoroutine<String> { cont ->
		val collection = Firebase.firestore.collection("recipes")
		val document = if (recipe.id.isBlank()) collection.document() else collection.document(recipe.id)
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
}