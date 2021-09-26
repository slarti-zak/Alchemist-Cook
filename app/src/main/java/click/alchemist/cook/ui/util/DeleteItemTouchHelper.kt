package click.alchemist.cook.ui.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import click.alchemist.cook.R

abstract class DeleteItemTouchHelper(dragDirs: Int, swipeDirs: Int, context: Context) :
		ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {

	private val backgroundPaint = Paint().apply {
		color = ContextCompat.getColor(context, R.color.delete)
	}
	private val deleteDrawable =
		ContextCompat.getDrawable(context, R.drawable.ic_delete)!!.apply {
			DrawableCompat.setTint(this, Color.WHITE);
		}
	private val intrinsicWidth = deleteDrawable.intrinsicWidth
	private val intrinsicHeight = deleteDrawable.intrinsicHeight

	override fun onChildDraw(
		c: Canvas,
		recyclerView: RecyclerView,
		viewHolder: RecyclerView.ViewHolder,
		dX: Float,
		dY: Float,
		actionState: Int,
		isCurrentlyActive: Boolean
	) {
		if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0f) {
			val itemView = viewHolder.itemView
			val itemHeight = itemView.height

			val background = if (dX < 0) RectF(
				itemView.right.toFloat() + dX,
				itemView.top.toFloat(),
				itemView.right.toFloat(),
				itemView.bottom.toFloat()
			) else RectF(
				itemView.left.toFloat(),
				itemView.top.toFloat(),
				itemView.left.toFloat() + dX,
				itemView.bottom.toFloat()
			)

			c.drawRect(background, backgroundPaint)
			val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
			val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
			val deleteIconBottom = deleteIconTop + intrinsicHeight

			val deleteIconLeft =
				if (dX < 0) itemView.right - deleteIconMargin - intrinsicWidth
				else itemView.left + deleteIconMargin
			val deleteIconRight =
				if (dX < 0) itemView.right - deleteIconMargin
				else itemView.left + deleteIconMargin + intrinsicWidth

			deleteDrawable.setBounds(
				deleteIconLeft,
				deleteIconTop,
				deleteIconRight,
				deleteIconBottom
			)
			deleteDrawable.draw(c)
		}

		super.onChildDraw(
			c,
			recyclerView,
			viewHolder,
			dX,
			dY,
			actionState,
			isCurrentlyActive
		)
	}
}