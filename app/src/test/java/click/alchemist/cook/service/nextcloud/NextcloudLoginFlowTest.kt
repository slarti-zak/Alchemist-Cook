package click.alchemist.cook.service.nextcloud

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NextcloudLoginFlowTest {

	private lateinit var server: MockWebServer
	private lateinit var flow: NextcloudLoginFlow

	@Before
	fun setUp() {
		server = MockWebServer()
		server.start()
		flow = NextcloudLoginFlow()
	}

	@After
	fun tearDown() {
		server.shutdown()
	}

	@Test
	fun `start posts to login v2 and parses the login and poll URLs`() = runTest {
		server.enqueue(
			MockResponse().setResponseCode(200).setBody(
				"""{"poll": {"token": "the-token", "endpoint": "${server.url("/login/v2/poll")}"}, "login": "${server.url("/login/v2/flow/the-token")}"}"""
			)
		)

		val init = flow.start(server.url("/").toString())

		val recorded = server.takeRequest()
		assertEquals("POST", recorded.method)
		assertTrue(recorded.path!!.endsWith("/index.php/login/v2"))
		// So the resulting app password shows up in Nextcloud's security settings as this app, not
		// as OkHttp's generic default.
		assertTrue(recorded.getHeader("User-Agent")!!.startsWith("AlchemistCook/"))
		assertEquals(server.url("/").toString().trimEnd('/'), init.serverUrl)
		assertEquals("the-token", init.token)
		assertEquals(server.url("/login/v2/poll").toString(), init.pollEndpoint)
		assertEquals(server.url("/login/v2/flow/the-token").toString(), init.loginUrl)
	}

	@Test
	fun `poll returns null on 404, meaning the user hasn't finished logging in yet`() = runTest {
		server.enqueue(MockResponse().setResponseCode(404))

		val credentials = flow.poll(loginFlowInit())

		assertNull(credentials)
	}

	@Test
	fun `poll parses server, loginName and appPassword once the login completes`() = runTest {
		server.enqueue(
			MockResponse().setResponseCode(200).setBody(
				"""{"server": "https://cloud.example.com", "loginName": "alice", "appPassword": "secret-app-password"}"""
			)
		)

		val credentials = flow.poll(loginFlowInit())

		assertEquals(NextcloudCredentials("https://cloud.example.com", "alice", "secret-app-password"), credentials)

		val recorded = server.takeRequest()
		assertEquals("POST", recorded.method)
		assertTrue(recorded.body.readUtf8().contains("token=tok"))
	}

	@Test(expected = NextcloudLoginException::class)
	fun `poll throws on a non-404 error response`() = runTest {
		server.enqueue(MockResponse().setResponseCode(500))

		flow.poll(loginFlowInit())
	}

	private fun loginFlowInit() = LoginFlowInit(
		serverUrl = server.url("/").toString().trimEnd('/'),
		loginUrl = "irrelevant",
		pollEndpoint = server.url("/poll").toString(),
		token = "tok"
	)

	@Test
	fun `credentials derive the per-user WebDAV root`() {
		val webDavConfig = NextcloudCredentials("https://cloud.example.com/", "alice", "secret").toWebDavConfig()

		assertEquals("https://cloud.example.com/remote.php/dav/files/alice", webDavConfig.baseUrl)
		assertEquals("alice", webDavConfig.username)
		assertEquals("secret", webDavConfig.password)
	}
}
