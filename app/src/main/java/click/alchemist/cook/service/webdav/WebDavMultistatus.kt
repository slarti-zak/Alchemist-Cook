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

	fun parse(xml: ByteArray, requestPath: String): List<WebDavResource> {
		val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
		val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(xml))

		val responses = document.getElementsByTagNameNS(DAV_NS, "response")
		val resources = mutableListOf<WebDavResource>()

		for (i in 0 until responses.length) {
			val response = responses.item(i) as? Element ?: continue
			val href = childText(response, "href") ?: continue
			val path = URLDecoder.decode(href, "UTF-8").trimEnd('/')

			val prop = firstDescendant(response, "prop") ?: continue
			val isCollection = firstDescendant(prop, "resourcetype")
				?.let { firstDescendant(it, "collection") != null } ?: false
			val etag = childText(prop, "getetag")?.trim('"')
			val lastModified = childText(prop, "getlastmodified")?.let(::parseHttpDate)
			val contentLength = childText(prop, "getcontentlength")?.toLongOrNull()
			val relativePath = path.removePrefix("/")

			// Skip the entry for the collection being queried itself; callers only want children.
			if (relativePath == requestPath.trim('/')) continue

			resources.add(WebDavResource(relativePath, isCollection, etag, lastModified, contentLength))
		}

		return resources
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
