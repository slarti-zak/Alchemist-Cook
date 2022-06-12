package click.alchemist.cook.extension

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlin.reflect.KProperty1

fun CollectionReference.whereEqualTo(property: KProperty1<*, String>, comparand: String?): Query {
	return whereEqualTo(property.name, comparand)
}
