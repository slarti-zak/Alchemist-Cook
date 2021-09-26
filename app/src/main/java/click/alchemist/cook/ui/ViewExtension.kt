package click.alchemist.cook.ui

import android.graphics.Paint
import android.widget.TextView

fun TextView.strikeThru(strikeThru: Boolean) {
    if (strikeThru) {
        if (!this.paint.isStrikeThruText) {
            this.paintFlags = this.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }
    } else {
        if (this.paint.isStrikeThruText) {
            this.paintFlags = this.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }
}