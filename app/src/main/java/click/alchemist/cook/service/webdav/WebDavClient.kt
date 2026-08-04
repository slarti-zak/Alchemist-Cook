package click.alchemist.cook.service.webdav

import click.alchemist.cook.logDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Minimal WebDAV client covering the operations a folder/file sync engine needs:
 * listing a directory (PROPFIND), reading/writing files (GET/PUT), removing entries
 * (DELETE) and creating directories (MKCOL). Paths are always relative to [WebDavConfig.baseUrl]
 * and use forward slashes, without a leading slash (e.g. "recipes/pasta/recipe.md").
 */
class WebDavClient(
	private val config: WebDavConfig,
	private val httpClient: OkHttpClient = defaultHttpClient
) {
	private val baseUrl = config.baseUrl.trimEnd('/').toHttpUrl()
	private val basePath = baseUrl.encodedPath
	private val authHeader = Credentials.basic(config.username, config.password)

	suspend fun propfind(path: String, depth: Int = 1): List<WebDavResource> = withContext(Dispatchers.IO) {
		val request = requestBuilder(path)
			.method("PROPFIND", WebDavMultistatus.PROPFIND_BODY.toRequestBody("application/xml".toMediaType()))
			.header("Depth", depth.toString())
			.build()

		execute(request) { response ->
			if (response.code == 404) return@execute emptyList()
			requireSuccess(response, "PROPFIND $path")
			WebDavMultistatus.parse(response.body?.bytes() ?: ByteArray(0), path, basePath)
		}
	}

	/** Walks every collection under [path] (default: the library root) and returns all resources found. */
	suspend fun propfindRecursive(path: String = ""): List<WebDavResource> {
		val result = mutableListOf<WebDavResource>()
		val queue = ArrayDeque<String>()
		queue.add(path)
		while (queue.isNotEmpty()) {
			val current = queue.removeFirst()
			for (child in propfind(current, depth = 1)) {
				result.add(child)
				if (child.isCollection) queue.add(child.path)
			}
		}
		return result
	}

	suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
		val request = requestBuilder(path)
			.method("PROPFIND", WebDavMultistatus.PROPFIND_BODY.toRequestBody("application/xml".toMediaType()))
			.header("Depth", "0")
			.build()

		execute(request) { response -> response.code != 404 }
	}

	suspend fun get(path: String): ByteArray = withContext(Dispatchers.IO) {
		val request = requestBuilder(path).get().build()
		execute(request) { response ->
			requireSuccess(response, "GET $path")
			response.body?.bytes() ?: ByteArray(0)
		}
	}

	suspend fun put(path: String, content: ByteArray, contentType: String = "application/octet-stream") =
		withContext(Dispatchers.IO) {
			val request = requestBuilder(path)
				.put(content.toRequestBody(contentType.toMediaType()))
				.build()
			execute(request) { response -> requireSuccess(response, "PUT $path") }
		}

	suspend fun delete(path: String) = withContext(Dispatchers.IO) {
		val request = requestBuilder(path).delete().build()
		execute(request) { response ->
			if (response.code == 404) return@execute
			requireSuccess(response, "DELETE $path")
		}
	}

	/** Creates the directory at [path]. Assumes the parent already exists; see [mkcolRecursive]. */
	suspend fun mkcol(path: String) = withContext(Dispatchers.IO) {
		val request = requestBuilder(path).method("MKCOL", null).build()
		execute(request) { response ->
			// 405 = already exists, which is fine for our idempotent "ensure directory" use case.
			if (response.code == 405) return@execute
			requireSuccess(response, "MKCOL $path")
		}
	}

	/** Ensures every path segment of [path] exists as a collection, creating any that are missing. */
	suspend fun mkcolRecursive(path: String) {
		val segments = path.trim('/').split('/').filter { it.isNotBlank() }
		var current = ""
		for (segment in segments) {
			current = if (current.isEmpty()) segment else "$current/$segment"
			if (!exists(current)) {
				mkcol(current)
			}
		}
	}

	private fun requestBuilder(path: String): Request.Builder {
		val url = baseUrl.newBuilder().apply {
			path.trim('/').split('/').filter { it.isNotBlank() }.forEach { addPathSegment(it) }
		}.build()

		return Request.Builder()
			.url(url)
			.header("Authorization", authHeader)
	}

	private fun <T> execute(request: Request, block: (Response) -> T): T {
		logDebug(TAG, "${request.method} ${request.url}")
		return httpClient.newCall(request).execute().use(block)
	}

	private fun requireSuccess(response: Response, description: String) {
		if (!response.isSuccessful) {
			throw WebDavException("$description failed: HTTP ${response.code}", response.code)
		}
	}

	companion object {
		private const val TAG = "WebDavClient"

		private val defaultHttpClient = OkHttpClient.Builder()
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS)
			.writeTimeout(60, TimeUnit.SECONDS)
			.build()
	}
}
