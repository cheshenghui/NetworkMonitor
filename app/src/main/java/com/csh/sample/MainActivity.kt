package com.csh.sample

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.csh.networkmonitor.service.FloatWindowService
import com.csh.networkmonitor.ui.NetCheckActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

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
                open()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        open?.setOnClickListener {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(applicationContext)
            ) {
                Toast.makeText(
                    applicationContext,
                    "请开启悬浮窗权限",
                    Toast.LENGTH_SHORT
                ).show()
                startActivityLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } else {
                open()
                startActivity(Intent(this, NetCheckActivity::class.java)
                    .putExtra(NetCheckActivity.EXTRA_IP, "www.baidu.com"))
            }

        }

        close?.setOnClickListener {
            stopService(Intent(this, FloatWindowService::class.java))
        }

    }

    private fun open() {
//        val fv = FloatWinView.Builder(
//            this,
//            LayoutInflater.from(this).inflate(R.layout.float_window_view, null, false)
//        ).build()
        startService(Intent(this, FloatWindowService::class.java))
    }



}