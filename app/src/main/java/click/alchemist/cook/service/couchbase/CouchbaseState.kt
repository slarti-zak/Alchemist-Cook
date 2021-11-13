package click.alchemist.cook.service.couchbase

import com.couchbase.lite.AbstractReplicator

sealed class CouchbaseState {

	class AccountState(val status: AbstractReplicator.Status) : CouchbaseState()
	class GuestState : CouchbaseState()

	companion object {
		fun account(status: AbstractReplicator.Status): CouchbaseState {
			return AccountState(status)
		}

		fun guest(): CouchbaseState {
			return GuestState()
		}
	}
}