package click.alchemist.cook.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration

@Parcelize
data class DbDuration(val dbDuration: Duration) : Parcelable, Comparable<DbDuration> {
	override operator fun compareTo(other: DbDuration): Int {
		return dbDuration.compareTo(other.dbDuration)
	}

	companion object {
		val INFINITE: DbDuration = DbDuration(Duration.INFINITE)
		val ZERO: DbDuration = DbDuration(Duration.ZERO)
	}
}