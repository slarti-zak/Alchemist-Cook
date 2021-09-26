package click.alchemist.cook.model

import com.couchbase.lite.Blob

class BlobModel(val blob: Blob) {
	val isEmpty: Boolean
		get() = blob.contentType.isEmpty()

	companion object {
		val empty = BlobModel(Blob("", ByteArray(0)))
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is BlobModel) return false

		val d1 = blob.digest()
		val d2 = other.blob.digest()

		if (d1 != null && d2 != null) return d1 == d2

		return blob.contentType == other.blob.contentType
				&& blob.length() == other.blob.length()
	}

	override fun hashCode(): Int {
		val digest = blob.digest()
		if (digest != null) return digest.hashCode()

		val prime = 31
		var result = 1
		result = prime * result + blob.length().hashCode()
		result = prime * result + blob.contentType.hashCode()
		return result
	}
}