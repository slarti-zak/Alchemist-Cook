package click.alchemist.cook.service.store

import click.alchemist.cook.model.DbDuration
import click.alchemist.cook.service.couchbase.json.BigDecimalDeserializer
import click.alchemist.cook.service.couchbase.json.BigDecimalSerializer
import click.alchemist.cook.service.couchbase.json.DbDurationDeserializer
import click.alchemist.cook.service.couchbase.json.DbDurationSerializer
import click.alchemist.cook.service.couchbase.json.DurationDeserializer
import click.alchemist.cook.service.couchbase.json.DurationSerializer
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
 * doubling up. Reuses the same custom (de)serializers the Couchbase mapper used, so the on-disk
 * representation of [BigDecimal]/[DbDuration]/duration fields matches what shipped before.
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
