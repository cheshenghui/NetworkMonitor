package com.csh.networkmonitor.ui

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.csh.networkmonitor.NetworkMonitor
import com.csh.networkmonitor.R
import com.csh.networkmonitor.net.HttpEventListener
import kotlinx.android.synthetic.main.activity_network_check.*


class NetCheckActivity: AppCompatActivity(), HttpEventListener.TimeConsumingListener {

    companion object {
        const val EXTRA_IP = "EXTRA_IP"
    }

    private val ipStr by lazy { intent.extras?.getString(EXTRA_IP)?:"" }

    private val permissionLauncher : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

        }

    private val startActivityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(applicationContext)
            ) {
                Toast.makeText(
                    applicationContext,
                    "请开启悬浮窗权限",
                    Toast.LENGTH_SHORT
                ).show()
            } else {

            }
        }

    private var resultStr = "网络状况正常"
    private var resultStr2 = "完成网络监测，当前网络状况良好"

    private var permissionResult = "--"
    private var permissionColor = Color.RED

    private var connectResult = "--"
    private var connectColor = Color.RED

    private var serverConnectResult = "--"
    private var serverConnectColor = Color.RED

    private var dnsTimeResult = "--"
    private var dnsTimeColor = Color.RED

    private var connectTimeResult = "--"
    private var connectTimeColor = Color.RED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_check)

        checking.visibility = View.VISIBLE

        back?.setOnClickListener { finish() }

        btnChecking.setOnClickListener {
            btnChecking.showLoading()
            NetworkMonitor.check(this, ipStr, this)
        }

        btnCheck.setOnClickListener {
            checking.visibility = View.VISIBLE
            resetCheckResult()
            btnChecking.showLoading()
            NetworkMonitor.check(this, ipStr, this)
        }

    }

    private fun resetCheckResult() {
        resultStr = "网络状况正常"
        resultStr2 = "完成网络监测，当前网络状况良好"

        permissionResult = "--"
        permissionColor = Color.RED

        connectResult = "--"
        connectColor = Color.RED

        serverConnectResult = "--"
        serverConnectColor = Color.RED

        dnsTimeResult = "--"
        dnsTimeColor = Color.RED

        connectTimeResult = "--"
        connectTimeColor = Color.RED
    }

    private fun onCheckOver(isOk: Boolean) {
        if (isOk) {
            ivResult.setImageResource(R.drawable.qy)
        } else {
            ivResult.setImageResource(R.drawable.qw)
        }
        tvResult.text = resultStr
        tvResultTip.text = resultStr2
        permission.text = permissionResult
        permission.setTextColor(permissionColor)
        connect.text = connectResult
        connect.setTextColor(connectColor)
        serverConnect.text = serverConnectResult
        serverConnect.setTextColor(serverConnectColor)
        dnsTime.text = dnsTimeResult
        dnsTime.setTextColor(dnsTimeColor)
        connectTime.text = connectTimeResult
        connectTime.setTextColor(connectTimeColor)
    }

    override fun permissionCheckStart() {
        runOnUiThread {
            tv.text = "正在检测网络权限"
        }
    }

    override fun permissionOK() {
        permissionResult = "正常"
        permissionColor = ContextCompat.getColor(this, R.color.blue)
    }

    override fun netConnectCheckStart() {
        runOnUiThread {
            tv.text = "正在检测网络连接"
        }
    }

    override fun netConnectOk() {
        connectResult = "正常"
        connectColor = ContextCompat.getColor(this, R.color.blue)
    }

    override fun serverConnectCheckStart() {
        runOnUiThread {
            tv.text = "正在检测服务器连接情况"
        }
    }

    override fun serverConnectOk() {
        serverConnectResult = "正常"
        serverConnectColor = ContextCompat.getColor(this, R.color.blue)
    }

    override fun startConnect() {
        runOnUiThread {
            tv.text = "正在检测域名解析耗时"
        }
    }

    override fun dnsParseTime(time: Long?) {
        dnsTimeResult = "${time}ms"
        dnsTimeColor = ContextCompat.getColor(this, R.color.blue)
        runOnUiThread {
            tv.text = "正在检测连接耗时"
        }
    }

    override fun connectTime(time: Long?) {
        connectTimeResult = "${time}ms"
        connectTimeColor = ContextCompat.getColor(this, R.color.blue)
    }

    override fun error(type: Int, desc: String) {
        resultStr = "网络状况异常"
        resultStr2 = "完成网络监测，请切换其他良好网络"
        when (type) {
            NetworkMonitor.ERROR_CODE -> {
                dnsTimeResult = "--"
                connectTimeResult = "--"
                dnsTimeColor = ContextCompat.getColor(this, R.color.red)
                connectTimeColor = ContextCompat.getColor(this, R.color.red)
            }
            NetworkMonitor.ERROR_CODE_PERMISSION -> {
                permissionResult = "异常"
                permissionColor = ContextCompat.getColor(this, R.color.red)
            }
            NetworkMonitor.ERROR_CODE_NET_CONNECT -> {
                connectResult = "异常"
                connectColor = ContextCompat.getColor(this, R.color.red)
            }
            NetworkMonitor.ERROR_CODE_SERVER_CONNECT -> {
                serverConnectResult = "异常"
                serverConnectColor = ContextCompat.getColor(this, R.color.red)
            }
        }
        runOnUiThread {
            checking.visibility = View.GONE
            onCheckOver(false)
        }
    }

    override fun complete() {
        resultStr = "网络状况正常"
        resultStr2 = "完成网络监测，当前网络状况良好"
        runOnUiThread {
            checking.visibility = View.GONE
            onCheckOver(true)
        }
    }

}