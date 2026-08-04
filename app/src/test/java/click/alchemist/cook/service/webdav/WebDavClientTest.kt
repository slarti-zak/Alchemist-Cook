package click.alchemist.cook.service.webdav

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebDavClientTest {

	private lateinit var server: MockWebServer
	private lateinit var client: WebDavClient

	@Before
	fun setUp() {
		server = MockWebServer()
		server.start()
		client = WebDavClient(WebDavConfig(server.url("/").toString(), "user", "pass"))
	}

	@After
	fun tearDown() {
		server.shutdown()
	}

	private val multistatusBody = """
		<?xml version="1.0" encoding="utf-8"?>
		<D:multistatus xmlns:D="DAV:">
			<D:response>
				<D:href>/recipes/</D:href>
				<D:propstat>
					<D:prop>
						<D:resourcetype><D:collection/></D:resourcetype>
					</D:prop>
					<D:status>HTTP/1.1 200 OK</D:status>
				</D:propstat>
			</D:response>
			<D:response>
				<D:href>/recipes/pasta/recipe.md</D:href>
				<D:propstat>
					<D:prop>
						<D:getetag>"abc123"</D:getetag>
						<D:getlastmodified>Mon, 12 Jan 2024 10:00:00 GMT</D:getlastmodified>
						<D:getcontentlength>42</D:getcontentlength>
						<D:resourcetype/>
					</D:prop>
					<D:status>HTTP/1.1 200 OK</D:status>
				</D:propstat>
			</D:response>
		</D:multistatus>
	""".trimIndent()

	@Test
	fun `propfind parses collections and files, skipping the queried resource itself`() = runTest {
		server.enqueue(MockResponse().setResponseCode(207).setBody(multistatusBody))

		val resources = client.propfind("recipes")

		assertEquals(1, resources.size)
		val file = resources[0]
		assertEquals("recipes/pasta/recipe.md", file.path)
		assertEquals(false, file.isCollection)
		assertEquals("abc123", file.etag)
		assertEquals(42L, file.contentLength)
	}

	@Test
	fun `propfind returns empty list on 404 instead of throwing`() = runTest {
		server.enqueue(MockResponse().setResponseCode(404))

		val resources = client.propfind("does-not-exist")

		assertTrue(resources.isEmpty())
	}

	@Test
	fun `get returns response body bytes`() = runTest {
		server.enqueue(MockResponse().setResponseCode(200).setBody("hello world"))

		val bytes = client.get("recipes/pasta/recipe.md")

		assertEquals("hello world", String(bytes))
	}

	@Test
	fun `put sends request body and succeeds on 2xx`() = runTest {
		server.enqueue(MockResponse().setResponseCode(201))

		client.put("recipes/pasta/recipe.md", "content".toByteArray())

		val recorded = server.takeRequest()
		assertEquals("PUT", recorded.method)
		assertEquals("content", recorded.body.readUtf8())
	}

	@Test(expected = WebDavException::class)
	fun `put throws on non-2xx response`() = runTest {
		server.enqueue(MockResponse().setResponseCode(500))

		client.put("recipes/pasta/recipe.md", "content".toByteArray())
	}

	@Test
	fun `delete treats 404 as already-deleted, not an error`() = runTest {
		server.enqueue(MockResponse().setResponseCode(404))

		client.delete("recipes/pasta/recipe.md")
	}
}
