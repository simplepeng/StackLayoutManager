package me.simple.alm

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class AvatarLayoutManager(
    private val orientation: Int = HORIZONTAL,
    private val reverseLayout: Boolean = false,
    private val offset: Int = 1,
    private val changeDrawingOrder: Boolean = false
) : RecyclerView.LayoutManager() {

    companion object {
        const val HORIZONTAL = LinearLayoutManager.HORIZONTAL
        const val VERTICAL = LinearLayoutManager.VERTICAL

        const val FILL_START = -1
        const val FILL_END = 1
    }

    private var mPendingPosition: Int = RecyclerView.NO_POSITION
    private var mCurrentPosition: Int = 0
    private var mAvailable: Int = 0

    private var mItemFillDirection: Int = FILL_END

    private var mLastFillCoordinate: Int = 0

    private val mRecycleChildren = mutableListOf<View>()

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

//        mAvailable = getTotalSpace()
        logDebug("totalSpace == ${getTotalSpace()}")

        mLastFillCoordinate = if (orientation == HORIZONTAL) {
            if (reverseLayout) {
                width - paddingRight
            } else {
                paddingLeft
            }
        } else {
            if (reverseLayout) {
                height - paddingBottom
            } else {
                paddingTop
            }
        }
        fill(getTotalSpace(), recycler, state)

        logDebug("fillLayoutChildren end totalSpace == ${getTotalSpace()}")

        logChildren(recycler)
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

    //delta > 0 向右或者下滑，反之则反
    private fun scrollBy(
        delta: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (childCount == 0 || delta == 0) {
            return 0
        }

        logDebug("delta == $delta")

        val consume = fillScroll(delta, recycler, state)
        offsetChildren(-consume)
        recycleChildren(recycler)

        logChildren(recycler)

        return consume
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

    private fun fill(
        available: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ) {
        mItemFillDirection = if (available > 0) FILL_END else FILL_START

        var remainingSpace = abs(available)

        while (remainingSpace > 0 && hasMore(state)) {
            val child = nextView(recycler)
            if (mItemFillDirection == FILL_END) {
                addView(child)
            } else {
                addView(child, 0)
            }
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
                right = mLastFillCoordinate
                left = right - getItemWidth(child)
            } else {
                left = mLastFillCoordinate
                right = left + getItemWidth(child)
            }
            top = paddingTop
            bottom = top + getItemHeight(child) - paddingBottom
        } else {
            if (reverseLayout) {
                bottom = mLastFillCoordinate
                top = bottom - getItemHeight(child)
            } else {
                top = mLastFillCoordinate
                bottom = top + getItemHeight(child)
            }
            left = paddingLeft
            right = left + getItemWidth(child) - paddingRight
        }

        layoutDecoratedWithMargins(child, left, top, right, bottom)

        if (orientation == HORIZONTAL) {
            if (reverseLayout) {
                mLastFillCoordinate -= (getItemWidth(child) / 2 + offset)
            } else {
                mLastFillCoordinate += (getItemWidth(child) / 2 + offset)
            }
        } else {
            if (reverseLayout) {
                mLastFillCoordinate -= (getItemHeight(child) / 2 + offset)
            } else {
                mLastFillCoordinate += (getItemHeight(child) / 2 + offset)
            }
        }

    }

    private fun fillScroll(
        delta: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (delta > 0) {
            return fillEnd(delta, recycler, state)
        } else {
            return fillStart()
        }
    }

    private fun fillEnd(
        delta: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        val lastView = getChildAt(childCount - 1)!!
        val lastLeft = getDecoratedLeft(lastView)
        if (lastLeft > getEnd()) {
            return delta
        }

        val lastRight = getDecoratedRight(lastView)
        val lastPosition = getPosition(lastView)
        if (lastPosition == state.itemCount - 1 && lastRight <= getEnd()) {
            return lastRight - getEnd()
        }

        mCurrentPosition = lastPosition + 1
        mLastFillCoordinate = getDecoratedRight(lastView) - getItemWidth(lastView) / 2
        fill(delta, recycler, state)
        return delta
    }

    private fun fillStart(): Int {

        return 0
    }

    private fun recycleChildren(recycler: RecyclerView.Recycler) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)!!

            if (orientation == HORIZONTAL) {
                if (getDecoratedRight(child) < getStart() || getDecoratedLeft(child) > getEnd()) {
                    mRecycleChildren.add(child)
                }
            } else {
                if (getDecoratedBottom(child) < getStart() || getDecoratedTop(child) > getEnd()) {
                    mRecycleChildren.add(child)
                }
            }
        }

        for (child in mRecycleChildren) {
            logDebug("recycleChildren -- ${getPosition(child)}")
            removeAndRecycleView(child, recycler)
        }

        mRecycleChildren.clear()
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
        mCurrentPosition += mItemFillDirection
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

    /**
     * 获取一个item的宽度或者高度
     * 和getItemSpace相反
     */
    private fun getItemHeight(child: View): Int {
        val params = child.layoutParams as RecyclerView.LayoutParams
        return getDecoratedMeasuredHeight(child) + params.topMargin + params.bottomMargin
    }

    private fun getItemSpace(child: View) = if (orientation == HORIZONTAL) {
        getItemWidth(child)
    } else {
        getItemHeight(child)
    }

    private fun getStart(): Int {
        return if (orientation == HORIZONTAL) {
            paddingLeft
        } else {
            paddingTop
        }
    }

    private fun getEnd(): Int {
        return if (orientation == HORIZONTAL) {
            width - paddingRight
        } else {
            height - paddingBottom
        }
    }

    //模仿创建OrientationHelper帮助类结束


    private fun logDebug(msg: String) {
        Log.d("AvatarLayoutManager", msg)
    }

    private fun logChildren(recycler: RecyclerView.Recycler) {
        logDebug("childCount = $childCount -- scrapSize = ${recycler.scrapList.size}")
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