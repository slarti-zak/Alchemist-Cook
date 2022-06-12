package click.alchemist.cook.service.firestore

import click.alchemist.cook.model.firestore.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class UserFirestore {
	val account: Flow<FirebaseUser?> = callbackFlow {
		val listener = FirebaseAuth.AuthStateListener {
			this.trySend(it.currentUser)
		}
		FirebaseAuth.getInstance().addAuthStateListener(listener)
		awaitClose {
			FirebaseAuth.getInstance().removeAuthStateListener(listener)
		}
	}

	val user: Flow<User?> = account.flatMapLatest {
		if (it == null) {
			flowOf(null)
		} else {
			callbackFlow {
				val document = Firebase.firestore.collection("users")
					.document(it.uid)
				val token = document.addSnapshotListener { value, error ->
					val user = value?.toObject<User>()
					trySend(user)
				}
				awaitClose {
					token.remove()
				}
			}
		}
	}
}