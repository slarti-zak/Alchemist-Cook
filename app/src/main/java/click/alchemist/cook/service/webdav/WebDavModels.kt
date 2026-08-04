package click.alchemist.cook.service.webdav

data class WebDavConfig(
	val baseUrl: String,
	val username: String,
	val password: String
)

data class WebDavResource(
	val path: String,
	val isCollection: Boolean,
	val etag: String?,
	val lastModified: Long?,
	val contentLength: Long?
)

class WebDavException(message: String, val statusCode: Int? = null, cause: Throwable? = null) :
	Exception(message, cause)
