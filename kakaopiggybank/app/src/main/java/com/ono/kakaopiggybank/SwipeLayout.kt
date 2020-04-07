package com.ono.kakaopiggybank

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.NestedScrollingParent
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.core.widget.ListViewCompat
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher
import java.lang.Math.abs
import java.util.*
import java.util.concurrent.TimeUnit

class SwipeLayout
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ViewGroup(context, attrs), NestedScrollingParent, NestedScrollingChild {

    companion object {
        private val LOG_TAG = SwipeLayout::class.java!!.simpleName
        private val LAYOUT_ATTRS = intArrayOf(android.R.attr.enabled)

        private const val INVALID_POINTER = -1
        private const val DRAG_RATE = .5f
    }

    /** about nested scroll **/
    private var mTotalUnconsumed = 0f
    private val mNestedScrollingParentHelper: NestedScrollingParentHelper
    private val mNestedScrollingChildHelper: NestedScrollingChildHelper
    private val mParentScrollConsumed = IntArray(2)
    private val mParentOffsetInWindow = IntArray(2)
    private var mNestedScrollInProgress: Boolean = false

    /** about drag **/
    private var mTarget: View? = null                                               //현재 터치 childview
    private var mDefaultLayoutOffsetTop: Int = 0                                    //초기 Layout의 Top offset
    private var mCurrentLayoutOffsetTop: Int = 0                                    //현재 Layout의 Top offset된

    private val mTouchSlop: Int                                                     //터치로 인식되는 최대 범위
            by lazy {ViewConfiguration.get(context).scaledTouchSlop}
    private val mDragLimit = 64 * resources.displayMetrics.density           //드래그 가능 최대 범위

    private var mInitialMotionY = 0f                                                //터치가 아닌 드래그로 판명된 지점의 y offset
    private var mInitialDownY = 0f                                                  //맨처음 터치 지점의 y offset
    private var mActivePointerId = INVALID_POINTER                             //현재 활성화되어있는 손가락 포인터

    private var mIsBeingDragged: Boolean = false                                    //드래그 여부
    private var mReturningToStart: Boolean = false                                  //드래그 후, 리턴 중 여부

    private var mFrom: Int = 0                                                      //드래그가 끝난 시점의 y offset

    /** about callback **/
    private var mDragSuccess : OnDragSuccess? = null
    private var mChildScrollUpCallback: OnChildScrollUpCallback? = null

    init {
        setWillNotDraw(false)
        mNestedScrollingParentHelper = NestedScrollingParentHelper(this)
        mNestedScrollingChildHelper = NestedScrollingChildHelper(this)
        isNestedScrollingEnabled = true

        mCurrentLayoutOffsetTop = this.top
        mDefaultLayoutOffsetTop = mCurrentLayoutOffsetTop
        moveToStart(1.0f)

        val a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS)
        isEnabled = a.getBoolean(0, true)
        a.recycle()
    }

    private fun reset() {
        Log.d(LOG_TAG, "reset")
        setLayoutOffset(mDefaultLayoutOffsetTop - mCurrentLayoutOffsetTop)
        mCurrentLayoutOffsetTop = this.getTop()
    }

    private fun ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid out yet.
        if (mTarget == null) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                mTarget = child
                break
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) {
            reset()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        reset()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = measuredWidth
        val height = measuredHeight
        if (childCount == 0) {
            return
        }
        if (mTarget == null) {
            ensureTarget()
        }
        if (mTarget == null) {
            return
        }
        val child = mTarget
        val childLeft = paddingLeft
        val childTop = paddingTop
        val childWidth = width - paddingLeft - paddingRight
        val childHeight = height - paddingTop - paddingBottom
        child!!.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (mTarget == null) {
            ensureTarget()
        }
        if (mTarget == null) {
            return
        }
        mTarget!!.measure(
            View.MeasureSpec.makeMeasureSpec(
                measuredWidth - paddingLeft - paddingRight,
                View.MeasureSpec.EXACTLY
            ), View.MeasureSpec.makeMeasureSpec(
                measuredHeight - paddingTop - paddingBottom, View.MeasureSpec.EXACTLY
            )
        )
    }

    fun canChildScrollUp(): Boolean {
        if (mChildScrollUpCallback != null) {
            return mChildScrollUpCallback!!.canChildScrollUp(this, mTarget)
        }
        return if (mTarget is ListView) {
            ListViewCompat.canScrollList((mTarget as ListView?)!!, -1)
        } else mTarget!!.canScrollVertically(-1)
    }

    override fun requestDisallowInterceptTouchEvent(b: Boolean) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if (android.os.Build.VERSION.SDK_INT < 21 && mTarget is AbsListView || mTarget != null && !ViewCompat.isNestedScrollingEnabled(
                mTarget!!
            )
        ) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b)
        }
    }

    /** NestedScrollingParent**/

    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        return (isEnabled && !mReturningToStart && nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0)
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes)
        // Dispatch up to the nested parent
        startNestedScroll(axes and ViewCompat.SCROLL_AXIS_VERTICAL)
        mTotalUnconsumed = 0f
        mNestedScrollInProgress = true
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - mTotalUnconsumed.toInt()
                mTotalUnconsumed = 0f
            } else {
                mTotalUnconsumed -= dy.toFloat()
                consumed[1] = dy
            }
            move(mTotalUnconsumed)
        }

        // Now let our nested parent consume the leftovers
        val parentConsumed = mParentScrollConsumed
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0]
            consumed[1] += parentConsumed[1]
        }
    }

    override fun getNestedScrollAxes(): Int {
        return mNestedScrollingParentHelper.nestedScrollAxes
    }

    override fun onStopNestedScroll(target: View) {
        mNestedScrollingParentHelper.onStopNestedScroll(target)
        mNestedScrollInProgress = false
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            finish(mTotalUnconsumed)
            mTotalUnconsumed = 0f
        }
        // Dispatch up our nested parent
        stopNestedScroll()
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int
    ) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
            mParentOffsetInWindow
        )

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        val dy = dyUnconsumed + mParentOffsetInWindow[1]
        if (dy < 0 && !canChildScrollUp()) {
            mTotalUnconsumed += Math.abs(dy).toFloat()
            move(mTotalUnconsumed)
        }
    }

    /** NestedScrollingChild **/

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mNestedScrollingChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return mNestedScrollingChildHelper.startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return mNestedScrollingChildHelper.hasNestedScrollingParent()
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, offsetInWindow: IntArray?
    ): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed,
            dxUnconsumed, dyUnconsumed, offsetInWindow
        )
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
            dx, dy, consumed, offsetInWindow
        )
    }

    override fun onNestedPreFling(
        target: View, velocityX: Float,
        velocityY: Float
    ): Boolean {
        return dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun onNestedFling(
        target: View, velocityX: Float, velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    /** override for drag **/

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        ensureTarget()
        val action = ev.actionMasked
        val pointerIndex: Int

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false
        }

        if (!isEnabled || mReturningToStart || canChildScrollUp() || mNestedScrollInProgress) {
            return false
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                setLayoutOffset(mDefaultLayoutOffsetTop)
                mActivePointerId = ev.getPointerId(0)
                mIsBeingDragged = false

                pointerIndex = ev.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    return false
                }
                mInitialDownY = ev.getY(pointerIndex)
            }

            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.")
                    return false
                }

                pointerIndex = ev.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    return false
                }
                val y = ev.getY(pointerIndex)
                startDragging(y)
            }

            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsBeingDragged = false
                mActivePointerId = INVALID_POINTER
            }
        }

        return mIsBeingDragged
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        var pointerIndex = -1

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false
        }

        if (!isEnabled || mReturningToStart || canChildScrollUp() || mNestedScrollInProgress) {
            return false
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = ev.getPointerId(0)
                mIsBeingDragged = false
            }

            MotionEvent.ACTION_MOVE -> {
                pointerIndex = ev.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.")
                    return false
                }

                val y = ev.getY(pointerIndex)
                startDragging(y)

                if (mIsBeingDragged) {
                    val overscrollTop = (y - mInitialMotionY) * DRAG_RATE
                    if (overscrollTop > 0) {
                        move(overscrollTop)
                    } else {
                        return false
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                pointerIndex = ev.actionIndex
                if (pointerIndex < 0) {
                    Log.e(
                        LOG_TAG,
                        "Got ACTION_POINTER_DOWN event but have an invalid action index."
                    )
                    return false
                }
                mActivePointerId = ev.getPointerId(pointerIndex)
            }

            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)

            MotionEvent.ACTION_UP -> {
                pointerIndex = ev.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.")
                    return false
                }

                if (mIsBeingDragged) {
                    val y = ev.getY(pointerIndex)
                    val overscrollTop = (y - mInitialMotionY) * DRAG_RATE
                    mIsBeingDragged = false
                    finish(overscrollTop)
                }
                mActivePointerId = INVALID_POINTER
                return false
            }
            MotionEvent.ACTION_CANCEL -> return false
        }

        return true
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.actionIndex
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mActivePointerId = ev.getPointerId(newPointerIndex)
        }
    }

    /** drag core **/

    private class RepeatWithDelay(private var timer: Float, private var pollingInterval: Long) :
        Function<Flowable<Any>, Publisher<Long>> {

        private val repeatLimit by lazy {
            timer / pollingInterval
        }
        private var repeatCount = 0

        override fun apply(t: Flowable<Any>): Publisher<Long> {
            return t.flatMap(
                object : Function<Any, Publisher<Long>> {
                    override fun apply(t: Any): Publisher<Long> {
                        if (repeatCount >= repeatLimit) {
                            return Flowable.empty()
                        }
                        repeatCount += 1
                        println(repeatCount)
                        return Flowable.timer(pollingInterval, TimeUnit.MILLISECONDS)
                    }
                }
            )
        }
    }

    private fun move(overscrollTop: Float) {
        val originalDragPercent = overscrollTop / mDragLimit

        val dragPercent = Math.min(1f, Math.abs(originalDragPercent))

        val extraOS = Math.abs(overscrollTop) - mDragLimit
        val slingshotDist = mDragLimit

        val tensionSlingshotPercent =
            Math.max(0f, (Math.min(extraOS, slingshotDist * 2) / slingshotDist))
        val tensionPercent = ((((tensionSlingshotPercent / 4) - Math.pow(
            (tensionSlingshotPercent / 4).toDouble(), 2.0
        ))).toFloat() * 2f)
        val extraMove = (slingshotDist) * tensionPercent * 2f

        val targetY = mDefaultLayoutOffsetTop + ((slingshotDist * dragPercent) + extraMove).toInt()
        setLayoutOffset(targetY - mCurrentLayoutOffsetTop)
    }

    @SuppressLint("CheckResult")
    private fun finish(overscrollTop: Float) {
        if (overscrollTop > mDragLimit) {
            if (mDragSuccess != null) {
                mDragSuccess!!.onDragSucces()
            }
        }

        val timer = 100F
        val handle = RepeatWithDelay(timer, 10) // fps

        val t1 = Date().time
        mFrom = mCurrentLayoutOffsetTop

        val flowable = Flowable.just("Polling")
            .repeatWhen(handle as Function<in Flowable<Any>, out Publisher<*>>)

        flowable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val t2 = Date().time
                val diff = abs(t2 - t1)
                val interpolatedTime = diff / timer
                moveToStart(interpolatedTime)
            }
    }

    private fun startDragging(y: Float) {
        val yDiff = y - mInitialDownY

        if (yDiff > mTouchSlop && !mIsBeingDragged) {
            mInitialMotionY = mInitialDownY + mTouchSlop
            mIsBeingDragged = true
        }
    }

    private fun moveToStart(interpolatedTime: Float) {
        var targetTop = 0

        if (interpolatedTime < 1) {
            targetTop = (mFrom + ((mDefaultLayoutOffsetTop - mFrom) * interpolatedTime).toInt())
        }

        val offset = targetTop - this.top
        setLayoutOffset(offset)
    }

    private fun setLayoutOffset(offset: Int) {
        val adjustOffset = offset
        ViewCompat.offsetTopAndBottom(this, adjustOffset)
        mCurrentLayoutOffsetTop = this.top
    }


    /** callback **/
    fun setOnDragSuccessCallback(callback: OnDragSuccess?){
        mDragSuccess = callback
    }

    interface OnDragSuccess {
        fun onDragSucces()
    }

    fun setOnChildScrollUpCallback(callback: OnChildScrollUpCallback?) {
        mChildScrollUpCallback = callback
    }

    interface OnChildScrollUpCallback {
        fun canChildScrollUp(parent: SwipeLayout, child: View?): Boolean
    }
}
