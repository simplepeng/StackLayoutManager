package me.simple.alm

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.StringBuilder
import kotlin.math.abs

/**
 * @param orientation 支持的方向
 */
class AvatarLayoutManager @JvmOverloads constructor(
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

    private var mItemFillDirection: Int = FILL_END

    //填充view的锚点
    private var mFillAnchor: Int = 0

    private val mOutChildren = hashSetOf<View>()

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

//        logDebug("state - $state")

        //不支持预测动画，可以直接return
        if (state.isPreLayout) return

        mCurrentPosition = 0

        detachAndScrapAttachedViews(recycler)

        fillLayout(recycler, state)
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

//        logDebug("delta == $delta")

        val consume = fillScroll(delta, recycler, state)
        offsetChildren(-consume)
        recycleChildren(delta, recycler)

        logChildren(recycler)
        logChildrenPosition(recycler)

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

    //自定义方法

    private fun fill(
        available: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        var remainingSpace = abs(available)

        while (remainingSpace > 0 && hasMore(state)) {

            val child = nextView(recycler)

            if (mItemFillDirection == FILL_START) {
                addView(child, 0)
            } else {
                addView(child)
            }

            measureChildWithMargins(child, 0, 0)

            layoutChunk(child)

            remainingSpace -= getItemSpace(child) / 2
        }

        return available
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
                right = mFillAnchor
                left = right - getItemWidth(child)
            } else {
                left = mFillAnchor
                right = left + getItemWidth(child)
            }
            top = paddingTop
            bottom = top + getItemHeight(child) - paddingBottom
        } else {
            if (reverseLayout) {
                bottom = mFillAnchor
                top = bottom - getItemHeight(child)
            } else {
                top = mFillAnchor
                bottom = top + getItemHeight(child)
            }
            left = paddingLeft
            right = left + getItemWidth(child) - paddingRight
        }

        layoutDecoratedWithMargins(child, left, top, right, bottom)

        if (mItemFillDirection == FILL_START) {
            mFillAnchor -= (getItemSpace(child) / 2 + offset)
        } else {
            mFillAnchor += (getItemSpace(child) / 2 + offset)
        }

    }

    private fun fillLayout(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ) {
        //计算填充view的初始锚点
        mFillAnchor = if (orientation == HORIZONTAL) {
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

        mItemFillDirection = if (reverseLayout) FILL_START else FILL_END

        fill(getTotalSpace(), recycler, state)
    }

    private fun fillScroll(
        delta: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {

        mItemFillDirection = if (delta > 0) FILL_END else FILL_START

        return if (delta > 0) {
            fillEnd(delta, recycler, state)
        } else {
            fillStart(delta, recycler, state)
        }
    }

    //delta < 0
    private fun fillStart(
        delta: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        //如果startView结束的边减去`加上`移动的距离还是没出现在屏幕内
        //那么就可以继续滚动，不填充view
        val startView = getStartView()
        val startViewDecoratedEnd = getDecoratedEnd(startView)
        if (startViewDecoratedEnd - delta < getStart()) {
            return delta
        }

        //如果 startPosition == 0 且startPosition的开始的边加上移动的距离
        //大于等于Recyclerview的最小宽度或高度，就返回修正过后的移动距离
        val startViewDecoratedStart = getDecoratedStart(startView)
        val startPosition = getPosition(startView)
        //已经拖动到了最左边或者顶部
        if (startPosition == getStartPosition(state) && startViewDecoratedStart - delta >= getStart()) {
            return startViewDecoratedStart - getStart()
        }

        resetCurrentPosition(startPosition)

        mFillAnchor = if (reverseLayout) {
            startViewDecoratedStart + getItemSpace(startView) / 2
        } else {
            startViewDecoratedStart - getItemSpace(startView) / 2
        }

        return fill(delta, recycler, state)
    }

    //delta > 0
    private fun fillEnd(
        delta: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        //如果endView的开始的边`减去`移动的距离还是没出现在屏幕内
        //那么就可以继续滚动，不填充view
        val endView = getEndView()
        val endViewDecoratedStart = getDecoratedStart(endView)
        if (endViewDecoratedStart - delta > getEnd()) {
            return delta
        }

        //如果 endPosition == itemCount - 1 且endView的结束的边减去移动的距离
        //小于等于Recyclerview的最大宽度或高度，就返回修正过后的移动距离
        val endViewDecoratedEnd = getDecoratedEnd(endView)
        val endPosition = getPosition(endView)
        if (endPosition == getEndPosition(state) && endViewDecoratedEnd - delta <= getEnd()) {
            return endViewDecoratedEnd - getEnd()
        }

        resetCurrentPosition(endPosition)

        //如果是逆序布局，填充锚点为
        //如果是正序布局，填充锚点为endViewDecoratedEnd减去endView宽度或者高度的一半
        mFillAnchor = if (reverseLayout) {
            endViewDecoratedEnd + getItemSpace(endView) / 2
        } else {
            endViewDecoratedEnd - getItemSpace(endView) / 2
        }

        return fill(delta, recycler, state)
    }


    /**
     * fillStart == -1
     * fillEnd == 1
     * 重新计算当前要填充view的position
     */
    private fun resetCurrentPosition(position: Int) {
        mCurrentPosition = if (reverseLayout) {
            position - mItemFillDirection
        } else {
            position + mItemFillDirection
        }
    }

    private fun getStartPosition(state: RecyclerView.State) =
        if (reverseLayout) state.itemCount - 1 else 0

    private fun getEndPosition(state: RecyclerView.State) =
        if (reverseLayout) 0 else state.itemCount - 1

    private fun recycleChildren(
        consume: Int,
        recycler: RecyclerView.Recycler
    ) {
        if (childCount == 0 || consume == 0) return

        if (consume > 0) {//recycle start
            recycleStart()
        } else {//recycle end
            recycleEnd()
        }

//        logOutChildren()
        recycleOutChildren(recycler)
    }

    private fun recycleStart() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)!!
            val end = getRecycleStartEdge(child)
            if (end > getStart()) break

            logDebug("recycleStart -- ${getPosition(child)}")
            mOutChildren.add(child)
        }
    }

    private fun getRecycleStartEdge(child: View) = if (orientation == HORIZONTAL) {
        getDecoratedRight(child)
    } else {
        getDecoratedBottom(child)
    }

    private fun recycleEnd() {
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)!!
            val start = getRecycleEndEdge(child)
            if (start < getEnd()) break

            logDebug("recycleEnd -- ${getPosition(child)}")
            mOutChildren.add(child)
        }
    }

    private fun getRecycleEndEdge(child: View) = if (orientation == HORIZONTAL) {
        getDecoratedLeft(child)
    } else {
        getDecoratedTop(child)
    }

    private fun recycleOutChildren(recycler: RecyclerView.Recycler) {
        for (view in mOutChildren) {
//            logDebug("recycle -- ${getPosition(view)}")
            removeAndRecycleView(view, recycler)
        }
        mOutChildren.clear()
    }

    //自定义方法结束

//---- 模仿创建OrientationHelper帮助类开始

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

    /**
     * 移动所有子view
     */
    private fun offsetChildren(amount: Int) {
        if (orientation == HORIZONTAL) {
            offsetChildrenHorizontal(amount)
        } else {
            offsetChildrenVertical(amount)
        }
    }

    private fun nextView(recycler: RecyclerView.Recycler): View {
        val view = recycler.getViewForPosition(mCurrentPosition)

        if (reverseLayout) {
            mCurrentPosition -= mItemFillDirection
        } else {
            mCurrentPosition += mItemFillDirection
        }
        return view
    }

    private fun getItemWidth(child: View): Int {
        val params = child.layoutParams as RecyclerView.LayoutParams
        return getDecoratedMeasuredWidth(child) + params.leftMargin + params.rightMargin
    }

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

    private fun getDecoratedStart(child: View): Int {
        return if (orientation == HORIZONTAL) {
            getDecoratedLeft(child)
        } else {
            getDecoratedTop(child)
        }
    }

    private fun getDecoratedEnd(child: View): Int {
        return if (orientation == HORIZONTAL) {
            getDecoratedRight(child)
        } else {
            getDecoratedBottom(child)
        }
    }

    private fun getStartView() = getChildAt(0)!!

    private fun getEndView() = getChildAt(childCount - 1)!!
//---- 模仿创建OrientationHelper帮助类结束

    private fun logDebug(msg: String) {
        Log.d("AvatarLayoutManager", msg)
    }

    private fun logChildren(recycler: RecyclerView.Recycler) {
        logDebug("childCount = $childCount -- scrapSize = ${recycler.scrapList.size}")
    }

    private fun logChildrenPosition(recycler: RecyclerView.Recycler) {
        val builder = StringBuilder()
        for (i in 0 until childCount) {
            val child = getChildAt(i)!!
            builder.append(getPosition(child))
            builder.append(",")
        }
        logDebug("child position == $builder")
    }

    private fun logOutChildren() {
        val builder = StringBuilder()
        for (view in mOutChildren) {
            builder.append(getPosition(view))
            builder.append(",")
        }
        logDebug("out children == ${builder.toString()}")
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