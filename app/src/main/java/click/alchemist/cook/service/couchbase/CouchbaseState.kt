package click.alchemist.cook.service.couchbase

import com.couchbase.lite.ReplicatorStatus

sealed class CouchbaseState {

	class AccountState(val status: ReplicatorStatus) : CouchbaseState()
	class GuestState : CouchbaseState()

	companion object {
		fun account(status: ReplicatorStatus): CouchbaseState {
			return AccountState(status)
		}

		fun guest(): CouchbaseState {
			return GuestState()
		}
	}
}