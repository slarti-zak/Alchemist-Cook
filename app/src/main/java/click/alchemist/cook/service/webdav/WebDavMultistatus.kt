package click.alchemist.cook.service.webdav

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

private const val DAV_NS = "DAV:"

internal object WebDavMultistatus {
	val PROPFIND_BODY = """
		<?xml version="1.0" encoding="utf-8" ?>
		<D:propfind xmlns:D="DAV:">
			<D:prop>
				<D:getetag/>
				<D:getlastmodified/>
				<D:resourcetype/>
				<D:getcontentlength/>
			</D:prop>
		</D:propfind>
	""".trimIndent()

	// RFC 1123 date format used by the WebDAV/HTTP "Last-Modified" property.
	private val httpDateFormat = ThreadLocal.withInitial {
		SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
	}

	/**
	 * @param basePath the WebDAV connection's own root path (e.g. Nextcloud's
	 * "/remote.php/dav/files/user"), so each response's absolute `href` can be turned into a path
	 * relative to that root — the same space [WebDavClient] callers, [requestPath], and everything
	 * downstream (local mirror, Room index) operate in. Without this, hrefs are absolute server
	 * paths and never match up with our relative paths, so nothing looks like it exists remotely.
	 */
	fun parse(xml: ByteArray, requestPath: String, basePath: String): List<WebDavResource> {
		val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
		val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(xml))

		val responses = document.getElementsByTagNameNS(DAV_NS, "response")
		val resources = mutableListOf<WebDavResource>()
		val normalizedBasePath = basePath.trim('/')

		for (i in 0 until responses.length) {
			val response = responses.item(i) as? Element ?: continue
			val href = childText(response, "href") ?: continue
			val relativePath = relativeToBase(href, normalizedBasePath)

			val prop = firstDescendant(response, "prop") ?: continue
			val isCollection = firstDescendant(prop, "resourcetype")
				?.let { firstDescendant(it, "collection") != null } ?: false
			val etag = childText(prop, "getetag")?.trim('"')
			val lastModified = childText(prop, "getlastmodified")?.let(::parseHttpDate)
			val contentLength = childText(prop, "getcontentlength")?.toLongOrNull()

			// Skip the entry for the collection being queried itself; callers only want children.
			if (relativePath == requestPath.trim('/')) continue

			resources.add(WebDavResource(relativePath, isCollection, etag, lastModified, contentLength))
		}

		return resources
	}

	/** Strips [basePath] (still percent-encoded, matching [href]) before decoding the remainder. */
	private fun relativeToBase(href: String, basePath: String): String {
		val encodedPath = href.trim('/')
		val encodedRelative = if (basePath.isNotEmpty() && encodedPath.startsWith(basePath)) {
			encodedPath.removePrefix(basePath).trim('/')
		} else {
			encodedPath
		}
		return URLDecoder.decode(encodedRelative, "UTF-8")
	}

	private fun childText(element: Element, localName: String): String? {
		val node = firstDescendant(element, localName) ?: return null
		return node.textContent?.takeIf { it.isNotBlank() }
	}

	private fun firstDescendant(node: Node, localName: String): Element? {
		val children = node.childNodes
		for (i in 0 until children.length) {
			val child = children.item(i)
			if (child is Element && child.localName == localName) return child
		}
		// WebDAV props can be nested one level deeper (e.g. resourcetype/collection); search recursively.
		for (i in 0 until children.length) {
			val child = children.item(i)
			if (child is Element) {
				firstDescendant(child, localName)?.let { return it }
			}
		}
		return null
	}

	private fun parseHttpDate(value: String): Long? {
		return try {
			httpDateFormat.get()!!.parse(value)?.time
		} catch (e: Exception) {
			null
		}
	}
}
