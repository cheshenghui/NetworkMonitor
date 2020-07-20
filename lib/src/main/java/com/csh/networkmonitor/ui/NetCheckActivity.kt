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
    private var permissionResult = "--"
    private var connectResult = "--"
    private var serverConnectResult = "--"
    private var dnsTimeResult = "--"
    private var connectTimeResult = "--"

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
            btnChecking.showLoading()
            NetworkMonitor.check(this, ipStr, this)
        }

    }

    private fun onChecked(isOk: Boolean) {
        if (isOk) {
            ivResult.setImageResource(R.drawable.qy)
        } else {
            ivResult.setImageResource(R.drawable.qw)
        }
        tvResult.text = resultStr
        permission.text = permissionResult
        connect.text = connectResult
        serverConnect.text = serverConnectResult
        dnsTime.text = dnsTimeResult
        connectTime.text = connectTimeResult
    }

    override fun permissionCheckStart() {
        runOnUiThread {
            tv.text = "正在检测网络权限"
        }
    }

    override fun permissionOK() {
        permissionResult = "正常"
    }

    override fun netConnectCheckStart() {
        runOnUiThread {
            tv.text = "正在检测网络连接"
        }
    }

    override fun netConnectOk() {
        connectResult = "正常"
    }

    override fun serverConnectCheckStart() {
        runOnUiThread {
            tv.text = "正在检测服务器连接情况"
        }
    }

    override fun serverConnectOk() {
        serverConnectResult = "正常"
    }

    override fun startConnect() {
        runOnUiThread {
            tv.text = "正在检测域名解析耗时"
        }
    }

    override fun dnsParseTime(time: Long?) {
        dnsTimeResult = "${time}ms"
        runOnUiThread {
            tv.text = "正在检测连接耗时"
        }
    }

    override fun connectTime(time: Long?) {
        connectTimeResult = "${time}ms"
    }

    override fun error(type: Int, desc: String) {
        resultStr = "网络状况异常"
        when (type) {
            1000 -> {
                dnsTimeResult = "--"
                connectTimeResult = "--"
            }
            9000 -> permissionResult = "异常"
            8000 -> connectResult = "异常"
            7000 -> serverConnectResult = "异常"
        }
        runOnUiThread {
            checking.visibility = View.GONE
            onChecked(false)
        }
    }

    override fun complete() {
        resultStr = "网络状况正常"
        runOnUiThread {
            checking.visibility = View.GONE
            onChecked(true)
        }
    }

}