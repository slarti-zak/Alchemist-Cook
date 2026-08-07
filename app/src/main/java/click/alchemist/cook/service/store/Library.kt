package click.alchemist.cook.service.store

import click.alchemist.cook.service.webdav.WebDavConfig
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * A "library" is one storage location with its own local mirror and its own slice of the local
 * index — the replacement for Couchbase's per-user database + `owner`/`"!"`-channel scheme. Every
 * install has exactly one [LibraryRole.PERSONAL] library; [LibraryRole.SHARED] libraries are added
 * explicitly by the user to collaborate with other accounts on a shared [LibraryConnection].
 */
data class LibraryConfig(
	val id: String,
	val label: String,
	val role: LibraryRole,
	val connection: LibraryConnection
)

enum class LibraryRole { PERSONAL, SHARED }

/**
 * How a library's files are stored/synced. [WebDav] and [Nextcloud] both ultimately speak plain
 * WebDAV (see [webDavConfig]) and are reconciled by [SyncEngine] against a [PrivateLocalMirror]
 * staging copy; only how their [WebDavConfig] gets populated differs. [LocalFolder] has no remote
 * at all — a user-picked on-device folder ([SafLocalMirror]) *is* the canonical storage, written to
 * directly rather than synced.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes(
	JsonSubTypes.Type(value = LibraryConnection.WebDav::class, name = "webdav"),
	JsonSubTypes.Type(value = LibraryConnection.Nextcloud::class, name = "nextcloud"),
	JsonSubTypes.Type(value = LibraryConnection.LocalFolder::class, name = "localFolder")
)
sealed class LibraryConnection {
	data class WebDav(val config: WebDavConfig) : LibraryConnection()

	/**
	 * Same WebDAV transport as [WebDav] once connected — [config] is derived from the credentials
	 * Nextcloud's Login Flow v2 hands back (see `service.nextcloud.NextcloudLoginFlow`). [serverUrl]
	 * is kept separately from `config.baseUrl` (the derived `/remote.php/dav/files/<user>` endpoint)
	 * so reconnecting/re-authenticating can re-run the login flow without the user re-typing it.
	 */
	data class Nextcloud(val config: WebDavConfig, val serverUrl: String) : LibraryConnection()

	/** [treeUri] is a persisted-permission SAF tree URI (`ACTION_OPEN_DOCUMENT_TREE`); [displayName] is for display only. */
	data class LocalFolder(val treeUri: String, val displayName: String) : LibraryConnection()
}

/** The underlying WebDAV endpoint, for the connection kinds that have one — null for [LibraryConnection.LocalFolder]. */
val LibraryConnection.webDavConfig: WebDavConfig?
	get() = when (this) {
		is LibraryConnection.WebDav -> config
		is LibraryConnection.Nextcloud -> config
		is LibraryConnection.LocalFolder -> null
	}
