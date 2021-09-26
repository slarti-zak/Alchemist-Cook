package click.alchemist.cook.extension

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import java.util.*

fun View.clearFocusAndCloseKeyboard() {
	val imm: InputMethodManager =
		context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
	imm.hideSoftInputFromWindow(windowToken, 0)
	clearFocus()
}

inline fun <reified T> View.findViewByClass(): T? {
	if (this is T) return this

	if (this is ViewGroup) {
		val toSearch = LinkedList<ViewGroup>()
		toSearch.addFirst(this)

		while (toSearch.isNotEmpty()) {
			val toCheck = toSearch.removeFirst()

			val count = toCheck.childCount
			for (index in 0..count) {
				val child = toCheck.getChildAt(index)
				if (child is T) return child
				if (child is ViewGroup) {
					toSearch.addLast(child)
				}
			}
		}
	}

	return null
}

inline fun <reified T> View.findViewByDirectSubClass(): T? {
	if (this is ViewGroup) {
		(0..childCount).forEach {
			val view = getChildAt(it)
			if (view is T)
				return view
		}
	}

	return null
}

fun View.awaitSize(function: (width: Int, height: Int) -> Unit) {
	val width = this.width
	val height = this.height
	if (width > 0 && height > 0) {
		function(width, height)
	} else {
		this.awaitLayout { function(this.width, this.height) }
	}
}

fun View.awaitLayout(callback: () -> Unit) {
	this.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
		override fun onGlobalLayout() {
			this@awaitLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
			callback()
		}
	})
}