package com.csh.networkmonitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.csh.networkmonitor.net.HttpEventListener
import kotlinx.coroutines.*
import okhttp3.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL


object NetworkMonitor {
    const val ERROR_CODE = 1000
    const val ERROR_CODE_PERMISSION = 90000
    const val ERROR_CODE_NET_CONNECT = 80000
    const val ERROR_CODE_SERVER_CONNECT = 70000

    private var rxTotal: Long = 0
    private var txTotal: Long = 0
    private var rxWifi: Long = 0
    private var txWifi: Long = 0
    private var rxMobile: Long = 0
    private var txMobile: Long = 0

    var mobileRxSpeed = 0L
        private set
    var mobileTxSpeed = 0L
        private set
    var wifiRxSpeed = 0L
        private set
    var wifiTxSpeed = 0L
        private set

    fun reset() {
        rxTotal = TrafficStats.getTotalRxBytes()
        txTotal = TrafficStats.getTotalTxBytes()

        rxMobile = TrafficStats.getMobileRxBytes()
        txMobile = TrafficStats.getMobileTxBytes()
        rxWifi = TrafficStats.getTotalRxBytes() - rxMobile
        txWifi = TrafficStats.getTotalTxBytes() - txMobile
    }

    fun loop(period: Long = 1000) {
        var _rxTotal = TrafficStats.getTotalRxBytes()
        var _txTotal = TrafficStats.getTotalTxBytes()
        val _rxMobile = TrafficStats.getMobileRxBytes()
        val _txMobile = TrafficStats.getMobileTxBytes()
        val _rxWifi = TrafficStats.getTotalRxBytes() - _rxMobile
        val _txWifi = TrafficStats.getTotalTxBytes() - _txMobile

        mobileRxSpeed = (_rxMobile - rxMobile) * 1000 / period // b
        mobileTxSpeed = (_txMobile - txMobile) * 1000 / period // b
        wifiRxSpeed = (_rxWifi - rxWifi) * 1000 / period // b
        wifiTxSpeed = (_txWifi - txWifi) * 1000 / period // b

        rxMobile = _rxMobile
        txMobile = _txMobile
        rxWifi = _rxWifi
        txWifi = _txWifi

    }

    fun formatSpeed(speed: Long): String {
        if (speed >= 1024) {
            if (speed / 1024 >= 1024)
                return "${speed / 1024 / 1024}m/s"
            else
                return "${speed / 1024}k/s"
        } else {
            return "${speed}b/s"
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @param ipStr http(s)://example.com
     */
    fun check(context: Context, ipStr: String="", listener: HttpEventListener.TimeConsumingListener?=null) {
        CoroutineScope(Dispatchers.Main).launch {
            // 1 网络权限
            val hasPermission =
                hasNetworkPermission(
                    context
                )
            listener?.permissionCheckStart()
            delay(500)
            if (!hasPermission) {
                listener?.error(ERROR_CODE_PERMISSION, "没有网络权限")
                return@launch
            }
            listener?.permissionOK()

            // 2 网络连接情况
            val hasConnected =
                hasNetworkConnected(context)
            listener?.netConnectCheckStart()
            delay(800)
            if (!hasConnected) {
                listener?.error(ERROR_CODE_NET_CONNECT, "网络连接失败")
                return@launch
            }
            listener?.netConnectOk()

//            // 3 服务器连接情况
//            val serverConnected =
//                checkServerConnection(ipStr)
//            listener?.serverConnectCheckStart()
//            delay(800)
//            if (!serverConnected) {
//                listener?.error(ERROR_CODE_SERVER_CONNECT, "服务器连接失败")
//                return@launch
//            }
//            listener?.serverConnectOk()

            // 4 监听
            listenerNetworkStatus(
                ipStr,
                listener
            )

        }
    }

    /**
     * 判断是否有网络权限
     *
     * @param context
     * @return
     */
    private suspend fun hasNetworkPermission(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            val permission = ActivityCompat.checkSelfPermission(
                context,
                "android.permission.INTERNET"
            ) // -1: 没有打开  0: 已经打开
            permission == 0
        }
    }

    /**
     * 判断是否有网络连接
     *
     * @param context
     * @return
     */
    private suspend fun hasNetworkConnected(context: Context?): Boolean {
        return withContext(Dispatchers.IO) {
            var ret = false
            if (context != null) {
                val cm =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                if (Build.VERSION.SDK_INT < 23) {
                    val networkInfo = cm.activeNetworkInfo
                    ret = networkInfo?.isAvailable ?: false
                } else {
                    val network = cm.activeNetwork
                    if (network != null) {
                        val nc = cm.getNetworkCapabilities(network)
                        if (nc != null)
                            ret = nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                                    || nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    }
                }
            }
            ret
        }
    }

//
//    /**
//     * 检查服务器是连接情况
//     * @param ipString http(s)://example.com
//     */
//    private suspend fun checkServerConnection(ipString: String): Boolean {
//        return withContext(Dispatchers.IO) {
//            val ipHost = URL(ipString).host
//            val p = Runtime.getRuntime().exec("ping -c 1 -w 1 $ipHost")
//            val input: InputStream = p.inputStream
//            val ins = BufferedReader(InputStreamReader(input))
//            val stringBuffer = StringBuffer()
//            var content: String?
//            while (ins.readLine().also { content = it } != null) {
//                stringBuffer.append(content)
//            }
//            // 0 表示正常停止，即正常完成，未出现异常情况; 1 表示网络已连接，但是无法访问; 2 表示网络未连接
//            val isOK = (p.waitFor() == 0)
//            isOK
//        }
//    }

    /**
     * 监听 DNS耗时、连接耗时...
     * @param url http(s)://example.com
     */
    private fun listenerNetworkStatus(url: String, listen: HttpEventListener.TimeConsumingListener?=null) {
        val okClient =
            OkHttpClient.Builder()
//                .eventListenerFactory(HttpEventListener.FACTORY)
                .eventListenerFactory {
                    HttpEventListener(
                        null,
                        it.request().url(),
                        System.currentTimeMillis(),
                        listen
                    )
                }
                .build()
        val request: Request = Request.Builder().get().url(url).build()
        val call: Call = okClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                Log.e("Test Network onFailure:", e?.message ?: "")
            }

            override fun onResponse(call: Call?, response: Response) {
                val ret = response.body()?.string()
            }
        })
    }


}