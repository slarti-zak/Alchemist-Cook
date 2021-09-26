package click.alchemist.cook.extension

import android.widget.TextView
import androidx.core.view.isGone

inline fun TextView.applyTextAndGone(gone: Boolean, text: () -> String) {
	this.isGone = gone
	if (!gone) {
		this.text = text()
	}
}