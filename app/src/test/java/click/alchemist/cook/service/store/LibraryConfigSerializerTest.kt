package click.alchemist.cook.service.store

import click.alchemist.cook.service.webdav.WebDavConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryConfigSerializerTest {

	@Test
	fun `blank or empty input deserializes to no libraries`() {
		assertTrue(LibraryConfigSerializer.deserialize("").isEmpty())
		assertTrue(LibraryConfigSerializer.deserialize(LibraryConfigSerializer.EMPTY).isEmpty())
	}

	@Test
	fun `a serialized library round-trips through every connection kind`() {
		val libraries = listOf(
			LibraryConfig("personal", "Personal", LibraryRole.PERSONAL, LibraryConnection.WebDav(WebDavConfig("https://dav.example.com", "me", "secret"))),
			LibraryConfig("family", "Family", LibraryRole.SHARED, LibraryConnection.Nextcloud(WebDavConfig("https://cloud.example.com/remote.php/dav/files/family", "family", "token"), "https://cloud.example.com")),
			LibraryConfig("camping", "Camping", LibraryRole.SHARED, LibraryConnection.LocalFolder("content://example/tree/camping", "Camping"))
		)

		val roundTripped = LibraryConfigSerializer.deserialize(LibraryConfigSerializer.serialize(libraries))

		assertEquals(libraries, roundTripped)
	}

	@Test
	fun `a legacy flat-WebDavConfig library shape migrates to LibraryConnection_WebDav instead of being wiped`() {
		val legacyYaml = """
			- id: "personal"
			  label: "Personal"
			  role: "PERSONAL"
			  webDav:
			    baseUrl: "https://dav.example.com"
			    username: "me"
			    password: "secret"
			- id: "family"
			  label: "Family"
			  role: "SHARED"
			  webDav:
			    baseUrl: "https://cloud.example.com/dav"
			    username: "family"
			    password: ""
		""".trimIndent()

		val libraries = LibraryConfigSerializer.deserialize(legacyYaml)

		assertEquals(
			listOf(
				LibraryConfig("personal", "Personal", LibraryRole.PERSONAL, LibraryConnection.WebDav(WebDavConfig("https://dav.example.com", "me", "secret"))),
				LibraryConfig("family", "Family", LibraryRole.SHARED, LibraryConnection.WebDav(WebDavConfig("https://cloud.example.com/dav", "family", "")))
			),
			libraries
		)
	}

	@Test
	fun `unparseable input resets to no libraries rather than throwing`() {
		assertTrue(LibraryConfigSerializer.deserialize("not: [valid, at, all: -").isEmpty())
	}
}
