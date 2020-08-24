package me.simple.alm

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.recyclerview.widget.RecyclerView

class AvatarRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    init {
        isChildrenDrawingOrderEnabled = true
    }

    override fun getChildDrawingOrder(childCount: Int, i: Int): Int {
        Log.d("getChildDrawingOrder", "childCount == $childCount -- i == $i")
//        return childCount - 1 - i
        return super.getChildDrawingOrder(childCount, i)
    }

    override fun setChildDrawingOrderCallback(childDrawingOrderCallback: ChildDrawingOrderCallback?) {
        super.setChildDrawingOrderCallback(childDrawingOrderCallback)
    }
}