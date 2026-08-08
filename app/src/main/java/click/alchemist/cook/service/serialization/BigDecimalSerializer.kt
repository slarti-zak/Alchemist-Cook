package click.alchemist.cook.service.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.IOException
import java.math.BigDecimal

class BigDecimalSerializer : StdSerializer<BigDecimal>(BigDecimal::class.java) {
    @Throws(IOException::class)
    override fun serialize(
        car: BigDecimal,
        jsonGenerator: JsonGenerator,
        serializer: SerializerProvider
    ) {
        jsonGenerator.writeString(car.toString())
    }
}