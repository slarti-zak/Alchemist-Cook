package click.alchemist.cook.extension

import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.reflect.KProperty1

fun CollectionReference.whereEqualTo(property: KProperty1<*, String>, comparand: String?): Query {
	return whereEqualTo(property.name, comparand)
}

fun <T> Query.asFlow(successConverter: (QuerySnapshot) -> T) = callbackFlow {
	val token = addSnapshotListener { value, error ->
		if (error == null && value != null) {
			trySend(successConverter(value))
		}
	}
	awaitClose {
		token.remove()
	}
}

inline fun <reified T : Any> Query.asFlow() = asFlow { it.toObjects<T>() }

fun <T> DocumentReference.asFlow(successConverter: (DocumentSnapshot) -> T?): Flow<T> = callbackFlow {
	val token = addSnapshotListener { value, error ->
		if (error == null && value != null) {
			val converted = successConverter(value)
			if (converted != null) {
				trySend(converted)
			}
		}
	}
	awaitClose {
		token.remove()
	}
}

inline fun <reified T> DocumentReference.asFlow(): Flow<T> = asFlow { it.toObject() }