package click.alchemist.cook

import android.content.Context

private const val FALLBACK_LABEL = "AlchemistCook"

/**
 * Identifies this app in the `User-Agent` header of outgoing HTTP requests, in place of OkHttp's
 * generic default (`okhttp/x.x`). Nextcloud's Login Flow v2 in particular uses whatever User-Agent
 * made the request as the display name for the resulting app/device entry in the server's security
 * settings — without this, every login shows up there simply as "okhttp".
 *
 * Reuses the resolved app label (`android:label`, i.e. the manifest's `applicationLabel` placeholder
 * — see [initUserAgent]) rather than a hardcoded name, since that already differs between a debug
 * build and a release one, and this should tell them apart the same way. Defaults to
 * [FALLBACK_LABEL] until [initUserAgent] runs (or for anything that never calls it, e.g. unit tests).
 */
var USER_AGENT: String = "$FALLBACK_LABEL/${BuildConfig.VERSION_NAME}"
	private set

/** Called once from [App.onCreate], where a real [Context] — needed to resolve the app's own label — is available. */
fun initUserAgent(context: Context) {
	val label = context.packageManager.getApplicationLabel(context.applicationInfo).toString()
	USER_AGENT = "$label/${BuildConfig.VERSION_NAME}"
}
