package com.csh.networkmonitor.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.csh.networkmonitor.NetworkMonitor
import com.csh.networkmonitor.R
import com.csh.networkmonitor.view.FloatWinView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class FloatWindowService : Service() {

    private var isRunning = false
    private var dps: Disposable? = null
    private var fv: FloatWinView? = null

    private val floatView by lazy {
        LayoutInflater.from(this).inflate(R.layout.float_window_view, null, false)
    }
    private val rxMobileTv by lazy { floatView?.findViewById<TextView>(R.id.tvMobileRx) }
    private val txMobileTv by lazy { floatView?.findViewById<TextView>(R.id.tvMobileTx) }
    private val rxWifiTv by lazy { floatView?.findViewById<TextView>(R.id.tvWlanRx) }
    private val txWifiTv by lazy { floatView?.findViewById<TextView>(R.id.tvWlanTx) }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (!isRunning) {
            getNetSpeed()
            isRunning = true
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 确保service运行中，此方法只能调用一次
     */
    private fun getNetSpeed() {
        // 显示悬浮窗
        fv = FloatWinView.Builder(
            this,
            floatView,
            contentParams = FrameLayout.LayoutParams(dip2px(80f), FrameLayout.LayoutParams.WRAP_CONTENT)
        ).build()
        // 开始获取网速
        Observable.interval(1000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { NetworkMonitor.reset() }
            .observeOn(AndroidSchedulers.mainThread())
            .takeUntil { !isRunning }
            .subscribe(object : Observer<Long> {
                override fun onSubscribe(d: Disposable) { dps = d }
                override fun onError(e: Throwable) {}
                override fun onComplete() {}
                override fun onNext(aLong: Long) {
                    // 获取网速
                    NetworkMonitor.loop(1000)
                    // 显示网速
                    val mr = NetworkMonitor.mobileRxSpeed
                    val mt = NetworkMonitor.mobileTxSpeed
                    val wr = NetworkMonitor.wifiRxSpeed
                    val wt = NetworkMonitor.wifiTxSpeed
                    // 格式化显示
                    rxMobileTv?.text = NetworkMonitor.formatSpeed(mr)
                    txMobileTv?.text = NetworkMonitor.formatSpeed(mt)
                    rxWifiTv?.text = NetworkMonitor.formatSpeed(wr)
                    txWifiTv?.text = NetworkMonitor.formatSpeed(wt)
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止定时器继续运行
        isRunning = false
        dps?.dispose()
        fv?.detach()
    }

    private fun dip2px(dpValue: Float): Int {
        val scale = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

}