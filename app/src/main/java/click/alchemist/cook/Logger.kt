package click.alchemist.cook

import android.util.Log

fun logInfo(msg: String) = logInfo("AlchemistCook", msg)

fun logInfo(tag: String, msg: String) = Log.i(tag, msg)

fun logDebug(msg: String) = logDebug("AlchemistCook", msg)

fun logDebug(tag: String, msg: String) = Log.d(tag, msg)

fun logError(tag: String, msg: String) = Log.e(tag, msg)

fun logError(tag: String, msg: String, tr: Throwable) = Log.e(tag, msg, tr)

fun logError(msg: String, tr: Throwable) = Log.e("AlchemistCook", msg, tr)