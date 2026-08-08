package click.alchemist.cook.service.serialization

import click.alchemist.cook.model.DbDuration
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.IOException
import kotlin.time.DurationUnit

class DbDurationSerializer : StdSerializer<DbDuration>(DbDuration::class.java) {
	@Throws(IOException::class)
	override fun serialize(
		value: DbDuration,
		jsonGenerator: JsonGenerator,
		serializer: SerializerProvider
	) {
		val ms = value.dbDuration.toDouble(DurationUnit.MILLISECONDS)
		if (ms == Double.POSITIVE_INFINITY) {
			jsonGenerator.writeNumber(Double.MAX_VALUE)
		} else {
			jsonGenerator.writeNumber(ms)
		}
	}
}