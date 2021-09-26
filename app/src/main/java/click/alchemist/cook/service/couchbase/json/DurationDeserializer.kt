package click.alchemist.cook.service.couchbase.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.io.IOException

class DurationDeserializer : StdDeserializer<Double>(Double::class.java) {

	@Throws(IOException::class, JsonProcessingException::class)
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Double {
		val value = p.doubleValue
		return if (value == Double.MAX_VALUE) Double.POSITIVE_INFINITY else value
	}
}