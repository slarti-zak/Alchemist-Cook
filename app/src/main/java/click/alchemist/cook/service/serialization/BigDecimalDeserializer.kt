package click.alchemist.cook.service.serialization

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.io.IOException
import java.math.BigDecimal

class BigDecimalDeserializer : StdDeserializer<BigDecimal>(BigDecimal::class.java) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BigDecimal {
        val s = p.readValueAs(String::class.java)
        return BigDecimal(s)
    }
}