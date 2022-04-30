package click.alchemist.cook.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

@Parcelize
data class DbDuration(val dbDuration: Duration) : Parcelable, Comparable<DbDuration> {
	override operator fun compareTo(other: DbDuration): Int {
		return dbDuration.compareTo(other.dbDuration)
	}

	companion object : Parceler<DbDuration> {
		val INFINITE: DbDuration = DbDuration(Duration.INFINITE)
		val ZERO: DbDuration = DbDuration(Duration.ZERO)

		override fun DbDuration.write(parcel: Parcel, flags: Int) {
			parcel.writeLong(dbDuration.inWholeNanoseconds)
		}

		override fun create(parcel: Parcel): DbDuration {
			val nanos = parcel.readLong()
			return DbDuration(nanos.nanoseconds)
		}
	}
}