package click.alchemist.cook.service.nextcloud

import click.alchemist.cook.logDebug
import click.alchemist.cook.service.webdav.WebDavConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/** What [NextcloudLoginFlow.start] gets back: where to send the user, and how to poll for the result. */
data class LoginFlowInit(val loginUrl: String, val pollEndpoint: String, val token: String)

/** What a completed login hands back — a scoped app-password, never the user's real account password. */
data class NextcloudCredentials(val server: String, val loginName: String, val appPassword: String) {
	/** `remote.php/dav/files/<user>` is Nextcloud's standard per-user WebDAV root. */
	fun toWebDavConfig(): WebDavConfig = WebDavConfig(
		baseUrl = "${server.trimEnd('/')}/remote.php/dav/files/$loginName",
		username = loginName,
		password = appPassword
	)
}

class NextcloudLoginException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Nextcloud's "Login Flow v2": the user is sent to their own server's login page (in a browser, not
 * a form in this app, so 2FA/SSO/etc. just work and this app never sees the real password), and this
 * polls a token-scoped endpoint until the server confirms the login and hands back an app-password.
 * See https://docs.nextcloud.com/server/latest/developer_manual/client_apis/LoginFlow/index.html#login-flow-v2.
 */
class NextcloudLoginFlow(private val httpClient: OkHttpClient = defaultHttpClient) {

	suspend fun start(serverUrl: String): LoginFlowInit = withContext(Dispatchers.IO) {
		val request = Request.Builder()
			.url("${normalize(serverUrl)}/index.php/login/v2")
			.post(ByteArray(0).toRequestBody(null))
			.build()

		execute(request) { response ->
			requireSuccess(response, "Starting Nextcloud login")
			val json = mapper.readTree(response.body?.bytes() ?: ByteArray(0))
			LoginFlowInit(
				loginUrl = json["login"].asText(),
				pollEndpoint = json["poll"]["endpoint"].asText(),
				token = json["poll"]["token"].asText()
			)
		}
	}

	/** One poll attempt; null means the user hasn't finished logging in yet — call again. */
	suspend fun poll(init: LoginFlowInit): NextcloudCredentials? = withContext(Dispatchers.IO) {
		val body = "token=${init.token}".toRequestBody("application/x-www-form-urlencoded".toMediaType())
		val request = Request.Builder().url(init.pollEndpoint).post(body).build()

		execute(request) { response ->
			if (response.code == 404) return@execute null
			requireSuccess(response, "Polling Nextcloud login")
			val json = mapper.readTree(response.body?.bytes() ?: ByteArray(0))
			NextcloudCredentials(
				server = json["server"].asText(),
				loginName = json["loginName"].asText(),
				appPassword = json["appPassword"].asText()
			)
		}
	}

	private fun normalize(input: String): String {
		val trimmed = input.trim().trimEnd('/')
		return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
	}

	private fun <T> execute(request: Request, block: (Response) -> T): T {
		logDebug(TAG, "${request.method} ${request.url}")
		return httpClient.newCall(request).execute().use(block)
	}

	private fun requireSuccess(response: Response, description: String) {
		if (!response.isSuccessful) throw NextcloudLoginException("$description failed: HTTP ${response.code}")
	}

	companion object {
		private const val TAG = "NextcloudLoginFlow"

		private val mapper = ObjectMapper().registerKotlinModule()

		private val defaultHttpClient = OkHttpClient.Builder()
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.writeTimeout(30, TimeUnit.SECONDS)
			.build()
	}
}
