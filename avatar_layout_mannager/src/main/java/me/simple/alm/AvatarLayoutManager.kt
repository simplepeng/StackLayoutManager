package me.simple.alm

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AvatarLayoutManager(
    private val orientation: Int = HORIZONTAL,
    private val reverseLayout: Boolean = false,
    private val offset: Int = 1,
    private val changeDrawingOrder: Boolean = false
) : RecyclerView.LayoutManager() {

    companion object {
        const val HORIZONTAL = LinearLayoutManager.HORIZONTAL
        const val VERTICAL = LinearLayoutManager.VERTICAL

        const val LAYOUT_HEAD = -1
        const val LAYOUT_TAIL = 1
    }

    private var mPendingPosition: Int = RecyclerView.NO_POSITION
    private var mCurrentPosition: Int = 0
    private var mAvailable: Int = 0

    private var mItemLayoutDirection: Int = LAYOUT_TAIL

    private var mLastLayoutCoordinate: Int = 0

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    override fun onLayoutChildren(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ) {
        if (state.itemCount == 0) {
            removeAndRecycleAllViews(recycler)
            return
        }

        logDebug("state - $state")

        if (state.isPreLayout) return

//        mCurrentPosition = if (mPendingPosition != RecyclerView.NO_POSITION) {
//            mPendingPosition
//        } else {
//            0
//        }
        mCurrentPosition = 0

        if (state.isMeasuring) {
            mAvailable = getTotalSpace()
//            return
        }

        detachAndScrapAttachedViews(recycler)
        mLastLayoutCoordinate = if (reverseLayout) {
            width - paddingRight
        } else {
            0
        }

//        mAvailable = getTotalSpace()
        logDebug("totalSpace == ${getTotalSpace()}")

        fillLayoutChildren(recycler, state)

        logDebug("fillLayoutChildren end totalSpace == ${getTotalSpace()}")
    }

    override fun onLayoutCompleted(state: RecyclerView.State) {
        mPendingPosition = RecyclerView.NO_POSITION
    }

    override fun canScrollHorizontally() = orientation == HORIZONTAL

    override fun canScrollVertically() = orientation == VERTICAL

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (orientation == VERTICAL) return 0

        return scrollBy(dx, recycler, state)
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (orientation == HORIZONTAL) return 0

        return scrollBy(dy, recycler, state)
    }

    private fun scrollBy(
        delta: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (childCount == 0 || delta == 0) {
            return 0
        }


        return delta
    }

    override fun scrollToPosition(position: Int) {
        mPendingPosition = position
        requestLayout()
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int
    ) {

    }

    //自定义工具方法

    private fun fillLayoutChildren(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ) {
        var remainingSpace = getTotalSpace()

        while (remainingSpace > 0 && hasMore(state)) {
            val child = nextView(recycler)
            addView(child)
            measureChildWithMargins(child, 0, 0)

            layoutChunk(child)

            remainingSpace -= getItemSpace(child) / 2
            logDebug("remainingSpace == $remainingSpace")
        }
    }

    private fun layoutChunk(
        child: View
    ) {
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        if (orientation == HORIZONTAL) {
            if (reverseLayout) {
                right = mLastLayoutCoordinate
                left = right - getItemWidth(child)
            } else {
                left = mLastLayoutCoordinate
                right = left + getItemWidth(child)
            }
            top = paddingTop
            bottom = getItemHeight(child) - paddingBottom
        } else {
            if (reverseLayout) {

            } else {

            }
        }

        layoutDecoratedWithMargins(child, left, top, right, bottom)

        if (reverseLayout) {
            mLastLayoutCoordinate -= (getItemWidth(child) / 2 + offset)
        } else {
            mLastLayoutCoordinate += (getItemWidth(child) / 2 + offset)
        }
    }

    private fun fillScrollChildren() {

    }

    //模仿创建OrientationHelper帮助类开始

    private fun hasMore(state: RecyclerView.State): Boolean {
        return mCurrentPosition >= 0 && mCurrentPosition < state.itemCount
    }

    private fun getTotalSpace(): Int {
        return if (orientation == HORIZONTAL) {
            width - paddingLeft - paddingRight
        } else {
            height - paddingTop - paddingBottom
        }
    }

    private fun offsetChildren(amount: Int) {
        if (orientation == HORIZONTAL) {
            offsetChildrenHorizontal(amount)
        } else {
            offsetChildrenVertical(amount)
        }
    }

    private fun nextView(recycler: RecyclerView.Recycler): View {
        val view = recycler.getViewForPosition(mCurrentPosition)
        mCurrentPosition += mItemLayoutDirection
        return view
    }

    /**
     * 获取一个item的所占的空间
     * HORIZONTAL的时候就是width，VERTICAL时就是高度
     */
    private fun getItemWidth(child: View): Int {
        val params = child.layoutParams as RecyclerView.LayoutParams
        return getDecoratedMeasuredWidth(child) + params.leftMargin + params.rightMargin
    }

    private fun getItemSpace(child: View) = if (orientation == HORIZONTAL) {
        getItemWidth(child)
    } else {
        getItemHeight(child)
    }

    /**
     * 获取一个item的宽度或者高度
     * 和getItemSpace相反
     */
    private fun getItemHeight(child: View): Int {
        val params = child.layoutParams as RecyclerView.LayoutParams
        return getDecoratedMeasuredHeight(child) + params.topMargin + params.bottomMargin
    }

    //模仿创建OrientationHelper帮助类结束


    private fun logDebug(msg: String) {
        Log.d("AvatarLayoutManager", msg)
    }

    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)

        //改变children绘制顺序
        if (changeDrawingOrder) {
            view.setChildDrawingOrderCallback { childCount, i ->
                childCount - 1 - i
            }
        }
    }

}