package click.alchemist.cook.service.couchbase.json

import click.alchemist.cook.model.DbDuration
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.milliseconds

class DbDurationDeserializer : StdDeserializer<DbDuration>(DbDuration::class.java) {

	@Throws(IOException::class, JsonProcessingException::class)
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DbDuration {
		val value = p.doubleValue
		return if (value == Double.MAX_VALUE)
			DbDuration(Duration.INFINITE)
		else
			DbDuration(value.milliseconds)
	}
}