package demo.simple.stacklayoutmanager

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.min

class BlogLayoutManager : RecyclerView.LayoutManager() {

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {

        //轻量级的将view移除屏幕
        detachAndScrapAttachedViews(recycler)
        //开始填充view

        var totalSpace = width - paddingRight
        var currentPosition = 0

        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        //模仿LinearLayoutManager的写法，当可用距离足够和要填充
        //的itemView的position在合法范围内才填充View
        while (totalSpace > 0 && currentPosition < state.itemCount) {
            val view = recycler.getViewForPosition(currentPosition)
            addView(view)
            measureChild(view, 0, 0)

            right = left + getDecoratedMeasuredWidth(view)
            bottom = top + getDecoratedMeasuredHeight(view)
            layoutDecorated(view, left, top, right, bottom)

            currentPosition++
            left += getDecoratedMeasuredWidth(view)
            //关键点
            totalSpace -= getDecoratedMeasuredWidth(view)
        }
        //layout完成后输出相关信息
        logChildCount("onLayoutChildren", recycler)
    }

    private fun logChildCount(tag: String, recycler: RecyclerView.Recycler) {
        Log.d(tag, "childCount = $childCount -- scrapSize = ${recycler.scrapList.size}")
    }

    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        if (childCount == 0 || dx == 0) return 0
        Log.d("scrollHorizontallyBy", "dx == $dx")

        //填充View，consumed就是修复后的移动值
        val consumed = fill(dx, recycler)
        Log.d("scrollHorizontallyBy", "consumed == $consumed")
        //回收View
        recycle(consumed, recycler)
        //移动View
        offsetChildrenHorizontal(-consumed)

        //输出children
//        logChildCount("scrollHorizontallyBy", recycler)
        return consumed
    }

    private fun fill(dx: Int, recycler: RecyclerView.Recycler): Int {
        //将要填充的position
        var fillPosition = RecyclerView.NO_POSITION
        //可用的空间，和onLayoutChildren中的totalSpace类似
        var availableSpace = abs(dx)
        //增加一个滑动距离的绝对值，方便计算
        val absDelta = abs(dx)

        //将要填充的View的左上右下
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0

        //dx>0就是手指从右滑向左，所以就要填充尾部
        if (dx > 0) {
            val anchorView = getChildAt(childCount - 1)!!
            val anchorPosition = getPosition(anchorView)
            val anchorRight = getDecoratedRight(anchorView)

            left = anchorRight
            //填充尾部，那么下一个position就应该是+1
            fillPosition = anchorPosition + 1

            //如果要填充的position超过合理范围并且最后一个View的
            //right-移动的距离 < 右边缘(width)那就要修正真实能移动的距离
            if (fillPosition >= itemCount && anchorRight - absDelta < width) {
                return anchorRight - width
            }

            //如果尾部的锚点位置加上dx还是在屏幕外，就不填充下一个View
            if (anchorRight + absDelta > width) {
                return dx
            }
        }

        //dx<0就是手指从左滑向右，所以就要填充头部
        if (dx < 0) {
            val anchorView = getChildAt(0)!!
            val anchorPosition = getPosition(anchorView)
            val anchorLeft = getDecoratedLeft(anchorView)

            right = anchorLeft
            //填充头部，那么上一个position就应该是-1
            fillPosition = anchorPosition - 1

            //如果要填充的position超过合理范围并且第一个View的
            //left+移动的距离 > 左边缘(0)那就要修正真实能移动的距离
            if (fillPosition < 0 && anchorLeft + absDelta > 0) {
                return anchorLeft
            }

            //如果头部的锚点位置加上dx还是在屏幕外，就不填充上一个View
            if (anchorLeft + absDelta < 0) {
                return dx
            }
        }

        //根据限定条件，不停地填充View进来
        while (availableSpace > 0 && fillPosition < itemCount && fillPosition >= 0) {
            val itemView = recycler.getViewForPosition(fillPosition)

            if (dx > 0) {
                addView(itemView)
            } else {
                addView(itemView, 0)
            }

            measureChild(itemView, 0, 0)

            if (dx > 0) {
                right = left + getDecoratedMeasuredWidth(itemView)
            } else {
                left = right - getDecoratedMeasuredWidth(itemView)
            }

            bottom = top + getDecoratedMeasuredHeight(itemView)
            layoutDecorated(itemView, left, top, right, bottom)

            if (dx > 0) {
                left += getDecoratedMeasuredWidth(itemView)
                fillPosition++
            } else {
                right -= getDecoratedMeasuredWidth(itemView)
                fillPosition--
            }

            availableSpace -= getDecoratedMeasuredWidth(itemView)
        }

        if (availableSpace > 0) {
            if (dx > 0) {
                return absDelta - availableSpace
            } else {
                return -absDelta - availableSpace
            }
        } else {
            return dx
        }

        return dx
    }

    private fun recycle(
        dx: Int,
        recycler: RecyclerView.Recycler
    ) {
        val absDelta = abs(dx)
        //要回收View的集合，暂存
        val recycleViews = hashSetOf<View>()

        //dx>0就是手指从右滑向左，开始填充后面的View，所以要回收前面的children
        if (dx > 0) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)!!
                val right = getDecoratedRight(child)

                //itemView的right<0就是要超出屏幕要回收View
                if (right > 0) break
                recycleViews.add(child)
            }
        }

        //dx<0就是从左滑向右，所以要回收后面的children
        if (dx < 0) {
            for (i in childCount - 1 downTo 0) {
                val child = getChildAt(i)!!
                val left = getDecoratedLeft(child)

                //itemView的left>recyclerView.width就是要超出屏幕要回收View
                if (left < width) break
                recycleViews.add(child)
            }
        }

        //真正把View移除掉
        for (view in recycleViews) {
            removeAndRecycleView(view, recycler)
        }
        recycleViews.clear()
    }

}