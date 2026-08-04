package click.alchemist.cook.service.store

import click.alchemist.cook.service.webdav.WebDavConfig

/**
 * A "library" is one WebDAV connection with its own local mirror and its own slice of the local
 * index — the replacement for Couchbase's per-user database + `owner`/`"!"`-channel scheme.
 * Every install has exactly one [LibraryRole.PERSONAL] library; [LibraryRole.SHARED] libraries are
 * added explicitly by the user to collaborate with other accounts on a shared WebDAV folder.
 */
data class LibraryConfig(
	val id: String,
	val label: String,
	val role: LibraryRole,
	val webDav: WebDavConfig
)

enum class LibraryRole { PERSONAL, SHARED }
