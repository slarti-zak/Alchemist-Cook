package click.alchemist.cook.service.couchbase.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.IOException

class DurationSerializer : StdSerializer<Double>(Double::class.java) {
	@Throws(IOException::class)
	override fun serialize(
		value: Double,
		jsonGenerator: JsonGenerator,
		serializer: SerializerProvider
	) {
		if (value == Double.POSITIVE_INFINITY) {
			jsonGenerator.writeNumber(Double.MAX_VALUE)
		} else {
			jsonGenerator.writeNumber(value)
		}
	}
}