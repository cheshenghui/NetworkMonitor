package com.csh.networkmonitor.view

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout


class FloatWinView(
    context: Context,
    contentView: View?,
    private val lp: WindowManager.LayoutParams?,
    contentParams: LayoutParams?=null
) : FrameLayout(context) {

    init {
        // 设置view
        contentView?.let {
            setContentView(it, contentParams)
        }
        isClickable = true

        // 添加到window
        attach(lp)
    }

    private val windowManager: WindowManager
        get() = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Sets a content view that will be displayed inside this FloatWinView.
     */
    private fun setContentView(contentView: View?, contentParams: LayoutParams?) {
        val params: LayoutParams
        if (contentParams == null) {
            params = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        } else {
            params = contentParams
        }
        contentView?.isClickable = false
        this.addView(contentView, params)
    }

    /**
     * Attaches it to the content view with specified LayoutParams.
     * @param wLayoutParams
     */
    fun attach(wLayoutParams: ViewGroup.LayoutParams?) {
            try {
                windowManager.addView(this, wLayoutParams)
                update(getScreenWidth(context), getScreenHeight(context)/2)
            } catch (e: SecurityException) {
                throw SecurityException(
                    "Your application must have SYSTEM_ALERT_WINDOW " +
                            "permission to create a system window."
                )
            }
    }

    /**
     * Detaches it from the container view.
     */
    fun detach() {
        windowManager.removeView(this)
    }

    /**
     * Update it's position.
     */
    fun update(x: Int, y: Int) {
        val windowParams = this.layoutParams as WindowManager.LayoutParams
        windowParams.x = x
        windowParams.y = y
        windowManager.updateViewLayout(this, windowParams)
    }

    private var lastX: Int=0
    private var lastY: Int=0
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (lp == null) return super.dispatchTouchEvent(event)
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX.toInt()
                lastY = event.rawY.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                // 更新悬浮窗位置
                update((lp.x + (event.rawX - lastX)).toInt(),
                    (lp.y + (event.rawY - lastY)).toInt())
                lastX = event.rawX.toInt()
                lastY = event.rawY.toInt()
            }
            MotionEvent.ACTION_UP -> {
                // 贴边悬浮窗位置
                if (lp.x > getScreenWidth(context) / 2) {
                    lp.x =
                        getScreenWidth(context) - this.measuredWidth
                } else {
                    lp.x = 0
                }
                windowManager.updateViewLayout(this, lp)
            }
        }
        return super.dispatchTouchEvent(event)
    }


    /**
     * A builder for [FloatWinView] in conventional Java Builder format
     */
    class Builder(
        private val context: Context,
        private var contentView: View?,
        private var wLayoutParams: WindowManager.LayoutParams? = getDefaultWindowParams(),
        private var contentParams: LayoutParams? = null
    ) {

        companion object {
            fun getDefaultWindowParams(): WindowManager.LayoutParams {
                val wLayoutType: Int
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    wLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
//                    wLayoutType =  WindowManager.LayoutParams.TYPE_SYSTEM_ALERT  // z-ordering
                    wLayoutType = WindowManager.LayoutParams.TYPE_PHONE
                }
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    wLayoutType,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )
                params.format = PixelFormat.RGBA_8888
//                params.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                params.gravity = Gravity.CENTER_HORIZONTAL
                return params
            }
        }

        fun setWinLayoutParams(params: WindowManager.LayoutParams?): Builder {
            wLayoutParams = params
            return this
        }

        fun setContentView(
            contentView: View,
            contentParams: LayoutParams?=null
        ): Builder {
            this.contentView = contentView
            this.contentParams = contentParams
            return this
        }

        fun build(): FloatWinView {
            return FloatWinView(
                context,
                contentView,
                wLayoutParams,
                contentParams
            )
        }
    }

    /**
     * 获得屏幕高度
     *
     * @param context
     * @return
     */
    private fun getScreenWidth(context: Context): Int {
        val wm =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.widthPixels
    }

    /**
     * 获得屏幕宽度
     *
     * @param context
     * @return
     */
    private fun getScreenHeight(context: Context): Int {
        val wm =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.heightPixels
    }
}