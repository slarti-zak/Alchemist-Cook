package click.alchemist.cook.service.store

import click.alchemist.cook.model.DbDuration
import click.alchemist.cook.service.serialization.BigDecimalDeserializer
import click.alchemist.cook.service.serialization.BigDecimalSerializer
import click.alchemist.cook.service.serialization.DbDurationDeserializer
import click.alchemist.cook.service.serialization.DbDurationSerializer
import click.alchemist.cook.service.serialization.DurationDeserializer
import click.alchemist.cook.service.serialization.DurationSerializer
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.math.BigDecimal

/**
 * Shared YAML mapper for the WebDAV file store. Front-matter delimiters are written by hand
 * (see [RecipeFileFormat]), so the automatic "---" document-start marker is disabled to avoid
 * doubling up. Uses the same custom (de)serializers as the rest of the app for
 * [BigDecimal]/[DbDuration]/duration fields, so the on-disk representation stays consistent.
 */
internal object YamlMapper {
	val instance: ObjectMapper = ObjectMapper(
		YAMLFactory()
			.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
			.disable(YAMLGenerator.Feature.SPLIT_LINES)
	).apply {
		registerKotlinModule()
		configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
		configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)

		registerModule(SimpleModule().apply {
			addSerializer(BigDecimal::class.java, BigDecimalSerializer())
			addDeserializer(BigDecimal::class.java, BigDecimalDeserializer())

			addSerializer(Double::class.java, DurationSerializer())
			addDeserializer(Double::class.java, DurationDeserializer())

			addSerializer(DbDuration::class.java, DbDurationSerializer())
			addDeserializer(DbDuration::class.java, DbDurationDeserializer())
		})
	}
}
